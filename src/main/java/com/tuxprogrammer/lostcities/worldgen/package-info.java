/**
 * Minecraft glue for world generation: the chunk-provider overlay, terrain, writing parts into chunks and population
 * (docs/design/0005-rwg-overlay.md). It stays thin: it asks {@code layout} what goes in a chunk and writes it through
 * {@link com.tuxprogrammer.lostcities.worldgen.buffer.ChunkBuffer}. M01 provides the buffer and its digest; the overlay
 * arrives in M04.
 *
 * <p>
 * Side: server.
 */
package com.tuxprogrammer.lostcities.worldgen;
