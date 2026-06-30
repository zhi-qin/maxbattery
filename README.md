# MaxBattery

一个基于 **GTNH 2.8.0** 的 GregTech 增强模组，随缘更新各种科技与狠活，天顶星出品。

---

## 📦 已实现功能

### 🔋 智能电池 (SmartBattery)
- 容量接近 `Long.MAX_VALUE`（约 9.22 × 10¹⁸ EU），相当于无限电
- **右键点击 GT 机器**自动适配电压等级（ULV ~ OPV）
- 传输速率 = 当前电压 × 2
- HUD 显示当前电压等级、容量、电量百分比
- 组装机/工作台均可合成

### ❄️ 无限冷却单元 (InfiniteCoolantCell)
- 实现 IC2 `IReactorComponent` 接口
- 最大储热量为 `Integer.MAX_VALUE`，永不熔毁
- 每 tick 额外吸收 10,000 HU 热量
- 无条件接受相邻组件的热量脉冲
- 2×2 原石即可合成

### ☢️ IC2 反应堆防爆 (Mixin)
- Mixin 注入 `TileEntityNuclearReactorElectric.calculateHeatEffects`
- 当热量 ≥ 99% 最大热量时，强制锁定在 99% 并阻止爆炸
- 安全网机制，防止反应堆过热损坏

### 🏭 杂酚油炼煤机 (MTECreosoteCoalConverter)
- 青铜外观，无需电力
- 消耗 1000mB 杂酚油 → 产出 1 个煤炭
- 正面为物品输出口，底面/顶面为流体输入口
- 遵循 GT 电力机器标准的输入输出面布局
- 8 个熔炉围框合成

### ⛏️ 超速采矿机 (MaxBatteryMiner)
- 支持 LV / MV / HV / EV / IV 五个电压等级
- **内置 20 倍加速**，无需外部加速设备
- 分帧扫描机制，每 tick 扫描 2 层，避免服务器卡顿
- 使用螺丝刀调节工作半径（潜行=缩小，普通=增大）
- 双输出槽设计提高吞吐量
- 继承 GT 原版采矿机的矿石掉落逻辑
- 原版 GT 采矿机 + 红石 无序合成

### 🔥 Tick熔炉 (TickFurnace)
- 外观与原版熔炉完全一致（继承 `BlockFurnace`）
- **100 倍加速**炼制速度（燃料消耗 100/tick，炼制进度 +100/tick）
- 燃料效率与原版完全一致（burnTime/totalCookTime 守恒）
- 1 块煤炭可炼制 8 个物品（与原版相同）
- 合成：原版熔炉 + 木棍（无序合成）
- 自动管理燃烧动画元数据

### 🔧 调试辅助
- **无尽导线**：3 原石合成
- **Debug 维护仓**：1 维护仓无序合成

---

## 📁 项目结构

```
src/main/java/com/github/vividfuzhu/maxbattery/
├── MaxBattery.java                         ← @Mod 主入口
├── config/
│   ├── ModIds.java                         ← ID 常量中心（机器 ID + 调试物品 meta）
│   └── ModConfig.java                      ← 可调参数中心（采矿机/熔炉/冷却/电池）
├── init/
│   ├── ModItems.java                       ← 物品注册中心
│   ├── ModBlocks.java                      ← 方块注册中心
│   ├── ModMachines.java                    ← GT 机器注册
│   └── ModRecipes.java                     ← 配方注册
├── item/
│   ├── SmartBattery.java                   ← 智能电池
│   └── InfiniteCoolantCell.java            ← 无限冷却单元
├── block/
│   └── furnace/
│       ├── BlockTickFurnace.java           ← Tick熔炉方块
│       └── TileTickFurnace.java            ← Tick熔炉 TileEntity
├── machine/
│   ├── creosote/
│   │   └── MTECreosoteCoalConverter.java   ← 杂酚油炼煤机
│   └── miner/
│       └── MaxBatteryMiner.java            ← 超速采矿机
└── mixin/
    └── IC2Mixin.java                       ← IC2 反应堆防爆 Mixin

src/main/resources/
├── mixins.maxbattery.json                  ← Mixin 配置文件
├── mcmod.info                              ← 模组元信息
└── assets/maxbattery/
    ├── lang/
    │   ├── zh_CN.lang                      ← 中文语言文件
    │   └── en_US.lang                      ← 英文语言文件
    └── textures/items/
        ├── infinite_coolant_cell.png       ← 无限冷却单元贴图
        └── maxbattery_smart.png            ← 智能电池贴图
```

---

## 🔧 构建与开发

| 项目属性 | 值 |
|---|---|
| Minecraft 版本 | 1.7.10 |
| Forge 版本 | 10.13.4.1614 |
| MCP 映射 | stable_12 |
| 核心依赖 | GT5-Unofficial 5.09.51.476 |
| Mixin | ✅ 启用 |
| Modern Java | ✅ Jabel (语法兼容至 Java 17) |
| Spotless | ✅ GTNH 代码格式化 |

```bash
# 运行客户端
./gradlew runClient

# 构建
./gradlew build
```

---

## 📝 开发规范

- **日志输出**：必须使用 Log4j Logger（`MaxBattery.LOG`），禁止 `System.out`/`System.err`
- **物品注册**：`MetaBaseItem` 子类构造时已自动注册，**禁止重复调用** `GameRegistry.registerItem`
- **机器输入输出面**：遵循 GT 电力机器标准（正面=物品输出，非正面=输入）
- **非电力机器**：使用伪装电力机器模式（`isElectric()=true` 但 `maxEUInput()=0`）以复用父类 IO 逻辑
- **配置外置化**：所有可调参数集中在 `ModConfig`，ID 常量集中在 `ModIds`，新增机器只需扩展这两个文件

---

## 📄 许可

见 [LICENSE](LICENSE) 文件。
