package com.tuxprogrammer.lostcities.mixins.late;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tuxprogrammer.lostcities.worldgen.overlay.StoneGuard;

/**
 * Keeps GregTech's stone boulders (granite, basalt and the other {@code WorldgenStone} blobs) out of the footprint:
 * the generator replaces stone, cobblestone, stained clay and the like in a sphere with {@code World.setBlock}, so a
 * boulder cut into a building's walls, floors and cellar (0.1.0-beta known issue). GT's world generator runs after
 * every other, after our closing tree generator, because its {@code GameRegistryMixin} moves it to the end of Forge's
 * list (GT5-Unofficial@5.09.54.265:src/mixin/java/gregtech/mixin/mixins/early/forge/GameRegistryMixin.java), so no
 * frame from {@code TreeGuard} covers it. This mixin brackets each {@code executeWorldgen} with a footprint frame
 * instead (GT5-Unofficial@5.09.54.265:src/main/java/gregtech/common/WorldgenStone.java#L69-L240; the write is the
 * {@code aWorld.setBlock} at L230). GT's ore veins and small ores are a different class and pass.
 *
 * <p>
 * A late mixin, applied only when GregTech is loaded ({@code compat.LateMixinLoader}); the target is named by string
 * so nothing here links GT, and {@code @Pseudo} lets the build pass without GT on the compile classpath. Side: both (mixin loading), acting on the server only.
 */
@Pseudo
@Mixin(targets = "gregtech.common.WorldgenStone", remap = false)
public abstract class MixinGtWorldgenStone {

    @Inject(method = "executeWorldgen", at = @At("HEAD"), remap = false)
    private void lostcitiesnh$open(World world, Random random, String biome, int chunkX, int chunkZ,
        IChunkProvider generator, IChunkProvider provider, CallbackInfoReturnable<Boolean> callback) {
        StoneGuard.open(world);
    }

    @Inject(method = "executeWorldgen", at = @At("RETURN"), remap = false)
    private void lostcitiesnh$close(World world, Random random, String biome, int chunkX, int chunkZ,
        IChunkProvider generator, IChunkProvider provider, CallbackInfoReturnable<Boolean> callback) {
        StoneGuard.close(world);
    }
}
