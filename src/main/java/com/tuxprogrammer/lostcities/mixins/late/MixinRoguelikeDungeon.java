package com.tuxprogrammer.lostcities.mixins.late;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.tuxprogrammer.lostcities.compat.roguelike.RoguelikePlacements;
import com.tuxprogrammer.lostcities.compat.roguelike.RoguelikeSteering;
import com.tuxprogrammer.lostcities.compat.roguelike.SteeringRule;

import greymerk.roguelike.dungeon.Dungeon;
import greymerk.roguelike.worldgen.IWorldEditor;

/**
 * The Roguelike Dungeons steering (0006 §5; other-worldgen.md T4; decision log D26, D31): refuses a candidate point
 * whose tower lot touches a city chunk, an edge ring or a scattered structure's footprint, unless it is the dungeon's
 * predicted, reserved lot ({@code compat.roguelike.SteeringRule}). The dungeon then lands in its lot, outside the
 * city, or nowhere, and the outcome follows the layout instead of the order chunks generate in.
 *
 * <p>
 * Why no event works: Roguelike Dungeons is an {@code IWorldGenerator} that tries 50 points and builds at the first its
 * own {@code validLocation} accepts; it fires no event per point or per dungeon, and Forge's populate events do not
 * cover {@code IWorldGenerator}s. Denying the whole generator in city chunks would also refuse the points outside the
 * city that its search reaches from there. Only a check inside the per-point test steers the search. Source:
 * https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/dungeon/Dungeon.java#L51-L66
 * and #L165-L212.
 *
 * <p>
 * The head injection only ever refuses, so Roguelike's own test still decides every point the steering lets through;
 * the injection into {@code generateNear} only remembers the search's origin for the scenario's record. A late mixin,
 * applied only when Roguelike Dungeons is loaded ({@code compat.LateMixinLoader}); Roguelike's classes are not
 * obfuscated, so nothing is remapped. Side: both (the class is common), acting on the server only.
 */
@Mixin(value = Dungeon.class, remap = false)
public abstract class MixinRoguelikeDungeon {

    @Shadow(remap = false)
    private IWorldEditor editor;

    @Unique
    private int lostcitiesnh$originX;

    @Unique
    private int lostcitiesnh$originZ;

    /** Whether the steering refused the point being tested, so the return hook never records it as a site. */
    @Unique
    private boolean lostcitiesnh$refused;

    @Inject(method = "generateNear", at = @At("HEAD"), remap = false)
    private void lostcitiesnh$rememberOrigin(Random rand, int x, int z, CallbackInfo callback) {
        lostcitiesnh$originX = x;
        lostcitiesnh$originZ = z;
    }

    @Inject(method = "validLocation", at = @At("HEAD"), cancellable = true, remap = false)
    private void lostcitiesnh$steer(Random rand, int x, int z, CallbackInfoReturnable<Boolean> callback) {
        int dimension = editor.getDimension();
        SteeringRule.Outcome outcome = RoguelikeSteering.decide(dimension, x, z);
        lostcitiesnh$refused = outcome.refuses();
        if (lostcitiesnh$refused) {
            RoguelikePlacements.refused(dimension, lostcitiesnh$originX, lostcitiesnh$originZ, x, z, outcome);
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "validLocation", at = @At("RETURN"), remap = false)
    private void lostcitiesnh$recordSite(Random rand, int x, int z, CallbackInfoReturnable<Boolean> callback) {
        if (lostcitiesnh$refused) {
            return;
        }
        if (callback.getReturnValueZ()) {
            RoguelikePlacements.accepted(editor.getDimension(), lostcitiesnh$originX, lostcitiesnh$originZ, x, z);
        } else {
            RoguelikePlacements.rejected(editor.getDimension(), lostcitiesnh$originX, lostcitiesnh$originZ, x, z);
        }
    }
}
