package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveMissionObjectiveAcceptanceSmoke {
    private EchoNativeAgent5LiveMissionObjectiveAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> missionLog = object(EchoNativeAgent5MissionLogEndToEndAcceptanceSmoke.capture()
                .get("accepted"));
        Map<String, Object> hudUpdate = EchoNativeAgent5HudUpdateSmoke.capture();
        Map<String, Object> accepted = EchoNativeAgent5LiveMissionObjectiveAcceptance.assess(
                missionLog,
                hudUpdate
        );
        Map<String, Object> rejectedNoMission = EchoNativeAgent5LiveMissionObjectiveAcceptance.assess(
                Map.of("accepted", false),
                hudUpdate
        );
        Map<String, Object> rejectedNoHud = EchoNativeAgent5LiveMissionObjectiveAcceptance.assess(
                missionLog,
                Map.of("passed", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_mission_objective:accepted:MISSION_ACTION/HUD:secure_crash_outpost:UPDATED"
                        .equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoMission.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHud.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveMissionObjectiveAcceptanceSmokeClass",
                EchoNativeAgent5LiveMissionObjectiveAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoMission", rejectedNoMission);
        smoke.put("rejectedNoHud", rejectedNoHud);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
