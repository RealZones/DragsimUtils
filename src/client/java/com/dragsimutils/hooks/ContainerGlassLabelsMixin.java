package com.dragsimutils.hooks;

import com.dragsimutils.visual.InventoryGlass;
import com.dragsimutils.ui.Theme;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerGlassLabelsMixin {
    @ModifyArg(method = "extractLabels", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 4)
    private int dragsimutils$label(int original) { return InventoryGlass.applies(this) ? Theme.text() : original; }
}
