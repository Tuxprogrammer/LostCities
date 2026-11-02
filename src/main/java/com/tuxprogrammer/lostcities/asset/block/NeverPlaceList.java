package com.tuxprogrammer.lostcities.asset.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.Problems;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * The blocks generation never places, read from {@code data/lostcitiesnh/blockmap/never_place.json}: unbreakable,
 * explosive and painfully hard blocks, valuable storage outside loot, metals above EV, GT ore hosts, infested blocks,
 * casings and machines, and gameplay hazards (D28 rule 7 as amended by D29; D19; docs/new-spec/05-assets/
 * block-catalog/index.md, "Never placed"). The resolver enforces it at load for every palette, variant and override:
 * an entry naming such a block is rejected and takes its fallback.
 *
 * <p>
 * Treasure characters are loot, so they may name valuable storage blocks ({@link #VALUABLE}); nothing else is exempt.
 * Side: both.
 */
public final class NeverPlaceList {

    /** The bundled data file, as a class-path resource. */
    public static final String RESOURCE = "/data/lostcitiesnh/blockmap/never_place.json";

    /** The reason treasure characters are exempt from. */
    public static final String VALUABLE = "valuable storage outside loot";

    private final Map<String, List<BlockPattern>> reasons;

    private NeverPlaceList(Map<String, List<BlockPattern>> reasons) {
        this.reasons = reasons;
    }

    /** Returns a list that forbids nothing, for tests. */
    public static NeverPlaceList empty() {
        return new NeverPlaceList(new LinkedHashMap<>());
    }

    /**
     * Reads the list.
     *
     * @param json  the parsed {@code never_place.json}
     * @param scope where problems go
     * @return the list
     */
    public static NeverPlaceList parse(JsonElement json, Problems.Scope scope) {
        Map<String, List<BlockPattern>> reasons = new LinkedHashMap<>();
        if (!json.isJsonObject()) {
            scope.error("", null, "must be an object");
            return new NeverPlaceList(reasons);
        }
        JsonFields root = new JsonFields(json.getAsJsonObject(), "", scope);
        JsonFields byReason = root.object("reasons", true);
        if (byReason == null) {
            return new NeverPlaceList(reasons);
        }
        for (String reason : byReason.keys()) {
            List<String> patterns = byReason.stringList(reason, true);
            List<BlockPattern> parsed = new ArrayList<>();
            if (patterns != null) {
                for (String pattern : patterns) {
                    try {
                        parsed.add(BlockPattern.parse(pattern));
                    } catch (IllegalArgumentException e) {
                        byReason.error(reason, pattern, e.getMessage());
                    }
                }
            }
            reasons.put(reason, Collections.unmodifiableList(parsed));
        }
        return new NeverPlaceList(reasons);
    }

    /**
     * Returns why a block is never placed.
     *
     * @param block    the block
     * @param treasure whether the entry is a treasure character, which may hold valuable storage blocks
     * @return the reason, or null when the block may be placed
     */
    public String reason(BlockRef block, boolean treasure) {
        for (Map.Entry<String, List<BlockPattern>> entry : reasons.entrySet()) {
            if (treasure && VALUABLE.equals(entry.getKey())) {
                continue;
            }
            for (BlockPattern pattern : entry.getValue()) {
                if (pattern.matches(block)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
