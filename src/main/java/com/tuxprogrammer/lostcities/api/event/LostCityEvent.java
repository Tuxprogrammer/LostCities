package com.tuxprogrammer.lostcities.api.event;

import net.minecraft.world.World;

import com.tuxprogrammer.lostcities.api.ILostChunkBlocks;
import com.tuxprogrammer.lostcities.api.ILostCityInformation;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

/**
 * The generation events, posted on {@code MinecraftForge.EVENT_BUS} from the terrain phase of a chunk in a world with
 * cities (docs/api.md, Events; docs/original-spec/01-core/api.md, Events). Each carries the world, the chunk and its
 * blocks while they are written.
 *
 * <p>
 * A listener must be deterministic, a function of the world seed and the chunk, and must write only into the chunk it
 * is given, or worlds stop being reproducible and generation cascades (CONTRIBUTING § World generation rules).
 *
 * <p>
 * Deviation: upstream's {@code CharacteristicsEvent} is deferred: it would change the layout, which is pinned per
 * world and pure, and a listener would be invisible to the pin (P-CORE-3, D31).
 *
 * <p>
 * Side: server, during world generation, on the server thread.
 */
public abstract class LostCityEvent extends Event {

    private final World world;
    private final ILostCityInformation info;
    private final int chunkX;
    private final int chunkZ;
    private final ILostChunkBlocks blocks;

    /**
     * Creates an event.
     *
     * @param world  the world
     * @param info   what Lost Cities planned for it
     * @param chunkX chunk X
     * @param chunkZ chunk Z
     * @param blocks the chunk's blocks
     */
    protected LostCityEvent(World world, ILostCityInformation info, int chunkX, int chunkZ, ILostChunkBlocks blocks) {
        this.world = world;
        this.info = info;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = blocks;
    }

    /** Returns the world. Never read or write blocks through it from a listener: use {@link #getBlocks()}. */
    public World getWorld() {
        return world;
    }

    /** Returns what Lost Cities planned for the world. */
    public ILostCityInformation getLostCities() {
        return info;
    }

    /** Returns the chunk X. */
    public int getChunkX() {
        return chunkX;
    }

    /** Returns the chunk Z. */
    public int getChunkZ() {
        return chunkZ;
    }

    /** Returns the chunk's blocks. */
    public ILostChunkBlocks getBlocks() {
        return blocks;
    }

    /**
     * Before a city chunk's building or street is written; the chunk is already leveled to its city ground. Cancel it
     * to skip the building or street only.
     */
    @Cancelable
    public static class PreGenCityChunk extends LostCityEvent {

        /**
         * Creates the event.
         *
         * @param world  the world
         * @param info   what Lost Cities planned for it
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @param blocks the chunk's blocks
         */
        public PreGenCityChunk(World world, ILostCityInformation info, int chunkX, int chunkZ,
            ILostChunkBlocks blocks) {
            super(world, info, chunkX, chunkZ, blocks);
        }
    }

    /** After a city chunk's building or street, before ruins, highways, railways and damage. */
    public static class PostGenCityChunk extends LostCityEvent {

        /**
         * Creates the event.
         *
         * @param world  the world
         * @param info   what Lost Cities planned for it
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @param blocks the chunk's blocks
         */
        public PostGenCityChunk(World world, ILostCityInformation info, int chunkX, int chunkZ,
            ILostChunkBlocks blocks) {
            super(world, info, chunkX, chunkZ, blocks);
        }
    }

    /** After a chunk outside every city, blended, bridged or untouched, before highways, railways and damage. */
    public static class PostGenOutsideChunk extends LostCityEvent {

        /**
         * Creates the event.
         *
         * @param world  the world
         * @param info   what Lost Cities planned for it
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @param blocks the chunk's blocks
         */
        public PostGenOutsideChunk(World world, ILostCityInformation info, int chunkX, int chunkZ,
            ILostChunkBlocks blocks) {
            super(world, info, chunkX, chunkZ, blocks);
        }
    }

    /** Before explosion damage and debris in a chunk near a city. Cancel it to skip both. */
    @Cancelable
    public static class PreExplosion extends LostCityEvent {

        /**
         * Creates the event.
         *
         * @param world  the world
         * @param info   what Lost Cities planned for it
         * @param chunkX chunk X
         * @param chunkZ chunk Z
         * @param blocks the chunk's blocks
         */
        public PreExplosion(World world, ILostCityInformation info, int chunkX, int chunkZ, ILostChunkBlocks blocks) {
            super(world, info, chunkX, chunkZ, blocks);
        }
    }
}
