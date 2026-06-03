# Epic ParCool: Momentum 3.2.1 更新日志

## 重点更新

### WOM Spider Techniques 模式稳定化

- 完成 `spiderTechniquesWallRunMode=WOM` 模式的测试版稳定化。
- 在已安装 WOM、玩家已学习 Spider Techniques、并切换到 WOM 模式时，保留 WOM 原版 Spider Techniques 跑墙系统。
- 在 WOM 模式下禁用 ParCool 的横向跑墙、滑墙以及对应快捷键冲突，避免 ParCool 输入抢走 WOM 原版动作。
- 保持 DEFAULT/PARCOOL 模式原有行为边界，不把 WOM 模式的改动扩散到其他模式。

### 90 度墙面切换

- WOM 模式侧向跑墙到 90 度墙角时，可以根据当前跑墙前进方向自动转到相邻墙面继续侧向跑墙。
- 转墙后会重新锁定新墙面，身体 yaw、跑墙方向和移动向量都按新墙的面对方向重新计算。
- `A`/`D` 方向以当前墙面为基准，不再被摄像机方向反转。
- 摄像机仍可用于选择转墙目标，但不会改变实际侧向移动方向。
- 增加墙面接触检测、转墙冷却和回退保护，减少墙角抖动、反向跑墙和空气墙吸附。

### 蹬墙跳兼容

- 恢复 WOM 模式侧向跑墙接 WOM 原版蹬墙跳的一次成功判定。
- 修复蹬墙跳后 WOM 侧向跑墙动画残留、落地后仍保持侧跑动画的问题。
- 重新加入 ParCool WallJump bridge：只在 WOM 侧向跑墙中、且 ParCool 蹬墙跳输入成立时补充 ParCool WallJump 启动信息。
- ParCool WallJump 真正启动后才清理 WOM 侧跑状态，避免提前破坏 WOM 原版输入链路。

### 滑墙与粒子

- 修复 WOM 模式中 `Ctrl+Shift` 慢速滑墙被强制下墙的问题。
- 在 WOM 模式下禁用 ParCool 原版滑墙功能，避免和 WOM Spider Techniques 滑墙状态冲突。
- ParCool 替换模式下恢复 WOM 风格滑墙手部粒子，并调整为和 WOM 原版数量、位置一致。

### 代码整理

- 将 Spider Techniques 跑墙相关逻辑拆分为明确的职责类：
  - `WomSpiderWallRunModeGate`
  - `WomSpiderWallHooks`
  - `WomSpiderWallContactResolver`
  - `WomSpiderWallCornerTransfer`
  - `WomSpiderWallYawLock`
  - `WomOriginalSpiderWallRunDirectionFix`
  - `WomOriginalSpiderWallRunDiagnostics`
  - `WomParCoolWallJumpBridge`
- 清理 `EPMClientHooks` 中过重的 WOM 跑墙逻辑，把每 tick hook 转发到独立 helper。
- 降低常规 tick 中不必要的状态写入和日志输出，保留 debug 诊断能力。

## 验证

- 已通过 `./gradlew clean compileJava processResources`。

## 版本信息

- Mod 版本：3.2.1
- Minecraft / Forge：1.20.1 / 47.4.20
- 主要兼容：Epic Fight、ParCool、EpicParCool、Weapons of Miracles
