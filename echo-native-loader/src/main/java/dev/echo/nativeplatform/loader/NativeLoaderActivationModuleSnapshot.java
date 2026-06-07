package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderActivationModuleSnapshot {
    public static final String SERVICE_ID = "echo.native.activation_module_snapshot";

    private NativeLoaderActivationModuleSnapshot() {
    }

    public static Map<String, Object> module(
            String id,
            Map<String, Object> activation,
            String creativeItemId,
            boolean creativeContentVisible,
            Map<String, Object> productGameplayBridge
    ) {
        Map<String, Object> gameplayBridge = productGameplayBridge == null ? Map.of() : productGameplayBridge;
        boolean nativeActivated = nativeActivationLoaded(activation);
        boolean classLoaded = activation != null
                && !String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
        boolean attemptedWithoutLoadedClass = activation != null
                && !String.valueOf(activation.getOrDefault("entrypoint", "")).isBlank()
                && String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
        boolean gameplayVerified = nativeActivated
                && Boolean.TRUE.equals(gameplayBridge.get("liveGameplayHandlersAttached"))
                && Boolean.TRUE.equals(gameplayBridge.get("gameplayHandlerExecuted"));
        Map<String, Object> module = new LinkedHashMap<>();
        module.put("nativeActivationModuleSnapshotServiceId", SERVICE_ID);
        module.put("id", id);
        module.put("activationMarkerWritten", true);
        module.put("nativeModuleActivated", nativeActivated);
        module.put("entrypoint", activation == null ? "" : activation.getOrDefault("entrypoint", ""));
        module.put("loadedClassName", activation == null ? "" : activation.getOrDefault("loadedClassName", ""));
        module.put("loadedClassLoader", activation == null ? "" : activation.getOrDefault("loadedClassLoader", ""));
        module.put("nativeAdapterCodeExecuted", activation != null && Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted")));
        module.put("serviceCodeExecuted", activation != null && Boolean.TRUE.equals(activation.get("serviceCodeExecuted")));
        module.put("nativeLoaderLifecycleAttempted", activation != null
                && Boolean.TRUE.equals(activation.get("nativeLoaderLifecycleAttempted")));
        module.put("nativeLoaderLifecycleFallback", activation != null
                && Boolean.TRUE.equals(activation.get("nativeLoaderLifecycleFallback")));
        module.put("activationStage", activation == null ? "" : activation.getOrDefault("activationStage", ""));
        module.put("status", activation == null ? "" : activation.getOrDefault("status", ""));
        module.put("registeredServiceCount", activation == null ? 0 : integer(activation.get("registeredServiceCount")));
        module.put("registeredContentCount", activation == null ? 0 : integer(activation.get("registeredContentCount")));
        module.put("registeredServices", activation == null ? List.of() : activation.getOrDefault("registeredServices", List.of()));
        module.put("registeredContent", activation == null ? List.of() : activation.getOrDefault("registeredContent", List.of()));
        module.put("registeredFeatureContracts", activation == null ? List.of() : activation.getOrDefault("registeredFeatureContracts", List.of()));
        module.put("adapterDomains", activation == null ? List.of() : activation.getOrDefault("adapterDomains", List.of()));
        module.put("runtimeTargets", activation == null ? List.of() : activation.getOrDefault("runtimeTargets", List.of()));
        module.put("lifecyclePhaseHistory", activation == null ? List.of() : activation.getOrDefault("lifecyclePhaseHistory", List.of()));
        module.put("nativeLifecycleDispatch", activation == null ? Map.of() : activation.getOrDefault("nativeLifecycleDispatch", Map.of()));
        module.put("lifecycleBridge", activation == null ? Map.of() : activation.getOrDefault("lifecycleBridge", Map.of()));
        module.put("serviceBridge", activation == null ? Map.of() : activation.getOrDefault("serviceBridge", Map.of()));
        module.put("registryBridge", activation == null ? Map.of() : activation.getOrDefault("registryBridge", Map.of()));
        module.put("eventBridge", activation == null ? Map.of() : activation.getOrDefault("eventBridge", Map.of()));
        module.put("loadedModuleStateWritten", activation != null
                && Boolean.TRUE.equals(activation.get("loadedModuleStateWritten")));
        module.put("loadedModuleStatePath", activation == null ? "" : activation.getOrDefault("loadedModuleStatePath", ""));
        module.put("loadedModuleState", activation == null ? Map.of() : activation.getOrDefault("loadedModuleState", Map.of()));
        module.put("nativeLoadedModuleState", activation == null ? Map.of() : activation.getOrDefault("nativeLoadedModuleState", Map.of()));
        module.put("diagnostics", activation == null ? List.of() : activation.getOrDefault("diagnostics", List.of()));
        module.put("registeredAsItem", creativeItemId != null && !creativeItemId.isBlank());
        module.put("visibleInCreative", creativeContentVisible && creativeItemId != null && !creativeItemId.isBlank());
        module.put("creativeItemId", creativeItemId == null ? "" : creativeItemId);
        module.put("liveGameplayHookVerified", gameplayVerified);
        module.put("liveGameplayHookBlocker", gameplayVerified
                ? ""
                : nativeActivated
                ? "native_gameplay_handler_execution_missing"
                : classLoaded
                ? "native_module_adapter_activation_missing"
                : attemptedWithoutLoadedClass
                ? "native_module_class_load_evidence_missing"
                : "token_only_module_has_no_native_adapter");
        module.put("state", gameplayVerified
                ? "native_module_adapter_gameplay_verified"
                : nativeActivated
                ? "native_module_adapter_activated"
                : classLoaded
                ? "native_module_class_loaded_activation_pending"
                : attemptedWithoutLoadedClass ? "native_module_class_load_evidence_missing" : "module_token_registered");
        return module;
    }

    public static boolean nativeActivationLoaded(Map<String, Object> activation) {
        return activation != null
                && Boolean.TRUE.equals(activation.get("activated"))
                && Boolean.TRUE.equals(activation.get("nativeAdapterCodeExecuted"))
                && !String.valueOf(activation.getOrDefault("entrypoint", "")).isBlank()
                && !String.valueOf(activation.getOrDefault("loadedClassName", "")).isBlank();
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
