package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5WikiEndToEndAcceptanceSmoke {
    private static final String SURFACE_OPEN_ACTION_ID = "native.ui.surface_open";
    private static final String WIKI_CANONICAL_ID = "echowiki:ashfall";

    private EchoNativeAgent5WikiEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> hotkey = Map.of(
                "handled", true,
                "physicalPoller", false,
                "serviceCodeExecuted", true,
                "key", "MODULE_ROUTE",
                "surface", "WIKI",
                "effect", "native_data_surface.open:WIKI"
        );
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "WIKI",
                "WIKI"
        );
        Map<String, Object> physicalInput = EchoNativeAgent5PhysicalInputAcceptance.assess(hotkey, liveSurface);
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "WIKI",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> render = EchoNativeAgent5LiveSurfaceRenderAcceptance.assess(liveSurface, snapshot);
        Map<String, Object> interaction = EchoNativeAgent5UiHostInteractionSmoke.run(
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        );
        Map<String, Object> surfaceOpenMutation = EchoNativeBootstrapMain.executeNativeSurfaceOpenFromUi(
                "WIKI",
                "native_data_surface.open:WIKI"
        );
        Map<String, Object> interactionWithSurfaceOpen = withSurfaceOpenMutation(interaction, surfaceOpenMutation);
        Map<String, Object> accepted = EchoNativeAgent5WikiEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                interactionWithSurfaceOpen
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5WikiEndToEndAcceptance.assess(
                hotkey,
                Map.of("accepted", false, "surface", "WIKI"),
                render,
                interactionWithSurfaceOpen
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5WikiEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                Map.of("accepted", false, "surface", "WIKI"),
                interactionWithSurfaceOpen
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5WikiEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                withSurfaceOpenMutation(Map.of("passed", false, "steps", java.util.List.of()), surfaceOpenMutation)
        );
        Map<String, Object> rejectedNoMutation = EchoNativeAgent5WikiEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                withSurfaceOpenMutation(interaction, withoutSurfaceOpenMutation(surfaceOpenMutation))
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "wiki_end_to_end:MODULE_ROUTE->WIKI:ashfall".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMutation.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("wikiEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5WikiEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedNoInteraction", rejectedNoInteraction);
        smoke.put("rejectedNoMutation", rejectedNoMutation);
        smoke.put("surfaceOpenMutation", surfaceOpenMutation);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Boolean> pressed(String key) {
        Map<String, Boolean> state = new LinkedHashMap<>(EchoNativeAgent5PhysicalHotkeyPoller.emptyState());
        state.put(key, true);
        return Map.copyOf(state);
    }

    private static Map<String, Object> withSurfaceOpenMutation(
            Map<String, Object> interaction,
            Map<String, Object> surfaceOpenMutation
    ) {
        Map<String, Object> copy = new LinkedHashMap<>(interaction == null ? Map.of() : interaction);
        copy.put("surfaceOpenMutation", surfaceOpenMutation == null ? Map.of() : surfaceOpenMutation);
        return Map.copyOf(copy);
    }

    private static Map<String, Object> withoutSurfaceOpenMutation(Map<String, Object> surfaceOpenMutation) {
        Map<String, Object> copy = new LinkedHashMap<>(surfaceOpenMutation == null ? Map.of() : surfaceOpenMutation);
        copy.put("mutated", false);
        copy.put("saveTouched", true);
        copy.put("missionUpdated", true);
        copy.put("feedbackEmitted", true);
        copy.put("runtimeActionId", SURFACE_OPEN_ACTION_ID);
        copy.put("eventName", SURFACE_OPEN_ACTION_ID);
        copy.put("resultSnapshot", Map.of("target", WIKI_CANONICAL_ID));
        return Map.copyOf(copy);
    }
}
