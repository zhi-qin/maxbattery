//// package com.github.vividfuzhu.maxbattery.wire;
//
// package com.github.vividfuzhu.maxbattery.wire;
//
// import net.minecraft.client.renderer.texture.IIconRegister;
// import net.minecraft.creativetab.CreativeTabs;
// import net.minecraft.item.Item;
// import net.minecraft.item.ItemStack;
// import cpw.mods.fml.relauncher.Side;
// import cpw.mods.fml.relauncher.SideOnly;
//
// public class Maxwire extends Item {
//
// public Maxwire() {
// setUnlocalizedName("maxbattery.superconductor_cable"); // 注册名
// setCreativeTab(CreativeTabs.tabMisc); // 或 GregTechAPI.TAB_GREGTECH
// setMaxStackSize(64);
// }
//
// @Override
// @SideOnly(Side.CLIENT)
// public void registerIcons(IIconRegister reg) {
// // 关键：复用 GT 超导线缆的贴图！
// // GT 线缆贴图命名规则通常是：material_name + _ + prefix
// // 例如：superconductor_cableGt16
// itemIcon = reg.registerIcon("gregtech:materialicons/SUPERCONDUCTOR/cableGt16");
// }
//
// // 可选：让名字显示为“超导线缆 (16A)”
// @Override
// public String getItemStackDisplayName(ItemStack stack) {
// return "超导线缆 (16A)";
// }
// }
