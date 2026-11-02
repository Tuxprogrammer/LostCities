package com.tuxprogrammer.lostcities.core.assets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraftforge.common.BiomeDictionary;

import com.tuxprogrammer.lostcities.asset.AssetProblem;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.match.BiomeEntry;
import com.tuxprogrammer.lostcities.core.selfcheck.SelfCheck;

/**
 * The asset part of the dev self-check (CONTRIBUTING § Testing, layer 3): every asset loads and resolves, and the
 * biome vocabulary the asset system knows in plain Java matches the running Forge. Fallbacks taken because a mod is
 * absent are not failures; they are listed in the asset report, and the real-pack scenario
 * {@code assets.pack_resolution} requires there are none (docs/new-spec/00-pack-integration/block-mapping.md, "To check
 * in game").
 *
 * <p>
 * Side: both.
 */
public final class AssetSelfCheck implements SelfCheck {

    private final AssetSet set;

    /**
     * Creates the check.
     *
     * @param set the loaded set
     */
    public AssetSelfCheck(AssetSet set) {
        this.set = set;
    }

    @Override
    public String name() {
        return "assets";
    }

    @Override
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        for (AssetProblem problem : set.problems(AssetProblem.Severity.ERROR)) {
            out.add(problem.toString());
        }
        Set<String> forge = new TreeSet<>();
        for (BiomeDictionary.Type type : BiomeDictionary.Type.values()) {
            forge.add(type.name());
        }
        Set<String> known = new TreeSet<>(BiomeEntry.KNOWN_TYPES);
        if (!forge.equals(known)) {
            out.add("BiomeEntry.KNOWN_TYPES " + known + " differs from Forge's BiomeDictionary.Type " + forge);
        }
        return out;
    }
}
