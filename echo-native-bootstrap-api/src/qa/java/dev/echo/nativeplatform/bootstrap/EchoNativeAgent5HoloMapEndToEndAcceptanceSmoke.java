package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke {
    private static final String SURFACE_OPEN_ACTION_ID = "native.ui.surface_open";
    private static final String HOLOMAP_CANONICAL_ID = "echoholomap:ashfall_map";

    private EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> hotkey = EchoNativeAgent5PhysicalHotkeyPoller.poll(
                EchoNativeAgent5PhysicalHotkeyPoller.emptyState(),
                pressed("J")
        );
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "HOLOMAP",
                "HOLOMAP"
        );
        Map<String, Object> physicalInput = EchoNativeAgent5PhysicalInputAcceptance.assess(hotkey, liveSurface);
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "HOLOMAP",
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
                "HOLOMAP",
                "native_data_surface.open:HOLOMAP"
        );
        Map<String, Object> interactionWithSurfaceOpen = withSurfaceOpenMutation(interaction, surfaceOpenMutation);
        Map<String, Object> accepted = EchoNativeAgent5HoloMapEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                interactionWithSurfaceOpen
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5HoloMapEndToEndAcceptance.assess(
                hotkey,
                Map.of("accepted", false, "surface", "HOLOMAP"),
                render,
                interactionWithSurfaceOpen
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5HoloMapEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                Map.of("accepted", false, "surface", "HOLOMAP"),
                interactionWithSurfaceOpen
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5HoloMapEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                withSurfaceOpenMutation(Map.of("passed", false, "steps", java.util.List.of()), surfaceOpenMutation)
        );
        Map<String, Object> rejectedNoMutation = EchoNativeAgent5HoloMapEndToEndAcceptance.assess(
                hotkey,
                physicalInput,
                render,
                withSurfaceOpenMutation(interaction, withoutSurfaceOpenMutation(surfaceOpenMutation))
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && ("holomap_end_to_end:J->HOLOMAP:" + EchoNativeAgent5UiExpectedValues.holomapMarker())
                .equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMutation.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("holoMapEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5HoloMapEndToEndAcceptanceSmoke.class.getSimpleName());
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
        copy.put("resultSnapshot", Map.of("target", HOLOMAP_CANONICAL_ID));
        return Map.copyOf(copy);
    }
}
