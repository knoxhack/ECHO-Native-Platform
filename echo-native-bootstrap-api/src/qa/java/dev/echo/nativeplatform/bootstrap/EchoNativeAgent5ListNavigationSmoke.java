package dev.echo.nativeplatform.bootstrap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5ListNavigationSmoke {
    private EchoNativeAgent5ListNavigationSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> mainMenuDown = EchoNativeAgent5UiActionRouter.routeListNavigation("DOWN", "MAIN_MENU", 0);
        Map<String, Object> mainMenuDownAgain = EchoNativeAgent5UiActionRouter.routeListNavigation(
                "DOWN",
                "MAIN_MENU",
                integer(mainMenuDown.get("selectedIndex"))
        );
        Map<String, Object> settingsDown = EchoNativeAgent5UiActionRouter.routeListNavigation("DOWN", "SETTINGS", 0);
        Map<String, Object> settingsDownAgain = EchoNativeAgent5UiActionRouter.routeListNavigation(
                "DOWN",
                "SETTINGS",
                integer(settingsDown.get("selectedIndex"))
        );
        Map<String, Object> pauseUp = EchoNativeAgent5UiActionRouter.routeListNavigation("UP", "PAUSE", 0);

        List<String> selectedOptions = List.of(
                String.valueOf(mainMenuDown.get("selectedOption")),
                String.valueOf(mainMenuDownAgain.get("selectedOption")),
                String.valueOf(settingsDown.get("selectedOption")),
                String.valueOf(settingsDownAgain.get("selectedOption")),
                String.valueOf(pauseUp.get("selectedOption"))
        );
        List<String> effects = List.of(
                String.valueOf(mainMenuDown.get("effect")),
                String.valueOf(mainMenuDownAgain.get("effect")),
                String.valueOf(settingsDown.get("effect")),
                String.valueOf(settingsDownAgain.get("effect")),
                String.valueOf(pauseUp.get("effect"))
        );

        ArrayList<String> renderedLines = new ArrayList<>();
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render("MAIN_MENU", state(mainMenuDownAgain), dataSources)));
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render("SETTINGS", state(settingsDownAgain), dataSources)));
        renderedLines.addAll(lines(EchoNativeAgent5SurfaceRenderer.render("PAUSE", state(pauseUp), dataSources)));

        boolean passed = selectedOptions.equals(List.of(
                "New Ashfall Run",
                "Settings",
                "Theme",
                "Input Mode",
                "Quit to Main Menu"
        ))
                && effects.containsAll(List.of(
                        "list:main_menu:down",
                        "list:settings:down",
                        "list:pause:up"
                ))
                && renderedLines.stream().anyMatch(line -> line.contains("Selected: Settings"))
                && renderedLines.stream().anyMatch(line -> line.contains("Selected: Input Mode"))
                && renderedLines.stream().anyMatch(line -> line.contains("Selected: Quit to Main Menu"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("listNavigationSmokeClass", EchoNativeAgent5ListNavigationSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("selectedOptions", selectedOptions);
        smoke.put("effects", effects);
        smoke.put("renderedLines", List.copyOf(renderedLines));
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> state(Map<String, Object> navigation) {
        return Map.of(
                "selectedIndex", navigation.get("selectedIndex"),
                "selectedOption", navigation.get("selectedOption")
        );
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> rendered) {
        Object value = rendered.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
