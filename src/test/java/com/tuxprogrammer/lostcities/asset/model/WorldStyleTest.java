package com.tuxprogrammer.lostcities.asset.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.AssetType;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.TestAssets;
import com.tuxprogrammer.lostcities.asset.match.BiomeDescriptor;

/** World styles (docs/original-spec/05-assets/world-styles.md). */
public class WorldStyleTest {

    private static final AssetSet BUNDLED = TestAssets.bundled(TestAssets.vanilla());

    /** A minimal valid world style's assets, with a placeholder for the world style's own fields. */
    private static AssetSet world(String fields) {
        String[] files = { "lostcities/palettes/p.json", "{\"palette\": []}", "lostcities/styles/st.json",
            "{\"randompalettes\": [[{\"factor\": 1, \"palette\": \"p\"}]]}", "lostcities/citystyles/c.json",
            "{\"style\": \"st\"}", "lostcities/worldstyles/w.json", "{\"outsidestyle\": \"st\", " + fields + "}" };
        return TestAssets.loadFiles(files);
    }

    @Test
    public void theBundledStandardKeepsUpstreamsValues() {
        WorldStyle standard = BUNDLED.worldStyle(AssetId.parse("standard"));
        assertEquals(
            AssetId.parse("outside"),
            standard.outsideStyle()
                .id());
        assertEquals(
            10,
            standard.multiSettings()
                .areaSize());
        assertEquals(
            0.8f,
            standard.multiSettings()
                .correctStyleFactor(),
            0f);
        assertEquals(
            5,
            standard.scattered()
                .size());
        assertEquals(Integer.valueOf(8), standard.scatteredAreaSize());
        assertEquals(Character.valueOf('v'), standard.bridgeSupport());
        assertEquals(WorldStyle.RailwayAvoidance.IGNORE, standard.railwayAvoidance());
        assertEquals(
            "highway_tunnel_bi",
            standard.parts("highways", "tunnel_bi")
                .get(0)
                .id()
                .path());
        assertEquals(
            BlockRef.parse("minecraft:vine:2"),
            standard.vine("west")
                .block());
        assertEquals(
            BlockRef.parse("minecraft:vine:8"),
            standard.vine("east")
                .block());
        assertNotNull(
            standard.randomCityStyle(BiomeDescriptor.NONE, new Random(1)),
            "every biome resolves a city style");
    }

    @Test
    public void doesNotReproducePROF12_aWorldStyleWithoutAnUnfilteredCityStyleIsRejected() {
        AssetSet set = world(
            "\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\", \"biomes\": {\"if_any\": [\"type:SANDY\"]}}]");
        assertEquals(0, set.count(AssetType.WORLDSTYLES));
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.ERROR)
                .get(0)
                .contains("P-PROF-8"));
        assertEquals(1, world("\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\"}]").count(AssetType.WORLDSTYLES));
    }

    @Test
    public void doesNotReproduceSCAT06_aZeroScatteredTotalIsRejected() {
        AssetSet set = world(
            "\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\"}], \"scattered\": {\"areasize\": 8, \"chance\": 0.5,"
                + " \"weightnone\": 0, \"list\": []}");
        assertEquals(0, set.count(AssetType.WORLDSTYLES));
    }

    @Test
    public void doesNotReproduceASSET32_anEmptyPartListIsRejected() {
        AssetSet set = world(
            "\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\"}], \"parts\": {\"highways\": {\"tunnel\": []}}}");
        assertEquals(0, set.count(AssetType.WORLDSTYLES));
    }

    @Test
    public void doesNotReproduceASSET23_supportCharactersAreOneCharacter() {
        AssetSet set = world("\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\"}], \"bridgesupport\": \"vv\"");
        assertEquals(0, set.count(AssetType.WORLDSTYLES));
    }

    @Test
    public void doesNotReproduceASSET25_citySphereSettingsAreIgnoredWithAWarning() {
        AssetSet set = world(
            "\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\"}], \"cityspheres\": {\"centertype\": \"nonsense\"}");
        assertEquals(1, set.count(AssetType.WORLDSTYLES), "an unknown sphere enum no longer throws inside a codec");
        assertTrue(
            TestAssets.lines(set, AssetProblem.Severity.WARNING)
                .stream()
                .anyMatch(w -> w.contains("cityspheres")));
    }

    @Test
    public void vineObjectsReadIntoTheSameBlockModel() {
        AssetSet set = world(
            "\"citystyles\": [{\"factor\": 1, \"citystyle\": \"c\"}], \"settings\": {\"railwayavoidance\": \"block_railway\","
                + " \"vinenorth\": {\"Name\": \"minecraft:vine\", \"Properties\": {\"north\": \"true\", \"up\": \"false\"}}}");
        WorldStyle world = set.worldStyle(AssetId.parse("w"));
        assertEquals(WorldStyle.RailwayAvoidance.BLOCK_RAILWAY, world.railwayAvoidance());
        assertEquals(
            BlockRef.parse("minecraft:vine:4"),
            world.vine("north")
                .block());
    }
}
