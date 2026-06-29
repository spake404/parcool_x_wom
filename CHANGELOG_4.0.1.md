# Epic ParCool: Momentum 4.0.1 Changelog

## 4.0.1 Hotfix

- Vault 后自动恢复 FastRun 时，不再触发免费的 Natural Sprinter 起步 Step；FastRun 恢复和连段窗口仍然保留。
- 这个抑制只作用于 Vault hold/grace 恢复窗口，不影响正常按 FastRun 触发的起步 Step，也不影响 R 键手动 FastRun Step。

## 中文

### Vault / FastRun 稳定性

- 修复 FastRun 撞墙或被 ParCool 条件提前结束后，下一帧 Vault 已经满足几何条件却因为 `FastRun.canActWithRunning=false` 被拒绝的问题。
- 新增 `vaultStartFastRunGrace` 配置项，默认开启；关闭后恢复为只有 ParCool FastRun 正在 doing 时才能作为 Vault 起点。
- Vault 起跳宽限仍然要求 ParCool 原生 Vault 几何、移动输入、FastRun 按键模式、非潜行、非水中、非飞行、非载具等条件通过，不会绕过基础安全条件。
- Vault 调试日志会在宽限实际放行时记录 `phase=vault_start_fast_run_recent_grace`，方便继续定位偶发失败。

### 性能与实现

- FastRun 最近状态不再通过 `FastRun.onClientTick` 每 tick 记录。
- 改为在 ParCool `FastRun.onStopInLocalClient` 停止回调和 `canActWithRunning=true` 查询结果处记录一次最近 FastRun tick。
- 宽限窗口只在 Vault/FastRun 查询路径中检查；兼容层不会在服务器 tick 中扫描方块，也不会每 tick 调用 `getVaultableStep` / `getWallHeight`。
- 将 Vault 起跳 FastRun 宽限拆成独立 `VaultStartFastRunGrace` 模块，避免继续扩大 `EPMClientHooks`。

## English

### Vault / FastRun Stability

- Fixed a case where FastRun could stop after hitting a wall, then Vault geometry was valid on the next frame but ParCool rejected Vault because `FastRun.canActWithRunning=false`.
- Vault FastRun recovery no longer triggers a free Natural Sprinter startup Step after Vault finishes; it still restores FastRun state for chaining, but the post-Vault recovery path is now step-suppressed.
- The suppression is scoped to the Vault hold/grace recovery window and does not disable normal manual FastRun startup Steps or R-key FastRun Step input.
- Added `vaultStartFastRunGrace`, enabled by default. Disabling it restores the stricter behavior where Vault start requires ParCool FastRun to still be actively doing.
- The Vault start grace still requires ParCool's native Vault geometry, movement input, FastRun key mode, and hard blockers such as sneaking, water, fall-flying, and vehicles to pass.
- Vault debug logging now records `phase=vault_start_fast_run_recent_grace` when the grace path actually allows a start.

### Performance And Implementation

- Recent FastRun state is no longer sampled from `FastRun.onClientTick` every tick.
- The compatibility layer now records one recent FastRun tick from ParCool's `FastRun.onStopInLocalClient` callback and from successful `canActWithRunning=true` queries.
- The grace window is checked only from the Vault/FastRun query path; it does not scan blocks on server ticks and does not call `getVaultableStep` / `getWallHeight` every tick.
- The Vault start grace logic was split into a focused `VaultStartFastRunGrace` module instead of expanding `EPMClientHooks`.
