package com.tuxprogrammer.lostcities.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

/**
 * The late mixins: mixins into other mods' classes, which exist only once mods are loaded, each applied only when its
 * mod is (CONTRIBUTING § Mixins: UniMixins in the GTNH style). GTNHMixins finds this class by its {@link LateMixin}
 * annotation and asks it for the mixins of {@code mixins.lostcitiesnh.late.json}.
 *
 * <ul>
 * <li>{@code MixinRoguelikeDungeon}: the Roguelike Dungeons steering (0006 §5), when {@code Roguelike} is loaded.</li>
 * </ul>
 *
 * <p>
 * Side: both (mixin loading).
 */
@LateMixin
public final class LateMixinLoader implements ILateMixinLoader {

    /** The late mixin config, a resource of this jar. */
    public static final String CONFIG = "mixins.lostcitiesnh.late.json";
    /** Roguelike Dungeons' mod ID (the compat package's constant would load more than a mixin loader should). */
    static final String ROGUELIKE = "Roguelike";

    @Override
    public String getMixinConfig() {
        return CONFIG;
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return mixinsFor(loadedMods);
    }

    /**
     * Returns the late mixins for a set of loaded mods.
     *
     * @param loadedMods the loaded mods' IDs
     * @return the mixin class names, relative to the config's package
     */
    static List<String> mixinsFor(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        if (loadedMods.contains(ROGUELIKE)) {
            mixins.add("MixinRoguelikeDungeon");
        }
        return mixins;
    }
}
