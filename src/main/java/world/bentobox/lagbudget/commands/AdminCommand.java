package world.bentobox.lagbudget.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.lagbudget.LagBudget;

/**
 * {@code /<admin> lagbudget} with {@code top}, {@code info} and {@code reload} sub-commands.
 * On its own it behaves like {@code top}, so the ranking is one command away.
 *
 * @author tastybento
 */
public class AdminCommand extends CompositeCommand {

    private final LagBudget addon;
    private final TopCommand top;

    public AdminCommand(LagBudget addon, CompositeCommand parent) {
        super(parent, "lagbudget");
        this.addon = addon;
        top = new TopCommand(addon, this);
        new InfoCommand(addon, this);
        new ReloadCommand(addon, this);
    }

    @Override
    public void setup() {
        setPermission("lagbudget.admin");
        setOnlyPlayer(false);
        setParametersHelp("lagbudget.admin.main.parameters");
        setDescription("lagbudget.admin.main.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty()) {
            // Bare command: show the ranking, the thing an admin most often wants
            return top.execute(user, label, args);
        }
        showHelp(this, user);
        return false;
    }

    LagBudget getLagBudget() {
        return addon;
    }
}
