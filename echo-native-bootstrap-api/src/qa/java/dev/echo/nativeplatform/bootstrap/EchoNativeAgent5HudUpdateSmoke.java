package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5HudUpdateSmoke {
    private EchoNativeAgent5HudUpdateSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> update = EchoNativeAgent5UiActionRouter.routeHudUpdate(Map.of("hudHealth", 92));
        Map<String, Object> state = Map.of(
                "hudHealth", update.get("hudHealth"),
                "hudHazard", update.get("hudHazard"),
                "hudMission", update.get("hudMission"),
                "hudUpdateOutput", update.get("hudUpdateOutput")
        );
        Map<String, Object> surface = EchoNativeAgent5SurfaceRenderer.render(
                "HUD",
                state,
                EchoNativeAgent5UiHandlerRegistry.dataSources()
        );
        Map<String, Object> host = EchoNativeAgent5ScreenHostModel.render(
                "HUD",
                state,
                "ashfall",
                12,
                3,
                2,
                1
        );
        Map<String, Object> hudMutation = EchoNativeBootstrapMain.executeNativeHudRefreshFromUi(
                integer(update.get("hudHealth")),
                String.valueOf(update.get("hudHazard")),
                String.valueOf(update.get("hudMission")),
                String.valueOf(EchoNativeAgent5UiExpectedValues.terminal().get("title"))
        );
        boolean runtimeMutationAccepted = runtimeMutationAccepted(
                hudMutation,
                "native.ui.hud_refresh",
                "client_tick"
        );

        boolean passed = Boolean.TRUE.equals(update.get("handled"))
                && Integer.valueOf(85).equals(update.get("hudHealth"))
                && String.valueOf(EchoNativeAgent5UiExpectedValues.hud().get("hazard")).equals(update.get("hudHazard"))
                && "hud:update:health_hazard_mission".equals(update.get("effect"))
                && runtimeMutationAccepted
                && lines(surface).stream().anyMatch(line -> line.contains("HUD: Health 85"))
                && lines(surface).stream().anyMatch(line -> line.contains("Hazard: " + update.get("hudHazard")))
                && list(host, "headerLines").stream().anyMatch(line -> line.contains(
                        "HUD: Health 85 / " + update.get("hudHazard")));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("hudUpdateSmokeClass", EchoNativeAgent5HudUpdateSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("hudHealth", update.get("hudHealth"));
        smoke.put("hudHazard", update.get("hudHazard"));
        smoke.put("hudMission", update.get("hudMission"));
        smoke.put("effect", update.get("effect"));
        smoke.put("surfaceLines", lines(surface));
        smoke.put("hostHeaderLines", list(host, "headerLines"));
        smoke.put("hudMutation", hudMutation);
        smoke.put("runtimeMutationAccepted", runtimeMutationAccepted);
        smoke.put("runtimeActionId", String.valueOf(hudMutation.getOrDefault("runtimeActionId", "")));
        smoke.put("eventName", String.valueOf(hudMutation.getOrDefault("eventName", "")));
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> rendered) {
        Object value = rendered.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Map<String, Object> model, String key) {
        Object value = model.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static boolean runtimeMutationAccepted(
            Map<String, Object> evidence,
            String runtimeActionId,
            String eventName
    ) {
        Map<String, Object> result = evidence == null ? Map.of() : evidence;
        return Boolean.TRUE.equals(result.get("mutated"))
                && Boolean.TRUE.equals(result.get("saveTouched"))
                && Boolean.TRUE.equals(result.get("missionUpdated"))
                && Boolean.TRUE.equals(result.get("feedbackEmitted"))
                && runtimeActionId.equals(result.get("runtimeActionId"))
                && eventName.equals(result.get("eventName"));
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
