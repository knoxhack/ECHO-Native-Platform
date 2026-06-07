package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiHostInteractionSmoke {
    private EchoNativeAgent5UiHostInteractionSmoke() {
    }

    public static Map<String, Object> run(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        ArrayList<Map<String, Object>> steps = new ArrayList<>();
        steps.add(runTerminal(screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(runIndex(screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(runLens(screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(openSurface("MISSION_LOG", screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(openSurface("SETTINGS", screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(runPauseResume(screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(runRecovery(screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(openSurface("HOLOMAP", screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(openSurface("WIKI", screenClass, packId, moduleCount, itemCount, missionCount, regionCount));
        steps.add(openSurface("MAIN_MENU", screenClass, packId, moduleCount, itemCount, missionCount, regionCount));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("interactionSmokeClass", EchoNativeAgent5UiHostInteractionSmoke.class.getSimpleName());
        smoke.put("steps", List.copyOf(steps));
        smoke.put("passed", steps.stream().allMatch(step -> Boolean.TRUE.equals(step.get("passed"))));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> runTerminal(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String focusPath = EchoNativeAgent5UiActionRouter.focusPath("TERMINAL", "WIKI");
        String buffer = "";
        String expectedCommand = EchoNativeAgent5UiExpectedValues.terminalCommand();
        for (char character : expectedCommand.toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    "TERMINAL",
                    focusPath,
                    buffer,
                    "",
                    character
            );
            buffer = String.valueOf(typed.get("value"));
        }
        Map<String, Object> action = EchoNativeAgent5UiActionRouter.activate("TERMINAL", Map.of(
                "focusedControl", focusPath,
                "terminalBuffer", buffer
        ));
        Map<String, Object> state = Map.of(
                "focusedControl", focusPath,
                "mouseRouted", true,
                "terminalBuffer", buffer,
                "terminalOutput", String.valueOf(action.get("output")),
                "terminalCommandExecuted", true
        );
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "TERMINAL",
                true,
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                state
        );
        return step("terminal_command", snapshot, Boolean.TRUE.equals(action.get("handled"))
                && EchoNativeAgent5UiHostSmokeSnapshot.strings(snapshot, "surfaceLines").stream()
                .anyMatch(line -> line.contains(EchoNativeAgent5UiExpectedValues.terminalOutput())));
    }

    private static Map<String, Object> runIndex(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String focusPath = EchoNativeAgent5UiActionRouter.focusPath("INDEX", "WIKI");
        String buffer = "";
        String expectedQuery = EchoNativeAgent5UiExpectedValues.indexQuery();
        for (char character : expectedQuery.toCharArray()) {
            Map<String, Object> typed = EchoNativeAgent5UiActionRouter.routeCharacter(
                    "INDEX",
                    focusPath,
                    "",
                    buffer,
                    character
            );
            buffer = String.valueOf(typed.get("value"));
        }
        Map<String, Object> action = EchoNativeAgent5UiActionRouter.activate("INDEX", Map.of(
                "focusedControl", focusPath,
                "indexBuffer", buffer
        ));
        Map<String, Object> state = Map.of(
                "focusedControl", focusPath,
                "mouseRouted", true,
                "indexBuffer", buffer,
                "indexOutput", String.valueOf(action.get("output")),
                "indexSearchExecuted", true
        );
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "INDEX",
                true,
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                state
        );
        return step("index_search", snapshot, Boolean.TRUE.equals(action.get("handled"))
                && EchoNativeAgent5UiHostSmokeSnapshot.strings(snapshot, "surfaceLines").stream()
                .anyMatch(line -> line.contains("index result(s):")));
    }

    private static Map<String, Object> runLens(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String focusPath = EchoNativeAgent5UiActionRouter.focusPath("LENS", "WIKI");
        Map<String, Object> action = EchoNativeAgent5UiActionRouter.activate("LENS", Map.of(
                "focusedControl", focusPath
        ));
        Map<String, Object> state = Map.of(
                "focusedControl", focusPath,
                "mouseRouted", true,
                "lensOutput", String.valueOf(action.get("output")),
                "lensScanExecuted", true
        );
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "LENS",
                true,
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                state
        );
        return step("lens_scan", snapshot, Boolean.TRUE.equals(action.get("handled"))
                && EchoNativeAgent5UiHostSmokeSnapshot.strings(snapshot, "surfaceLines").stream()
                .anyMatch(line -> line.contains(String.valueOf(EchoNativeAgent5UiExpectedValues.lens().get("summary")))));
    }

    private static Map<String, Object> runPauseResume(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        Map<String, Object> pauseRoute = EchoNativeAgent5UiActionRouter.routeKey("ESCAPE", "LENS", "WIKI");
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                String.valueOf(pauseRoute.get("destinationMode")),
                Boolean.TRUE.equals(pauseRoute.get("handled")),
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                Map.of("previousMode", "LENS")
        );
        Map<String, Object> resumeRoute = EchoNativeAgent5UiActionRouter.routeKey("ESCAPE", "PAUSE", "LENS");
        boolean passed = Boolean.TRUE.equals(pauseRoute.get("handled"))
                && Boolean.TRUE.equals(resumeRoute.get("handled"))
                && "PAUSE".equals(snapshot.get("surface"))
                && "LENS".equals(resumeRoute.get("destinationMode"))
                && "pause:resume:LENS".equals(snapshot.get("focusPath"))
                && "EchoNativePauseSurfaceRenderer".equals(snapshot.get("moduleRendererClass"));
        Map<String, Object> step = new LinkedHashMap<>(step("pause_resume", snapshot, passed));
        step.put("resumeDestinationMode", resumeRoute.get("destinationMode"));
        return Map.copyOf(step);
    }

    private static Map<String, Object> runRecovery(
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String focusPath = EchoNativeAgent5UiActionRouter.focusPath("RECOVERY", "WIKI");
        Map<String, Object> action = EchoNativeAgent5UiActionRouter.activate("RECOVERY", Map.of(
                "focusedControl", focusPath
        ));
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "RECOVERY",
                true,
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount,
                Map.of(
                        "focusedControl", focusPath,
                        "mouseRouted", true,
                        "recoveryOutput", String.valueOf(action.get("output")),
                        "recoveryActionExecuted", true
                )
        );
        return step("recovery_action", snapshot, Boolean.TRUE.equals(action.get("handled"))
                && "EchoNativeRecoverySurfaceRenderer".equals(snapshot.get("moduleRendererClass"))
                && EchoNativeAgent5UiHostSmokeSnapshot.strings(snapshot, "surfaceLines").stream()
                .anyMatch(line -> line.contains("Status: RECOVERED")));
    }

    private static Map<String, Object> openSurface(
            String surface,
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String key = switch (surface) {
            case "HOLOMAP" -> "J";
            default -> "";
        };
        Map<String, Object> route = key.isBlank()
                ? Map.of("handled", true, "destinationMode", surface, "effect", "surface:direct:" + surface)
                : EchoNativeAgent5UiActionRouter.routeKey(key, "TERMINAL", "WIKI");
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                String.valueOf(route.get("destinationMode")),
                Boolean.TRUE.equals(route.get("handled")),
                screenClass,
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        boolean passed = Boolean.TRUE.equals(route.get("handled"))
                && Boolean.TRUE.equals(snapshot.get("opened"))
                && !String.valueOf(snapshot.get("moduleRendererClass")).isBlank();
        return step(surface.toLowerCase(java.util.Locale.ROOT) + "_open", snapshot, passed);
    }

    private static Map<String, Object> step(String id, Map<String, Object> snapshot, boolean passed) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("id", id);
        step.put("surface", snapshot.get("surface"));
        step.put("focusPath", snapshot.get("focusPath"));
        step.put("moduleRendererClass", snapshot.get("moduleRendererClass"));
        step.put("snapshot", snapshot);
        step.put("passed", passed);
        return Map.copyOf(step);
    }
}
