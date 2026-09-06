package world.bentobox.lagbudget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettingsTest {

    @Mock
    private LagBudget addon;
    private YamlConfiguration config;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        config = new YamlConfiguration();
        config.load("src/main/resources/config.yml");
        when(addon.getConfig()).thenReturn(config);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testDefaults() {
        Settings s = new Settings(addon);
        assertEquals(List.of("BSkyBlock", "AcidIsland", "CaveBlock", "AOneBlock", "SkyGrid", "Boxed"), s.getGameModes());
        assertEquals(500, s.getBudget());
        assertEquals(30, s.getSampleInterval());
        assertEquals(100, s.getChunksPerTick());
        assertTrue(s.isLogOverBudget());
        assertFalse(s.isPlaceholders(), "placeholders must be off by default");
        assertEquals(10, s.getTopSize());
        assertFalse(s.isDropHopperTransfers(), "hopper dropping must be off by default");
        assertEquals(1.0, s.getDefaultMobWeight());
        verify(addon, never()).logError(contains("Unknown"));
    }

    @Test
    void testDropSpawnReasons() {
        Settings s = new Settings(addon);
        assertTrue(s.getDropSpawnReasons().contains(SpawnReason.NATURAL));
        assertTrue(s.getDropSpawnReasons().contains(SpawnReason.SPAWNER));
        assertTrue(s.getDropSpawnReasons().contains(SpawnReason.BREEDING));
        assertFalse(s.getDropSpawnReasons().contains(SpawnReason.CUSTOM));
    }

    @Test
    void testUnknownSpawnReasonLogged() {
        config.set("drop.spawn-reasons", List.of("NATURAL", "NOT_A_REASON"));
        Settings s = new Settings(addon);
        assertTrue(s.getDropSpawnReasons().contains(SpawnReason.NATURAL));
        assertEquals(1, s.getDropSpawnReasons().size());
        verify(addon).logError(contains("NOT_A_REASON"));
    }

    @Test
    void testBlockWeights() {
        Settings s = new Settings(addon);
        assertEquals(3, s.blockWeight(Material.HOPPER));
        assertEquals(5, s.blockWeight(Material.SPAWNER));
        assertEquals(0, s.blockWeight(Material.CHEST), "unlisted block entities cost nothing");
        assertEquals(0, s.blockWeight(Material.STONE));
    }

    @Test
    void testEntityWeights() {
        Settings s = new Settings(addon);
        assertEquals(10, s.entityWeight(EntityType.VILLAGER, true));
        assertEquals(6, s.entityWeight(EntityType.HOPPER_MINECART, false));
        assertEquals(1, s.entityWeight(EntityType.ITEM, false), "listed non-mobs use their weight");
        assertEquals(1, s.entityWeight(EntityType.SHEEP, true), "unlisted mobs use the default mob weight");
        assertEquals(0, s.entityWeight(EntityType.ARMOR_STAND, false), "unlisted non-mobs cost nothing");
        assertEquals(0, s.entityWeight(EntityType.PLAYER, false));
    }

    @Test
    void testDefaultMobWeightZeroIgnoresUnlistedMobs() {
        config.set("default-mob-weight", 0);
        Settings s = new Settings(addon);
        assertEquals(0, s.entityWeight(EntityType.SHEEP, true));
        assertEquals(10, s.entityWeight(EntityType.VILLAGER, true));
    }

    @Test
    void testUnknownMaterialAndEntityLogged() {
        config.set("block-weights.NOT_A_BLOCK", 4);
        config.set("entity-weights.NOT_AN_ENTITY", 4);
        new Settings(addon);
        verify(addon).logError(contains("NOT_A_BLOCK"));
        verify(addon).logError(contains("NOT_AN_ENTITY"));
    }

    @Test
    void testZeroAndNegativeWeightsIgnored() {
        config.set("block-weights.HOPPER", 0);
        config.set("entity-weights.VILLAGER", -5);
        Settings s = new Settings(addon);
        assertEquals(0, s.blockWeight(Material.HOPPER));
        assertFalse(s.getBlockWeights().containsKey(Material.HOPPER));
        assertEquals(1, s.entityWeight(EntityType.VILLAGER, true), "negative weight dropped, default mob weight applies");
    }

    @Test
    void testBoundsClamped() {
        config.set("sample-interval", 0);
        config.set("chunks-per-tick", -3);
        config.set("top-size", 0);
        config.set("budget", -1);
        Settings s = new Settings(addon);
        assertEquals(1, s.getSampleInterval());
        assertEquals(1, s.getChunksPerTick());
        assertEquals(1, s.getTopSize());
        assertEquals(0, s.getBudget());
    }
}
