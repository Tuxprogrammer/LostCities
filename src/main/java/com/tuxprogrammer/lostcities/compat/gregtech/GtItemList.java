package com.tuxprogrammer.lostcities.compat.gregtech;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;

import com.tuxprogrammer.lostcities.LostCities;

import cpw.mods.fml.common.Loader;

/**
 * Reads GregTech's {@code gregtech.api.enums.ItemList} by constant name, so the loot defaults name GT components the
 * way GT's own code does ({@code ItemList.Electric_Motor_LV.get(1)}) without raw damage values and without a compile
 * dependency on GT5-Unofficial (docs/new-spec/00-pack-integration/loot-and-mobs.md § Tier sources).
 *
 * <p>
 * Source: GT5-Unofficial 5.09.54.265, {@code ItemList.get(long, Object...)} and {@code hasBeenSet()},
 * https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/ItemList.java#L3462-L3492
 * ({@code get} returns the unified stack; a constant GT never set would log an error, so {@code hasBeenSet} is asked
 * first).
 *
 * <p>
 * Called at {@code postInit}, after GT has set its items. Side: both.
 */
public final class GtItemList {

    /** GT5-Unofficial's main mod ID. */
    public static final String MOD_ID = "gregtech";
    private static final String CLASS = "gregtech.api.enums.ItemList";

    private GtItemList() {}

    /**
     * Returns a stack of one {@code ItemList} constant.
     *
     * @param name the constant's name, such as {@code Electric_Motor_LV}
     * @return a stack of 1, or {@code null} when GT is absent, the constant does not exist or GT never set it
     */
    public static ItemStack get(String name) {
        if (!Loader.isModLoaded(MOD_ID)) {
            return null;
        }
        try {
            Class<?> type = Class.forName(CLASS);
            Object constant = constant(type, name);
            if (constant == null) {
                return null;
            }
            Method hasBeenSet = type.getMethod("hasBeenSet");
            if (!Boolean.TRUE.equals(hasBeenSet.invoke(constant))) {
                return null;
            }
            Method get = type.getMethod("get", long.class, Object[].class);
            Object stack = get.invoke(constant, 1L, new Object[0]);
            return stack instanceof ItemStack ? (ItemStack) stack : null;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
            | InvocationTargetException e) {
            LostCities.LOG.warn("GregTech's ItemList.{} could not be read: {}", name, e.toString());
            return null;
        }
    }

    private static Object constant(Class<?> type, String name) {
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name()
                .equals(name)) {
                return constant;
            }
        }
        return null;
    }
}
