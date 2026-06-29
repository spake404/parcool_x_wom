# Epic ParCool: Momentum Missing Changelog Supplement

This file records implemented features that were missing from earlier `CHANGELOG*.md` files, or were only described broadly without the current configuration names.

## 中文

### FastRun Step / Dodge 共键仲裁

- 新增 `naturalSprinterStepDodgeConflictMode`，用于处理 FastRun Step 和 ParCool Dodge 绑定到同一个按键时的冲突。
- 支持三种仲裁模式：
  - `short_dodge_long_step`：短按触发 Dodge，长按释放后触发 Step。
  - `short_step_hold_dodge`：短按触发 Step，长按触发 Dodge。
  - `single_step_double_dodge`：单击触发 Step，双击触发 Dodge。
- 新增 `naturalSprinterStepDodgeLongPressTicks`、`naturalSprinterStepDodgeDoubleTapGapTicks`、`naturalSprinterStepDodgeFirstTapMaxTicks`，可调整长按和双击判定窗口。
- 当 ParCool Dodge 被禁用时，共键仲裁会回退为 Step-only 行为，避免无意义地拦截 Step 输入。
- 新增 `genericFastRunStepCooldownTicks`，用于无 WOM Natural Sprinter 时的通用 FastRun Step 冷却；有 WOM 时仍优先使用 WOM 自己的 Step 资源。

### Phantom Ascent 衔接细节

- 新增 `climbUpPrimesPhantomAscent`，允许 ParCool ClimbUp 后按跳跃衔接 Epic Fight Phantom Ascent，并在该路径上优先处理滑翔伞请求，减少误展开。
- 新增 `replacePhantomAscentDoubleJumpAnimations`，可选择把 Phantom Ascent 的起跳、下落和落地动画替换为本模组的二段跳动画组。
- `phantomAscentFallProtectionDamageThreshold` 已暴露为可配置项，用于调整 Phantom Ascent 下一次摔落保护可抵消的最大伤害。

### WallJump 战斗衔接

- 新增 `wallJumpPrimesAirAttack`，ParCool WallJump 后会打开短时间 Epic Fight 空中攻击窗口。
- 新增 `wallJumpAirAttackFallProtectionDamageThreshold`，用于控制 WallJump 空中攻击窗口后的摔落保护阈值。
- 新增 `taczShootDuringWallJump`，TaCZ 枪械可以打断 ParCool WallJump 并立即开火，避免蹬墙跳状态阻塞射击输入。

### EpicParCool ClimbUp 手感补偿

- 新增 `epicParCoolClimbUpVerticalVelocity`，为 EpicParCool ClimbUp 后的上升速度提供可配置下限。
- 新增 `epicParCoolClimbUpLateralAirControlVelocity` 和 `epicParCoolClimbUpLateralAirControlTicks`，在 ClimbUp 后短时间提供左右横向空中控制补偿。
- ClingToCliff 横移、面墙和跳跃时机满足条件时，可以更稳定地衔接 ClimbUp。

### EpicFightX Combat Mastery II 兼容

- 新增 `epicFightXCombatMasteryFastRunControlCompatibility`，学会 EpicFightX Combat Mastery II 后，可由本模组接管闪避后的疾跑触发逻辑。
- 新增 `epicFightXCombatMasterySprintTriggerMode`，支持 `Toggle`、`Auto`、`PressKey` 三种触发模式，让 Combat Mastery II 的闪避后疾跑更接近 ParCool FastRun 操作习惯。
- Combat Mastery II 停止疾跑时会清理相关速度效果和残留 ParCool FastRun 速度修饰，并可衔接 Natural Sprinter / FastRun 起手 Step。

### WOM Spider Techniques 墙跳优先级

- 新增 `spiderWallJumpWomFrontAngle`，按玩家面对墙面的水平角度决定 WOM Spider Techniques 墙跳是否优先于 ParCool WallJump。
- 默认角度允许正面看墙时走 WOM 墙跳，侧向跑墙输入时更倾向 ParCool WallJump，减少两个模块抢输入。

### 调试与诊断工具

- 新增 `debugCameraEventState`，用于记录相机角度事件、旋转同步和姿态追踪信息，方便排查偶发 FOV / 相机抖动。
- 新增 `debugEpicFightXCombatMasterySprintState`，用于记录 Combat Mastery II 疾跑接管、速度修饰、FOV 计算和动画状态。
- 新增 `debugExhaustionPoseState`，用于记录低体力、ParCool 体力、Epic Fight 体力和当前姿态动画。
- 新增 `debugGliderState`、`debugVaultState`、`debugClingToCliffState`、`debugNaturalSprinterFastRunStepState` 等细分调试开关，相关日志均可通过配置关闭。

### 配置和界面整理

- 上述用户可见配置已加入 Alt+P 设置界面，并补全中英文显示名。
- 新增一次性内部配置 `parCoolDodgeDefaultMigrationApplied`，用于记录 ParCool Dodge 默认关闭迁移是否已执行，避免之后覆盖玩家手动修改。

## English

### FastRun Step / Dodge Shared-Key Arbitration

- Added `naturalSprinterStepDodgeConflictMode` to resolve conflicts when FastRun Step and ParCool Dodge are bound to the same key.
- Added three arbitration modes:
  - `short_dodge_long_step`: short press triggers Dodge; long press then release triggers Step.
  - `short_step_hold_dodge`: short press triggers Step; long press triggers Dodge.
  - `single_step_double_dodge`: single tap triggers Step; double tap triggers Dodge.
- Added `naturalSprinterStepDodgeLongPressTicks`, `naturalSprinterStepDodgeDoubleTapGapTicks`, and `naturalSprinterStepDodgeFirstTapMaxTicks` to tune long-press and double-tap timing.
- When ParCool Dodge is disabled, shared-key arbitration falls back to Step-only behavior instead of intercepting Step input unnecessarily.
- Added `genericFastRunStepCooldownTicks` for the generic no-WOM FastRun Step cooldown. WOM Natural Sprinter still uses WOM's own Step resource when available.

### Phantom Ascent Follow-Up Details

- Added `climbUpPrimesPhantomAscent`, allowing ParCool ClimbUp to prime Epic Fight Phantom Ascent on jump input while prioritizing this path over glider deployment.
- Added `replacePhantomAscentDoubleJumpAnimations`, optionally replacing Phantom Ascent jump, fall, and landing animations with this mod's double-jump animation set.
- Exposed `phantomAscentFallProtectionDamageThreshold` as a configurable maximum fall-damage value for Phantom Ascent next-fall protection.

### WallJump Combat Follow-Ups

- Added `wallJumpPrimesAirAttack`, opening a short Epic Fight air-attack window after ParCool WallJump.
- Added `wallJumpAirAttackFallProtectionDamageThreshold` to tune fall protection after a WallJump air-attack window.
- Added `taczShootDuringWallJump`, allowing TaCZ guns to cancel ParCool WallJump and fire immediately instead of being blocked by the WallJump state.

### EpicParCool ClimbUp Feel Compensation

- Added `epicParCoolClimbUpVerticalVelocity`, a configurable minimum upward velocity after EpicParCool ClimbUp.
- Added `epicParCoolClimbUpLateralAirControlVelocity` and `epicParCoolClimbUpLateralAirControlTicks` for a short left/right air-control compensation window after ClimbUp.
- Improved ClimbUp chaining from ClingToCliff side movement when wall-facing and jump timing conditions are valid.

### EpicFightX Combat Mastery II Compatibility

- Added `epicFightXCombatMasteryFastRunControlCompatibility`, letting this mod own post-dodge sprint triggering after learning EpicFightX Combat Mastery II.
- Added `epicFightXCombatMasterySprintTriggerMode` with `Toggle`, `Auto`, and `PressKey` modes so Combat Mastery II sprint can follow a ParCool FastRun-like control style.
- When Combat Mastery II exits sprint, the compatibility path clears related speed effects and stale ParCool FastRun speed modifiers, and can hand off into the Natural Sprinter / FastRun startup Step.

### WOM Spider Techniques Wall-Jump Priority

- Added `spiderWallJumpWomFrontAngle`, which decides whether WOM Spider Techniques wall jump should keep priority over ParCool WallJump based on the player's horizontal look angle into the wall.
- The default angle lets direct wall-facing input use WOM wall jump while side-facing wall-run input tends to prefer ParCool WallJump.

### Debug And Diagnostics

- Added `debugCameraEventState` for camera-angle event logs, rotation sync traces, and pose traces used to investigate intermittent FOV / camera shake.
- Added `debugEpicFightXCombatMasterySprintState` for Combat Mastery II sprint ownership, speed modifier, FOV, and animation diagnostics.
- Added `debugExhaustionPoseState` for low-stamina, ParCool stamina, Epic Fight stamina, and current pose animation diagnostics.
- Added focused debug switches such as `debugGliderState`, `debugVaultState`, `debugClingToCliffState`, and `debugNaturalSprinterFastRunStepState`; these logs are all gated by config options.

### Config And UI Cleanup

- The user-facing options above are available in the Alt+P settings screen and have English and Chinese localization.
- Added the internal one-time `parCoolDodgeDefaultMigrationApplied` marker so the ParCool Dodge default-off migration does not overwrite later player changes.
