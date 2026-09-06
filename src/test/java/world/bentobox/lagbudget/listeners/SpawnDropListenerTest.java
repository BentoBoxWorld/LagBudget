package world.bentobox.lagbudget.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
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

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.lagbudget.LagBudget;
import world.bentobox.lagbudget.Settings;
import world.bentobox.lagbudget.sampler.BudgetSampler;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpawnDropListenerTest {

    private static final String ISLAND_ID = "island-1";

    @Mock
    private LagBudget addon;
    @Mock
    private Settings settings;
    @Mock
    private BudgetSampler sampler;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private World world;
    @Mock
    private Location location;
    @Mock
    private LivingEntity entity;

    private SpawnDropListener listener;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getSampler()).thenReturn(sampler);
        when(addon.getIslands()).thenReturn(im);
        when(addon.inGameModeWorld(world)).thenReturn(true);
        when(settings.getDropSpawnReasons()).thenReturn(EnumSet.of(SpawnReason.NATURAL, SpawnReason.SPAWNER, SpawnReason.BREEDING));
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));
        when(island.getUniqueId()).thenReturn(ISLAND_ID);
        when(sampler.isOverBudget(ISLAND_ID)).thenReturn(true);
        listener = new SpawnDropListener(addon);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private CreatureSpawnEvent spawn(SpawnReason reason) {
        return new CreatureSpawnEvent(entity, reason);
    }

    @Test
    void testNaturalSpawnDroppedOnOverBudgetIsland() {
        CreatureSpawnEvent e = spawn(SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testSpawnerAndBreedingDropped() {
        CreatureSpawnEvent s = spawn(SpawnReason.SPAWNER);
        CreatureSpawnEvent b = spawn(SpawnReason.BREEDING);
        listener.onCreatureSpawn(s);
        listener.onCreatureSpawn(b);
        assertTrue(s.isCancelled());
        assertTrue(b.isCancelled());
    }

    @Test
    void testUnlistedReasonNotDropped() {
        CreatureSpawnEvent e = spawn(SpawnReason.SPAWNER_EGG);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
        verify(im, never()).getIslandAt(any());
    }

    @Test
    void testNotDroppedUnderBudget() {
        when(sampler.isOverBudget(ISLAND_ID)).thenReturn(false);
        CreatureSpawnEvent e = spawn(SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNotDroppedOutsideGameModeWorld() {
        when(addon.inGameModeWorld(world)).thenReturn(false);
        CreatureSpawnEvent e = spawn(SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
        verify(im, never()).getIslandAt(any());
    }

    @Test
    void testNotDroppedWhenNoIsland() {
        when(im.getIslandAt(location)).thenReturn(Optional.empty());
        CreatureSpawnEvent e = spawn(SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNullWorldIgnored() {
        when(location.getWorld()).thenReturn(null);
        CreatureSpawnEvent e = spawn(SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testSilent() {
        // Dropping must never talk to anyone: no user lookups, no player messages
        LagBudget spy = addon;
        listener.onCreatureSpawn(spawn(SpawnReason.NATURAL));
        verify(spy, never()).getPlayers();
        verify(spy, never()).log(any());
        verify(entity, never()).sendMessage(any(String.class));
    }

    @Test
    void testEmptyReasonListDropsNothing() {
        when(settings.getDropSpawnReasons()).thenReturn(EnumSet.noneOf(SpawnReason.class));
        CreatureSpawnEvent e = spawn(SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }
}
