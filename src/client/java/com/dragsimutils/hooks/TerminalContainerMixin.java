package com.dragsimutils.hooks;
import com.dragsimutils.dungeon.TerminalSolver;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerScreen.class)
abstract class TerminalContainerMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void dragsimutils$terminal(GuiGraphicsExtractor g, int x, int y, float delta, CallbackInfo ci) {
        if (TerminalSolver.compactActive((AbstractContainerScreen<?>) (Object)this)) ci.cancel();
    }
}
