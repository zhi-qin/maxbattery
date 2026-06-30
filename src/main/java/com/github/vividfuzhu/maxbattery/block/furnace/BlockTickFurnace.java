package com.github.vividfuzhu.maxbattery.block.furnace;

import net.minecraft.block.BlockFurnace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Tick熔炉方块 - 外观与原版熔炉完全一致，使用 TileTickFurnace 实现100倍加速。
 *
 * 继承 BlockFurnace 复用原版熔炉的：
 * <ul>
 *   <li>贴图渲染（顶部/侧面/正面开火动画）</li>
 *   <li>面向方向逻辑（根据玩家朝向旋转）</li>
 *   <li>右键打开熔炉 GUI</li>
 *   <li>破坏时掉落内部物品</li>
 * </ul>
 *
 * 注意：不覆盖 updateFurnaceBlockState，因为 TileTickFurnace 已自行管理元数据，
 * 不会调用父类的替换方块逻辑，保证方块始终是此类型。
 */
public class BlockTickFurnace extends BlockFurnace {

    public BlockTickFurnace() {
        super(false);
        this.setBlockName("maxbattery.tick_furnace");
        this.setHardness(3.5F);
        this.setHarvestLevel("pickaxe", 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileTickFurnace();
    }

}
