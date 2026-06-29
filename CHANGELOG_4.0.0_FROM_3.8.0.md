# Epic ParCool: Momentum 4.0.0 Changelog From 3.8.0

This changelog focuses on what players and pack makers can actually use in 4.0.0.

## 中文

### 更新重点

4.0.0 的核心更新是：FastRun 不再只是依赖 WOM Natural Sprinter 的附属功能，而是升级为本模组自己的“自定义 FastRun 动画系统”。

现在即使没有安装 Weapons of Miracles，玩家也可以让不同武器拥有更合适的 FastRun 动作；整合包作者也可以通过数据包为武器类型、具体武器和不同持刀姿态配置 FastRun 动画。

### 自定义 FastRun 动画

- 新增“启用自定义 FastRun / Custom FastRun Animations”功能。
- 开启后，本模组会接管 EpicParCool FastRun 的动画表现，并启用相关的跨步、跳跃视觉效果。
- 关闭后，会回到 EpicParCool 默认 FastRun 行为，R 键 FastRun 跨步效果也会关闭。
- 该选项已加入 ParCool `Alt+P` 设置界面。

### 无 WOM 也能使用武器 FastRun

- 未安装 WOM 时，FastRun 仍然可以使用本模组的自定义动画系统。
- 如果某把武器没有数据包配置，且开启“是否根据当前武器自动生成 FastRun 动画”，本模组会根据当前武器原本的 Epic Fight 奔跑动作生成 FastRun 姿态。
- 这样不同武器不再全部回到 EpicParCool 默认 FastRun 动作，重武器、长柄武器、双手武器会更接近自己的原始跑姿。
- 该选项默认开启，也已加入 ParCool `Alt+P` 设置界面。

### 数据包自定义武器 FastRun

- 整合包作者可以通过数据包为不同武器或武器类型指定 FastRun 动画。
- 支持按 Epic Fight weapon type 匹配，例如剑、斧、长枪、大剑、太刀等。
- 支持按具体武器匹配，适合给特殊武器单独设置动作。
- 支持按 weapon style 匹配，例如同一把武器在收刀、双手、特殊姿态下使用不同奔跑动作。
- 如果第一个动画不可用，系统会继续尝试后面的动画项；这可以用来写“优先使用 WOM 动画，没有 WOM 时使用 Epic Fight 动画”的规则。

### 新增默认 FastRun 数据包

- 内置更新了 Epic Fight 默认武器类型的 FastRun 规则。
- 新增 EpicFight Extra 默认模板，覆盖 `great_tachi`、`katana`、`modao`、`scythe`、`yoto` 等武器类型。
- 新增 EpicFight Nightfall 默认模板，覆盖 scythe、broadblade、aetherialdusk、crescentmoon、beastclaw、bloodlust、hf_blade、meenlance、pioneer、yamato 等武器类型。
- 新增 WOM 默认模板，覆盖 agony、antitheus、blackstar、clawed gauntle、ender blaster、evil tachi、herrscher、moonless、nova、satsujin、solar、torment 等武器类型。
- 对 uchigatana 这类存在多种奔跑姿态的武器，默认规则现在会按 style 选择正确动作。

### FastRun 跨步效果

- R 键 FastRun 跨步现在不再强制要求 WOM Natural Sprinter。
- 有 WOM Natural Sprinter 时，仍然使用 WOM 自己的 Step 资源。
- 没有 WOM 时，会使用本模组的通用消耗和冷却规则。
- 数据包可以调整跨步视觉强度、持续时间和向前推动感，方便不同武器做出更轻或更重的跨步表现。

### 武器切换稳定性

- 修复切换武器后 FastRun 偶尔继续播放上一把武器动作的问题。
- 修复切换到空手后仍残留上一把武器 FastRun 动作的问题。
- 修复某些武器在不同 style 之间切换时，下半身动作不更新或姿态不匹配的问题。
- 无 WOM 模式下，如果需要读取当前武器原本的奔跑动作，系统会等待当前武器动作真正刷新后再生成 FastRun，减少旧动作串到新武器上的情况。

### 调试工具

- 新增 Epic Fight 动画 HUD 调试功能。
- 开启后，物品栏上方会显示当前正在播放的 Epic Fight 动画 ID。
- 开启该 HUD 时，小键盘 0 可以把当前动画 ID 复制到剪贴板。
- 关闭该 HUD 时，小键盘 0 不会生效，也不会出现在 Minecraft 按键绑定列表里。

### 配置名称变化

- `naturalSprinterAnimations` 改名为 `customFastRunAnimations`。
- `noWomProceduralWeaponFastRun` 改名为 `autoGenerateFastRunFromCurrentWeapon`。
- Forge 配置说明已改为英文，游戏内设置界面保留中英文翻译。

### 适合谁使用

- 玩家：可以直接获得更自然的武器 FastRun 动作，尤其是在没有 WOM 的环境下。
- 整合包作者：可以用数据包给不同武器做专属 FastRun 动作。
- 模组包维护者：可以为 Epic Fight、WOM、EpicFight Extra、EpicFight Nightfall 的武器类型准备统一模板。
- 调试人员：可以直接看到当前 Epic Fight 动画 ID，排查武器动作和数据包匹配问题更方便。

## English

### Highlights

The main 4.0.0 change is that FastRun is no longer only a WOM Natural Sprinter add-on. It is now this mod's own custom FastRun animation system.

Even without Weapons of Miracles installed, weapons can now have better FastRun visuals. Pack makers can use datapacks to configure FastRun animations for weapon types, individual weapons, and different Epic Fight weapon styles.

### Custom FastRun Animations

- Added the `Custom FastRun Animations` feature.
- When enabled, this mod controls EpicParCool FastRun animation replacement and related step/jump visuals.
- When disabled, EpicParCool's default FastRun behavior is preserved and the R-key FastRun step effect is disabled.
- This option is available in the ParCool `Alt+P` settings screen.

### Weapon FastRun Without WOM

- Custom FastRun now works even when WOM is not installed.
- If a weapon has no datapack rule and `Auto-generate FastRun from Current Weapon` is enabled, this mod generates FastRun from the weapon's current Epic Fight RUN animation.
- This prevents unmatched weapons from all falling back to EpicParCool's default FastRun animation.
- Heavy weapons, polearms, two-handed weapons, and other weapon types can keep a FastRun pose closer to their original run animation.
- This option is enabled by default and is available in the ParCool `Alt+P` settings screen.

### Datapack-Based FastRun Customization

- Pack makers can define FastRun animations through datapacks.
- Rules can match Epic Fight weapon types, such as sword, axe, spear, greatsword, and tachi.
- Rules can also match individual weapons for special cases.
- Rules can match Epic Fight weapon styles, allowing one weapon to use different FastRun animations in sheathed, two-handed, or other style states.
- Animation entries can be ordered by priority. If a higher-priority animation is unavailable, the system can try the next one. This makes it possible to prefer WOM animations when WOM is installed and fall back to Epic Fight animations when it is not.

### Built-In FastRun Datapacks

- Updated the built-in Epic Fight weapon-type FastRun rules.
- Added EpicFight Extra templates for weapon types such as `great_tachi`, `katana`, `modao`, `scythe`, and `yoto`.
- Added EpicFight Nightfall templates for weapon types such as scythe, broadblade, aetherialdusk, crescentmoon, beastclaw, bloodlust, hf_blade, meenlance, pioneer, and yamato.
- Added WOM templates for weapon types such as agony, antitheus, blackstar, clawed gauntle, ender blaster, evil tachi, herrscher, moonless, nova, satsujin, solar, and torment.
- Weapons with multiple run poses, such as uchigatana, can now select the correct animation by style.

### FastRun Step Visuals

- R-key FastRun Step no longer strictly requires WOM Natural Sprinter.
- When WOM Natural Sprinter is available, the mod still uses WOM's own Step resource.
- Without WOM, the mod uses its own generic cost and cooldown rule.
- Datapacks can tune step strength, duration, and forward push so different weapons can feel lighter or heavier during FastRun steps.

### Weapon-Switch Stability

- Fixed cases where FastRun kept playing the previous weapon's animation after switching weapons.
- Fixed cases where switching to barehand could keep the previous weapon's FastRun animation.
- Fixed style-sensitive weapons sometimes keeping an incorrect lower-body run pose.
- In no-WOM mode, when FastRun needs the current weapon's original run animation, the system waits until the current weapon animation has actually refreshed before generating FastRun. This reduces stale animation carry-over.

### Debug Tools

- Added an Epic Fight animation HUD debug option.
- When enabled, the current Epic Fight animation ID is shown above the hotbar.
- While the HUD is enabled, Numpad 0 copies the current animation ID to the clipboard.
- When the HUD is disabled, the Numpad 0 shortcut is inactive and is not shown in Minecraft's key bindings screen.

### Config Renames

- `naturalSprinterAnimations` was renamed to `customFastRunAnimations`.
- `noWomProceduralWeaponFastRun` was renamed to `autoGenerateFastRunFromCurrentWeapon`.
- Forge config comments are now English-only, while in-game settings keep English and Chinese localization.

### Who This Helps

- Players get more natural weapon FastRun animations, especially without WOM.
- Pack makers can create weapon-specific FastRun animation rules with datapacks.
- Modpack maintainers can ship templates for Epic Fight, WOM, EpicFight Extra, and EpicFight Nightfall weapons.
- Debuggers can inspect the current Epic Fight animation ID directly in game.

## Version Info

- Compared against: GitHub 3.8.0 baseline, represented locally by `origin/test/parcool-wallrun-original-adapter` (`Add no-WOM FastRun animation support`)
- Mod version: 4.0.0
- Minecraft / Forge: 1.20.1 / 47.4.20
- Main compatibility targets: Epic Fight, ParCool, EpicParCool, Weapons of Miracles, EpicFight Extra, EpicFight Nightfall, TaCZ
