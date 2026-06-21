# Natural Sprinter FastRun Example Datapack

Copy this folder into:

```text
saves/<world>/datapacks/natural_sprinter_fastrun_effects_example
```

Then run `/reload`.

Rules go under:

```text
data/<namespace>/epic_parcool_momentum/natural_sprinter_fastrun/*.json
```

Effect behavior:

- Rules with only `run`: use procedural step. Startup step has full effects. Manual R step only has trailing POOF.
- Rules with `left_step` / `right_step`: use real step animations. Effects are controlled by the datapack:

```json
"effects": {
  "startup_step": true,
  "manual_step": false
}
```

Sustained FastRun pose can also be tuned per rule:

```json
"run_pose": {
  "enabled": true,
  "scale": 0.4,
  "blend_ticks": 3.0
}
```

`scale` is relative to the procedural step pose, and `blend_ticks` controls the short transition into that pose.

Set `enabled` to `true` after replacing the example animation ids with real Epic Fight animation ids.
