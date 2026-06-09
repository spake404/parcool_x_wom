# Epic ParCool: Momentum 3.7.0 更新日志 / Changelog

## 中文

### Gliders 与 Phantom Ascent

- 新增 Gliders 兼容路径，用于处理 Phantom Ascent / 幻影跳跃启动时与滑翔伞同时触发的问题。
- Phantom Ascent 启动后的前 13 tick 内，滑翔伞模型不会提前显示，Gliders 打开动画不会提前开始，玩家身体的 gliding 动画也会被延迟。
- 延迟窗口结束后，Gliders 会恢复自己的原生打开流程，并从头播放滑翔伞打开动画。
- 该逻辑只在本地玩家、Gliders active、且当前 Epic Fight 动作为 Phantom Ascent 时生效，不会影响普通滑翔伞使用。

### 动作优先级

- 当玩家同时触发 Phantom Ascent 和 Gliders 时，优先表现 Phantom Ascent 的启动动作。
- 避免二段跳启动帧中滑翔伞模型、滑翔伞打开动画和身体滑翔动画同时抢状态，减少动作衔接突兀感。

### 代码与性能

- Gliders 相关 mixin 只会在 `vc_gliders` 安装时加载。
- 滑翔伞延迟判断按玩家 tick 缓存，同一 tick 内多个渲染和动画入口不会重复查询 Epic Fight capability 与当前动画。
- `GliderData` 的一次 `glideAndFallLogic` 调用内只计算一次延迟状态。
- 移除用于排查动画开始时间的临时 debug 日志，避免正常游玩时产生额外日志开销。

## English

### Gliders And Phantom Ascent

- Added a Gliders compatibility path for cases where Phantom Ascent and the glider are triggered at the same time.
- During the first 13 ticks of Phantom Ascent startup, the glider model is hidden, the Gliders opening animation is not started, and the player's gliding animation is delayed.
- After the delay window ends, Gliders resumes its native opening flow and plays the glider opening animation from the beginning.
- The delay only applies to the local player while Gliders is active and the current Epic Fight animation is Phantom Ascent, so normal glider usage is not affected.

### Action Priority

- When Phantom Ascent and Gliders are triggered together, Phantom Ascent now owns the startup visual priority.
- This prevents the glider model, glider opening animation, and body gliding animation from fighting the Phantom Ascent startup on the same frame.

### Code And Performance

- Gliders mixins are loaded only when `vc_gliders` is installed.
- Glider delay checks are cached per player tick, avoiding repeated Epic Fight capability and current-animation lookups from multiple render and animation hooks in the same tick.
- Each `GliderData.glideAndFallLogic` call computes the delay state only once.
- Temporary animation-start debug logging was removed to avoid normal gameplay log overhead.

## Version Info

- Mod version: 3.7.0
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, TaCZ, Gliders
