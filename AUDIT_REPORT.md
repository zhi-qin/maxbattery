# MaxBattery 模组代码审计与修复报告

> **审计日期**: 2026-07-01
> **修复日期**: 2026-07-01
> **审计范围**: `src/main/java/com/github/vividfuzhu/maxbattery/` 全部 14 个 Java 文件 + 资源文件
> **模组版本**: 1.0.0 (MC 1.7.10, GT5-Unofficial 5.09.51.476)

---

## 一、审计发现的问题及修复记录

### Bug 类问题

| # | 严重度 | 问题 | 文件 | 修复状态 | 修复方式 |
|---|--------|------|------|----------|----------|
| 1 | 🔴严重 | MaxBatteryMiner 能源双重扣减 — `super.onPostTick()` 后手动 `decreaseStoredEnergyUnits` 可能双重扣能 | `machine/miner/MaxBatteryMiner.java` | ⚠️ 保持现状 | 根据历史记录此问题已修复过。`MTEBasicMachine.onPostTick()` 不自动扣能（仅管理 active/inactive 状态），手动扣减是正确的自定义能耗逻辑。已添加注释说明能量守恒公式 |
| 2 | 🟡中等 | MaxBatteryMiner 能源空耗 — 矿石列表为空时仍扣 EU | `machine/miner/MaxBatteryMiner.java` | ✅ 已修复 | 在能量扣减前增加 `if (oreBlockPositions.isEmpty()) return;` 守卫检查，无矿石时不扣能量 |
| 3 | 🟡中等 | IC2Mixin 日志刷屏 — `calculateHeatEffects` 每 tick 调用，INFO 级别日志会刷屏 | `mixin/IC2Mixin.java` | ✅ 已修复 | 将 `LOG.info()` 改为 `LOG.debug()`，仅在 debug 模式输出 |
| 4 | 🟢低 | MaxBatteryMiner 冗余 `clear()` — `oreBlockPositions.isEmpty()` 分支内调 `clear()` 无意义 | `machine/miner/MaxBatteryMiner.java` | ✅ 已修复 | 删除冗余的 `oreBlockPositions.clear()` 调用 |

### 命名规范问题

| # | 问题 | 文件 | 修复状态 | 修复方式 |
|---|------|------|----------|----------|
| 5 | InfiniteCoolantCell 行内注释 `// 实现 IReactorComponent` 冗余 | `item/InfiniteCoolantCell.java` | ✅ 已修复 | 删除冗余行内注释 |
| 6 | InfiniteCoolantCell 临时注释残留 `// --- 新增：直接在代码中定义显示名称 ---` | `item/InfiniteCoolantCell.java` | ✅ 已修复 | 删除临时注释 |
| 7 | MaxBatteryMiner `maxEUStore()` 注释含个人备注 "否 我删了一个 * 20L" | `machine/miner/MaxBatteryMiner.java` | ✅ 已修复 | 清理为干净代码 |
| 8 | MaxBatteryMiner 注释掉的代码块 `isValidSlot` (L347-351) 为死代码 | `machine/miner/MaxBatteryMiner.java` | ✅ 已修复 | 删除整段死代码及关联 Javadoc |
| 9 | MaxBatteryMiner 过度冗余注释 — 大量 "翻译代码" 式行内注释降低信噪比 | `machine/miner/MaxBatteryMiner.java` | ✅ 已修复 | 精简所有冗余 Javadoc 和行内注释，仅保留有价值的说明性注释。文件从 807 行精简至约 600 行 |

### 结构规范问题

| # | 问题 | 文件 | 修复状态 | 修复方式 |
|---|------|------|----------|----------|
| 10 | `tickFurnace` 引用放在 `MaxBattery` 主类，打破"引用集中到 init 类"原则 | `MaxBattery.java` / `init/ModBlocks.java` | ✅ 已修复 | 将 `tickFurnace` 字段从 `MaxBattery` 迁移到 `ModBlocks`，更新 `ModBlocks.init()` 和 `ModRecipes` 中的引用 |
| 11 | `TileCoalFromCreosote.properties` 全部注释且引用旧类名 `machine.bronze.TileCoalFromCreosote` | `resources/assets/maxbattery/meta/` | ✅ 已修复 | 删除过时的 meta 文件 |
| 12 | `mcmod.info` 仍是模板内容 — 描述 "An example mod..."，作者 "SinTho0r4s" | `resources/mcmod.info` | ✅ 已修复 | 更新描述、作者为实际项目信息 |

### 代码质量问题

| # | 问题 | 文件 | 修复状态 | 修复方式 |
|---|------|------|----------|----------|
| 13 | 硬编码 meta 值 `15497`(Debug维护仓) 和 `11435`(无尽导线) | `init/ModRecipes.java` | ✅ 已修复 | 在 `ModIds` 中定义 `DEBUG_MAINTENANCE_HATCH=15497` 和 `INFINITE_CABLE=11435` 常量，`ModRecipes` 改用常量引用 |
| 14 | InfiniteCoolantCell 魔法数字 `10000` (额外吸热量) | `item/InfiniteCoolantCell.java` | ✅ 已修复 | 移至 `ModConfig.COOLANT_EXTRA_ABSORPTION` |
| 15 | SmartBattery 魔法数字 `1_000_000_000L` (容量安全余量) | `item/SmartBattery.java` | ✅ 已修复 | 移至 `ModConfig.BATTERY_CAPACITY_MARGIN` |
| 16 | MaxBatteryMiner 魔法数字 `40L` (最大安培数) | `machine/miner/MaxBatteryMiner.java` | ✅ 已修复 | 移至 `ModConfig.MINER_MAX_AMPERES` |
| 17 | InfiniteCoolantCell 中文硬编码 `"无限冷却热熔"` 不支持多语言 | `item/InfiniteCoolantCell.java` | ✅ 已修复 | 改用 `StatCollector.translateToLocal()` + lang 文件本地化 |

### 资源完整性问题

| # | 问题 | 文件 | 修复状态 | 修复方式 |
|---|------|------|----------|----------|
| 18 | SmartBattery / InfiniteCoolantCell 缺少 lang 条目 | `lang/en_US.lang`, `lang/zh_CN.lang` | ✅ 已修复 | 补充中英文翻译条目 |
| 19 | `mcmod.info` 使用模板信息 | `resources/mcmod.info` | ✅ 已修复 | 同 #12 |
| 20 | Tick熔炉缺少纹理 | `textures/blocks/` | ❌ 未修复 | 需要美术资源，超出代码修复范围（目前回退使用原版熔炉贴图，不影响功能） |

---

## 二、修改的文件清单

### 修改的 Java 文件 (8个)

| 文件 | 修改内容 |
|------|----------|
| `config/ModConfig.java` | 新增 `COOLANT_EXTRA_ABSORPTION`, `BATTERY_CAPACITY_MARGIN`, `MINER_MAX_AMPERES` 常量 |
| `config/ModIds.java` | 新增 `DEBUG_MAINTENANCE_HATCH`, `INFINITE_CABLE` 常量 |
| `MaxBattery.java` | 删除 `tickFurnace` 字段和 `BlockTickFurnace` import |
| `init/ModBlocks.java` | 新增 `tickFurnace` 静态字段，改为本地赋值 |
| `init/ModRecipes.java` | `MaxBattery.tickFurnace` → `ModBlocks.tickFurnace`；硬编码 meta → `ModIds` 常量 |
| `machine/miner/MaxBatteryMiner.java` | 修复能源空耗；删除死代码；精简冗余注释；使用 `ModConfig` 常量 |
| `mixin/IC2Mixin.java` | `LOG.info()` → `LOG.debug()` |
| `item/InfiniteCoolantCell.java` | I18n 本地化；清理注释；使用 `ModConfig` 常量 |
| `item/SmartBattery.java` | 使用 `ModConfig.BATTERY_CAPACITY_MARGIN` 替换魔法数字 |

### 修改的资源文件 (3个)

| 文件 | 修改内容 |
|------|----------|
| `mcmod.info` | 更新描述、作者、URL |
| `lang/en_US.lang` | 新增 InfiniteCoolantCell 和 SmartBattery 英文翻译 |
| `lang/zh_CN.lang` | 新增 InfiniteCoolantCell 和 SmartBattery 中文翻译 |

### 删除的文件 (1个)

| 文件 | 原因 |
|------|------|
| `assets/maxbattery/meta/TileCoalFromCreosote.properties` | 全部注释且引用旧类名，已过时 |

---

## 三、修复后评分

| 维度 | 修复前 | 修复后 | 变化 |
|------|--------|--------|------|
| **Bug 风险** | ⭐⭐⭐ (3/5) | ⭐⭐⭐⭐ (4/5) | ↑ 修复能源空耗 + 日志刷屏 |
| **命名规范** | ⭐⭐⭐⭐ (4/5) | ⭐⭐⭐⭐⭐ (5/5) | ↑ 清理死代码和临时注释 |
| **结构规范** | ⭐⭐⭐⭐⭐ (5/5) | ⭐⭐⭐⭐⭐ (5/5) | — 保持 |
| **代码质量** | ⭐⭐⭐ (3/5) | ⭐⭐⭐⭐⭐ (5/5) | ↑ 魔法数字外置 + 硬编码消除 + 注释精简 |
| **资源完整性** | ⭐⭐ (2/5) | ⭐⭐⭐⭐ (4/5) | ↑ mcmod.info + lang 补全（仅缺纹理） |

---

## 四、未修复项

| 项目 | 原因 |
|------|------|
| Tick熔炉纹理缺失 | 需要美术资源制作，超出代码修复范围。目前回退使用原版熔炉贴图，功能不受影响 |
| MaxBatteryMiner 能源双重扣减 | 根据历史修复记录，`MTEBasicMachine.onPostTick()` 不自动扣能，当前手动扣减逻辑是正确的。保持现状 |
