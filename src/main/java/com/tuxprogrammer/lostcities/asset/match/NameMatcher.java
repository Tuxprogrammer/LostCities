package com.tuxprogrammer.lostcities.asset.match;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/regassets/data/ResourceLocationMatcher.java
// Avoids: ASSET-33

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * A stuff asset's test on the chunk's building: {@code if_any} and {@code excluding} lists of names
 * (docs/original-spec/05-assets/scattered-stuff-predefined.md, "Name matcher"). Every name is normalized to a full id
 * and compared as a full id, as every other name in the asset system (P05-2, D31; fixes ASSET-33). A null id (no
 * building) never matches a non-empty {@code if_any}, as upstream.
 *
 * <p>
 * Immutable. Side: both.
 */
public final class NameMatcher {

    /** The matcher that accepts every name. */
    public static final NameMatcher ANY = new NameMatcher(null, Collections.<AssetId>emptySet());

    private final Set<AssetId> ifAny;
    private final Set<AssetId> excluding;

    private NameMatcher(Set<AssetId> ifAny, Set<AssetId> excluding) {
        this.ifAny = ifAny;
        this.excluding = excluding;
    }

    /**
     * Reads a matcher object.
     *
     * @param fields the object
     * @return the matcher; malformed names are reported and left out
     */
    public static NameMatcher parse(JsonFields fields) {
        return new NameMatcher(names(fields, "if_any"), orEmpty(names(fields, "excluding")));
    }

    /**
     * Tests an id.
     *
     * @param id the chunk's building, or null for none
     * @return whether it is in {@code if_any} (when given) and not in {@code excluding}
     */
    public boolean matches(AssetId id) {
        if (ifAny != null && (id == null || !ifAny.contains(id))) {
            return false;
        }
        return id == null || !excluding.contains(id);
    }

    private static Set<AssetId> orEmpty(Set<AssetId> set) {
        return set == null ? Collections.<AssetId>emptySet() : set;
    }

    private static Set<AssetId> names(JsonFields fields, String key) {
        List<String> names = fields.stringList(key, false);
        if (names == null) {
            return null;
        }
        Set<AssetId> out = new LinkedHashSet<>();
        for (int i = 0; i < names.size(); i++) {
            try {
                out.add(AssetId.parse(names.get(i)));
            } catch (IllegalArgumentException e) {
                fields.scope()
                    .error(fields.field(key) + "[" + i + "]", names.get(i), e.getMessage());
            }
        }
        return Collections.unmodifiableSet(out);
    }
}
