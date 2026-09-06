package world.bentobox.lagbudget;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.bentobox.api.addons.Addon;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LagBudgetPladdonTest {

    private LagBudgetPladdon pladdon;

    @BeforeEach
    void setUp() {
        // The BentoBox Addon constructor initialises Util, which needs a live server.
        MockBukkit.mock();
        // Pladdon extends JavaPlugin, which needs a PluginClassLoader; CALLS_REAL_METHODS skips the constructor.
        pladdon = mock(LagBudgetPladdon.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void testGetAddonReturnsLagBudget() {
        Addon addon = pladdon.getAddon();
        assertNotNull(addon);
        assertInstanceOf(LagBudget.class, addon);
    }

    @Test
    void testGetAddonReturnsSameInstance() {
        assertSame(pladdon.getAddon(), pladdon.getAddon());
    }
}
