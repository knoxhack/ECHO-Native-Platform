package dev.echo.nativeplatform.bootstrap;

import java.util.List;
import java.util.Map;

final class EchoNativeAgent5UiExpectedValues {
    private EchoNativeAgent5UiExpectedValues() {
    }

    static Map<String, Object> terminal() {
        return Map.of("title", "ECHO Terminal", "prompt", "ashfall>");
    }

    static Map<String, Object> terminalState() {
        return Map.of("title", terminal().get("title"), "command", terminalCommand(), "output", terminalOutput());
    }

    static String terminalCommand() {
        return "scan ashfall";
    }

    static String terminalOutput() {
        return "Ashfall systems online";
    }

    static Map<String, Object> indexState() {
        return Map.of("query", indexQuery(), "result", indexOutput(), "output", indexSearchOutput());
    }

    static String indexQuery() {
        return "drop pod beacon";
    }

    static String indexOutput() {
        return "Drop Pod Beacon";
    }

    static String indexSearchOutput() {
        return "Drop Pod Beacon indexed";
    }

    static Map<String, Object> lens() {
        return Map.of("target", lensTarget(), "summary", lensOutput());
    }

    static Map<String, Object> lensState() {
        return Map.of("target", lensTarget(), "summary", lensOutput(), "result", lensOutput());
    }

    static String lensTarget() {
        return "ashfall_core_relay";
    }

    static String lensOutput() {
        return "Relay integrity nominal";
    }

    static Map<String, Object> holomap() {
        return Map.of("layer", "ashfall_region", "marker", holomapMarker());
    }

    static String holomapMarker() {
        return "crash_site_alpha";
    }

    static Map<String, Object> wiki() {
        return Map.of("guide", "Ashfall Field Guide", "page", "Crash Site");
    }

    static String wikiLink() {
        return "wiki://ashfall/crash-site";
    }

    static Map<String, Object> hud() {
        return Map.of("health", 92, "hazard", "LOW", "mission", missionObjective());
    }

    static String hudLineToken() {
        return "HUD: Health " + hud().get("health");
    }

    static String missionObjective() {
        return "Stabilize the relay";
    }

    static Map<String, Object> recoveryState() {
        return Map.of("status", recoveryOutput());
    }

    static String recoveryOutput() {
        return "Recovery route ready";
    }

    static List<String> notificationMessages() {
        return List.of("Relay synchronized", "Ashfall route updated");
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
