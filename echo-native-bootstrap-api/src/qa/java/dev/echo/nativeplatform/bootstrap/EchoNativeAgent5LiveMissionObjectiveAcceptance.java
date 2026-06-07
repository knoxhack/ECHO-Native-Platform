package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveMissionObjectiveAcceptance {
    private EchoNativeAgent5LiveMissionObjectiveAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> missionLogEndToEndAcceptance,
            Map<String, Object> hudUpdateSmoke
    ) {
        Map<String, Object> mission = missionLogEndToEndAcceptance == null ? Map.of() : missionLogEndToEndAcceptance;
        Map<String, Object> hud = hudUpdateSmoke == null ? Map.of() : hudUpdateSmoke;
        boolean missionAccepted = Boolean.TRUE.equals(mission.get("accepted"))
                && "mission_log_end_to_end:MISSION_ACTION->MISSION_LOG:secure_crash_outpost:UPDATED".equals(mission.get("effect"))
                && "MISSION_ACTION".equals(mission.get("key"))
                && "MISSION_LOG".equals(mission.get("surface"))
                && "echoashfallprotocol:secure_crash_outpost".equals(mission.get("missionId"))
                && "UPDATED".equals(mission.get("missionStatus"))
                && Double.valueOf(0.5D).equals(mission.get("missionProgress"))
                && Boolean.TRUE.equals(mission.get("physicalInputAccepted"))
                && Boolean.TRUE.equals(mission.get("renderAccepted"))
                && Boolean.TRUE.equals(mission.get("interactionAccepted"))
                && Boolean.TRUE.equals(mission.get("updateAccepted"))
                && Boolean.TRUE.equals(mission.get("missionLogRendered"))
                && Boolean.TRUE.equals(mission.get("runtimeMutationAccepted"));
        boolean hudAccepted = Boolean.TRUE.equals(hud.get("passed"))
                && "EchoNativeAgent5HudUpdateSmoke".equals(hud.get("hudUpdateSmokeClass"))
                && Integer.valueOf(85).equals(hud.get("hudHealth"))
                && EchoNativeAgent5UiExpectedValues.hud().get("hazard").equals(hud.get("hudHazard"))
                && EchoNativeAgent5UiExpectedValues.hud().get("mission").equals(hud.get("hudMission"))
                && "hud:update:health_hazard_mission".equals(hud.get("effect"))
                && Boolean.TRUE.equals(hud.get("runtimeMutationAccepted"));
        boolean accepted = missionAccepted && hudAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("missionAccepted", missionAccepted);
        result.put("hudAccepted", hudAccepted);
        result.put("missionRuntimeMutationAccepted", Boolean.TRUE.equals(mission.get("runtimeMutationAccepted")));
        result.put("hudRuntimeMutationAccepted", Boolean.TRUE.equals(hud.get("runtimeMutationAccepted")));
        result.put("missionId", String.valueOf(mission.getOrDefault("missionId", "")));
        result.put("missionStatus", String.valueOf(mission.getOrDefault("missionStatus", "")));
        result.put("missionProgress", mission.getOrDefault("missionProgress", 0.0D));
        result.put("hudHealth", hud.getOrDefault("hudHealth", 0));
        result.put("hudHazard", String.valueOf(hud.getOrDefault("hudHazard", "")));
        result.put("hudMission", String.valueOf(hud.getOrDefault("hudMission", "")));
        result.put("effect", accepted
                ? "live_mission_objective:accepted:MISSION_ACTION/HUD:secure_crash_outpost:UPDATED"
                : "live_mission_objective:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }
}
