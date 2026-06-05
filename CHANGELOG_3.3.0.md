# Epic ParCool: Momentum 3.3.0 更新日志

## 重点更新

### Aqua Maneuvre + ParCool FastSwim 兼容

- 新增 WOM Aqua Maneuvre 快游兼容系统。
- 未学习 Aqua Maneuvre 时，ParCool FastSwimAnimator 会使用 WOM Mermaid 快游动作。
- 学习 Aqua Maneuvre 后，ParCool 自己的 FastSwim 系统会被禁用，快游入口改为驱动 WOM 原版 Aqua Maneuvre 系统。
- 保留 WOM Aqua Maneuvre 的原版移动、速度、Water Dash、动作开始和动作退出逻辑。
- ParCool FastRun 控制模式现在会决定 WOM Aqua 快游入口：
  - Auto：进入原版游泳后自动进入快游。
  - PressKey：按住 ParCool FastRun 键进入快游。
  - Toggle：使用 ParCool FastRun 键切换快游。

### 快游动作稳定性

- 修复 Water Dash 后 Mermaid 快游动作被 EpicFightX 普通游泳 living motion 覆盖的问题。
- 新增水面快游 living motion 映射，水面快游 dash 后会恢复 WOM 原版 crawl 动作。
- 水下快游 dash 后会恢复 Mermaid 快游动作，水面和水下不再互相串动作。
- 修复 PressKey / Auto / Toggle 模式下退出快游后动作无法正常回到普通游泳的问题。
- 修复 Toggle 模式切换时可能出现的短暂动作抽搐。

### 攻击衔接修复

- 快游中平A不再被快游动作吞掉。
- 攻击期间不会强制重新抢回 Mermaid 快游动作。
- 修复 PressKey / Toggle 模式下快游中攻击导致 FOV 持续抖动的问题。
- 攻击期间暂停 WOM 对 `sprinting=true` 的写回，避免和 Epic Fight 攻击动作状态互相抢占。
- 攻击结束后，如果快游输入仍然成立，会继续走 WOM 原版快游恢复链路。

### 代码整理与性能优化

- 新增 `aqua` 包，将 Aqua Maneuvre 快游兼容逻辑从根包拆出。
- 新增 `animation` 包，集中放置本模组注册的 living motion 和动画资源。
- Aqua 快游可用性判断加入同 tick 缓存，减少同一 tick 内重复查询 Epic Fight playerPatch 和 WOM 技能状态。
- 新增 `debugAquaManeuvreFastSwimState` 配置，默认关闭 Aqua 详细诊断日志。
- 默认关闭 `[EPM/Aqua]` 高频 tick 日志，减少正常游戏时的日志噪音和字符串构造开销。
- WOM Aqua 输入 tick 默认只读取必要状态，诊断字段仅在 debug 配置开启时读取。

## 使用说明

- `aquaManeuvreFastSwimAnimation=true` 时启用本次 Aqua Maneuvre 快游兼容。
- 未安装 WOM 或未学习 Aqua Maneuvre 时，不会触发 WOM Aqua 快游接管逻辑。
- 如需排查 Aqua 快游状态，可临时开启 `debugAquaManeuvreFastSwimState`。

## 验证

- 已通过 `./gradlew compileJava processResources`。

## 版本信息

- Mod 版本：3.3.0
- Minecraft / Forge：1.20.1 / 47.4.20
- 主要兼容：Epic Fight、ParCool、EpicParCool、Weapons of Miracles
