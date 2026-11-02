package com.tuxprogrammer.lostcities.network.validation;

import java.util.ArrayList;
import java.util.List;

/** A {@link RejectionSink} that remembers every report, for tests. */
public final class RecordingSink implements RejectionSink {

    public final List<Violation> rejected = new ArrayList<>();
    public final List<RuntimeException> failures = new ArrayList<>();

    @Override
    public void rejected(String packet, String origin, Violation violation) {
        rejected.add(violation);
    }

    @Override
    public void handlerFailed(String packet, String origin, RuntimeException thrown) {
        failures.add(thrown);
    }
}
