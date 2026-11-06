package com.dragsimutils.hooks;

import com.dragsimutils.visual.Perspective;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the mouse's turn to the camera while Perspective Mode is on.
 */
@Mixin(Entity.class)
abstract class PerspectiveTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void dragsimutils$camera(double yRot, double xRot, CallbackInfo ci) {
        if (!Perspective.active() || (Object) this != Minecraft.getInstance().player) return;
        Perspective.turn(yRot, xRot);
        ci.cancel();
    }
}
