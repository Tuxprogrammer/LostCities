package com.tuxprogrammer.lostcities.asset.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * How a 1.20 block's state properties become 1.7.10 metadata: the D19 rules of
 * docs/new-spec/00-pack-integration/block-mapping.md, "Blockstate properties to metadata". The bits a rule computes
 * are added to the base metadata of the mapping's candidate, which holds the type bits (a slab's material, a sapling's
 * wood).
 *
 * <p>
 * Values are the pre-flattening encodings that 1.8 to 1.12 kept from 1.7.10 for the same blocks (stairs facing east 0,
 * west 1, south 2, north 3; doors and trapdoors as their vanilla blocks; rails 0 to 9), so a 1.20 property that the
 * 1.13 flattening produced maps back to the metadata it came from. Properties that 1.7.10 computes from neighbors are
 * dropped: stair {@code shape}, fence, pane, bar and wall connections, {@code waterlogged}, and {@code powered} or
 * {@code lit} where 1.7.10 has no bit for them. An unknown property is a warning; a known property with an unknown
 * value is an error, so a typo never silently becomes the default.
 *
 * <p>
 * Each rule also names the rotation family the 1.7.10 block belongs to, so the writer can turn its metadata with a
 * part's transform ({@link com.tuxprogrammer.lostcities.asset.transform.MetaRotation}). Side: both.
 */
public enum MetaRule {

    /** No properties consumed; the base metadata is used as is. */
    PLAIN(RotationFamily.NONE) {

        @Override
        int bits(Props props) {
            return 0;
        }
    },
    /** Stairs: {@code facing} east 0, west 1, south 2, north 3; {@code half=top} adds 4. */
    STAIRS(RotationFamily.STAIRS, "facing", "half") {

        @Override
        int bits(Props props) {
            int facing = props.choice("facing", "north", "east", "west", "south", "north");
            return facing + (props.choice("half", "bottom", "bottom", "top") == 1 ? 4 : 0);
        }
    },
    /** Slabs: {@code type} bottom adds 0, top adds 8; double is a different block, chosen by the mapping row. */
    SLAB(RotationFamily.NONE, "type") {

        @Override
        int bits(Props props) {
            return props.choice("type", "bottom", "bottom", "top", "double") == 1 ? 8 : 0;
        }
    },
    /**
     * Doors: the lower half {@code facing} east 0, south 1, west 2, north 3, {@code open} adds 4; the upper half is 8,
     * right {@code hinge} adds 1, {@code powered} adds 2.
     */
    DOOR(RotationFamily.DOOR, "facing", "half", "hinge", "open", "powered") {

        @Override
        int bits(Props props) {
            int facing = props.choice("facing", "north", "east", "south", "west", "north");
            boolean open = props.bool("open");
            boolean upper = props.choice("half", "lower", "lower", "upper") == 1;
            boolean right = props.choice("hinge", "left", "left", "right") == 1;
            boolean powered = props.bool("powered");
            if (upper) {
                return 8 + (right ? 1 : 0) + (powered ? 2 : 0);
            }
            return facing + (open ? 4 : 0);
        }
    },
    /** Standing torches and redstone torches: metadata 5. {@code lit} is dropped. */
    TORCH(RotationFamily.TORCH, "lit") {

        @Override
        int bits(Props props) {
            props.consume("lit");
            return 5;
        }
    },
    /** Wall torches and redstone wall torches: {@code facing} east 1, west 2, south 3, north 4. */
    WALL_TORCH(RotationFamily.TORCH, "facing", "lit") {

        @Override
        int bits(Props props) {
            props.consume("lit");
            return 1 + props.choice("facing", "north", "east", "west", "south", "north");
        }
    },
    /** Ladders, furnaces and chests: {@code facing} north 2, south 3, west 4, east 5. {@code lit} is dropped. */
    FACING(RotationFamily.FACING, "facing", "lit", "type") {

        @Override
        int bits(Props props) {
            props.consume("lit");
            props.consume("type");
            return 2 + props.choice("facing", "north", "north", "south", "west", "east");
        }
    },
    /**
     * Levers: on a wall, {@code facing} east 1, west 2, south 3, north 4; on the floor 5 (north-south) or 6
     * (east-west);
     * on the ceiling 7 (north-south) or 0 (east-west). {@code powered} adds 8.
     */
    LEVER(RotationFamily.LEVER, "face", "facing", "powered") {

        @Override
        int bits(Props props) {
            int face = props.choice("face", "wall", "wall", "floor", "ceiling");
            int facing = props.choice("facing", "north", "east", "west", "south", "north");
            boolean alongX = facing <= 1;
            int meta = switch (face) {
                case 1 -> alongX ? 6 : 5;
                case 2 -> alongX ? 0 : 7;
                default -> 1 + facing;
            };
            return meta + (props.bool("powered") ? 8 : 0);
        }
    },
    /** Rails: {@code shape} north_south 0 to north_east 9, in vanilla order. */
    RAIL(RotationFamily.RAIL, "shape") {

        @Override
        int bits(Props props) {
            return props.choice("shape", "north_south", RAIL_SHAPES);
        }
    },
    /** Powered rails: {@code shape} 0 to 5 (no curves), {@code powered} adds 8. */
    POWERED_RAIL(RotationFamily.POWERED_RAIL, "shape", "powered") {

        @Override
        int bits(Props props) {
            int shape = props.choice("shape", "north_south", Arrays.copyOf(RAIL_SHAPES, PoweredRails.STRAIGHT_SHAPES));
            return shape + (props.bool("powered") ? 8 : 0);
        }
    },
    /**
     * Trapdoors: {@code facing} north 0, south 1, west 2, east 3; {@code open} adds 4; {@code half=top} adds 8.
     * {@code powered} is dropped.
     */
    TRAPDOOR(RotationFamily.TRAPDOOR, "facing", "half", "open", "powered") {

        @Override
        int bits(Props props) {
            props.consume("powered");
            int facing = props.choice("facing", "north", "north", "south", "west", "east");
            return facing + (props.bool("open") ? 4 : 0)
                + (props.choice("half", "bottom", "bottom", "top") == 1 ? 8 : 0);
        }
    },
    /** Saplings: {@code stage=1} adds 8. */
    SAPLING(RotationFamily.NONE, "stage") {

        @Override
        int bits(Props props) {
            return props.choice("stage", "0", "0", "1") == 1 ? 8 : 0;
        }
    },
    /** Leaves: {@code persistent=true} adds 4, the no-decay bit; {@code distance} is dropped. */
    LEAVES(RotationFamily.NONE, "persistent", "distance") {

        @Override
        int bits(Props props) {
            props.consume("distance");
            return props.bool("persistent") ? 4 : 0;
        }
    },
    /** Barrels: piston-style {@code facing} down 0, up 1, north 2, south 3, west 4, east 5; {@code open} adds 8. */
    BARREL(RotationFamily.PISTON, "facing", "open") {

        @Override
        int bits(Props props) {
            int facing = props.choice("facing", "north", "down", "up", "north", "south", "west", "east");
            return facing + (props.bool("open") ? 8 : 0);
        }
    },
    /** Chains: {@code axis} y 0, x 1, z 2. */
    CHAIN(RotationFamily.AXIS, "axis") {

        @Override
        int bits(Props props) {
            return props.choice("axis", "y", "y", "x", "z");
        }
    },
    /** Lanterns: {@code hanging=true} is 1, standing 0. */
    LANTERN(RotationFamily.NONE, "hanging") {

        @Override
        int bits(Props props) {
            return props.bool("hanging") ? 1 : 0;
        }
    },
    /** Anvils: {@code facing} as a horizontal index, south 0, west 1, north 2, east 3. */
    ANVIL(RotationFamily.ANVIL, "facing") {

        @Override
        int bits(Props props) {
            return props.choice("facing", "north", "south", "west", "north", "east");
        }
    },
    /** Vines: one bit per side, south 1, west 2, north 4, east 8; {@code up} is dropped. */
    VINE(RotationFamily.VINE, "north", "south", "east", "west", "up") {

        @Override
        int bits(Props props) {
            props.consume("up");
            return (props.bool("south") ? 1 : 0) + (props.bool("west") ? 2 : 0)
                + (props.bool("north") ? 4 : 0)
                + (props.bool("east") ? 8 : 0);
        }
    };

    /** Rail shapes in 1.7.10 metadata order. */
    static final String[] RAIL_SHAPES = { "north_south", "east_west", "ascending_east", "ascending_west",
        "ascending_north", "ascending_south", "south_east", "south_west", "north_west", "north_east" };

    /** Properties dropped for every rule, because 1.7.10 computes them or has no bit for them. */
    static final Set<String> ALWAYS_DROPPED = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("waterlogged", "shape", "north", "south", "east", "west", "up", "powered", "lit")));

    private final RotationFamily family;
    private final Set<String> consumed;

    MetaRule(RotationFamily family, String... consumed) {
        this.family = family;
        this.consumed = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(consumed)));
    }

    /** Returns the rotation family of blocks whose metadata follows this rule. */
    public RotationFamily family() {
        return family;
    }

    /**
     * Returns the rule with a data-file name such as {@code stairs} or {@code wall_torch}.
     *
     * @param name the name, case-insensitive
     * @return the rule, or null when there is none
     */
    public static MetaRule byName(String name) {
        for (MetaRule rule : values()) {
            if (rule.name()
                .equalsIgnoreCase(name)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Computes the metadata bits for a set of properties.
     *
     * @param properties the 1.20 properties
     * @return the bits and the problems found
     */
    public Result apply(Map<String, String> properties) {
        Props props = new Props(properties);
        int bits = bits(props);
        for (String key : properties.keySet()) {
            if (!props.used.contains(key) && !consumed.contains(key) && !ALWAYS_DROPPED.contains(key)) {
                props.warnings.add("property '" + key + "' has no 1.7.10 meaning here and is ignored");
            }
        }
        return new Result(bits, props.errors, props.warnings);
    }

    abstract int bits(Props props);

    /** The bits a rule computed, with what it could not read. */
    public static final class Result {

        private final int bits;
        private final List<String> errors;
        private final List<String> warnings;

        Result(int bits, List<String> errors, List<String> warnings) {
            this.bits = bits;
            this.errors = Collections.unmodifiableList(errors);
            this.warnings = Collections.unmodifiableList(warnings);
        }

        /** Returns the metadata bits to add to the candidate's base metadata. */
        public int bits() {
            return bits;
        }

        /** Returns the errors: known properties with unknown values. */
        public List<String> errors() {
            return errors;
        }

        /** Returns the warnings: properties ignored. */
        public List<String> warnings() {
            return warnings;
        }
    }

    /** Powered-rail constants. */
    private static final class PoweredRails {

        /** Powered rails have the six straight shapes only. */
        static final int STRAIGHT_SHAPES = 6;
    }

    /** Reads properties for one rule, recording which it used and what was wrong. */
    static final class Props {

        private final Map<String, String> values;
        private final Set<String> used = new HashSet<>();
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        Props(Map<String, String> values) {
            this.values = values;
        }

        void consume(String key) {
            used.add(key);
        }

        /** Returns the index of the property's value among the options, the default's index when absent. */
        int choice(String key, String defaultValue, String... options) {
            used.add(key);
            String value = values.containsKey(key) ? values.get(key)
                .toLowerCase(Locale.ROOT) : defaultValue;
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(value)) {
                    return i;
                }
            }
            errors.add("property " + key + "=" + value + " is not one of " + Arrays.toString(options));
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(defaultValue)) {
                    return i;
                }
            }
            return 0;
        }

        /** Returns a boolean property, false when absent. */
        boolean bool(String key) {
            return choice(key, "false", "false", "true") == 1;
        }
    }
}
