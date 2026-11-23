package com.tuxprogrammer.lostcities.layout.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Exclusive spans and the split line of {@link PerfSpans} (D-perf3-1). */
public class PerfSpansTest {

    private final Map<String, Long> closed = new LinkedHashMap<>();
    private final PerfSpans.Sink sink = (name, nanos) -> closed.merge(name, nanos, Long::sum);

    @Test
    void nestedSpansAreTakenOutOfTheirParent() {
        PerfSpans.Stack stack = new PerfSpans.Stack();
        stack.enter("load", 0);
        stack.enter("provide", 10);
        stack.exit("provide", 40, sink);
        stack.enter("populate", 50);
        stack.enter("load", 60);
        stack.exit("load", 70, sink);
        stack.exit("populate", 90, sink);
        stack.exit("load", 100, sink);
        assertEquals(30L, closed.get("provide"));
        assertEquals(30L, closed.get("populate"));
        // The root's 100 less provide 30 and populate 40; the nested load's own 10 adds to its total.
        assertEquals(30L + 10L, closed.get("load"));
        long sum = closed.values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();
        assertEquals(100L, sum, "the exclusive parts add up to the root");
        assertEquals(0, stack.depth());
    }

    @Test
    void anUnclosedInnerSpanClosesWithItsParent() {
        PerfSpans.Stack stack = new PerfSpans.Stack();
        stack.enter("outer", 0);
        stack.enter("inner", 10);
        stack.exit("outer", 50, sink);
        assertEquals(40L, closed.get("inner"));
        assertEquals(10L, closed.get("outer"));
        assertEquals(0, stack.depth());
    }

    @Test
    void anExitWithNoOpenSpanIsIgnored() {
        PerfSpans.Stack stack = new PerfSpans.Stack();
        stack.exit("nothing", 5, sink);
        assertEquals(0, closed.size());
    }

    @Test
    void splitNamesTheRest() {
        String line = PerfSpans.split(1_000_000_000L, new String[] { "a", "b" }, new long[] { 600_000_000L, 300_000_000L });
        assertEquals("1000.0 ms = a 600.0 (60.0 %) + b 300.0 (30.0 %) + outside 100.0 (10.0 %)", line);
    }
}
