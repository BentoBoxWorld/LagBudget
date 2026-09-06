package world.bentobox.lagbudget.sampler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.lagbudget.LagBudget;
import world.bentobox.lagbudget.Settings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BudgetSamplerTest {

    private static final String ISLAND_ONE = "island-1";
    private static final String ISLAND_TWO = "island-2";

    @Mock
    private LagBudget addon;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private World world;
    @Mock
    private Island island;
    @Mock
    private Island island2;
    @Mock
    private Island spawn;
    @Mock
    private Location onIsland;
    @Mock
    private Location onIsland2;
    @Mock
    private Location onSpawn;
    @Mock
    private Location nowhere;

    private YamlConfiguration config;
    private UUID owner;
    private BudgetSampler sampler;
    private MockedStatic<BentoBox> mockedBentoBox;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        mockedBentoBox = Mockito.mockStatic(BentoBox.class);
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(plugin);

        config = new YamlConfiguration();
        config.load("src/main/resources/config.yml");
        when(addon.getConfig()).thenReturn(config);
        refreshSettings();
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(pm);
        when(addon.getGameModes()).thenReturn(List.of(gameMode));
        when(gameMode.getOverWorld()).thenReturn(world);

        owner = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn(ISLAND_ONE);
        when(island.getOwner()).thenReturn(owner);
        when(island2.getUniqueId()).thenReturn(ISLAND_TWO);
        when(spawn.getUniqueId()).thenReturn("spawn");
        when(spawn.isSpawn()).thenReturn(true);
        when(im.getIslandAt(onIsland)).thenReturn(Optional.of(island));
        when(im.getIslandAt(onIsland2)).thenReturn(Optional.of(island2));
        when(im.getIslandAt(onSpawn)).thenReturn(Optional.of(spawn));
        when(im.getIslandAt(nowhere)).thenReturn(Optional.empty());
        when(pm.getName(owner)).thenReturn("tastybento");

        sampler = new BudgetSampler(addon);
    }

    @AfterEach
    void tearDown() {
        if (sampler != null) {
            sampler.stop();
        }
        if (mockedBentoBox != null) {
            mockedBentoBox.close();
        }
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private BlockState blockEntity(Material type, Location loc) {
        BlockState bs = mock(BlockState.class);
        when(bs.getType()).thenReturn(type);
        when(bs.getLocation()).thenReturn(loc);
        return bs;
    }

    private <T extends Entity> T entity(Class<T> clazz, EntityType type, Location loc) {
        T e = mock(clazz);
        when(e.getType()).thenReturn(type);
        when(e.getLocation()).thenReturn(loc);
        return e;
    }

    private Chunk chunk(BlockState[] blockEntities, Entity... entities) {
        Chunk c = mock(Chunk.class);
        when(c.isLoaded()).thenReturn(true);
        when(c.getTileEntities(false)).thenReturn(blockEntities);
        when(c.getEntities()).thenReturn(entities);
        return c;
    }

    private void refreshSettings() {
        Settings settings = new Settings(addon);
        when(addon.getSettings()).thenReturn(settings);
    }

    @Test
    void testSampleChunkAttributesWeightedBlockEntities() {
        Chunk c = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland), blockEntity(Material.HOPPER, onIsland),
                blockEntity(Material.SPAWNER, onIsland), blockEntity(Material.CHEST, onIsland) });
        sampler.sampleChunk(c);
        sampler.finishPass();

        IslandScore sc = sampler.getScore(ISLAND_ONE).orElseThrow();
        assertEquals(11, sc.getTotal(), "2 hoppers x3 + spawner x5; chest is not budgeted");
        assertEquals(2, sc.getCount("HOPPER"));
        assertEquals(1, sc.getCount("SPAWNER"));
        assertEquals(0, sc.getCount("CHEST"));
        assertEquals(owner, sc.getOwner());
    }

    @Test
    void testSampleChunkAttributesEntities() {
        Chunk c = chunk(new BlockState[0],
                entity(Villager.class, EntityType.VILLAGER, onIsland),
                entity(Sheep.class, EntityType.SHEEP, onIsland),
                entity(Item.class, EntityType.ITEM, onIsland),
                entity(ArmorStand.class, EntityType.ARMOR_STAND, onIsland),
                entity(Player.class, EntityType.PLAYER, onIsland));
        sampler.sampleChunk(c);
        sampler.finishPass();

        IslandScore sc = sampler.getScore(ISLAND_ONE).orElseThrow();
        assertEquals(12, sc.getTotal(), "villager 10 + sheep default 1 + item 1; armor stand and player 0");
        assertEquals(0, sc.getCount("PLAYER"));
        assertEquals(0, sc.getCount("ARMOR_STAND"));
    }

    @Test
    void testSampleChunkSplitsAcrossIslandsAndSkipsSpawnAndNoIsland() {
        Chunk c = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland), blockEntity(Material.HOPPER, onIsland2),
                blockEntity(Material.HOPPER, onSpawn), blockEntity(Material.HOPPER, nowhere) });
        sampler.sampleChunk(c);
        sampler.finishPass();

        assertEquals(3, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal());
        assertEquals(3, sampler.getScore(ISLAND_TWO).orElseThrow().getTotal());
        assertTrue(sampler.getScore("spawn").isEmpty(), "spawn islands are never budgeted");
        assertEquals(2, sampler.getLatest().size());
    }

    @Test
    void testUnloadedChunkSkipped() {
        Chunk c = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) });
        when(c.isLoaded()).thenReturn(false);
        sampler.sampleChunk(c);
        sampler.finishPass();
        assertTrue(sampler.getLatest().isEmpty());
        verify(c, never()).getTileEntities(false);
    }

    @Test
    void testNothingPublishedBeforeFinishPass() {
        sampler.sampleChunk(chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) }));
        assertFalse(sampler.hasSample());
        assertTrue(sampler.getScore(ISLAND_ONE).isEmpty());
        sampler.finishPass();
        assertTrue(sampler.hasSample());
        assertTrue(sampler.getScore(ISLAND_ONE).isPresent());
    }

    @Test
    void testOverBudgetDetectedAndLogged() {
        // 167 hoppers x 3 = 501 > 500
        BlockState[] hoppers = new BlockState[167];
        Arrays.fill(hoppers, blockEntity(Material.HOPPER, onIsland));
        sampler.sampleChunk(chunk(hoppers));
        sampler.finishPass();

        assertTrue(sampler.isOverBudget(ISLAND_ONE));
        assertFalse(sampler.isOverBudget(ISLAND_TWO));
        verify(addon).log(contains("tastybento is over the lag budget: 501/500 (HOPPER 501)"));
    }

    @Test
    void testExactlyAtBudgetIsNotOver() {
        BlockState[] hoppers = new BlockState[100];
        Arrays.fill(hoppers, blockEntity(Material.SPAWNER, onIsland));
        sampler.sampleChunk(chunk(hoppers));
        sampler.finishPass();
        assertEquals(500, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal());
        assertFalse(sampler.isOverBudget(ISLAND_ONE));
        verify(addon, never()).log(any());
    }

    @Test
    void testBackUnderBudgetLoggedOnce() {
        BlockState[] hoppers = new BlockState[200];
        Arrays.fill(hoppers, blockEntity(Material.HOPPER, onIsland));
        sampler.sampleChunk(chunk(hoppers));
        sampler.finishPass();
        assertTrue(sampler.isOverBudget(ISLAND_ONE));

        // Still over on the next pass: no repeat log
        sampler.sampleChunk(chunk(hoppers));
        sampler.finishPass();
        verify(addon, times(1)).log(contains("over the lag budget"));

        // Hoppers gone: back under, logged once, no longer over
        sampler.sampleChunk(chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) }));
        sampler.finishPass();
        assertFalse(sampler.isOverBudget(ISLAND_ONE));
        verify(addon).log(contains("tastybento is back under the lag budget"));
        assertEquals(3, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal());
    }

    @Test
    void testLoggingCanBeDisabled() {
        config.set("log-over-budget", false);
        refreshSettings();
        BlockState[] hoppers = new BlockState[200];
        Arrays.fill(hoppers, blockEntity(Material.HOPPER, onIsland));
        sampler.sampleChunk(chunk(hoppers));
        sampler.finishPass();
        assertTrue(sampler.isOverBudget(ISLAND_ONE));
        verify(addon, never()).log(any());
    }

    @Test
    void testUnknownOwnerDescribedByIslandId() {
        when(island.getOwner()).thenReturn(null);
        BlockState[] hoppers = new BlockState[200];
        Arrays.fill(hoppers, blockEntity(Material.HOPPER, onIsland));
        sampler.sampleChunk(chunk(hoppers));
        sampler.finishPass();
        verify(addon).log(contains("Island of " + ISLAND_ONE + " is over"));
    }

    @Test
    void testGetTopOrdersHighestFirst() {
        sampler.sampleChunk(chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) },
                entity(Villager.class, EntityType.VILLAGER, onIsland2)));
        sampler.finishPass();

        List<IslandScore> top = sampler.getTop(10);
        assertEquals(2, top.size());
        assertEquals(ISLAND_TWO, top.get(0).getIslandId());
        assertEquals(ISLAND_ONE, top.get(1).getIslandId());
        assertEquals(1, sampler.getTop(1).size());
    }

    @Test
    void testPreviousPassStaysReadableWhileNextBuilds() {
        sampler.sampleChunk(chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) }));
        sampler.finishPass();
        // A new pass in progress must not disturb the published result
        sampler.sampleChunk(chunk(new BlockState[] { blockEntity(Material.SPAWNER, onIsland) }));
        assertEquals(3, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal());
        sampler.finishPass();
        assertEquals(5, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal());
    }

    @Test
    void testScheduledPassSamplesLoadedChunksOfGameModeWorlds() {
        World nether = mock(World.class);
        when(gameMode.getNetherWorld()).thenReturn(nether);
        when(gameMode.getEndWorld()).thenReturn(null);
        Chunk overworldChunk = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) });
        Chunk netherChunk = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) });
        when(world.getLoadedChunks()).thenReturn(new Chunk[] { overworldChunk });
        when(nether.getLoadedChunks()).thenReturn(new Chunk[] { netherChunk });

        sampler.start();
        MockBukkit.getMock().getScheduler().performOneTick();

        assertTrue(sampler.hasSample());
        assertEquals(6, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal(), "one hopper in each of overworld and nether");
    }

    @Test
    void testScheduledPassIsPacedByChunksPerTick() {
        Chunk c = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) });
        Chunk[] loaded = new Chunk[250];
        Arrays.fill(loaded, c);
        when(world.getLoadedChunks()).thenReturn(loaded);

        sampler.start();
        MockBukkit.getMock().getScheduler().performOneTick();
        assertFalse(sampler.hasSample(), "250 chunks at 100 per tick must not finish in one tick");
        MockBukkit.getMock().getScheduler().performOneTick();
        assertFalse(sampler.hasSample());
        MockBukkit.getMock().getScheduler().performOneTick();
        assertTrue(sampler.hasSample());
        assertEquals(750, sampler.getScore(ISLAND_ONE).orElseThrow().getTotal());
        verify(c, times(250)).getTileEntities(false);
    }

    @Test
    void testNextPassWaitsForInterval() {
        Chunk c = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) });
        when(world.getLoadedChunks()).thenReturn(new Chunk[] { c });

        sampler.start();
        MockBukkit.getMock().getScheduler().performTicks(40L);
        // First pass ran on tick 1; the 30 second interval has not elapsed in wall-clock time
        verify(c, times(1)).getTileEntities(false);
    }

    @Test
    void testStopCancelsSamplingButKeepsResults() {
        Chunk c = chunk(new BlockState[] { blockEntity(Material.HOPPER, onIsland) });
        when(world.getLoadedChunks()).thenReturn(new Chunk[] { c });
        sampler.start();
        MockBukkit.getMock().getScheduler().performOneTick();
        assertTrue(sampler.hasSample());

        sampler.stop();
        MockBukkit.getMock().getScheduler().performTicks(5L);
        verify(c, times(1)).getTileEntities(false);
        assertTrue(sampler.getScore(ISLAND_ONE).isPresent());
        assertEquals(0, MockBukkit.getMock().getScheduler().getPendingTasks().size());
    }

    @Test
    void testStartTwiceRunsOneTask() {
        sampler.start();
        sampler.start();
        assertEquals(1, MockBukkit.getMock().getScheduler().getPendingTasks().size());
    }
}
