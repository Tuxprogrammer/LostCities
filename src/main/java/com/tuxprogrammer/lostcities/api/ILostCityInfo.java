package com.tuxprogrammer.lostcities.api;

/**
 * A city, as its center chunk reports it (docs/original-spec/01-core/api.md, {@code ILostCityInfo}).
 *
 * <p>
 * Side: server.
 */
public interface ILostCityInfo {

    /** Returns the city's radius, in blocks. */
    float getCityRadius();

    /** Returns the city style of the city's center. */
    String getCityStyle();
}
