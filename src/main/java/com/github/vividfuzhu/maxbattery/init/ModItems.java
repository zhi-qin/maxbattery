package com.github.vividfuzhu.maxbattery.init;

import com.github.vividfuzhu.maxbattery.item.InfiniteCoolantCell;
import com.github.vividfuzhu.maxbattery.item.SmartBattery;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 物品注册 - 集中管理所有自定义物品的实例化和注册。
 *
 * 所有物品通过静态字段暴露，外部类以 {@code ModItems.xxx} 引用：
 * <pre>
 *   new ItemStack(ModItems.smartBattery)
 * </pre>
 *
 * 扩展方式：
 * 1. 添加 public static 字段
 * 2. 在 init() 中 new + 必要时调用 GameRegistry.registerItem
 * 3. SmartBattery 继承 GT 的 MetaBaseItem，构造时已自动注册
 */
public final class ModItems {

    private ModItems() {}

    // ========== 物品实例（由 init() 填充） ==========
    public static InfiniteCoolantCell infiniteCoolantCell;
    public static SmartBattery smartBattery;

    public static void init() {
        infiniteCoolantCell = new InfiniteCoolantCell();
        // SmartBattery 继承自 GregTech 的 MetaBaseItem，构造时自动注册
        smartBattery = new SmartBattery();

        GameRegistry.registerItem(infiniteCoolantCell, "infiniteCoolantCell");
    }

}
