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
- 移除发动攻击时自动结束斯安威斯坦的限制；玩家现在可以在技能持续期间正常进行普攻、连续攻击和攻击型技能。
- 新增可选的斯安威斯坦性能诊断日志，用于区分残影渲染、后处理 Shader 和绿色滤镜造成的卡顿。

#### English

- Added the Sandevistan skill with a dedicated Parkour Skill slot and an extra hotkey; the default duration is 10 seconds.
- Added a local entity Tick clock: the user keeps normal speed while entities inside the configured radius run at a reduced Tick frequency, approximately 5 TPS by default.
- Added client-side local partialTick interpolation for smoother slowed entity positions, rotations, and Epic Fight animation rendering.
- Added Epic Fight-style white afterimages with configurable count, lifetime, spawn interval, minimum movement distance, alpha, and color gradient.
- Added a local-player-only green filter, edge blur, activation edge warp, chromatic aberration, and white activation flash, with Alt+P settings and bilingual tooltips.
- Added the Sandevistan activation sound sandevistan_start, played when the local player successfully activates the skill.
- Removed the restriction that automatically ended Sandevistan when attacking; players can now use basic attacks, attack combos, and offensive skills while it remains active.
- Added optional Sandevistan performance diagnostics to distinguish hitches caused by afterimage rendering, post-processing Shader work, or the green filter.

### Sandevistan Model And Mechanic Expansion / 斯安威斯坦型号与机制扩展

#### 中文

- 将原有斯安威斯坦重制为**迪纳拉斯安威斯坦4型**，并新增**千替“实境扭曲”斯安威斯坦5型**、**军用科技“游隼”斯安威斯坦**、**军用科技“远地点”斯安威斯坦**和**泽塔科技斯安威斯坦**；五种型号均拥有独立技能书、图标入口、持续时间、冷却、时间倍率和战斗属性。
- 新增型号配置档案系统，将时间减速、影响范围、伤害倍率、伤害减免、冷却、击杀奖励和充能规则从通用客户端设置中分离，后续可继续扩展新的斯安威斯坦型号。
- 新增**反应调幅**机制：激活时读取 Epic Fight 最大耐力，每点最大耐力增加 0.1 秒可用持续时间，不消耗耐力，并在技能书界面显示当前最大耐力及实际增加的持续时间。
- 新增军用科技型号的**部分充能激活**：游隼和远地点可在仍有剩余持续时间时再次启动，技能结束后保留未使用时间；只有全部容量耗尽后才进入完整冷却，激活期间暂停恢复，且手动结束不会返还冷却。
- 新增按 `V` 再次触发以提前结束斯安威斯坦；支持返还的型号会按照剩余持续时间比例的 60% 折算冷却返还，军用科技游隼和远地点不使用该返还机制。
- 新增型号专属战斗效果：千替5型提供普通伤害减免以及火焰、凋零伤害的强化减免；泽塔科技根据地面/空中状态动态切换时间倍率，空中攻击获得额外伤害并减少原版摔落伤害。
- 新增击杀奖励：游隼击杀后恢复持续时间和最大生命值百分比；远地点与泽塔科技击杀后恢复持续时间和最大耐力百分比。持续时间恢复不会超过该次激活由基础时间与反应调幅共同确定的上限。
- 重写斯安威斯坦技能书说明与中英文本地化，按功能分段显示时间倍率、范围、伤害、减伤、持续时间、冷却、反应调幅、充能规则及击杀奖励，并修复跑酷技能类型在界面中显示原始 ID 的问题。
- 优化残影渲染路径：新增专用残影渲染器、视锥剔除并移除残影的动态披风渲染，在保留原有残影位置、生成时机与叠加效果的同时降低光影环境下的渲染负担。

#### English

- Reworked the original Sandevistan into the **Dinara Sandevistan Mk.4** and added the **Qiant “Warp Dancer” Sandevistan Mk.5**, **Militech “Falcon” Sandevistan**, **Militech “Apogee” Sandevistan**, and **Zetatech Sandevistan**. All five models have independent skill books, skill entries, durations, cooldowns, time scales, and combat attributes.
- Added a profile-based model system that separates time dilation, radius, damage multipliers, damage reduction, cooldowns, kill rewards, and charge rules from general client configuration, allowing additional Sandevistan models to be added cleanly later.
- Added **Reaction Tuning**: activation reads Epic Fight maximum stamina and grants 0.1 seconds of additional usable duration per maximum stamina point without consuming stamina. The skill-book interface displays the current maximum stamina and resulting duration bonus.
- Added **partial-charge activation** for the Militech models: Falcon and Apogee can reactivate while usable duration remains, preserve unused duration after deactivation, and enter their full cooldown only after the entire capacity is consumed. Recharge pauses while active, and manual deactivation grants no cooldown refund.
- Added a second press of `V` to end Sandevistan early. Eligible models convert 60% of the remaining-duration percentage into cooldown refund, while Militech Falcon and Apogee do not use this refund mechanic.
- Added model-specific combat effects: Qiant Mk.5 reduces ordinary incoming damage and provides stronger protection against fire and wither damage; Zetatech dynamically switches time scale between grounded and airborne states, increases airborne attack damage, and reduces vanilla fall damage.
- Added kill rewards: Falcon restores duration and a percentage of maximum health, while Apogee and Zetatech restore duration and a percentage of maximum stamina. Restored duration cannot exceed the activation limit determined by base duration plus Reaction Tuning.
- Reworked the Sandevistan skill-book descriptions and bilingual localization into readable functional sections covering time scale, radius, damage, defenses, duration, cooldown, Reaction Tuning, charge behavior, and kill rewards. Also fixed the Parkour Skill category displaying its raw ID in the interface.
- Optimized the afterimage rendering path with a dedicated renderer, frustum culling, and removal of dynamic cape rendering, reducing shader-heavy rendering cost while preserving the established afterimage positions, spawn timing, and layered appearance.

### Stationary Action Afterimages / 原地动作残影

#### 中文

- 新增原地动作残影：斯安威斯坦激活期间，即使玩家没有产生水平位移，只要正在播放 Epic Fight 攻击或动作状态，也会捕获当前模型姿势并生成残影；普通站立待机不会生成残影。
- 将原地动作残影与移动拖尾残影完全拆分。移动拖尾继续使用原有的最小距离、生命周期、透明度淡出以及青蓝、紫色、橙色渐变配置，不受原地动作残影调整影响。
- 原地动作残影使用独立的生成间隔、显示延迟、生命周期、起始透明度、结束透明度和固定颜色配置；当前默认值为：每 2 Tick 捕获一次、延迟 2 Tick 显示、显示 15 Tick、起始透明度 0.85、结束透明度 0.45、固定青蓝色 `#33E6FF`。
- 新增固定客户端 Tick 显示队列：模型姿势被捕获后会等待配置的 Tick 数再进入渲染，生命周期从真正显示时才开始计算。显示延迟按客户端 Tick 计数，不受正常帧率波动影响。
- 原地动作残影在生命周期内保持固定颜色，不执行移动拖尾的颜色渐变；透明度使用固定的 2.2 次幂 Ease-In 曲线，前段淡出较慢、后段逐渐加快，并在生命周期末尾准确达到配置的结束透明度。
- 原地动作残影的数值配置已加入 ParCool Alt+P 设置界面，并补充完整的中英文名称与悬停说明；十六进制颜色继续通过 Forge 配置文件修改。

#### English

- Added stationary action afterimages: while Sandevistan is active, the current model pose is captured when an Epic Fight attack or action is playing even without horizontal player movement. Ordinary idle standing does not generate afterimages.
- Fully separated stationary action afterimages from moving trail afterimages. Moving trails retain their existing minimum-distance, lifetime, opacity fade, and cyan-to-purple-to-orange gradient settings and are unaffected by stationary-action adjustments.
- Added independent stationary-action settings for capture interval, display delay, lifetime, starting opacity, ending opacity, and fixed color. Current defaults are one capture every 2 ticks, a 2-tick display delay, a 15-tick visible lifetime, 0.85 starting opacity, 0.45 ending opacity, and fixed cyan-blue `#33E6FF`.
- Added a fixed client-tick display queue: captured model poses wait for the configured number of ticks before entering rendering, and their visible lifetime begins only after they are displayed. The delay is counted in client ticks and is independent of ordinary frame-rate fluctuations.
- Stationary action afterimages keep a constant color and do not use the moving trail color gradient. Their opacity uses a fixed power-2.2 Ease-In curve, fading slowly at first and accelerating later until it reaches the configured ending opacity at the end of the lifetime.
- Added the stationary-action numeric settings to the ParCool Alt+P configuration screen with complete Chinese and English names and tooltips. The hexadecimal color remains editable through the Forge configuration file.

### 斯安威斯坦 HUD、环境时缓与兼容修复 / Sandevistan HUD, Environmental Time Dilation, and Compatibility Fixes

#### 中文

- 新增独立的斯安威斯坦能量 HUD，并移除原有动作栏持续时间、冷却时间以及启动/结束提示。状态条使用绿色与红色格子显示可用时间和已消耗时间；总格数会根据基础持续时间与反应调幅后的实际容量按秒四舍五入。激活时格子从右向左由绿色变为红色，普通型号冷却时从左向右恢复为绿色，游隼与远地点的部分充能也会按照实际恢复的可用秒数更新格子。
- 将斯安威斯坦状态条注册为 Epic Fight HUD 设置中的独立可调整组件，不再跟随 Epic Fight 原有技能或武器技能 HUD。状态条默认水平居中，支持 Epic Fight 原生的水平/垂直锚点与 X/Y 拖动设置，并会根据窗口和 GUI 尺寸自动缩放。
- 新增状态条战斗可见性规则：造成或受到伤害、锁定有效敌人时进入战斗显示状态，最后一次战斗活动结束 5 秒后退出；能量未满时即使脱战也会持续显示，能量充满且脱战后等待 1 秒再开始渐隐，并在随后 1 秒内完全隐藏。
- 修复手动按 `V` 提前结束斯安威斯坦后，客户端状态条仍按照旧的激活结束时间继续减少、且恢复状态需要再次启动技能后才会刷新的问题。服务端结束同步现在会立即清除本地激活状态并切换到正确的剩余能量或冷却恢复显示。
- 为斯安威斯坦技能书替换新的专用图标，并加入状态条所使用的绿色、红色格子纹理资源。
- 新增 `Use EpicParCool Default CatLeap Animation` / `使用 EpicParCool 默认猫跳动画` 配置，默认关闭并放置在 Alt+P 设置界面的 `Auto FastRun Dash` 下方。开启后恢复 EpicParCool 原本的猫跳准备与腾空动画，跳过 WOM Natural Sprinter 的冲刺跳跃动画和运动修正，同时保留猫跳触发、耐力消耗与幻影跃升预备效果。
- 将斯安威斯坦局部时间减速扩展到客户端粒子系统：范围内的普通粒子和 TrackingEmitter 使用独立的局部 Tick 时钟降低更新频率，并使用对应的局部 partialTick 平滑渲染；离开影响范围后恢复正常速度。施术者自己的斯安威斯坦残影被明确排除，继续保持原有生成、运动与淡出速度。
- 将局部时间减速扩展到原版雨雪的特殊程序化动画。雨水纹理滚动、雪花下落与横向漂移会按照所在 X/Z 天气列的局部时间倍率减速，范围外天气继续使用世界正常时间，并通过独立局部时间和 partialTick 避免低速天气动画产生明显跳动。
- 修复玩家获得速度效果后，Epic Fight 普通 RUN 动画播放速度没有随实际水平速度提高的问题。原有扩展逻辑在 `MovementAnimation#getPlaySpeed` 阶段使用 `x - xo` / `z - zo` 采样，但该时机的位移尚未更新，结果始终为 `0`。现在直接使用 `getDeltaMovement().horizontalDistance()` 驱动现有平滑曲线；普通速度范围保持 Epic Fight 原始行为，超过原版疾跑速度后继续加速至配置上限。该修复适用于 Epic Fight 武器 RUN、WOM `BIPED_SPRINT` / `BIPED_SPRINT_BAREHAND` 及其他 `MovementAnimation`，不会直接影响 LinkAnimation、攻击、跳跃或 Step 动画。

#### English

- Added an independent Sandevistan energy HUD and removed the previous action-bar duration, cooldown, activation, and deactivation messages. Green and red cells represent available and consumed time, with the total cell count rounded to the nearest second from the actual capacity after base duration and Reaction Tuning are applied. While active, cells turn from green to red from right to left; ordinary-model cooldowns restore green cells from left to right, and Falcon/Apogee partial charge updates the bar according to the actual usable seconds recovered.
- Registered the Sandevistan bar as an independent adjustable component in Epic Fight's HUD setup screen instead of attaching it to Epic Fight's existing skill or weapon-skill HUD. The bar is horizontally centered by default, supports Epic Fight's native horizontal/vertical anchors and X/Y positioning, and automatically scales with the window and GUI size.
- Added combat-based visibility rules for the bar. Dealing or receiving damage and focusing a valid enemy marks combat activity, which expires five seconds after the last activity. The bar remains visible while energy is incomplete even out of combat; once fully charged and out of combat, it waits one second before fading out completely over the following second.
- Fixed manually ending Sandevistan with `V` leaving the client bar draining against the old activation end time and preventing the recovery display from refreshing until the skill was activated again. Server stop synchronization now immediately clears the local activation state and switches the HUD to the correct remaining-energy or cooldown-recovery state.
- Replaced the Sandevistan skill-book icon with a dedicated new texture and added the green/red cell textures used by the energy HUD.
- Added the `Use EpicParCool Default CatLeap Animation` option, disabled by default and positioned directly below `Auto FastRun Dash` in the Alt+P settings screen. Enabling it restores EpicParCool's original CatLeap preparation and airborne animations, skips WOM Natural Sprinter's sprint-jump animation and motion adjustment, and preserves CatLeap activation, stamina use, and Phantom Ascent priming.
- Extended Sandevistan local time dilation to the client particle system. Standard particles and tracking emitters inside the affected area use independent local Tick clocks for reduced update frequency and matching local partialTicks for smooth rendering, then return to normal speed outside the field. The caster's own Sandevistan afterimages are explicitly excluded and retain their existing spawn, movement, and fade timing.
- Extended local time dilation to vanilla rain and snow procedural animation. Rain texture scrolling, snow descent, and horizontal snow drift now follow the local time scale of each affected X/Z weather column, while precipitation outside the field continues using normal world time. Independent local time and partialTick values prevent visibly jerky low-speed weather animation.
- Fixed Epic Fight ordinary RUN animations not increasing their playback speed when the player had a movement-speed effect. The previous extension sampled `x - xo` and `z - zo` during `MovementAnimation#getPlaySpeed`, but movement had not yet updated at that stage and the measured value remained `0`. The existing smoothed curve is now driven directly by `getDeltaMovement().horizontalDistance()`; ordinary-speed ranges retain Epic Fight's original behavior, while movement above vanilla sprint speed continues toward the configured cap. The fix applies to Epic Fight weapon RUN animations, WOM `BIPED_SPRINT` / `BIPED_SPRINT_BAREHAND`, and other `MovementAnimation` instances without directly affecting LinkAnimation, attacks, jumps, or Step animations.

### 斯安威斯坦渲染兼容修复

#### 中文

- 修复其他模组修改 `ParticleEngine` 粒子渲染路径后，斯安威斯坦粒子局部 partialTick 的强制 Mixin 注入找不到原始 `Particle.render` 调用、进而在游戏初始化阶段触发 `MixinTransformerError` 崩溃的问题。粒子渲染平滑钩子现已改为可选兼容注入；如果 Embeddium、Oculus 或其他模组替换了对应渲染路径，游戏将继续正常启动并保留粒子 Tick 减速，只停用该路径上的局部渲染插值并输出一次兼容警告。
- 修复在启用 Oculus 光影与 Distant Horizons 时，开启斯安威斯坦会导致远处 LOD 越过近处方块、地形和实体显示的问题。原因是技能激活过程中动态调用主渲染目标的 `enableStencil()`，该操作会销毁并重建主帧缓冲的颜色与深度附件，使 Distant Horizons 或 Oculus 缓存的深度关联失效。
- 移除对 Minecraft、Oculus 和 Distant Horizons 共用主渲染目标的 Stencil 修改。斯安威斯坦现在使用独立的轻量 Mask RenderTarget 绘制本地玩家、第一人称手部、残影和其他斯安威斯坦使用者，再由后处理 Shader 根据遮罩决定哪些像素不受绿色滤镜影响。
- 窗口尺寸或主帧缓冲附件变化时，只释放并重建斯安威斯坦自己的 Mask 与 PostChain，不再重建或替换主颜色、深度附件。Distant Horizons 在全屏或分辨率切换后仍需自行重建的管线问题不再由 EPM 注入额外光影重载或帧缓冲修复。

#### English

- Fixed a startup `MixinTransformerError` that occurred when another mod replaced the `ParticleEngine` rendering path and the mandatory Sandevistan local-partialTick redirect could no longer find the original `Particle.render` invocation. The particle smoothing hook is now an optional compatibility injection: when Embeddium, Oculus, or another mod replaces that path, the game continues loading and particle Tick slowdown remains active, while only local render interpolation for that path is disabled with a single compatibility warning.
- Fixed Distant Horizons LOD rendering through nearby blocks, terrain, and entities when Sandevistan was activated with Oculus shaders enabled. The issue was caused by dynamically calling `enableStencil()` on the main render target during activation, which destroyed and recreated the main framebuffer color and depth attachments and invalidated depth references cached by Distant Horizons or Oculus.
- Removed all stencil modification of the main render target shared by Minecraft, Oculus, and Distant Horizons. Sandevistan now draws the local player, first-person hands, afterimages, and other active users into an independent lightweight Mask RenderTarget, and the post-processing shader samples that mask to decide which pixels are excluded from the green filter.
- When the window size or main framebuffer attachments change, only Sandevistan's own mask and PostChain are released and rebuilt. EPM no longer injects extra shader reloads or framebuffer repairs for the separate Distant Horizons pipeline-resize issue.

### 近期系统与兼容更新 / Recent Systems And Compatibility Updates

#### 中文

- 新增 EPM 独立创造模式物品栏，集中展示本模组注册的斯安威斯坦技能书、图标物品及后续新增内容，避免散落在 Epic Fight 原有标签页中。
- 重制 EpicParCool 趴下动画系统，加入 `crawl_enter`、`crawl_idle`、`crawl_move_l`、`crawl_move_r` 和 `crawl_exit` 五段自定义动画。每次进入趴下时首次随机选择左右领爬，后续进入时左右轮换；单次趴下期间只循环选中的一侧，停止移动进入 Idle，退出时播放 Exit。
- 修复滑铲同时触发 ParCool Crawl 状态时，趴下动画错误覆盖滑铲动画的问题。滑铲期间爬行动画处理器会立即放弃动画所有权，不再播放 Crawl Enter、Idle 或 Exit。
- 修复斯安威斯坦局部 Tick 减速同时延长实体受击无敌窗口的问题。`invulnerableTime`、`hurtTime` 和 `hurtDuration` 继续按正常世界 Tick 推进，保留实体动作与 AI 的时缓效果，同时恢复原版及模组攻击的正常连续命中节奏。
- 为被时缓影响的其他玩家和实体统一提供局部 partialTick 渲染上下文，使位置、旋转和 Epic Fight 动画使用相同的局部时间插值，降低多人环境中低 Tick 速率造成的模型抽搐。
- 新增 AsyncParticles GPU 粒子路径兼容。支持的 GPU 顶点布局会在写入粒子缓冲时应用斯安威斯坦局部插值，从而同时保留 AsyncParticles 的 GPU 加速与粒子平滑；布局不匹配时会安全停用该路径，不导致游戏崩溃。
- 更新军用科技“游隼”与“远地点”的部分充能机制：技能关闭后立即从当前剩余能量继续自然恢复，恢复途中可随时按已有能量再次启动，激活期间暂停恢复。主动关闭仍不会获得普通型号的额外冷却返还。
- 完整充能时间仍为游隼 35 秒、远地点 30 秒；反应调幅后的实际容量会按照对应完整冷却周期恢复，旧版本保存的“满层标记加部分能量”状态会自动迁移为可继续恢复的新状态。

#### English

- Added a dedicated EPM creative-mode tab that collects the mod's registered Sandevistan skill books, icon items, and future content instead of scattering them across Epic Fight's existing tabs.
- Rebuilt the EpicParCool crawl animation system with custom `crawl_enter`, `crawl_idle`, `crawl_move_l`, `crawl_move_r`, and `crawl_exit` animations. The first crawl entry chooses a lead side randomly and later entries alternate sides; each individual crawl keeps looping only its selected side, enters Idle when movement stops, and plays Exit when crawling ends.
- Fixed ParCool Slide also reporting Crawl state and allowing the new crawl controller to overwrite the slide animation. The crawl handler now relinquishes animation ownership for the entire slide action.
- Fixed local Sandevistan Tick slowdown extending entity hit-invulnerability windows. `invulnerableTime`, `hurtTime`, and `hurtDuration` continue advancing at normal world-Tick speed, preserving slowed entity actions and AI while restoring normal repeated-hit cadence for vanilla and modded attacks.
- Added a shared local-partialTick rendering context for other slowed players and entities so position, rotation, and Epic Fight animation interpolation consume the same local clock, reducing model jitter at low effective Tick rates in multiplayer.
- Added compatibility with the AsyncParticles GPU particle path. Supported GPU vertex layouts receive Sandevistan local interpolation while the particle buffer is written, preserving both AsyncParticles acceleration and smooth slowed particles; unknown layouts disable this path safely instead of crashing the game.
- Updated Militech Falcon and Apogee partial charge: after shutdown, recharge immediately resumes from the current remaining energy, and the skill can be reactivated at any point using the energy recovered so far. Recharge still pauses while active, and manual shutdown still grants no extra cooldown refund.
- Full recharge remains 35 seconds for Falcon and 30 seconds for Apogee. Capacity added by Reaction Tuning recovers across the same full cooldown period, and legacy saved states using a full-stack marker plus partial energy migrate automatically to the new continuously recharging state.

## Version Info

- Mod version: 4.0.0
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, TaCZ, Gliders
