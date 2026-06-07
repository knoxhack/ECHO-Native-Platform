# Troubleshooting

## Native Loader Refuses Release Mode

Check for:

- Missing `META-INF/echo.mod.json`.
- Missing native entrypoint.
- `nativeClasspath` entries pointing at `build/classes`, `build/resources`, `tmp`, or absolute local paths.
- Descriptor dependency cycles.

## Addon Loads But Does Not Mutate

A module counts as mutated only when a typed host service returns a real mutation receipt. Descriptor metadata and diagnostics are not mutation proof.

## Client/Server Side Failure

Verify descriptor `side` and route registrations. Client render hooks, HUDs, keybinds, and screens must be gated through client-side bridge services.

## Packet Rejection

Check packet ids, schema versions, direction, and rate-limit policies. Serverbound action packets should validate player context before mutating state.

## Release Gate Fails

Run:

```powershell
.\gradlew.bat generatePlan3ReleasePrepReport
```

Then inspect `reports/echo/native/plan3/plan3-release-prep.md`.
