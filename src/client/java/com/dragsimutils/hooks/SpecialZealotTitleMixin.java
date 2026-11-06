package com.dragsimutils.hooks;

import com.dragsimutils.zealots.ZealotTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the personal spawn popup without changing its display. */
@Mixin(Gui.class)
abstract class SpecialZealotTitleMixin {
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void dragsimutils$specialTitle(Component message, CallbackInfo ci) {
        ZealotTracker.onSpecialTitle(message, false);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void dragsimutils$specialSubtitle(Component message, CallbackInfo ci) {
        ZealotTracker.onSpecialTitle(message, true);
    }
}
