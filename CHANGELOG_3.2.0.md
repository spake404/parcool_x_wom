# Epic ParCool: Momentum 3.2.0 更新日志

## 重点更新

### Spider Techniques + ParCool 跑墙模式优化

- 优化 ParCool 跑墙模式下的 WOM Spider Techniques 竖向跑墙、横向跑墙和滑墙衔接。
- 修复跑墙结束、落地或离开墙面后，WOM 跑墙/滑墙动画和内部状态可能残留的问题。
- 修复贴墙从地面按 `W+R` 启动竖向跑墙时，可能被落地判定立刻清掉并触发翻滚的问题。
- 跑墙入口保持为 `W+R`，并继续只在已学习 Spider Techniques 且配置为 ParCool 跑墙模式时生效。

### 新增 WOM 风格滑墙兼容

- 在 ParCool 跑墙模式下，`R` 且不按 `W` 可以触发 WOM 风格滑墙。
- ParCool 原本的“贴墙滑降 / WallSlide”快捷键仍然保留，也可以手动触发 WOM 风格滑墙。
- `Shift+R` 和 `Shift+贴墙滑降` 都会进入慢速滑墙。
- `W+R` 优先跑墙，不会被滑墙输入抢占。
- 滑墙可以衔接 WOM 的蹬墙跳/后翻逻辑，耐力检查与 WOM 原版保持一致。

### 状态与性能优化

- 优化 WOM Spider Techniques 状态写入，避免每 tick 重复同步没有变化的数据。
- 将临时调试日志降级为 debug 或默认关闭，减少正常游戏日志噪声。
- 增加跑墙/滑墙状态兜底清理，避免平地残留动作。
- 保持兼容模块只在需要时运行，降低不必要的每 tick 检查。

## 使用说明

- 该版本的 ParCool 跑墙模式只在安装 WOM、玩家学习 Spider Techniques，并且配置项选择 ParCool 跑墙模式时接管。
- 其他模式、未学习 Spider Techniques、或未安装 WOM 时，不会触发本模块的 WOM 跑墙/滑墙替换逻辑。
- 跑墙耐力消耗与 WOM 原版一致，跑墙时为 `0.5` 耐力 / tick。

## 版本信息

- Mod 版本：3.2.0
- Minecraft / Forge：1.20.1
- 主要兼容：Epic Fight、ParCool、EpicParCool、Weapons of Miracles
