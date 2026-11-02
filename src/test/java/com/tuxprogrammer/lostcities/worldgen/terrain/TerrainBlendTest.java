package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkKind;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.CityPlan;

/** The terrain blend, against docs/original-spec/03-terrain/terrain-blend.md and 0006 rule E1. */
public class TerrainBlendTest {

    /** A city east of X = 0 at the given ground; land with the fixture's heights to the west. */
    private static CityPlan cityEastOf(int ground) {
        return (cx, cz) -> {
            int natural = TestTerrain.surface((cx << 4) + 8, (cz << 4) + 8) + 1;
            if (cx >= 0) {
                return ChunkPlan.builder(cx, cz, ChunkKind.CITY)
                    .groundY(ground)
                    .naturalHeight(natural)
                    .build();
            }
            return ChunkPlan.none(cx, cz, 0, 71, natural, false);
        };
    }

    /** Landmark cores at chunk X 0 and 1, land everywhere else. */
    private static CityPlan coreAt(int coreHeight) {
        return (cx, cz) -> {
            int natural = TestTerrain.surface((cx << 4) + 8, (cz << 4) + 8) + 1;
            if (cx == 0 || cx == 1) {
                return ChunkPlan.builder(cx, cz, ChunkKind.LANDMARK_CORE)
                    .groundY(coreHeight)
                    .naturalHeight(coreHeight)
                    .build();
            }
            return ChunkPlan.none(cx, cz, 0, 71, natural, false);
        };
    }

    @Test
    public void cornerWhereCityMeetsLandIsFixedAtTheCityGround() {
        TerrainBlend blend = new TerrainBlend(TestTerrain.context(7L));
        assertTrue(blend.computeCorners(-1, 0, cityEastOf(77)));
        int[] corner = blend.l2Corner(1);
        assertNotNull(corner, "the +X corner of the chunk next to the city is constrained");
        assertTrue(corner[0] >= 77 - 4 && corner[0] <= 77 - 3, "lower = ground + [-4, -3], got " + corner[0]);
        assertTrue(corner[1] >= 77 - 1 && corner[1] <= 77 + 1, "upper = ground + [-1, 1], got " + corner[1]);
    }

    @Test
    public void blendReachesTwoChunksOutAndNoFurther() {
        TerrainBlend blend = new TerrainBlend(TestTerrain.context(7L));
        assertTrue(blend.computeCorners(-3, 0, cityEastOf(77)), "the chunk whose +X corners are two out");
        assertFalse(blend.computeCorners(-4, 0, cityEastOf(77)), "three chunks out is free");
    }

    @Test
    public void blendedTerrainMeetsTheCityGroundAtTheEdge() {
        TerrainContext context = TestTerrain.context(7L);
        TerrainBlend blend = new TerrainBlend(context);
        ArrayChunkBuffer buffer = TestTerrain.chunk(-1, 0);
        blend.apply(buffer, -1, 0, cityEastOf(90));
        int top = ColumnOps.surface(buffer, context.classifier(), 15, 8, true);
        assertTrue(top >= 90 - 4 - 5 && top <= 90 + 1, "the column at the city edge is near the ground, got " + top);
    }

    @Test
    public void landmarkCoreCornerIsFixedAtItsNaturalHeight() {
        TerrainBlend blend = new TerrainBlend(TestTerrain.context(7L));
        assertTrue(blend.computeCorners(-1, 0, coreAt(64)));
        int[] corner = blend.l2Corner(1);
        assertNotNull(corner);
        assertEquals(63, corner[0], "E1: lower = natural - 1");
        assertEquals(65, corner[1], "E1: upper = natural + 1");
    }

    @Test
    public void landmarkCoreIsNeverReshaped() {
        TerrainContext context = TestTerrain.context(7L);
        ArrayChunkBuffer buffer = TestTerrain.chunk(0, 0);
        String before = ChunkBufferDigest.sha256(buffer);
        assertFalse(new TerrainBlend(context).apply(buffer, 0, 0, coreAt(64)));
        assertEquals(before, ChunkBufferDigest.sha256(buffer));
    }

    @Test
    public void landFarFromCitiesIsUntouched() {
        TerrainBlend blend = new TerrainBlend(TestTerrain.context(7L));
        assertFalse(blend.computeCorners(-10, 0, cityEastOf(77)));
        assertNull(blend.l2Corner(0));
    }

    @Test
    public void doesNotReproduceTERR10_blendingNeverChangesAHeight() {
        CityPlan plan = cityEastOf(90);
        int before = plan.chunk(-1, 0)
            .naturalHeight();
        TerrainContext context = TestTerrain.context(7L);
        ArrayChunkBuffer first = TestTerrain.chunk(-1, 0);
        new TerrainBlend(context).apply(first, -1, 0, plan);
        assertEquals(
            before,
            plan.chunk(-1, 0)
                .naturalHeight(),
            "the plan's height is unchanged");
        ArrayChunkBuffer second = TestTerrain.chunk(-1, 0);
        new TerrainBlend(context).apply(second, -1, 0, plan);
        assertEquals(ChunkBufferDigest.sha256(first), ChunkBufferDigest.sha256(second), "a second blend agrees");
    }

    @Test
    public void doesNotReproduceTERR24_aConstrainedCornerAbove255StaysConstrained() {
        TerrainBlend blend = new TerrainBlend(TestTerrain.context(7L));
        // A corner one chunk from a city at Y 250: its upper mesh is 250 + 20..29, above 255. Upstream's "< 256"
        // tests would take it for an unconstrained corner.
        assertTrue(blend.computeCorners(-2, 0, cityEastOf(250)));
        int[] corner = blend.l2Corner(1);
        assertNotNull(corner, "the explicit flag keeps the corner constrained");
        assertTrue(corner[1] > 255, "upper mesh above the build limit, got " + corner[1]);
    }

    @Test
    public void doesNotReproduceTERR27_cornerJitterDependsOnTheWorldSeed() {
        TerrainBlend one = new TerrainBlend(TestTerrain.context(1L));
        TerrainBlend two = new TerrainBlend(TestTerrain.context(2L));
        int differences = 0;
        for (int i = 0; i < 64; i++) {
            if (one.randomizedOffset(i, -i, -4, 4) != two.randomizedOffset(i, -i, -4, 4)) {
                differences++;
            }
            if (one.heightOffset(i, i, 3L, 5L) != two.heightOffset(i, i, 3L, 5L)) {
                differences++;
            }
        }
        assertTrue(differences > 0, "two world seeds give different jitter");
    }
}
