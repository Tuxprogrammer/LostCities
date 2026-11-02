package com.tuxprogrammer.lostcities.network.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Each validation rule accepts what is in bounds and rejects what is not, with the right reason. */
public class RulesTest {

    private enum Color {
        RED,
        GREEN
    }

    @Test
    public void indexOutOfRangeIsRejected() {
        assertNull(
            rules().indexInRange("slot", 0, 3)
                .firstViolation());
        assertNull(
            rules().indexInRange("slot", 2, 3)
                .firstViolation());
        assertReason(Rejection.OUT_OF_RANGE, rules().indexInRange("slot", 3, 3));
        assertReason(Rejection.OUT_OF_RANGE, rules().indexInRange("slot", -1, 3));
        assertReason(Rejection.OUT_OF_RANGE, rules().indexInRange("slot", 0, 0));
    }

    @Test
    public void rangeIsInclusive() {
        assertNull(
            rules().inRange("y", 255, 0, 255)
                .firstViolation());
        assertReason(Rejection.OUT_OF_RANGE, rules().inRange("y", 256, 0, 255));
        assertReason(Rejection.OUT_OF_RANGE, rules().inRange("y", Integer.MIN_VALUE, 0, 255));
    }

    @Test
    public void enumOrdinalOutOfRangeIsRejected() {
        assertNull(
            rules().ordinalInRange("color", 1, Color.class)
                .firstViolation());
        assertReason(Rejection.OUT_OF_RANGE, rules().ordinalInRange("color", 2, Color.class));
        assertReason(Rejection.OUT_OF_RANGE, rules().ordinalInRange("color", -1, Color.class));
    }

    @Test
    public void nonFiniteNumbersAreRejected() {
        assertNull(
            rules().finite("x", 1.5)
                .firstViolation());
        assertReason(Rejection.OUT_OF_RANGE, rules().finite("x", Double.NaN));
        assertReason(Rejection.OUT_OF_RANGE, rules().finite("x", Double.POSITIVE_INFINITY));
    }

    @Test
    public void wrongDimensionIsRejected() {
        FakeSender sender = FakeSender.player();
        sender.dimension = -1;
        assertNull(
            Rules.forSender(sender)
                .sameDimension(-1)
                .firstViolation());
        assertReason(
            Rejection.WRONG_DIMENSION,
            Rules.forSender(sender)
                .sameDimension(0));
    }

    @Test
    public void unauthorizedSenderIsRejected() {
        assertReason(
            Rejection.UNAUTHORIZED,
            Rules.forSender(FakeSender.player())
                .permissionLevel(2));
        assertNull(
            Rules.forSender(FakeSender.operator())
                .permissionLevel(2)
                .firstViolation());
        assertNull(
            Rules.forSender(FakeSender.player())
                .permissionLevel(0)
                .firstViolation());
    }

    @Test
    public void reachIsMeasuredToTheBlockCenter() {
        FakeSender sender = FakeSender.player();
        assertNull(
            Rules.forSender(sender)
                .withinReachOfBlock(7, 64, 0)
                .firstViolation());
        assertReason(
            Rejection.OUT_OF_REACH,
            Rules.forSender(sender)
                .withinReachOfBlock(9, 64, 0));
        assertReason(
            Rejection.OUT_OF_REACH,
            Rules.forSender(sender)
                .withinReach(Double.NaN, 64, 0, 8));
    }

    @Test
    public void deadSenderIsRejected() {
        FakeSender sender = FakeSender.operator();
        sender.alive = false;
        assertReason(
            Rejection.SENDER_INACTIVE,
            Rules.forSender(sender)
                .senderActive());
    }

    @Test
    public void targetLookupChecksPresenceAndType() {
        assertReason(Rejection.TARGET_MISSING, rules().targetOfType("tile", () -> null, String.class));
        assertReason(Rejection.WRONG_TARGET_TYPE, rules().targetOfType("tile", () -> 42, String.class));
        assertNull(
            rules().targetOfType("tile", () -> "ok", String.class)
                .firstViolation());
    }

    @Test
    public void firstFailingRuleWinsAndLaterLookupsDoNotRun() {
        Rules rules = rules().indexInRange("slot", 9, 3)
            .targetOfType(
                "tile",
                () -> { throw new AssertionError("lookup ran after a failed range check"); },
                Object.class);
        assertReason(Rejection.OUT_OF_RANGE, rules);
    }

    @Test
    public void senderRulesNeedASender() {
        assertThrows(
            IllegalStateException.class,
            () -> Rules.withoutSender()
                .sameDimension(0));
        assertThrows(
            IllegalStateException.class,
            () -> Rules.withoutSender()
                .permissionLevel(2));
    }

    private static Rules rules() {
        return Rules.withoutSender();
    }

    private static void assertReason(Rejection expected, Rules rules) {
        Violation violation = rules.firstViolation();
        assertEquals(expected, violation == null ? null : violation.reason(), String.valueOf(violation));
    }
}
