package world.bentobox.lagbudget.commands;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.lagbudget.LagBudget;
import world.bentobox.lagbudget.Settings;
import world.bentobox.lagbudget.sampler.BudgetSampler;

/**
 * Shared scaffolding for command tests: a mocked BentoBox, a mocked game-mode admin
 * command as parent, and a mocked sampler so results can be dictated.
 */
abstract class CommandTestBase {

    protected static final String ISLAND_ONE = "island-1";
    protected static final String ISLAND_TWO = "island-2";

    @Mock
    protected LagBudget addon;
    @Mock
    protected Settings settings;
    @Mock
    protected BudgetSampler sampler;
    @Mock
    protected BentoBox plugin;
    @Mock
    protected IslandsManager im;
    @Mock
    protected PlayersManager pm;
    @Mock
    protected GameModeAddon gameMode;
    @Mock
    protected CompositeCommand parent;
    @Mock
    protected World world;
    @Mock
    protected Island island;
    @Mock
    protected Island island2;
    @Mock
    protected User user;

    protected UUID owner;
    protected UUID owner2;
    private MockedStatic<BentoBox> mockedBentoBox;

    @BeforeEach
    void baseSetUp() {
        MockBukkit.mock();
        mockedBentoBox = Mockito.mockStatic(BentoBox.class);
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(plugin);
        when(plugin.getCommandsManager()).thenReturn(mock(CommandsManager.class));
        when(plugin.getIslands()).thenReturn(im);
        when(plugin.getPlayers()).thenReturn(pm);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(world.getName()).thenReturn("bskyblock_world");

        when(addon.getSettings()).thenReturn(settings);
        when(addon.getSampler()).thenReturn(sampler);
        when(addon.getIslands()).thenReturn(im);
        when(settings.getBudget()).thenReturn(500d);
        when(settings.getTopSize()).thenReturn(10);
        when(settings.getSampleInterval()).thenReturn(30);

        when(parent.getAddon()).thenReturn(gameMode);
        when(parent.getWorld()).thenReturn(world);
        when(parent.getPermissionPrefix()).thenReturn("bskyblock.");
        when(parent.getTopLabel()).thenReturn("bsbadmin");
        when(parent.getSubCommands()).thenReturn(new HashMap<>());
        when(parent.getSubCommandAliases()).thenReturn(new HashMap<>());

        owner = UUID.randomUUID();
        owner2 = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn(ISLAND_ONE);
        when(island.getOwner()).thenReturn(owner);
        when(island.getWorld()).thenReturn(world);
        when(island2.getUniqueId()).thenReturn(ISLAND_TWO);
        when(island2.getOwner()).thenReturn(owner2);
        when(island2.getWorld()).thenReturn(world);
        when(im.getIslandById(ISLAND_ONE)).thenReturn(java.util.Optional.of(island));
        when(im.getIslandById(ISLAND_TWO)).thenReturn(java.util.Optional.of(island2));
        when(pm.getName(owner)).thenReturn("tastybento");
        when(pm.getName(owner2)).thenReturn("poslovitch");
        when(user.getTranslation("lagbudget.admin.top.over")).thenReturn("*");
    }

    @AfterEach
    void baseTearDown() {
        if (mockedBentoBox != null) {
            mockedBentoBox.close();
        }
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }
}
