package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5MainMenuOptionActivationSmoke {
    private EchoNativeAgent5MainMenuOptionActivationSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> continueRoute = EchoNativeAgent5UiActionRouter.routeMainMenuOption("Continue");
        Map<String, Object> newRunRoute = EchoNativeAgent5UiActionRouter.routeMainMenuOption("New Run");
        Map<String, Object> settingsRoute = EchoNativeAgent5UiActionRouter.routeMainMenuOption("Settings");
        Map<String, Object> quitRoute = EchoNativeAgent5UiActionRouter.routeMainMenuOption("Quit");
        Map<String, Object> rendered = EchoNativeAgent5SurfaceRenderer.render("MAIN_MENU", Map.of(
                "selectedOption", String.valueOf(settingsRoute.get("selectedOption")),
                "mainMenuOutput", String.valueOf(settingsRoute.get("mainMenuOutput"))
        ), dataSources);

        List<String> selectedOptions = List.of(
                String.valueOf(continueRoute.get("selectedOption")),
                String.valueOf(newRunRoute.get("selectedOption")),
                String.valueOf(settingsRoute.get("selectedOption")),
                String.valueOf(quitRoute.get("selectedOption"))
        );
        List<String> destinations = List.of(
                String.valueOf(continueRoute.get("destinationMode")),
                String.valueOf(newRunRoute.get("destinationMode")),
                String.valueOf(settingsRoute.get("destinationMode")),
                String.valueOf(quitRoute.get("destinationMode"))
        );
        List<String> effects = List.of(
                String.valueOf(continueRoute.get("effect")),
                String.valueOf(newRunRoute.get("effect")),
                String.valueOf(settingsRoute.get("effect")),
                String.valueOf(quitRoute.get("effect"))
        );

        boolean passed = List.of(continueRoute, newRunRoute, settingsRoute, quitRoute).stream()
                .allMatch(route -> Boolean.TRUE.equals(route.get("handled")))
                && selectedOptions.equals(List.of("Continue", "New Run", "Settings", "Quit"))
                && destinations.equals(List.of("WIKI", "WORLD_SETUP", "SETTINGS", "MAIN_MENU"))
                && effects.equals(List.of("main_menu:continue", "main_menu:new_run_world_setup", "main_menu:settings", "main_menu:quit_requested"))
                && Boolean.TRUE.equals(quitRoute.get("quitRequested"))
                && lines(rendered).stream().anyMatch(line -> line.contains("Selected: Settings"))
                && lines(rendered).stream().anyMatch(line -> line.contains("Action: Settings selected: opening Settings"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("mainMenuOptionActivationSmokeClass", EchoNativeAgent5MainMenuOptionActivationSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("selectedOptions", selectedOptions);
        smoke.put("destinations", destinations);
        smoke.put("effects", effects);
        smoke.put("quitRequested", quitRoute.get("quitRequested"));
        smoke.put("renderedLines", lines(rendered));
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
