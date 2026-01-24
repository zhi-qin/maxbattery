// 文件路径: src/main/java/com/github/vividfuzhu/maxbattery/item/SmartBattery.java
package com.github.vividfuzhu.maxbattery.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaBaseItem;

public class SmartBattery extends MetaBaseItem {

    // --- 将原来的静态常量移到这里，并改为 public static，以便主类可以访问 ---
    public static SmartBattery ITEM_SMART_BATTERY; // 静态实例，在主类 preInit 中初始化

    private static final String[] TIER_NAMES = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV",
        "UIV", "UMV", "UXV", "MAX", "OPV" };

    // 静态初始化块，用于延迟初始化（虽然实际初始化在 MaxBattery.preInit 中进行，但声明在这里）
    static {
        // INSTANCE 将在 MaxBattery.preInit 中被赋值
        ITEM_SMART_BATTERY = null;
    }

    public SmartBattery() {
        super("maxbattery.smart");
        this.setCreativeTab(GregTechAPI.TAB_GREGTECH);
        this.setMaxStackSize(1);
        String textureName = "maxbattery:maxbattery_smart";
        this.setTextureName(textureName);
        System.out.println("[SmartBattery Constructor] Texture name set to: " + textureName);
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
                            "§a[智能电池] §7已适配电压：" + "§c" + oldName + " §7→ §a" + newName + " §7(" + machineName + ")"));
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
