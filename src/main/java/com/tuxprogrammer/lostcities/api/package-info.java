/**
 * The public API other mods may use, kept minimal and documented (docs/api.md, docs/design/0008-public-api.md): the
 * entry point {@link com.tuxprogrammer.lostcities.api.LostCitiesAPI}, per-chunk queries of what Lost Cities planned,
 * profile callbacks, and the generation events in {@code api.event}. Nothing here names a class outside this package
 * but Minecraft's, Forge's and FML's.
 *
 * <p>
 * Side: both; the queries answer on the server only.
 */
package com.tuxprogrammer.lostcities.api;
