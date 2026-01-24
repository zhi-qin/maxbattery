// 文件路径: src/main/java/com/github/vividfuzhu/maxbattery/MaxBattery.java
package com.github.vividfuzhu.maxbattery;

import com.github.vividfuzhu.maxbattery.item.InfiniteCoolantCell;
import com.github.vividfuzhu.maxbattery.item.SmartBattery;
import com.github.vividfuzhu.maxbattery.machine.recipe.ModMachines;
import com.github.vividfuzhu.maxbattery.machine.recipe.ModRecipes;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = MaxBattery.MODID,
    name = MaxBattery.NAME,
    version = MaxBattery.VERSION,
    dependencies = "required-after:gregtech")
public class MaxBattery {

    public static final String MODID = "maxbattery";
    public static final String NAME = "Max Battery";
    public static final String VERSION = "1.0.0";

    // 移除智能电池实例
    // public static final SmartBattery ITEM_SMART_BATTERY = new SmartBattery();

    public static final InfiniteCoolantCell ITEM_INFINITE_COOLANT_CELL = new InfiniteCoolantCell();
    // 静态实例：无限冷却剂单元物品，供全局引用和注册使用

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        System.out.println("Smart Battery Mod Loaded!");

        // 委托：注册所有机器（包括青铜机和超速采矿机）
        ModMachines.registerMachines();

        // 注册无限冷却剂单元到 Forge 物品注册表
        // 第二个参数是注册名，最终物品 ID 为 "maxbattery:infiniteCoolantCell"
        GameRegistry.registerItem(ITEM_INFINITE_COOLANT_CELL, "infiniteCoolantCell");

        // --- 新增：注册智能电池 ---
        // 创建智能电池实例并注册
        SmartBattery.ITEM_SMART_BATTERY = new SmartBattery();
        // GameRegistry.registerItem(SmartBattery.ITEM_SMART_BATTERY, "smartBattery"); // 使用新的注册名
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        // ========== 注册超速采矿机（LV, MV, HV）到 GregTech 的 MetaTileEntity 数组 ==========
        // GregTechAPI.METATILEENTITIES[27016] = new MaxBatteryMiner(27016, "maxbattery.miner.lv", "【超速采矿机】(LV)", 1);
        // GregTechAPI.METATILEENTITIES[27017] = new MaxBatteryMiner(27017, "maxbattery.miner.mv", "【超速采矿机】(MV)", 2);
        // GregTechAPI.METATILEENTITIES[27018] = new MaxBatteryMiner(27018, "maxbattery.miner.hv", "【超速采矿机】(HV)", 3);

        // System.out.println("[MaxBattery] Successfully registered 3 fast miners (LV, MV, HV) at IDs 27016～27018");

        // ========== 委托给 ModRecipes 处理所有配方 ==========
        ModRecipes.init(e);
    }

    // --- 将 SmartBattery 类移动到单独的文件中 ---
    // 原来的 SmartBattery 内部类内容被移到了 src/main/java/com/github/vividfuzhu/maxbattery/item/SmartBattery.java
}
