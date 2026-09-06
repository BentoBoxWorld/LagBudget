package world.bentobox.lagbudget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.bukkit.World;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.lagbudget.commands.AdminCommand;
import world.bentobox.lagbudget.listeners.HopperDropListener;
import world.bentobox.lagbudget.listeners.SpawnDropListener;
import world.bentobox.lagbudget.sampler.BudgetSampler;
import world.bentobox.lagbudget.sampler.IslandScore;

/**
 * BentoBox addon that gives every island a lag budget and quietly drops spawns on
 * islands that exceed it. Admin-facing: nothing is shown to players unless the
 * placeholders are switched on.
 *
 * @author tastybento
 */
public class LagBudget extends Addon {

    private Settings settings;
    private List<GameModeAddon> gameModes = new ArrayList<>();
    private BudgetSampler sampler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(this);
        gameModes = getPlugin().getAddonsManager().getGameModeAddons().stream()
                .filter(gm -> settings.getGameModes().contains(gm.getDescription().getName()))
                .toList();
        if (gameModes.isEmpty()) {
            logWarning("No active game modes match the gamemodes list in config.yml; LagBudget will do nothing.");
        }
        sampler = new BudgetSampler(this);
        gameModes.forEach(gm -> {
            gm.getAdminCommand().ifPresent(a -> new AdminCommand(this, a));
            if (settings.isPlaceholders()) {
                registerPlaceholders(gm);
            }
            log("LagBudget will apply to " + gm.getDescription().getName());
        });
        registerListener(new SpawnDropListener(this));
        if (settings.isDropHopperTransfers()) {
            registerListener(new HopperDropListener(this));
            log("Hopper transfer dropping is enabled. This listens to every hopper transfer on the server.");
        }
        sampler.start();
    }

    @Override
    public void onDisable() {
        if (sampler != null) {
            sampler.stop();
        }
    }

    @Override
    public void onReload() {
        reloadSettings();
    }

    /**
     * Re-read config.yml and restart the sampler. Listener and placeholder registration
     * is not repeated, so changing {@code drop.hopper-transfers} or {@code placeholders}
     * needs a server restart.
     */
    public void reloadSettings() {
        reloadConfig();
        boolean hopperBefore = settings != null && settings.isDropHopperTransfers();
        boolean placeholdersBefore = settings != null && settings.isPlaceholders();
        settings = new Settings(this);
        if (settings.isDropHopperTransfers() != hopperBefore || settings.isPlaceholders() != placeholdersBefore) {
            logWarning("drop.hopper-transfers and placeholders changes take effect after a restart.");
        }
        if (sampler != null) {
            sampler.start();
        }
        log("LagBudget config reloaded.");
    }

    private void registerPlaceholders(GameModeAddon gm) {
        String prefix = gm.getDescription().getName().toLowerCase(Locale.ENGLISH) + "_island_lag_";
        PlaceholdersManager pm = getPlugin().getPlaceholdersManager();
        pm.registerPlaceholder(this, prefix + "score", user -> islandOf(gm, user)
                .flatMap(sampler::getScore)
                .map(sc -> IslandScore.format(sc.getTotal()))
                .orElse("0"));
        pm.registerPlaceholder(this, prefix + "budget", user -> IslandScore.format(settings.getBudget()));
        pm.registerPlaceholder(this, prefix + "over_budget", user -> islandOf(gm, user)
                .map(island -> String.valueOf(sampler.isOverBudget(island.getUniqueId())))
                .orElse("false"));
    }

    private Optional<Island> islandOf(GameModeAddon gm, User user) {
        if (user == null || user.getUniqueId() == null || gm.getOverWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getIslands().getIsland(gm.getOverWorld(), user.getUniqueId()));
    }

    public Settings getSettings() {
        return settings;
    }

    public List<GameModeAddon> getGameModes() {
        return gameModes;
    }

    public BudgetSampler getSampler() {
        return sampler;
    }

    /**
     * @param world world
     * @return true if the world belongs to a game mode this addon applies to
     */
    public boolean inGameModeWorld(World world) {
        return gameModes.stream().anyMatch(gm -> gm.inWorld(world));
    }
}
