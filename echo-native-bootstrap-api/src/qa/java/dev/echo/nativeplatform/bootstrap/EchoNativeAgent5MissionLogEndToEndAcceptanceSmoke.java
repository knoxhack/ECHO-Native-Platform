package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke {
    private EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> missionAction = routeAction("MISSION_ACTION", "MISSION_LOG", "mission.open");
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "MISSION_LOG",
                "MISSION_LOG"
        );
        Map<String, Object> menuInput = menuInput("MISSION_LOG");
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "MISSION_LOG",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> render = EchoNativeAgent5LiveSurfaceRenderAcceptance.assess(liveSurface, snapshot);
        Map<String, Object> interaction = EchoNativeAgent5UiHostInteractionSmoke.run(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> update = EchoNativeAgent5MissionLogUpdateSmoke.capture();
        Map<String, Object> accepted = EchoNativeAgent5MissionLogEndToEndAcceptance.assess(
                missionAction,
                menuInput,
                render,
                interaction,
                update
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5MissionLogEndToEndAcceptance.assess(
                missionAction,
                Map.of("accepted", false, "surface", "MISSION_LOG"),
                render,
                interaction,
                update
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5MissionLogEndToEndAcceptance.assess(
                missionAction,
                menuInput,
                Map.of("accepted", false, "surface", "MISSION_LOG"),
                interaction,
                update
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5MissionLogEndToEndAcceptance.assess(
                missionAction,
                menuInput,
                render,
                Map.of("passed", false, "steps", java.util.List.of()),
                update
        );
        Map<String, Object> rejectedNoUpdate = EchoNativeAgent5MissionLogEndToEndAcceptance.assess(
                missionAction,
                menuInput,
                render,
                interaction,
                Map.of("passed", false)
        );
        Map<String, Object> rejectedNoMutation = EchoNativeAgent5MissionLogEndToEndAcceptance.assess(
                missionAction,
                menuInput,
                render,
                interaction,
                withoutRuntimeMutation(update)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "mission_log_end_to_end:MISSION_ACTION->MISSION_LOG:secure_crash_outpost:UPDATED".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoUpdate.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMutation.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("missionLogEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedNoInteraction", rejectedNoInteraction);
        smoke.put("rejectedNoUpdate", rejectedNoUpdate);
        smoke.put("rejectedNoMutation", rejectedNoMutation);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> routeAction(String key, String surface, String action) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("handled", true);
        route.put("key", key);
        route.put("surface", surface);
        route.put("action", action);
        route.put("source", "menu_action");
        return Map.copyOf(route);
    }

    private static Map<String, Object> menuInput(String surface) {
        return Map.of(
                "accepted", true,
                "surface", surface,
                "source", "menu_action",
                "serviceCodeExecuted", true
        );
    }

    private static Map<String, Object> withoutRuntimeMutation(Map<String, Object> update) {
        Map<String, Object> copy = new LinkedHashMap<>(update);
        copy.put("runtimeMutationAccepted", false);
        copy.put("runtimeActionId", "native.ui.mission_log_update");
        copy.put("eventName", "mission.objective_completed");
        return Map.copyOf(copy);
    }
}
