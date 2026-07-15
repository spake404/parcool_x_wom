# Epic ParCool: Momentum 4.0.0 Changelog

## 中文

### Forge 兼容性

- **修复 Forge 事件优先级兼容性**：将客户端调试事件处理器从不兼容的 `EventPriority.MONITOR` 调整为 Forge 1.20.1 支持的 `EventPriority.LOWEST`，避免在 Forge 47.4.10 等环境中因缺少 `MONITOR` 枚举导致模组加载崩溃。该调整不改变 ParCool、Epic Fight 或其他核心功能，仅改变相关调试日志和调试 HUD 快捷键的执行时机。

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

### 高速移动动画播放曲线

- **扩展 Epic Fight 玩家移动动画的高速播放区间**：普通行走和原版疾跑速度范围继续使用 Epic Fight 原有 `MovementAnimation` 播放逻辑；超过原版疾跑速度后，动画播放倍率会继续随玩家实际水平移动速度提高，不再在普通疾跑时提前触顶。
- **原版疾跑 160% 速度时达到新上限**：默认以原版平地疾跑速度作为 100% 基准，在 160% 速度时达到 `1.856x` 最大播放倍率。默认曲线约为：100% = `1.16x`、120% = `1.392x`、140% = `1.624x`、160% = `1.856x`。
- **使用实际水平位移并平滑变化**：播放速度根据玩家每 tick 的 X/Z 实际位移计算，并使用平滑采样减少加速、减速和网络位置波动造成的动画跳速；跳跃产生的 Y 轴速度不会错误加快跑步动画。
- **普通跑步与 FastRun 共用曲线**：Epic Fight 武器 RUN、WOM `BIPED_SPRINT` / `BIPED_SPRINT_BAREHAND`，以及复用武器 RUN 的自动 FastRun，只要实际动画类型为 `MovementAnimation`，都会使用同一条高速动态曲线。
- **保持非移动动画兼容**：LinkAnimation、普通 `StaticAnimation`、攻击、Step、跳跃和滑铲动画不受该曲线直接影响；动画自身的 `PLAY_SPEED_MODIFIER` 仍会在基础播放倍率之后正常应用。
- **新增两个可调配置项**：`movementAnimationSpeedCapMultiplier` 默认 `1.6`，控制达到动画上限所需的相对疾跑速度；`movementAnimationMaxPlaySpeed` 默认 `1.856`，控制玩家 `MovementAnimation` 的最大播放倍率。两个选项均已加入 ParCool Alt+P 设置页。

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

### Forge Compatibility

- **Fixed Forge event-priority compatibility**: changed the client debug event handlers from the incompatible `EventPriority.MONITOR` to Forge 1.20.1's supported `EventPriority.LOWEST`, preventing mod-loading crashes on environments such as Forge 47.4.10 where the `MONITOR` enum is unavailable. This does not change ParCool, Epic Fight, or other core gameplay behavior; it only changes the execution timing of related debug logs and the debug HUD shortcut.

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

### High-Speed Movement Animation Curve

- **Extended Epic Fight player movement animation playback above vanilla sprint speed**: normal walking and vanilla sprint ranges continue to use Epic Fight's original `MovementAnimation` behavior. Above vanilla sprint speed, playback now keeps scaling with the player's actual horizontal movement instead of reaching its ceiling during ordinary sprinting.
- **New maximum is reached at 160% of vanilla sprint speed**: vanilla flat-ground sprint speed is used as the 100% baseline by default, with a `1.856x` playback maximum at 160%. The default curve is approximately: 100% = `1.16x`, 120% = `1.392x`, 140% = `1.624x`, and 160% = `1.856x`.
- **Uses smoothed actual horizontal displacement**: playback speed is calculated from the player's real per-tick X/Z displacement and smoothed to reduce abrupt changes caused by acceleration, deceleration, or network position corrections. Vertical jump velocity does not incorrectly accelerate running animations.
- **Shared by ordinary RUN and FastRun animations**: Epic Fight weapon RUN animations, WOM `BIPED_SPRINT` / `BIPED_SPRINT_BAREHAND`, and automatically generated FastRun profiles that reuse weapon RUN all use the same extended curve when the resolved animation is a `MovementAnimation`.
- **Preserves non-movement animation behavior**: LinkAnimation, ordinary `StaticAnimation`, attacks, Steps, jumps, and slides are not directly modified by this curve. Animation-specific `PLAY_SPEED_MODIFIER` properties are still applied after the base playback speed.
- **Added two configurable options**: `movementAnimationSpeedCapMultiplier` defaults to `1.6` and controls the relative sprint speed required to reach the animation ceiling. `movementAnimationMaxPlaySpeed` defaults to `1.856` and controls the maximum player `MovementAnimation` playback multiplier. Both options are available from the ParCool Alt+P settings page.

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

### Sandevistan / 斯安威斯坦

#### 中文

- 新增斯安威斯坦技能，使用独立的跑酷技能槽和额外热键触发，默认持续时间为 10 秒。
- 新增范围实体局部 Tick 时钟：使用者保持正常速度，范围内其他实体按配置倍率降低实体 Tick 频率，默认约为 5 TPS。
- 同步客户端局部 partialTick，平滑处理实体位置、旋转以及 Epic Fight 动画的减速显示。
- 新增 Epic Fight 风格白色残影，支持残影数量、持续时间、生成间隔、最小移动距离、透明度和颜色渐变配置。
- 新增仅本地玩家可见的绿色滤镜、边缘模糊、启动边缘畸变、色差分离和白色闪光效果，并加入 Alt+P 配置项及中英文悬停说明。
- 新增斯安威斯坦启动音效 sandevistan_start，在本地玩家成功开启技能时播放。
- 新增可选的斯安威斯坦性能诊断日志，用于区分残影渲染、后处理 Shader 和绿色滤镜造成的卡顿。

#### English

- Added the Sandevistan skill with a dedicated Parkour Skill slot and an extra hotkey; the default duration is 10 seconds.
- Added a local entity Tick clock: the user keeps normal speed while entities inside the configured radius run at a reduced Tick frequency, approximately 5 TPS by default.
- Added client-side local partialTick interpolation for smoother slowed entity positions, rotations, and Epic Fight animation rendering.
- Added Epic Fight-style white afterimages with configurable count, lifetime, spawn interval, minimum movement distance, alpha, and color gradient.
- Added a local-player-only green filter, edge blur, activation edge warp, chromatic aberration, and white activation flash, with Alt+P settings and bilingual tooltips.
- Added the Sandevistan activation sound sandevistan_start, played when the local player successfully activates the skill.
- Added optional Sandevistan performance diagnostics to distinguish hitches caused by afterimage rendering, post-processing Shader work, or the green filter.

## Version Info

- Mod version: 4.0.0
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, TaCZ, Gliders
