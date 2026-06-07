# Events Guide

Use `EchoNativeEventService` for addon-visible event publication and subscription.

## Subscribe

`subscribe` registers interest in an event target. The target should be stable and namespaced when addon-owned.

```java
events.subscribe(env.mutation("events", "subscribe", "exampleaddon:machine_tick"));
```

## Publish

`publish` emits an event through the active runtime host. A publish receipt proves the host accepted the event, not that every listener mutated state.

```java
events.publish(env.mutation("events", "publish", "exampleaddon:machine_tick"));
```

## Collision Rules

Runtime hosts should reject duplicate subscription identities when the duplicate would register the same module, surface, and target twice. Event target naming should be deterministic so diagnostics can identify collisions.

