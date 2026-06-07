package dev.echo.nativeplatform.loader;

import java.util.Map;

public final class NativeLoaderRegistryRuntimeSupport {
    public static final String SERVICE_ID = "echo.native.registry_runtime_support";

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

    public static void freezeNativeRegistry(Object registry) {
        if (registry == null) {
            return;
        }
        try {
            registry.getClass().getMethod("freeze").invoke(registry);
        } catch (Throwable ignored) {
            // If refreezing fails, the next registry access or log will expose the runtime issue.
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
