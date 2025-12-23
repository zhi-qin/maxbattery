package com.github.vividfuzhu.maxbattery.machine.miner;

import static gregtech.api.enums.GTValues.V;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.modularui.IAddUIWidgets;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.recipe.BasicUIProperties;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTLog;
import gregtech.api.util.GTUtility;
import gregtech.common.blocks.BlockOresAbstract;
import gregtech.common.blocks.TileEntityOres;

/**
 * 高速采矿机 - 性能优化的20倍加速无管道采矿机
 * 扫描从机器 Y 层向下至 Y=0 的所有矿石，分帧扫描避免卡顿
 *
 * 主要特性：
 * - 性能优化的20倍挖掘速度提升
 * - 智能批量处理，避免重复循环
 * - 无需外部加速设备
 * - 自动扫描工作区域内所有矿石
 * - 分帧扫描避免服务器卡顿
 * - 双输出槽设计提高吞吐量
 */
public class MaxBatteryMiner extends MTEBasicMachine implements IAddUIWidgets {

    /**
     * 不同等级机器的工作半径配置
     * LV/MV: 8格, HV: 16格, EV: 24格, IV: 32格
     */
    static final int[] RADIUS = { 8, 8, 16, 24, 32 };

    /**
     * 不同等级机器的挖掘速度（刻数）- 性能优化的20倍加速版本
     * 原值: {160, 160, 80, 40, 20} -> 现值: {8, 8, 4, 2, 1}
     * 挖掘时间: LV/MV: 0.4秒, HV: 0.2秒, EV: 0.1秒, IV: 0.05秒
     */
    static final int[] SPEED = { 8, 8, 4, 2, 1 }; // 原值除以20实现20倍加速

    /**
     * 不同等级机器的能耗（EU/t）- 为了保持总能耗不变，这里乘以20
     * 原值: {8, 8, 32, 128, 512} -> 现值: {160, 160, 640, 2560, 10240}
     * 总能耗保持一致：160*8=1280, 160*8=1280, 640*4=2560, 2560*2=5120, 10240*1=10240
     */
    static final int[] ENERGY = { 160, 160, 640, 2560, 10240 };

    /**
     * 当前配置的工作半径，可通过螺丝刀调节
     */
    private int radiusConfig;

    /**
     * 存储扫描到的所有矿石位置的列表
     * 使用相对坐标存储以节省内存
     */
    private final ArrayList<ChunkPosition> oreBlockPositions = new ArrayList<>();

    /**
     * 当前挖掘进度计数器
     * 每tick增加1，达到mSpeed时执行一次挖掘
     */
    private int currentMiningProgress = 0;

    /**
     * 是否已完成初始扫描的标志
     * true表示已扫描完成，开始挖掘阶段
     * false表示仍在扫描阶段
     */
    private boolean hasScanned = false;

    /**
     * 当前扫描的Y坐标
     * 从机器Y坐标开始向下扫描，逐步减小
     * 初始值为-1，表示未开始扫描
     */
    private int scanYCursor = -1;

    /**
     * 每tick处理的层数，用于分帧扫描避免卡顿
     * 设置为2表示每tick扫描2层
     */
    private static final int LAYERS_PER_TICK = 2;

    /**
     * 当前机器的速度配置值
     * 根据机器等级确定，对应SPEED数组中的值
     */
    private final int mSpeed;

    /**
     * 构造函数 - 用于注册机器时调用
     *
     * 调用父类MTEBasicMachine的构造函数，初始化机器的基本属性
     * 包括：机器ID、名称、等级、输入输出总线、描述信息、纹理等
     *
     * @param aID           机器ID - 在配置文件中注册的唯一标识符
     * @param aName         机器内部名称 - 用于机器注册和识别
     * @param aNameRegional 机器显示名称 - 翻译键，显示在UI中
     * @param aTier         机器等级（LV=1, MV=2, HV=3, EV=4, IV=5）
     *
     *                      参数说明：
     *                      - 第4个参数(1): 输入总线数量 - 修复电流计算问题，确保电力输入正确
     *                      - 第6个参数(2): 输出槽数量 - 支持双输出槽设计，提高吞吐量
     *                      - 第7个参数(1): 输出物品数量 - 每次处理1个物品
     *
     *                      描述信息包括：
     *                      - "High-speed ore miner without pipes": 高速无管道采矿机
     *                      - "Built-in 20x speed boost": 内置20倍加速
     *                      - "Use Screwdriver to regulate work area": 使用螺丝刀调节工作区域
     *                      - 能耗和挖掘时间信息（根据等级动态生成）
     *                      - 最大工作区域信息（根据等级动态生成）
     *                      - 小型矿石幸运加成（等级值）
     *
     *                      纹理设置包括：侧面、前面、上面、下面的激活/非激活状态纹理
     */
    public MaxBatteryMiner(int aID, String aName, String aNameRegional, int aTier) {
        super(
            aID, // 机器ID - 注册时的唯一标识符
            aName, // 机器内部名称 - 用于代码识别
            aNameRegional, // 机器显示名称 - 翻译键
            aTier, // 机器等级 - 决定性能参数
            1, // 输入总线数量 - 修复电流计算问题，确保电力输入正确
            new String[] { // 机器描述信息 - 显示在物品提示中
                "高速无管道采矿机", // 高速无管道采矿机
                "内置20倍加速", // 内置20倍加速
                "使用螺丝刀调节工作区域", // 使用螺丝刀调节工作区域
                String.format("%d EU/t, %.1f秒/方块", ENERGY[aTier], SPEED[aTier] / 20.0), // 能耗和挖掘时间
                String.format("最大工作区域 %dx%d", (RADIUS[aTier] * 2 + 1), (RADIUS[aTier] * 2 + 1)), // 最大工作区域
                String.format("小型矿石幸运加成 %d", aTier) // 小型矿石幸运加成
            },
            2, // 物理输出总线数量 - 分配2个底层输出槽位（用于存储和掉落），但UI不一定全部显示
            2, // UI输出槽显示数量 / 最大输出物品种类数 - GUI仅显示1个输出槽，且每次最多输出1种物品
               // 下面是各种面的纹理定义 - 激活/非激活状态的纹理
            TextureFactory.of( // 侧面激活纹理 - 显示机器工作状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_SIDE_ACTIVE")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_SIDE_ACTIVE_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 侧面纹理 - 非激活状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_SIDE")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_SIDE_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 前面激活纹理 - 显示机器工作状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_FRONT_ACTIVE")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_FRONT_ACTIVE_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 前面纹理 - 非激活状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_FRONT")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_FRONT_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 上面激活纹理 - 显示机器工作状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_TOP_ACTIVE")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_TOP_ACTIVE_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 上面纹理 - 非激活状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_TOP")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_TOP_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 下面激活纹理 - 显示机器工作状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_BOTTOM_ACTIVE")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_BOTTOM_ACTIVE_GLOW"))
                    .glow()
                    .build()),
            TextureFactory.of( // 下面纹理 - 非激活状态
                TextureFactory.of(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_BOTTOM")),
                TextureFactory.builder()
                    .addIcon(new Textures.BlockIcons.CustomIcon("basicmachines/miner/OVERLAY_BOTTOM_GLOW"))
                    .glow()
                    .build()));
        mSpeed = SPEED[aTier]; // 根据机器等级设置挖掘速度（对应SPEED数组中的值）
        radiusConfig = RADIUS[mTier]; // 根据机器等级设置默认工作半径（对应RADIUS数组中的值）
    }

    /**
     * 构造函数 - 用于复制机器时调用（如世界加载、机器复制等）
     *
     * 这个构造函数用于在游戏运行时创建机器实例，比如：
     * - 从NBT数据加载机器
     * - 复制机器（如使用扳手复制）
     * - 世界加载时重新创建机器实例
     *
     * @param aName        机器内部名称 - 用于机器识别
     * @param aTier        机器等级 - 决定性能参数
     * @param aDescription 机器描述 - 显示在UI和物品提示中
     * @param aTextures    机器纹理 - 各个面的纹理定义
     *
     *                     参数说明：
     *                     - aName: 机器内部名称，用于代码识别
     *                     - aTier: 机器等级，影响性能参数
     *                     - aDescription: 机器描述信息数组，显示在UI中
     *                     - aTextures: 机器纹理定义，包含各个面的激活/非激活纹理
     *
     *                     修复参数：
     *                     - 输入总线数: 1，确保电流计算正确
     *                     - 输出槽数: 2，支持双输出槽设计
     *                     - 输出物品数: 1，每次处理一个物品
     */
    public MaxBatteryMiner(String aName, int aTier, String[] aDescription, ITexture[][][] aTextures) {
        super(aName, aTier, 1, aDescription, aTextures, 2, 2); // 修复参数：输入总线数1，输出槽2个，输出物品数1
        mSpeed = SPEED[aTier]; // 根据等级设置挖掘速度
        radiusConfig = RADIUS[mTier]; // 根据等级设置默认工作半径
    }

    /**
     * 创建新的MetaTileEntity实例
     * 用于机器复制和世界加载
     *
     * @param aTileEntity 关联的TileEntity
     * @return 新的MetaTileEntity实例
     */
    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MaxBatteryMiner(mName, mTier, mDescriptionArray, mTextures);
    }

    /**
     * 获取机器最大EU存储量
     * 为了适应20倍能耗，增加存储容量
     *
     * @return 最大EU存储量
     */
    @Override
    public long maxEUStore() {
        return Math.max(V[mTier] * 64L, 4096L); // 增加20倍存储容量 否 我删了一个 * 20L
    }

    /**
     * 获取机器最大EU输入
     *
     * @return 最大EU输入量
     */
    @Override
    public long maxEUInput() {
        return V[mTier]; // 确保输入电压正确
    }

    /**
     * 获取机器最大安培输入
     * 为了适应20倍能耗，增加电流容量
     *
     * @return 最大安培输入量
     */
    @Override
    public long maxAmperesIn() {
        return 40L; // 增加到40A以适应高能耗
    }

    /**
     * 检查是否为能量输入端口
     *
     * @return true表示是能量输入端口
     */
    @Override
    public boolean isEnetInput() {
        return true;
    }

    /**
     * 检查是否为电力机器
     *
     * @return true表示是电力机器
     */
    @Override
    public boolean isElectric() {
        return true;
    }

    /**
     * 检查指定面是否为输入面
     * 除了前面和主面外，其他面都可以输入
     *
     * @param side 方向
     * @return true表示是输入面
     */
    @Override
    public boolean isInputFacing(ForgeDirection side) {
        return side != getBaseMetaTileEntity().getFrontFacing() && side != mMainFacing;
    }

    /**
     * 检查指定面是否为输出面
     * 采矿机没有输出面，所有输出通过内部槽位
     *
     * @param side 方向
     * @return true表示是输出面
     */
    @Override
    public boolean isOutputFacing(ForgeDirection side) {
        return false;
    }

    /**
     * 检查玩家是否有权限访问机器
     *
     * @param aPlayer 玩家
     * @return true表示允许访问
     */
    @Override
    public boolean isAccessAllowed(EntityPlayer aPlayer) {
        return true;
    }

    /**
     * 检查指定槽位是否为有效槽位
     * 只有输出槽是有效的
     *
     * @param aIndex 槽位索引
     * @return true表示是有效槽位
     */
    // @Override
    // public boolean isValidSlot(int aIndex) {
    // return aIndex >= getOutputSlot() && aIndex < getOutputSlot() + 2; // 支持2个输出槽
    // }
    // 处理掉落逻辑的代码块，有点问题

    /**
     * 机器第一次tick时调用
     * 初始化扫描状态
     *
     * @param aBaseMetaTileEntity 基础元TileEntity
     */
    @Override
    public void onFirstTick(IGregTechTileEntity aBaseMetaTileEntity) {
        if (aBaseMetaTileEntity.isServerSide()) {
            scanYCursor = aBaseMetaTileEntity.getYCoord(); // 从机器所在 Y 开始向下扫描
            hasScanned = false; // 标记为未扫描完成
            oreBlockPositions.clear(); // 清空矿石位置列表
        }
    }

    /**
     * 检查是否有足够的输出空间
     * 检查两个输出槽是否还有空间存放物品
     *
     * @return true表示有足够空间
     */
    public boolean hasFreeSpace() {
        for (int i = getOutputSlot(); i < getOutputSlot() + 2; i++) { // 检查2个输出槽
            if (mInventory[i] == null) return true; // 槽位为空
            if (mInventory[i] != null && mInventory[i].stackSize < mInventory[i].getMaxStackSize()) return true; // 槽位未满
        }
        return false; // 所有槽位都满了
    }

    /**
     * 使用螺丝刀右键点击时调用
     * 用于调节工作半径
     *
     * @param side    点击的面
     * @param aPlayer 玩家
     * @param aX      X坐标
     * @param aY      Y坐标
     * @param aZ      Z坐标
     * @param aTool   工具
     */
    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer aPlayer, float aX, float aY, float aZ,
        ItemStack aTool) {
        if (side != getBaseMetaTileEntity().getFrontFacing() && side != mMainFacing) {
            if (aPlayer.isSneaking()) {
                radiusConfig = Math.max(0, radiusConfig - 1); // 潜行点击减小半径
            } else {
                radiusConfig = Math.min(RADIUS[mTier], radiusConfig + 1); // 普通点击增大半径
            }

            // 发送当前工作区域信息给玩家
            GTUtility.sendChatToPlayer(
                aPlayer,
                String.format(
                    "%s %dx%d", // 格式："工作区域已设置为 XxY"
                    StatCollector.translateToLocal("GT5U.machines.workareaset"), // 翻译键
                    (radiusConfig * 2 + 1), // X轴范围
                    (radiusConfig * 2 + 1))); // Y轴范围

            // 重置扫描状态，触发重新扫描
            scanYCursor = getBaseMetaTileEntity().getYCoord();
            hasScanned = false;
            oreBlockPositions.clear();
        }
    }

    /**
     * 每tick调用一次的主要逻辑
     * 处理扫描和挖掘两个阶段
     *
     * @param aBaseMetaTileEntity 基础元TileEntity
     * @param aTick               当前tick数
     */
    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick); // 调用父类方法

        if (!aBaseMetaTileEntity.isServerSide()) return; // 只在服务端执行

        // 阶段 1：分帧扫描
        if (!hasScanned) { // 如果还没有扫描完成
            if (scanYCursor >= 0) { // 如果还在有效Y范围内
                boolean madeProgress = false;
                // 每tick最多扫描LAYERS_PER_TICK层，避免卡顿
                for (int layer = 0; layer < LAYERS_PER_TICK && scanYCursor >= 0; layer++) {
                    scanLayer(aBaseMetaTileEntity, scanYCursor); // 扫描当前层
                    scanYCursor--; // Y坐标减1
                    madeProgress = true;
                }
                if (scanYCursor < 0) { // 如果扫描完成
                    hasScanned = true; // 标记为已扫描完成
                    if (gregtech.api.enums.GTValues.debugBlockMiner) { // 如果启用调试
                        GTLog.out
                            .println("MAXBATTERY MINER: Scan completed, found " + oreBlockPositions.size() + " ores");
                    }
                }
            } else {
                hasScanned = true; // Y坐标小于0，扫描完成
            }
            return; // 扫描阶段结束，返回
        }

        // 阶段 2：挖掘阶段
        if (!aBaseMetaTileEntity.isAllowedToWork()) return; // 检查机器是否被允许工作
        if (!hasFreeSpace()) return; // 检查是否有输出空间
        if (!aBaseMetaTileEntity.isUniversalEnergyStored(ENERGY[mTier])) return; // 检查是否有足够能源

        // 性能优化：一次性处理20倍进度，而不是循环20次
        int totalProgressToAdd = 20; // 20倍加速

        // 消耗能源（一次性消耗20倍的能源）
        if (!aBaseMetaTileEntity.decreaseStoredEnergyUnits(ENERGY[mTier] * 20, false)) {
            // 如果能源不足，按比例减少进度
            long availableEnergy = aBaseMetaTileEntity.getUniversalEnergyStored();
            int possibleTicks = (int) (availableEnergy / ENERGY[mTier]);
            if (possibleTicks <= 0) return; // 没有足够能源执行一次挖掘
            totalProgressToAdd = Math.min(20, possibleTicks);
            aBaseMetaTileEntity.decreaseStoredEnergyUnits(ENERGY[mTier] * totalProgressToAdd, true);
        } else {
            // 能源充足，消耗全部20倍能源
            aBaseMetaTileEntity.decreaseStoredEnergyUnits(ENERGY[mTier] * 20, true);
        }

        // 累加进度
        currentMiningProgress += totalProgressToAdd;

        // 一次性处理所有可能的挖掘
        while (currentMiningProgress >= mSpeed && !oreBlockPositions.isEmpty()) {
            currentMiningProgress -= mSpeed; // 减去阈值
            mineNextOre(aBaseMetaTileEntity); // 挖掘下一个矿石
        }

        // 如果矿石挖完了，重新扫描
        if (oreBlockPositions.isEmpty()) {
            scanYCursor = aBaseMetaTileEntity.getYCoord();
            hasScanned = false;
            oreBlockPositions.clear();
        }
    }

    /**
     * 扫描指定Y层的矿石
     * 遍历工作区域内所有方块，找到矿石并记录位置
     *
     * @param aBaseMetaTileEntity 基础元TileEntity
     * @param scanY               要扫描的Y坐标
     */
    private void scanLayer(IGregTechTileEntity aBaseMetaTileEntity, int scanY) {
        int machineX = aBaseMetaTileEntity.getXCoord(); // 获取机器X坐标
        int machineZ = aBaseMetaTileEntity.getZCoord(); // 获取机器Z坐标

        // 遍历工作区域内所有方块
        for (int dz = -radiusConfig; dz <= radiusConfig; dz++) { // Z轴偏移
            for (int dx = -radiusConfig; dx <= radiusConfig; dx++) { // X轴偏移
                int worldX = machineX + dx; // 计算世界X坐标
                int worldZ = machineZ + dz; // 计算世界Z坐标

                // 检查区块是否存在且地形已生成，避免访问无效区块
                int chunkX = worldX >> 4; // 计算区块X坐标
                int chunkZ = worldZ >> 4; // 计算区块Z坐标
                if (!aBaseMetaTileEntity.getWorld()
                    .getChunkProvider()
                    .chunkExists(chunkX, chunkZ)) {
                    continue; // 区块不存在，跳过
                }
                net.minecraft.world.chunk.Chunk chunk = aBaseMetaTileEntity.getWorld()
                    .getChunkFromChunkCoords(chunkX, chunkZ);
                if (!chunk.isTerrainPopulated) {
                    continue; // 区块地形未生成完成，跳过
                }

                // 获取方块和元数据
                Block block = aBaseMetaTileEntity.getWorld()
                    .getBlock(worldX, scanY, worldZ);
                int meta = aBaseMetaTileEntity.getWorld()
                    .getBlockMetadata(worldX, scanY, worldZ);

                // 检查是否为GregTech矿石
                if (block instanceof BlockOresAbstract) {
                    TileEntity te = aBaseMetaTileEntity.getWorld()
                        .getTileEntity(worldX, scanY, worldZ);
                    // 检查是否为天然矿石
                    if (te instanceof TileEntityOres && ((TileEntityOres) te).mNatural) {
                        // 添加到矿石位置列表（使用相对坐标）
                        oreBlockPositions.add(new ChunkPosition(dx, scanY - aBaseMetaTileEntity.getYCoord(), dz));
                    }
                } else if (GTUtility.isOre(block, meta)) { // 检查是否为其他模组的矿石
                    // 添加到矿石位置列表（使用相对坐标）
                    oreBlockPositions.add(new ChunkPosition(dx, scanY - aBaseMetaTileEntity.getYCoord(), dz));
                }
            }
        }
    }

    /**
     * 挖掘下一个矿石
     * 从矿石位置列表中取出一个位置，挖掘对应的方块
     *
     * @param aBaseMetaTileEntity 基础元TileEntity
     */
    private void mineNextOre(IGregTechTileEntity aBaseMetaTileEntity) {
        if (oreBlockPositions.isEmpty()) return; // 如果没有待挖掘矿石，返回

        ChunkPosition pos = oreBlockPositions.remove(0); // 取出第一个矿石位置
        int x = aBaseMetaTileEntity.getXCoord() + pos.chunkPosX; // 计算世界X坐标
        int y = aBaseMetaTileEntity.getYCoord() + pos.chunkPosY; // 计算世界Y坐标
        int z = aBaseMetaTileEntity.getZCoord() + pos.chunkPosZ; // 计算世界Z坐标

        // 二次验证：确保方块仍存在且是有效矿石
        if (!aBaseMetaTileEntity.getWorld()
            .blockExists(x, y, z)) {
            return; // 方块不存在，跳过
        }

        Block block = aBaseMetaTileEntity.getWorld()
            .getBlock(x, y, z);
        int meta = aBaseMetaTileEntity.getWorld()
            .getBlockMetadata(x, y, z);

        boolean isValidOre = false;
        if (block instanceof BlockOresAbstract) { // 检查是否为GregTech矿石
            TileEntity te = aBaseMetaTileEntity.getWorld()
                .getTileEntity(x, y, z);
            if (te instanceof TileEntityOres && ((TileEntityOres) te).mNatural) {
                isValidOre = true;
            }
        } else if (GTUtility.isOre(block, meta)) { // 检查是否为其他模组矿石
            isValidOre = true;
        }

        if (!isValidOre) { // 如果不是有效矿石，跳过
            return;
        }

        // 获取矿石掉落物
        List<ItemStack> drops = block.getDrops(aBaseMetaTileEntity.getWorld(), x, y, z, meta, mTier);
        for (ItemStack drop : drops) {
            if (drop != null && drop.stackSize > 0) {
                if (!addOutputToSlot(drop.copy())) { // 尝试添加到输出槽
                    // 如果输出槽满了，直接在世界上生成物品
                    aBaseMetaTileEntity.getWorld()
                        .spawnEntityInWorld(
                            new net.minecraft.entity.item.EntityItem(
                                aBaseMetaTileEntity.getWorld(),
                                x + 0.5,
                                y + 0.5,
                                z + 0.5,
                                drop.copy()));
                }
            }
        }
        aBaseMetaTileEntity.getWorld()
            .setBlockToAir(x, y, z); // 破坏方块
    }

    /**
     * 将物品添加到输出槽
     * 优先合并相同物品，然后寻找空槽位
     *
     * @param stack 要添加的物品栈
     * @return true表示成功添加
     */
    private boolean addOutputToSlot(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return false; // 无效物品栈

        // 遍历2个输出槽
        for (int slotOffset = 0; slotOffset < 2; slotOffset++) {
            int slot = getOutputSlot() + slotOffset; // 计算实际槽位索引
            if (mInventory[slot] == null) { // 如果槽位为空
                mInventory[slot] = stack.copy(); // 直接放入
                return true;
            } else if (GTUtility.areStacksEqual(mInventory[slot], stack) // 如果物品相同
                && mInventory[slot].stackSize + stack.stackSize <= mInventory[slot].getMaxStackSize()) { // 且合并后不超过最大堆叠数
                    mInventory[slot].stackSize += stack.stackSize; // 合并堆叠
                    return true;
                }
        }
        return false; // 所有槽位都不合适
    }

    /**
     * 检查是否允许从指定槽位拉取物品
     * 只允许从输出槽拉取物品
     *
     * @param aBaseMetaTileEntity 基础元TileEntity
     * @param aIndex              槽位索引
     * @param side                方向
     * @param aStack              物品栈
     * @return true表示允许拉取
     */
    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return aIndex >= getOutputSlot() && aIndex < getOutputSlot() + 2; // 允许从2个输出槽拉取
    }

    /**
     * 检查是否允许向指定槽位放入物品
     * 采矿机不允许放入物品
     *
     * @param aBaseMetaTileEntity 基础元TileEntity
     * @param aIndex              槽位索引
     * @param side                方向
     * @param aStack              物品栈
     * @return true表示允许放入
     */
    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false; // 采矿机不允许放入物品
    }

    /**
     * 设置物品NBT数据
     * 用于机器物品的序列化
     *
     * @param aNBT NBT标签
     */
    @Override
    public void setItemNBT(NBTTagCompound aNBT) {
        super.setItemNBT(aNBT);
        if (radiusConfig != RADIUS[mTier]) aNBT.setInteger("radiusConfig", radiusConfig); // 只在非默认值时保存
    }

    /**
     * 保存NBT数据
     * 用于机器数据的持久化存储
     *
     * @param aNBT NBT标签
     */
    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("radiusConfig", radiusConfig); // 保存半径配置
        aNBT.setInteger("currentMiningProgress", currentMiningProgress); // 保存当前进度
        aNBT.setBoolean("hasScanned", hasScanned); // 保存扫描状态
        aNBT.setInteger("scanYCursor", scanYCursor); // 保存扫描光标
    }

    /**
     * 加载NBT数据
     * 用于机器数据的恢复
     *
     * @param aNBT NBT标签
     */
    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        if (aNBT.hasKey("radiusConfig")) {
            // 加载半径配置，确保在有效范围内
            radiusConfig = Math.min(RADIUS[mTier], Math.max(0, aNBT.getInteger("radiusConfig")));
        }
        if (aNBT.hasKey("currentMiningProgress")) {
            currentMiningProgress = aNBT.getInteger("currentMiningProgress"); // 加载当前进度
        }
        if (aNBT.hasKey("hasScanned")) {
            hasScanned = aNBT.getBoolean("hasScanned"); // 加载扫描状态
        }
        if (aNBT.hasKey("scanYCursor")) {
            scanYCursor = aNBT.getInteger("scanYCursor"); // 加载扫描光标
        }
    }

    /**
     * 获取机器信息数据
     * 显示在WAILA/HWYLA等工具中
     *
     * @return 信息字符串数组
     */
    @Override
    public String[] getInfoData() {
        return new String[] {
            EnumChatFormatting.BLUE + StatCollector.translateToLocal("GT5U.machines.miner") + EnumChatFormatting.RESET, // "采矿机"
            StatCollector.translateToLocal("GT5U.machines.workarea") + ": " // "工作区域:"
                + EnumChatFormatting.GREEN
                + (radiusConfig * 2 + 1)
                + "x"
                + (radiusConfig * 2 + 1)
                + EnumChatFormatting.RESET
                + " "
                + StatCollector.translateToLocal("GT5U.machines.blocks"), // "方块"
            StatCollector.translateToLocal("GT5U.machines.speed") + ": " // "速度:"
                + EnumChatFormatting.RED
                + "20x " // 20倍速度
                + EnumChatFormatting.RESET
                + StatCollector.translateToLocal("GT5U.machines.faster") }; // "更快"
    }

    /**
     * 获取UI属性
     * 设置进度条纹理
     *
     * @return 基本UI属性
     */
    @Override
    protected BasicUIProperties getUIProperties() {
        return super.getUIProperties().toBuilder()
            .progressBarTexture(GTUITextures.fallbackableProgressbar("miner", GTUITextures.PROGRESSBAR_CANNER)) // 设置采矿机进度条纹理
            .build();
    }

    /**
     * 获取活动音效循环
     *
     * @return 音效资源
     */
    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GTCEU_LOOP_MINER; // 返回采矿机音效
    }

    /**
     * 机器爆炸时调用
     * 将所有库存物品丢出到世界中
     */
    @Override
    public void onExplosion() {
        // 遍历2个输出槽
        for (int i = getOutputSlot(); i < getOutputSlot() + 2; i++) {
            if (mInventory[i] != null) { // 如果槽位不为空
                getBaseMetaTileEntity().getWorld()
                    .spawnEntityInWorld(
                        new net.minecraft.entity.item.EntityItem( // 在世界上生成物品实体
                            getBaseMetaTileEntity().getWorld(),
                            getBaseMetaTileEntity().getXCoord() + 0.5, // X坐标（中心点）
                            getBaseMetaTileEntity().getYCoord() + 0.5, // Y坐标（中心点）
                            getBaseMetaTileEntity().getZCoord() + 0.5, // Z坐标（中心点）
                            mInventory[i])); // 物品栈
                mInventory[i] = null; // 清空槽位
            }
        }
    }

    /**
     * 适配加速火把 - 提供增加进度的方法
     *
     * @param aProgressAmount 进度增加量
     */
    public void increaseProgressForAccelerator(int aProgressAmount) {
        // 加速当前的挖掘进度
        currentMiningProgress += aProgressAmount;

        // 检查是否达到挖掘阈值
        if (currentMiningProgress >= mSpeed) {
            currentMiningProgress = 0;
            if (oreBlockPositions.isEmpty()) {
                // 重新扫描
                scanYCursor = getBaseMetaTileEntity().getYCoord();
                hasScanned = false;
                oreBlockPositions.clear();
                return;
            }
            // 如果有可用矿石，立即挖掘
            mineNextOre(getBaseMetaTileEntity());
        }
    }

    /**
     * 返回当前进度 - 供加速火把使用
     *
     * @return 当前进度值
     */
    public int getProgresstime() {
        // 如果正在挖掘，返回当前进度；否则返回0
        return currentMiningProgress > 0 ? currentMiningProgress : 0;
    }
}
