package com.github.vividfuzhu.maxbattery.machine.creosote;

import static gregtech.api.enums.Textures.BlockIcons.FLUID_IN_SIGN;
import static gregtech.api.enums.Textures.BlockIcons.ITEM_OUT_SIGN;
import static gregtech.api.enums.Textures.BlockIcons.MACHINE_BRONZE_SIDE;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.github.vividfuzhu.maxbattery.config.ModConfig;

import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEBasicMachine;
import gregtech.api.render.TextureFactory;

/**
 * 杂酚油→煤炭 转化机（青铜外壳，无电力消耗）。
 *
 * 输入：杂酚油（流体，仅限顶面和底面）
 * 输出：煤炭（物品，正面）
 * 效率：{@value ModConfig#CREOSOTE_PER_COAL} mB 杂酚油 → 1 煤炭
 *
 * 因为非电气化，覆写了 isElectric / isEnetInput / isEnetOutput / maxEUInput / maxEUOutput / maxEUStore
 * 模拟成"不耗电的电气机器"以复用 GT 的基础设施（流体槽、物品槽等）。
 */
public class MTECreosoteCoalConverter extends MTEBasicMachine {

    /* -------------------------------------------------- */
    /* 1. 构造器                                            */
    /* -------------------------------------------------- */

    public MTECreosoteCoalConverter(int aID, String aName, String aNameRegional) {
        super(
            aID,
            "creo_coal",
            "杂酚油炼煤机",
            0,
            1,
            new String[] {
                "将杂酚油转化为煤炭",
                "§9输入：杂酚油（流体）",
                "§a输出：煤炭（物品）",
                "§7无需电力，持续工作",
                "§7" + ModConfig.CREOSOTE_PER_COAL + "L:1",
                "§9§o科技与狠活,天顶星出品"
            },
            0,
            1,
            // 侧面（无功能纹理）
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE)),
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE)),
            // 正面 = 物品输出
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE), TextureFactory.of(ITEM_OUT_SIGN)),
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE), TextureFactory.of(ITEM_OUT_SIGN)),
            // 上面 = 流体输入
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE), TextureFactory.of(FLUID_IN_SIGN)),
            // 下面 = 流体输入
            TextureFactory.of(TextureFactory.of(MACHINE_BRONZE_SIDE), TextureFactory.of(FLUID_IN_SIGN))
        );
    }

    public MTECreosoteCoalConverter(String aName, int aTier, int aAmperage, String[] aDescription,
        ITexture[][][] aTextures, int aInputSlotCount, int aOutputSlotCount) {
        super(aName, aTier, aAmperage, aDescription, aTextures, aInputSlotCount, aOutputSlotCount);
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTECreosoteCoalConverter(
            mName, mTier, mAmperage, mDescriptionArray, mTextures,
            mInputSlotCount, mOutputItems.length);
    }

    /* -------------------------------------------------- */
    /* 2. 电气伪装（无电力但伪装成电气机器）                   */
    /* -------------------------------------------------- */

    @Override
    public boolean isElectric()   { return true; }

    @Override
    public boolean isEnetInput()  { return false; }

    @Override
    public boolean isEnetOutput() { return false; }

    @Override
    public long maxEUInput()      { return 0L; }

    @Override
    public long maxEUOutput()     { return 0L; }

    @Override
    public long maxEUStore()      { return 1L; }

    /* -------------------------------------------------- */
    /* 3. 输入/输出面配置                                     */
    /* -------------------------------------------------- */

    @Override
    public boolean isInputFacing(ForgeDirection side) {
        // 顶面和底面为流体输入，侧面不应参与输入
        return side == ForgeDirection.UP || side == ForgeDirection.DOWN;
    }

    @Override
    public boolean isOutputFacing(ForgeDirection side) {
        return side == getBaseMetaTileEntity().getFrontFacing();
    }

    @Override
    public boolean allowPutStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return false;
    }

    @Override
    public boolean allowPullStack(IGregTechTileEntity aBaseMetaTileEntity, int aIndex, ForgeDirection side,
        ItemStack aStack) {
        return side == getBaseMetaTileEntity().getFrontFacing()
            && aIndex >= getOutputSlot()
            && aIndex < getOutputSlot() + 1;
    }

    /* -------------------------------------------------- */
    /* 4. 流体相关                                          */
    /* -------------------------------------------------- */

    @Override
    public int getCapacity() {
        return ModConfig.CREOSOTE_TANK_CAPACITY;
    }

    @Override
    public boolean isFluidInputAllowed(FluidStack aFluid) {
        return aFluid != null && "creosote".equals(aFluid.getFluid().getName());
    }

    /* -------------------------------------------------- */
    /* 5. 核心逻辑 - 每20 tick执行一次                         */
    /* -------------------------------------------------- */

    @Override
    public void onPostTick(IGregTechTileEntity te, long tick) {
        if (te.isServerSide()) {
            if (tick % 20 == 0) {
                if (canWork()) {
                    doWork();
                }
            }
            if (tick % 20 == 10) {
                tryAutoOutput();
            }
        }
    }

    private boolean canWork() {
        if (mFluid == null || mFluid.amount < ModConfig.CREOSOTE_PER_COAL) {
            return false;
        }

        ItemStack out = mInventory[getOutputSlot()];
        ItemStack coal = new ItemStack(Items.coal);
        int currentCount = out == null ? 0 : out.stackSize;
        int maxStack = coal.getMaxStackSize();
        return currentCount + 1 <= maxStack;
    }

    private void doWork() {
        mFluid.amount -= ModConfig.CREOSOTE_PER_COAL;
        if (mFluid.amount <= 0) {
            mFluid = null;
        }

        ItemStack coal = new ItemStack(Items.coal, 1);
        int outputSlot = getOutputSlot();

        if (mInventory[outputSlot] == null) {
            mInventory[outputSlot] = coal.copy();
        } else {
            mInventory[outputSlot].stackSize += 1;
        }
    }

    /* -------------------------------------------------- */
    /* 6. 自动输出到相邻容器                                    */
    /* -------------------------------------------------- */

    private void tryAutoOutput() {
        ItemStack outStack = mInventory[getOutputSlot()];
        if (outStack == null) {
            return;
        }

        IGregTechTileEntity baseTE = getBaseMetaTileEntity();
        ForgeDirection outputSide = baseTE.getFrontFacing();
        TileEntity adjTE = baseTE.getTileEntityAtSide(outputSide);

        if (adjTE instanceof IInventory) {
            IInventory inv = (IInventory) adjTE;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                if (!inv.isItemValidForSlot(i, outStack)) {
                    continue;
                }

                ItemStack slotStack = inv.getStackInSlot(i);
                if (slotStack == null) {
                    inv.setInventorySlotContents(i, outStack.copy());
                    mInventory[getOutputSlot()] = null;
                    return;
                } else if (slotStack.isItemEqual(outStack)
                    && ItemStack.areItemStackTagsEqual(slotStack, outStack)
                    && slotStack.stackSize < slotStack.getMaxStackSize()) {
                    int space = slotStack.getMaxStackSize() - slotStack.stackSize;
                    int toMove = Math.min(space, outStack.stackSize);
                    slotStack.stackSize += toMove;
                    outStack.stackSize -= toMove;
                    if (outStack.stackSize <= 0) {
                        mInventory[getOutputSlot()] = null;
                    }
                    return;
                }
            }
        }
    }

}
