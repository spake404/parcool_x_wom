# Epic ParCool: Momentum 2.1.0 更新日志

## 主要更新

### TaCZ 枪械动作兼容优化

当玩家主手拿着 TaCZ 枪械时，下蹲现在不会再触发 EpicParCool 的 Jump ChargingAnimator 蓄力跳准备动画。

这个改动用于避免持枪下蹲时出现不合适的跑酷蓄力姿态，让 TaCZ 枪械的持枪、瞄准和下蹲体验更加稳定。

## 实际效果

- 拿着 TaCZ 枪械下蹲时，不再播放 EpicParCool 的蓄力跳准备动画。
- 该限制只针对 TaCZ 枪械生效。
- 不影响未持枪时的 ParCool / EpicParCool 蓄力跳表现。
- 不需要配置选项，安装后自动生效。

## 技术说明

本版本在兼容层中拦截 ParCool 的 `JumpChargingAnimator`。

当检测到玩家主手持有 TaCZ 枪械时，会跳过该 animator 的模型姿态和旋转处理，并让它立即从当前动画流程中移除。

## 版本信息

- Mod 版本：2.1.0
- Minecraft：Forge 1.20.1
- TaCZ / Tijn's Epic Arsenal：可选兼容
