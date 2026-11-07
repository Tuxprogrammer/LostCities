package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/**
 * D-tunnel-light-1: the bundled world style's highway tunnels are lit by ceiling glowstone whatever
 * {@code generateLighting} says. Each of the four variants is placed with lighting off and its blocks are checked
 * for the lamps at the lattice {2, 6, 9, 13} squared, and for light level 8 or above over the road lanes.
 */
public class HighwayTunnelLightTest {

    private static final AssetSet ASSETS = CityFixtures.ASSETS;
    private static final int[] LATTICE = { 2, 6, 9, 13 };
    private static final int DECK_Y = 70;
    private static final int CEILING_Y = DECK_Y + 5;
    private static final String[] SLOTS = { "tunnel", "tunnel_bi", "tunnel_bend", "tunnel_t" };

    private static ArrayChunkBuffer place(Part part) {
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        BlockRef rock = BlockRef.of("minecraft:dirt", 0);
        for (int y = DECK_Y - 3; y < DECK_Y + 12; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    buffer.set(x, y, z, rock);
                }
            }
        }
        CompiledPalette palette = CompiledPalette.compile(
            MetalFlags.ALL_ON,
            ASSETS.palette(AssetId.parse("common")),
            ASSETS.palette(AssetId.parse("default")));
        PartWriter.Placement placement = new PartWriter.Placement(
            part,
            palette,
            Transform.ROTATE_NONE,
            0,
            DECK_Y,
            0,
            HardAir.AIR,
            null);
        PartWriter.Rules rules = new PartWriter.Rules(
            false,
            true,
            false,
            false,
            false,
            63,
            BlockRef.of("minecraft:water", 0));
        new PartWriter(new BufferTarget(buffer, null), new PaletteSequence(1, 0, 0), rules, LayoutLog.SILENT)
            .place(placement);
        return buffer;
    }

    private static boolean glowstone(ArrayChunkBuffer b, int x, int y, int z) {
        return "minecraft:glowstone".equals(
            b.get(x, y, z)
                .name());
    }

    @Test
    public void everyTunnelVariantHasTheCeilingLampsWithLightingOff() {
        for (String slot : SLOTS) {
            Part part = ASSETS.worldStyle(AssetId.parse("standard"))
                .parts("highways", slot)
                .get(0);
            assertTrue(part.id()
                .path()
                .startsWith("nh_highway_"), slot + " uses the lit variant");
            ArrayChunkBuffer b = place(part);
            TreeSet<String> lamps = new TreeSet<>();
            for (int y = DECK_Y; y < DECK_Y + 12; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (glowstone(b, x, y, z)) {
                            assertEquals(CEILING_Y, y, slot + ": lamps sit in the ceiling slice");
                            lamps.add(x + "," + z);
                        }
                    }
                }
            }
            TreeSet<String> expected = new TreeSet<>();
            for (int x : LATTICE) {
                for (int z : LATTICE) {
                    expected.add(x + "," + z);
                }
            }
            assertEquals(expected, lamps, slot + ": lamps at the lattice, spacing 4/3/4 and 5 across a chunk edge");
        }
    }

    @Test
    public void everyTunnelVariantKeepsTheRoadLanesAtLightEightOrMore() {
        for (String slot : SLOTS) {
            Part part = ASSETS.worldStyle(AssetId.parse("standard"))
                .parts("highways", slot)
                .get(0);
            ArrayChunkBuffer b = place(part);
            Map<Long, Integer> light = new HashMap<>();
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (int x : LATTICE) {
                for (int z : LATTICE) {
                    if (glowstone(b, x, CEILING_Y, z)) {
                        light.put(key(x, CEILING_Y, z), 15);
                        queue.add(new int[] { x, CEILING_Y, z });
                    }
                }
            }
            int[][] dirs = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
            while (!queue.isEmpty()) {
                int[] p = queue.poll();
                int level = light.get(key(p[0], p[1], p[2]));
                for (int[] d : dirs) {
                    int x = p[0] + d[0];
                    int y = p[1] + d[1];
                    int z = p[2] + d[2];
                    if (x < 0 || x > 15 || z < 0 || z > 15 || !b.get(x, y, z)
                        .isAir() || light.getOrDefault(key(x, y, z), 0) >= level - 1) {
                        continue;
                    }
                    light.put(key(x, y, z), level - 1);
                    queue.add(new int[] { x, y, z });
                }
            }
            BlockRef lane = b.get(8, DECK_Y, 7);
            int lanes = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (b.get(x, DECK_Y, z)
                        .equals(lane) && b.get(x, DECK_Y + 1, z)
                            .isAir()) {
                        lanes++;
                        int level = light.getOrDefault(key(x, DECK_Y + 1, z), 0);
                        assertTrue(level >= 8, slot + ": lane cell " + x + "," + z + " has light " + level);
                    }
                }
            }
            assertTrue(lanes >= 32, slot + ": the lane cells were found (" + lanes + ")");
        }
    }

    @Test
    public void theUpstreamPartsStayUnlitSoTheLampsAreOurOverrideOnly() {
        for (String name : Arrays.asList("highway_tunnel", "highway_tunnel_bi", "highway_tunnel_bend", "highway_tunnel_t")) {
            ArrayChunkBuffer b = place(ASSETS.part(AssetId.parse(name)));
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    assertTrue(!glowstone(b, x, CEILING_Y, z), name + " has no lamp at " + x + "," + z);
                }
            }
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) x << 40) | ((long) y << 20) | z;
    }
}
