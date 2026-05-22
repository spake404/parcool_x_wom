# Epic ParCool: Momentum

Momentum-focused compatibility mod for ParCool, Epic Fight, EpicParCool, and optional Weapons of Miracles integrations on Minecraft Forge 1.20.1.

## Dependencies

Required:

- Minecraft Forge 1.20.1
- Epic Fight
- ParCool
- EpicParCool

Optional compatibility:

- Weapons of Miracles
- Epic Fight Invincible
- EpicFight Nightfall
- Epic Fight Avalon
- EpicFight Skill ExtraSlots

## Features

| Feature | Description |
| --- | --- |
| Phantom Ascent follow-ups | Allows Cat Leap and ParCool wall jump to prime Phantom Ascent follow-ups; adds Spider Techniques wall jump support when Weapons of Miracles is installed. |
| Phantom Ascent air attacks | Lets Phantom Ascent transition into air attacks while preserving normal ground attack behavior. |
| Natural Sprinter support | Integrates WOM Natural Sprinter sprint and sprint-jump animations with ParCool actions when Weapons of Miracles is installed. |
| Vault sprint compatibility | Preserves ParCool FastRun behavior through Vault with control-mode-specific handling, even without WOM Natural Sprinter. |
| Vault height tuning | Adds a configurable ParCool Vault height scale, defaulting to 1.5 for stable three-block air vaults. |
| Spider Techniques compatibility | Optionally disables ParCool vertical wall run after learning WOM Spider Techniques to avoid animation conflicts when Weapons of Miracles is installed. |
| Weapon compatibility | Adds handling for Epic Fight, optional WOM, optional Epic Fight Invincible, and optional EpicFight Nightfall style air-attack conditions. |
| Runtime performance | Limits per-tick compatibility checks to active states and keeps temporary diagnostics out of normal runtime paths. |

## Changelog

### 1.3.0

- Added automatic sprint recovery after ParCool WallJump, with an independent configuration option.
- Added WallJump air-attack support so ParCool WallJump can open a short Epic Fight air-attack window.
- Added configurable fall-damage protection thresholds for Phantom Ascent and WallJump air-attack follow-ups; both default to 2.5.
- Fixed a Phantom Ascent air-attack state leak that could allow repeated air attacks after landing.
- Improved WOM Spider Techniques compatibility by letting WOM handle its native wall-attack state while blocking ordinary BasicAttack and ComboBasicAttack during Spider wall-glide and transition windows.
- Added a temporary `debugSpiderTechniquesAttackState` config option for diagnosing Spider Techniques attack-state issues without logging during normal gameplay.
- Refined optional compatibility guards around WOM, Epic Fight Invincible, and EpicFight Nightfall paths.

### 1.2.0

- Added a configurable ParCool Vault height scale.
- Set the compatibility default Vault height scale to 1.5 for stable three-block air vaults; ParCool's original value is 0.86.
- Preserved ParCool FastRun through Vault even when the player has not learned WOM Natural Sprinter.
- Added localization for the Vault height scale configuration option.
- Made Weapons of Miracles an optional dependency and disabled WOM-specific compatibility paths when it is not installed.
- Added conditional Mixin loading for optional Epic Fight Invincible and EpicFight Nightfall compatibility.

### 1.1.1

- Reduced per-tick compatibility checks on both client and server by skipping inactive Phantom Ascent, Spider Techniques, Vault, and Natural Sprinter state handlers.
- Split Natural Sprinter FastRun animation handling out of the main Forge event class to keep event dispatch lightweight and easier to maintain.
- Removed temporary diagnostic logging that was only used while locating Phantom Ascent and Vault compatibility issues.
- Added the missing localization key for the Spider Techniques vertical wall-run config option.

### 1.1.0

- Added Phantom Ascent air-attack support after compatible ParCool and Spider Techniques movement actions.
- Fixed Phantom Ascent follow-up limits so it can only be used once before landing.
- Improved Natural Sprinter sprint, sprint-jump, and Vault compatibility.
- Added support for Epic Fight Invincible and EpicFight Nightfall style air-attack conditions.
- Added a config option to disable ParCool vertical wall run after learning WOM Spider Techniques.
- Improved compatibility safeguards to avoid ground attacks being incorrectly interrupted by air-attack windows.

### 1.0.0

- Initial compatibility scaffold for ParCool and Weapons of Miracles.
