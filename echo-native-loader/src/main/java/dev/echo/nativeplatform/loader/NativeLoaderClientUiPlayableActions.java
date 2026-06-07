package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class NativeLoaderClientUiPlayableActions {
    public static final String SERVICE_ID = "echo.native.client_ui_playable_actions";

    private NativeLoaderClientUiPlayableActions() {
    }

    public static boolean mutationAccepted(Map<String, Object> evidence) {
        Map<String, Object> result = evidence == null ? Map.of() : evidence;
        return Boolean.TRUE.equals(result.get("mutated"))
                && Boolean.TRUE.equals(result.get("saveTouched"))
                && Boolean.TRUE.equals(result.get("missionUpdated"))
                && Boolean.TRUE.equals(result.get("feedbackEmitted"));
    }

    public static Map<String, Object> useScanner(Context context) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("runtimeActionId", context.scannerUsedAction());
        evidence.put("eventName", context.scannerUsedAction());
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.Events");
        evidence.put("nativeMethod", "publish");
        context.putSelectedRuntimeHostEvidence().accept(evidence, null);
        if (context.selectedRuntimeHostConfigured().get()) {
            Object selectedRuntimeHost = context.selectedRegisteredRuntimeHost().get();
            context.putSelectedRuntimeHostEvidence().accept(evidence, selectedRuntimeHost);
            if (selectedRuntimeHost == null) {
                evidence.put("failureKind", "missing_selected_runtime_host");
                return Map.copyOf(evidence);
            }
            if (!context.runtimeSurfaceSupported().test(selectedRuntimeHost, "events")) {
                evidence.put("failureKind", "missing_events_host");
                return Map.copyOf(evidence);
            }
            if (!context.runtimeActionSupported().test(selectedRuntimeHost, context.scannerUsedAction())) {
                evidence.put("failureKind", "unsupported_runtime_action");
                return Map.copyOf(evidence);
            }
            try {
                Object result = context.publishEvent().publish(
                        selectedRuntimeHost,
                        context.scannerUsedAction(),
                        scannerUsePayload(context, "native_ui_scanner", false),
                        "native_client.scanner_used." + context.productPath().apply(context.recoveryItemId().get()));
                context.putNativeResultEvidence().accept(evidence, result);
                return Map.copyOf(evidence);
            } catch (Throwable exception) {
                evidence.put("failureKind", exception.getClass().getSimpleName());
                evidence.put("message", exception.getMessage() == null ? "" : exception.getMessage());
                return Map.copyOf(evidence);
            }
        }
        if (context.clientCallerOnly().get()) {
            evidence.put("failureKind", "native_client_caller_no_host");
            evidence.put("message", "Native client is caller-only mode and no runtime host is configured.");
            return Map.copyOf(evidence);
        }
        if (context.standaloneMode().get()) {
            Object host = context.standaloneRuntimeHost().get();
            if (host == null) {
                evidence.put("failureKind", "missing_standalone_host");
                return Map.copyOf(evidence);
            }
            String status = context.standaloneHostStatus().status(
                    host,
                    "emitEvent",
                    context.scannerUsedAction(),
                    context.productPath().apply(context.recoveryItemId().get())
            );
            putStandaloneMutation(evidence, status);
            return Map.copyOf(evidence);
        }
        Object minecraft = context.minecraftInstance().get();
        Object player = context.minecraftField().apply(minecraft, "player");
        Object level = context.minecraftField().apply(minecraft, "level");
        try {
            Object result = context.scannerUseResult().scan(
                    level,
                    player,
                    context.productPath().apply(context.recoveryItemId().get()),
                    false);
            context.putNativeResultEvidence().accept(evidence, result);
            return Map.copyOf(evidence);
        } catch (Throwable exception) {
            evidence.put("failureKind", exception.getClass().getSimpleName());
            evidence.put("message", exception.getMessage() == null ? "" : exception.getMessage());
            return Map.copyOf(evidence);
        }
    }

    public static Map<String, Object> grantItem(Context context, String itemId, int count) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("runtimeActionId", context.grantItemAction());
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.PlayerInventory");
        evidence.put("nativeMethod", "grant");
        evidence.put("requestedItemId", itemId == null ? "" : itemId);
        evidence.put("requestedCount", Math.max(1, count));
        context.putSelectedRuntimeHostEvidence().accept(evidence, null);
        if (context.selectedRuntimeHostConfigured().get()) {
            Object selectedRuntimeHost = context.selectedRegisteredRuntimeHost().get();
            context.putSelectedRuntimeHostEvidence().accept(evidence, selectedRuntimeHost);
            if (selectedRuntimeHost == null) {
                evidence.put("failureKind", "missing_selected_runtime_host");
                return Map.copyOf(evidence);
            }
            if (!context.runtimeActionSupported().test(selectedRuntimeHost, context.grantItemAction())) {
                evidence.put("failureKind", "unsupported_runtime_action");
                return Map.copyOf(evidence);
            }
            try {
                String resolvedItemId = context.resolveRuntimeItemId().apply(selectedRuntimeHost, itemId);
                evidence.put("itemId", resolvedItemId);
                if (resolvedItemId.isBlank()) {
                    evidence.put("failureKind", "missing_item_id");
                    return Map.copyOf(evidence);
                }
                Object result = context.grantItemResult().grant(selectedRuntimeHost, resolvedItemId, Math.max(1, count));
                context.putNativeResultEvidence().accept(evidence, result);
                return Map.copyOf(evidence);
            } catch (Throwable exception) {
                evidence.put("failureKind", exception.getClass().getSimpleName());
                evidence.put("message", exception.getMessage() == null ? "" : exception.getMessage());
                return Map.copyOf(evidence);
            }
        }
        if (context.clientCallerOnly().get()) {
            evidence.put("failureKind", "native_client_caller_no_host");
            evidence.put("message", "Native client is caller-only mode and no runtime host is configured.");
            return Map.copyOf(evidence);
        }
        if (context.standaloneMode().get()) {
            Object host = context.standaloneRuntimeHost().get();
            if (host == null) {
                evidence.put("failureKind", "missing_standalone_host");
                return Map.copyOf(evidence);
            }
            String resolvedItemId = itemId == null || itemId.isBlank() ? "" : itemId;
            evidence.put("itemId", resolvedItemId);
            if (resolvedItemId.isBlank()) {
                evidence.put("failureKind", "missing_item_id");
                return Map.copyOf(evidence);
            }
            String status = context.standaloneHostStatus().status(
                    host,
                    "grantItem",
                    "player:standalone",
                    resolvedItemId,
                    Math.max(1, count)
            );
            putStandaloneMutation(evidence, status);
            return Map.copyOf(evidence);
        }
        Object minecraft = context.minecraftInstance().get();
        Object player = context.minecraftField().apply(minecraft, "player");
        if (player == null || itemId == null || itemId.isBlank()) {
            evidence.put("failureKind", "missing_player_or_item");
            return Map.copyOf(evidence);
        }
        try {
            String resolvedItemId = context.resolveItemId().apply(itemId);
            evidence.put("itemId", resolvedItemId);
            if (resolvedItemId.isBlank()) {
                evidence.put("failureKind", "missing_item_id");
                return Map.copyOf(evidence);
            }
            Object serverPlayer = context.runtimeServerPlayer().apply(player);
            Object serverLevel = context.runtimeServerLevel().apply(null, serverPlayer);
            Object server = context.runtimeServer().apply(player, serverPlayer);
            boolean mutated = context.invokeOnServer().invoke(server, () -> {
                Object runtimeHost = context.runtimeHost().apply(serverPlayer, serverLevel);
                context.putNativeRuntimeHostEvidence().accept(evidence, runtimeHost);
                Object result = context.grantItemResult().grant(runtimeHost, resolvedItemId, Math.max(1, count));
                context.putNativeResultEvidence().accept(evidence, result);
                return context.runtimeResultMutated().test(result);
            });
            evidence.put("mutated", mutated && Boolean.TRUE.equals(evidence.get("mutated")));
            return Map.copyOf(evidence);
        } catch (Throwable exception) {
            evidence.put("failureKind", exception.getClass().getSimpleName());
            evidence.put("message", exception.getMessage() == null ? "" : exception.getMessage());
            return Map.copyOf(evidence);
        }
    }

    private static Map<String, Object> scannerUsePayload(Context context, String source, boolean deepScan) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String scanTarget = context.uiDefaultContentId().apply("scanner.scanTarget", context.targetForSurface().apply("LENS"));
        payload.put("screenId", context.screenIdForSurface().apply("LENS"));
        payload.put("canonicalId", context.recoveryItemId().get());
        payload.put("target", scanTarget);
        payload.put("scanTarget", scanTarget);
        payload.put("itemId", context.recoveryItemId().get());
        payload.put("source", source == null || source.isBlank() ? "native_ui_scanner" : source);
        payload.put("deepScan", deepScan);
        payload.put("signalFound", true);
        payload.put("runtimeFeedback", true);
        payload.put("runtimePoiDiscovery", true);
        payload.put("eventName", context.scannerUsedAction());
        return Map.copyOf(payload);
    }

    private static void putStandaloneMutation(Map<String, Object> evidence, String status) {
        evidence.put("mutated", "MUTATED".equals(status));
        evidence.put("saveTouched", true);
        evidence.put("missionUpdated", true);
        evidence.put("feedbackEmitted", true);
    }

    public record Context(
            String scannerUsedAction,
            String grantItemAction,
            Supplier<String> recoveryItemId,
            Function<String, String> productPath,
            Function<String, String> screenIdForSurface,
            Function<String, String> targetForSurface,
            BiFunction<String, String, String> uiDefaultContentId,
            Supplier<Boolean> selectedRuntimeHostConfigured,
            Supplier<Object> selectedRegisteredRuntimeHost,
            BiPredicate<Object, String> runtimeSurfaceSupported,
            BiPredicate<Object, String> runtimeActionSupported,
            BiConsumer<Map<String, Object>, Object> putSelectedRuntimeHostEvidence,
            EventPublisher publishEvent,
            BiConsumer<Map<String, Object>, Object> putNativeResultEvidence,
            Supplier<Boolean> clientCallerOnly,
            Supplier<Boolean> standaloneMode,
            Supplier<Object> standaloneRuntimeHost,
            StandaloneHostStatus standaloneHostStatus,
            Supplier<Object> minecraftInstance,
            BiFunction<Object, String, Object> minecraftField,
            ScannerUseResult scannerUseResult,
            BiFunction<Object, String, String> resolveRuntimeItemId,
            Function<String, String> resolveItemId,
            Function<Object, Object> runtimeServerPlayer,
            BiFunction<Object, Object, Object> runtimeServerLevel,
            BiFunction<Object, Object, Object> runtimeServer,
            ServerInvoker invokeOnServer,
            BiFunction<Object, Object, Object> runtimeHost,
            BiConsumer<Map<String, Object>, Object> putNativeRuntimeHostEvidence,
            GrantItemResult grantItemResult,
            Predicate<Object> runtimeResultMutated
    ) {
    }

    public interface EventPublisher {
        Object publish(
                Object runtimeHost,
                String eventId,
                Map<String, Object> payload,
                String idempotencyKey
        ) throws ReflectiveOperationException;
    }

    public interface ScannerUseResult {
        Object scan(Object level, Object player, String source, boolean deepScan) throws Throwable;
    }

    public interface GrantItemResult {
        Object grant(Object runtimeHost, String itemId, int count) throws ReflectiveOperationException;
    }

    public interface StandaloneHostStatus {
        String status(Object host, String methodName, Object... args);
    }

    public interface ServerInvoker {
        boolean invoke(Object server, ServerAction action);
    }

    public interface ServerAction {
        boolean run() throws Throwable;
    }
}
