package com.tuxprogrammer.lostcities.network.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The ordered validation rules a packet declares for its untrusted fields (CONTRIBUTING § Code standards, "Validate
 * everything the client sends"; networking-side-safety). Server-bound packets get a set with a {@link Sender}, which
 * adds the dimension, reach and permission rules; client-bound packets get one without.
 *
 * <p>
 * Declaring a rule only records it. {@link #firstViolation()} evaluates the rules in declaration order and stops at the
 * first that fails, so target lookups, passed as suppliers, run only after the cheaper range checks passed.
 *
 * <p>
 * Side: both.
 */
public final class Rules {

    /** The default reach for block and tile targets, in blocks: survival reach plus a margin for latency. */
    public static final double DEFAULT_REACH_BLOCKS = 8.0;

    private final Sender sender;
    private final List<Rule> rules = new ArrayList<>();

    private Rules(Sender sender) {
        this.sender = sender;
    }

    /**
     * Returns an empty rule set for a packet a player sent.
     *
     * @param sender the player
     * @return the rule set
     */
    public static Rules forSender(Sender sender) {
        if (sender == null) {
            throw new IllegalArgumentException("sender is null");
        }
        return new Rules(sender);
    }

    /** Returns an empty rule set for a packet the server sent, which has no sender to check. */
    public static Rules withoutSender() {
        return new Rules(null);
    }

    /** Returns the sender these rules check; only for server-bound packets. */
    public Sender sender() {
        return requireSender("sender()");
    }

    /**
     * Adds a custom rule.
     *
     * @param rule the rule
     * @return this set
     */
    public Rules custom(Rule rule) {
        rules.add(rule);
        return this;
    }

    /**
     * Requires {@code 0 <= value < size}, for list, array and slot indices.
     *
     * @param name  the field name, for the log
     * @param value the untrusted value
     * @param size  the exclusive upper bound
     * @return this set
     */
    public Rules indexInRange(String name, int value, int size) {
        return inRange(name, value, 0, (long) size - 1);
    }

    /**
     * Requires {@code min <= value <= max}.
     *
     * @param name  the field name, for the log
     * @param value the untrusted value
     * @param min   the inclusive lower bound
     * @param max   the inclusive upper bound
     * @return this set
     */
    public Rules inRange(String name, long value, long min, long max) {
        return custom(
            () -> value < min || value > max
                ? new Violation(Rejection.OUT_OF_RANGE, name + "=" + value + " not in [" + min + ", " + max + "]")
                : null);
    }

    /**
     * Requires an ordinal of a constant of the enum, so it can index {@code values()} safely.
     *
     * @param name    the field name, for the log
     * @param ordinal the untrusted ordinal
     * @param type    the enum
     * @return this set
     */
    public Rules ordinalInRange(String name, int ordinal, Class<? extends Enum<?>> type) {
        return indexInRange(name, ordinal, type.getEnumConstants().length);
    }

    /**
     * Requires a finite number, so NaN and infinity never reach distance math.
     *
     * @param name  the field name, for the log
     * @param value the untrusted value
     * @return this set
     */
    public Rules finite(String name, double value) {
        return custom(
            () -> Double.isNaN(value) || Double.isInfinite(value)
                ? new Violation(Rejection.OUT_OF_RANGE, name + "=" + value + " is not finite")
                : null);
    }

    /**
     * Requires the lookup to find an object of the expected type, so the handler's cast cannot fail.
     *
     * @param name     what is looked up, for the log
     * @param lookup   finds the target, or returns {@code null} when it is missing or unloaded; must not load chunks
     * @param expected the type the handler casts to
     * @return this set
     */
    public Rules targetOfType(String name, Supplier<?> lookup, Class<?> expected) {
        return custom(() -> {
            Object target = lookup.get();
            if (target == null) {
                return new Violation(Rejection.TARGET_MISSING, name + " not found");
            }
            if (!expected.isInstance(target)) {
                return new Violation(
                    Rejection.WRONG_TARGET_TYPE,
                    name + " is "
                        + target.getClass()
                            .getSimpleName()
                        + ", expected "
                        + expected.getSimpleName());
            }
            return null;
        });
    }

    /**
     * Requires the sender to be alive and connected. Server-bound packets always get this rule first.
     *
     * @return this set
     */
    public Rules senderActive() {
        Sender who = requireSender("senderActive()");
        return custom(() -> who.isAlive() ? null : new Violation(Rejection.SENDER_INACTIVE, "sender is dead or gone"));
    }

    /**
     * Requires the sender to hold a permission level.
     *
     * @param level 0 for any player, 2 for operators
     * @return this set
     */
    public Rules permissionLevel(int level) {
        Sender who = requireSender("permissionLevel()");
        return custom(
            () -> who.hasPermissionLevel(level) ? null
                : new Violation(Rejection.UNAUTHORIZED, who.name() + " lacks permission level " + level));
    }

    /**
     * Requires the packet's dimension to be the one the sender is in.
     *
     * @param dimensionId the dimension the packet names
     * @return this set
     */
    public Rules sameDimension(int dimensionId) {
        Sender who = requireSender("sameDimension()");
        return custom(
            () -> dimensionId == who.dimensionId() ? null
                : new Violation(
                    Rejection.WRONG_DIMENSION,
                    "packet names dimension " + dimensionId + ", sender is in " + who.dimensionId()));
    }

    /**
     * Requires a block within {@link #DEFAULT_REACH_BLOCKS} of the sender, measured to the block's center.
     *
     * @param x block X
     * @param y block Y
     * @param z block Z
     * @return this set
     */
    public Rules withinReachOfBlock(int x, int y, int z) {
        return withinReach(x + 0.5, y + 0.5, z + 0.5, DEFAULT_REACH_BLOCKS);
    }

    /**
     * Requires a point within a distance of the sender. A NaN coordinate fails.
     *
     * @param x           point X, in blocks
     * @param y           point Y, in blocks
     * @param z           point Z, in blocks
     * @param reachBlocks the largest allowed distance, in blocks, greater than zero
     * @return this set
     */
    public Rules withinReach(double x, double y, double z, double reachBlocks) {
        if (!(reachBlocks > 0)) {
            throw new IllegalArgumentException("reachBlocks must be positive: " + reachBlocks);
        }
        Sender who = requireSender("withinReach()");
        return custom(() -> {
            double dx = x - who.x();
            double dy = y - who.y();
            double dz = z - who.z();
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            // Written as a negated <= so that NaN fails.
            return !(distanceSquared <= reachBlocks * reachBlocks)
                ? new Violation(Rejection.OUT_OF_REACH, "distance^2 " + distanceSquared + " > " + reachBlocks + "^2")
                : null;
        });
    }

    /** Returns the first violation, evaluating the rules in declaration order, or {@code null} when all hold. */
    public Violation firstViolation() {
        for (Rule rule : rules) {
            Violation violation = rule.check();
            if (violation != null) {
                return violation;
            }
        }
        return null;
    }

    /** Returns how many rules are declared. */
    public int size() {
        return rules.size();
    }

    private Sender requireSender(String what) {
        if (sender == null) {
            throw new IllegalStateException(what + " needs a sender; client-bound packets have none");
        }
        return sender;
    }
}
