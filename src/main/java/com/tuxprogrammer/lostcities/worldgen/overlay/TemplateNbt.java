package com.tuxprogrammer.lostcities.worldgen.overlay;

import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.tuxprogrammer.lostcities.LostCities;
import com.tuxprogrammer.lostcities.asset.json.JsonFields;

/**
 * Turns a palette entry's tile-entity template, its 1.7.10 NBT written as JSON with items named by registry name,
 * into NBT and applies it to a tile entity (docs/new-spec/05-assets/asset-format.md, "Tile-entity data";
 * block-mapping.md § How the mapping works).
 *
 * <p>
 * Types follow 1.7.10's readers: in an item stack (a compound inside a list) {@code id} is the item's numeric ID as a
 * short, {@code Count} and {@code Slot} bytes and {@code Damage} a short ({@code ItemStack.readFromNBT}); a top-level
 * {@code Item} naming an item is its numeric ID as an int ({@code TileEntityFlowerPot}). Other whole numbers are ints,
 * other numbers doubles, booleans bytes, strings strings, objects compounds and arrays lists. An item name the
 * registry does not know drops its stack, with one log line. The template is merged over the tile entity's own NBT, so
 * its position and id stay.
 *
 * <p>
 * Server thread only. Side: server.
 */
public final class TemplateNbt {

    private TemplateNbt() {}

    /**
     * Applies a template to a tile entity.
     *
     * @param tile     the tile entity
     * @param template the template's JSON text; nothing happens when it is empty
     * @return whether the template was applied
     */
    public static boolean apply(TileEntity tile, String template) {
        if (template.isEmpty()) {
            return false;
        }
        JsonObject json;
        try {
            json = JsonFields.parseStrict(template)
                .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            LostCities.LOG.error("Tile-entity template is not a JSON object: {}", template);
            return false;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tile.writeToNBT(tag);
        NBTTagCompound overlay = compound(json, false);
        for (Object key : overlay.func_150296_c()) {
            String name = (String) key;
            tag.setTag(name, overlay.getTag(name));
        }
        tile.readFromNBT(tag);
        // No markDirty: its comparator update reads the neighbors, which on a chunk's edge loads the next chunk; the
        // caller marks the chunk modified instead.
        return true;
    }

    /**
     * Converts a template object.
     *
     * @param json      the object
     * @param itemStack whether it is an item stack (a compound inside a list), whose {@code id} is an item
     * @return the compound
     */
    static NBTTagCompound compound(JsonObject json, boolean itemStack) {
        NBTTagCompound out = new NBTTagCompound();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            NBTBase value = value(entry.getKey(), entry.getValue(), itemStack);
            if (value != null) {
                out.setTag(entry.getKey(), value);
            }
        }
        return out;
    }

    private static NBTBase value(String key, JsonElement element, boolean itemStack) {
        if (element.isJsonObject()) {
            return compound(element.getAsJsonObject(), false);
        }
        if (element.isJsonArray()) {
            NBTTagList list = new NBTTagList();
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonObject()) {
                    NBTTagCompound stack = compound(child.getAsJsonObject(), true);
                    if (stack.hasKey("id") || !child.getAsJsonObject()
                        .has("id")) {
                        list.appendTag(stack);
                    }
                } else {
                    NBTBase value = value(key, child, false);
                    if (value != null) {
                        list.appendTag(value);
                    }
                }
            }
            return list;
        }
        if (!element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (itemStack) {
            switch (key) {
                case "id":
                    return primitive.isString() ? itemId(primitive.getAsString(), true)
                        : new NBTTagShort(primitive.getAsShort());
                case "Count":
                case "Slot":
                    return new NBTTagByte(primitive.getAsByte());
                case "Damage":
                    return new NBTTagShort(primitive.getAsShort());
                default:
                    break;
            }
        }
        if ("Item".equals(key) && primitive.isString()) {
            return itemId(primitive.getAsString(), false);
        }
        if (primitive.isBoolean()) {
            return new NBTTagByte((byte) (primitive.getAsBoolean() ? 1 : 0));
        }
        if (primitive.isNumber()) {
            double number = primitive.getAsDouble();
            if (number == Math.rint(number) && Math.abs(number) <= Integer.MAX_VALUE) {
                return new NBTTagInt((int) number);
            }
            return new NBTTagDouble(number);
        }
        return new NBTTagString(primitive.getAsString());
    }

    private static NBTBase itemId(String name, boolean asShort) {
        Object item = Item.itemRegistry.getObject(name);
        if (!(item instanceof Item)) {
            LostCities.LOG.warn("Tile-entity template names item {}, which is not registered; dropped", name);
            return null;
        }
        int id = Item.getIdFromItem((Item) item);
        return asShort ? new NBTTagShort((short) id) : new NBTTagInt(id);
    }
}
