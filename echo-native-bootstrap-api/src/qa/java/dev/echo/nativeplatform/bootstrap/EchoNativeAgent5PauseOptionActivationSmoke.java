package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5PauseOptionActivationSmoke {
    private EchoNativeAgent5PauseOptionActivationSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> resume = EchoNativeAgent5UiActionRouter.routePauseOption("Resume", "LENS");
        Map<String, Object> settings = EchoNativeAgent5UiActionRouter.routePauseOption("Settings", "LENS");
        Map<String, Object> quit = EchoNativeAgent5UiActionRouter.routePauseOption("Quit to Main Menu", "LENS");

        List<String> selectedOptions = List.of(
                String.valueOf(resume.get("selectedOption")),
                String.valueOf(settings.get("selectedOption")),
                String.valueOf(quit.get("selectedOption"))
        );
        List<String> destinations = List.of(
                String.valueOf(resume.get("destinationMode")),
                String.valueOf(settings.get("destinationMode")),
                String.valueOf(quit.get("destinationMode"))
        );
        List<String> effects = List.of(
                String.valueOf(resume.get("effect")),
                String.valueOf(settings.get("effect")),
                String.valueOf(quit.get("effect"))
        );
        Map<String, Object> renderedPause = EchoNativeAgent5SurfaceRenderer.render("PAUSE", Map.of(
                "previousMode", "LENS",
                "selectedOption", "Settings"
        ), EchoNativeAgent5UiHandlerRegistry.dataSources());

        boolean passed = Boolean.TRUE.equals(resume.get("handled"))
                && Boolean.TRUE.equals(settings.get("handled"))
                && Boolean.TRUE.equals(quit.get("handled"))
                && selectedOptions.equals(List.of("Resume", "Settings", "Quit to Main Menu"))
                && destinations.equals(List.of("LENS", "SETTINGS", "MAIN_MENU"))
                && effects.equals(List.of("pause:resume", "pause:settings", "pause:main_menu"))
                && lines(renderedPause).stream().anyMatch(line -> line.contains("Selected: Settings"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("pauseOptionActivationSmokeClass", EchoNativeAgent5PauseOptionActivationSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("selectedOptions", selectedOptions);
        smoke.put("destinations", destinations);
        smoke.put("effects", effects);
        smoke.put("renderedLines", lines(renderedPause));
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
}
