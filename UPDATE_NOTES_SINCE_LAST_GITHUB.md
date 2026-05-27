# Epic ParCool: Momentum 更新公告

本次更新主要优化了 Natural Sprinter、TaCZ 枪械、ParCool 动作衔接，以及 Epic Fight 相关动画表现。整体目标是让疾跑、开火、换弹、蹬墙跳、猫挂和爬升之间的衔接更加自然。

## 重点更新

### Natural Sprinter 动画开关

新增 `Natural Sprinter Animation 动画` 配置项。

开启后，将继续使用 Natural Sprinter 的疾跑、跨步和跳跃相关动画。

关闭后，Natural Sprinter 的兼容逻辑仍然保留，但动画表现会回到普通跑步、普通跳跃或 EpicParCool 原本动画。这个选项适合不想使用 Natural Sprinter 动画，但仍想保留动作兼容修复的玩家。

### R 键主动跨步

新增 `Natural Sprinter R 键主动跨步` 配置项。

学会 Natural Sprinter 后，在 Natural Sprinter 疾跑状态下可以按 R 主动触发跨步。该跨步仍然会消耗 Natural Sprinter 原本的精力。

如果手持 TaCZ 枪械并正在换弹，R 键会优先执行换弹，不会触发跨步。

### TaCZ 枪械疾跑动画分类

TaCZ 枪械现在可以按照枪种选择疾跑动画。

默认情况下：

- 手枪使用空手疾跑动画。
- 其他枪种使用持武器疾跑动画。

整合包作者可以通过配置自行调整哪些枪种使用空手疾跑动画。

## TaCZ 兼容优化

- 修复安装 ParCool 后，疾跑中长按左键只能打断跑步、不能立刻射击的问题。
- 修复连发枪在疾跑后接开火时，容易射击几发后又被疾跑打断的问题。
- 如果开火前处于 ParCool FastRun，开火结束后会尝试恢复 FastRun。
- TaCZ 开火现在可以打断 ParCool 蹬墙跳。
- TaCZ 换弹、射击结束后的短时间内不会触发 Natural Sprinter 自动跨步，减少多余的跨步动画。
- 猫挂期间禁止 TaCZ 开火，避免动作状态冲突。

## ParCool 动作衔接优化

- WallJump 后可以更稳定地接 Epic Fight 空中攻击。
- CatLeap、WallJump、Spider Techniques 墙跳后，可以配置是否触发 Phantom Ascent。
- Phantom Ascent 的摔落保护伤害阈值现在可以配置。
- EpicParCool 的 ClingToCliff 横移期间，现在可以更稳定地接 ClimbUp。
- EpicParCool ClimbUp 增加了垂直速度和横向空中移动补偿，让爬升衔接更接近 ParCool 原版。
- 触发 Breakfall 时，Natural Sprinter 跨步会延迟到 Breakfall 动作结束后再播放。

## ParCool Dodge 调整

安装本模组后，ParCool 自带 Dodge 会被强制禁用。

- Dodge 不再注册到快捷键列表。
- ParCool 的 Alt+P 设置界面中，Dodge 会显示为灰色禁用状态。
- Dodge 选项会显示提示：已禁用，也许使用 Epic Fight 的闪避技能是更好的选择。

## Spider Techniques

- 新增配置：学会 Spider Techniques 后，可以禁用 ParCool 的垂直蹬墙跑。
- Spider Techniques 墙跳可以配置是否触发 Phantom Ascent。

## Vault

- 新增 Vault 高度倍率配置。
- 默认提高撑越检测高度，让三格空气撑越更稳定。

## 配置整理

- 新增和完善中英文配置文本。
- ClimbUp 相关配置现在整理到 `Climb` 分类。
- `Natural Sprinter Animation 动画` 会在配置界面中排在 Natural Sprinter 相关选项前面。

## 已知说明

如果切换语言时日志中出现以下报错：

```text
epic_fight_avalon:item_skins/merlin_gg.json
EOFException: End of input
```

这是 Epic Fight Avalon 中的空 JSON 文件导致的资源重载报错，不是本模组的问题。
