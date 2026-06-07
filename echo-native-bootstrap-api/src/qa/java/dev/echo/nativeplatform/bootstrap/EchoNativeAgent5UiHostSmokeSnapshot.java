package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiHostSmokeSnapshot {
    private EchoNativeAgent5UiHostSmokeSnapshot() {
    }

    public static Map<String, Object> capture(
            String surface,
            boolean opened,
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        return capture(
                surface,
                opened,
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                stateFor(normalizeSurface(surface))
        );
    }

    public static Map<String, Object> capture(
            String surface,
            boolean opened,
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount,
            Map<String, Object> state
    ) {
        String normalizedSurface = normalizeSurface(surface);
        Map<String, Object> hostModel = EchoNativeAgent5ScreenHostModel.render(
                normalizedSurface,
                state,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        Map<String, Object> surfaceModel = EchoNativeAgent5UiHandlerRegistry.renderSurface(normalizedSurface, state);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("surface", normalizedSurface);
        snapshot.put("opened", opened);
        snapshot.put("screenClass", screenClass);
        snapshot.put("screenTitle", hostModel.get("screenTitle"));
        snapshot.put("headerLines", hostModel.get("headerLines"));
        snapshot.put("surfaceLines", hostModel.get("surfaceLines"));
        snapshot.put("footerLine", hostModel.get("footerLine"));
        snapshot.put("hudValues", hostModel.get("hudValues"));
        snapshot.put("notificationAnchor", hostModel.get("notificationAnchor"));
        snapshot.put("focusPath", surfaceModel.get("focusPath"));
        snapshot.put("moduleRendererClass", surfaceModel.get("moduleRendererClass"));
        snapshot.put("hostModelClass", hostModel.get("hostModelClass"));
        snapshot.put("adapterCoreBridge", true);
        snapshot.put("serviceCodeExecuted", true);
        snapshot.put("snapshotClass", EchoNativeAgent5UiHostSmokeSnapshot.class.getSimpleName());
        return Map.copyOf(snapshot);
    }

    private static Map<String, Object> stateFor(String surface) {
        return switch (surface) {
            case "TERMINAL" -> EchoNativeAgent5UiExpectedValues.terminalState();
            case "INDEX" -> EchoNativeAgent5UiExpectedValues.indexState();
            case "LENS" -> EchoNativeAgent5UiExpectedValues.lensState();
            case "RECOVERY" -> EchoNativeAgent5UiExpectedValues.recoveryState();
            case "PAUSE" -> Map.of("previousMode", "WIKI");
            default -> Map.of();
        };
    }

    @SuppressWarnings("unchecked")
    public static List<String> strings(Map<String, Object> snapshot, String key) {
        Object value = snapshot.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static String normalizeSurface(String surface) {
        String normalized = surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }
}
