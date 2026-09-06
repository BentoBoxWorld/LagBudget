package world.bentobox.lagbudget.sampler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class IslandScoreTest {

    @Test
    void testAddAccumulates() {
        UUID owner = UUID.randomUUID();
        IslandScore sc = new IslandScore("id", owner);
        assertEquals("id", sc.getIslandId());
        assertEquals(owner, sc.getOwner());
        assertEquals(0, sc.getTotal());

        sc.add("HOPPER", 3);
        sc.add("HOPPER", 3);
        sc.add("VILLAGER", 10);

        assertEquals(16, sc.getTotal());
        assertEquals(6, sc.getScores().get("HOPPER"));
        assertEquals(2, sc.getCount("HOPPER"));
        assertEquals(1, sc.getCount("VILLAGER"));
        assertEquals(0, sc.getCount("NOTHING"));
    }

    @Test
    void testNullOwnerAllowed() {
        IslandScore sc = new IslandScore("id", null);
        assertNull(sc.getOwner());
    }

    @Test
    void testTopContributorsOrderedAndLimited() {
        IslandScore sc = new IslandScore("id", null);
        sc.add("HOPPER", 3);
        sc.add("VILLAGER", 10);
        sc.add("ITEM", 1);
        sc.add("ITEM", 1);

        List<Map.Entry<String, Double>> top = sc.topContributors(2);
        assertEquals(2, top.size());
        assertEquals("VILLAGER", top.get(0).getKey());
        assertEquals("HOPPER", top.get(1).getKey());
        assertEquals(3, sc.topContributors(10).size());
    }

    @Test
    void testTopContributorsTieBreaksByKey() {
        IslandScore sc = new IslandScore("id", null);
        sc.add("ZOMBIE", 2);
        sc.add("CHICKEN", 2);
        List<Map.Entry<String, Double>> top = sc.topContributors(2);
        assertEquals("CHICKEN", top.get(0).getKey());
        assertEquals("ZOMBIE", top.get(1).getKey());
    }

    @Test
    void testSummary() {
        IslandScore sc = new IslandScore("id", null);
        sc.add("HOPPER", 3);
        sc.add("HOPPER", 3);
        sc.add("VILLAGER", 10);
        assertEquals("VILLAGER 10, HOPPER 6", sc.summary(2));
        assertEquals("VILLAGER 10", sc.summary(1));
        assertTrue(new IslandScore("x", null).summary(3).isEmpty());
    }

    @Test
    void testFormat() {
        assertEquals("0", IslandScore.format(0));
        assertEquals("500", IslandScore.format(500));
        assertEquals("13", IslandScore.format(12.6));
        assertEquals("1234", IslandScore.format(1234.4));
    }
}
