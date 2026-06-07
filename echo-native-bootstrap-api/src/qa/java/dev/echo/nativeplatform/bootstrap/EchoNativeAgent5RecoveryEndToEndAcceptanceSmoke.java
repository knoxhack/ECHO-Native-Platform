package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke {
    private static final String RECOVERY_ITEM_ID = "echoashfallprotocol:portable_signal_scanner";
    private static final String RECOVERY_RUNTIME_ACTION_ID = "player.inventory.grant";

    private EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> recoveryAction = routeAction("RECOVERY_ACTION", "RECOVERY", "recovery.recover");
        Map<String, Object> liveSurface = EchoNativeAgent5LiveSurfaceAcceptance.assess(
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "RECOVERY",
                "RECOVERY"
        );
        Map<String, Object> menuInput = menuInput("RECOVERY");
        Map<String, Object> snapshot = EchoNativeAgent5UiHostSmokeSnapshot.capture(
                "RECOVERY",
                true,
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                "echoashfallprotocol",
                92,
                20,
                1,
                1,
                Map.of()
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
        Map<String, Object> recoveryMutation = EchoNativeBootstrapMain.grantNativeItemFromUiEvidence(
                RECOVERY_ITEM_ID,
                1
        );
        Map<String, Object> interactionWithRecoveryMutation = withRecoveryMutation(interaction, recoveryMutation);
        Map<String, Object> accepted = EchoNativeAgent5RecoveryEndToEndAcceptance.assess(
                recoveryAction,
                menuInput,
                render,
                interactionWithRecoveryMutation
        );
        Map<String, Object> rejectedNoInput = EchoNativeAgent5RecoveryEndToEndAcceptance.assess(
                recoveryAction,
                Map.of("accepted", false, "surface", "RECOVERY"),
                render,
                interactionWithRecoveryMutation
        );
        Map<String, Object> rejectedNoRender = EchoNativeAgent5RecoveryEndToEndAcceptance.assess(
                recoveryAction,
                menuInput,
                Map.of("accepted", false, "surface", "RECOVERY"),
                interactionWithRecoveryMutation
        );
        Map<String, Object> rejectedNoInteraction = EchoNativeAgent5RecoveryEndToEndAcceptance.assess(
                recoveryAction,
                menuInput,
                render,
                withRecoveryMutation(Map.of("passed", false, "steps", List.of()), recoveryMutation)
        );
        Map<String, Object> rejectedNoMutation = EchoNativeAgent5RecoveryEndToEndAcceptance.assess(
                recoveryAction,
                menuInput,
                render,
                withRecoveryMutation(interaction, withoutRecoveryMutation(recoveryMutation))
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "recovery_end_to_end:RECOVERY_ACTION->RECOVERY:RECOVERED".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoInput.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoRender.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoInteraction.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoMutation.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("recoveryEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5RecoveryEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoInput", rejectedNoInput);
        smoke.put("rejectedNoRender", rejectedNoRender);
        smoke.put("rejectedNoInteraction", rejectedNoInteraction);
        smoke.put("rejectedNoMutation", rejectedNoMutation);
        smoke.put("recoveryMutation", recoveryMutation);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    private static Map<String, Object> routeAction(String key, String surface, String action) {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("handled", true);
        route.put("key", key);
        route.put("surface", surface);
        route.put("action", action);
        route.put("source", "menu_action");
        return Map.copyOf(route);
    }

    private static Map<String, Object> menuInput(String surface) {
        return Map.of(
                "accepted", true,
                "surface", surface,
                "source", "menu_action",
                "serviceCodeExecuted", true
        );
    }

    private static Map<String, Object> withRecoveryMutation(
            Map<String, Object> interaction,
            Map<String, Object> recoveryMutation
    ) {
        Map<String, Object> copy = new LinkedHashMap<>(interaction == null ? Map.of() : interaction);
        copy.put("recoveryMutation", recoveryMutation == null ? Map.of() : recoveryMutation);
        return Map.copyOf(copy);
    }

    private static Map<String, Object> withoutRecoveryMutation(Map<String, Object> recoveryMutation) {
        Map<String, Object> copy = new LinkedHashMap<>(recoveryMutation == null ? Map.of() : recoveryMutation);
        copy.put("mutated", false);
        copy.put("saveTouched", true);
        copy.put("missionUpdated", true);
        copy.put("feedbackEmitted", true);
        copy.put("runtimeActionId", RECOVERY_RUNTIME_ACTION_ID);
        copy.put("requestedItemId", RECOVERY_ITEM_ID);
        copy.put("itemId", RECOVERY_ITEM_ID);
        return Map.copyOf(copy);
    }
}
