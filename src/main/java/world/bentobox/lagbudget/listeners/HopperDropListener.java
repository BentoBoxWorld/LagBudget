package world.bentobox.lagbudget.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

import world.bentobox.lagbudget.LagBudget;

/**
 * Optional: silently cancels hopper-initiated item transfers on islands that are over
 * their lag budget. Only registered when {@code drop.hopper-transfers} is true, because
 * merely listening to {@link InventoryMoveItemEvent} has a cost on every hopper on the
 * server.
 *
 * @author tastybento
 */
public class HopperDropListener implements Listener {

    private final LagBudget addon;

    public HopperDropListener(LagBudget addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        // The initiator is the hopper (block or minecart) doing the moving.
        Location loc = e.getInitiator().getLocation();
        if (loc == null || loc.getWorld() == null || !addon.inGameModeWorld(loc.getWorld())) {
            return;
        }
        addon.getIslands().getIslandAt(loc)
                .filter(island -> addon.getSampler().isOverBudget(island.getUniqueId()))
                .ifPresent(island -> e.setCancelled(true));
    }
}
