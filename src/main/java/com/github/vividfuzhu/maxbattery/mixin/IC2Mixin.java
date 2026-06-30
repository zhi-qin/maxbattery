package com.github.vividfuzhu.maxbattery.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.vividfuzhu.maxbattery.MaxBattery;

/**
 * IC2核反应堆防过热 Mixin。
 *
 * 当反应堆热量 ≥ 99% 时自动降温至 99% 并取消热效应，
 * 防止因过热导致的爆炸。
 *
 * 注入目标：{@code ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric#calculateHeatEffects}
 */
@Mixin(value = ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric.class, remap = false)
public abstract class IC2Mixin {

    @Shadow
    public int heat;

    @Shadow
    public int maxHeat;

    @Inject(
        method = "calculateHeatEffects",
        at = @At("HEAD"),
        cancellable = true,
        require = 1)
    private void modifyHeatLogic(CallbackInfoReturnable<Boolean> cir) {
        if (maxHeat > 0 && heat >= maxHeat * 0.99) {
            this.heat = (int) (maxHeat * 0.99);
            MaxBattery.LOG.debug("IC2反应堆过热！安全系统已介入并降温。");
            cir.setReturnValue(false);
        }
    }

}
