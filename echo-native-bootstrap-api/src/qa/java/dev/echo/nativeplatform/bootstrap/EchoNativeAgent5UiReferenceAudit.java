package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5UiReferenceAudit {
    private EchoNativeAgent5UiReferenceAudit() {
    }

    public static List<Map<String, Object>> records() {
        return List.of(
                record("custom_main_menu", "echo:main_menu", "MAIN_MENU", "MENU_ACTION", "mainMenu",
                        "main_menu_end_to_end_acceptance_smoke_executes"),
                record("terminal", "echoterminal:terminal", "TERMINAL", "M", "terminal",
                        "terminal_end_to_end_acceptance_smoke_executes"),
                record("index", "echoindex:index", "INDEX", "G/R/U/B", "index",
                        "index_end_to_end_acceptance_smoke_executes"),
                record("lens_scanner", "echolens:lens", "LENS", "LEFT_ALT", "lens",
                        "lens_end_to_end_acceptance_smoke_executes"),
                record("hud", "echohudcore:hud", "HUD", "MODULE_EVENT", "hud",
                        "hud_overlay_end_to_end_acceptance_smoke_executes"),
                record("mission_log", "echoscreencore:mission_log", "MISSION_LOG", "MISSION_ACTION", "missionLog",
                        "mission_log_end_to_end_acceptance_smoke_executes"),
                record("notifications", "echonotificationcore:queue", "HUD", "MODULE_EVENT", "notifications",
                        "notification_end_to_end_acceptance_smoke_executes"),
                record("holomap", "echoholomap:holomap", "HOLOMAP", "J/K/RIGHT_BRACKET/LEFT_BRACKET/BACKSLASH", "holomap",
                        "holomap_end_to_end_acceptance_smoke_executes"),
                record("wiki", "echowiki:wiki", "WIKI", "MODULE_ROUTE", "wiki",
                        "wiki_end_to_end_acceptance_smoke_executes"),
                record("settings", "echoscreencore:settings", "SETTINGS", "SETTINGS_ACTION", "settings",
                        "settings_end_to_end_acceptance_smoke_executes"),
                record("pause_flow", "echoscreencore:pause_flow", "PAUSE", "ESCAPE", "pauseFlow",
                        "pause_end_to_end_acceptance_smoke_executes"),
                record("death_recovery_screen", "echoscreencore:death_recovery", "RECOVERY", "RECOVERY_ACTION", "deathRecovery",
                        "recovery_end_to_end_acceptance_smoke_executes"),
                record("signalos_terminal", "signalos:terminal", "SIGNALOS", "N", "signalos",
                        "signalos_end_to_end_acceptance_smoke_executes"),
                record("ashfall_drone", "echoashfallprotocol:drone", "ASHFALL_DRONE", "X/C/Y/Z/B", "ashfallDrone",
                        "ashfall_drone_hotkey_route_executes")
        );
    }

    private static Map<String, Object> record(
            String behavior,
            String screenId,
            String surface,
            String routeKey,
            String dataSource,
            String acceptanceFeature
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("behavior", behavior);
        record.put("screenId", screenId);
        record.put("surface", surface);
        record.put("routeKey", routeKey);
        record.put("dataSource", dataSource);
        record.put("acceptanceFeature", acceptanceFeature);
        record.put("adapterCoreBridge", true);
        return Map.copyOf(record);
    }
}
