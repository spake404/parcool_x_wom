# Epic ParCool: Momentum 功能介绍

Epic ParCool: Momentum 是一个面向 Minecraft Forge 1.20.1 的动作兼容模组。

它主要用于改善 ParCool、Epic Fight、EpicParCool、Weapons of Miracles、TaCZ(需要Tijōn's Epic Arsenal) 以及部分 Epic Fight 附属之间的动作衔接，让跑酷、开火、换弹、空中攻击、幻影跃升和爬墙动作之间的体验更加自然。


## 主要功能

### Natural Sprinter 兼容

安装 Weapons of Miracles 并学习 Natural Sprinter 后，本模组会让 Natural Sprinter 更好地融入 EpicParCool 的 FastRun 和相关动作。
支持内容包括：
- EpicParCool FastRun 可使用 Natural Sprinter 疾跑动画。
- 可以开启或关闭 Natural Sprinter 动画表现。
- 可以按 R 主动触发 Natural Sprinter 跨步。

如果你不喜欢 Natural Sprinter 的动画，也可以关闭 `Natural Sprinter Animation 动画`。关闭后，兼容逻辑仍然保留，但动画表现会回到普通跑步、普通跳跃或 EpicParCool 原本动作。

### TaCZ 枪械兼容(依赖Tijōn's Epic Arsenal)

本模组优化了 TaCZ 枪械和 ParCool 疾跑动作之间的冲突。
额外兼容内容包括：
- 疾跑中长按左键可以打断跑步并立刻开火。
- 开火前如果处于 ParCool FastRun，开火结束后会恢复 FastRun。
- ParCool 蹬墙跳可以进行开火。

TaCZ 枪械还可以按枪种配置疾跑动画。默认情况下，手枪使用空手疾跑动画，其他枪种使用持武器疾跑动画。

### Phantom Ascent 衔接

本模组允许部分 ParCool 动作后接 Epic Fight 的 Phantom Ascent。
可支持的触发来源包括：
- CatLeap
- WallJump
- Spider Techniques 墙跳

触发后，玩家可以在短时间窗口内接 Phantom Ascent。相关功能都可以通过配置开启或关闭。
Phantom Ascent 的摔落保护伤害阈值也可以配置。

### 空中攻击衔接

让原本不能进行空中攻击的动作支持空中攻击的动作：
ParCool WallJump 后可以接 Epic Fight 空中攻击。
CatLeap 后可以接 Epic Fight 空中攻击。
Spider Techniques 墙跳 后可以接 Epic Fight 空中攻击。

这个功能用于改善移动动作后的战斗衔接，让玩家可以更自然地从跑酷动作进入攻击动作。

### ClingToCliff / ClimbUp 优化

EpicParCool 的ClimbUp跳跃高度比parcool的要低，横向移动也更短，ClingToCliff横移时也无法进行ClimbUp。

本模组优化后，让玩家在攀附、横移、拐角移动后更稳定地爬上去。
ClingToCliff横移时可以进行ClimbUp了。
同时，本模组也为 EpicParCool ClimbUp 增加了可配置的速度补偿：
- 垂直速度补偿：让爬升高度更接近 ParCool 原版表现。
- 横向空中控制补偿：让 ClimbUp 期间的左右移动距离更自然。

这些配置位于 `Climb` 分类中。

### WallJump 优化

本模组改善了 ParCool WallJump 与 Epic Fight、TaCZ、Phantom Ascent 之间的衔接。

支持内容包括：
- WallJump 后可接 Phantom Ascent。
- WallJump 后可接 Epic Fight 空中攻击。
- TaCZ 开火可打断 WallJump。
- WallJump 后可短时间自动恢复 sprint / FastRun。
- WallJump 空中攻击后提供可配置的摔落保护阈值。

### Vault 优化

本模组提供 Vault 高度倍率配置。

默认值比 ParCool 原版更高，可以让三格空气撑越更稳定。

如果你觉得撑越太宽松，可以调低这个数值；如果希望空中撑越更容易触发，可以适当调高。

### Spider Techniques 兼容

安装 Weapons of Miracles 并学习 Spider Techniques 后，本模组可以减少它和 ParCool 墙面动作之间的冲突。

支持内容包括：
- 可配置为学习 Spider Techniques 后禁用 ParCool 垂直蹬墙跑。
- Spider Techniques 墙跳后可以配置是否触发 Phantom Ascent。
- 提供调试配置，用于排查 Spider Techniques 和攻击状态之间的冲突。

### ParCool Dodge 禁用

安装本模组后，ParCool 自带 Dodge 会被强制禁用。

原因是 Epic Fight 本身已经有更适合战斗系统的闪避技能，保留 ParCool Dodge 容易导致动作和战斗状态冲突。

实际表现：
- ParCool Dodge 无法启动。
- Dodge 快捷键不会出现在快捷键列表中。
- ParCool Alt+P 界面中的 Dodge 会变成灰色禁用状态。
- Dodge 旁边会显示禁用提示。

## 配置选项分类

### Natural Sprinter

用于控制 Natural Sprinter 动画、主动跨步、自动跨步，以及 TaCZ 枪种动画分类：
- `Natural Sprinter Animation 动画`
- `Natural Sprinter R 键主动跨步`
- `TaCZ 使用空手疾跑的枪种`
- `自动触发疾跑冲刺`

### Phantom Ascent

用于控制哪些动作可以触发 Phantom Ascent，以及摔落保护阈值。

### WallJump

用于控制 WallJump 后的 sprint 恢复、空中攻击、TaCZ 开火打断，以及相关摔落保护。

### Spider Techniques

用于控制 Spider Techniques 和 ParCool 墙面动作之间的兼容行为。

### Vault

用于调整 ParCool Vault 的高度检测倍率。

### Climb

用于调整 EpicParCool ClimbUp 的垂直速度和横向空中控制。

## 注意事项
- 部分功能需要安装 Weapons of Miracles 或 TaCZ(需要Tijōn's Epic Arsenal) 后才会生效。
- 如果修改配置后没有立刻生效，建议重新进入世界或重启客户端。


  