package com.github.vividfuzhu.maxbattery.init;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.github.vividfuzhu.maxbattery.MaxBattery;
import com.github.vividfuzhu.maxbattery.config.ModIds;

import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;

/**
 * 模组配方注册 - 所有自定义合成表集中管理。
 *
 * 在 FMLPostInitialization 阶段执行，此时 GT 及其他模组均已就绪。
 *
 * 扩展方式：
 * 1. 在 init() 末尾或新增方法中追加配方注册代码
 * 2. 引用 ModItems.xxx 获取物品实例
 * 3. 引用 ModIds.xxx 获取机器ID，使用 {@code GregTechAPI.sBlockMachines} 构造 ItemStack
 */
public final class ModRecipes {

    private ModRecipes() {}

    /**
     * 注册所有配方。
     *
     * @param event FMLPostInitialization 事件（暂未使用，保留签名以支持未来扩展）
     */
    public static void init(FMLPostInitializationEvent event) {

        // ──────────────────────────────────────────────
        // 第一部分：原版工作台合成
        // ──────────────────────────────────────────────

        // === 无限冷却热熔：4原石 → 1冷却热熔 ===
        GameRegistry.addShapedRecipe(
            new ItemStack(ModItems.infiniteCoolantCell, 1),
            "CC",
            "CC",
            'C',
            new ItemStack(Blocks.cobblestone));

        // === 智能电池手工合成：4煤炭 → 1智能电池 ===
        GameRegistry.addRecipe(
            new ItemStack(ModItems.smartBattery, 1),
            "CC ",
            "CC ",
            "   ",
            'C',
            new ItemStack(Items.coal, 1, 0));
        // 木炭版本
        GameRegistry.addRecipe(
            new ItemStack(ModItems.smartBattery, 1),
            "CC ",
            "CC ",
            "   ",
            'C',
            new ItemStack(Items.coal, 1, 1));

        // === 杂酚油炼煤机：8熔炉围框 → 1机器 ===
        GameRegistry.addRecipe(
            new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.COAL_FROM_CREOSOTE),
            "FFF",
            "F F",
            "FFF",
            'F',
            new ItemStack(Blocks.furnace));
        MaxBattery.LOG.info("Registered crafting recipe for Creosote -> Coal machine");

        // === tick熔炉：熔炉 + 木棍 → tick熔炉（无序） ===
        GameRegistry.addShapelessRecipe(
            new ItemStack(ModBlocks.tickFurnace, 1),
            Blocks.furnace,
            Items.stick);
        MaxBattery.LOG.info("Registered shapeless recipe: Furnace + Stick -> TickFurnace");

        // === 维护仓 → Debug维护仓（无序） ===
        GameRegistry.addShapelessRecipe(
            new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.DEBUG_MAINTENANCE_HATCH),
            new ItemStack(GregTechAPI.sBlockMachines, 1, 90));
        MaxBattery.LOG.info("Registered shapeless recipe: Maintenance Chest -> Debug Maintenance Chest");

        // === 无尽导线调试配方：3原石横排 → 1无尽导线 ===
        GameRegistry.addRecipe(
            new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.INFINITE_CABLE),
            "CCC",
            'C',
            Blocks.cobblestone);
        MaxBattery.LOG.info("Registered crafting recipe: 3x Cobblestone -> Infinite Cable");

        // ──────────────────────────────────────────────
        // 第二部分：GT组装机配方
        // ──────────────────────────────────────────────

        // === 智能电池：6煤炭 + 6木炭 → 1智能电池 ===
        GTValues.RA.stdBuilder()
            .itemInputs(
                GTUtility.copyAmount(6, new ItemStack(Items.coal, 1, 0)),
                GTUtility.copyAmount(6, new ItemStack(Items.coal, 1, 1)))
            .itemOutputs(new ItemStack(ModItems.smartBattery, 1))
            .duration(5 * 20)
            .eut(GTValues.V[1])
            .addTo(RecipeMaps.assemblerRecipes);

        // ──────────────────────────────────────────────
        // 第三部分：超速采矿机升级配方
        // ──────────────────────────────────────────────

        try {
            // 原版 GT 挖矿机 → 超速挖矿机（LV/MV/HV）
            ItemStack originalLV = ItemList.Machine_LV_Miner.get(1);
            ItemStack originalMV = ItemList.Machine_MV_Miner.get(1);
            ItemStack originalHV = ItemList.Machine_HV_Miner.get(1);

            GameRegistry.addShapelessRecipe(
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_LV),
                originalLV, Items.redstone);
            GameRegistry.addShapelessRecipe(
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_MV),
                originalMV, Items.redstone);
            GameRegistry.addShapelessRecipe(
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_HV),
                originalHV, Items.redstone);
            MaxBattery.LOG.info("Registered upgrade recipes: Miner + Redstone -> MaxBatteryMiner (LV/MV/HV)");

            // MaxBatteryMiner 升级链：HV → EV → IV
            GameRegistry.addShapelessRecipe(
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_EV),
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_HV), Items.redstone);
            GameRegistry.addShapelessRecipe(
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_IV),
                new ItemStack(GregTechAPI.sBlockMachines, 1, ModIds.MINER_EV), Items.redstone);
            MaxBattery.LOG.info("Registered upgrade recipes: MaxBatteryMiner (EV/IV)");
        } catch (Exception ex) {
            MaxBattery.LOG.error("Failed to register MaxBatteryMiner crafting recipes!", ex);
        }

    }

}
