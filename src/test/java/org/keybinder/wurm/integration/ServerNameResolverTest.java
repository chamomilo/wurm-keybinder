package org.keybinder.wurm.integration;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ServerNameResolverTest {
    private final ServerNameResolver resolver = new ServerNameResolver();

    @Test public void expandsShardNameFromBrowserEntry() {
        assertEquals("Sklotopolis - Novus", resolver.resolve("Novus",
                Arrays.asList("Other server", "Sklotopolis - Novus")));
    }

    @Test public void keepsShortNameWhenNoMatchExists() {
        assertEquals("Novus", resolver.resolve("Novus", Arrays.asList("Another cluster - Prime")));
    }

    @Test public void refusesAmbiguousClusterMatches() {
        assertEquals("Novus", resolver.resolve("Novus",
                Arrays.asList("Cluster A - Novus", "Cluster B - Novus")));
    }
}
