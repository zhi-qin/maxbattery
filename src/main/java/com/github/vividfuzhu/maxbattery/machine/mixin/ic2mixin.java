package com.github.vividfuzhu.maxbattery.machine.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1. 声明 Mixin：我要修改这个类
@Mixin(value = ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric.class, remap = false)
public abstract class ic2mixin {

    // 2. 引用原版类里的私有变量 (我们需要读写它们)
    @Shadow
    public int heat; // 当前热量

    @Shadow
    public int maxHeat; // 最大热量

    // 3. 注入点：在 "calculateHeatEffects" 方法执行时拦截
    // 这个方法是 IC2 用来判断是否爆炸的核心逻辑
    @Inject(
        method = "calculateHeatEffects",
        at = @At("HEAD"), // 在方法最开始就介入
        cancellable = true, // 允许我们取消原版逻辑
        require = 1 // 确保只注入一次，防止报错
    )
    private void modifyHeatLogic(CallbackInfoReturnable<Boolean> cir) {

        // 4. 核心逻辑：判断是否超过 99%
        // 注意：maxHeat 可能为 0，做个保护
        if (maxHeat > 0 && heat >= maxHeat * 0.99) {

            // 4.1 强制将热量锁定在 99%
            this.heat = (int) (maxHeat * 0.99);

            // 4.2 可选：这里可以加一句日志，提示玩家过热了
            // System.out.println("[MaxBattery] 警告：反应堆过热！安全系统已介入并降温。");

            // 4.3 阻止原版爆炸逻辑执行
            // 原版逻辑如果返回 true，就会爆炸。我们强制让它返回 false。
            cir.setReturnValue(false);

            // 4.4 结束方法，不再执行原版的代码
            cir.cancel();
        }

        // 如果热量没超限，什么也不做，让原版逻辑正常运行
    }
}
