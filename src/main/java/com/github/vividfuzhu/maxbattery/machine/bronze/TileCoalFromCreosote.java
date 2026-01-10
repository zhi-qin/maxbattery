package com.github.vividfuzhu.maxbattery.machine.bronze;

import static gregtech.api.enums.Textures.BlockIcons.FLUID_IN_SIGN;
import static gregtech.api.enums.Textures.BlockIcons.MACHINE_BRONZE_SIDE;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.render.TextureFactory;

public class TileCoalFromCreosote extends MTEBasicMachine {

    /* -------------------------------------------------- */
    /* 1. 构造器 */
    /* -------------------------------------------------- */
    public TileCoalFromCreosote(int aID, String aName, String aNameRegional) {
        super(
            aID,
            "creo_coal",
            "杂酚油炼煤机",
            0,
            1, // 不耗电（aEUCapacity=0, aAmperage=1）
            new String[] { "将杂酚油转化为煤炭", "§9输入：杂酚油（流体）", "§a输出：煤炭（物品）", "§7无需电力，持续工作", "§71000L:3", "§9§o科技与狠活,天顶星出品" },
            0, // 输入物品槽：0
            1, // 输出物品槽：1
               // [0] 侧面（非正面）处于工作状态（Active）时的贴图
               // 例如：当机器正在运行且玩家从侧面观察时显示
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE)),

            // [1] 侧面（非正面）处于空闲状态（Inactive）时的贴图
            // 例如：机器未运行时从侧面看到的样子
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE)),

            // [2] 正面（朝向玩家的一面）处于工作状态（Active）时的贴图
            // 底层为青铜外壳，上层叠加“流体输入”标识（蓝色箭头）
            // 表示该面是流体输入口，且机器当前正在处理
            TextureFactory.of(
                TextureFactory.of(MACHINE_BRONZE_SIDE), // 底图：青铜机器外壳
                TextureFactory.of(FLUID_IN_SIGN) // 覆盖层：流体输入标识
            ),

            // [3] 正面（朝向玩家的一面）处于空闲状态（Inactive）时的贴图
            // 即使机器未运行，也保留流体输入标识，方便玩家识别功能
            TextureFactory.of(
                TextureFactory.of(MACHINE_BRONZE_SIDE), // 底图：青铜机器外壳
                TextureFactory.of(FLUID_IN_SIGN) // 覆盖层：流体输入标识（始终显示）
            )
        // 注：索引 [4] 到 [13] 未提供，将使用 MTEBasicMachine 内部默认贴图（如管道、顶部等）

        );
    }

    /** 克隆构造器 —— 必须保留 */
    public TileCoalFromCreosote(String aName, int aTier, int aAmperage, String[] aDescription, ITexture[][][] aTextures,
        int aInputSlotCount, int aOutputSlotCount) {
        super(aName, aTier, aAmperage, aDescription, aTextures, aInputSlotCount, aOutputSlotCount);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new TileCoalFromCreosote(
            mName,
            mTier,
            mAmperage,
            mDescriptionArray,
            mTextures,
            mInputSlotCount,
            mOutputItems.length);
    }

    /* -------------------------------------------------- */
    /* 2. 电力 —— 不耗电 */
    /* -------------------------------------------------- */
    @Override
    public boolean isElectric() {
        return false;
    }

    @Override
    public boolean isEnetInput() {
        return false;
    }

    @Override
    public long maxEUInput() {
        return 0L;
    }

    @Override
    public long maxEUStore() {
        return 0L; // ✅ 告诉外部：没有储能，不是电池
    }

    /* -------------------------------------------------- */
    /* 3. 流体 —— 只接杂酚油，单槽 */
    /* -------------------------------------------------- */
    @Override
    public int getCapacity() {
        return 16000; // 16 kL
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return aFluid != null && "creosote".equals(
            aFluid.getFluid()
                .getName());
    }

    /* -------------------------------------------------- */
    /* 4. 每 tick 逻辑 */
    /* -------------------------------------------------- */
    @Override
    public void onPostTick(IGregTechTileEntity te, long tick) {
        super.onPostTick(te, tick);
        if (te.isServerSide() && tick % 20 == 0) {
            if (canWork()) doWork();
        }
    }

    private boolean canWork() {
        if (mFluid == null || mFluid.amount < 1000) return false; // 改为 1000 mB

        ItemStack out = mInventory[getOutputSlot()];
        ItemStack coal = new ItemStack(Items.coal);
        // 检查是否能放入 3 个煤炭（避免一次产 3 个导致溢出）
        int currentCount = out == null ? 0 : out.stackSize;
        int maxStack = coal.getMaxStackSize(); // 通常是 64
        return currentCount + 3 <= maxStack;
    }

    private void doWork() {
        // 耗 1000 mB 杂酚油
        mFluid.amount -= 1000;
        if (mFluid.amount <= 0) {
            mFluid = null;
        }

        // 产 3 个煤炭
        ItemStack coal = new ItemStack(Items.coal, 1); // 直接创建 stackSize=1 的堆
        int outputSlot = getOutputSlot();

        if (mInventory[outputSlot] == null) {
            mInventory[outputSlot] = coal.copy();
        } else {
            mInventory[outputSlot].stackSize += 1;
        }
    }
}
