package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.worldgen.terrain.TerrainPhase;
import com.tuxprogrammer.lostcities.worldgen.timing.GenerationTimers;

/**
 * The overlay's hook: a delegating {@code IChunkProvider} that replaces {@code ChunkProviderServer
 * .currentChunkProvider} of a wrapped dimension (docs/design/0005-rwg-overlay.md §1). {@code provideChunk} calls the
 * delegate (RWG2 or RWG), then runs the terrain phase on the returned chunk; {@code populate} calls the delegate with
 * the footprint write guard active, then runs the population phase. Forge runs the {@code IWorldGenerator}s afterwards,
 * GT's ore veins last, so the overlay sees all of RWG's terrain before writing and all of RWG's decoration before
 * populating, the order upstream 1.20 has over vanilla.
 *
 * <p>
 * Every other method delegates unchanged; {@code makeString} returns the delegate's string. {@code loadChunk} goes
 * through {@link #provideChunk}, as the vanilla and RWG generators' own {@code loadChunk} does, so no path skips the
 * terrain phase. The wrapper never wraps another wrapper.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class OverlayChunkProvider implements IChunkProvider {

    private final IChunkProvider delegate;
    private final OverlayWorld overlay;
    private final World world;

    OverlayChunkProvider(IChunkProvider delegate, OverlayWorld overlay) {
        if (delegate instanceof OverlayChunkProvider) {
            throw new IllegalArgumentException("refusing to wrap the overlay's own provider");
        }
        this.delegate = delegate;
        this.overlay = overlay;
        this.world = overlay.world();
    }

    /** Returns the wrapped provider. */
    public IChunkProvider delegate() {
        return delegate;
    }

    /** Returns the world's overlay state. */
    public OverlayWorld overlay() {
        return overlay;
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        long start = System.nanoTime();
        Chunk chunk = delegate.provideChunk(chunkX, chunkZ);
        long generated = System.nanoTime();
        TerrainPhase.ChunkClass chunkClass = chunk == null ? TerrainPhase.ChunkClass.UNTOUCHED
            : overlay.terrain(chunk);
        long done = System.nanoTime();
        GenerationTimers timers = overlay.timers();
        timers.record(GenerationTimers.Series.PROVIDE, chunkClass, generated - start);
        timers.record(GenerationTimers.Series.TERRAIN, chunkClass, done - generated);
        return chunk;
    }

    @Override
    public Chunk loadChunk(int chunkX, int chunkZ) {
        return provideChunk(chunkX, chunkZ);
    }

    @Override
    public void populate(IChunkProvider provider, int chunkX, int chunkZ) {
        TerrainPhase.ChunkClass chunkClass = overlay.populationClass(chunkX, chunkZ);
        long start = System.nanoTime();
        boolean pushed = FootprintGuard.push(world, overlay.plan(), true);
        if (!pushed) {
            LostCities.LOG.warn("Population nested too deep at chunk ({}, {}); the footprint guard is off", chunkX, chunkZ);
        }
        try {
            delegate.populate(provider, chunkX, chunkZ);
        } finally {
            if (pushed) {
                FootprintGuard.pop();
            }
        }
        long populated = System.nanoTime();
        boolean suspended = FootprintGuard.push(world, overlay.plan(), false);
        try {
            overlay.population(chunkX, chunkZ);
        } finally {
            if (suspended) {
                FootprintGuard.pop();
            }
        }
        long done = System.nanoTime();
        GenerationTimers timers = overlay.timers();
        timers.record(GenerationTimers.Series.POPULATE, chunkClass, populated - start);
        timers.record(GenerationTimers.Series.POPULATION, chunkClass, done - populated);
    }

    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return delegate.chunkExists(chunkX, chunkZ);
    }

    @Override
    public boolean saveChunks(boolean all, IProgressUpdate progress) {
        return delegate.saveChunks(all, progress);
    }

    @Override
    public boolean unloadQueuedChunks() {
        return delegate.unloadQueuedChunks();
    }

    @Override
    public boolean canSave() {
        return delegate.canSave();
    }

    @Override
    public String makeString() {
        return delegate.makeString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BiomeGenBase.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
        return delegate.getPossibleCreatures(type, x, y, z);
    }

    @Override
    public ChunkPosition func_147416_a(World world, String structure, int x, int y, int z) {
        return delegate.func_147416_a(world, structure, x, y, z);
    }

    @Override
    public int getLoadedChunkCount() {
        return delegate.getLoadedChunkCount();
    }

    @Override
    public void recreateStructures(int chunkX, int chunkZ) {
        delegate.recreateStructures(chunkX, chunkZ);
    }

    @Override
    public void saveExtraData() {
        delegate.saveExtraData();
    }
}
