# Natural Sprinter FastRun Animation Overrides

Datapacks can override Natural Sprinter FastRun animations with JSON files under:

```text
data/<namespace>/epic_parcool_momentum/natural_sprinter_fastrun/*.json
```

Item-specific rules win over Epic Fight weapon type rules. If multiple rules target the same item or type, the higher `priority` wins.

```json
{
  "enabled": true,
  "match": {
    "item": "minecraft:diamond_sword"
  },
  "run": "examplemod:biped/fastrun/diamond_sword",
  "left_step": "examplemod:biped/fastrun/diamond_sword_left_step",
  "right_step": "examplemod:biped/fastrun/diamond_sword_right_step",
  "run_pose": {
    "enabled": true,
    "scale": 0.4,
    "blend_ticks": 3.0
  },
  "fallback": "weapon",
  "priority": 0
}
```

```json
{
  "enabled": true,
  "match": {
    "type": "greatsword"
  },
  "run": "examplemod:biped/fastrun/greatsword",
  "fallback": "weapon"
}
```

`run`, `left_step`, and `right_step` are all optional individually, but at least one must be present. Missing animations fall back to the default Natural Sprinter `barehand` or `weapon` family. If `fallback` is omitted, EPM chooses the same default family the held item would normally use.

`run_pose` is optional and only affects datapack `run` animations. It adds the same Root/Torso/Chest sprint-lean pose modifier used by procedural step startup, without changing leg IK.

```json
"run_pose": {
  "enabled": true,
  "scale": 0.4,
  "blend_ticks": 3.0
}
```

Fields:

- `enabled`: enables the sustained FastRun pose layer. Default: `true`.
- `scale`: strength relative to the procedural step pose. `0.4` means 40% of step strength. Default: `0.4`.
- `blend_ticks`: smooth transition time when entering the pose. Default: `3.0`.

Set `"enabled": false` to use the custom `run` animation exactly as authored.

The mod ships disabled examples at:

```text
data/epic_parcool_momentum/epic_parcool_momentum/natural_sprinter_fastrun/example_type_greatsword.json
data/epic_parcool_momentum/epic_parcool_momentum/natural_sprinter_fastrun/example_item_diamond_sword.json
```

Change `enabled` to `true` after replacing the example animation IDs with real Epic Fight animation IDs.
