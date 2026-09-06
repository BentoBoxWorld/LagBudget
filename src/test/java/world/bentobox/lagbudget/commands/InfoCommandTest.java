package world.bentobox.lagbudget.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.lagbudget.sampler.IslandScore;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InfoCommandTest extends CommandTestBase {

    private InfoCommand cmd;
    private IslandScore score;

    @BeforeEach
    void setUp() {
        cmd = new InfoCommand(addon, parent);
        score = new IslandScore(ISLAND_ONE, owner);
        score.add("HOPPER", 3);
        score.add("HOPPER", 3);
        score.add("VILLAGER", 10);
        when(pm.getUUID("tastybento")).thenReturn(owner);
        when(im.getIsland(world, owner)).thenReturn(island);
        when(sampler.getScore(island)).thenReturn(Optional.of(score));
    }

    @Test
    void testSetup() {
        assertEquals("bskyblock.lagbudget.admin", cmd.getPermission());
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("lagbudget.admin.info.description", cmd.getDescription());
        assertEquals("lagbudget.admin.info.parameters", cmd.getParameters());
        assertTrue(parent.getSubCommands().containsKey("info"));
    }

    @Test
    void testBreakdown() {
        assertTrue(cmd.execute(user, "info", List.of("tastybento")));
        verify(user).sendMessage("lagbudget.admin.info.header", "[name]", "tastybento", "[score]", "16", "[budget]", "500");
        verify(user, never()).sendMessage("lagbudget.admin.info.over");
        verify(user).sendMessage("lagbudget.admin.info.line", "[key]", "VILLAGER", "[count]", "1", "[score]", "10");
        verify(user).sendMessage("lagbudget.admin.info.line", "[key]", "HOPPER", "[count]", "2", "[score]", "6");
    }

    @Test
    void testOverBudgetNotice() {
        when(sampler.isOverBudget(ISLAND_ONE)).thenReturn(true);
        cmd.execute(user, "info", List.of("tastybento"));
        verify(user).sendMessage("lagbudget.admin.info.over");
    }

    @Test
    void testNoSampleForIsland() {
        when(sampler.getScore(island)).thenReturn(Optional.empty());
        assertTrue(cmd.execute(user, "info", List.of("tastybento")));
        verify(user).sendMessage("lagbudget.admin.info.none");
        verify(user, never()).sendMessage(eq("lagbudget.admin.info.header"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUnknownPlayer() {
        assertFalse(cmd.execute(user, "info", List.of("nobody")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "nobody");
    }

    @Test
    void testPlayerWithoutIsland() {
        when(im.getIsland(world, owner)).thenReturn(null);
        assertFalse(cmd.execute(user, "info", List.of("tastybento")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    @Test
    void testWrongArgumentCountShowsHelp() {
        assertFalse(cmd.execute(user, "info", List.of()));
        assertFalse(cmd.execute(user, "info", List.of("a", "b")));
        verify(user, never()).sendMessage(eq("lagbudget.admin.info.header"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testTabComplete() {
        assertTrue(cmd.tabComplete(user, "info", List.of()).isEmpty());
        Optional<List<String>> completions = cmd.tabComplete(user, "info", List.of("ta"));
        assertTrue(completions.isPresent());
    }
}
