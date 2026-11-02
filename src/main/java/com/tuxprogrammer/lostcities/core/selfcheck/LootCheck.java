package com.tuxprogrammer.lostcities.core.selfcheck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.tuxprogrammer.lostcities.asset.loot.LootDefaults;
import com.tuxprogrammer.lostcities.asset.loot.LootTier;
import com.tuxprogrammer.lostcities.core.assets.LoadedAssets;
import com.tuxprogrammer.lostcities.worldgen.city.LootRules;
import com.tuxprogrammer.lostcities.worldgen.overlay.CityLoot;
import com.tuxprogrammer.lostcities.worldgen.overlay.SpawnerMobs;

/**
 * The tier ceiling as a check, not a convention (docs/design/0004-gtnh-layer-and-tier-ceiling.md, Consequences;
 * loot-and-mobs.md § How the EV ceiling and the weighting are expressed): walks every {@code ChestGenHooks} category
 * this mod registers and reports any item missing from the tier map or above EV, every problem of the bundled loot
 * defaults, an empty category, and every bundled spawner mob the entity registry does not know.
 *
 * <p>
 * Runs in {@code postInit}, after {@link CityLoot#register()} and before TooMuchLoot rewrites categories at server
 * start. Side: both.
 */
public final class LootCheck implements SelfCheck {

    @Override
    public String name() {
        return "loot";
    }

    @Override
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        LootDefaults defaults = CityLoot.defaults();
        if (defaults == null) {
            out.add("the loot categories were never registered");
            return out;
        }
        out.addAll(defaults.problems());
        Random random = new Random(0);
        for (String category : LootRules.CATEGORIES) {
            WeightedRandomChestContent[] items = ChestGenHooks.getItems(category, random);
            if (items.length == 0) {
                out.add(category + " is empty");
            }
            for (WeightedRandomChestContent content : items) {
                LootTier tier = CityLoot.tierOf(content.theItemId);
                String key = CityLoot.key(content.theItemId);
                if (tier == null) {
                    out.add(category + ": " + key + " is not in the tier map");
                } else if (!tier.withinCeiling()) {
                    out.add(category + ": " + key + " is " + tier + ", above " + LootTier.CEILING);
                }
            }
        }
        if (LoadedAssets.isLoaded()) {
            out.addAll(SpawnerMobs.problems(LoadedAssets.current()));
        }
        return out;
    }
}
