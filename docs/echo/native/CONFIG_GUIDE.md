# Config Guide

Use `EchoNativeConfigService` for config schema registration, writes, and reloads.

## Operations

- `register` declares the config schema.
- `write` persists a typed config value or section.
- `reload` asks the host to re-read config state and publish resulting changes.

## Rules

- Config IDs are module-local targets such as `client`, `server`, or `common`.
- Config writes must return a receipt.
- Config reload should not claim mutation unless the host changed active runtime state or persisted new data.
- Schema maps and evidence are diagnostics; the typed service call and receipt are the contract.

