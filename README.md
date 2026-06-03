# Epic ParCool: Momentum

Epic ParCool: Momentum is an action-flow compatibility mod for Minecraft Forge 1.20.1.

It improves the way ParCool, Epic Fight, EpicParCool, Weapons of Miracles, TaCZ, Tijn's Epic Arsenal, and related Epic Fight add-ons work together. The goal is to make sprinting, parkour, climbing, shooting, reloading, aerial attacks, Phantom Ascent, and wall actions transition more naturally without forcing every optional add-on to be installed.

## Dependencies

Required:

- Minecraft Forge 1.20.1
- Epic Fight
- ParCool
- EpicParCool

Optional compatibility:

- Weapons of Miracles
- TaCZ
- Tijn's Epic Arsenal
- Epic Fight Invincible
- EpicFight Nightfall
- Epic Fight Avalon
- EpicFight Skill ExtraSlots
- Epic Fight Extra

## Features

| Feature | Description |
| --- | --- |
| Natural Sprinter compatibility | Integrates WOM Natural Sprinter with EpicParCool FastRun, supports optional animation replacement, TaCZ gun-type sprint animation rules, and an optional R-key manual dash step. |
| TaCZ compatibility | Lets TaCZ shooting interrupt sprint and WallJump, restores FastRun after shooting when appropriate, and avoids unwanted Natural Sprinter dash steps around reload/shoot timing. |
| Phantom Ascent follow-ups | Allows CatLeap, WallJump, and Spider Techniques wall jump to open a short Phantom Ascent follow-up window. |
| Aerial attack transitions | Lets CatLeap, WallJump, Spider Techniques wall jump, and Phantom Ascent flow into Epic Fight aerial attacks while preserving normal ground attack behavior. |
| ClingToCliff / ClimbUp improvements | Makes EpicParCool ClimbUp more reliable after cling movement and corner movement, with configurable vertical and lateral compensation. |
| WallJump improvements | Adds configurable sprint restoration, TaCZ shooting interruption, aerial attack windows, Phantom Ascent chaining, and fall-protection thresholds. |
| Vault tuning | Adds a configurable ParCool Vault height scale for more stable three-block-air vaults. |
| Spider Techniques compatibility | Reduces conflicts between WOM Spider Techniques and ParCool wall actions, including optional ParCool vertical wall-run disabling. |
| ParCool Dodge handling | Forcibly disables ParCool's built-in Dodge and hides its key binding so Epic Fight dodge skills can own combat dodging. |
| Runtime performance | Keeps compatibility checks scoped to active states and avoids unnecessary per-tick work where possible. |

## Changelog

### 3.2.1

- Stabilized `spiderTechniquesWallRunMode=WOM` so WOM keeps its original Spider Techniques wall-run ownership while ParCool side wall-run, WallSlide, and their key conflicts are suppressed after learning the skill.
- Added camera-guided 90-degree wall transfer for WOM Spider Techniques side wall-run without letting camera yaw reverse the actual A/D run direction.
- Fixed side wall-run direction so `A` and `D` are resolved from the active wall face instead of camera-facing direction.
- Restored reliable WOM wall jump and ParCool WallJump bridge behavior from WOM side wall-run, including animation cleanup after jump and landing.
- Matched WOM wall-glide hand particles in ParCool replacement mode and kept slow wall-glide behavior compatible with `Ctrl+Shift`.
- Split WOM wall-run mode gates, wall contact detection, corner transfer, yaw lock, diagnostics, and hook dispatch into focused helper classes.

### 3.2.0

- Improved ParCool wall-run mode for WOM Spider Techniques.
- Implemented `spiderTechniquesWallRunMode=WOM`, where WOM's original sprint input owns Spider Techniques wall running while ParCool HorizontalWallRun and its key input are disabled after learning the skill.
- Added WOM-style wall-glide support through both the ParCool horizontal wall-run key and the original ParCool WallSlide key.
- Kept `W+R` reserved for wall running while allowing `R` without forward input to trigger wall-glide.
- Added slow wall-glide support through `Shift+R` and `Shift+WallSlide`.
- Fixed stale WOM wall-run/wall-glide state after landing or leaving a wall.
- Reduced unnecessary Spider Techniques state sync and moved temporary diagnostic logging out of normal info logs.

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
