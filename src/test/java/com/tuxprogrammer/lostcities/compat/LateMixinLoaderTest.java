package com.tuxprogrammer.lostcities.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/** The late mixins apply only with their mods, and their config names the package the mixins live in. */
class LateMixinLoaderTest {

    @Test
    void roguelikeSteeringOnlyWithRoguelike() {
        assertEquals(
            Collections.emptyList(),
            LateMixinLoader.mixinsFor(new HashSet<>(Arrays.asList("NotEnoughItems"))));
        assertEquals(
            Collections.singletonList("MixinRoguelikeDungeon"),
            LateMixinLoader.mixinsFor(new HashSet<>(Arrays.asList("Roguelike", "NotEnoughItems"))));
    }

    @Test
    void stoneBoulderGuardOnlyWithGregTech() {
        assertEquals(
            Collections.singletonList("MixinGtWorldgenStone"),
            LateMixinLoader.mixinsFor(new HashSet<>(Arrays.asList("gregtech"))));
    }

    @Test
    void witcheryVetoOnlyWithWitchery() {
        assertEquals(
            Collections.singletonList("MixinWitcheryWorldGenerator"),
            LateMixinLoader.mixinsFor(new HashSet<>(Arrays.asList("witchery"))));
    }

    @Test
    void configNamesTheLatePackage() throws IOException {
        try (InputStream in = LateMixinLoader.class.getResourceAsStream("/" + LateMixinLoader.CONFIG)) {
            assertTrue(in != null, "the late mixin config is a resource");
            String text = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")
                .next();
            assertTrue(text.contains("\"package\": \"com.tuxprogrammer.lostcities.mixins.late\""), text);
        }
        assertTrue(
            LateMixinLoader.class.getClassLoader()
                .getResource("com/tuxprogrammer/lostcities/mixins/late/MixinRoguelikeDungeon.class") != null);
    }
}
