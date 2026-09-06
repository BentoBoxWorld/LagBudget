package world.bentobox.lagbudget.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
import world.bentobox.lagbudget.sampler.BudgetSampler;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HopperDropListenerTest {

    private static final String ISLAND_ID = "island-1";

    @Mock
    private LagBudget addon;
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
    private Inventory hopper;
    @Mock
    private Inventory chest;

    private HopperDropListener listener;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        when(addon.getSampler()).thenReturn(sampler);
        when(addon.getIslands()).thenReturn(im);
        when(addon.inGameModeWorld(world)).thenReturn(true);
        when(location.getWorld()).thenReturn(world);
        when(hopper.getLocation()).thenReturn(location);
        when(im.getIslandAt(location)).thenReturn(Optional.of(island));
        when(island.getUniqueId()).thenReturn(ISLAND_ID);
        when(sampler.isOverBudget(ISLAND_ID)).thenReturn(true);
        listener = new HopperDropListener(addon);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** A hopper pulling from a chest: the hopper is the destination and the initiator. */
    private InventoryMoveItemEvent hopperPull() {
        return new InventoryMoveItemEvent(chest, mock(ItemStack.class), hopper, false);
    }

    /** A hopper pushing into a chest: the hopper is the source and the initiator. */
    private InventoryMoveItemEvent hopperPush() {
        return new InventoryMoveItemEvent(hopper, mock(ItemStack.class), chest, true);
    }

    @Test
    void testTransferDroppedOnOverBudgetIsland() {
        InventoryMoveItemEvent pull = hopperPull();
        InventoryMoveItemEvent push = hopperPush();
        listener.onInventoryMove(pull);
        listener.onInventoryMove(push);
        assertTrue(pull.isCancelled());
        assertTrue(push.isCancelled());
    }

    @Test
    void testNotDroppedUnderBudget() {
        when(sampler.isOverBudget(ISLAND_ID)).thenReturn(false);
        InventoryMoveItemEvent e = hopperPull();
        listener.onInventoryMove(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNotDroppedOutsideGameModeWorld() {
        when(addon.inGameModeWorld(world)).thenReturn(false);
        InventoryMoveItemEvent e = hopperPull();
        listener.onInventoryMove(e);
        assertFalse(e.isCancelled());
        verify(im, never()).getIslandAt(any());
    }

    @Test
    void testNullLocationIgnored() {
        when(hopper.getLocation()).thenReturn(null);
        InventoryMoveItemEvent e = hopperPull();
        listener.onInventoryMove(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNoIslandIgnored() {
        when(im.getIslandAt(location)).thenReturn(Optional.empty());
        InventoryMoveItemEvent e = hopperPull();
        listener.onInventoryMove(e);
        assertFalse(e.isCancelled());
    }
}
