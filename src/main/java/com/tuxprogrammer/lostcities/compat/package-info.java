/**
 * Optional integrations, one subpackage per mod ({@code compat.rwg2}, {@code compat.rwg}, and so on), each guarded by
 * {@code Loader.isModLoaded} or {@code @Optional} so the mod boots without them (CONTRIBUTING § GTNH integration).
 * {@link com.tuxprogrammer.lostcities.compat.HeightOracles} picks a world's height oracle.
 *
 * <p>
 * Side: both.
 */
package com.tuxprogrammer.lostcities.compat;
