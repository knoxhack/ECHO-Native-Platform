# Public Contract And Versioning

This document locks the Native SDK package boundary for addon authors and runtime implementors.

## Artifacts

| Artifact | Role | Addon dependency |
| --- | --- | --- |
| `echo-native-contracts` | Loader/runtime SPI, typed host services, mutation receipts, runtime lanes, descriptors | Allowed |
| `echoaddonapi` | Addon author API: addon identity, lifecycle, registries, commands, events, diagnostics, recipes, platform capabilities | Allowed |
| `echoadaptercore` | Feature/runtime bridge API: gameplay contracts and runtime host bridge surfaces | Allowed when the addon contributes runtime behavior |
| `echo-native-testkit` | Test-only fake hosts, parity assertions, module fixtures | Allowed in test/QA source sets only |
| `echo-native-loader` | Native Loader implementation | Not allowed as an addon dependency |

## Stability Markers

Native SDK types use `@EchoNativeApiStatus` with `EchoNativeApiStability`.

| Marker | Meaning |
| --- | --- |
| `STABLE` | Supported public API. Breaking changes require a major SDK bump or compatibility adapter. |
| `BETA` | Public API intended for addon use, but still allowed to evolve before the stable SDK release. |
| `INTERNAL` | Runtime implementation or diagnostic detail. Addons must not compile against it. |
| `TEST_ONLY` | Testkit, fixtures, assertions, or smoke helpers. Production addons must not ship against it. |
| `DEPRECATED` | Supported only for migration. New addons must avoid it. |

## Versioning Rules

- Addons depend on API artifacts, not implementation artifacts.
- Minor SDK versions may add new typed services, methods, receipt evidence keys, and optional capabilities.
- Minor SDK versions must not remove `STABLE` types or change their meaning.
- `BETA` types may change shape, but releases should provide migration notes.
- `INTERNAL` and `TEST_ONLY` types do not carry compatibility guarantees.
- `echo-native-loader` can change implementation classes without addon compatibility promises.

## Map Boundary

`Map<String, Object>` is allowed only for descriptor compatibility, diagnostics, report evidence, and structured receipt evidence. New runtime behavior must be represented by typed services such as `EchoNativeRegistryService`, `EchoNativeEventService`, `EchoNativeNetworkService`, and `EchoNativeMutationLedger`.

## Mutation Rule

A module is considered `MUTATED` only when a typed host service returns an `EchoNativeMutationReceipt` with status `MUTATED`. Descriptor metadata, legacy `activateNative(Map)` claims, and diagnostic maps are not enough.

