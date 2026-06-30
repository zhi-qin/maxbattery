package com.github.vividfuzhu.maxbattery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.vividfuzhu.maxbattery.init.ModBlocks;
import com.github.vividfuzhu.maxbattery.init.ModItems;
import com.github.vividfuzhu.maxbattery.init.ModMachines;
import com.github.vividfuzhu.maxbattery.init.ModRecipes;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = MaxBattery.MODID,
    name = MaxBattery.NAME,
    version = MaxBattery.VERSION,
    dependencies = "required-after:gregtech")
public class MaxBattery {

    public static final String MODID = "maxbattery";
    public static final String NAME = "Max Battery";
    public static final String VERSION = "1.0.0";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        LOG.info("MaxBattery preInit started");

        // 委托：注册所有物品
        ModItems.init();

        // 委托：注册所有机器（包括青铜机和超速采矿机）
        ModMachines.registerMachines();

        // 委托：注册所有非GT方块（Tick熔炉等）
        ModBlocks.init();

        LOG.info("MaxBattery preInit completed");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        // ========== 委托给 ModRecipes 处理所有配方 ==========
        ModRecipes.init(e);
    }

    // 物品引用已迁移至 ModItems 类
}
