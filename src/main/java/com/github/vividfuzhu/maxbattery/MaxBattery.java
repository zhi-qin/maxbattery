package com.github.vividfuzhu.maxbattery;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import com.github.vividfuzhu.maxbattery.machine.recipe.ModMachines;
import com.github.vividfuzhu.maxbattery.machine.recipe.ModRecipes;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaBaseItem;

@Mod(
    modid = MaxBattery.MODID,
    name = MaxBattery.NAME,
    version = MaxBattery.VERSION,
    dependencies = "required-after:gregtech")
public class MaxBattery {

    public static final String MODID = "maxbattery";
    public static final String NAME = "Max Battery";
    public static final String VERSION = "1.0.0";
    public static final SmartBattery ITEM_SMART_BATTERY = new SmartBattery();

    /**
     * 杂酚油→煤炭 单方块青铜机器的方块实例。
     * 在 preInit 阶段实例化并注册到游戏，使其作为实体机器存在。
     */

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        System.out.println("Smart Battery Mod Loaded!");

        // 委托：注册所有机器（包括青铜机和超速采矿机）
        ModMachines.registerMachines();
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

    public static class SmartBattery extends MetaBaseItem {

        private static final String[] TIER_NAMES = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV",
            "UEV", "UIV", "UMV", "UXV", "MAX", "OPV" };

        public SmartBattery() {
            super("maxbattery.smart");
            this.setCreativeTab(GregTechAPI.TAB_GREGTECH);
            this.setMaxStackSize(1);
        }

        @Override
        public Long[] getElectricStats(ItemStack aStack) {
            // 从 NBT 读取当前电压等级，默认 LV
            int currentTier = getCurrentTier(aStack);

            // 安全容量：1万亿EU
            long capacity = Long.MAX_VALUE - 1_000_000_000L;

            // 获取当前电压值
            long voltage = getVoltageValue(currentTier);

            return new Long[] { capacity, // 容量
                voltage * 2, // 传输速率 = 电压 × 2
                (long) currentTier, // 当前电压等级
                -3L // 可充电电池，可作为能源
            };
        }

        @Override
        public Long[] getFluidContainerStats(ItemStack aStack) {
            return null;
        }

        // 关键：右键点击机器时自动适配电压
        @Override
        public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
            if (world.isRemote) return false; // 只在服务端执行

            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof IGregTechTileEntity) {
                IGregTechTileEntity gtTile = (IGregTechTileEntity) te;

                // 获取机器信息
                int machineTier = getMachineTier(gtTile);
                String machineName = getMachineName(gtTile);

                if (machineTier >= 0) {
                    // 更新电池电压等级
                    int oldTier = getCurrentTier(stack);
                    setCurrentTier(stack, machineTier);

                    // 发送反馈消息
                    if (oldTier != machineTier) {
                        String oldName = getTierName(oldTier);
                        String newName = getTierName(machineTier);

                        player.addChatMessage(
                            new ChatComponentText(
                                "§a[智能电池] §7已适配电压：" + "§c"
                                    + oldName
                                    + " §7→ §a"
                                    + newName
                                    + " §7("
                                    + machineName
                                    + ")"));
                    } else {
                        player.addChatMessage(
                            new ChatComponentText(
                                "§a[智能电池] §7当前电压：§a" + getTierName(machineTier) + " §7(与 " + machineName + " 匹配)"));
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            int tier = getCurrentTier(stack);
            return "智能电池 [" + getTierName(tier) + "]";
        }

        @Override
        public void addAdditionalToolTips(List<String> tooltip, ItemStack stack, EntityPlayer player) {
            int tier = getCurrentTier(stack);

            // 显示当前电压
            tooltip.add("§6当前电压: §e" + getTierName(tier) + " (Tier " + tier + ")");
            tooltip.add("§6电压值: §e" + getVoltageValue(tier) + " EU/t");

            // 显示电池属性
            Long[] stats = getElectricStats(stack);
            if (stats != null) {
                tooltip.add("§6容量: §e" + formatNumber(stats[0]) + " EU");
                tooltip.add("§6传输速率: §e" + formatNumber(stats[1]) + " EU/t");

                // 当前电量
                long currentCharge = getRealCharge(stack);
                double percent = stats[0] > 0 ? (currentCharge * 100.0) / stats[0] : 0;
                tooltip.add("§a当前电量: §e" + formatNumber(currentCharge) + " EU");
                tooltip.add("§a电量: §e" + String.format("%.1f", percent) + "%");
            }

            // 使用说明
            tooltip.add("§9§o右键点击机器自动适配电压,天顶星出品");
        }

        // ========== 辅助方法 ==========

        private int getCurrentTier(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                // 默认 LV 电压
                NBTTagCompound nbt = new NBTTagCompound();
                nbt.setInteger("VoltageTier", 1); // LV
                stack.setTagCompound(nbt);
                return 1;
            }
            return stack.getTagCompound()
                .getInteger("VoltageTier");
        }

        private void setCurrentTier(ItemStack stack, int tier) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            // 限制在 0-15 范围内
            tier = Math.max(0, Math.min(tier, 15));
            stack.getTagCompound()
                .setInteger("VoltageTier", tier);
        }

        // 电压值转电压等级
        private int getMachineTier(IGregTechTileEntity gtTile) {
            try {
                // 获取机器的 NBT 数据

                // gtTile.writeToNBT(nbt);

                // 打印出NBT内容
                // System.out.println("NBT keys in machine:");
                // for (Object keyObj : nbt.func_150296_c()) {
                // String key = (String) keyObj;
                // System.out.println(" " + key + " = " + nbt.getTag(key));
                // }
                long inputV = gtTile.getInputVoltage(); // 官方只读 API
                System.out.println("[SmartBattery] 机器输入电压 = " + inputV + " EU/t");

                // 电压值 -> 电压等级
                long[] stdV = GTValues.V;

                for (int i = stdV.length - 1; i >= 0; i--) {
                    if (inputV >= stdV[i]) return i; // 第一个 <= inputV 的档就是目标
                }
                return 0; // 比 ULV 还小，兜底 ULV

            } catch (Exception e) {
                // System.out.println("Error reading machine voltage: " + e.getMessage());
            }

            // 默认返回 LV (1)
            // System.out.println("Using default voltage tier: 1 (LV)");
            return 1;
        }

        private String getMachineName(IGregTechTileEntity gtTile) {
            try {
                if (gtTile.getMetaTileEntity() != null) {
                    return gtTile.getMetaTileEntity()
                        .getMetaName();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "未知机器";
        }

        private String getTierName(int tier) {
            if (tier >= 0 && tier < TIER_NAMES.length) {
                return TIER_NAMES[tier];
            }
            return "未知";
        }

        private long getVoltageValue(int tier) {
            if (tier >= 0 && tier < GTValues.V.length) {
                return GTValues.V[tier];
            }
            return GTValues.V[1]; // 默认 LV
        }

        private String formatNumber(long number) {
            if (number >= 1_000_000_000L) {
                return String.format("%.1fB", number / 1_000_000_000.0);
            } else if (number >= 1_000_000L) {
                return String.format("%.1fM", number / 1_000_000.0);
            } else if (number >= 1_000L) {
                return String.format("%.1fk", number / 1_000.0);
            }
            return String.valueOf(number);
        }
    }
}
