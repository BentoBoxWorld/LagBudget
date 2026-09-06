package world.bentobox.lagbudget;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 * Parsed {@code config.yml}. Immutable after construction; build a new one to reload.
 *
 * @author tastybento
 */
public class Settings {

    private final List<String> gameModes;
    private final double budget;
    private final int sampleInterval;
    private final int chunksPerTick;
    private final boolean logOverBudget;
    private final boolean placeholders;
    private final int topSize;
    private final Set<SpawnReason> dropSpawnReasons = EnumSet.noneOf(SpawnReason.class);
    private final boolean dropHopperTransfers;
    private final Map<Material, Double> blockWeights = new EnumMap<>(Material.class);
    private final Map<EntityType, Double> entityWeights = new EnumMap<>(EntityType.class);
    private final double defaultMobWeight;

    public Settings(LagBudget addon) {
        FileConfiguration c = addon.getConfig();
        gameModes = c.getStringList("gamemodes");
        budget = Math.max(0, c.getDouble("budget", 500));
        sampleInterval = Math.max(1, c.getInt("sample-interval", 30));
        chunksPerTick = Math.max(1, c.getInt("chunks-per-tick", 100));
        logOverBudget = c.getBoolean("log-over-budget", true);
        placeholders = c.getBoolean("placeholders", false);
        topSize = Math.max(1, c.getInt("top-size", 10));
        for (String name : c.getStringList("drop.spawn-reasons")) {
            try {
                dropSpawnReasons.add(SpawnReason.valueOf(name.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                addon.logError("Unknown spawn reason in drop.spawn-reasons: " + name);
            }
        }
        dropHopperTransfers = c.getBoolean("drop.hopper-transfers", false);
        loadBlockWeights(addon, c.getConfigurationSection("block-weights"));
        loadEntityWeights(addon, c.getConfigurationSection("entity-weights"));
        defaultMobWeight = Math.max(0, c.getDouble("default-mob-weight", 1));
    }

    private void loadBlockWeights(LagBudget addon, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Material m = Material.matchMaterial(key);
            if (m == null) {
                addon.logError("Unknown material in block-weights: " + key);
                continue;
            }
            double w = section.getDouble(key, 0);
            if (w > 0) {
                blockWeights.put(m, w);
            }
        }
    }

    private void loadEntityWeights(LagBudget addon, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                EntityType t = EntityType.valueOf(key.toUpperCase(Locale.ENGLISH));
                double w = section.getDouble(key, 0);
                if (w > 0) {
                    entityWeights.put(t, w);
                }
            } catch (IllegalArgumentException e) {
                addon.logError("Unknown entity type in entity-weights: " + key);
            }
        }
    }

    /**
     * @param material block entity material
     * @return its weight, or 0 if it is not budgeted
     */
    public double blockWeight(Material material) {
        return blockWeights.getOrDefault(material, 0d);
    }

    /**
     * @param type entity type
     * @param isMob true if the entity is a {@link org.bukkit.entity.Mob} (has AI)
     * @return the configured weight, else the default mob weight for mobs, else 0
     */
    public double entityWeight(EntityType type, boolean isMob) {
        Double w = entityWeights.get(type);
        if (w != null) {
            return w;
        }
        return isMob ? defaultMobWeight : 0d;
    }

    public List<String> getGameModes() {
        return gameModes;
    }

    public double getBudget() {
        return budget;
    }

    /** @return seconds between the start of one sample pass and the next */
    public int getSampleInterval() {
        return sampleInterval;
    }

    public int getChunksPerTick() {
        return chunksPerTick;
    }

    public boolean isLogOverBudget() {
        return logOverBudget;
    }

    public boolean isPlaceholders() {
        return placeholders;
    }

    public int getTopSize() {
        return topSize;
    }

    public Set<SpawnReason> getDropSpawnReasons() {
        return dropSpawnReasons;
    }

    public boolean isDropHopperTransfers() {
        return dropHopperTransfers;
    }

    public Map<Material, Double> getBlockWeights() {
        return blockWeights;
    }

    public Map<EntityType, Double> getEntityWeights() {
        return entityWeights;
    }

    public double getDefaultMobWeight() {
        return defaultMobWeight;
    }
}
