package com.tuxprogrammer.lostcities.mixins;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tuxprogrammer.lostcities.worldgen.overlay.FootprintGuard;

/**
 * The footprint write guard (docs/design/0005-rwg-overlay.md §3a): drops a block write inside a city's footprint
 * while the wrapped world's delegate runs {@code populate}.
 *
 * <p>
 * Why no event works: RWG2 and classic RWG fire {@code DecorateBiomeEvent.Decorate} for their trees, boulders and
 * ruins but ignore its result ({@code TerrainGen.decorate}'s return value is never read), and mineshaft and stronghold
 * pieces fire nothing; every one of those writes reaches the world through {@code Chunk.func_150807_a}, the chunk's
 * block setter. Sources: RWG2 2.0.6 ChunkGeneratorRealistic.java#L984-L1038 and RWG alpha-1.5.3
 * ChunkGeneratorRealistic.java#L643-L682 (docs/new-spec/00-pack-integration/rwg.md).
 *
 * <p>
 * The injection is at the head and only cancels, so it composes with ArchaicFix's redirects inside the same method
 * (MixinChunkVanilla, archaicfix 0.8.2). When no population runs it costs one static field read. Side: both (the
 * class is common), active on the server only.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkFootprintGuard {

    @Shadow
    public World worldObj;

    @Shadow
    @Final
    public int xPosition;

    @Shadow
    @Final
    public int zPosition;

    @Inject(method = "func_150807_a", at = @At("HEAD"), cancellable = true)
    private void lostcitiesnh$guardFootprint(int x, int y, int z, Block block, int meta,
        CallbackInfoReturnable<Boolean> callback) {
        if (FootprintGuard.drops(worldObj, xPosition, zPosition, x, y, z)) {
            callback.setReturnValue(false);
        }
    }
}
