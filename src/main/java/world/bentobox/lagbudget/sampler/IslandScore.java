package world.bentobox.lagbudget.sampler;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

/**
 * The lag score of one island from one sample pass: the weighted total plus a per-key
 * breakdown, where a key is a block entity Material name or an EntityType name.
 *
 * @author tastybento
 */
public class IslandScore {

    private final String islandId;
    @Nullable
    private final UUID owner;
    private double total;
    private final Map<String, Double> scores = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();

    public IslandScore(String islandId, @Nullable UUID owner) {
        this.islandId = islandId;
        this.owner = owner;
    }

    /**
     * Add one budgeted thing to this island.
     * @param key what it was (Material or EntityType name)
     * @param weight its cost
     */
    public void add(String key, double weight) {
        total += weight;
        scores.merge(key, weight, Double::sum);
        counts.merge(key, 1, Integer::sum);
    }

    public String getIslandId() {
        return islandId;
    }

    @Nullable
    public UUID getOwner() {
        return owner;
    }

    public double getTotal() {
        return total;
    }

    /** @return weighted score per key */
    public Map<String, Double> getScores() {
        return scores;
    }

    /** @return number of things per key */
    public Map<String, Integer> getCounts() {
        return counts;
    }

    public int getCount(String key) {
        return counts.getOrDefault(key, 0);
    }

    /**
     * @param n maximum entries
     * @return the keys contributing the most score, highest first
     */
    public List<Map.Entry<String, Double>> topContributors(int n) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(n)
                .toList();
    }

    /**
     * @param n maximum entries
     * @return a short human summary such as {@code HOPPER 120, VILLAGER 80}
     */
    public String summary(int n) {
        return topContributors(n).stream()
                .map(e -> e.getKey() + " " + format(e.getValue()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Format a score for display: whole numbers, no locale surprises.
     */
    public static String format(double value) {
        return String.format(Locale.ENGLISH, "%.0f", value);
    }
}
