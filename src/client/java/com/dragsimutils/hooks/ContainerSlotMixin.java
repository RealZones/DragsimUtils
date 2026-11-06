package com.dragsimutils.hooks;

import com.dragsimutils.qol.DebugMessage;
import com.dragsimutils.qol.SlotLock;
import com.dragsimutils.visual.RarityOutline;
import com.dragsimutils.storage.ForgeRecipes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The moment before a window draws an item into a slot: the pose is already moved to
 * the window's corner, so the slot's own x and y are the place. See RarityOutline for
 * why the colour has to go in here and not after the screen is done.
 */
@Mixin(AbstractContainerScreen.class)
abstract class ContainerSlotMixin {

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void dragsimutils$behindSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        RarityOutline.behindSlot(g, slot);
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void dragsimutils$overSlot(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        ForgeRecipes.overSlot((AbstractContainerScreen<?>) (Object) this, g, slot);
        DebugMessage.overSlot((AbstractContainerScreen<?>) (Object) this, g, slot);
        SlotLock.overSlot(g, slot);
    }
}
