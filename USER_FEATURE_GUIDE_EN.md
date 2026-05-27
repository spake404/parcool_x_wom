# Epic ParCool: Momentum Feature Guide

Epic ParCool: Momentum is an action compatibility mod for Minecraft Forge 1.20.1.

It is mainly designed to improve action transitions between ParCool, Epic Fight, EpicParCool, Weapons of Miracles, TaCZ (requires Tijn's Epic Arsenal), and some Epic Fight add-ons, making parkour, shooting, reloading, aerial attacks, Phantom Ascent, and wall-climbing actions feel more natural.


## Main Features

### Natural Sprinter Compatibility

After installing Weapons of Miracles and learning Natural Sprinter, this mod makes Natural Sprinter fit better with EpicParCool FastRun and related actions.

Supported features include:
- EpicParCool FastRun can use the Natural Sprinter sprint animation.
- Natural Sprinter animation visuals can be enabled or disabled.
- Press R to manually trigger a Natural Sprinter dash step.

If you do not like the Natural Sprinter animations, you can disable `Natural Sprinter Animation`. After disabling it, the compatibility logic will still remain, but the visual animations will return to normal running, normal jumping, or the original EpicParCool actions.

### TaCZ Gun Compatibility (Requires Tijn's Epic Arsenal)

This mod improves conflicts between TaCZ guns and ParCool sprint actions.

Additional compatibility features include:
- While sprinting, holding left click can interrupt the sprint and fire immediately.
- If the player was in ParCool FastRun before firing, FastRun will be restored after firing ends.
- ParCool WallJump can be interrupted by shooting.

TaCZ guns can also use sprint animations based on gun type. By default, pistols use the barehand sprint animation, while other gun types use the weapon-holding sprint animation.

### Phantom Ascent Transitions

This mod allows Epic Fight's Phantom Ascent to be chained after certain ParCool actions.

Supported trigger sources include:
- CatLeap
- WallJump
- Spider Techniques wall jump

After triggering one of these actions, the player can chain into Phantom Ascent within a short time window. These features can all be enabled or disabled through the config.

The fall-protection damage threshold for Phantom Ascent can also be configured.

### Aerial Attack Transitions

This mod allows actions that originally could not transition into aerial attacks to chain into Epic Fight aerial attacks:

ParCool WallJump can chain into Epic Fight aerial attacks.

CatLeap can chain into Epic Fight aerial attacks.

Spider Techniques wall jump can chain into Epic Fight aerial attacks.

This feature improves combat flow after movement actions, allowing players to transition more naturally from parkour actions into attacks.

### ClingToCliff / ClimbUp Improvements

EpicParCool's ClimbUp has a lower jump height and shorter sideways movement than the original ParCool version, and ClimbUp cannot normally be triggered while moving sideways during ClingToCliff.

After this mod's improvements, players can climb up more reliably after clinging, sideways movement, or corner movement.

ClimbUp can now be triggered while moving sideways during ClingToCliff.

This mod also adds configurable speed compensation for EpicParCool ClimbUp:
- Vertical speed compensation: makes the climb height closer to the original ParCool behavior.
- Lateral air-control compensation: makes left and right movement during ClimbUp feel more natural.

These options are located in the `Climb` category.

### WallJump Improvements

This mod improves transitions between ParCool WallJump, Epic Fight, TaCZ, and Phantom Ascent.

Supported features include:
- WallJump can chain into Phantom Ascent.
- WallJump can chain into Epic Fight aerial attacks.
- TaCZ shooting can interrupt WallJump.
- Sprint / FastRun can be restored for a short time after WallJump.
- Configurable fall-protection threshold after WallJump aerial attacks.

### Vault Improvements

This mod provides a Vault height multiplier config.

The default value is higher than the original ParCool value, making three-block-air Vaults more stable.

If Vault feels too forgiving, you can lower this value. If you want aerial Vaults to trigger more easily, you can raise it slightly.

### Spider Techniques Compatibility

After installing Weapons of Miracles and learning Spider Techniques, this mod can reduce conflicts between Spider Techniques and ParCool wall actions.

Supported features include:
- ParCool vertical wall run can be disabled after learning Spider Techniques.
- Spider Techniques wall jump can be configured to trigger Phantom Ascent.
- Debug configs are available for checking conflicts between Spider Techniques and attack states.

### ParCool Dodge Disabled

After installing this mod, ParCool's built-in Dodge will be forcibly disabled.

This is because Epic Fight already provides dodge skills that fit its combat system better, while keeping ParCool Dodge enabled can easily cause conflicts between movement and combat states.

Actual behavior:
- ParCool Dodge cannot start.
- The Dodge key binding will not appear in the key binding list.
- Dodge in the ParCool Alt+P screen will appear greyed out and disabled.
- A disabled notice will be displayed next to Dodge.

## Config Categories

### Natural Sprinter

Controls Natural Sprinter animations, manual dash step, automatic dash step, and TaCZ gun-type animation categories:
- `Natural Sprinter Animation`
- `Natural Sprinter R Key Manual Step`
- `TaCZ Gun Types Using Barehand Sprint`
- `Auto FastRun Dash`

### Phantom Ascent

Controls which actions can trigger Phantom Ascent and the fall-protection threshold.

### WallJump

Controls sprint restoration after WallJump, aerial attacks, TaCZ shooting interruption, and related fall protection.

### Spider Techniques

Controls compatibility behavior between Spider Techniques and ParCool wall actions.

### Vault

Adjusts the height detection multiplier for ParCool Vault.

### Climb

Adjusts vertical speed and lateral air control for EpicParCool ClimbUp.

## Notes

- Some features only take effect after installing Weapons of Miracles or TaCZ (requires Tijn's Epic Arsenal).
- If config changes do not take effect immediately, it is recommended to re-enter the world or restart the client.
