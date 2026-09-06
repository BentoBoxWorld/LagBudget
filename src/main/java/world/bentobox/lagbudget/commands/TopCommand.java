package world.bentobox.lagbudget.commands;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.lagbudget.LagBudget;
import world.bentobox.lagbudget.sampler.BudgetSampler;
import world.bentobox.lagbudget.sampler.IslandScore;

/**
 * {@code /<admin> lagbudget top [count]}: islands of this game mode ranked by lag score.
 *
 * @author tastybento
 */
public class TopCommand extends CompositeCommand {

    private static final String BUDGET = "[budget]";
    private final LagBudget addon;

    public TopCommand(LagBudget addon, CompositeCommand parent) {
        super(parent, "top");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("lagbudget.admin");
        setOnlyPlayer(false);
        setParametersHelp("lagbudget.admin.top.parameters");
        setDescription("lagbudget.admin.top.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        int count = addon.getSettings().getTopSize();
        if (!args.isEmpty()) {
            try {
                count = Integer.parseInt(args.get(0));
            } catch (NumberFormatException e) {
                count = 0;
            }
            if (count < 1) {
                user.sendMessage("general.errors.must-be-positive-number", "[number]", args.get(0));
                return false;
            }
        }
        BudgetSampler sampler = addon.getSampler();
        if (!sampler.hasSample()) {
            user.sendMessage("lagbudget.admin.top.none", "[interval]",
                    String.valueOf(addon.getSettings().getSampleInterval()));
            return true;
        }
        String budget = IslandScore.format(addon.getSettings().getBudget());
        List<IslandScore> top = sampler.getTop(Integer.MAX_VALUE).stream()
                .filter(this::inThisGameMode)
                .limit(count)
                .toList();
        user.sendMessage("lagbudget.admin.top.header", "[count]", String.valueOf(top.size()), BUDGET, budget);
        int rank = 1;
        for (IslandScore sc : top) {
            String over = sampler.isOverBudget(sc.getIslandId()) ? user.getTranslation("lagbudget.admin.top.over") : "";
            user.sendMessage("lagbudget.admin.top.line",
                    "[rank]", String.valueOf(rank++),
                    "[name]", ownerName(sc) + over,
                    "[score]", IslandScore.format(sc.getTotal()),
                    BUDGET, budget,
                    "[top]", sc.summary(3));
        }
        return true;
    }

    private boolean inThisGameMode(IslandScore sc) {
        return addon.getIslands().getIslandById(sc.getIslandId())
                .map(island -> Util.sameWorld(island.getWorld(), getWorld()))
                .orElse(false);
    }

    private String ownerName(IslandScore sc) {
        UUID owner = sc.getOwner();
        String name = owner == null ? "" : getPlayers().getName(owner);
        return name == null || name.isEmpty() ? sc.getIslandId() : name;
    }
}
