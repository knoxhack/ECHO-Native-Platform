package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeEntityDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderEntityRegistryBridge {
    public static final String SERVICE_ID = "echo.native.entity_registry_bridge";

    private NativeLoaderEntityRegistryBridge() {
    }

    public static Map<String, Object> applyPostBootstrap(Config config, Context context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bridge", bridgeId(config));
        data.put("serviceId", SERVICE_ID);
        data.put("applied", false);
        data.put("vanillaBootstrapInvoked", false);
        try {
            Class.forName(context.runtimeClass("SharedConstants")).getMethod("tryDetectVersion").invoke(null);
            Class.forName(context.runtimeClass("server.Bootstrap")).getMethod("bootStrap").invoke(null);
            data.put("vanillaBootstrapInvoked", true);

            Class<?> identifierClass = Class.forName(context.runtimeClass("resources.Identifier"));
            Class<?> registriesClass = Class.forName(context.runtimeClass("core.registries.Registries"));
            Class<?> resourceKeyClass = Class.forName(context.runtimeClass("resources.ResourceKey"));
            Class<?> builtInRegistriesClass = Class.forName(context.runtimeClass("core.registries.BuiltInRegistries"));
            Class<?> registryClass = Class.forName(context.runtimeClass("core.Registry"));
            Class<?> entityTypeClass = Class.forName(context.runtimeClass("world.entity.EntityType"));
            Class<?> entityFactoryClass = Class.forName(context.runtimeClass("world.entity.EntityType$EntityFactory"));
            Class<?> entityTypeBuilderClass = Class.forName(context.runtimeClass("world.entity.EntityType$Builder"));
            Class<?> mobCategoryClass = Class.forName(context.runtimeClass("world.entity.MobCategory"));
            Object entityRegistry = builtInRegistriesClass.getField("ENTITY_TYPE").get(null);
            Object entityRegistryKey = registriesClass.getField("ENTITY_TYPE").get(null);

            List<Map<String, Object>> registeredNativeEntities = registerNativeProductEntities(
                    config,
                    context,
                    identifierClass,
                    resourceKeyClass,
                    registryClass,
                    entityTypeClass,
                    entityFactoryClass,
                    entityTypeBuilderClass,
                    mobCategoryClass,
                    entityRegistry,
                    entityRegistryKey
            );
            long registeredCount = registeredNativeEntities.stream()
                    .filter(entry -> "registered".equals(String.valueOf(entry.get("status")))
                            || "already_registered".equals(String.valueOf(entry.get("status"))))
                    .count();
            long failedCount = registeredNativeEntities.stream()
                    .filter(entry -> "failed".equals(String.valueOf(entry.get("status"))))
                    .count();
            data.put("applied", failedCount == 0 && registeredCount > 0);
            data.put("registeredNativeEntityCount", registeredCount);
            data.put("failedNativeEntityCount", failedCount);
            data.put("registeredNativeEntities", registeredNativeEntities);
            data.put("summary", "Registered " + config.profile().nativeGameplayDisplayName()
                    + " entity ids after vanilla bootstrap initialized Blocks/EntityType.");
        } catch (Throwable exception) {
            data.put("failureKind", exception.getClass().getSimpleName());
            data.put("failureMessage", failureMessage(exception));
            data.put("summary", config.profile().nativeGameplayDisplayName()
                    + " native entity registry bridge failed after vanilla bootstrap: " + failureMessage(exception));
        }
        return data;
    }

    public static void merge(Map<String, Object> runtimeBridge, Map<String, Object> nativeEntityBridge) {
        Map<String, Object> registryBridge = new LinkedHashMap<>();
        Object existing = runtimeBridge.get("registryBridge");
        if (existing instanceof Map<?, ?> map) {
            map.forEach((key, value) -> registryBridge.put(String.valueOf(key), value));
        }
        registryBridge.put("nativeEntityBridge", nativeEntityBridge);
        registryBridge.put("nativeEntityBridgeDeferredUntilVanillaBootstrap", false);
        registryBridge.put("nativeEntityBridgePostBootstrapApplied", Boolean.TRUE.equals(nativeEntityBridge.get("applied")));
        registryBridge.put("registeredNativeEntityCount", integer(nativeEntityBridge.get("registeredNativeEntityCount")));
        registryBridge.put("failedNativeEntityCount", integer(nativeEntityBridge.get("failedNativeEntityCount")));
        Object entities = nativeEntityBridge.get("registeredNativeEntities");
        if (entities instanceof Iterable<?>) {
            registryBridge.put("registeredNativeEntities", entities);
        }
        runtimeBridge.put("registryBridge", registryBridge);
    }

    public static void markDeferred(Map<String, Object> runtimeBridge, Config config) {
        Map<String, Object> registryBridge = new LinkedHashMap<>();
        Object existing = runtimeBridge.get("registryBridge");
        if (existing instanceof Map<?, ?> map) {
            map.forEach((key, value) -> registryBridge.put(String.valueOf(key), value));
        }
        Map<String, Object> nativeEntityBridge = new LinkedHashMap<>();
        nativeEntityBridge.put("bridge", bridgeId(config));
        nativeEntityBridge.put("serviceId", SERVICE_ID);
        nativeEntityBridge.put("applied", false);
        nativeEntityBridge.put("preHandoffDisabled", true);
        nativeEntityBridge.put("summary",
                "Deferred: native entity registration must not load EntityType or Blocks before Minecraft Main completes vanilla bootstrap.");
        registryBridge.put("nativeEntityBridge", Map.copyOf(nativeEntityBridge));
        registryBridge.put("nativeEntityBridgeDeferredUntilVanillaBootstrap", true);
        registryBridge.put("nativeEntityBridgePostBootstrapApplied", false);
        registryBridge.put("registeredNativeEntityCount", 0);
        registryBridge.put("failedNativeEntityCount", 0);
        runtimeBridge.put("registryBridge", registryBridge);
    }

    private static List<Map<String, Object>> registerNativeProductEntities(
            Config config,
            Context context,
            Class<?> identifierClass,
            Class<?> resourceKeyClass,
            Class<?> registryClass,
            Class<?> entityTypeClass,
            Class<?> entityFactoryClass,
            Class<?> entityTypeBuilderClass,
            Class<?> mobCategoryClass,
            Object entityRegistry,
            Object entityRegistryKey
    ) {
        List<Map<String, Object>> registered = new ArrayList<>();
        boolean wasFrozen = NativeLoaderRegistryRuntimeSupport.unfreezeNativeRegistry(entityRegistry);
        NativeLoaderRegistryRuntimeSupport.enableNativeIntrusiveHolders(entityRegistry);
        try {
            for (NativeEntityDefinition definition : config.profile().nativeEntities()) {
                try {
                    Object id = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                            .invoke(null, config.profile().namespace(), definition.path());
                    if (registryContainsExact(entityRegistry, id)) {
                        Map<String, Object> entry = nativeEntityReport(config, definition, "already_registered", "", false);
                        registered.add(entry);
                        continue;
                    }
                    Object entityKey = resourceKeyClass.getMethod("create", resourceKeyClass, identifierClass)
                            .invoke(null, entityRegistryKey, id);
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object category = Enum.valueOf((Class<Enum>) mobCategoryClass.asSubclass(Enum.class), definition.category());
                    EntityFactoryResolution resolution = nativeEntityFactory(config, context, definition, entityFactoryClass, entityTypeClass);
                    Object builder = entityTypeBuilderClass.getMethod("of", entityFactoryClass, mobCategoryClass)
                            .invoke(null, resolution.factory(), category);
                    entityTypeBuilderClass.getMethod("sized", float.class, float.class)
                            .invoke(builder, definition.width(), definition.height());
                    entityTypeBuilderClass.getMethod("clientTrackingRange", int.class)
                            .invoke(builder, definition.trackingRange());
                    if (definition.fireImmune()) {
                        entityTypeBuilderClass.getMethod("fireImmune").invoke(builder);
                    }
                    Object entityType = entityTypeBuilderClass.getMethod("build", resourceKeyClass)
                            .invoke(builder, entityKey);
                    registryClass.getMethod("register", registryClass, identifierClass, Object.class)
                            .invoke(null, entityRegistry, id, entityType);
                    boolean attributesInstalled = installNativeEntityAttributes(config, context, entityType, definition, resolution.entityClassName());
                    Map<String, Object> entry = nativeEntityReport(config, definition, "registered", resolution.entityClassName(), attributesInstalled);
                    entry.put("factoryFallbackUsed", resolution.fallbackUsed());
                    registered.add(entry);
                } catch (Throwable exception) {
                    Map<String, Object> entry = nativeEntityReport(config, definition, "failed", "", false);
                    entry.put("failureKind", exception.getClass().getSimpleName());
                    entry.put("failureMessage", failureMessage(exception));
                    registered.add(entry);
                }
            }
        } finally {
            if (wasFrozen) {
                NativeLoaderRegistryRuntimeSupport.freezeNativeRegistry(entityRegistry);
            }
        }
        return registered;
    }

    private static String nativeProductEntityId(Config config, NativeEntityDefinition definition) {
        return definition.id(config.profile().namespace());
    }

    private static Map<String, Object> nativeEntityReport(
            Config config,
            NativeEntityDefinition definition,
            String status,
            String entityClassName,
            boolean attributesInstalled
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("entityId", nativeProductEntityId(config, definition));
        entry.put("status", status);
        entry.put("declaredClass", definition.className());
        entry.put("nativeFactoryClass", entityClassName);
        entry.put("category", definition.category());
        entry.put("width", definition.width());
        entry.put("height", definition.height());
        entry.put("trackingRange", definition.trackingRange());
        entry.put("fireImmune", definition.fireImmune());
        entry.put("attributesInstalled", attributesInstalled);
        return entry;
    }

    private record EntityFactoryResolution(Object factory, String entityClassName, boolean fallbackUsed) {
    }

    private static EntityFactoryResolution nativeEntityFactory(
            Config config,
            Context context,
            NativeEntityDefinition definition,
            Class<?> entityFactoryClass,
            Class<?> entityTypeClass
    ) throws ReflectiveOperationException {
        Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
        Class<?> entityClass = nativeEntityImplementationClass(config, context, definition);
        boolean fallbackUsed = !definition.className().equals(entityClass.getName());
        Object factory = java.lang.reflect.Proxy.newProxyInstance(
                entityFactoryClass.getClassLoader(),
                new Class<?>[]{entityFactoryClass},
                (proxy, method, args) -> {
                    String methodName = method.getName();
                    if ("create".equals(methodName) && args != null && args.length == 2) {
                        return createNativeEntityInstance(entityClass, entityTypeClass, levelClass, args[0], args[1]);
                    }
                    if ("toString".equals(methodName)) {
                        return "EchoNativeProductEntityFactory[" + nativeProductEntityId(config, definition) + " -> " + entityClass.getName() + "]";
                    }
                    if ("hashCode".equals(methodName)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(methodName)) {
                        return args != null && args.length == 1 && proxy == args[0];
                    }
                    return defaultProxyReturn(method.getReturnType());
                }
        );
        return new EntityFactoryResolution(factory, entityClass.getName(), fallbackUsed);
    }

    private static Class<?> nativeEntityImplementationClass(
            Config config,
            Context context,
            NativeEntityDefinition definition
    ) {
        Class<?> realClass = tryClass(definition.className());
        if (realClass != null && hasNativeEntityConstructor(context, realClass)) {
            return realClass;
        }
        Class<?> fallbackClass = tryClass(definition.fallbackClassName());
        if (fallbackClass != null && hasNativeEntityConstructor(context, fallbackClass)) {
            return fallbackClass;
        }
        for (String fallback : List.of(
                "net.minecraft.world.entity.monster.zombie.Zombie",
                "net.minecraft.world.entity.monster.Vex",
                "net.minecraft.world.entity.monster.Slime"
        )) {
            Class<?> candidate = tryClass(fallback);
            if (candidate != null && hasNativeEntityConstructor(context, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No native entity implementation class available for " + nativeProductEntityId(config, definition));
    }

    private static boolean hasNativeEntityConstructor(Context context, Class<?> entityClass) {
        try {
            Class<?> entityTypeClass = Class.forName(context.runtimeClass("world.entity.EntityType"));
            Class<?> levelClass = Class.forName(context.runtimeClass("world.level.Level"));
            entityClass.getConstructor(entityTypeClass, levelClass);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object createNativeEntityInstance(
            Class<?> entityClass,
            Class<?> entityTypeClass,
            Class<?> levelClass,
            Object entityType,
            Object level
    ) throws ReflectiveOperationException {
        return entityClass.getConstructor(entityTypeClass, levelClass).newInstance(entityType, level);
    }

    private static Object defaultProxyReturn(Class<?> returnType) {
        if (returnType == null || !returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType)) {
            return (byte) 0;
        }
        if (short.class.equals(returnType)) {
            return (short) 0;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (float.class.equals(returnType)) {
            return 0.0F;
        }
        if (double.class.equals(returnType)) {
            return 0.0D;
        }
        return null;
    }

    private static boolean installNativeEntityAttributes(
            Config config,
            Context context,
            Object entityType,
            NativeEntityDefinition definition,
            String entityClassName
    ) {
        try {
            Object supplier = nativeEntityAttributeSupplier(context, definition, entityClassName);
            if (supplier == null) {
                return false;
            }
            Class<?> defaultAttributesClass = Class.forName(context.runtimeClass("world.entity.ai.attributes.DefaultAttributes"));
            java.lang.reflect.Field suppliersField = defaultAttributesClass.getDeclaredField("SUPPLIERS");
            suppliersField.setAccessible(true);
            Object current = suppliersField.get(null);
            if (current instanceof Map<?, ?> currentMap) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> mutable = (Map<Object, Object>) currentMap;
                try {
                    mutable.put(entityType, supplier);
                    return true;
                } catch (UnsupportedOperationException ignored) {
                    Map<Object, Object> copy = new LinkedHashMap<>(mutable);
                    copy.put(entityType, supplier);
                    setStaticFinalField(suppliersField, copy);
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Missing attributes are visible in runtime diagnostics if Minecraft rejects a spawn.
        }
        return false;
    }

    private static Object nativeEntityAttributeSupplier(
            Context context,
            NativeEntityDefinition definition,
            String entityClassName
    ) {
        for (String className : List.of(entityClassName, definition.className(), definition.fallbackClassName(),
                "net.minecraft.world.entity.monster.zombie.Zombie", "net.minecraft.world.entity.Mob")) {
            Class<?> sourceClass = tryClass(className);
            if (sourceClass == null) {
                continue;
            }
            try {
                Object builder = sourceClass.getMethod("createAttributes").invoke(null);
                return buildAttributeSupplier(builder);
            } catch (Throwable ignored) {
                try {
                    Object builder = sourceClass.getMethod("createMobAttributes").invoke(null);
                    return buildAttributeSupplier(builder);
                } catch (Throwable ignoredAgain) {
                    // Try the next source.
                }
            }
        }
        return null;
    }

    private static Object buildAttributeSupplier(Object builderOrSupplier) throws ReflectiveOperationException {
        if (builderOrSupplier == null) {
            return null;
        }
        try {
            return builderOrSupplier.getClass().getMethod("build").invoke(builderOrSupplier);
        } catch (NoSuchMethodException ignored) {
            return builderOrSupplier;
        }
    }

    private static void setStaticFinalField(java.lang.reflect.Field field, Object value) throws ReflectiveOperationException {
        try {
            field.set(null, value);
            return;
        } catch (IllegalAccessException ignored) {
            // Java 21 keeps many static-final fields write-protected; Unsafe is the last-resort bridge here.
        }
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Object base = unsafeClass.getMethod("staticFieldBase", java.lang.reflect.Field.class).invoke(unsafe, field);
        long offset = ((Number) unsafeClass.getMethod("staticFieldOffset", java.lang.reflect.Field.class).invoke(unsafe, field)).longValue();
        unsafeClass.getMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, base, offset, value);
    }

    private static boolean registryContainsExact(Object registry, Object identifier) {
        if (registry == null || identifier == null) {
            return false;
        }
        Class<?> identifierClass = identifier.getClass();
        try {
            Object contains = registry.getClass().getMethod("containsKey", identifierClass).invoke(registry, identifier);
            if (contains instanceof Boolean value) {
                return value;
            }
        } catch (Throwable ignored) {
            // Some registry implementations expose Optional/getKey instead of containsKey.
        }
        try {
            Object optional = registry.getClass().getMethod("getOptional", identifierClass).invoke(registry, identifier);
            if (optional instanceof java.util.Optional<?> value) {
                return value.isPresent();
            }
        } catch (Throwable ignored) {
            // Fall through to exact key comparison for defaulted registries.
        }
        try {
            Object value = registry.getClass().getMethod("getValue", identifierClass).invoke(registry, identifier);
            if (value == null) {
                return false;
            }
            Object actualIdentifier = registry.getClass().getMethod("getKey", Object.class).invoke(registry, value);
            return identifier.equals(actualIdentifier);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> tryClass(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String bridgeId(Config config) {
        return config.profile().nativeGameplayPackId() + ".native_entity_registry.post_bootstrap";
    }

    public static final class Config {
        private final EchoNativeBootstrapProductProfile profile;

        public Config(EchoNativeBootstrapProductProfile profile) {
            this.profile = profile;
        }

        EchoNativeBootstrapProductProfile profile() {
            return profile;
        }
    }

    public static final class Context {
        private final RuntimeClassResolver runtimeClassResolver;

        public Context(RuntimeClassResolver runtimeClassResolver) {
            this.runtimeClassResolver = runtimeClassResolver;
        }

        String runtimeClass(String suffix) {
            return runtimeClassResolver.resolve(suffix);
        }
    }

    @FunctionalInterface
    public interface RuntimeClassResolver {
        String resolve(String suffix);
    }
}
