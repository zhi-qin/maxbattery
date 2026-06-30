package com.github.vividfuzhu.maxbattery.init;

import com.github.vividfuzhu.maxbattery.MaxBattery;
import com.github.vividfuzhu.maxbattery.config.ModIds;
import com.github.vividfuzhu.maxbattery.machine.creosote.MTECreosoteCoalConverter;
import com.github.vividfuzhu.maxbattery.machine.miner.MaxBatteryMiner;

import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.MetaTileEntity;

/**
 * GT机器注册 - 集中管理所有 GregTech 机器的注册。
 *
 * 扩展方式（三步）：
 * <ol>
 *   <li>在 {@link ModIds} 中定义新机器 ID</li>
 *   <li>创建机器的 MetaTileEntity 类（放在对应的 {@code machine/xxx/} 包下）</li>
 *   <li>在此类的 {@code registerMachines()} 中添加 {@link #safeRegister} 调用</li>
 * </ol>
 */
public final class ModMachines {

    private ModMachines() {}

    /** 防止重复注册 */
    private static boolean registered = false;

    public static void registerMachines() {
        if (registered) {
            MaxBattery.LOG.warn("ModMachines already registered, skipping.");
            return;
        }
        try {
            // ---- 杂酚油→煤炭 青铜机器 ----
            safeRegister(
                ModIds.COAL_FROM_CREOSOTE,
                new MTECreosoteCoalConverter(
                    ModIds.COAL_FROM_CREOSOTE,
                    "maxbattery.machine.coal_from_creosote",
                    "maxbattery.machine.coal_from_creosote"));

            // ---- 超速采矿机 (LV / MV / HV / EV / IV) ----
            safeRegister(ModIds.MINER_LV, new MaxBatteryMiner(ModIds.MINER_LV, "maxbattery.miner.lv", "【超速采矿机】(LV)", 1));
            safeRegister(ModIds.MINER_MV, new MaxBatteryMiner(ModIds.MINER_MV, "maxbattery.miner.mv", "【超速采矿机】(MV)", 2));
            safeRegister(ModIds.MINER_HV, new MaxBatteryMiner(ModIds.MINER_HV, "maxbattery.miner.hv", "【超速采矿机】(HV)", 3));
            safeRegister(ModIds.MINER_EV, new MaxBatteryMiner(ModIds.MINER_EV, "maxbattery.miner.ev", "【超速采矿机】(EV)", 4));
            safeRegister(ModIds.MINER_IV, new MaxBatteryMiner(ModIds.MINER_IV, "maxbattery.miner.iv", "【超速采矿机】(IV)", 5));

            MaxBattery.LOG.info("All machines registered successfully.");
            registered = true;
        } catch (Exception e) {
            MaxBattery.LOG.error("Failed to register machines!", e);
        }
    }

    /**
     * 安全注册 MTE：校验 ID 范围、防冲突覆盖。
     *
     * @param id      MTE 注册 ID
     * @param machine 机器实例
     */
    private static void safeRegister(int id, MetaTileEntity machine) {
        if (id <= 0 || id >= GregTechAPI.METATILEENTITIES.length) {
            throw new IllegalArgumentException("Invalid MTE ID: " + id);
        }
        if (GregTechAPI.METATILEENTITIES[id] != null) {
            MaxBattery.LOG.warn("Warning: MTE ID {} is already occupied!", id);
        }
        GregTechAPI.METATILEENTITIES[id] = machine;
        MaxBattery.LOG.info("Registered MTE: {} @ ID {}", machine.getInventoryName(), id);
    }

}
