package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveVisualFrameAcceptance {
    private EchoNativeAgent5LiveVisualFrameAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> themeApplicationSmoke,
            Map<String, Object> renderCoreLayoutSmoke,
            Map<String, Object> cameraCinematicSmoke,
            Map<String, Object> hudOverlaySmoke
    ) {
        Map<String, Object> theme = themeApplicationSmoke == null ? Map.of() : themeApplicationSmoke;
        Map<String, Object> layout = renderCoreLayoutSmoke == null ? Map.of() : renderCoreLayoutSmoke;
        Map<String, Object> camera = cameraCinematicSmoke == null ? Map.of() : cameraCinematicSmoke;
        Map<String, Object> hud = hudOverlaySmoke == null ? Map.of() : hudOverlaySmoke;
        boolean themeAccepted = Boolean.TRUE.equals(theme.get("passed"))
                && "EchoNativeAgent5ThemeApplicationSmoke".equals(theme.get("themeApplicationSmokeClass"))
                && "echo_native:loader_blue_console".equals(theme.get("nativeLoaderThemeId"))
                && "loader_default".equals(theme.get("nativeLoaderThemeMode"))
                && "ashfall-accessible".equals(theme.get("settingsProfile"))
                && "keyboard_mouse".equals(theme.get("inputMode"))
                && "ECHO>".equals(object(theme.get("tokens")).get("terminal.prompt"));
        boolean layoutAccepted = Boolean.TRUE.equals(layout.get("passed"))
                && "EchoNativeAgent5RenderCoreLayoutSmoke".equals(layout.get("renderCoreLayoutSmokeClass"))
                && Integer.valueOf(620).equals(layout.get("desktopPanelW"))
                && Integer.valueOf(300).equals(layout.get("compactPanelW"))
                && intValue(layout.get("compactTextMaxWidth")) >= 80
                && intValue(layout.get("compactBodyLinesRendered")) <= 12;
        boolean cameraAccepted = Boolean.TRUE.equals(camera.get("passed"))
                && "EchoNativeAgent5CameraCinematicSmoke".equals(camera.get("cameraCinematicSmokeClass"))
                && "over_shoulder".equals(camera.get("cameraMode"))
                && Integer.valueOf(72).equals(camera.get("cameraFov"))
                && EchoNativeAgent5UiExpectedValues.terminal().get("title").equals(camera.get("cinematicCue"))
                && Integer.valueOf(1).equals(camera.get("cinematicFrame"))
                && Boolean.TRUE.equals(camera.get("cinematicLetterbox"))
                && ("camera_cinematic:frame:" + EchoNativeAgent5UiExpectedValues.terminal().get("title"))
                        .equals(camera.get("effect"));
        boolean hudAccepted = Boolean.TRUE.equals(hud.get("passed"))
                && "EchoNativeAgent5HudOverlaySmoke".equals(hud.get("hudOverlaySmokeClass"))
                && "echohudcore:hud".equals(hud.get("overlayLayerId"))
                && "hud:passive".equals(hud.get("trigger"))
                && Boolean.TRUE.equals(hud.get("clientUiHostAttached"))
                && Boolean.TRUE.equals(hud.get("overlayRendered"))
                && "top_left_safe_area".equals(hud.get("notificationAnchor"))
                && strings(hud, "overlayLines").stream().anyMatch(line -> line.contains(
                        "Health " + EchoNativeAgent5UiExpectedValues.hud().get("health")))
                && strings(hud, "overlayLines").stream().anyMatch(line -> line.contains("Anchor top_left_safe_area"));
        boolean accepted = themeAccepted && layoutAccepted && cameraAccepted && hudAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("themeAccepted", themeAccepted);
        result.put("layoutAccepted", layoutAccepted);
        result.put("cameraAccepted", cameraAccepted);
        result.put("hudAccepted", hudAccepted);
        result.put("themeId", String.valueOf(theme.getOrDefault("nativeLoaderThemeId", "")));
        result.put("desktopPanelW", layout.getOrDefault("desktopPanelW", 0));
        result.put("compactPanelW", layout.getOrDefault("compactPanelW", 0));
        result.put("cameraMode", String.valueOf(camera.getOrDefault("cameraMode", "")));
        result.put("cinematicCue", String.valueOf(camera.getOrDefault("cinematicCue", "")));
        result.put("overlayLayerId", String.valueOf(hud.getOrDefault("overlayLayerId", "")));
        result.put("effect", accepted
                ? "live_visual_frame:accepted:theme/render/camera/hud"
                : "live_visual_frame:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
