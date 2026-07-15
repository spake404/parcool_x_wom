# Epic ParCool: Momentum 4.0.1 Changelog

## 中文

### Vault / FastRun 稳定性

- 修复 FastRun 撞墙或被 ParCool 条件提前结束后，下一帧 Vault 几何条件已经满足却因为 `FastRun.canActWithRunning=false` 被拒绝的问题。
- 新增并启用 `vaultStartFastRunGrace`，允许 Vault 在 FastRun 刚刚停止后的短窗口内继续按 ParCool 原生 Vault 条件启动。
- Vault FastRun 恢复不再触发一次免费的 Natural Sprinter 起步 Step；Vault 后的 FastRun 恢复和连段窗口仍然保留。
- 这个 Step 抑制只作用于 Vault hold/grace 恢复窗口，不影响正常手动 FastRun 起步 Step，也不影响 R 键手动 FastRun Step。
- Vault 起跳宽限仍然要求 ParCool 原生 Vault 几何、移动输入、FastRun 按键模式，以及潜行、水中、飞行、载具等硬性条件通过，不会绕过基础安全检查。

### ParCool WallJump / WOM Spider Techniques / Phantom Ascent 仲裁

- 修复正式整合包中 `MovementInputUpdateEvent` 顺序导致 Epic Fight `PhantomAscentSkill` 先于 ParCool `KeyRecorder` 执行的问题。
- 修复在同一帧按下墙跳键时，ParCool WallJump 因 `KeyRecorder.keyWallJump.isPressed()` 尚未更新而误判 `input_not_done`，随后被 Phantom Ascent / WOM wall backflip 抢走的问题。
- Phantom Ascent 启动前现在会进行一次 ParCool WallJump 预仲裁：当 ParCool 墙跳候选成立时，会先让 `PARCOOL_WALL_JUMP` 赢得 `JumpActionArbiter`，并取消 Phantom native start。
- 预仲裁只在 Phantom 先于 ParCool KeyRecorder 的场景下使用物理按键快照；普通 ParCool WallJump 路径仍然使用 ParCool 原生 `isInputDone()` 判断。
- WOM 侧向跑墙时，如果 ParCool 自己的墙面扫描当帧未命中，但 WOM 当前墙跑墙面仍然有效，预仲裁可以把该墙面作为 ParCool WallJump 候选依据，避免侧向跑墙蹬墙跳被错误抢走。
- 保留既有 WOM 优先级逻辑：当 `spiderWallJumpWomFrontAngle` 判断玩家正面看墙、应由 WOM Spider Techniques 墙跳优先时，不会强行改成 ParCool WallJump。
- 修复结果是：侧向跑墙输入更稳定地触发 ParCool WallJump，正面墙跳仍可按原优先级走 WOM。

### 调试与诊断

- 新增 `MovementInputUpdateEvent` listener 顺序诊断日志，用于确认正式环境中 Epic Fight、ParCool、ShoulderSurfing、ssrcamerafixes 等 listener 的实际执行顺序。
- 新增输入顺序诊断日志，记录 Phantom 入口、ParCool KeyRecorder 前后、JumpArbiter winner、按键状态和当前动画，方便定位同帧输入抢占问题。
- 新增 ParCool WallJump 候选日志，记录失败原因，例如 `input_not_done`、`no_parcool_wall`、`landing_grace`、`cooldown`，以及成功的 `ok_preinput` / `ok_preinput_wom_wall`。
- 上述诊断仍受 `debugActionArbitrationState` 控制，默认不会刷日志。

## English

### Vault / FastRun Stability

- Fixed a case where FastRun could stop after hitting a wall, then Vault geometry was valid on the next frame but ParCool rejected Vault because `FastRun.canActWithRunning=false`.
- Added and enabled `vaultStartFastRunGrace`, allowing Vault to start during a short post-FastRun grace window while still relying on ParCool's native Vault conditions.
- Vault FastRun recovery no longer triggers a free Natural Sprinter startup Step after Vault finishes; FastRun recovery and chaining behavior are preserved.
- The Step suppression is scoped to the Vault hold/grace recovery window and does not affect normal manual FastRun startup Steps or R-key FastRun Step input.
- The Vault start grace still requires ParCool's native Vault geometry, movement input, FastRun key mode, and hard blockers such as sneaking, water, fall-flying, and vehicles to pass.

### ParCool WallJump / WOM Spider Techniques / Phantom Ascent Arbitration

- Fixed a formal modpack event-order issue where Epic Fight `PhantomAscentSkill` could run before ParCool `KeyRecorder` on `MovementInputUpdateEvent`.
- Fixed same-frame wall-jump presses being rejected by ParCool as `input_not_done` because `KeyRecorder.keyWallJump.isPressed()` had not been updated yet, allowing Phantom Ascent / WOM wall backflip to steal the input.
- Phantom Ascent now performs a ParCool WallJump pre-arbitration check before native startup. If the ParCool wall-jump candidate is valid, `PARCOOL_WALL_JUMP` wins `JumpActionArbiter` and Phantom native startup is canceled.
- The pre-arbitration path uses a physical key snapshot only for the Phantom-before-KeyRecorder case; the normal ParCool WallJump path still uses ParCool's native `isInputDone()` check.
- During WOM side wall-run, if ParCool's own wall scan misses on that frame but WOM still has valid wall contact, the pre-arbitration path can use that wall contact as the ParCool WallJump candidate source.
- Existing WOM priority is preserved: when `spiderWallJumpWomFrontAngle` decides the player is facing the wall directly and WOM Spider Techniques should win, ParCool does not override WOM.
- Result: side wall-run jump input more reliably triggers ParCool WallJump, while front-facing wall jumps can still use WOM according to the configured priority.

### Debug And Diagnostics

- Added a `MovementInputUpdateEvent` listener-order dump to verify the actual runtime ordering of Epic Fight, ParCool, ShoulderSurfing, ssrcamerafixes, and related listeners.
- Added input-order diagnostics for Phantom entry, ParCool KeyRecorder head/return, JumpArbiter winner, key state, and current animation.
- Added ParCool WallJump candidate logs that explain failures such as `input_not_done`, `no_parcool_wall`, `landing_grace`, and `cooldown`, plus success reasons such as `ok_preinput` and `ok_preinput_wom_wall`.
- These diagnostics remain gated behind `debugActionArbitrationState` and are quiet by default.

## Version Info

- Mod version: 4.0.1
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, ShoulderSurfing, SSR Camera Fixes
