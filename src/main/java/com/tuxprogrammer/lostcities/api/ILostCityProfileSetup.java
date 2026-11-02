package com.tuxprogrammer.lostcities.api;

/**
 * Handed to a profile callback ({@link ILostCities#registerProfileSetup}): creates profiles from a base profile
 * (docs/original-spec/01-core/api.md, {@code ILostCityProfileSetup}).
 *
 * <p>
 * Side: server.
 */
public interface ILostCityProfileSetup {

    /**
     * Creates a profile as a copy of a base profile. Set its options on the returned object before the callback
     * returns; the profile is then checked like a profile file and, if valid, can be pinned.
     *
     * @param name        the new profile's name: lower-case letters, digits and {@code _}, not a built-in's or a
     *                    file's name
     * @param baseProfile the profile to copy: a built-in or a file profile
     * @return the new profile, to configure
     * @throws IllegalArgumentException for a bad or taken name or an unknown base profile
     */
    ILostCityProfile createProfile(String name, String baseProfile);
}
