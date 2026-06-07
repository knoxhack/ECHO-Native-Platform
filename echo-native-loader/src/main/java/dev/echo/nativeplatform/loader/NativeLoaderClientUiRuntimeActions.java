package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NativeLoaderClientUiRuntimeActions {
    public static final String SERVICE_ID = "echo.native.client_ui_runtime_actions";

    private NativeLoaderClientUiRuntimeActions() {
    }

    public static Map<String, Object> executeTerminalCommand(Context context, String command, String output) {
        Map<String, Object> evidence = baseEventEvidence(context.terminalCommandAction(), context.commandExecutionEvent());
        Object runtimeHost = actionRuntimeHost(context);
        context.putSelectedRuntimeHostEvidence().accept(evidence, runtimeHost);
        if (runtimeHost == null) {
            context.putMissingNativeRuntimeHostEvidence().accept(evidence);
            return Map.copyOf(evidence);
        }
        if (!context.runtimeActionSupported().test(runtimeHost, context.terminalCommandAction())) {
            evidence.put("failureKind", "unsupported_runtime_action");
            return Map.copyOf(evidence);
        }
        String safeCommand = command == null ? "" : command.trim();
        String terminalRoute = context.uiDefaultContentId().apply("terminal.defaultRoute", context.targetForSurface().apply("TERMINAL"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", context.screenIdForSurface().apply("TERMINAL"));
        payload.put("command", safeCommand);
        payload.put("output", output == null ? "" : output);
        payload.put("eventName", context.commandExecutionEvent());
        payload.put("canonicalId", context.canonicalIdForSurface().apply("TERMINAL"));
        payload.put("terminalId", terminalRoute);
        payload.put("target", terminalRoute);
        payload.put("source", "native_ui_terminal");
        return publishEvent(
                context,
                evidence,
                runtimeHost,
                context.commandExecutionEvent(),
                payload,
                "native_client.terminal_command." + compactActionKey(safeCommand));
    }

    public static Map<String, Object> executeIndexSearch(Context context, String query, String output) {
        Map<String, Object> evidence = baseEventEvidence(context.indexSearchAction(), context.terminalOpenedEvent());
        Object runtimeHost = actionRuntimeHost(context);
        context.putSelectedRuntimeHostEvidence().accept(evidence, runtimeHost);
        if (runtimeHost == null) {
            context.putMissingNativeRuntimeHostEvidence().accept(evidence);
            return Map.copyOf(evidence);
        }
        if (!context.runtimeActionSupported().test(runtimeHost, context.indexSearchAction())) {
            evidence.put("failureKind", "unsupported_runtime_action");
            return Map.copyOf(evidence);
        }
        String safeQuery = query == null ? "" : query.trim();
        String indexTarget = context.targetForSurface().apply("INDEX");
        String terminalRoute = context.uiDefaultContentId().apply("terminal.defaultRoute", context.targetForSurface().apply("TERMINAL"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", context.screenIdForSurface().apply("INDEX"));
        payload.put("canonicalId", context.canonicalIdForSurface().apply("INDEX"));
        payload.put("terminalId", terminalRoute);
        payload.put("target", safeQuery.isBlank() ? indexTarget : indexTarget + "/" + compactActionKey(safeQuery));
        payload.put("blockId", context.targetForSurface().apply("TERMINAL"));
        payload.put("x", 3);
        payload.put("y", 4);
        payload.put("z", 3);
        payload.put("query", safeQuery);
        payload.put("output", output == null ? "" : output);
        payload.put("source", "native_ui_index");
        return publishEvent(
                context,
                evidence,
                runtimeHost,
                context.terminalOpenedEvent(),
                payload,
                "native_client.index_search." + compactActionKey(safeQuery));
    }

    public static Map<String, Object> executeHudRefresh(
            Context context,
            int health,
            String hazard,
            String mission,
            String cinematicCue
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", context.screenIdForSurface().apply("HUD"));
        payload.put("canonicalId", context.canonicalIdForSurface().apply("HUD"));
        payload.put("target", context.targetForSurface().apply("HUD"));
        payload.put("ticks", 1);
        payload.put("seconds", 1.0D);
        payload.put("moved", true);
        payload.put("health", Math.max(0, health));
        payload.put("hazard", hazard == null ? "" : hazard);
        payload.put("mission", mission == null ? "" : mission);
        payload.put("cinematicCue", cinematicCue == null ? "" : cinematicCue);
        payload.put("source", "native_ui_hud");
        return executeEvent(
                context,
                context.hudRefreshAction(),
                context.clientTickEvent(),
                "EchoNativeRuntimeHost.Events",
                "publish",
                payload,
                "native_client.hud_refresh." + compactActionKey(String.valueOf(payload.get("hazard"))));
    }

    public static Map<String, Object> executeMissionLogUpdate(
            Context context,
            String missionId,
            String missionTitle,
            String missionObjective,
            double missionProgress,
            String missionStatus,
            String missionUpdateLine
    ) {
        String safeMissionId = missionId == null || missionId.isBlank()
                ? context.canonicalIdForSurface().apply("MISSION_LOG")
                : missionId.trim();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", context.screenIdForSurface().apply("MISSION_LOG"));
        payload.put("canonicalId", safeMissionId);
        payload.put("target", safeMissionId);
        payload.put("missionId", safeMissionId);
        payload.put("missionTitle", missionTitle == null ? "" : missionTitle);
        payload.put("missionObjective", missionObjective == null ? "" : missionObjective);
        payload.put("missionProgress", missionProgress);
        payload.put("missionStatus", missionStatus == null ? "" : missionStatus);
        payload.put("missionUpdateLine", missionUpdateLine == null ? "" : missionUpdateLine);
        payload.put(
                "itemId",
                context.uiDefaultContentId().apply(
                        "missionLog.itemId",
                        context.canonicalIdForSurface().apply("MISSION_LOG")));
        payload.put("source", "native_ui_mission_log");
        return executeEvent(
                context,
                context.missionLogUpdateAction(),
                context.missionObjectiveCompletedEvent(),
                "EchoNativeRuntimeHost.Events",
                "publish",
                payload,
                "native_client.mission_log_update." + compactActionKey(safeMissionId));
    }

    public static Map<String, Object> executeSurfaceOpen(Context context, String surface, String effect) {
        String safeSurface = surface == null || surface.isBlank() ? "UNKNOWN" : surface.trim();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("screenId", context.screenIdForSurface().apply(safeSurface));
        payload.put("canonicalId", context.canonicalIdForSurface().apply(safeSurface));
        payload.put("target", context.targetForSurface().apply(safeSurface));
        payload.put("surface", safeSurface);
        payload.put("effect", effect == null ? "" : effect);
        payload.put("source", "native_ui_surface_open");
        return executeRuntimeEvent(context, context.surfaceOpenAction(), payload);
    }

    public static Map<String, Object> executeMachineSurfaceOpen(Context context, Map<String, Object> machineContext) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (machineContext != null) {
            payload.putAll(machineContext);
        }
        String defaultMachineScreenId = firstNonBlank(
                context.defaultMachineScreenId().get(),
                context.screenIdForSurface().apply("MACHINE"));
        String defaultMachineId = firstNonBlank(
                context.defaultMachineId().get(),
                context.targetForSurface().apply("MACHINE"));
        String effectPrefix = firstNonBlank(context.machineEffectPrefix().get(), "native_machine_screen.open");
        String machineId = String.valueOf(payload.getOrDefault("machineId",
                payload.getOrDefault("blockId", defaultMachineId)));
        String blockId = String.valueOf(payload.getOrDefault("blockId", machineId));
        payload.put("screenId", defaultMachineScreenId);
        payload.put("canonicalId", machineId);
        payload.put("target", machineId);
        payload.put("blockId", blockId);
        payload.put("surface", "MACHINE");
        payload.put("effect", effectPrefix + ":" + blockId);
        payload.put("source", "native_machine_block_use");
        return executeRuntimeEvent(context, context.surfaceOpenAction(), payload);
    }

    public static Map<String, Object> executeRuntimeEvent(Context context, String runtimeActionId, Map<String, Object> payload) {
        String safeRuntimeActionId = runtimeActionId == null ? "" : runtimeActionId.trim();
        Map<String, Object> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            safePayload.putAll(payload);
        }
        safePayload.put("runtimeActionId", safeRuntimeActionId);
        safePayload.put("eventName", safeRuntimeActionId);
        safePayload.putIfAbsent("source", "native_ui");
        String evidenceTarget = firstNonBlank(
                String.valueOf(safePayload.getOrDefault("target", "")),
                firstNonBlank(
                        String.valueOf(safePayload.getOrDefault("canonicalId", "")),
                        String.valueOf(safePayload.getOrDefault("screenId", "ui_action"))));
        return executeEvent(
                context,
                safeRuntimeActionId,
                safeRuntimeActionId,
                "EchoNativeRuntimeHost.Events",
                "publish",
                Map.copyOf(safePayload),
                "native_client." + compactActionKey(safeRuntimeActionId) + "." + compactActionKey(evidenceTarget));
    }

    public static Map<String, Object> executeSaveDataMutation(
            Context context,
            String runtimeActionId,
            String scope,
            String key,
            Map<String, Object> payload
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        String safeRuntimeActionId = runtimeActionId == null ? "" : runtimeActionId.trim();
        String safeScope = scope == null || scope.isBlank() ? "native_ui" : scope.trim();
        String safeKey = key == null || key.isBlank() ? "ui_action" : key.trim();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("runtimeActionId", safeRuntimeActionId);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.SaveData");
        evidence.put("nativeMethod", "write");
        evidence.put("scope", safeScope);
        evidence.put("key", safeKey);
        context.putSelectedRuntimeHostEvidence().accept(evidence, null);
        Object runtimeHost = actionRuntimeHost(context);
        context.putSelectedRuntimeHostEvidence().accept(evidence, runtimeHost);
        if (runtimeHost == null) {
            context.putMissingNativeRuntimeHostEvidence().accept(evidence);
            return Map.copyOf(evidence);
        }
        if (!context.runtimeActionSupported().test(runtimeHost, safeRuntimeActionId)) {
            evidence.put("failureKind", "unsupported_runtime_action");
            return Map.copyOf(evidence);
        }
        Map<String, Object> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            safePayload.putAll(payload);
        }
        safePayload.put("runtimeActionId", safeRuntimeActionId);
        safePayload.putIfAbsent("source", "native_ui");
        try {
            Object result = context.writeSaveData().write(
                    runtimeHost,
                    safeScope,
                    safeKey,
                    Map.copyOf(safePayload),
                    "native_client." + compactActionKey(safeRuntimeActionId) + "." + compactActionKey(safeKey));
            context.putNativeResultEvidence().accept(evidence, result);
            return Map.copyOf(evidence);
        } catch (Throwable exception) {
            evidence.put("failureKind", exception.getClass().getSimpleName());
            evidence.put("message", exception.getMessage() == null ? "" : exception.getMessage());
            return Map.copyOf(evidence);
        }
    }

    public static boolean hostActionGateActive() {
        return true;
    }

    public static List<String> supportedActionIds(Context context) {
        Object runtimeHost = context.runtimeHost().get();
        Object grantRuntimeHost = context.grantRuntimeHost().apply(runtimeHost);
        Object actionRuntimeHost = context.actionRuntimeHost().apply(runtimeHost);
        List<String> actions = new ArrayList<>();
        if (context.runtimeSurfaceSupported().test(actionRuntimeHost, "events")
                && context.runtimeActionSupported().test(actionRuntimeHost, context.scannerUsedAction())) {
            actions.add(context.scannerUsedAction());
            actions.add(context.useScannerAction());
        }
        if (context.runtimeSurfaceSupported().test(grantRuntimeHost, "playerInventory")
                && context.runtimeActionSupported().test(grantRuntimeHost, context.grantItemAction())) {
            actions.add(context.grantItemAction());
        }
        addEventAction(context, actions, actionRuntimeHost, context.terminalCommandAction());
        addEventAction(context, actions, actionRuntimeHost, context.indexSearchAction());
        addEventAction(context, actions, actionRuntimeHost, context.hudRefreshAction());
        addEventAction(context, actions, actionRuntimeHost, context.missionLogUpdateAction());
        addEventAction(context, actions, actionRuntimeHost, context.surfaceOpenAction());
        addEventAction(context, actions, actionRuntimeHost, context.indexBookmarkAction());
        addEventAction(context, actions, actionRuntimeHost, context.holoMapStateAction());
        addEventAction(context, actions, actionRuntimeHost, context.signalOsTerminalAction());
        addEventAction(context, actions, actionRuntimeHost, context.productCommandAction());
        addEventAction(context, actions, actionRuntimeHost, "native.ui.ashfall_drone_command");
        return List.copyOf(actions);
    }

    private static void addEventAction(Context context, List<String> actions, Object runtimeHost, String actionId) {
        if (context.runtimeSurfaceSupported().test(runtimeHost, "events")
                && context.runtimeActionSupported().test(runtimeHost, actionId)) {
            actions.add(actionId);
        }
    }

    private static Map<String, Object> executeEvent(
            Context context,
            String runtimeActionId,
            String eventName,
            String nativeInterface,
            String nativeMethod,
            Map<String, Object> payload,
            String idempotencyKey
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("runtimeActionId", runtimeActionId);
        evidence.put("eventName", eventName);
        evidence.put("nativeInterface", nativeInterface);
        evidence.put("nativeMethod", nativeMethod);
        context.putSelectedRuntimeHostEvidence().accept(evidence, null);
        Object runtimeHost = actionRuntimeHost(context);
        context.putSelectedRuntimeHostEvidence().accept(evidence, runtimeHost);
        if (runtimeHost == null) {
            context.putMissingNativeRuntimeHostEvidence().accept(evidence);
            return Map.copyOf(evidence);
        }
        if (!context.runtimeActionSupported().test(runtimeHost, runtimeActionId)) {
            evidence.put("failureKind", "unsupported_runtime_action");
            return Map.copyOf(evidence);
        }
        return publishEvent(context, evidence, runtimeHost, eventName, payload == null ? Map.of() : payload, idempotencyKey);
    }

    private static Map<String, Object> publishEvent(
            Context context,
            Map<String, Object> evidence,
            Object runtimeHost,
            String eventName,
            Map<String, Object> payload,
            String idempotencyKey
    ) {
        try {
            Object result = context.publishEvent().publish(runtimeHost, eventName, payload, idempotencyKey);
            context.putNativeResultEvidence().accept(evidence, result);
            return Map.copyOf(evidence);
        } catch (Throwable exception) {
            evidence.put("failureKind", exception.getClass().getSimpleName());
            evidence.put("message", exception.getMessage() == null ? "" : exception.getMessage());
            return Map.copyOf(evidence);
        }
    }

    private static Map<String, Object> baseEventEvidence(String runtimeActionId, String eventName) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("attempted", true);
        evidence.put("mutated", false);
        evidence.put("runtimeActionId", runtimeActionId);
        evidence.put("eventName", eventName);
        evidence.put("nativeInterface", "EchoNativeRuntimeHost.Events");
        evidence.put("nativeMethod", "publish");
        return evidence;
    }

    private static Object actionRuntimeHost(Context context) {
        Object runtimeHost = context.actionRuntimeHost().apply(context.runtimeHost().get());
        return runtimeHost;
    }

    private static String firstNonBlank(String first, String second) {
        String safeFirst = first == null ? "" : first.trim();
        return safeFirst.isBlank() ? second == null ? "" : second.trim() : safeFirst;
    }

    private static String compactActionKey(String value) {
        String key = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        key = key.replaceAll("_+", "_").replaceAll("^_+|_+$", "");
        return key.isBlank() ? "action" : key;
    }

    public record Context(
            String scannerUsedAction,
            String useScannerAction,
            String grantItemAction,
            String terminalCommandAction,
            String indexSearchAction,
            String hudRefreshAction,
            String missionLogUpdateAction,
            String surfaceOpenAction,
            String indexBookmarkAction,
            String holoMapStateAction,
            String signalOsTerminalAction,
            String productCommandAction,
            String commandExecutionEvent,
            String terminalOpenedEvent,
            String clientTickEvent,
            String missionObjectiveCompletedEvent,
            Function<String, String> screenIdForSurface,
            Function<String, String> canonicalIdForSurface,
            Function<String, String> targetForSurface,
            BiFunction<String, String, String> uiDefaultContentId,
            Supplier<String> defaultMachineScreenId,
            Supplier<String> defaultMachineId,
            Supplier<String> machineEffectPrefix,
            Supplier<Object> runtimeHost,
            Function<Object, Object> grantRuntimeHost,
            Function<Object, Object> actionRuntimeHost,
            BiPredicate<Object, String> runtimeSurfaceSupported,
            BiPredicate<Object, String> runtimeActionSupported,
            BiConsumer<Map<String, Object>, Object> putSelectedRuntimeHostEvidence,
            Consumer<Map<String, Object>> putMissingNativeRuntimeHostEvidence,
            BiConsumer<Map<String, Object>, Object> putNativeResultEvidence,
            EventPublisher publishEvent,
            SaveDataWriter writeSaveData
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

    public interface SaveDataWriter {
        Object write(
                Object runtimeHost,
                String scope,
                String key,
                Map<String, Object> payload,
                String idempotencyKey
        ) throws ReflectiveOperationException;
    }
}
