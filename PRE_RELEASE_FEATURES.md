# Epic ParCool: Momentum 功能介绍

Epic ParCool: Momentum 是一个面向 Minecraft Forge 1.20.1 的兼容模组，用于改善 ParCool、Epic Fight、EpicParCool，以及可选的 Weapons of Miracles 和部分 Epic Fight 附属模组之间的动作、技能和攻击衔接体验。

它主要解决 ParCool 动作与 WOM 技能动画、Epic Fight 战斗状态之间的冲突，让跑酷动作、幻影跃升、空中攻击和疾跑动作能够更自然地连接。

## 主要功能

### Phantom Ascent 动作衔接

本模组允许部分 ParCool 动作衔接 Epic Fight 的 Phantom Ascent；安装 Weapons of Miracles 后，也会启用 WOM 相关动作衔接。

支持的触发来源包括：

- ParCool Cat Leap
- ParCool Wall Jump
- WOM Spider Techniques wall jump（需要安装 Weapons of Miracles）

完成这些动作后，玩家可以在空中按跳跃键触发 Phantom Ascent。（仅限一次）


### Phantom Ascent 空中攻击

原版兼容环境下，Phantom Ascent 后无法进行空中攻击+。

本模组为 Phantom Ascent 添加了空中攻击窗口，使其可以更稳定地衔接 Epic Fight 基础攻击和部分附属模组的空中攻击逻辑，同时尽量保留正常地面攻击行为。

已兼容的攻击条件包括：

- Epic Fight 基础攻击
- Weapons of Miracles 相关攻击（可选）
- Epic Fight Invincible 风格的空中攻击条件（可选）
- EpicFight Nightfall 风格的空中攻击条件（可选）

### Natural Sprinter 支持

安装 Weapons of Miracles 并学习 WOM Natural Sprinter 后，ParCool 的 FastRun、Cat Leap 等动作会使用更合适的 WOM 疾跑、冲刺和跳跃动画。

包括：

- FastRun 时切换 WOM Natural Sprinter 疾跑动画
- 根据武器类型选择持械/空手疾跑动画
- Cat Leap 时接入 Natural Sprinter 跳跃动画
- 进入 sprint 时可自动触发一次 Natural Sprinter 冲刺动作

相关行为可通过配置调整。

### Vault 疾跑保持

ParCool Vault 后，FastRun 容易被中断，导致翻越动作结束后速度衔接不自然。

本模组会在 Vault 前记录玩家的 FastRun 状态，并在 Vault 过程中尽量保持 sprint/FastRun。

### Vault 高度调节

本模组添加了 ParCool Vault 高度乘数配置：

vaultHeightScale

默认值为： 1.5

范围为： 0.86 ~ 2.0

ParCool 原版数值为 `0.86`。

本模组默认使用 `1.5`，用于让跳跃后的三格高空中撑越更加稳定。

数值越低，越需要精准掌握跳跃和撑越时机。数值越高，越容易检测到较高障碍，让撑越触发更宽松。

### Spider Techniques 兼容

安装 Weapons of Miracles 并学习 WOM Spider Techniques 后，ParCool 的 Vertical Wall Run 可能与 WOM 墙面动作动画产生冲突。

本模组提供配置项，可在玩家学习 Spider Techniques 后禁用 ParCool Vertical Wall Run，从而避免两个动作系统争抢墙面动作表现。

## 配置项

### Natural Sprinter

#### autoFastRunDash

是否在进入 EpicParCool FastRun 时自动触发 Natural Sprinter 冲刺。

- `true`：进入 FastRun 时自动触发
- `false`：只在按下 ParCool FastRun 键时触发

### Phantom Ascent

#### catLeapPrimesPhantomAscent

Cat Leap 后是否允许触发 Phantom Ascent。

#### wallJumpPrimesPhantomAscent

ParCool Wall Jump 后是否允许触发 Phantom Ascent。

#### spiderWallJumpPrimesPhantomAscent

Spider Techniques wall jump 后是否允许触发 Phantom Ascent。

### Spider Techniques

#### disableVerticalWallRunWithSpiderTechniques

学习 WOM Spider Techniques 后，是否禁用 ParCool Vertical Wall Run。

### Vault

#### vaultHeightScale

ParCool Vault 最高障碍检测高度乘数。

- ParCool 原版：`0.86`
- 本模组默认：`1.5`
- 允许范围：`0.86 ~ 2.0`

用于调整空中撑越较高障碍时的稳定性。

## 兼容目标

本模组主要面向以下模组：

- ParCool（必需）
- Epic Fight（必需）
- EpicParCool（必需）
- Weapons of Miracles（可选兼容）
- Epic Fight Invincible（可选兼容）
- EpicFight Nightfall（可选兼容）
- Epic Fight Avalon（可选兼容）
- EpicFight Skill ExtraSlots（可选兼容）


## 注意事项

- 本模组为 Forge 1.20.1 项目。
- 当前主要目标是改善动作兼容性，不添加新的武器、方块或实体。
- Vault 高度乘数过高可能让撑越触发更宽松，建议根据服务器或整合包手感调整。
- 如果配置修改后没有立即生效，建议重启客户端或重新进入世界。
