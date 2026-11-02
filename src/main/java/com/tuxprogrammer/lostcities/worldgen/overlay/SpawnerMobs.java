package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.EntityList;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.AssetSet;
import com.tuxprogrammer.lostcities.asset.model.Condition;
import com.tuxprogrammer.lostcities.worldgen.city.LootRules;

/**
 * Checks spawner mobs against the entity registry (docs/new-spec/08-loot-mobs-blocks/index.md § Spawners, "Unknown
 * entity names"): a name the registry knows is set as is; an EnderZoo mob whose mod is absent takes
 * {@link LootRules#FALLBACK_MOB}; any other unknown name leaves the spawner position as air, never a pig spawner. The
 * plan keeps the drawn name, so it stays a pure function of the seed; the fallback is applied where the spawner is
 * written.
 *
 * <p>
 * {@link #problems} lists the bundled mob conditions' names that the registry does not know and that have no
 * fallback, for the dev self-check. Side: server (the population phase), and the self-check on both.
 */
public final class SpawnerMobs {

    /** The mob conditions the bundled palettes name (upstream's {@code common} palette, characters 1 and 4). */
    public static final List<String> BUNDLED_CONDITIONS = Collections
        .unmodifiableList(java.util.Arrays.asList("easymobs", "hardmobs"));

    private static final Set<String> WARNED = Collections.synchronizedSet(new TreeSet<>());

    private SpawnerMobs() {}

    /**
     * Returns the entity name a spawner takes for a drawn name.
     *
     * @param name the name the plan drew, such as {@code Zombie} or {@code enderzoo.FallenKnight}
     * @return the name to set, or the empty string when the spawner becomes air
     */
    public static String resolve(String name) {
        if (name == null || name.isEmpty() || known(name)) {
            return name == null ? "" : name;
        }
        String fallback = name.startsWith("enderzoo.") ? LootRules.FALLBACK_MOB : "";
        if (WARNED.add(name)) {
            LostCities.LOG.warn(
                "Spawner mob {} is not registered in this pack; {}",
                name,
                fallback.isEmpty() ? "those spawners become air" : "using " + fallback);
        }
        return fallback;
    }

    /**
     * Returns whether the entity registry knows a name.
     *
     * @param name the entity name
     * @return {@code true} when {@code EntityList} can create it
     */
    public static boolean known(String name) {
        return EntityList.stringToClassMapping.containsKey(name);
    }

    /**
     * Lists the bundled mob conditions' entity names the registry does not know and that have no fallback.
     *
     * @param assets the loaded assets
     * @return one line per problem
     */
    public static List<String> problems(AssetSet assets) {
        List<String> out = new ArrayList<>();
        for (String id : BUNDLED_CONDITIONS) {
            Condition condition = assets.condition(AssetId.parse(id));
            if (condition == null) {
                out.add("mob condition " + id + " is missing");
                continue;
            }
            for (Condition.Value value : condition.values()) {
                String name = LootRules.entityName(value.value());
                if (!known(name)) {
                    out.add("mob condition " + id + ": " + value.value() + " (" + name + ") is not a registered entity");
                }
            }
        }
        if (!known(LootRules.FALLBACK_MOB)) {
            out.add("the fallback mob " + LootRules.FALLBACK_MOB + " is not a registered entity");
        }
        return out;
    }
}
