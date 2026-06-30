package com.github.vividfuzhu.maxbattery.config;

/**
 * 模组ID常量 - 集中管理所有方块、物品、机器的注册ID。
 *
 * 遵循"配置外置化"设计原则，所有ID统一在此定义：
 * - 新增机器：在此定义新ID → ModMachines 中用 safeRegister 注册
 * - 新增物品：在此注释分配区域 → ModItems 中注册
 * - 无需再在 ModMachines 或配方文件中硬编码数字
 */
public final class ModIds {

    private ModIds() {}

    // ==================== GT机器ID (MTE ID) ====================
    // 范围需在 GregTech 配置中预留，避免冲突

    public static final int COAL_FROM_CREOSOTE = 27015;

    public static final int MINER_LV = 27016;
    public static final int MINER_MV = 27017;
    public static final int MINER_HV = 27018;
    public static final int MINER_EV = 27019;
    public static final int MINER_IV = 27020;

    // ==================== 调试物品ID (GT内部meta) ====================
    // 注意: 这些值依赖 GT 内部注册顺序，版本更新时需核对

    /** Debug维护仓 (GT内部meta) */
    public static final int DEBUG_MAINTENANCE_HATCH = 15497;
    /** 无尽导线 (GT内部meta) */
    public static final int INFINITE_CABLE = 11435;

    // ==================== 预留ID范围 ====================
    // 机器: 27015 - 27029 (15个ID)
    // 物品: GregTech MetaBaseItem 自动分配
    // 方块: GameRegistry.registerBlock 自动分配

}
