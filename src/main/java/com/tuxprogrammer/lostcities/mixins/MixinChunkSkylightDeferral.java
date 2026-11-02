package com.tuxprogrammer.lostcities.mixins;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tuxprogrammer.lostcities.worldgen.overlay.SkylightDeferral;

/**
 * Skips the delegate's sky light pass on the chunk the overlay's provider is generating; the provider lights the
 * chunk once after the terrain phase ({@link SkylightDeferral}). The injection is at the head and only cancels; when
 * no deferral is active it costs one static field read. Side: both (the class is common), active on the server only.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkSkylightDeferral {

    @Shadow
    public World worldObj;

    @Shadow
    @Final
    public int xPosition;

    @Shadow
    @Final
    public int zPosition;

    @Inject(method = "generateSkylightMap", at = @At("HEAD"), cancellable = true)
    private void lostcitiesnh$deferSkylight(CallbackInfo callback) {
        if (SkylightDeferral.skips(worldObj, xPosition, zPosition)) {
            callback.cancel();
        }
    }
}
