package com.tuxprogrammer.lostcities.worldgen.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.layout.ScatteredFixtures.FakeAsset;
import com.tuxprogrammer.lostcities.layout.ScatteredPlan;
import com.tuxprogrammer.lostcities.layout.view.ScatteredView;
import com.tuxprogrammer.lostcities.worldgen.buffer.ArrayChunkBuffer;

/** {@link ScatteredWriter}: floors, overlays, rotation and the terrain fixes of a scattered structure's share. */
public class ScatteredWriterTest {

    private static final CityWriteContext CONTEXT = CityFixtures.plan()
        .context();

    private static ScatteredPlan plan(FakeAsset asset, int lowest, ScatteredPlan.Rotation rotation,
        ScatteredPlan.Share share) {
        return new ScatteredPlan(
            0,
            0,
            asset.name(),
            asset,
            share.chunkX(),
            share.chunkZ(),
            1,
            1,
            rotation,
            lowest,
            null,
            lowest,
            lowest,
            new ScatteredPlan.Share[] { share });
    }

    private static ArrayChunkBuffer write(ScatteredPlan plan, ArrayChunkBuffer buffer) {
        ScatteredPlan.Share share = plan.shares()
            .get(0);
        new ScatteredWriter(
            CONTEXT,
            plan,
            share,
            new BufferTarget(buffer, null),
            new PaletteSequence(CONTEXT.worldSeed(), share.chunkX(), share.chunkZ())).write();
        return buffer;
    }

    private static Part part(String name) {
        return CityFixtures.ASSETS.part(CityPalettes.id(name));
    }

    @Test
    public void writesTheCabinOnItsFoundationTurnedWithThePlan() {
        Part cabin = part("lostcities:cabin");
        assertNotNull(cabin);
        for (ScatteredPlan.Rotation rotation : ScatteredPlan.Rotation.values()) {
            ScatteredPlan.Share share = new ScatteredPlan.Share(
                40,
                40,
                "lostcities:cabin",
                new String[] { "lostcities:cabin" },
                new String[] { null });
            ArrayChunkBuffer buffer = write(
                plan(new FakeAsset("lostcities:cabin"), 70, rotation, share),
                new ArrayChunkBuffer());
            Transform transform = ScatteredWriter.transform(rotation);
            int filled = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    char[] column = cabin.column(x, z);
                    boolean solid = column != null && column[0] != ' ';
                    BlockRef block = buffer.get(transform.rotateX(x, z), 70, transform.rotateZ(x, z));
                    assertEquals(solid, !block.isAir(), rotation + " at " + x + "," + z);
                    filled += solid ? 1 : 0;
                }
            }
            assertTrue(filled > 50);
            assertTrue(
                buffer.get(transform.rotateX(1, 1), 70, transform.rotateZ(1, 1))
                    .isAir());
        }
    }

    @Test
    public void doesNotReproduceSCAT13_anOverlayStartsAtItsFloorsBase() {
        // Radio tower legs with the cabin as the floor's overlay: the cabin's foundation fills (7, 5), where the legs
        // leave a gap, at the floor's base, not on top of the tower.
        Part tower = part("lostcities:radiotower");
        assertTrue(tower.column(7, 5) == null || tower.column(7, 5)[0] == ' ');
        ScatteredPlan.Share share = new ScatteredPlan.Share(
            40,
            40,
            "lostcities:radiotower",
            new String[] { "lostcities:radiotower" },
            new String[] { "lostcities:cabin" });
        ArrayChunkBuffer buffer = write(
            plan(new FakeAsset("lostcities:radiotower"), 70, ScatteredPlan.Rotation.NONE, share),
            new ArrayChunkBuffer());
        assertFalse(
            buffer.get(7, 70, 5)
                .isAir());
        assertTrue(
            buffer.get(7, 70 + tower.height(), 5)
                .isAir());
    }

    @Test
    public void doesNotReproduceSCAT08_theRepeatedSliceStopsAtYOne() {
        // An empty column under a gas station: upstream's loop ran down through the void.
        FakeAsset gas = new FakeAsset("lostcities:highway_gas_station").fix(ScatteredView.TerrainFix.REPEATSLICE)
            .support("lostcities:roadside_supports");
        ScatteredPlan.Share share = new ScatteredPlan.Share(
            40,
            40,
            "lostcities:highway_gas_station",
            new String[] { "lostcities:highway_gas_station" },
            new String[] { null });
        Part supports = part("lostcities:roadside_supports");
        int sx = -1;
        int sz = -1;
        for (int x = 0; x < 16 && sx < 0; x++) {
            for (int z = 0; z < 16; z++) {
                char[] column = supports.column(x, z);
                if (column != null && column[0] != ' ') {
                    sx = x;
                    sz = z;
                    break;
                }
            }
        }
        ArrayChunkBuffer buffer = write(plan(gas, 40, ScatteredPlan.Rotation.NONE, share), new ArrayChunkBuffer());
        for (int y = ScatteredWriter.SUPPORT_BOTTOM; y < 40; y++) {
            assertFalse(
                buffer.get(sx, y, sz)
                    .isAir(),
                "support at y " + y);
        }
        assertTrue(
            buffer.get(sx, 0, sz)
                .isAir());
    }

    @Test
    public void theSupportStopsAtTheGround() {
        FakeAsset gas = new FakeAsset("lostcities:highway_gas_station").fix(ScatteredView.TerrainFix.REPEATSLICE)
            .support("lostcities:roadside_supports");
        ScatteredPlan.Share share = new ScatteredPlan.Share(
            40,
            40,
            "lostcities:highway_gas_station",
            new String[] { "lostcities:highway_gas_station" },
            new String[] { null });
        ArrayChunkBuffer ground = new ArrayChunkBuffer();
        BlockRef stone = BlockRef.of("minecraft:stone", 0);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y <= 60; y++) {
                    ground.set(x, y, z, stone);
                }
            }
        }
        write(plan(gas, 70, ScatteredPlan.Rotation.NONE, share), ground);
        Part supports = part("lostcities:roadside_supports");
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                char[] column = supports.column(x, z);
                boolean support = column != null && column[0] != ' ';
                // Between the ground (60) and the pavement (70) only support columns are filled.
                assertEquals(
                    support,
                    !ground.get(x, 65, z)
                        .isAir(),
                    x + "," + z);
            }
        }
    }

    @Test
    public void clearEmptiesFiftyBlocksAboveTheLowestLevel() {
        FakeAsset asset = new FakeAsset("lostcities:cabin").fix(ScatteredView.TerrainFix.CLEAR);
        ScatteredPlan.Share share = new ScatteredPlan.Share(
            40,
            40,
            "lostcities:cabin",
            new String[] { "lostcities:cabin" },
            new String[] { null });
        ArrayChunkBuffer buffer = new ArrayChunkBuffer();
        BlockRef stone = BlockRef.of("minecraft:stone", 0);
        for (int y = 60; y < 130; y++) {
            buffer.set(0, y, 0, stone);
        }
        write(plan(asset, 70, ScatteredPlan.Rotation.NONE, share), buffer);
        assertFalse(
            buffer.get(0, 69, 0)
                .isAir());
        for (int y = 70; y < 120; y++) {
            assertTrue(
                buffer.get(0, y, 0)
                    .isAir(),
                "y " + y);
        }
        assertFalse(
            buffer.get(0, 120, 0)
                .isAir());
    }
}
