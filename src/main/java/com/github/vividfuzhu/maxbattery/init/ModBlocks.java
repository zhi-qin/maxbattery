package com.github.vividfuzhu.maxbattery.init;

import com.github.vividfuzhu.maxbattery.MaxBattery;
import com.github.vividfuzhu.maxbattery.block.furnace.BlockTickFurnace;
import com.github.vividfuzhu.maxbattery.block.furnace.TileTickFurnace;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * 方块注册 - 集中管理所有非GT方块（原版 Block）的实例化与注册。
 *
 * 扩展方式：
 * 1. 在此类中声明 public static 字段持有方块实例
 * 2. 在 init() 中 new + GameRegistry.registerBlock
 * 3. 如需自定义 TileEntity，一并 registerTileEntity
 * 4. 外部类通过 {@code ModBlocks.xxx} 引用已注册的方块
 */
public final class ModBlocks {

    private ModBlocks() {}

    // ========== 方块实例（由 init() 填充） ==========
    /** Tick熔炉方块 - 外部通过 ModBlocks.tickFurnace 引用 */
    public static BlockTickFurnace tickFurnace;

    public static void init() {
        // === Tick熔炉 ===
        tickFurnace = new BlockTickFurnace();
        GameRegistry.registerBlock(tickFurnace, "tickFurnace");
        GameRegistry.registerTileEntity(TileTickFurnace.class, "tickFurnace");
        MaxBattery.LOG.info("Registered TickFurnace block and TileEntity");
    }

}
