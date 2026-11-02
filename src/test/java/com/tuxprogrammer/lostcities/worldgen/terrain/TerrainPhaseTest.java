package com.tuxprogrammer.lostcities.worldgen.terrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.worldgen.TestTerrain;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;
import com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBufferDigest;
import com.tuxprogrammer.lostcities.worldgen.buffer.GoldenChunks;
import com.tuxprogrammer.lostcities.worldgen.height.ChunkHeights;
import com.tuxprogrammer.lostcities.worldgen.plan.CachedCityPlan;
import com.tuxprogrammer.lostcities.worldgen.plan.ChunkPlan;
import com.tuxprogrammer.lostcities.worldgen.synthetic.SyntheticCityPlan;

/**
 * The terrain phase over the synthetic city on the fixture terrain: golden hashes of the chunk after the terrain
 * phase (0005, "Determinism tests"), and the same result whatever order chunks are generated in.
 */
public class TerrainPhaseTest {

    static final long SEED = 20261102L;

    /** Golden cases: a name per kind of chunk the phase treats differently. */
    static final Map<String, int[]> CASES = new LinkedHashMap<>();

    static {
        CASES.put("terrain_city_street", new int[] { 0, 0 });
        CASES.put("terrain_city_lot_1_1", new int[] { 1, 1 });
        CASES.put("terrain_city_lot_2_2", new int[] { 2, 2 });
        CASES.put("terrain_city_lot_5_1", new int[] { 5, 1 });
        CASES.put("terrain_soft_edge_street", new int[] { 3, -4 });
        CASES.put("terrain_core_lane", new int[] { 4, -4 });
        CASES.put("terrain_core_untouched", new int[] { 5, -5 });
        CASES.put("terrain_edge_core_ring", new int[] { -5, 0 });
        CASES.put("terrain_blend_beside_core", new int[] { -8, 0 });
        CASES.put("terrain_blend_south", new int[] { 0, 8 });
    }

    static TerrainPhase phase() {
        TerrainContext context = TestTerrain.context(SEED);
        ChunkHeights heights = new ChunkHeights(
            TestTerrain.oracle(),
            context.settings()
                .heightSampleSize(),
            ChunkHeights.Listener.NONE);
        return new TerrainPhase(
            context,
            new CachedCityPlan(new SyntheticCityPlan(SEED, context.settings(), heights)));
    }

    static ArrayChunkBuffer run(TerrainPhase phase, int chunkX, int chunkZ) {
        ArrayChunkBuffer buffer = TestTerrain.chunk(chunkX, chunkZ);
        phase.run(buffer, chunkX, chunkZ);
        return buffer;
    }

    @Test
    public void goldenChunksAfterTheTerrainPhase() throws Exception {
        TerrainPhase phase = phase();
        for (Map.Entry<String, int[]> entry : CASES.entrySet()) {
            int[] at = entry.getValue();
            GoldenChunks.assertMatches(entry.getKey(), run(phase, at[0], at[1]));
        }
    }

    @Test
    public void generationOrderDoesNotMatter() {
        List<int[]> chunks = new ArrayList<>();
        for (int cx = -9; cx <= 9; cx += 2) {
            for (int cz = -9; cz <= 9; cz += 3) {
                chunks.add(new int[] { cx, cz });
            }
        }
        Map<String, String> forward = new LinkedHashMap<>();
        TerrainPhase first = phase();
        for (int[] chunk : chunks) {
            forward.put(chunk[0] + "," + chunk[1], ChunkBufferDigest.sha256(run(first, chunk[0], chunk[1])));
        }
        List<int[]> reversed = new ArrayList<>(chunks);
        Collections.reverse(reversed);
        TerrainPhase second = phase();
        for (int[] chunk : reversed) {
            assertEquals(
                forward.get(chunk[0] + "," + chunk[1]),
                ChunkBufferDigest.sha256(run(second, chunk[0], chunk[1])),
                "chunk " + chunk[0] + "," + chunk[1]);
        }
    }

    @Test
    public void streetsSitAtTheCityGround() {
        TerrainPhase phase = phase();
        ChunkPlan plan = phase.cityPlan()
            .chunk(0, 0);
        ArrayChunkBuffer buffer = run(phase, 0, 0);
        int paved = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (BlockRef.of("minecraft:double_stone_slab", 0)
                    .equals(buffer.get(x, plan.groundY(), z))) {
                    paved++;
                }
            }
        }
        assertTrue(paved > 200, "street blocks at Y " + plan.groundY() + ": " + paved);
    }

    @Test
    public void anUntouchedCoreIsExactlyTheFixture() {
        TerrainPhase phase = phase();
        assertEquals(
            ChunkBufferDigest.sha256(TestTerrain.chunk(5, -5)),
            ChunkBufferDigest.sha256(run(phase, 5, -5)),
            "the city writes nothing in a landmark core without a lane");
        assertEquals(TerrainPhase.ChunkClass.UNTOUCHED, phase.run(TestTerrain.chunk(5, -5), 5, -5)
            .chunkClass());
    }

    @Test
    public void farFromCitiesNothingChanges() {
        TerrainPhase phase = phase();
        assertEquals(
            ChunkBufferDigest.sha256(TestTerrain.chunk(30, 30)),
            ChunkBufferDigest.sha256(run(phase, 30, 30)));
    }
}
