package world.bentobox.lagbudget.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.lagbudget.sampler.IslandScore;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminCommandTest extends CommandTestBase {

    private AdminCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new AdminCommand(addon, parent);
        IslandScore one = new IslandScore(ISLAND_ONE, owner);
        one.add("HOPPER", 3);
        when(sampler.hasSample()).thenReturn(true);
        when(sampler.getTop(anyInt())).thenReturn(List.of(one));
    }

    @Test
    void testSetup() {
        assertEquals("bskyblock.lagbudget.admin", cmd.getPermission());
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("lagbudget.admin.main.description", cmd.getDescription());
        assertTrue(parent.getSubCommands().containsKey("lagbudget"));
        assertTrue(cmd.getSubCommands().keySet().containsAll(List.of("top", "info", "reload", "help")));
    }

    @Test
    void testBareCommandShowsRanking() {
        assertTrue(cmd.execute(user, "lagbudget", List.of()));
        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "1", "[budget]", "500");
        verify(user).sendMessage("lagbudget.admin.top.line", "[rank]", "1", "[name]", "tastybento", "[score]", "3",
                "[budget]", "500", "[top]", "HOPPER 3");
    }

    @Test
    void testBareCommandBeforeFirstSample() {
        when(sampler.hasSample()).thenReturn(false);
        assertTrue(cmd.execute(user, "lagbudget", List.of()));
        verify(user).sendMessage("lagbudget.admin.top.none", "[interval]", "30");
    }

    @Test
    void testUnknownArgumentShowsHelpNotRanking() {
        assertFalse(cmd.execute(user, "lagbudget", List.of("bogus")));
        verify(user, never()).sendMessage(eq("lagbudget.admin.top.header"), any(), any(), any(), any());
    }
}
