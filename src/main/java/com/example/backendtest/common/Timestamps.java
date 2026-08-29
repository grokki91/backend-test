package com.example.backendtest.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Postgres stores timestamps at microsecond precision. Truncating before we persist keeps
 * the value returned by a write equal to the value a later read gives back.
 */
public final class Timestamps {

    private Timestamps() {
    }

    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
