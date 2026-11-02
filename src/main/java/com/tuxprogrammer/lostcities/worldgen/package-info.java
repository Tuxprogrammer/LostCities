/**
 * Minecraft glue for world generation: the chunk-provider overlay, terrain, writing parts into chunks and population
 * (docs/design/0005-rwg-overlay.md). It stays thin: it asks {@code layout} what goes in a chunk and writes it through
 * {@link com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer}. Subpackages: {@code buffer} (the chunk buffer and
 * its digest), {@code plan} (the plan interface the overlay reads), {@code terrain} and {@code damage} (the terrain
 * phase, pure Java), {@code height} (oracle heights), {@code timing}, {@code synthetic} (the M04 test city) and
 * {@code overlay} (the wrapper, the population phase, the write guard and saved data).
 *
 * <p>
 * Side: server.
 */
package com.tuxprogrammer.lostcities.worldgen;
