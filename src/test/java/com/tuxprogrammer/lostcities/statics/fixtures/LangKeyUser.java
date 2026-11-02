package com.tuxprogrammer.lostcities.statics.fixtures;

/**
 * Fixture for {@code LangKeysTest}: one key-shaped constant no lang file defines, and identifiers that are not keys.
 */
public class LangKeyUser {

    /** A JVM property name: camelCase, so not a key. */
    public static final String PROPERTY = "lostcitiesnh.testHooks";

    /** Returns a key that is missing from every lang file. */
    public String key() {
        return "lostcitiesnh.fixture.missing_key";
    }
}
