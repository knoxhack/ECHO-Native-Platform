package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NativeLoaderRuntimeHostSupport {
    public static final String SERVICE_ID = "echo.native.runtime_host_support";

    private static final ThreadLocal<Map<String, Object>> RUNTIME_HOST_FAILURE =
            ThreadLocal.withInitial(LinkedHashMap::new);
    private static volatile Object standaloneRuntimeHost;

    private NativeLoaderRuntimeHostSupport() {
    }

    public static Object runtimeHost(Object serverPlayer, Object level, Context context) {
        RUNTIME_HOST_FAILURE.get().clear();
        try {
            Class<?> serverPlayerClass = Class.forName(context.runtimeClass().apply("server.level.ServerPlayer"));
            Class<?> serverLevelClass = Class.forName(context.runtimeClass().apply("server.level.ServerLevel"));
            if (!serverPlayerClass.isInstance(serverPlayer) || !serverLevelClass.isInstance(level)) {
                Map<String, Object> failure = RUNTIME_HOST_FAILURE.get();
                failure.put("failureKind", "runtime_host_type_mismatch");
                failure.put("expectedServerPlayerClass", serverPlayerClass.getName());
                failure.put("actualServerPlayerClass", serverPlayer == null ? "" : serverPlayer.getClass().getName());
                failure.put("expectedServerLevelClass", serverLevelClass.getName());
                failure.put("actualServerLevelClass", level == null ? "" : level.getClass().getName());
                return null;
            }
            for (String factoryClassName : context.profile().nativeRuntimeHostFactoryClasses()) {
                Object nativeHost = createRuntimeHost(
                        factoryClassName,
                        serverPlayerClass,
                        serverLevelClass,
                        serverPlayer,
                        level,
                        context.nativeClientModuleClassLoader().get());
                if (nativeHost != null) {
                    return nativeHost;
                }
            }
            recordHostFailure(
                    "runtime_host_factory_missing",
                    "nativeRuntimeHost",
                    new IllegalStateException("Product profile did not provide a native runtime host factory"));
            return null;
        } catch (Throwable failure) {
            recordHostFailure("runtime_host_resolution_exception", "nativeRuntimeHost", failure);
            return null;
        }
    }

    public static void putMissingHostEvidence(Map<String, Object> evidence) {
        if (evidence == null) {
            return;
        }
        evidence.put("failureKind", "missing_runtime_host");
        Map<String, Object> failure = RUNTIME_HOST_FAILURE.get();
        if (!failure.isEmpty()) {
            evidence.put("runtimeHostResolutionFailure", new LinkedHashMap<>(failure));
        }
    }

    public static void putInvocationFailure(Map<String, Object> evidence, String failureSource, Throwable failure) {
        if (evidence == null) {
            return;
        }
        Throwable unwrapped = unwrapReflectiveFailure(failure);
        evidence.put("mutated", false);
        evidence.put("failureKind", "native_runtime_invocation_exception");
        evidence.put("failureSource", failureSource == null ? "" : failureSource);
        evidence.put("failureType", unwrapped == null ? "" : unwrapped.getClass().getName());
        evidence.put("failureMessage", unwrapped == null || unwrapped.getMessage() == null
                ? ""
                : unwrapped.getMessage());
        Throwable cause = unwrapped == null ? null : unwrapped.getCause();
        evidence.put("failureCauseType", cause == null ? "" : cause.getClass().getName());
        evidence.put("failureCauseMessage", cause == null || cause.getMessage() == null ? "" : cause.getMessage());
    }

    public static String hostLane(Object runtimeHost) {
        Object lane = optionalMethodValue(runtimeHost, "runtimeLane");
        if (lane != null && !String.valueOf(lane).isBlank()) {
            return String.valueOf(lane);
        }
        String className = runtimeHost == null ? "" : runtimeHost.getClass().getName();
        if (className.contains("NativeLoader")) {
            return "Native Loader";
        }
        if (className.contains("NeoForge")) {
            return "NeoForge compatibility backend";
        }
        return className.isBlank() ? "" : "unknown";
    }

    public static String compatibilityDelegate(Object runtimeHost) {
        Object delegate = optionalMethodValue(runtimeHost, "compatibilityDelegateId");
        return delegate == null ? "" : String.valueOf(delegate);
    }

    public static String hostId(Object runtimeHost) {
        Object runtimeHostId = optionalMethodValue(runtimeHost, "runtimeHostId");
        return runtimeHostId == null ? "" : String.valueOf(runtimeHostId);
    }

    public static boolean hostRegistered(String runtimeHostId) {
        if (runtimeHostId == null || runtimeHostId.isBlank()) {
            return false;
        }
        try {
            Class<?> registryClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry");
            Object registry = registryClass.getMethod("global").invoke(null);
            Object optional = registryClass.getMethod("host", String.class).invoke(registry, runtimeHostId);
            return optional != null && Boolean.TRUE.equals(optional.getClass().getMethod("isPresent").invoke(optional));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Map<String, Object> capabilitiesSnapshot(String runtimeHostId) {
        if (runtimeHostId == null || runtimeHostId.isBlank()) {
            return Map.of();
        }
        try {
            Object capabilities = selectedRegistryValue(runtimeHostId, "capabilities");
            Object snapshot = capabilities == null ? null : capabilities.getClass().getMethod("snapshot").invoke(capabilities);
            return object(snapshot);
        } catch (Throwable ignored) {
            return Map.of();
        }
    }

    public static boolean selectedRuntimeHostConfigured(Context context) {
        return !selectedRuntimeHostId(context).isBlank();
    }

    public static String selectedRuntimeHostId(Context context) {
        String property = System.getProperty(context.runtimeHostIdProperty(), "");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String env = System.getenv(context.runtimeHostIdEnv());
        return env == null ? "" : env.trim();
    }

    public static String runtimeMode(Context context) {
        String property = System.getProperty(context.runtimeModeProperty(), "");
        if (property != null && !property.isBlank()) {
            String normalized = normalizedRuntimeMode(property);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        String env = System.getenv(context.runtimeModeEnv());
        if (env != null && !env.isBlank()) {
            String normalized = normalizedRuntimeMode(env);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        if (selectedRuntimeHostConfigured(context)) {
            return "NEOFORGE";
        }
        Object minecraft = context.minecraftInstance().get();
        if (minecraft != null) {
            return "NEOFORGE";
        }
        return "STANDALONE";
    }

    public static boolean clientCallerOnly(Context context) {
        return "NATIVE_CLIENT".equals(runtimeMode(context));
    }

    public static boolean standaloneMode(Context context) {
        return "STANDALONE".equals(runtimeMode(context));
    }

    public static Object standaloneRuntimeHost(Context context) {
        if (standaloneRuntimeHost != null) {
            return standaloneRuntimeHost;
        }
        if (!standaloneMode(context)) {
            return null;
        }
        synchronized (NativeLoaderRuntimeHostSupport.class) {
            if (standaloneRuntimeHost != null) {
                return standaloneRuntimeHost;
            }
            try {
                standaloneRuntimeHost = NativeLoaderCoreServiceRegistrar.createStandaloneRuntimeHost();
            } catch (Throwable ignored) {
                standaloneRuntimeHost = null;
            }
            return standaloneRuntimeHost;
        }
    }

    public static String standaloneHostStatus(Object host, String methodName, Object... args) {
        if (host == null || methodName == null || methodName.isBlank()) {
            return "";
        }
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int index = 0; index < args.length; index++) {
                Object arg = args[index];
                types[index] = arg instanceof Integer ? int.class : String.class;
            }
            Object status = host.getClass().getMethod(methodName, types).invoke(host, args);
            return status == null ? "" : String.valueOf(status);
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static Object selectedRegisteredRuntimeHost(Context context) {
        String runtimeHostId = selectedRuntimeHostId(context);
        return runtimeHostId.isBlank() ? null : selectedRegistryValue(runtimeHostId, "host");
    }

    public static Object selectedRegisteredRuntimeHostCapabilities(Context context) {
        String runtimeHostId = selectedRuntimeHostId(context);
        return runtimeHostId.isBlank() ? null : selectedRegistryValue(runtimeHostId, "capabilities");
    }

    public static Object serverPlayer(Object player, Context context) {
        try {
            Class<?> serverPlayerClass = tryClass(context.runtimeClass().apply("server.level.ServerPlayer"));
            if (serverPlayerClass != null && serverPlayerClass.isInstance(player)) {
                return player;
            }
            Object uuid = optionalMethodValue(player, "getUUID");
            if (!(uuid instanceof java.util.UUID playerUuid)) {
                return null;
            }
            Object minecraft = context.minecraftInstance().get();
            Object server = optionalMethodValue(minecraft, "getSingleplayerServer");
            Object playerList = optionalMethodValue(server, "getPlayerList");
            return playerList == null ? null
                    : playerList.getClass().getMethod("getPlayer", java.util.UUID.class)
                    .invoke(playerList, playerUuid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object serverLevel(Object level, Object serverPlayer, Context context) {
        try {
            Class<?> serverLevelClass = tryClass(context.runtimeClass().apply("server.level.ServerLevel"));
            if (serverLevelClass != null && serverLevelClass.isInstance(level)) {
                return level;
            }
            Object resolved = optionalMethodValue(serverPlayer, "serverLevel");
            if (resolved == null) {
                resolved = optionalMethodValue(serverPlayer, "level");
            }
            return serverLevelClass == null || serverLevelClass.isInstance(resolved) ? resolved : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object server(Object player, Object serverPlayer, Context context) {
        Object server = optionalMethodValue(serverPlayer, "getServer");
        if (server == null) {
            server = optionalFieldValue(serverPlayer, "server");
        }
        if (server != null) {
            return server;
        }
        Object minecraft = context.minecraftInstance().get();
        return optionalMethodValue(minecraft, "getSingleplayerServer");
    }

    public static boolean invokeOnServer(Object server, BooleanAction action) {
        if (action == null) {
            return false;
        }
        if (server == null || Boolean.TRUE.equals(optionalMethodValue(server, "isSameThread"))) {
            try {
                return action.run();
            } catch (Throwable ignored) {
                return false;
            }
        }
        boolean[] result = new boolean[]{false};
        Throwable[] failure = new Throwable[1];
        Runnable runnable = () -> {
            try {
                result[0] = action.run();
            } catch (Throwable exception) {
                failure[0] = exception;
            }
        };
        try {
            java.lang.reflect.Method execute = server.getClass().getMethod("execute", Runnable.class);
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            execute.invoke(server, (Runnable) () -> {
                try {
                    runnable.run();
                } finally {
                    latch.countDown();
                }
            });
            if (!latch.await(5L, java.util.concurrent.TimeUnit.SECONDS)) {
                return false;
            }
            return failure[0] == null && result[0];
        } catch (NoSuchMethodException ignored) {
            try {
                return action.run();
            } catch (Throwable exception) {
                return false;
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean invokeForServerPlayer(Object level, Object player, ServerAction action, Context context) {
        if (action == null) {
            return false;
        }
        Object serverPlayer = serverPlayer(player, context);
        if (serverPlayer == null) {
            return false;
        }
        Object serverLevel = serverLevel(level, serverPlayer, context);
        if (serverLevel == null) {
            return false;
        }
        Object server = server(player, serverPlayer, context);
        return invokeOnServer(server, () -> action.run(serverPlayer, serverLevel));
    }

    public static boolean resultMutated(Object result) {
        Object completed = optionalMethodValue(result, "completedWithMutation");
        if (completed instanceof Boolean value) {
            return value;
        }
        Object mutated = optionalMethodValue(result, "mutated");
        if (mutated instanceof Boolean value) {
            return value;
        }
        return "MUTATED".equals(resultStatus(result));
    }

    public static String resultStatus(Object result) {
        Object resultStatus = optionalMethodValue(result, "resultStatus");
        if (resultStatus != null && !String.valueOf(resultStatus).isBlank()) {
            return String.valueOf(resultStatus);
        }
        Object status = optionalMethodValue(result, "status");
        return status == null ? "" : String.valueOf(status);
    }

    public static void putResultEvidence(Map<String, Object> evidence, Object result) {
        if (evidence == null) {
            return;
        }
        if (result == null) {
            evidence.put("mutated", false);
            evidence.putIfAbsent("failureKind", "missing_native_result");
            return;
        }
        Map<String, Object> snapshot = object(optionalMethodValue(result, "snapshot"));
        Map<String, Object> details = object(snapshot.get("details"));
        boolean mutated = resultMutated(result);
        boolean hostSaveTouched = Boolean.TRUE.equals(snapshot.get("hostSaveTouched"));
        boolean saveTouched = hostSaveTouched
                || Boolean.TRUE.equals(snapshot.get("saveTouched"))
                || Boolean.TRUE.equals(details.get("saveTouched"));
        boolean missionAdvanced = Boolean.TRUE.equals(snapshot.get("missionAdvanced"))
                || Boolean.TRUE.equals(details.get("missionAdvanced"));
        boolean gameplayStateChanged = Boolean.TRUE.equals(snapshot.get("gameplayStateChanged"));
        boolean missionUpdated = missionAdvanced
                || gameplayStateChanged
                || Boolean.TRUE.equals(snapshot.get("missionTouched"))
                || snapshot.containsKey("mission")
                || details.containsKey("mission");
        evidence.put("mutated", mutated);
        evidence.put("status", resultStatus(result));
        evidence.put("message", String.valueOf(optionalMethodValue(result, "message")));
        evidence.put("resultSnapshot", snapshot);
        evidence.put("adapterCoreCallEnteredNativeLoaderHost",
                Boolean.TRUE.equals(snapshot.get("adapterCoreCallEnteredNativeLoaderHost")));
        evidence.put("adapterCoreCallEnteredNativeLoaderBackend",
                Boolean.TRUE.equals(snapshot.get("adapterCoreCallEnteredNativeLoaderBackend")));
        evidence.put("adapterCoreBackendClass", String.valueOf(snapshot.getOrDefault("adapterCoreBackendClass", "")));
        evidence.put("nativeLoaderBackendAttached", Boolean.TRUE.equals(snapshot.get("nativeLoaderBackendAttached")));
        evidence.put("nativeLoaderBackendRecordStatus",
                String.valueOf(snapshot.getOrDefault("nativeLoaderBackendRecordStatus", "")));
        evidence.put("nativeLoaderBackendRecord", object(snapshot.get("nativeLoaderBackendRecord")));
        evidence.put("nativeLoaderRuntimeHostClass",
                String.valueOf(snapshot.getOrDefault("nativeLoaderRuntimeHostClass", "")));
        evidence.put("compatibilityFallbackUsed", Boolean.TRUE.equals(snapshot.get("compatibilityFallbackUsed")));
        evidence.put("hostSaveTouched", hostSaveTouched);
        evidence.put("saveTouched", saveTouched);
        evidence.put("gameplayStateChanged", gameplayStateChanged);
        evidence.put("missionAdvanced", missionAdvanced);
        evidence.put("missionUpdated", missionUpdated);
        evidence.put("hudOrEventEmitted", Boolean.TRUE.equals(snapshot.get("hudOrEventEmitted")));
        evidence.put("feedbackEmitted", Boolean.TRUE.equals(snapshot.get("feedbackEmitted"))
                || Boolean.TRUE.equals(snapshot.get("hudOrEventEmitted")));
    }

    private static Object createRuntimeHost(
            String factoryClassName,
            Class<?> serverPlayerClass,
            Class<?> serverLevelClass,
            Object serverPlayer,
            Object level,
            ClassLoader nativeClientModuleClassLoader
    ) {
        try {
            Class<?> factoryClass = Class.forName(
                    factoryClassName,
                    true,
                    nativeClientModuleClassLoader);
            return factoryClass.getMethod("create", serverPlayerClass, serverLevelClass)
                    .invoke(null, serverPlayer, level);
        } catch (Throwable failure) {
            recordHostFailure("runtime_host_factory_failed", factoryClassName, failure);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void recordHostFailure(String failureKind, String source, Throwable failure) {
        Map<String, Object> details = RUNTIME_HOST_FAILURE.get();
        details.put("failureKind", failureKind == null ? "runtime_host_failure" : failureKind);
        details.put("lastFailureSource", source == null ? "" : source);
        Throwable unwrapped = unwrapReflectiveFailure(failure);
        details.put("lastFailureType", unwrapped == null ? "" : unwrapped.getClass().getName());
        details.put("lastFailureMessage", unwrapped == null || unwrapped.getMessage() == null ? "" : unwrapped.getMessage());
        Object existing = details.get("factoryFailures");
        List<Map<String, Object>> failures = existing instanceof List
                ? (List<Map<String, Object>>) existing
                : new ArrayList<>();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("source", source == null ? "" : source);
        entry.put("failureKind", failureKind == null ? "runtime_host_failure" : failureKind);
        entry.put("failureType", unwrapped == null ? "" : unwrapped.getClass().getName());
        entry.put("failureMessage", unwrapped == null || unwrapped.getMessage() == null ? "" : unwrapped.getMessage());
        Throwable cause = unwrapped == null ? null : unwrapped.getCause();
        entry.put("causeType", cause == null ? "" : cause.getClass().getName());
        entry.put("causeMessage", cause == null || cause.getMessage() == null ? "" : cause.getMessage());
        failures.add(entry);
        details.put("factoryFailures", failures);
    }

    private static Object selectedRegistryValue(String runtimeHostId, String methodName) {
        if (runtimeHostId == null || runtimeHostId.isBlank()) {
            return null;
        }
        try {
            Class<?> registryClass = Class.forName("com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry");
            Object registry = registryClass.getMethod("global").invoke(null);
            Object optional = registryClass.getMethod(methodName, String.class).invoke(registry, runtimeHostId);
            if (optional == null) {
                return null;
            }
            Object present = optional.getClass().getMethod("isPresent").invoke(optional);
            if (!Boolean.TRUE.equals(present)) {
                return null;
            }
            return optional.getClass().getMethod("get").invoke(optional);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String normalizedRuntimeMode(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.trim().toUpperCase()) {
            case "NATIVE_CLIENT", "NEOFORGE", "STANDALONE" -> value.trim().toUpperCase();
            default -> "";
        };
    }

    private static Throwable unwrapReflectiveFailure(Throwable failure) {
        if (failure instanceof java.lang.reflect.InvocationTargetException invocation
                && invocation.getTargetException() != null) {
            return invocation.getTargetException();
        }
        if (failure instanceof java.lang.ExceptionInInitializerError initializer
                && initializer.getException() != null) {
            return initializer.getException();
        }
        return failure;
    }

    private static Class<?> tryClass(String className) {
        try {
            return className == null || className.isBlank() ? null : Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object optionalFieldValue(Object target, String fieldName) {
        try {
            if (target == null || fieldName == null || fieldName.isBlank()) {
                return null;
            }
            return target.getClass().getField(fieldName).get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object optionalMethodValue(Object target, String methodName) {
        try {
            if (target == null || methodName == null || methodName.isBlank()) {
                return null;
            }
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    public record Context(
            EchoNativeBootstrapProductProfile profile,
            Function<String, String> runtimeClass,
            Supplier<Object> minecraftInstance,
            Supplier<ClassLoader> nativeClientModuleClassLoader,
            String runtimeHostIdProperty,
            String runtimeHostIdEnv,
            String runtimeModeProperty,
            String runtimeModeEnv
    ) {
    }

    public interface BooleanAction {
        boolean run() throws Throwable;
    }

    public interface ServerAction {
        boolean run(Object serverPlayer, Object serverLevel) throws Throwable;
    }
}
