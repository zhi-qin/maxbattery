package com.github.vividfuzhu.maxbattery.config;

/**
 * 模组配置参数 - 集中管理所有可调数值。
 *
 * 遵循"配置外置化"设计原则：
 * - 所有硬编码数值汇集于此，未来可一键迁移至 .cfg 配置文件
 * - 新增机器等级时只需扩展数组，无需修改业务逻辑
 */
public final class ModConfig {

    private ModConfig() {}

    // ==================== 超速采矿机参数 ====================
    // 数组索引对应机器等级: 0=ULV, 1=LV, 2=MV, 3=HV, 4=EV, 5=IV
    // 扩展方式: 在数组末尾追加新等级的值即可

    /** 各等级机器的工作半径（格数） */
    public static final int[] MINER_RADIUS    = { 8,   8,   16,  24,  32,  32 };

    /** 各等级机器的挖掘速度（tick/方块） */
    public static final int[] MINER_SPEED     = { 8,   8,   4,   2,   1,   1 };

    /** 各等级机器的能耗（EU/t） */
    public static final int[] MINER_ENERGY    = { 160, 160, 640, 2560, 10240, 40960 };

    // ==================== Tick熔炉参数 ====================

    /** 每tick消耗的燃料倍率（相对原版1/tick） */
    public static final int FURNACE_FUEL_RATE      = 100;
    /** 每tick增加的炼制进度值 */
    public static final int FURNACE_COOK_AMOUNT    = 100;
    /** 炼制完成阈值（进度≥此值即产出） */
    public static final int FURNACE_COOK_THRESHOLD = 200;

    // ==================== 杂酚油炼煤机参数 ====================

    /** 每产出1个煤炭所需的杂酚油量（mB） */
    public static final int CREOSOTE_PER_COAL      = 1000;
    /** 杂酚油储罐容量（mB） */
    public static final int CREOSOTE_TANK_CAPACITY = 16000;

    // ==================== 无限冷却单元参数 ====================

    /** 每 tick 额外从反应堆中吸收的热量（HU） */
    public static final int COOLANT_EXTRA_ABSORPTION = 10000;

    // ==================== 智能电池参数 ====================

    /** 容量安全余量（防止溢出），单位 EU */
    public static final long BATTERY_CAPACITY_MARGIN = 1_000_000_000L;

    // ==================== 超速采矿机扩展参数 ====================

    /** 采矿机最大安培输入 */
    public static final long MINER_MAX_AMPERES = 40L;

}
