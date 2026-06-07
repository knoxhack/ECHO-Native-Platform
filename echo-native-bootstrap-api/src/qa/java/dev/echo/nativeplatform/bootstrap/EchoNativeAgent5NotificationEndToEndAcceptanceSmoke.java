package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5NotificationEndToEndAcceptanceSmoke {
    private EchoNativeAgent5NotificationEndToEndAcceptanceSmoke() {
    }

    public static Map<String, Object> capture(String packId, int moduleCount, int itemCount, int missionCount, int regionCount) {
        Map<String, Object> queue = EchoNativeAgent5NotificationQueueSmoke.capture(
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        Map<String, Object> dismiss = EchoNativeAgent5NotificationDismissSmoke.capture(
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        Map<String, Object> hud = EchoNativeAgent5HudOverlaySmoke.capture(
                true,
                true,
                "hud:passive",
                "dev.echo.nativeplatform.generated.EchoNativeDashboardScreen",
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        Map<String, Object> accepted = EchoNativeAgent5NotificationEndToEndAcceptance.assess(queue, dismiss, hud);
        Map<String, Object> rejectedNoQueue = EchoNativeAgent5NotificationEndToEndAcceptance.assess(
                Map.of("passed", false),
                dismiss,
                hud
        );
        Map<String, Object> rejectedNoDismiss = EchoNativeAgent5NotificationEndToEndAcceptance.assess(
                queue,
                Map.of("passed", false),
                hud
        );
        Map<String, Object> rejectedNoHud = EchoNativeAgent5NotificationEndToEndAcceptance.assess(
                queue,
                dismiss,
                Map.of("passed", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "notification_end_to_end:queue->hud:drop-oldest".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoQueue.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoDismiss.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoHud.get("accepted"));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("notificationEndToEndAcceptanceSmokeClass",
                EchoNativeAgent5NotificationEndToEndAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoQueue", rejectedNoQueue);
        smoke.put("rejectedNoDismiss", rejectedNoDismiss);
        smoke.put("rejectedNoHud", rejectedNoHud);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }
}
