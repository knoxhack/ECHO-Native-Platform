package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5CameraCinematicSmoke {
    private EchoNativeAgent5CameraCinematicSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> frame = EchoNativeAgent5UiActionRouter.routeCameraCinematicFrame(Map.of("cinematicFrame", 0));
        Map<String, Object> surface = EchoNativeAgent5UiHandlerRegistry.renderSurface("HUD", frame);
        Map<String, Object> host = EchoNativeAgent5ScreenHostModel.render("HUD", frame, "ashfall", 12, 3, 2, 1);
        boolean passed = Boolean.TRUE.equals(frame.get("handled"))
                && "over_shoulder".equals(frame.get("cameraMode"))
                && Integer.valueOf(72).equals(frame.get("cameraFov"))
                && EchoNativeAgent5UiExpectedValues.terminal().get("title").equals(frame.get("cinematicCue"))
                && Integer.valueOf(1).equals(frame.get("cinematicFrame"))
                && Boolean.TRUE.equals(frame.get("cinematicLetterbox"))
                && ("camera_cinematic:frame:" + EchoNativeAgent5UiExpectedValues.terminal().get("title"))
                .equals(frame.get("effect"))
                && linesContain(surface, "Camera over_shoulder frame 1 cue "
                + EchoNativeAgent5UiExpectedValues.terminal().get("title"))
                && linesContain(surface, "Letterbox: active")
                && linesContain(host, "Camera over_shoulder frame 1 cue "
                + EchoNativeAgent5UiExpectedValues.terminal().get("title"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("cameraCinematicSmokeClass", EchoNativeAgent5CameraCinematicSmoke.class.getSimpleName());
        smoke.put("cameraMode", frame.get("cameraMode"));
        smoke.put("cameraFov", frame.get("cameraFov"));
        smoke.put("cameraTarget", frame.get("cameraTarget"));
        smoke.put("cinematicCue", frame.get("cinematicCue"));
        smoke.put("cinematicFrame", frame.get("cinematicFrame"));
        smoke.put("cinematicLetterbox", frame.get("cinematicLetterbox"));
        smoke.put("cinematicSubtitle", frame.get("cinematicSubtitle"));
        smoke.put("cinematicOutput", frame.get("cinematicOutput"));
        smoke.put("effect", frame.get("effect"));
        smoke.put("surfaceLines", surface.get("lines"));
        smoke.put("hostSurfaceLines", host.get("surfaceLines"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static boolean linesContain(Map<String, Object> model, String token) {
        Object value = model.get("lines");
        if (value == null) {
            value = model.get("surfaceLines");
        }
        if (value instanceof Iterable<?> lines) {
            for (Object line : lines) {
                if (String.valueOf(line).contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }
}
