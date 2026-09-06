package world.bentobox.lagbudget;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;

/**
 * Plugin wrapper so the addon can be dropped into the plugins folder.
 */
public class LagBudgetPladdon extends Pladdon {

    private Addon addon;

    @Override
    public Addon getAddon() {
        if (addon == null) {
            addon = new LagBudget();
        }
        return addon;
    }
}
