package dev.echo.nativeplatform.loader;

import java.util.Map;
import java.util.function.Function;

public final class NativeLoaderRuntimeHostFlow {
    public static final String SERVICE_ID = "echo.native.runtime_host_flow";

    private final NativeLoaderRuntimeHostSupport.Context runtimeHostContext;
    private final NativeLoaderAdapterCoreRuntimeMutations.Context mutationContext;
    private final NativeLoaderAdapterCoreGameplayRuntimeActions.Context gameplayContext;
    private final Function<String, String> resolveItemId;

    public NativeLoaderRuntimeHostFlow(
            NativeLoaderRuntimeHostSupport.Context runtimeHostContext,
            NativeLoaderAdapterCoreRuntimeMutations.Context mutationContext,
            NativeLoaderAdapterCoreGameplayRuntimeActions.Context gameplayContext,
            Function<String, String> resolveItemId
    ) {
        this.runtimeHostContext = runtimeHostContext;
        this.mutationContext = mutationContext;
        this.gameplayContext = gameplayContext;
        this.resolveItemId = resolveItemId;
    }

    public String resolveRuntimeItemId(Object runtimeHost, String requestedId) {
        String resolved = resolveItemId.apply(requestedId);
        if (!resolved.isBlank()) {
            return resolved;
        }
        String id = lowerContentId(requestedId);
        if (!id.isBlank() && canonicalContentSupported(runtimeHost, id)) {
            return id;
        }
        return "";
    }

    public Object uiRuntimeHost() {
        Object standalone = standaloneRuntimeHost();
        if (standalone != null) {
            return standalone;
        }
        Object minecraft = minecraftInstance();
        Object player = minecraftField(minecraft, "player");
        Object serverPlayer = serverPlayer(player);
        Object level = serverLevel(minecraftField(minecraft, "level"), serverPlayer);
        return serverPlayer == null || level == null ? null : runtimeHost(serverPlayer, level);
    }

    public Object uiGrantRuntimeHost(Object minecraftRuntimeHost) {
        if (selectedRuntimeHostConfigured()) {
            return selectedRegisteredRuntimeHost();
        }
        if (clientCallerOnly()) {
            return null;
        }
        Object standalone = standaloneRuntimeHost();
        return standalone == null ? minecraftRuntimeHost : standalone;
    }

    public Object uiActionRuntimeHost(Object minecraftRuntimeHost) {
        if (selectedRuntimeHostConfigured()) {
            return selectedRegisteredRuntimeHost();
        }
        if (clientCallerOnly()) {
            return null;
        }
        Object standalone = standaloneRuntimeHost();
        return standalone == null ? minecraftRuntimeHost : standalone;
    }

    public void putSelectedHostEvidence(Map<String, Object> evidence, Object runtimeHost) {
        if (evidence == null) {
            return;
        }
        boolean configured = selectedRuntimeHostConfigured();
        evidence.put("selectedRuntimeHostConfigured", configured);
        evidence.put("selectedRuntimeHostId", configured ? selectedRuntimeHostId() : "");
        evidence.put("selectedRuntimeHostResolved", configured && runtimeHost != null);
        if (runtimeHost != null) {
            putRuntimeHostEvidence(evidence, runtimeHost);
        }
    }

    public void putRuntimeHostEvidence(Map<String, Object> evidence, Object runtimeHost) {
        NativeLoaderAdapterCoreRuntimeMutations.putRuntimeHostEvidence(mutationContext, evidence, runtimeHost);
    }

    public boolean runtimeSurfaceSupported(Object runtimeHost, String surfaceMethod) {
        if (runtimeHost == null || surfaceMethod == null || surfaceMethod.isBlank()) {
            return false;
        }
        try {
            Object surface = runtimeHost.getClass().getMethod(surfaceMethod).invoke(runtimeHost);
            if (surface == null) {
                return false;
            }
            String surfaceClass = surface.getClass().getName();
            return !surfaceClass.startsWith("com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost$");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean canonicalContentSupported(Object runtimeHost, String contentId) {
        if (runtimeHost == null || contentId == null || contentId.isBlank()) {
            return false;
        }
        Object capabilities = selectedRegisteredRuntimeHostCapabilities();
        if (capabilities == null) {
            return false;
        }
        try {
            Object supported = capabilities.getClass()
                    .getMethod("supportsCanonicalContent", String.class)
                    .invoke(capabilities, contentId);
            return Boolean.TRUE.equals(supported);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean runtimeActionSupported(Object runtimeHost, String actionId) {
        if (runtimeHost == null || actionId == null || actionId.isBlank()) {
            return false;
        }
        if (!selectedRuntimeHostConfigured()) {
            return true;
        }
        Object capabilities = selectedRegisteredRuntimeHostCapabilities();
        if (capabilities == null) {
            return false;
        }
        try {
            Object supported = capabilities.getClass().getMethod("supportsAction", String.class).invoke(capabilities, actionId);
            return Boolean.TRUE.equals(supported);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public Object runtimeHost(Object serverPlayer, Object level) {
        return NativeLoaderRuntimeHostSupport.runtimeHost(serverPlayer, level, runtimeHostContext);
    }

    public void putMissingHostEvidence(Map<String, Object> evidence) {
        NativeLoaderRuntimeHostSupport.putMissingHostEvidence(evidence);
    }

    public boolean selectedRuntimeHostConfigured() {
        return NativeLoaderRuntimeHostSupport.selectedRuntimeHostConfigured(runtimeHostContext);
    }

    public String selectedRuntimeHostId() {
        return NativeLoaderRuntimeHostSupport.selectedRuntimeHostId(runtimeHostContext);
    }

    public String runtimeMode() {
        return NativeLoaderRuntimeHostSupport.runtimeMode(runtimeHostContext);
    }

    public boolean clientCallerOnly() {
        return NativeLoaderRuntimeHostSupport.clientCallerOnly(runtimeHostContext);
    }

    public boolean standaloneMode() {
        return NativeLoaderRuntimeHostSupport.standaloneMode(runtimeHostContext);
    }

    public Object standaloneRuntimeHost() {
        return NativeLoaderRuntimeHostSupport.standaloneRuntimeHost(runtimeHostContext);
    }

    public String standaloneHostStatus(Object host, String methodName, Object... args) {
        return NativeLoaderRuntimeHostSupport.standaloneHostStatus(host, methodName, args);
    }

    public Object selectedRegisteredRuntimeHost() {
        return NativeLoaderRuntimeHostSupport.selectedRegisteredRuntimeHost(runtimeHostContext);
    }

    public Object selectedRegisteredRuntimeHostCapabilities() {
        return NativeLoaderRuntimeHostSupport.selectedRegisteredRuntimeHostCapabilities(runtimeHostContext);
    }

    public Object serverPlayer(Object player) {
        return NativeLoaderRuntimeHostSupport.serverPlayer(player, runtimeHostContext);
    }

    public Object serverLevel(Object level, Object serverPlayer) {
        return NativeLoaderRuntimeHostSupport.serverLevel(level, serverPlayer, runtimeHostContext);
    }

    public Object server(Object player, Object serverPlayer) {
        return NativeLoaderRuntimeHostSupport.server(player, serverPlayer, runtimeHostContext);
    }

    public boolean invokeOnServer(Object server, BooleanAction action) {
        return NativeLoaderRuntimeHostSupport.invokeOnServer(server, action == null ? null : action::run);
    }

    public boolean invokeForServerPlayer(Object level, Object player, ServerAction action) {
        return NativeLoaderRuntimeHostSupport.invokeForServerPlayer(
                level,
                player,
                action == null ? null : action::run,
                runtimeHostContext
        );
    }

    public boolean resultMutated(Object result) {
        return NativeLoaderRuntimeHostSupport.resultMutated(result);
    }

    public void putResultEvidence(Map<String, Object> evidence, Object result) {
        NativeLoaderRuntimeHostSupport.putResultEvidence(evidence, result);
    }

    public boolean invokeRealItemUse(Object level, Object player, Object handOrStack, String itemId) {
        return NativeLoaderAdapterCoreGameplayRuntimeActions.directRealItemUse(
                gameplayContext,
                level,
                player,
                handOrStack,
                itemId
        );
    }

    public Object minecraftInstance() {
        try {
            Class<?> minecraftClass = Class.forName(runtimeHostContext.runtimeClass().apply("client.Minecraft"));
            return minecraftClass.getMethod("getInstance").invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public Object minecraftField(Object minecraft, String fieldName) {
        try {
            return NativeLoaderClientReflectionSupport.optionalFieldValue(minecraft, fieldName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @FunctionalInterface
    public interface BooleanAction {
        boolean run() throws Throwable;
    }

    @FunctionalInterface
    public interface ServerAction {
        boolean run(Object serverPlayer, Object serverLevel) throws Throwable;
    }
}
