# ParCool x WOM

Compatibility mod for ParCool, Epic Fight, EpicParCool, and Weapons of Miracles on Minecraft Forge 1.20.1.

## Features

| Feature | Description |
| --- | --- |
| Phantom Ascent follow-ups | Allows Cat Leap, ParCool wall jump, and Spider Techniques wall jump to prime Phantom Ascent follow-ups. |
| Phantom Ascent air attacks | Lets Phantom Ascent transition into air attacks while preserving normal ground attack behavior. |
| Natural Sprinter support | Integrates WOM Natural Sprinter sprint and sprint-jump animations with ParCool actions. |
| Vault sprint compatibility | Preserves ParCool FastRun behavior through Vault with control-mode-specific handling. |
| Spider Techniques compatibility | Optionally disables ParCool vertical wall run after learning WOM Spider Techniques to avoid animation conflicts. |
| Weapon compatibility | Adds handling for Epic Fight, WOM, Epic Fight Invincible, and EpicFight Nightfall style air-attack conditions. |
| Runtime performance | Limits per-tick compatibility checks to active states and keeps temporary diagnostics out of normal runtime paths. |

## Changelog

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
