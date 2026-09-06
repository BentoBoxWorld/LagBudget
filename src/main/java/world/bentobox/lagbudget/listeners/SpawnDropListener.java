package world.bentobox.lagbudget.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import world.bentobox.lagbudget.LagBudget;

/**
 * Silently cancels configured spawn reasons on islands that are over their lag budget.
 * No message is sent to anyone; the admin sees the island in {@code lagbudget top}.
 *
 * @author tastybento
 */
public class SpawnDropListener implements Listener {

    private final LagBudget addon;

    public SpawnDropListener(LagBudget addon) {
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!addon.getSettings().getDropSpawnReasons().contains(e.getSpawnReason())) {
            return;
        }
        Location loc = e.getLocation();
        if (loc.getWorld() == null || !addon.inGameModeWorld(loc.getWorld())) {
            return;
        }
        addon.getIslands().getIslandAt(loc)
                .filter(island -> addon.getSampler().isOverBudget(island.getUniqueId()))
                .ifPresent(island -> e.setCancelled(true));
    }
}
