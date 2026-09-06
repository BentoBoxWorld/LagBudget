package world.bentobox.lagbudget;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.placeholders.PlaceholderReplacer;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.lagbudget.listeners.HopperDropListener;
import world.bentobox.lagbudget.listeners.SpawnDropListener;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LagBudgetTest {

    private static File jFile;
    @Mock
    private BentoBox plugin;
    @Mock
    private AddonsManager am;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private CompositeCommand adminCmd;
    @Mock
    private PlaceholdersManager phm;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private World world;
    @Mock
    private Island island;
    @Mock
    private User user;

    private LagBudget addon;
    private MockedStatic<BentoBox> mockedBentoBox;

    @BeforeAll
    static void beforeClass() throws Exception {
        cleanUp();
        jFile = new File("addon.jar");
        Path fromPath = Paths.get("src/main/resources/config.yml");
        Path path = Paths.get("config.yml");
        Files.copy(fromPath, path);
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jFile));
                FileInputStream fis = new FileInputStream(path.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            jar.putNextEntry(new JarEntry(path.toString()));
            while ((bytesRead = fis.read(buffer)) != -1) {
                jar.write(buffer, 0, bytesRead);
            }
        }
    }

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        mockedBentoBox = Mockito.mockStatic(BentoBox.class);
        mockedBentoBox.when(BentoBox::getInstance).thenReturn(plugin);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getCommandsManager()).thenReturn(mock(CommandsManager.class));
        when(plugin.getIWM()).thenReturn(mock(IslandWorldManager.class));
        when(plugin.getIslands()).thenReturn(im);
        when(plugin.getPlayers()).thenReturn(pm);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(plugin.getAddonsManager()).thenReturn(am);
        User.setPlugin(plugin);

        addon = new LagBudget();
        addon.setDataFolder(new File("addons/LagBudget"));
        addon.setFile(jFile);
        addon.setDescription(new AddonDescription.Builder("bentobox", "LagBudget", "1.0").description("test")
                .authors("tastybento").build());

        when(am.getGameModeAddons()).thenReturn(Collections.singletonList(gameMode));
        when(gameMode.getDescription()).thenReturn(
                new AddonDescription.Builder("bentobox", "BSkyBlock", "1.3").description("test").authors("tasty").build());
        when(gameMode.getOverWorld()).thenReturn(world);
        when(gameMode.getAdminCommand()).thenReturn(Optional.of(adminCmd));
        when(gameMode.getPermissionPrefix()).thenReturn("bskyblock.");
        when(adminCmd.getPermissionPrefix()).thenReturn("bskyblock.");
        when(adminCmd.getAddon()).thenReturn(gameMode);
        when(adminCmd.getWorld()).thenReturn(world);
        when(adminCmd.getSubCommands()).thenReturn(new HashMap<>());
        when(adminCmd.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(world.getName()).thenReturn("bskyblock_world");
        when(island.getUniqueId()).thenReturn("island-1");
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (addon != null) {
            addon.onDisable();
        }
        if (mockedBentoBox != null) {
            mockedBentoBox.close();
        }
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("addons"));
    }

    @AfterAll
    static void cleanUp() throws Exception {
        new File("addon.jar").delete();
        new File("config.yml").delete();
        deleteAll(new File("addons"));
    }

    private static void deleteAll(File file) throws IOException {
        if (file.exists()) {
            try (var walk = Files.walk(file.toPath())) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    /**
     * Enable with config overrides. saveDefaultConfig() will not overwrite an existing file, so
     * writing the config into the data folder first is enough for Settings to pick the values up.
     */
    private void enableWith(Map<String, Object> overrides) throws IOException {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        overrides.forEach(cfg::set);
        File dataFolder = addon.getDataFolder();
        assertTrue(dataFolder.mkdirs() || dataFolder.isDirectory());
        cfg.save(new File(dataFolder, "config.yml"));
        addon.onEnable();
    }

    @Test
    void testOnEnable() {
        assertNull(addon.getSettings());
        assertNull(addon.getSampler());
        assertTrue(addon.getGameModes().isEmpty());

        addon.onEnable();

        assertTrue(new File("addons/LagBudget/config.yml").exists(), "default config saved");
        assertNotNull(addon.getSettings());
        assertNotNull(addon.getSampler());
        assertEquals(List.of(gameMode), addon.getGameModes());
        assertEquals(1, MockBukkit.getMock().getScheduler().getPendingTasks().size(), "sampler task running");
        verify(am).registerListener(eq(addon), any(SpawnDropListener.class));
        verify(am, never()).registerListener(eq(addon), any(HopperDropListener.class));
        verify(phm, never()).registerPlaceholder(any(Addon.class), anyString(), any(PlaceholderReplacer.class));
    }

    @Test
    void testOnEnableRegistersAdminCommand() {
        addon.onEnable();
        assertTrue(adminCmd.getSubCommands().containsKey("lagbudget"));
        CompositeCommand lagbudget = adminCmd.getSubCommands().get("lagbudget");
        assertEquals("bskyblock.lagbudget.admin", lagbudget.getPermission());
        assertTrue(lagbudget.getSubCommands().keySet().containsAll(List.of("top", "info", "reload")));
    }

    @Test
    void testOnEnableNoMatchingGameModes() throws IOException {
        enableWith(Map.of("gamemodes", List.of("SomethingElse")));
        assertTrue(addon.getGameModes().isEmpty());
        assertFalse(adminCmd.getSubCommands().containsKey("lagbudget"));
        assertNotNull(addon.getSampler(), "sampler still exists so commands and listeners are safe");
    }

    @Test
    void testHopperListenerRegisteredWhenEnabled() throws IOException {
        enableWith(Map.of("drop.hopper-transfers", true));
        verify(am).registerListener(eq(addon), any(HopperDropListener.class));
    }

    @Test
    void testPlaceholdersRegisteredOnlyWhenEnabled() throws IOException {
        enableWith(Map.of("placeholders", true));
        ArgumentCaptor<String> names = ArgumentCaptor.forClass(String.class);
        verify(phm, Mockito.times(3)).registerPlaceholder(eq(addon), names.capture(), any(PlaceholderReplacer.class));
        assertTrue(names.getAllValues().containsAll(List.of("bskyblock_island_lag_score", "bskyblock_island_lag_budget",
                "bskyblock_island_lag_over_budget")));
    }

    @Test
    void testPlaceholderValues() throws IOException {
        enableWith(Map.of("placeholders", true));
        ArgumentCaptor<String> names = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PlaceholderReplacer> replacers = ArgumentCaptor.forClass(PlaceholderReplacer.class);
        verify(phm, Mockito.times(3)).registerPlaceholder(eq(addon), names.capture(), replacers.capture());
        Map<String, PlaceholderReplacer> byName = new HashMap<>();
        for (int i = 0; i < names.getAllValues().size(); i++) {
            byName.put(names.getAllValues().get(i), replacers.getAllValues().get(i));
        }
        assertEquals("500", byName.get("bskyblock_island_lag_budget").onReplace(user));

        // No island: safe defaults
        when(im.getIsland(world, user.getUniqueId())).thenReturn(null);
        assertEquals("0", byName.get("bskyblock_island_lag_score").onReplace(user));
        assertEquals("false", byName.get("bskyblock_island_lag_over_budget").onReplace(user));
        assertEquals("0", byName.get("bskyblock_island_lag_score").onReplace(null));

        // Island with no sample yet
        when(im.getIsland(world, user.getUniqueId())).thenReturn(island);
        assertEquals("0", byName.get("bskyblock_island_lag_score").onReplace(user));
        assertEquals("false", byName.get("bskyblock_island_lag_over_budget").onReplace(user));
    }

    @Test
    void testInGameModeWorld() {
        addon.onEnable();
        assertFalse(addon.inGameModeWorld(world));
        when(gameMode.inWorld(world)).thenReturn(true);
        assertTrue(addon.inGameModeWorld(world));
    }

    @Test
    void testOnDisableStopsSampler() {
        addon.onEnable();
        assertEquals(1, MockBukkit.getMock().getScheduler().getPendingTasks().size());
        addon.onDisable();
        assertEquals(0, MockBukkit.getMock().getScheduler().getPendingTasks().size());
    }

    @Test
    void testOnDisableBeforeEnableIsSafe() {
        assertDoesNotThrow(() -> addon.onDisable());
    }

    @Test
    void testReloadSettingsRereadsConfigAndRestartsSampler() throws IOException {
        addon.onEnable();
        assertEquals(500, addon.getSettings().getBudget());

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File("addons/LagBudget/config.yml"));
        cfg.set("budget", 750);
        cfg.save(new File("addons/LagBudget/config.yml"));

        addon.onReload();

        assertEquals(750, addon.getSettings().getBudget());
        assertEquals(1, MockBukkit.getMock().getScheduler().getPendingTasks().size(), "exactly one sampler task after restart");
    }

    @Test
    void testReloadWarnsAboutRestartOnlyOptions() throws IOException {
        addon.onEnable();
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File("addons/LagBudget/config.yml"));
        cfg.set("drop.hopper-transfers", true);
        cfg.save(new File("addons/LagBudget/config.yml"));

        addon.reloadSettings();

        assertTrue(addon.getSettings().isDropHopperTransfers());
        verify(am, never()).registerListener(eq(addon), any(HopperDropListener.class));
    }
}
