package world.bentobox.lagbudget.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.lagbudget.LagBudget;

/**
 * {@code /<admin> lagbudget reload}
 *
 * @author tastybento
 */
public class ReloadCommand extends CompositeCommand {

    private final LagBudget addon;

    public ReloadCommand(LagBudget addon, CompositeCommand parent) {
        super(parent, "reload");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("lagbudget.admin");
        setOnlyPlayer(false);
        setDescription("lagbudget.admin.reload.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        addon.reloadSettings();
        user.sendMessage("lagbudget.admin.reload.done");
        return true;
    }
}
