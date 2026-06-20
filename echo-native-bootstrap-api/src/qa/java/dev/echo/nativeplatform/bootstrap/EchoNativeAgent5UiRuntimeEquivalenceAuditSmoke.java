package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiRuntimeEquivalenceAuditSmoke {
    private static final List<String> EXPECTED_SCREEN_IDS = List.of(
            "echo:main_menu",
            "echoterminal:terminal",
            "echoindex:index",
            "echolens:lens",
            "echohudcore:hud",
            "echonotificationcore:queue",
            "echoscreencore:mission_log",
            "echoscreencore:settings",
            "echoscreencore:pause_flow",
            "echoscreencore:death_recovery",
            "echoholomap:holomap",
            "echowiki:wiki",
            "signalos:terminal",
            "echoashfallprotocol:drone"
    );

    private EchoNativeAgent5UiRuntimeEquivalenceAuditSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> contract = EchoNativeLiveUiBridge.contractSnapshot();
        Map<String, Object> dataSources = object(contract.get("agent5DataSources"));
        Map<String, Object> terminal = object(dataSources.get("terminal"));
        Map<String, Object> index = object(dataSources.get("index"));
        Map<String, Object> lens = object(dataSources.get("lens"));
        Map<String, Object> hud = object(dataSources.get("hud"));
        Map<String, Object> mission = object(dataSources.get("missionLog"));
        Map<String, Object> terminalResult = EchoNativeAgent5UiHandlerRegistry.executeTerminal("status");
        Map<String, Object> indexResult = EchoNativeAgent5UiHandlerRegistry.searchIndex("ashfall");
        Map<String, Object> lensResult = EchoNativeAgent5UiHandlerRegistry.scanLens(
                EchoNativeAgent5UiExpectedValues.lensTarget());
        Map<String, Object> missionUpdate = EchoNativeAgent5MissionLogUpdateSmoke.capture();

        boolean screenIdsMatch = strings(contract.get("screenIds")).equals(EXPECTED_SCREEN_IDS);
        boolean terminalMatches = EchoNativeAgent5UiExpectedValues.terminalCommand().equals(terminal.get("command"))
                && "ASH>".equals(terminal.get("prompt"))
                && EchoNativeAgent5UiExpectedValues.terminalOutput().equals(terminal.get("readyLine"))
                && Boolean.TRUE.equals(terminalResult.get("handled"))
                && EchoNativeAgent5UiExpectedValues.terminalOutput().equals(terminalResult.get("output"));
        boolean indexMatches = EchoNativeAgent5UiExpectedValues.indexQuery().equals(index.get("query"))
                && EchoNativeAgent5UiExpectedValues.indexOutput().equals(index.get("result"))
                && Boolean.TRUE.equals(indexResult.get("handled"))
                && String.valueOf(indexResult.get("output")).contains("index result(s):");
        boolean lensMatches = EchoNativeAgent5UiExpectedValues.lensTarget().equals(lens.get("target"))
                && EchoNativeAgent5UiExpectedValues.lensOutput().equals(lens.get("result"))
                && Boolean.TRUE.equals(lensResult.get("handled"))
                && String.valueOf(lensResult.get("output")).contains(String.valueOf(lens.get("summary")))
                && String.valueOf(lensResult.get("output")).contains(String.valueOf(lens.get("riskLabel")));
        Map<String, Object> expectedHud = EchoNativeAgent5UiExpectedValues.hud();
        boolean hudMatches = number(expectedHud.get("health")).equals(number(hud.get("health")))
                && expectedHud.get("hazard").equals(hud.get("hazard"))
                && "echoashfallprotocol:secure_crash_outpost".equals(hud.get("missionId"))
                && "TRACKED".equals(hud.get("missionStatus"))
                && expectedHud.get("mission").equals(hud.get("mission"));
        boolean missionMatches = "echoashfallprotocol:secure_crash_outpost".equals(mission.get("missionId"))
                && EchoNativeAgent5UiExpectedValues.missionObjective().equals(mission.get("objective"))
                && "TRACKED".equals(mission.get("status"))
                && number(mission.get("recordCount")) > 0
                && Boolean.TRUE.equals(missionUpdate.get("passed"))
                && "UPDATED".equals(missionUpdate.get("missionStatus"))
                && Double.valueOf(0.5D).equals(missionUpdate.get("missionProgress"));
        boolean notificationsMatch = notificationMessages(dataSources.get("notifications"))
                .equals(EchoNativeAgent5UiExpectedValues.notificationMessages());
        boolean passed = screenIdsMatch
                && terminalMatches
                && indexMatches
                && lensMatches
                && hudMatches
                && missionMatches
                && notificationsMatch;

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("uiRuntimeEquivalenceAuditSmokeClass",
                EchoNativeAgent5UiRuntimeEquivalenceAuditSmoke.class.getSimpleName());
        smoke.put("screenIdsMatch", screenIdsMatch);
        smoke.put("terminalMatches", terminalMatches);
        smoke.put("indexMatches", indexMatches);
        smoke.put("lensMatches", lensMatches);
        smoke.put("hudMatches", hudMatches);
        smoke.put("missionMatches", missionMatches);
        smoke.put("notificationsMatch", notificationsMatch);
        smoke.put("terminalOutput", terminalResult.get("output"));
        smoke.put("missionUpdateStatus", missionUpdate.get("missionStatus"));
        smoke.put("missionUpdateProgress", missionUpdate.get("missionProgress"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> notificationMessages(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .map(entry -> String.valueOf(entry.get("message")))
                    .toList();
        }
        return List.of();
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
