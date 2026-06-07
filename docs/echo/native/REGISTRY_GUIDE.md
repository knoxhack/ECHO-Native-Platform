# Registry Guide

Use `EchoNativeRegistryService` for content IDs and registry-like declarations.

## Surfaces

- `register` for immediate content registration.
- `deferredRegister` for content that is declared early and applied by the host at the correct lifecycle point.
- `snapshot` for diagnostics and parity assertions.

## Content IDs

Content IDs must be deterministic and namespaced:

```text
moduleid:path_name
```

The host must reject duplicate IDs for non-mergeable content. Mergeable tag overlays should be modeled as tags or resource/data entries, not hidden duplicate registry registrations.

## Receipt Evidence

Useful receipt evidence keys:

- `registry`
- `kind`
- `source`
- `blockstate`
- `model`
- `texture`
- `lang`
- `inputs`
- `outputs`
- `entries`

Evidence remains diagnostic. The receipt status is authoritative.

