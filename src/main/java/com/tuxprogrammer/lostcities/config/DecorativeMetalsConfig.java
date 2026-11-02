package com.tuxprogrammer.lostcities.config;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.block.DecorativeMetal;
import com.tuxprogrammer.lostcities.asset.block.MetalFlags;

/**
 * The {@code decorativeMetals} category of {@code config/lostcitiesnh.cfg}: one flag per decorative metal at or below
 * EV (decision log D29; docs/new-spec/05-assets/block-catalog/index.md, "Decorative metals (D29)"). A flag on lets that
 * metal's blocks appear as capped accents in palettes; off, each entry naming the metal takes its fallback. The flags
 * change palettes only, never what is registered, because the mod registers no content.
 *
 * <p>
 * Every key changes what a world generates, so the category is marked {@code @Config.RequiresWorldRestart} and its
 * comments say "new worlds only": the flags are pinned into a world's profile when it is created (D29), by M04's
 * profile pinning. Each field is named after its metal's {@link DecorativeMetal#key()}, so {@link #flags()} reads them
 * by name; {@code DecorativeMetalsConfigTest} keeps the two in step. Lang keys: {@code lostcitiesnh.decorativemetals.
 * <field in lowercase>}, as {@link WorldgenConfig} explains.
 *
 * <p>
 * Side: both; the values matter only on the server.
 */
@Config(modid = LostCities.MODID, category = DecorativeMetalsConfig.CATEGORY)
@Config.LangKeyPattern
@Config.RequiresWorldRestart
@Config.Comment({ "Decorative metals: one switch per metal at or below EV. On, the metal appears as small accents in",
    "buildings; off, those accents use another block. New worlds only: the values are pinned into a world when it is",
    "created." })
public final class DecorativeMetalsConfig {

    /** The category name, also the middle part of every lang key here. Not an option: GTNHLib reads every field. */
    @Config.Ignore
    public static final String CATEGORY = "decorativeMetals";

    @Config.Comment("Iron accents, and the gas station's iron canopy and pumps. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean iron = true;

    @Config.Comment("Gold accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean gold = true;

    @Config.Comment("Cast iron accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean castIron = true;

    @Config.Comment("Copper accents, and the cabin's copper walls and stairs. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean copper = true;

    @Config.Comment("Tin accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean tin = true;

    @Config.Comment("Bronze accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean bronze = true;

    @Config.Comment("Lead accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean lead = true;

    @Config.Comment("Silver accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean silver = true;

    @Config.Comment("Nickel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean nickel = true;

    @Config.Comment("Invar accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean invar = true;

    @Config.Comment("Electrum accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean electrum = true;

    @Config.Comment("Steel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean steel = true;

    @Config.Comment("Black steel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean blackSteel = true;

    @Config.Comment("Damascus steel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean damascusSteel = true;

    @Config.Comment("Meteoric iron accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean meteoricIron = true;

    @Config.Comment("Electrical steel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean electricalSteel = true;

    @Config.Comment("Dark steel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean darkSteel = true;

    @Config.Comment("Aluminum accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean aluminum = true;

    @Config.Comment("Aluminum brass accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean aluminumBrass = true;

    @Config.Comment("Cobalt accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean cobalt = true;

    @Config.Comment("Stainless steel accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean stainlessSteel = true;

    @Config.Comment("Titanium accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean titanium = true;

    @Config.Comment("Desh accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean desh = true;

    @Config.Comment("Thaumium accents. New worlds only.")
    @Config.DefaultBoolean(true)
    public static boolean thaumium = true;

    private DecorativeMetalsConfig() {}

    /**
     * Returns the flags as the config holds them now. A world's generation uses the flags pinned into it at creation,
     * which M04 takes from here.
     *
     * @return the flags
     * @throws IllegalStateException when a metal has no field, a programming error the unit test catches
     */
    public static MetalFlags flags() {
        Set<DecorativeMetal> on = EnumSet.noneOf(DecorativeMetal.class);
        for (DecorativeMetal metal : DecorativeMetal.values()) {
            try {
                Field field = DecorativeMetalsConfig.class.getField(metal.key());
                if (field.getBoolean(null)) {
                    on.add(metal);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException("decorativeMetals has no field for " + metal.key(), e);
            }
        }
        return MetalFlags.of(on);
    }
}
