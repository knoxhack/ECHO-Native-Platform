package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5MainMenuEndToEndAcceptance {
    private EchoNativeAgent5MainMenuEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> overrideSmoke,
            Map<String, Object> optionSmoke
    ) {
        Map<String, Object> override = overrideSmoke == null ? Map.of() : overrideSmoke;
        Map<String, Object> options = optionSmoke == null ? Map.of() : optionSmoke;
        List<String> selectedOptions = strings(options, "selectedOptions");
        List<String> destinations = strings(options, "destinations");
        List<String> effects = strings(options, "effects");
        List<String> surfaceLines = strings(override, "surfaceLines");
        List<String> renderedLines = strings(options, "renderedLines");
        boolean accepted = Boolean.TRUE.equals(override.get("passed"))
                && Boolean.TRUE.equals(override.get("guardSatisfied"))
                && Boolean.TRUE.equals(override.get("overrideAttached"))
                && String.valueOf(override.get("screenTitle")).contains("MAIN_MENU")
                && surfaceLines.stream().anyMatch(line -> line.contains("Main Menu: ECHO Ashfall Terminal boot routes"))
                && Boolean.TRUE.equals(options.get("passed"))
                && selectedOptions.equals(List.of("Continue", "New Run", "Settings", "Quit"))
                && destinations.equals(List.of("WIKI", "WORLD_SETUP", "SETTINGS", "MAIN_MENU"))
                && effects.equals(List.of(
                        "main_menu:continue",
                        "main_menu:new_run_world_setup",
                        "main_menu:settings",
                        "main_menu:quit_requested"
                ))
                && Boolean.TRUE.equals(options.get("quitRequested"))
                && renderedLines.stream().anyMatch(line -> line.contains("Action: Settings selected: opening Settings"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("surface", "MAIN_MENU");
        result.put("optionCount", selectedOptions.size());
        result.put("selectedOptions", selectedOptions);
        result.put("destinations", destinations);
        result.put("effects", effects);
        result.put("settingsDestination", destinations.contains("SETTINGS") ? "SETTINGS" : "");
        result.put("quitRequested", Boolean.TRUE.equals(options.get("quitRequested")));
        result.put("overrideAttached", Boolean.TRUE.equals(override.get("overrideAttached")));
        result.put("renderedSurfaceLine", surfaceLines.stream()
                .filter(line -> line.contains("Main Menu: ECHO Ashfall Terminal boot routes"))
                .findFirst()
                .orElse(""));
        result.put("effect", accepted
                ? "main_menu_end_to_end:accepted:4"
                : "main_menu_end_to_end:rejected:" + selectedOptions.size());
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
}
