package com.github.vividfuzhu.maxbattery.machine.recipe;

// Minecraft 原版物品和方块引用
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import com.github.vividfuzhu.maxbattery.MaxBattery;

import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;

/**
 * 模组配方注册类
 * 所有自定义合成表（包括 GT 机器配方和原版工作台配方）都在此初始化
 */
public class ModRecipes {

    /**
     * 在 FMLPostInitialization 阶段注册所有配方
     * 此时 GregTech 和其他模组均已加载完毕，可安全访问 sBlockMachines 等对象
     */
    public static void init(FMLPostInitializationEvent event) {

        // === 智能电池的组装机配方（煤炭/木炭 → 智能电池）===
        // 使用 GT 的 RecipeBuilder 构建一个组装机配方
        GTValues.RA.stdBuilder()
            .itemInputs(
                // 输入：6 个普通煤炭（meta=0）
                GTUtility.copyAmount(6, new ItemStack(Items.coal, 1, 0)),
                // 输入：6 个木炭（meta=1）
                GTUtility.copyAmount(6, new ItemStack(Items.coal, 1, 1)))
            .itemOutputs(
                // 输出：1 个智能电池（自定义物品）
                new ItemStack(MaxBattery.ITEM_SMART_BATTERY, 1))
            .duration(5 * 20) // 处理时间：5 秒（20 tick = 1 秒）
            .eut(GTValues.V[1]) // 能耗：30 EU/t（LV 电压等级）
            .addTo(RecipeMaps.assemblerRecipes); // 添加到组装机配方表

        // === 智能电池的手工合成配方（4 煤炭 或 4 木炭）===
        // 原版工作台合成：左上 2x2 放煤炭（普通）
        GameRegistry.addRecipe(
            new ItemStack(MaxBattery.ITEM_SMART_BATTERY, 1), // 输出
            "CC ", // 第一行：两个煤炭 + 一个空格
            "CC ", // 第二行：两个煤炭 + 一个空格
            "   ", // 第三行：全空
            'C',
            new ItemStack(Items.coal, 1, 0) // 'C' 代表普通煤炭
        );
        // 同样支持木炭合成
        GameRegistry.addRecipe(
            new ItemStack(MaxBattery.ITEM_SMART_BATTERY, 1),
            "CC ",
            "CC ",
            "   ",
            'C',
            new ItemStack(Items.coal, 1, 1) // 'C' 代表木炭（meta=1）
        );

        // === 杂酚油→煤炭 青铜机器的工作台配方 ===
        // 创建目标机器 ItemStack：meta=27015（自定义 GT 机器）
        ItemStack creoCoalMachine = new ItemStack(GregTechAPI.sBlockMachines, 1, 27015);
        // 安全检查：确保该 meta 对应的方块有效（避免崩溃）
        if (creoCoalMachine != null && creoCoalMachine.getItem() != null) {
            // 合成表：用熔炉围成一个框（类似信标底座）
            GameRegistry.addRecipe(
                creoCoalMachine, // 输出
                "FFF", // 第一行：三个熔炉
                "F F", // 第二行：熔炉 + 空 + 熔炉
                "FFF", // 第三行：三个熔炉
                'F',
                new ItemStack(Blocks.furnace) // 'F' 代表熔炉
            );
            System.out.println("✓ Registered crafting recipe for Creosote → Coal machine");
        }

        // === 超速采矿机升级配方：原版挖矿机 + 红石 → 超速挖矿机 ===
        try {
            // 获取原版 GT 挖矿机（LV/MV/HV）
            ItemStack originalLV = ItemList.Machine_LV_Miner.get(1);
            ItemStack originalMV = ItemList.Machine_MV_Miner.get(1);
            ItemStack originalHV = ItemList.Machine_HV_Miner.get(1);

            // 创建自定义超速挖矿机（meta=27016/17/18）
            ItemStack upgradedLV = new ItemStack(GregTechAPI.sBlockMachines, 1, 27016);
            ItemStack upgradedMV = new ItemStack(GregTechAPI.sBlockMachines, 1, 27017);
            ItemStack upgradedHV = new ItemStack(GregTechAPI.sBlockMachines, 1, 27018);

            // 无序合成：原版挖矿机 + 红石 → 超速挖矿机
            GameRegistry.addShapelessRecipe(upgradedLV, originalLV, Items.redstone);
            GameRegistry.addShapelessRecipe(upgradedMV, originalMV, Items.redstone);
            GameRegistry.addShapelessRecipe(upgradedHV, originalHV, Items.redstone);

            System.out
                .println("✓ Registered crafting recipes: [Original Miner + Redstone] → MaxBatteryMiner (LV/MV/HV)");
        } catch (Exception ex) {
            // 异常捕获：防止因 GT 版本差异导致崩溃
            System.err.println("✗ Failed to register MaxBatteryMiner crafting recipes!");
            ex.printStackTrace();
        }

        // ============ 新增：3 原石 → 无尽导线（调试用）============
        // 创建无尽导线 ItemStack：
        // - 使用 GregTech 的主机器方块（gt.blockmachines）
        // - meta=11435 是 GTNH 中“无尽导线”的固定 ID
        // - 该值已通过命令 /give @p gregtech:gt.blockmachines 1 11435 验证有效
        ItemStack infiniteCable = new ItemStack(GregTechAPI.sBlockMachines, 1, 11435);

        // 注册原版工作台合成表：
        // - 输出：1 个无尽导线
        // - 合成形状："CCC" 表示一行三个原石（横向）
        // - 'C' 代表原石（minecraft:cobblestone）
        GameRegistry.addRecipe(
            infiniteCable, // 输出物品
            "CCC", // 合成形状（单行三格）
            'C',
            Blocks.cobblestone // 符号 'C' 对应原石
        );

        // 控制台日志：确认配方注册成功
        System.out.println("✓ Registered crafting recipe: 3x Cobblestone → Infinite Cable");

        // === 新增：1个维护仓 → 1个Debug维护仓（无序合成）===
        ItemStack maintenanceChest = new ItemStack(GregTechAPI.sBlockMachines, 1, 90);
        ItemStack debugMaintenanceChest = new ItemStack(GregTechAPI.sBlockMachines, 1, 15497);

        if (maintenanceChest != null && debugMaintenanceChest != null) {
            GameRegistry.addShapelessRecipe(debugMaintenanceChest, maintenanceChest);
            System.out.println("✓ Registered shapeless recipe: Maintenance Chest → Debug Maintenance Chest");
        }

    }
}
