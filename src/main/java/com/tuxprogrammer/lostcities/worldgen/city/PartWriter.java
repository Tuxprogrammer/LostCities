package com.tuxprogrammer.lostcities.worldgen.city;
// Ported from Lost Cities (MIT):
// https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
// Avoids: HWY-12, ASSET-18, TERR-14

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.tuxprogrammer.lostcities.asset.AssetId;
import com.tuxprogrammer.lostcities.asset.BlockRef;
import com.tuxprogrammer.lostcities.asset.block.BlockChoice;
import com.tuxprogrammer.lostcities.asset.block.TileTemplate;
import com.tuxprogrammer.lostcities.asset.model.PaletteEntry;
import com.tuxprogrammer.lostcities.asset.model.Part;
import com.tuxprogrammer.lostcities.asset.palette.CompiledPalette;
import com.tuxprogrammer.lostcities.asset.transform.MetaRotation;
import com.tuxprogrammer.lostcities.asset.transform.Transform;
import com.tuxprogrammer.lostcities.asset.variety.PaletteVariety;
import com.tuxprogrammer.lostcities.layout.LayoutLog;
import com.tuxprogrammer.lostcities.worldgen.terrain.WorldLimits;

/**
 * Upstream's {@code generatePart}: writes one part's character columns into a chunk through its compiled palette, with
 * a transform, an offset and a hard-air setting (docs/original-spec/03-terrain/chunk-driver-and-parts.md,
 * {@code generatePart}; docs/original-spec/05-assets/parts-and-placement.md).
 *
 * <p>
 * Per character, bottom to top: a space or an {@code air} block leaves the world's block; the liquid becomes air with
 * {@code avoidWater}; hard air follows {@link HardAir} ({@code forcedair} makes it air, {@code nowater} keeps it from
 * becoming liquid); a torch character places its torch with {@code generateLighting} and becomes air without it; a
 * loot character places its container; a spawner character places its spawner with {@code generateSpawners} unless
 * the building has no loot, and air otherwise; a block with a tile-entity template places the block; saplings and
 * small flowers become air where the city style avoids foliage. Every other block is placed as the palette gives it,
 * its metadata turned with the transform by its rotation family.
 *
 * <p>
 * Changes from upstream:
 * <ul>
 * <li>Avoids HWY-12: a character the palette does not define is skipped with one log line per part and character,
 * never a crash mid-generation (CONTRIBUTING § Code standards);</li>
 * <li>avoids ASSET-18: every directional block turns with the part ({@link MetaRotation}), not only stairs, doors and
 * rails;</li>
 * <li>avoids TERR-14: a position outside the chunk is dropped, never aliased into this chunk's columns;</li>
 * <li>a template may accompany loot (asset-format.md, "Tile-entity data"); tile entities and loot are population jobs,
 * created after the terrain phase inside the populated chunk (0005 §2, §3);</li>
 * <li>saplings are placed and never grown (P03-10, D31): no global tick job exists.</li>
 * </ul>
 *
 * <p>
 * One instance per chunk write; not thread-safe. Side: both (plain Java).
 */
public final class PartWriter {

    /** The wall character, upstream's floors and ceilings in most building parts. */
    static final char WALL = '#';
    /** The floor-slab character, the floors of the larger buildings' parts. */
    static final char STREET_FLOOR = 'S';
    /** The light grid's period mask: every fourth block. */
    static final int LIGHT_GRID_MASK = 3;
    /** The light grid's offset within each period. */
    static final int LIGHT_GRID_OFFSET = 2;
    /**
     * The saplings and small flowers {@code avoidFoliage} removes: upstream's vanilla three, and the pack flowers the
     * palette variety plants in their place (D25).
     */
    static final Set<String> FOLIAGE = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "minecraft:sapling",
                "minecraft:red_flower",
                "minecraft:yellow_flower",
                "etfuturum:cornflower",
                "etfuturum:lily_of_the_valley",
                "BiomesOPlenty:flowers",
                "BiomesOPlenty:flowers2",
                "Botania:flower")));

    /** One part to write: what, with which palette and transform, where, and its context for conditions. */
    public static final class Placement {

        private final Part part;
        private final CompiledPalette palette;
        private final Transform transform;
        private final int ox;
        private final int oy;
        private final int oz;
        private final HardAir hardAir;
        private final AssetId building;
        private final boolean interior;

        /**
         * Describes a placement.
         *
         * @param part      the part
         * @param palette   the palette to write it with (the chunk's, with the part's own on top)
         * @param transform the transform
         * @param ox        local X offset, 0 for a full-chunk part
         * @param oy        block Y of the part's first slice
         * @param oz        local Z offset
         * @param hardAir   what hard air becomes
         * @param building  the building the part belongs to, or {@code null} on a street
         */
        public Placement(Part part, CompiledPalette palette, Transform transform, int ox, int oy, int oz,
            HardAir hardAir, AssetId building) {
            this(part, palette, transform, ox, oy, oz, hardAir, building, false);
        }

        private Placement(Part part, CompiledPalette palette, Transform transform, int ox, int oy, int oz,
            HardAir hardAir, AssetId building, boolean interior) {
            this.part = Objects.requireNonNull(part);
            this.palette = Objects.requireNonNull(palette);
            this.transform = Objects.requireNonNull(transform);
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.hardAir = Objects.requireNonNull(hardAir);
            this.building = building;
            this.interior = interior;
        }

        /**
         * Returns this placement marked as a building's floor part, whose interior floor, ceiling and ceiling lights
         * take the palette's interior characters ({@link PaletteVariety#FLOOR}, {@link PaletteVariety#CEILING},
         * {@link PaletteVariety#CEILING_LIGHT}) where the palette defines them.
         *
         * @param interior whether the part is a building's main floor part (not its roof, not an overlay)
         * @return the placement
         */
        public Placement interior(boolean interior) {
            return interior == this.interior ? this
                : new Placement(part, palette, transform, ox, oy, oz, hardAir, building, interior);
        }

        /** Returns whether the part is a building's floor part, written with the interior characters. */
        public boolean isInterior() {
            return interior;
        }

        /** Returns the part. */
        public Part part() {
            return part;
        }

        /** Returns the block Y of the part's first slice. */
        public int oy() {
            return oy;
        }

        /** Returns the transform. */
        public Transform transform() {
            return transform;
        }

        /** Returns the building the part belongs to, or {@code null}. */
        public AssetId building() {
            return building;
        }

        /** Returns the local X offset of the part's origin. */
        public int ox() {
            return ox;
        }

        /** Returns the local Z offset of the part's origin. */
        public int oz() {
            return oz;
        }
    }

    /** The profile and chunk facts a part write depends on. */
    public static final class Rules {

        private final boolean generateLighting;
        private final boolean generateSpawners;
        private final boolean avoidWater;
        private final boolean avoidFoliage;
        private final boolean noLoot;
        private final int waterLevel;
        private final BlockRef liquid;

        /**
         * Collects the rules.
         *
         * @param generateLighting the profile's {@code generateLighting}
         * @param generateSpawners the profile's {@code generateSpawners}
         * @param avoidWater       the profile's {@code avoidWater}
         * @param avoidFoliage     the city's {@code avoidFoliage}
         * @param noLoot           whether the building has no loot (no spawners either)
         * @param waterLevel       the water level, block Y
         * @param liquid           the profile's liquid block
         */
        public Rules(boolean generateLighting, boolean generateSpawners, boolean avoidWater, boolean avoidFoliage,
            boolean noLoot, int waterLevel, BlockRef liquid) {
            this.generateLighting = generateLighting;
            this.generateSpawners = generateSpawners;
            this.avoidWater = avoidWater;
            this.avoidFoliage = avoidFoliage;
            this.noLoot = noLoot;
            this.waterLevel = waterLevel;
            this.liquid = liquid;
        }

        /** Returns whether spawner characters place spawners here. */
        public boolean spawners() {
            return generateSpawners && !noLoot;
        }

        /** Returns whether the building has no loot. */
        public boolean noLoot() {
            return noLoot;
        }
    }

    private final CityTarget target;
    private final PaletteSequence sequence;
    private final Rules rules;
    private final LayoutLog log;
    private java.util.function.Consumer<Placement> listener;

    /**
     * Creates the writer of one chunk.
     *
     * @param target   where blocks and jobs go
     * @param sequence the chunk's palette sequence
     * @param rules    the profile and chunk facts
     * @param log      where an undefined character is reported, once per part and character
     */
    public PartWriter(CityTarget target, PaletteSequence sequence, Rules rules, LayoutLog log) {
        this.target = target;
        this.sequence = sequence;
        this.rules = rules;
        this.log = log;
    }

    /**
     * Writes a part.
     *
     * @param placement the part and where it goes
     * @return the Y above the part's last slice (upstream's return value)
     */
    public int place(Placement placement) {
        if (listener != null) {
            listener.accept(placement);
        }
        Part part = placement.part;
        CompiledPalette palette = placement.palette;
        boolean nowater = part.metaBoolean(Part.MetaKey.NOWATER);
        boolean forcedAir = part.metaBoolean(Part.MetaKey.FORCEDAIR);
        boolean floors = placement.interior && palette.isDefined(PaletteVariety.FLOOR);
        boolean ceilings = placement.interior && palette.isDefined(PaletteVariety.CEILING);
        boolean lights = placement.interior && palette.isDefined(PaletteVariety.CEILING_LIGHT);
        boolean interior = floors || ceilings || lights;
        for (int x = 0; x < part.xsize(); x++) {
            for (int z = 0; z < part.zsize(); z++) {
                char[] column = part.column(x, z);
                if (column == null) {
                    continue;
                }
                int rx = placement.ox + placement.transform.rotateX(x, z);
                int rz = placement.oz + placement.transform.rotateZ(x, z);
                if (rx < 0 || rx > 15 || rz < 0 || rz > 15) {
                    // Avoids TERR-14: never alias a position of another chunk into this one.
                    continue;
                }
                for (int y = 0; y < column.length; y++) {
                    char c = column[y];
                    if (c == ' ') {
                        continue;
                    }
                    if (interior) {
                        c = interiorCharacter(column, y, x, z, floors, ceilings, lights);
                    }
                    int worldY = placement.oy + y;
                    if (worldY < WorldLimits.MIN_Y || worldY > WorldLimits.MAX_Y) {
                        continue;
                    }
                    placeOne(placement, palette, c, rx, worldY, rz, nowater, forcedAir);
                }
            }
        }
        return placement.oy + part.height();
    }

    /**
     * Writes a part with each of its X slices raised by its own number of blocks: a highway ramp, whose straight part
     * climbs along its axis (M06, design record 0009). Part-local X is the axis the shifts follow, before the
     * transform.
     *
     * @param placement the part and where it goes
     * @param shifts    blocks each part-local X slice is raised by, indexed by part-local X
     * @return the Y above the part's last slice at its lowest column
     */
    public int placeShifted(Placement placement, int[] shifts) {
        if (listener != null) {
            listener.accept(placement);
        }
        Part part = placement.part;
        CompiledPalette palette = placement.palette;
        boolean nowater = part.metaBoolean(Part.MetaKey.NOWATER);
        boolean forcedAir = part.metaBoolean(Part.MetaKey.FORCEDAIR);
        for (int x = 0; x < part.xsize(); x++) {
            int shift = x < shifts.length ? shifts[x] : 0;
            for (int z = 0; z < part.zsize(); z++) {
                char[] column = part.column(x, z);
                if (column == null) {
                    continue;
                }
                int rx = placement.ox + placement.transform.rotateX(x, z);
                int rz = placement.oz + placement.transform.rotateZ(x, z);
                if (rx < 0 || rx > 15 || rz < 0 || rz > 15) {
                    continue;
                }
                for (int y = 0; y < column.length; y++) {
                    char c = column[y];
                    if (c == ' ') {
                        continue;
                    }
                    int worldY = placement.oy + shift + y;
                    if (worldY < WorldLimits.MIN_Y || worldY > WorldLimits.MAX_Y) {
                        continue;
                    }
                    placeOne(placement, palette, c, rx, worldY, rz, nowater, forcedAir);
                }
            }
        }
        return placement.oy + part.height();
    }

    /**
     * Sets what is told of each part before it is written: the part editor's recorder in an {@code editMode} world.
     *
     * @param value the listener, or {@code null} for none
     */
    public void onPlace(java.util.function.Consumer<Placement> value) {
        listener = value;
    }

    private void placeOne(Placement placement, CompiledPalette palette, char c, int x, int y, int z, boolean nowater,
        boolean forcedAir) {
        if (!palette.isDefined(c)) {
            // Avoids HWY-12: report and skip instead of throwing mid-generation.
            log.warn(
                "part " + placement.part.id()
                    + " uses character '"
                    + c
                    + "', which its palette does not define; those blocks are skipped");
            return;
        }
        BlockChoice choice = palette.isRandom(c) ? palette.choice(c, sequence.next()) : palette.choice(c, 0);
        if (choice.isHardAir()) {
            hardAir(placement, x, y, z, nowater, forcedAir);
            return;
        }
        BlockRef block = choice.block(palette.flags());
        if (placement.transform != Transform.ROTATE_NONE) {
            block = MetaRotation.rotate(block, choice.family(), placement.transform);
        }
        if (block.isAir()) {
            // Upstream: air in a part leaves the world's block.
            return;
        }
        if (block.equals(rules.liquid)) {
            target.set(x, y, z, rules.avoidWater ? BlockRef.AIR : block);
            return;
        }
        PaletteEntry info = palette.info(c);
        if (info != null && info.torch()) {
            if (rules.generateLighting) {
                target.set(x, y, z, block);
                target.torch(x, y, z);
            } else {
                target.set(x, y, z, BlockRef.AIR);
            }
            return;
        }
        TileTemplate template = palette.template(c, choice);
        if (info != null && info.loot() != null) {
            target.set(x, y, z, block);
            target.chest(x, y, z, info, template, placement.transform, placement);
            return;
        }
        if (info != null && info.mob() != null) {
            if (rules.spawners()) {
                target.set(x, y, z, block);
                target.spawner(x, y, z, info, placement);
            } else {
                target.set(x, y, z, BlockRef.AIR);
            }
            return;
        }
        if (template != null) {
            target.set(x, y, z, block);
            target.tileEntity(x, y, z, template, placement.transform);
            return;
        }
        if (rules.avoidFoliage && isFoliage(block)) {
            target.set(x, y, z, BlockRef.AIR);
            return;
        }
        target.set(x, y, z, block);
    }

    private void hardAir(Placement placement, int x, int y, int z, boolean nowater, boolean forcedAir) {
        if (forcedAir) {
            target.set(x, y, z, BlockRef.AIR);
            return;
        }
        switch (placement.hardAir) {
            case AIR:
                target.set(x, y, z, BlockRef.AIR);
                break;
            case WATERLEVEL:
                boolean water = !rules.avoidFoliage && !nowater && y < rules.waterLevel;
                target.set(x, y, z, water ? rules.liquid : BlockRef.AIR);
                break;
            default:
                // VOID: whatever is already there stays.
                break;
        }
    }

    /**
     * Returns the character a building floor part's block takes (deviation, AR-3 and D25: upstream's interiors are its
     * wall block throughout). In one column of the part, a floor block ({@code #} or {@code S}) with air right above it
     * and a solid character somewhere above that air is an interior floor; a wall block ({@code #}) with air right
     * below
     * it and a solid character somewhere below that air is a ceiling, and a light where the column is on the light
     * grid.
     * A space is the part's air: building chunks are cleared before their floors are written. Roofs (no ceiling above),
     * courtyards open to the sky and walls (no air) keep their characters.
     *
     * @param column   the part's column, bottom to top
     * @param y        the index in the column
     * @param x        part-local X, for the light grid
     * @param z        part-local Z
     * @param floors   whether the palette defines the floor character
     * @param ceilings whether the palette defines the ceiling character
     * @param lights   whether the palette defines the ceiling-light character
     * @return the character to write
     */
    static char interiorCharacter(char[] column, int y, int x, int z, boolean floors, boolean ceilings,
        boolean lights) {
        char c = column[y];
        if (floors && (c == WALL || c == STREET_FLOOR)
            && y + 1 < column.length
            && column[y + 1] == ' '
            && solidAbove(column, y + 1)) {
            return PaletteVariety.FLOOR;
        }
        if ((ceilings || lights) && c == WALL && y >= 1 && column[y - 1] == ' ' && solidBelow(column, y - 1)) {
            if (lights && onLightGrid(x, z)) {
                return PaletteVariety.CEILING_LIGHT;
            }
            return ceilings ? PaletteVariety.CEILING : c;
        }
        return c;
    }

    /**
     * Returns whether a part-local column is on the ceiling-light grid: every fourth block on both axes, two in from
     * the
     * part's edge, so a 16-block floor has lights at 2, 6, 10 and 14 (one per four-by-four bay).
     */
    static boolean onLightGrid(int x, int z) {
        return (x & LIGHT_GRID_MASK) == LIGHT_GRID_OFFSET && (z & LIGHT_GRID_MASK) == LIGHT_GRID_OFFSET;
    }

    private static boolean solidAbove(char[] column, int from) {
        for (int k = from; k < column.length; k++) {
            if (column[k] != ' ') {
                return true;
            }
        }
        return false;
    }

    private static boolean solidBelow(char[] column, int from) {
        for (int k = from; k >= 0; k--) {
            if (column[k] != ' ') {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether a block is a sapling or a small flower: what {@code avoidFoliage} removes from parts
     * (upstream's {@code SaplingBlock} and {@code FlowerBlock} test).
     *
     * @param block the block
     * @return whether it is foliage the city may avoid
     */
    static boolean isFoliage(BlockRef block) {
        return FOLIAGE.contains(block.name());
    }
}
