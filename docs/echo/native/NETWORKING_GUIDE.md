# Networking Guide

Use `EchoNativeNetworkService` for packet registration and delivery.

## Packet IDs

Packet IDs must be namespaced:

```text
exampleaddon:sync_machine
```

Register packets before sending or broadcasting them. Hosts should reject duplicate packet IDs.

## Operations

- `registerPacket` declares packet schema and handler ownership.
- `sendToPlayer` sends a packet to one player or connection target.
- `broadcast` sends a packet to a runtime-defined audience.

## Evidence

Use receipt evidence for schema version, payload type, direction, channel, and compatibility notes. Do not put opaque live payloads into receipt evidence.

