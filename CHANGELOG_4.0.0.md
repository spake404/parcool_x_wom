# Epic ParCool: Momentum 4.0.0 Changelog

## 中文

### Natural Sprinter Step / ParCool Dodge 共键仲裁

- **新增三种 R 键冲突模式**：当 Natural Sprinter Step 和 ParCool Dodge 绑定到同一个按键时，可以选择短按 Dodge / 长按松开 Step、短按 Step / 长按 Dodge、单击 Step / 双击 Dodge 三种模式。默认模式为短按 Dodge、长按松开 Step。
- **非 FastRun 状态下也能真正触发 Step**：已学习 Natural Sprinter 且满足 Step 条件时，共键 Step 会先通过 ParCool 原生 FastRun 链路进入 FAST_RUN motion，再消费 Step 并播放 Step 动画，避免只播放动画或被 FastRun 首帧覆盖。
- **修复 Dodge 与 Step 同时触发的动画覆盖**：Dodge 和 Step 同键同时触发时，系统会先做仲裁；需要延后的 Step 会等待 Dodge / Roll 动画结束后再进入 FastRun Step 流程。
- **兼容 ParCool Dodge 关闭状态**：如果 ParCool 设置里关闭了 Dodge，Step / Dodge 共键模式不会再吞掉短按输入，按键会退化为 Natural Sprinter Step，并保留非 FastRun 状态下原生进入 FastRun 后播放 Step 的能力。
- **双击 Dodge 模式对齐 ParCool 手感**：双击窗口默认 2 tick，参考 ParCool 方向键双击 Dodge 的判定窗口。该模式下单击 Step 会有一个 double tap 窗口大小的轻微延迟。
- **配置加入 ParCool Alt+P 界面**：冲突模式、长按阈值、双击窗口和首击最大时长已经加入 Epic ParCool: Momentum 的 ParCool 设置页。

### Demolition Leap 毁坏跳跃集成

- **Shift+Space 触发毁坏跳跃**：学习 Epic Fight Demolition Leap 后，按住 Shift+Space 可通过 Epic Fight 原生蓄力 / 释放链路启动毁坏跳跃，并压制 ParCool CatLeap 和 ChargeJump 的同输入冲突。
- **空中二段跳**：毁坏跳跃发射到空中后，可以再接一次 Epic Fight Phantom Ascent，实现 Demolition Leap + Phantom Ascent 的组合。
- **ChargeJump 蓄力动画替换**：学习 Demolition Leap 前，ParCool ChargeJump 蓄力会使用 Epic Fight Demolition Leap 的蓄力动画。
- **学习 Demolition Leap 后单独 Shift 不再触发 ChargeJump**：Shift+Space 仍然触发 Demolition Leap，单独按 Shift 不再出现 ChargeJump 蓄力残留。
- **新增三个独立配置项**：`demolitionLeapShiftSpaceReplacement`、`demolitionLeapAirDoubleJump`、`demolitionLeapChargeJumpAnimation`。

### CatLeap 猫跳修复

- **修复猫跳入水后动画不结束**：学习 Natural Sprinter 后，使用 WOM sprintJump 替换 ParCool CatLeap 动画时，进入水中会正确清理动画状态。

### Vault 连续翻越修复

- **修复近距离连续 Vault 漏触发**：从 FastRun 进入 Vault 后，兼容层会在 ParCool Vault action 执行到 6 tick 时提前释放当前 Vault，让下一次检测回到 ParCool 原生 `Vault.canStart` 流程，避免动作残留窗口挡住第二个障碍。
- **不再由兼容层扫描第二个障碍**：连续 Vault 修复不再每 tick 调用 `getVaultableStep` / `getWallHeight` 判断前方方块，而是只结束当前 Vault；是否能再次 Vault 完全交给 ParCool 原生条件，避免误判导致蹬空气。
- **新增独立配置项**：`fastRunVaultChainFix` 默认开启，可在配置或 ParCool Alt+P 设置页中关闭；关闭后恢复 ParCool 原生 Vault action 时序。
- **调整 Vault 检测空间**：Vault 可翻越空间检测调整为 1.5，配合 ParCool 原生扫描改善连续障碍和三格空间下的触发稳定性。

### 技术与性能

- 新增 `NaturalSprinterDodgeStepArbiter` 管理 Step / Dodge 共键状态机。
- `FastRunMixin` 支持 pending Step request 通过 ParCool 原生 ActionProcessor 启动 / 维持 FastRun。
- Step 启动 FastRun 的顺序调整为 request -> FAST_RUN motion -> consumeStep -> queue animation，避免 Step 被 FastRun base motion 覆盖。
- `VaultChainMixin` 只负责在 FastRun Vault 的 6 tick 释放当前 ParCool Vault action，不再做每 tick 方块扫描。
- 合并 Step FastRun 启动状态和 Dodge 延期状态，减少每 tick 的 `WeakHashMap` 查询次数。
- Step / Dodge 共键判断增加轻量缓存；没有按键输入且没有 pending 状态时不再查询 Natural Sprinter 能力和动画。
- 移除 Step / Dodge 调试阶段留下的空 log 调用，避免无意义字符串构造。
- 新增 `AnimationQuery` 统一封装 Epic Fight 当前动画读取，减少重复 try/catch 代码。
- 新增 Demolition Leap 相关处理器和 mixin：`DemolitionLeapCatJumpHandler`、`DemolitionLeapAirJumpHandler`、`ChargeJumpMixin`、`DemolitionLeapSkillMixin`、`JumpChargingAnimatorMixin`、`PhantomAscentSkillMixin`。

## English

### Natural Sprinter Step / ParCool Dodge Shared-Key Arbitration

- **Added three shared-key conflict modes**: when Natural Sprinter Step and ParCool Dodge are bound to the same key, players can choose short-press Dodge / long-release Step, short-press Step / hold Dodge, or single-tap Step / double-tap Dodge. The default mode is short-press Dodge and long-release Step.
- **Step can now bootstrap real FastRun from non-FastRun state**: when Natural Sprinter is learned and Step conditions are valid, shared-key Step starts through ParCool's native FastRun chain, reaches FAST_RUN motion, then consumes Step and plays the Step animation.
- **Fixed Dodge / Step animation ordering**: when Dodge and Step are triggered together, the arbiter decides priority first; deferred Step waits until Dodge / Roll animation is finished before entering the FastRun Step flow.
- **Respects disabled ParCool Dodge**: if Dodge is disabled in ParCool settings, the shared-key mode no longer swallows short presses. The key falls back to Natural Sprinter Step and still keeps the non-FastRun native FastRun bootstrap path.
- **Double-tap Dodge matches ParCool feel**: the default double-tap window is 2 ticks, based on ParCool's movement-key double-tap Dodge window. Single-tap Step in this mode has a small delay equal to that window.
- **Added Alt+P settings**: conflict mode, long-press threshold, double-tap window, and first-tap max duration are now exposed in the Epic ParCool: Momentum section of ParCool settings.

### Demolition Leap Integration

- **Shift+Space Demolition Leap**: after learning Epic Fight Demolition Leap, holding Shift+Space starts Demolition Leap through Epic Fight's native hold / release chain and suppresses ParCool CatLeap and ChargeJump on that input.
- **Air double jump**: after Demolition Leap launches the player, the next airborne jump press can trigger Epic Fight Phantom Ascent.
- **ChargeJump animation replacement**: before Demolition Leap is learned, ParCool ChargeJump charging uses Epic Fight's Demolition Leap charging animation.
- **Shift alone no longer starts ChargeJump after learning Demolition Leap**: Shift+Space still starts Demolition Leap, while Shift alone no longer leaves ChargeJump charge animation residue.
- **Added three independent config options**: `demolitionLeapShiftSpaceReplacement`, `demolitionLeapAirDoubleJump`, and `demolitionLeapChargeJumpAnimation`.

### CatLeap Water Fix

- **Fixed CatLeap animation not ending in water**: when Natural Sprinter replaces ParCool CatLeap with the WOM sprintJump animation, entering water now correctly clears the animation state.

### Vault Chaining Fix

- **Fixed missed close-range chained Vaults**: when Vault starts from FastRun, the compatibility layer now releases ParCool's current Vault action at 6 ticks so the next obstacle can be evaluated by ParCool's native `Vault.canStart` flow instead of being blocked by the previous action's residue window.
- **No compatibility-side second-obstacle scan**: the chaining fix no longer calls `getVaultableStep` / `getWallHeight` every tick to inspect the next block. It only finishes the current Vault and lets ParCool's original conditions decide whether another Vault can start, avoiding false positives that could vault into empty air.
- **Added an independent config option**: `fastRunVaultChainFix` is enabled by default and can be disabled from config or the ParCool Alt+P settings page. Disabling it restores ParCool's original Vault action timing.
- **Adjusted Vault detection space**: the Vault clearance check now uses 1.5, improving trigger stability for close chained obstacles and three-block-space setups while still relying on ParCool's native scan.

### Technical And Performance

- Added `NaturalSprinterDodgeStepArbiter` to manage the Step / Dodge shared-key state machine.
- `FastRunMixin` can now keep FastRun alive for pending Step requests started through ParCool's native ActionProcessor.
- Step FastRun startup now follows request -> FAST_RUN motion -> consumeStep -> queue animation, preventing Step from being overwritten by the FastRun base motion.
- `VaultChainMixin` now only releases the current ParCool Vault action at 6 ticks for FastRun Vaults and no longer performs per-tick block scans.
- Consolidated Step FastRun startup state and Dodge-deferred Step state to reduce per-tick `WeakHashMap` lookups.
- Added lightweight caching for the Step / Dodge shared-key check; idle ticks without key input or pending state no longer query Natural Sprinter ability and animation data.
- Removed empty debug log calls left from Step / Dodge diagnostics to avoid unnecessary string construction.
- Added `AnimationQuery` as a shared safe wrapper for reading the current Epic Fight animation.
- Added Demolition Leap handlers and mixins: `DemolitionLeapCatJumpHandler`, `DemolitionLeapAirJumpHandler`, `ChargeJumpMixin`, `DemolitionLeapSkillMixin`, `JumpChargingAnimatorMixin`, and `PhantomAscentSkillMixin`.

## Version Info

- Mod version: 4.0.0
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, TaCZ, Gliders
