package com.dragsimutils.hooks;

import com.dragsimutils.visual.SoulFade;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Room on every living render state for the soul fade. */
@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements SoulFade {

    @Unique
    private float dragsimutils$soulAlpha = -1f;

    @Override
    public float dragsimutils$soulAlpha() {
        return dragsimutils$soulAlpha;
    }

    @Override
    public void dragsimutils$setSoulAlpha(float alpha) {
        dragsimutils$soulAlpha = alpha;
    }
}
