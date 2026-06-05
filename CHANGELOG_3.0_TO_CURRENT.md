# Epic ParCool: Momentum 3.x 使用者更新汇总

本文汇总从 3.0 系列开始到当前 3.3.0 分支的主要功能性更新，面向玩家、整合包作者和配置维护者。内部重构、调试日志和实现细节不在这里展开。

## 当前开发中

- 新增配置：默认禁止在水下游泳时使用 Epic Fight 的 Phantom Ascent / 幻影跳跃。
- 如果希望保留原本水下也能触发幻影跳跃的行为，可以关闭 `disablePhantomAscentUnderwaterSwimming`。
- 该限制只影响“水下游泳姿态”中的幻影跳跃，不影响地面、空中或普通出水/入水时的正常使用。

## 3.3.0

### Aqua Maneuvre 与 ParCool 快游兼容

- 新增 WOM Aqua Maneuvre 快游兼容。
- 未学习 Aqua Maneuvre 时，ParCool FastSwim 可以使用 WOM Mermaid 快游动作，让普通快游的动作表现更统一。
- 学习 Aqua Maneuvre 后，快游入口会按 ParCool 的 FastRun 控制模式工作：
  - Auto：进入原版游泳后自动进入快游。
  - PressKey：按住 ParCool FastRun 键进入快游。
  - Toggle：按 ParCool FastRun 键切换快游。
- 学会 Aqua Maneuvre 后，WOM 的 Mermaid 快游、水面快游、Water Dash、速度和退出逻辑会继续保留。
- 修复 Water Dash 后快游动作被普通游泳动作覆盖的问题。
- 修复快游退出后动作不回到普通游泳、站立动作残留、FOV 抖动等问题。
- 修复快游中平 A 被快游动作抢掉的问题；攻击期间可以正常出招，攻击结束后再恢复快游。
- 区分水面快游和水下 Mermaid 快游，Water Dash 后会恢复到正确的水面或水下动作。

## 3.2.1

### WOM 模式稳定化

- 完成 `spiderTechniquesWallRunMode=WOM` 的稳定化。
- 学会 WOM Spider Techniques 并切换到 WOM 模式后，保留 WOM 原版 Spider Techniques 跑墙系统。
- WOM 模式下禁用 ParCool 的横向跑墙、滑墙和对应快捷键冲突，避免 ParCool 输入抢走 WOM 原版动作。
- 修复 WOM 侧向跑墙接 WOM 原版蹬墙跳时第一次不稳定的问题。
- 修复 WOM 侧向跑墙接 ParCool WallJump 后，跑墙动作残留、落地后仍保持侧跑动作的问题。
- 修复 WOM 模式中 `Ctrl+Shift` 慢速滑墙被强制下墙的问题。

### 90 度墙角转墙

- WOM 侧向跑墙到 90 度墙角时，可以自动转到相邻墙面继续跑墙。
- 跑墙方向改为以当前墙面为基准：`A` 为左、`D` 为右，不再因为镜头方向反转而倒着跑。
- 镜头仍可影响转墙目标选择，但不会改变当前墙面上的实际侧向移动方向。
- 增加墙角转向保护，减少墙角抖动、反复吸附和“空气墙”问题。
- 转墙后仍可以重新进入竖向跑墙，不会被横向转墙状态永久锁住。

### 滑墙与粒子

- ParCool 替换模式下恢复 WOM 风格滑墙手部粒子。
- 滑墙粒子的数量和位置调整为接近 WOM 原版表现。
- WOM 模式下禁用 ParCool 原版滑墙，避免两个滑墙系统同时争抢状态。

## 3.2.0

### Spider Techniques + ParCool 跑墙模式

- 新增并完善 `spiderTechniquesWallRunMode` 配置，支持 Default、ParCool、WOM 三种模式。
- ParCool 模式下，只有满足以下条件时才接管 WOM Spider Techniques 跑墙：
  - 已安装 WOM。
  - 玩家已学习 Spider Techniques。
  - `spiderTechniquesWallRunMode=PARCOOL`。
- ParCool 模式下，`W+R` 触发 WOM 风格跑墙。
- `R` 且不按 `W` 时触发 WOM 风格滑墙。
- ParCool 原本的 WallSlide 快捷键仍可触发 WOM 风格滑墙。
- `Shift+R` 或 `Shift+WallSlide` 触发慢速滑墙。
- `W+R` 优先跑墙，不会被滑墙输入抢走。
- 跑墙耐力消耗与 WOM 原版保持一致。

### 跑墙状态修复

- 修复离墙、落地后 WOM 跑墙或滑墙状态残留的问题。
- 修复平地仍播放跑墙动画的问题。
- 修复贴墙从地面 `W+R` 启动竖向跑墙时被落地清理立刻打断的问题。
- DEFAULT/WOM/PARCOOL 三种模式的行为边界更清晰，避免一个模式的修改影响另一个模式。

## 3.0 / 3.1 系列

### Natural Sprinter 与疾跑动作

- 改进 WOM Natural Sprinter 与 EpicParCool FastRun 的兼容。
- 可选择让 ParCool FastRun 使用 Natural Sprinter 风格动作。
- 新增 Natural Sprinter 主动跨步配置，允许使用独立按键触发跨步。
- 新增 TaCZ 枪种疾跑动作配置，可以按枪种决定使用空手疾跑还是武器疾跑动作。
- 优化开火、换弹、疾跑和跨步之间的衔接，减少误触发跨步或疾跑动作被打断的情况。

### TaCZ 与战斗衔接

- TaCZ 开火可以打断 ParCool FastRun，避免开火时继续保持不合适的疾跑动作。
- TaCZ 开火可以从 ParCool WallJump 中衔接，蹬墙跳后能更自然地开枪。
- 换弹和开火时会抑制不必要的 Natural Sprinter 跨步触发。
- 持枪下落时不再播放不合适的 EpicParCool 蓄力跳准备动作。

### Phantom Ascent / 幻影跳跃与空中攻击

- CatLeap、ParCool WallJump、Spider Techniques 墙跳后可以接 Phantom Ascent。
- CatLeap、WallJump、Spider 墙跳和 Phantom Ascent 可以衔接 Epic Fight 空中攻击。
- 修复 Phantom Ascent 空中攻击窗口残留导致落地后还能异常触发的问题。
- 新增 Phantom Ascent 与 WallJump 空中攻击的摔落保护伤害阈值配置。

### WallJump、Vault、ClimbUp

- ParCool WallJump 后可以自动恢复疾跑，并提供配置开关。
- WallJump 后可以接 Epic Fight 空中攻击。
- Vault 高度倍率可配置，默认更适合三格空气跨越。
- 优化 ClingToCliff、ClimbUp、横移和拐角移动后的 ClimbUp 衔接。
- 新增 ClimbUp 纵向补偿和横向空中控制补偿配置，让爬上方块后的手感更稳定。

### ParCool 与其他兼容行为

- 学会 WOM Spider Techniques 后，可配置禁用 ParCool 原版竖向跑墙。
- ParCool Dodge 默认关闭，减少和 Epic Fight 闪避技能的按键/行为冲突；玩家仍可在 ParCool 设置中手动开启。
- SSR Camera Fixes 不再作为强依赖声明；如果玩家安装了它，优先使用它自己的身体 yaw 修正。
- WOM、TaCZ、Epic Fight Invincible、EpicFight Nightfall 等可选兼容路径会在对应模组存在时才启用。

## 使用者需要注意的配置

- `spiderTechniquesWallRunMode`
  - `DEFAULT`：保留默认行为，只应用必要的兼容修复。
  - `PARCOOL`：用 ParCool 输入驱动 WOM 风格 Spider Techniques 跑墙/滑墙替换逻辑。
  - `WOM`：保留 WOM 原版 Spider Techniques 跑墙系统，并禁用冲突的 ParCool 跑墙/滑墙输入。
- `disableVerticalWallRunWithSpiderTechniques`
  - 学会 Spider Techniques 后是否禁用 ParCool 原版竖向跑墙。
- `aquaManeuvreFastSwimAnimation`
  - 是否启用 WOM Mermaid 与 ParCool FastSwim 的动作兼容。
- `disablePhantomAscentUnderwaterSwimming`
  - 是否禁止水下游泳时使用 Phantom Ascent / 幻影跳跃，默认开启。
- `naturalSprinterAnimations`
  - 是否让 ParCool FastRun 使用 Natural Sprinter 风格动作。
- `naturalSprinterManualStep`
  - 是否启用 Natural Sprinter 主动跨步键。
- `taczBarehandSprintTypes`
  - 哪些 TaCZ 枪种使用空手疾跑动作。
- `autoSprintAfterWallJump`
  - WallJump 后是否自动恢复疾跑。
- `vaultHeightScale`
  - ParCool Vault 可跨越高度倍率。

## 总结

3.x 系列的重点是把 Epic Fight、ParCool、EpicParCool、WOM 和 TaCZ 的动作衔接做得更统一：跑墙、滑墙、蹬墙跳、快游、开火、换弹、空中攻击和幻影跳跃不再各自抢状态，而是尽量按玩家当前选择的控制模式和技能学习情况工作。
