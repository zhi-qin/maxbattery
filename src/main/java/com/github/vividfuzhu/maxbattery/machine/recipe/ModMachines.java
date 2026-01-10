package com.github.vividfuzhu.maxbattery.machine.recipe;

import com.github.vividfuzhu.maxbattery.machine.bronze.TileCoalFromCreosote;
import com.github.vividfuzhu.maxbattery.machine.miner.MaxBatteryMiner;

import gregtech.api.GregTechAPI;

public class ModMachines {

    public static final int COAL_FROM_CREOSOTE_ID = 27015;
    public static final int MINER_LV_ID = 27016;
    public static final int MINER_MV_ID = 27017;
    public static final int MINER_HV_ID = 27018;

    public static void registerMachines() {
        try {
            safeRegister(
                COAL_FROM_CREOSOTE_ID,
                new TileCoalFromCreosote(
                    COAL_FROM_CREOSOTE_ID,
                    "maxbattery.machine.coal_from_creosote",
                    "maxbattery.machine.coal_from_creosote"));

            safeRegister(MINER_LV_ID, new MaxBatteryMiner(MINER_LV_ID, "maxbattery.miner.lv", "【超速采矿机】(LV)", 1));
            safeRegister(MINER_MV_ID, new MaxBatteryMiner(MINER_MV_ID, "maxbattery.miner.mv", "【超速采矿机】(MV)", 2));
            safeRegister(MINER_HV_ID, new MaxBatteryMiner(MINER_HV_ID, "maxbattery.miner.hv", "【超速采矿机】(HV)", 3));

            System.out.println("[MaxBattery] ✓ All machines registered successfully.");
        } catch (Exception e) {
            System.err.println("[MaxBattery] ✗ Failed to register machines!");
            e.printStackTrace();
        }
    }

    private static void safeRegister(int id, gregtech.api.metatileentity.MetaTileEntity machine) {
        if (id <= 0 || id >= GregTechAPI.METATILEENTITIES.length) {
            throw new IllegalArgumentException("Invalid MTE ID: " + id);
        }
        if (GregTechAPI.METATILEENTITIES[id] != null) {
            System.err.println("[MaxBattery] ⚠️ Warning: MTE ID " + id + " is already occupied!");
        }
        GregTechAPI.METATILEENTITIES[id] = machine;
        System.out.println("[MaxBattery] Registered MTE: " + machine.getInventoryName() + " @ ID " + id);
    }
}
