# Epic ParCool: Momentum 3.6.0 更新日志 / Changelog

## 中文

### Spider Techniques 跑墙与滑墙

- 优化 ParCool 模式下的 WOM 风格滑墙输入优先级。
- 修复鼠标右键滑墙会抢占 ParCool 其他右键动作的问题。
- 现在右键在可触发挂杠、撑越、猫挂/边缘悬挂、使用滑索、跳离横杠、翻越时，会优先交给 ParCool 原动作处理。
- 在 Spider Techniques 替换模式下停用 ParCool 原版 WallSlide 动作本体，避免 ParCool WallSlide 和 WOM 风格滑墙同时抢移动与动画状态。
- 保留 ParCool 模式下的 WOM 风格滑墙、慢速滑墙、滑墙粒子和蹬墙跳衔接。

### 跑墙状态稳定性

- 改进 ParCool 模式下 Spider Techniques 跑墙与 ParCool ClimbUp/翻越之间的互斥逻辑。
- 修复翻越或爬墙动作后，按住跑墙键可能重新接回竖向跑墙动画的问题。
- 改进落地与脚下方块支撑检测，减少跑墙状态残留和落地后继续播放跑墙动画的情况。
- 增加 Spider Techniques 跑墙状态调试选项，方便排查墙面动作、落地状态和动画残留问题。

### 代码与性能

- 整理右键动作优先级判断为独立模块，降低滑墙 handler 的复杂度。
- 优化滑墙右键让位检测，复用临时缓冲区，减少每 tick 对象分配。
- 移除滑墙方向读取中的反射访问，改为直接读取 ParCool WallSlide 状态。

## English

### Spider Techniques Wall Run And Wall Slide

- Improved right-click input priority for WOM-style wall slide in ParCool mode.
- Fixed an issue where right-click wall slide could block other ParCool right-click actions.
- Right click now yields to ParCool actions such as Hang Down, Vault, Cling To Cliff, Ride Zipline, Jump From Bar, and Climb Up when those actions are available.
- Disabled ParCool's original WallSlide action body in Spider Techniques replacement modes to prevent ParCool WallSlide and WOM-style wall slide from fighting over movement and animations.
- WOM-style wall slide, slow wall slide, slide particles, and wall-jump transitions remain available in ParCool mode.

### Wall State Stability

- Improved mutual exclusion between Spider Techniques wall run and ParCool ClimbUp/Vault-style climb transitions in ParCool mode.
- Fixed cases where holding the wall-run key after a climb transition could restart vertical wall-run animation.
- Improved landing and ground-support checks to reduce lingering wall-run states and animation leftovers after touching ground.
- Added a Spider Techniques wall-run debug option for diagnosing wall action, landing, and animation state issues.

### Code And Performance

- Moved ParCool right-click action priority checks into a dedicated helper module.
- Reduced per-tick allocations in wall-slide priority checks by reusing the temporary action-start buffer.
- Removed reflective WallSlide direction access and switched to direct ParCool WallSlide state reads.

## Version Info

- Mod version: 3.6.0
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, TaCZ
