package world.bentobox.lagbudget.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;
import world.bentobox.lagbudget.LagBudget;
import world.bentobox.lagbudget.sampler.IslandScore;

/**
 * {@code /<admin> lagbudget info <player>}: full score breakdown for a player's island.
 *
 * @author tastybento
 */
public class InfoCommand extends CompositeCommand {

    private final LagBudget addon;

    public InfoCommand(LagBudget addon, CompositeCommand parent) {
        super(parent, "info");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("lagbudget.admin");
        setOnlyPlayer(false);
        setParametersHelp("lagbudget.admin.info.parameters");
        setDescription("lagbudget.admin.info.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        UUID target = getPlayers().getUUID(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", "[name]", args.get(0));
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), target);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        Optional<IslandScore> score = addon.getSampler().getScore(island);
        if (score.isEmpty()) {
            user.sendMessage("lagbudget.admin.info.none");
            return true;
        }
        IslandScore sc = score.get();
        user.sendMessage("lagbudget.admin.info.header",
                "[name]", args.get(0),
                "[score]", IslandScore.format(sc.getTotal()),
                "[budget]", IslandScore.format(addon.getSettings().getBudget()));
        if (addon.getSampler().isOverBudget(island.getUniqueId())) {
            user.sendMessage("lagbudget.admin.info.over");
        }
        for (Map.Entry<String, Double> e : sc.topContributors(Integer.MAX_VALUE)) {
            user.sendMessage("lagbudget.admin.info.line",
                    "[key]", e.getKey(),
                    "[count]", String.valueOf(sc.getCount(e.getKey())),
                    "[score]", IslandScore.format(e.getValue()));
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.isEmpty()) {
            return Optional.empty();
        }
        String lastArg = args.get(args.size() - 1);
        return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), lastArg));
    }
}
