package com.github.vividfuzhu.maxbattery.block.furnace;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;

import com.github.vividfuzhu.maxbattery.config.ModConfig;

/**
 * Tick熔炉 TileEntity - 100倍加速熔炉。
 *
 * 继承 TileEntityFurnace，重写 {@link #updateEntity()} 实现：
 * <ul>
 *   <li>燃料消耗加快100倍（每tick -100）</li>
 *   <li>炼制进度加快100倍（每tick +100）</li>
 *   <li>炼制阈值固定为2 ticks（原版200 ticks）</li>
 *   <li>燃料效率与原版一致（burnTime / totalCookTime 守恒）</li>
 * </ul>
 *
 * 参考 FastFurnace 项目的三处 ASM 字节码补丁逻辑。
 */
public class TileTickFurnace extends TileEntityFurnace {

    public TileTickFurnace() {
        super();
    }

    @Override
    public void updateEntity() {
        boolean wasBurning = this.furnaceBurnTime > 0;
        boolean dirty = false;

        // === 100倍燃料消耗 ===
        if (this.furnaceBurnTime > 0) {
            this.furnaceBurnTime -= ModConfig.FURNACE_FUEL_RATE;
            if (this.furnaceBurnTime < 0) {
                this.furnaceBurnTime = 0;
            }
        }

        if (!this.worldObj.isRemote) {
            ItemStack fuel = this.getStackInSlot(1);

            // === 燃料耗尽时尝试消耗新燃料 ===
            if (this.furnaceBurnTime <= 0 && canSmeltInternal()) {
                int burnTime = getItemBurnTime(fuel);
                if (burnTime > 0) {
                    this.currentItemBurnTime = burnTime;
                    this.furnaceBurnTime = burnTime;

                    if (fuel != null) {
                        if (fuel.getItem().hasContainerItem(fuel)) {
                            this.setInventorySlotContents(
                                1,
                                fuel.getItem().getContainerItem(fuel));
                        } else {
                            fuel.stackSize--;
                            if (fuel.stackSize <= 0) {
                                this.setInventorySlotContents(1, null);
                            }
                        }
                        dirty = true;
                    }
                }
            }

            // === 100倍炼制进度 ===
            if (this.furnaceBurnTime > 0 && canSmeltInternal()) {
                this.furnaceCookTime += ModConfig.FURNACE_COOK_AMOUNT;
                // 每2 tick完成一次（100+100=200），1煤=8物品
                if (this.furnaceCookTime >= ModConfig.FURNACE_COOK_THRESHOLD) {
                    this.furnaceCookTime = 0;
                    doSmeltItem();
                    dirty = true;
                }
            } else {
                this.furnaceCookTime = 0;
            }

            // === 更新方块元数据（燃烧动画） ===
            boolean isBurning = this.furnaceBurnTime > 0;
            if (wasBurning != isBurning) {
                dirty = true;
                int meta = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
                if (isBurning) {
                    meta |= 1;
                } else {
                    meta &= ~1;
                }
                this.worldObj.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, meta, 2);
            }
        }

        if (dirty) {
            this.markDirty();
        }
    }

    /**
     * 判断是否可以炼制（替代原版私有的 canSmelt 方法）
     */
    private boolean canSmeltInternal() {
        ItemStack input = this.getStackInSlot(0);
        if (input == null) return false;

        ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(input);
        if (result == null) return false;

        ItemStack output = this.getStackInSlot(2);
        if (output == null) return true;
        if (!output.isItemEqual(result)) return false;

        int total = output.stackSize + result.stackSize;
        return total <= this.getInventoryStackLimit() && total <= result.getMaxStackSize();
    }

    /** 执行一次炼制（替代原版私有的 smeltItem 方法） */
    private void doSmeltItem() {
        if (!canSmeltInternal()) return;

        ItemStack input = this.getStackInSlot(0);
        ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(input);
        ItemStack output = this.getStackInSlot(2);

        if (output == null) {
            this.setInventorySlotContents(2, result.copy());
        } else if (output.isItemEqual(result)) {
            output.stackSize += result.stackSize;
        }

        this.decrStackSize(0, 1);
    }

    @Override
    public String getInventoryName() {
        return "container.tickFurnace";
    }

}
