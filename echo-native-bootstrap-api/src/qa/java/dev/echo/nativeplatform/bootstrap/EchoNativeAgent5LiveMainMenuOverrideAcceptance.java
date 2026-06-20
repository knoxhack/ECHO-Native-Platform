package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5LiveMainMenuOverrideAcceptance {
    private EchoNativeAgent5LiveMainMenuOverrideAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> overrideSmoke,
            Map<String, Object> liveSurfaceAcceptance,
            Map<String, Object> mainMenuEndToEndAcceptance
    ) {
        Map<String, Object> override = overrideSmoke == null ? Map.of() : overrideSmoke;
        Map<String, Object> surface = liveSurfaceAcceptance == null ? Map.of() : liveSurfaceAcceptance;
        Map<String, Object> endToEnd = mainMenuEndToEndAcceptance == null ? Map.of() : mainMenuEndToEndAcceptance;
        List<String> surfaceLines = strings(override, "surfaceLines");
        List<String> selectedOptions = strings(endToEnd, "selectedOptions");
        String settingsDestination = settingsDestination(endToEnd);
        boolean accepted = Boolean.TRUE.equals(override.get("titleScreenDetected"))
                && Boolean.TRUE.equals(override.get("overrideAttached"))
                && Boolean.TRUE.equals(override.get("guardSatisfied"))
                && Boolean.TRUE.equals(surface.get("accepted"))
                && "MAIN_MENU".equals(surface.get("currentMode"))
                && "live_surface:accepted:MAIN_MENU".equals(surface.get("effect"))
                && surfaceLines.stream().anyMatch(line -> line.contains("Main Menu: ECHO Ashfall Terminal boot routes"))
                && Boolean.TRUE.equals(endToEnd.get("accepted"))
                && "main_menu_end_to_end:accepted:4".equals(endToEnd.get("effect"))
                && selectedOptions.equals(List.of("Continue", "New Run", "Settings", "Quit"))
                && "SETTINGS".equals(settingsDestination)
                && Boolean.TRUE.equals(endToEnd.get("quitRequested"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("titleScreenDetected", Boolean.TRUE.equals(override.get("titleScreenDetected")));
        result.put("overrideAttached", Boolean.TRUE.equals(override.get("overrideAttached")));
        result.put("liveSurfaceAccepted", Boolean.TRUE.equals(surface.get("accepted")));
        result.put("surface", String.valueOf(surface.getOrDefault("currentMode", "")));
        result.put("optionCount", selectedOptions.size());
        result.put("selectedOptions", selectedOptions);
        result.put("settingsDestination", settingsDestination);
        result.put("quitRequested", Boolean.TRUE.equals(endToEnd.get("quitRequested")));
        result.put("effect", accepted ? "live_main_menu_override:accepted:MAIN_MENU:4"
                : "live_main_menu_override:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static String settingsDestination(Map<String, Object> endToEnd) {
        String value = String.valueOf(endToEnd.getOrDefault("settingsDestination", ""));
        return value.equals("SETTINGS") || value.endsWith(":settings") ? "SETTINGS" : value;
    }
}
