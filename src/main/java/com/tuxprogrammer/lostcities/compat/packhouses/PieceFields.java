package com.tuxprogrammer.lostcities.compat.packhouses;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Sets the two pieces of state a village piece keeps that a pack house must control, by reflection resolved by class
 * and field value, never by obfuscated name:
 * <ul>
 * <li>the <b>ground level</b>: a piece starts with {@code -1} in its ground field and, when it is still negative,
 * measures the ground and shifts its own box by an offset. Setting it makes the piece skip that shift, so the box the
 * house is built at is the box it fills. In the base class it is the one int field that starts at {@code -1}; in a
 * mod's subclass it is an int field whose name contains "ground" and that starts at {@code -1};</li>
 * <li>the <b>villager counters</b>: a piece spawns villagers until a counter reaches the count it asks for. Raising the
 * counters (the base class's int field that starts at {@code 0}, and a subclass's int field whose name contains
 * "spawned" and that starts at {@code 0}) means none is spawned (docs/design/decision-log.md D-p2e-4).</li>
 * </ul>
 * A field that is missing is not an error: the piece then keeps its own behavior, which on a lot's flat ground gives
 * the same box.
 *
 * <p>
 * Side: server (plain Java; no Minecraft types).
 */
public final class PieceFields {

    /** What {@link #apply} changed. */
    public static final class Result {
        private final int groundFields;
        private final int counterFields;

        Result(int groundFields, int counterFields) {
            this.groundFields = groundFields;
            this.counterFields = counterFields;
        }

        /** Returns how many ground fields were set. */
        public int groundFields() {
            return groundFields;
        }

        /** Returns how many villager counters were raised. */
        public int counterFields() {
            return counterFields;
        }
    }

    /** The value a villager counter is raised to: more than any piece asks for. */
    public static final int NO_VILLAGERS = 1000;

    private PieceFields() {}

    /**
     * Sets a piece's ground level and silences its villager counters.
     *
     * @param piece  the piece
     * @param base   the village base class: the topmost class searched, and the one whose int fields are matched by
     *               value alone
     * @param ground the ground level to set
     * @return what was set; zeros when the piece has no such fields
     */
    public static Result apply(Object piece, Class<?> base, int ground) {
        int grounds = 0;
        int counters = 0;
        for (Class<?> type = piece.getClass(); type != null; type = type == base ? null : type.getSuperclass()) {
            boolean isBase = type == base;
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String name = field.getName()
                    .toLowerCase(java.util.Locale.ROOT);
                try {
                    field.setAccessible(true);
                    int value = field.getInt(piece);
                    if (value == -1 && (isBase || name.contains("ground"))) {
                        field.setInt(piece, ground);
                        grounds++;
                    } else if (value == 0 && (isBase || name.contains("spawned"))) {
                        field.setInt(piece, NO_VILLAGERS);
                        counters++;
                    }
                } catch (IllegalAccessException | RuntimeException e) {
                    // A field that cannot be reached is left as it is.
                    continue;
                }
            }
        }
        return new Result(grounds, counters);
    }
}
