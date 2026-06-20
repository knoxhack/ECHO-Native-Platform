package dev.echo.nativeplatform.loader;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderRegistryRuntimeSupport {
    public static final String SERVICE_ID = "echo.native.registry_runtime_support";
    private static final java.util.concurrent.atomic.AtomicBoolean FREEZE_GUARD_RUNNING =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    private NativeLoaderRegistryRuntimeSupport() {
    }

    public static int clearUnregisteredIntrusiveHolders(Object... registries) {
        int cleared = 0;
        if (registries == null) {
            return cleared;
        }
        for (Object registry : registries) {
            if (registry == null) {
                continue;
            }
            try {
                java.lang.reflect.Field field = findField(registry.getClass(), "unregisteredIntrusiveHolders");
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(registry);
                if (value instanceof Map<?, ?> holders && !holders.isEmpty()) {
                    cleared += holders.size();
                    holders.clear();
                }
            } catch (Throwable ignored) {
                // Clearing failed optional registration residue is best-effort; successful registrations stay intact.
            }
        }
        return cleared;
    }

    public static boolean unfreezeNativeRegistry(Object registry) {
        if (registry == null) {
            return false;
        }
        try {
            java.lang.reflect.Field frozen = declaredFieldInHierarchy(registry.getClass(), "frozen");
            frozen.setAccessible(true);
            boolean wasFrozen = frozen.getBoolean(registry);
            if (wasFrozen) {
                frozen.setBoolean(registry, false);
            }
            return wasFrozen;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean freezeNativeRegistry(Object registry) {
        if (registry == null) {
            return false;
        }
        try {
            registry.getClass().getMethod("freeze").invoke(registry);
            return true;
        } catch (Throwable ignored) {
            // If refreezing fails, leave Minecraft's normal freeze path to surface or repair the runtime issue.
            return false;
        }
    }

    public static boolean restoreNativeRegistryFrozenFlag(Object registry, boolean frozenValue) {
        if (registry == null) {
            return false;
        }
        try {
            java.lang.reflect.Field frozen = declaredFieldInHierarchy(registry.getClass(), "frozen");
            frozen.setAccessible(true);
            frozen.setBoolean(registry, frozenValue);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int bindRegistryHoldersWithoutFreezing(Object registry) {
        if (registry == null) {
            return 0;
        }
        Map<Object, Object> keysByHolder = new IdentityHashMap<>();
        try {
            java.lang.reflect.Field byKey = findField(registry.getClass(), "byKey");
            if (byKey != null) {
                byKey.setAccessible(true);
                Object rawByKey = byKey.get(registry);
                if (rawByKey instanceof Map<?, ?> keyMap) {
                    for (Map.Entry<?, ?> entry : keyMap.entrySet()) {
                        Object holder = entry.getValue();
                        Object key = entry.getKey();
                        if (holder != null && key != null) {
                            keysByHolder.put(holder, key);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // Some runtime registry implementations do not expose byKey; value binding below remains useful.
        }

        int bound = 0;
        try {
            java.lang.reflect.Field byValue = findField(registry.getClass(), "byValue");
            if (byValue == null) {
                return bound;
            }
            byValue.setAccessible(true);
            Object rawByValue = byValue.get(registry);
            if (!(rawByValue instanceof Map<?, ?> valueMap)) {
                return bound;
            }
            for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
                Object value = entry.getKey();
                Object holder = entry.getValue();
                if (holder == null) {
                    continue;
                }
                Object key = keysByHolder.get(holder);
                if (bindHolderReferenceField(holder, "key", key)) {
                    bound++;
                }
                if (bindHolderReferenceField(holder, "value", value)) {
                    bound++;
                }
            }
        } catch (Throwable ignored) {
            // Binding is best-effort. Minecraft will still own the canonical freeze path.
        }
        return bound;
    }

    private static boolean bindHolderReferenceField(Object holder, String fieldName, Object value) {
        if (holder == null || value == null) {
            return false;
        }
        try {
            java.lang.reflect.Field field = findField(holder.getClass(), fieldName);
            if (field == null) {
                return false;
            }
            field.setAccessible(true);
            if (field.get(holder) != null) {
                return false;
            }
            field.set(holder, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean clearNativeRegistryBoundTags(Object registry) {
        if (registry == null) {
            return false;
        }
        if (isItemRegistry(registry)) {
            return false;
        }
        try {
            java.lang.reflect.Field allTags = findField(registry.getClass(), "allTags");
            if (allTags == null) {
                return false;
            }
            allTags.setAccessible(true);
            java.lang.reflect.Method unboundFactory = allTags.getType().getDeclaredMethod("unbound");
            unboundFactory.setAccessible(true);
            Object unbound = unboundFactory.invoke(null);
            allTags.set(registry, unbound);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isItemRegistry(Object registry) {
        String keyText = registryKeyText(registry).toLowerCase(java.util.Locale.ROOT);
        return keyText.contains("minecraft:item")
                || keyText.contains(" / item")
                || keyText.endsWith("/ item]")
                || keyText.endsWith(":item]");
    }

    private static String registryKeyText(Object registry) {
        if (registry == null) {
            return "";
        }
        try {
            java.lang.reflect.Field key = findField(registry.getClass(), "key");
            if (key != null) {
                key.setAccessible(true);
                Object value = key.get(registry);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
        } catch (Throwable ignored) {
            // Fall through to toString based registry hints.
        }
        try {
            return String.valueOf(registry);
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static void enableNativeIntrusiveHolders(Object registry) {
        if (registry == null) {
            return;
        }
        try {
            java.lang.reflect.Field holders = declaredFieldInHierarchy(registry.getClass(), "unregisteredIntrusiveHolders");
            holders.setAccessible(true);
            if (holders.get(registry) == null) {
                holders.set(registry, new java.util.IdentityHashMap<>());
            }
        } catch (Throwable ignored) {
            // Registries that do not use intrusive holders do not need this bootstrap window.
        }
    }

    public static int normalizeAllBuiltInRegistries(Class<?> builtInRegistriesClass) {
        if (builtInRegistriesClass == null) {
            return 0;
        }
        int normalized = 0;
        for (java.lang.reflect.Field field : builtInRegistriesClass.getFields()) {
            try {
                int modifiers = field.getModifiers();
                if (!java.lang.reflect.Modifier.isStatic(modifiers)) {
                    continue;
                }
                Object registry = field.get(null);
                if (registry == null || findField(registry.getClass(), "frozen") == null) {
                    continue;
                }
                clearUnregisteredIntrusiveHolders(registry);
                restoreNativeRegistryFrozenFlag(registry, true);
                normalized++;
            } catch (Throwable ignored) {
                // Non-registry constants and snapshot-specific registry shapes are ignored.
            }
        }
        return normalized;
    }

    public static int prepareAllBuiltInRegistriesForMinecraftFreeze(Class<?> builtInRegistriesClass) {
        if (builtInRegistriesClass == null) {
            return 0;
        }
        int prepared = 0;
        for (java.lang.reflect.Field field : builtInRegistriesClass.getFields()) {
            try {
                int modifiers = field.getModifiers();
                if (!java.lang.reflect.Modifier.isStatic(modifiers)) {
                    continue;
                }
                Object registry = field.get(null);
                if (registry == null || findField(registry.getClass(), "frozen") == null) {
                    continue;
                }
                clearUnregisteredIntrusiveHolders(registry);
                restoreNativeRegistryFrozenFlag(registry, true);
                prepared++;
            } catch (Throwable ignored) {
                // BuiltInRegistries includes non-registry constants on some snapshots.
            }
        }
        return prepared;
    }

    public static int prepareRegistryAndContentsForMinecraftFreeze(Object registryOfRegistries) {
        if (registryOfRegistries == null) {
            return 0;
        }
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int prepared = prepareRegistryForMinecraftFreeze(registryOfRegistries, seen);
        try {
            Object iterator = registryOfRegistries.getClass().getMethod("iterator").invoke(registryOfRegistries);
            if (iterator instanceof java.util.Iterator<?> values) {
                while (values.hasNext()) {
                    prepared += prepareRegistryForMinecraftFreeze(values.next(), seen);
                }
            }
        } catch (Throwable ignored) {
            if (registryOfRegistries instanceof Iterable<?> values) {
                for (Object value : values) {
                    prepared += prepareRegistryForMinecraftFreeze(value, seen);
                }
            }
        }
        return prepared;
    }

    public static boolean startBuiltInRegistryFreezeGuard(
            Class<?> builtInRegistriesClass,
            long durationMillis,
            long intervalMillis
    ) {
        if (builtInRegistriesClass == null || !FREEZE_GUARD_RUNNING.compareAndSet(false, true)) {
            return false;
        }
        Thread thread = new Thread(
                () -> {
                    long deadline = System.currentTimeMillis() + Math.max(1L, durationMillis);
                    try {
                        while (System.currentTimeMillis() < deadline) {
                            prepareAllBuiltInRegistriesForMinecraftFreeze(builtInRegistriesClass);
                            try {
                                prepareRegistryAndContentsForMinecraftFreeze(
                                        builtInRegistriesClass.getField("REGISTRY").get(null));
                            } catch (Throwable ignored) {
                                // BuiltInRegistries.REGISTRY may vary by runtime snapshot.
                            }
                            Thread.sleep(Math.max(1L, intervalMillis));
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        FREEZE_GUARD_RUNNING.set(false);
                    }
                },
                "echo-native-registry-freeze-guard"
        );
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private static int prepareRegistryForMinecraftFreeze(Object registry, Set<Object> seen) {
        if (registry == null || seen == null || !seen.add(registry)) {
            return 0;
        }
        try {
            if (findField(registry.getClass(), "frozen") == null) {
                return 0;
            }
            clearUnregisteredIntrusiveHolders(registry);
            restoreNativeRegistryFrozenFlag(registry, true);
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static int initializeNativeBlockStateIds(Class<?> blockClass, Object block) {
        if (blockClass == null || block == null) {
            return 0;
        }
        int initialized = 0;
        try {
            Object stateDefinition = blockClass.getMethod("getStateDefinition").invoke(block);
            Object possibleStates = stateDefinition.getClass().getMethod("getPossibleStates").invoke(stateDefinition);
            if (!(possibleStates instanceof Iterable<?> states)) {
                return initialized;
            }
            Object idMapper = blockClass.getField("BLOCK_STATE_REGISTRY").get(null);
            java.lang.reflect.Method getId = idMapper.getClass().getMethod("getId", Object.class);
            java.lang.reflect.Method add = idMapper.getClass().getMethod("add", Object.class);
            for (Object state : states) {
                try {
                    Object id = getId.invoke(idMapper, state);
                    if (id instanceof Number number && number.intValue() < 0) {
                        add.invoke(idMapper, state);
                    }
                    state.getClass().getMethod("initCache").invoke(state);
                    initialized++;
                } catch (Throwable ignored) {
                    // One bad state should not prevent the remaining native states from becoming client-safe.
                }
            }
        } catch (Throwable ignored) {
            // The vanilla state mapper/cache varies between snapshots; registration remains best effort.
        }
        return initialized;
    }

    public static int initializeAllNativeBlockStateCaches(Class<?> blockClass) {
        if (blockClass == null) {
            return 0;
        }
        int initialized = 0;
        try {
            Object idMapper = blockClass.getField("BLOCK_STATE_REGISTRY").get(null);
            if (idMapper instanceof Iterable<?> states) {
                for (Object state : states) {
                    if (initializeNativeBlockStateCache(state)) {
                        initialized++;
                    }
                }
                return initialized;
            }
            Object iterator = idMapper.getClass().getMethod("iterator").invoke(idMapper);
            if (iterator instanceof java.util.Iterator<?> states) {
                while (states.hasNext()) {
                    if (initializeNativeBlockStateCache(states.next())) {
                        initialized++;
                    }
                }
            }
        } catch (Throwable ignored) {
            // The state mapper shape varies between Minecraft snapshots; this is a render-crash hardening pass.
        }
        return initialized;
    }

    private static boolean initializeNativeBlockStateCache(Object state) {
        if (state == null) {
            return false;
        }
        try {
            state.getClass().getMethod("initCache").invoke(state);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static java.lang.reflect.Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static java.lang.reflect.Field declaredFieldInHierarchy(Class<?> type, String name)
            throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
