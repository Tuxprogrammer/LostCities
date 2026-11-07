package com.tuxprogrammer.lostcities.mixins.late;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.tuxprogrammer.lostcities.compat.witchery.WitcheryVeto;

/**
 * The Witchery veto (decision log D-landmark-policy-3): skips Witchery's overworld structure pass for a chunk whose
 * structure would stand on a city chunk or a scattered footprint ({@code compat.witchery.WitcheryVeto}), since the city
 * builds over Witchery's lots by default.
 *
 * <p>
 * Why a mixin: Witchery's {@code WitcheryWorldGenerator} is an {@code IWorldGenerator}; it fires no event, Forge's
 * populate events do not cover {@code IWorldGenerator}s, and its overworld pass places blocks with
 * {@code World.setBlock}, which the footprint guard's tree frame lets through. Its private
 * {@code generateOverworld(World, Random, int, int)} is the overworld pass alone (the dream dimension's flowers are a
 * different method), so cancelling it at the head skips one chunk's structure and nothing else (Decompiled:
 * witchery-1.7.10-0.24.1.jar (GTNH 2.9.0); reference copy
 * https://github.com/GTNH-Museum/Witchery/blob/4b7a98b60de7901e3503247016aa90c90099d0ce/src/main/java/com/emoniph/witchery/worldgen/WitcheryWorldGenerator.java#L55-L96).
 *
 * <p>
 * A late mixin, applied only when Witchery is loaded ({@code compat.LateMixinLoader}). Witchery is closed source with
 * no development jar, so the target is named by string and {@code @Pseudo} lets the build pass without it; its own
 * classes are not obfuscated, so nothing is remapped. Side: both (mixin loading), acting on the server only.
 */
@Pseudo
@Mixin(targets = "com.emoniph.witchery.worldgen.WitcheryWorldGenerator", remap = false)
public abstract class MixinWitcheryWorldGenerator {

    @Inject(method = "generateOverworld", at = @At("HEAD"), cancellable = true, remap = false)
    private void lostcitiesnh$veto(World world, Random random, int x, int z, CallbackInfo callback) {
        if (!world.isRemote && WitcheryVeto.skip(world, x, z)) {
            callback.cancel();
        }
    }
}
