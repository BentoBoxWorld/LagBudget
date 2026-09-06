package world.bentobox.lagbudget.sampler;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.lagbudget.LagBudget;
import world.bentobox.lagbudget.Settings;

/**
 * Periodically samples every loaded chunk in the game mode worlds and attributes the
 * weighted cost of its block entities and entities to the island they sit on.
 *
 * <p>A pass starts every {@code sample-interval} seconds and processes
 * {@code chunks-per-tick} chunks per tick until the snapshot of loaded chunks taken at
 * the start of the pass is exhausted. Only then are the results published, so readers
 * always see a complete, consistent pass. Everything runs on the main thread; block
 * entities are read without snapshots, and no block is ever scanned.
 *
 * @author tastybento
 */
public class BudgetSampler {

    private final LagBudget addon;
    private BukkitTask task;
    private final Deque<Chunk> pending = new ArrayDeque<>();
    private Map<String, IslandScore> building = new HashMap<>();
    private volatile Map<String, IslandScore> latest = Map.of();
    private volatile Set<String> overBudget = Set.of();
    private long lastPassStart;
    private boolean passRunning;
    private boolean sampled;

    public BudgetSampler(LagBudget addon) {
        this.addon = addon;
    }

    /** Start (or restart) the sampling task. */
    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(addon.getPlugin(), this::tick, 1L, 1L);
    }

    /** Stop sampling. Published results stay readable. */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        pending.clear();
        building = new HashMap<>();
        passRunning = false;
    }

    void tick() {
        if (!passRunning) {
            long now = System.currentTimeMillis();
            if (now - lastPassStart < addon.getSettings().getSampleInterval() * 1000L) {
                return;
            }
            beginPass(now);
        }
        int budget = addon.getSettings().getChunksPerTick();
        while (budget-- > 0 && !pending.isEmpty()) {
            sampleChunk(pending.poll());
        }
        if (pending.isEmpty()) {
            finishPass();
        }
    }

    void beginPass(long now) {
        lastPassStart = now;
        passRunning = true;
        building = new HashMap<>();
        for (GameModeAddon gm : addon.getGameModes()) {
            for (World world : Arrays.asList(gm.getOverWorld(), gm.getNetherWorld(), gm.getEndWorld())) {
                if (world != null) {
                    pending.addAll(Arrays.asList(world.getLoadedChunks()));
                }
            }
        }
    }

    /**
     * Attribute everything budgeted in one chunk. Package-private for tests.
     */
    void sampleChunk(Chunk chunk) {
        if (!chunk.isLoaded()) {
            return;
        }
        Settings s = addon.getSettings();
        for (BlockState bs : chunk.getTileEntities(false)) {
            double w = s.blockWeight(bs.getType());
            if (w > 0) {
                attribute(bs.getLocation(), bs.getType().name(), w);
            }
        }
        for (Entity e : chunk.getEntities()) {
            if (e instanceof Player) {
                continue;
            }
            double w = s.entityWeight(e.getType(), e instanceof Mob);
            if (w > 0) {
                attribute(e.getLocation(), e.getType().name(), w);
            }
        }
    }

    private void attribute(Location loc, String key, double weight) {
        addon.getIslands().getIslandAt(loc)
                .filter(i -> !i.isSpawn())
                .ifPresent(i -> building.computeIfAbsent(i.getUniqueId(), id -> new IslandScore(id, i.getOwner()))
                        .add(key, weight));
    }

    /**
     * Publish the pass that just completed and log budget-line crossings. Package-private for tests.
     */
    void finishPass() {
        passRunning = false;
        double budget = addon.getSettings().getBudget();
        Set<String> nowOver = new HashSet<>();
        for (IslandScore sc : building.values()) {
            if (sc.getTotal() > budget) {
                nowOver.add(sc.getIslandId());
            }
        }
        if (addon.getSettings().isLogOverBudget()) {
            for (String id : nowOver) {
                if (!overBudget.contains(id)) {
                    IslandScore sc = building.get(id);
                    addon.log("Island of " + describe(sc) + " is over the lag budget: " + IslandScore.format(sc.getTotal())
                            + "/" + IslandScore.format(budget) + " (" + sc.summary(3) + "). Dropping spawns there.");
                }
            }
            for (String id : overBudget) {
                if (!nowOver.contains(id)) {
                    IslandScore sc = latest.get(id);
                    addon.log("Island of " + describe(sc) + " is back under the lag budget.");
                }
            }
        }
        latest = Collections.unmodifiableMap(building);
        overBudget = Collections.unmodifiableSet(nowOver);
        building = new HashMap<>();
        sampled = true;
    }

    private String describe(IslandScore sc) {
        if (sc == null) {
            return "unknown island";
        }
        UUID owner = sc.getOwner();
        String name = owner == null ? "" : addon.getPlayers().getName(owner);
        return name == null || name.isEmpty() ? sc.getIslandId() : name;
    }

    /** @return true once at least one full pass has been published */
    public boolean hasSample() {
        return sampled;
    }

    /**
     * @param islandId island unique id
     * @return true if the island was over budget in the last published pass
     */
    public boolean isOverBudget(String islandId) {
        return overBudget.contains(islandId);
    }

    /**
     * @param islandId island unique id
     * @return the island's score from the last published pass, if it had any budgeted content
     */
    public Optional<IslandScore> getScore(String islandId) {
        return Optional.ofNullable(latest.get(islandId));
    }

    /**
     * @param island island
     * @return the island's score from the last published pass, if any
     */
    public Optional<IslandScore> getScore(Island island) {
        return getScore(island.getUniqueId());
    }

    /**
     * @param n maximum number of islands
     * @return islands ranked by score, highest first
     */
    public List<IslandScore> getTop(int n) {
        return latest.values().stream()
                .sorted(Comparator.comparingDouble(IslandScore::getTotal).reversed())
                .limit(n)
                .toList();
    }

    /** @return every island scored in the last published pass, unordered */
    public Map<String, IslandScore> getLatest() {
        return latest;
    }
}
