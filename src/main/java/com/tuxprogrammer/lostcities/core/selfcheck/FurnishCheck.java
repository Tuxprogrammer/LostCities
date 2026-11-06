package com.tuxprogrammer.lostcities.core.selfcheck;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishItem;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalette;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.FurnishPalettes;
import com.tuxprogrammer.lostcities.worldgen.city.furnish.RoomKind;
import com.tuxprogrammer.lostcities.worldgen.overlay.SpawnerMobs;

/**
 * The furnish palettes against the block and entity registries (design record 0016, lane 6c and 6d): every palette
 * parses, every vanilla block and every mob a palette names is registered, and, when the pack's GregTech is present,
 * every modded block is too. A modded block that is missing is logged as a warning, not a failure: the pass skips such
 * an item at write time and registration never depends on it. Side: both.
 */
public final class FurnishCheck implements SelfCheck {

    @Override
    public String name() {
        return "furnish";
    }

    @Override
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        FurnishPalettes palettes = FurnishPalettes.bundled();
        out.addAll(palettes.problems());
        // Unit tests run without a bootstrapped registry; only the palette parse is checked there.
        boolean booted = Block.blockRegistry.containsKey("minecraft:stone");
        boolean pack = Block.blockRegistry.containsKey(FurnishItem.GT_MACHINES);
        for (RoomKind kind : RoomKind.values()) {
            FurnishPalette palette = palettes.palette(kind);
            if (palette == null) {
                out.add("no furnish palette for " + kind.id());
                continue;
            }
            for (FurnishItem item : palette.items()) {
                String block = item.block();
                if (!booted || Block.blockRegistry.containsKey(block)) {
                    continue;
                }
                if (block.startsWith("minecraft:")) {
                    out.add("furnish " + kind.id() + ": block " + block + " is not registered");
                } else if (pack) {
                    LostCities.LOG.warn("Furnish {}: block {} is not registered in this pack; it is skipped", kind.id(), block);
                }
            }
            for (String mob : palette.mobs()
                .keySet()) {
                if (booted && !SpawnerMobs.known(mob)) {
                    out.add("furnish " + kind.id() + ": mob " + mob + " is not a registered entity");
                }
            }
        }
        return out;
    }
}
