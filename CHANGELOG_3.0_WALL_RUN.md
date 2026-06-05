# Epic ParCool: Momentum 3.0 系列跑墙功能汇总

这份 changelog 面向玩家和整合包作者，汇总 3.0 系列以来与跑墙、滑墙、转墙和蹬墙跳有关的功能性更新。

## Spider Techniques 跑墙模式

- 新增 `spiderTechniquesWallRunMode` 配置，用于决定学习 WOM Spider Techniques 后由哪个系统负责跑墙。
- 支持三种模式：
  - `DEFAULT`：保留默认跑墙行为，只应用必要的方向和兼容修复。
  - `PARCOOL`：使用 ParCool 的按键入口，触发 WOM 风格的 Spider Techniques 跑墙和滑墙。
  - `WOM`：保留 WOM 原版 Spider Techniques 跑墙系统，并禁用冲突的 ParCool 跑墙/滑墙入口。
- WOM 相关替换逻辑只会在安装 WOM、玩家已学习 Spider Techniques，并且选择对应模式时生效。
- 未安装 WOM 或未学习 Spider Techniques 时，不会强行接管 ParCool 原本跑墙。

## ParCool 模式下的 WOM 风格跑墙

- 在 `spiderTechniquesWallRunMode=PARCOOL` 时，ParCool 跑墙输入会触发 WOM 风格 Spider Techniques 跑墙。
- `W+R` 触发跑墙。
- `R` 且不按 `W` 时触发 WOM 风格滑墙。
- ParCool 原本的 WallSlide 快捷键仍然可以触发 WOM 风格滑墙。
- `Shift+R` 或 `Shift+WallSlide` 可以进入慢速滑墙。
- `W+R` 会优先判定为跑墙，不会被滑墙输入抢走。
- 跑墙耐力消耗与 WOM 原版保持一致。
- ParCool 模式下补回 WOM 风格滑墙手部粒子，粒子数量和位置尽量匹配 WOM 原版表现。

## WOM 模式下的原版跑墙保留

- 在 `spiderTechniquesWallRunMode=WOM` 时，WOM 原版 Spider Techniques 跑墙系统保持主导。
- 学会 Spider Techniques 后，ParCool 的横向跑墙、滑墙和对应快捷键会被禁用，避免和 WOM 原版输入冲突。
- WOM 模式下保留 WOM 原版侧向跑墙、滑墙、蹬墙跳和相关动作衔接。
- 修复 WOM 侧向跑墙接 WOM 原版蹬墙跳时判定不稳定的问题。
- 修复 WOM 侧向跑墙接 ParCool WallJump 后，跑墙动作残留、落地后仍保持侧跑动画的问题。
- 修复 WOM 模式下 `Ctrl+Shift` 慢速滑墙被强制下墙的问题。
- 横向跑墙中按住 `A` 或 `D` 时，`Shift` 不会再错误地强制下墙。

## 90 度墙角转墙

- 新增 90 度墙角转墙能力，侧向跑墙到墙角时可以转移到相邻墙面继续跑墙。
- 转墙后会重新面对新墙面，人物朝向和跑墙方向会按新墙面重新计算。
- 支持在转墙后继续横向跑墙，也可以重新进入竖向跑墙。
- 跑墙方向以当前墙面为基准：
  - `A` 表示沿当前墙面向左跑。
  - `D` 表示沿当前墙面向右跑。
- 镜头不再反转实际跑墙方向，避免因为摄像机角度导致倒着跑。
- 墙角检测加入保护，减少墙角抖动、反复切墙和吸附到空气墙的问题。
- DEFAULT / WOM / PARCOOL 相关模式下的跑墙方向和转墙边界更清晰，尽量避免一个模式的行为影响另一个模式。

## 跑墙与蹬墙跳衔接

- WOM 侧向跑墙可以更稳定地衔接 WOM 原版蹬墙跳。
- WOM 侧向跑墙也可以衔接 ParCool WallJump。
- ParCool WallJump 真正触发后才清理 WOM 侧跑状态，避免过早打断 WOM 原版跑墙链路。
- 蹬墙跳后会正确结束跑墙动画，落地后不再残留侧向跑墙动作。
- ParCool WallJump 后仍可按配置衔接疾跑、空中攻击或 Phantom Ascent。

## 跑墙状态修复

- 修复离墙后 WOM 跑墙或滑墙状态残留的问题。
- 修复落地后仍播放跑墙动画的问题。
- 修复在平地上错误保持跑墙动作的问题。
- 修复贴墙从地面 `W+R` 启动竖向跑墙时，被落地清理立刻打断并转成翻滚的问题。
- 优化跑墙、滑墙和墙跳之间的动作退出，让状态切换更稳定。

## 相关配置

- `spiderTechniquesWallRunMode`
  - 控制 Spider Techniques 学习后使用 DEFAULT、PARCOOL 还是 WOM 跑墙模式。
- `disableVerticalWallRunWithSpiderTechniques`
  - 学会 Spider Techniques 后是否禁用 ParCool 原版竖向跑墙。
- `parCoolWallRunAnimationTransition`
  - 调整 ParCool 模式下 WOM 风格跑墙动作切换的过渡时间。
- `debugSpiderTechniquesAttackState`
  - 调试 Spider Techniques 攻击状态用，正常游玩默认关闭。

## 总结

3.0 系列的跑墙更新重点是减少 ParCool、EpicParCool 和 WOM Spider Techniques 之间的输入冲突，并让跑墙、滑墙、墙角转向和蹬墙跳更像一个完整系统。玩家可以根据自己想要的手感选择 ParCool 模式或 WOM 模式，而不是让两个跑墙系统同时抢输入和动作状态。
