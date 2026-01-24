package com.github.vividfuzhu.maxbattery.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;

// import gregtech.api.GregTechAPI; // 如需

/**
 * 无限冷却单元
 * 该物品实现 IReactorComponent 接口，以便在 IC2 核反应堆中作为冷却组件使用。
 * 它旨在模拟一个拥有无限容量且能持续从反应堆整体温度中吸收热量的组件。
 */
public class InfiniteCoolantCell extends Item implements IReactorComponent { // 实现 IReactorComponent

    public InfiniteCoolantCell() {
        super();
        setUnlocalizedName("infiniteCoolantCell"); // 设置本地化键名 (旧版方式)
        setTextureName("maxbattery:infinite_coolant_cell"); // 设置图标纹理名称 (旧版方式)
        // setCreativeTab(GregTechAPI.TAB_GREGTECH); // 设置创造模式标签页 (如需)
        setMaxStackSize(64); // 设置最大堆叠数量
    }

    // --- 新增：直接在代码中定义显示名称 ---
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return "无限冷却热熔"; // 直接返回你想要的中文名称
    }

    // --- IReactorComponent 接口方法 ---

    /**
     * 在反应堆循环中处理此组件。
     * 通常用于执行一些持续性的逻辑，比如处理材料、触发效果等。
     * 对于纯冷却剂，一般不需要在此处做特殊处理。
     *
     * @param aReactor 反应堆实例
     * @param aStack   当前组件的 ItemStack
     * @param x        组件在反应堆中的 X 坐标 (0-8)
     * @param y        组件在反应堆中的 Y 坐标 (0-8)
     * @param aHeatRun 是否处于热量运行模式 (通常为 true)
     */
    @Override
    public void processChamber(IReactor aReactor, ItemStack aStack, int x, int y, boolean aHeatRun) {
        // 对于纯冷却剂，通常不需要在此处做特殊处理
        // 但可以用来触发一些视觉效果或更新 NBT 状态
    }

    /**
     * 决定此组件是否接受来自相邻组件（如燃料棒或其他脉冲冷却组件）的热量脉冲。
     * 脉冲通常发生在燃料棒裂变时，会尝试将热量传递给相邻的组件。
     *
     * @param aReactor     反应堆实例
     * @param aStack       当前组件的 ItemStack
     * @param pulsingStack 发出脉冲的组件的 ItemStack
     * @param youX         当前组件在反应堆中的 X 坐标
     * @param youY         当前组件在反应堆中的 Y 坐标
     * @param pulseX       发出脉冲的组件的 X 坐标
     * @param pulseY       发出脉冲的组件的 Y 坐标
     * @param aHeatRun     是否处于热量运行模式
     * @return true 表示接受来自相邻组件的热量脉冲，false 表示拒绝
     */
    @Override
    public boolean acceptUraniumPulse(IReactor aReactor, ItemStack aStack, ItemStack pulsingStack, int youX, int youY,
        int pulseX, int pulseY, boolean aHeatRun) {
        // 无条件接受脉冲（模拟无限吸收）
        // 这使得燃料棒等热源能将其热量传递给这个冷却单元
        return true;
    }

    /**
     * 决定此组件是否可以存储热量。
     * 会影响反应堆是否将其视为可蓄热的组件。
     *
     * @param aReactor 反应堆实例
     * @param aStack   当前组件的 ItemStack
     * @param x        组件在反应堆中的 X 坐标
     * @param y        组件在反应堆中的 Y 坐标
     * @return true 表示此组件可以存储热量
     */
    @Override
    public boolean canStoreHeat(IReactor aReactor, ItemStack aStack, int x, int y) {
        // 声明它可以存储热量 (这是冷却单元的基本属性)
        return true;
    }

    /**
     * 获取此组件的最大储热量。
     * 决定了组件能承受的最大热量而不损坏。
     *
     * @param aReactor 反应堆实例
     * @param aStack   当前组件的 ItemStack
     * @param x        组件在反应堆中的 X 坐标
     * @param y        组件在反应堆中的 Y 坐标
     * @return 最大储热量 (单位: HU - Heat Units)
     */
    @Override
    public int getMaxHeat(IReactor aReactor, ItemStack aStack, int x, int y) {
        // 返回极大值模拟无限容量 (Integer.MAX_VALUE 是一个非常大的整数)
        // 这使得冷却单元不会因为热量过高而损坏
        return Integer.MAX_VALUE;
    }

    /**
     * 获取此组件当前存储的热量。
     * 反应堆会根据此值判断组件的状态。
     *
     * @param aReactor 反应堆实例
     * @param aStack   当前组件的 ItemStack
     * @param x        组件在反应堆中的 X 坐标
     * @param y        组件在反应堆中的 Y 坐标
     * @return 当前储热量 (单位: HU)
     */
    @Override
    public int getCurrentHeat(IReactor aReactor, ItemStack aStack, int x, int y) {
        // 假设热量被瞬时吸收，当前热量为 0
        // 这与 alterHeat 的逻辑相配合，alterHeat 负责实际的冷却效果
        // 而 getCurrentHeat 只是向反应堆报告当前状态
        return 0;
    }

    /**
     * 影响反应堆发生蒸汽爆炸时的威力倍率。
     * 通常用于调整组件对爆炸风险的贡献。
     *
     * @param aReactor 反应堆实例
     * @param aStack   当前组件的 ItemStack
     * @return 爆炸威力倍率 (1.0F 为标准倍率)
     */
    @Override
    public float influenceExplosion(IReactor aReactor, ItemStack aStack) {
        // 返回 1.0F 表示不影响爆炸威力，或者根据需要调整
        // 对于一个理想化的冷却剂，通常不希望它增加爆炸风险
        return 1.0F;
    }

    /**
     * 修改传递给反应堆的净热量。
     * 这是实现核心冷却效果的关键方法。
     * 该方法的返回值会直接加到反应堆的全局热量计数器上。
     * 返回负值表示从反应堆中移除热量（冷却），正值表示增加热量（产热）。
     *
     * @param aReactor 反应堆实例
     * @param aStack   当前组件的 ItemStack
     * @param x        组件在反应堆中的 X 坐标
     * @param y        组件在反应堆中的 Y 坐标
     * @param aHeat    由 IC2 计算出的、该组件位置产生的净热量贡献 (可能来自燃料棒、脉冲等)
     * @return 返回一个整数值，该值将被添加到反应堆的总热量上 (单位: HU)
     */
    @Override
    public int alterHeat(IReactor aReactor, ItemStack aStack, int x, int y, int aHeat) {
        // --- 核心逻辑解释 ---
        // aHeat: IC2 认为这个组件位置在本次 Tick 产生的净热量。
        // 如果 aHeat > 0，表示此位置（或其周围）产生了 aHeat 的热量。
        // 如果 aHeat < 0，表示此位置（或其周围）吸收了 |aHeat| 的热量。
        // 如果 aHeat == 0，表示此位置没有净热量变化。

        // 我们的目标是让这个组件表现得像一个“无限”冷却器：
        // 1. 它能够吸收传入的 aHeat。
        // 2. 它还能从反应堆的全局热量中额外吸收热量。

        // 定义一个常量，表示每 Tick 额外从反应堆中吸收的热量
        int extraAbsorption = 10000; // 额外吸收的热量，可根据需要调整大小

        // 计算总共需要从反应堆中移除的热量
        // 这包括了需要抵消的传入热量 (aHeat) 和额外的冷却量 (extraAbsorption)
        int totalAbsorption = aHeat + extraAbsorption;

        // 为了让反应堆冷却，我们需要从其总热量中减去 totalAbsorption
        // 在 alterHeat 中，这意味着要返回一个负值，其绝对值等于 totalAbsorption
        // 这样，反应堆的总热量 = 原总热量 + ( - totalAbsorption ) = 原总热量 - totalAbsorption
        // 这就实现了直接降低反应堆“堆温”的效果。
        return -totalAbsorption; // 返回负值表示从反应堆中移除 (absorb) 热量
    }
}
