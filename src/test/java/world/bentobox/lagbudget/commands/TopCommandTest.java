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
import java.util.Optional;

import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.lagbudget.sampler.IslandScore;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TopCommandTest extends CommandTestBase {

    @Mock
    private World otherWorld;
    @Mock
    private Island otherIsland;

    private TopCommand cmd;
    private IslandScore one;
    private IslandScore two;

    @BeforeEach
    void setUp() {
        cmd = new TopCommand(addon, parent);
        one = new IslandScore(ISLAND_ONE, owner);
        one.add("HOPPER", 3);
        one.add("HOPPER", 3);
        two = new IslandScore(ISLAND_TWO, owner2);
        two.add("VILLAGER", 10);
        two.add("HOPPER", 3);
        when(sampler.hasSample()).thenReturn(true);
        when(sampler.getTop(anyInt())).thenReturn(List.of(two, one));
    }

    @Test
    void testSetup() {
        assertEquals("bskyblock.lagbudget.admin", cmd.getPermission());
        assertFalse(cmd.isOnlyPlayer());
        assertEquals("lagbudget.admin.top.description", cmd.getDescription());
        assertEquals("lagbudget.admin.top.parameters", cmd.getParameters());
        assertTrue(parent.getSubCommands().containsKey("top"));
    }

    @Test
    void testNoSampleYet() {
        when(sampler.hasSample()).thenReturn(false);
        assertTrue(cmd.execute(user, "top", List.of()));
        verify(user).sendMessage("lagbudget.admin.top.none", "[interval]", "30");
        verify(sampler, never()).getTop(anyInt());
    }

    @Test
    void testRankedOutput() {
        assertTrue(cmd.execute(user, "top", List.of()));
        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "2", "[budget]", "500");
        verify(user).sendMessage("lagbudget.admin.top.line", "[rank]", "1", "[name]", "poslovitch", "[score]", "13",
                "[budget]", "500", "[top]", "VILLAGER 10, HOPPER 3");
        verify(user).sendMessage("lagbudget.admin.top.line", "[rank]", "2", "[name]", "tastybento", "[score]", "6",
                "[budget]", "500", "[top]", "HOPPER 6");
    }

    @Test
    void testOverBudgetMarked() {
        when(sampler.isOverBudget(ISLAND_TWO)).thenReturn(true);
        cmd.execute(user, "top", List.of());
        verify(user).sendMessage(eq("lagbudget.admin.top.line"), eq("[rank]"), eq("1"), eq("[name]"), eq("poslovitch*"),
                any(), any(), any(), any(), any(), any());
        verify(user).sendMessage(eq("lagbudget.admin.top.line"), eq("[rank]"), eq("2"), eq("[name]"), eq("tastybento"),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCountArgumentLimitsList() {
        assertTrue(cmd.execute(user, "top", List.of("1")));
        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "1", "[budget]", "500");
        verify(user).sendMessage(eq("lagbudget.admin.top.line"), eq("[rank]"), eq("1"), eq("[name]"), eq("poslovitch"),
                any(), any(), any(), any(), any(), any());
        verify(user, never()).sendMessage(eq("lagbudget.admin.top.line"), eq("[rank]"), eq("2"), any(), any(), any(), any(),
                any(), any(), any(), any());
    }

    @Test
    void testDefaultCountFromConfig() {
        when(settings.getTopSize()).thenReturn(1);
        assertTrue(cmd.execute(user, "top", List.of()));
        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "1", "[budget]", "500");
    }

    @Test
    void testBadCountArgument() {
        assertFalse(cmd.execute(user, "top", List.of("lots")));
        verify(user).sendMessage("general.errors.must-be-positive-number", "[number]", "lots");
        assertFalse(cmd.execute(user, "top", List.of("0")));
        verify(user).sendMessage("general.errors.must-be-positive-number", "[number]", "0");
        verify(user, never()).sendMessage(eq("lagbudget.admin.top.header"), any(), any(), any(), any());
    }

    @Test
    void testOtherGameModeIslandsFiltered() {
        IslandScore other = new IslandScore("other", owner);
        other.add("VILLAGER", 100);
        when(otherWorld.getName()).thenReturn("other_world");
        when(otherIsland.getWorld()).thenReturn(otherWorld);
        when(im.getIslandById("other")).thenReturn(Optional.of(otherIsland));
        when(sampler.getTop(anyInt())).thenReturn(List.of(other, two, one));

        cmd.execute(user, "top", List.of());

        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "2", "[budget]", "500");
        verify(user).sendMessage(eq("lagbudget.admin.top.line"), eq("[rank]"), eq("1"), eq("[name]"), eq("poslovitch"),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void testDeletedIslandSkipped() {
        when(im.getIslandById(ISLAND_TWO)).thenReturn(Optional.empty());
        cmd.execute(user, "top", List.of());
        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "1", "[budget]", "500");
    }

    @Test
    void testOwnerlessIslandNamedById() {
        when(island2.getOwner()).thenReturn(null);
        two = new IslandScore(ISLAND_TWO, null);
        two.add("VILLAGER", 10);
        when(sampler.getTop(anyInt())).thenReturn(List.of(two));
        cmd.execute(user, "top", List.of());
        verify(user).sendMessage(eq("lagbudget.admin.top.line"), eq("[rank]"), eq("1"), eq("[name]"), eq(ISLAND_TWO),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void testEmptySampleListsNothing() {
        when(sampler.getTop(anyInt())).thenReturn(List.of());
        assertTrue(cmd.execute(user, "top", List.of()));
        verify(user).sendMessage("lagbudget.admin.top.header", "[count]", "0", "[budget]", "500");
    }
}
