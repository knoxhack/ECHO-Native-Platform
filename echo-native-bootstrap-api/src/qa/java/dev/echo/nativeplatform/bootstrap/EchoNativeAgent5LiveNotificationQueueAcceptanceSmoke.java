package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeAgent5LiveNotificationQueueAcceptanceSmoke {
    private EchoNativeAgent5LiveNotificationQueueAcceptanceSmoke() {
    }

    public static Map<String, Object> capture() {
        Map<String, Object> endToEnd = object(EchoNativeAgent5NotificationEndToEndAcceptanceSmoke.capture(
                "echoashfallprotocol",
                92,
                20,
                1,
                1
        ).get("accepted"));
        Map<String, Object> accepted = EchoNativeAgent5LiveNotificationQueueAcceptance.assess(
                true,
                "top_left_safe_area",
                endToEnd
        );
        Map<String, Object> rejectedNoDispatch = EchoNativeAgent5LiveNotificationQueueAcceptance.assess(
                false,
                "top_left_safe_area",
                endToEnd
        );
        Map<String, Object> rejectedWrongAnchor = EchoNativeAgent5LiveNotificationQueueAcceptance.assess(
                true,
                "bottom_right",
                endToEnd
        );
        Map<String, Object> rejectedNoEndToEnd = EchoNativeAgent5LiveNotificationQueueAcceptance.assess(
                true,
                "top_left_safe_area",
                Map.of("accepted", false)
        );
        boolean passed = Boolean.TRUE.equals(accepted.get("accepted"))
                && "live_notification_queue:accepted:2->1:top_left_safe_area".equals(accepted.get("effect"))
                && Boolean.FALSE.equals(rejectedNoDispatch.get("accepted"))
                && Boolean.FALSE.equals(rejectedWrongAnchor.get("accepted"))
                && Boolean.FALSE.equals(rejectedNoEndToEnd.get("accepted"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("liveNotificationQueueAcceptanceSmokeClass",
                EchoNativeAgent5LiveNotificationQueueAcceptanceSmoke.class.getSimpleName());
        smoke.put("accepted", accepted);
        smoke.put("rejectedNoDispatch", rejectedNoDispatch);
        smoke.put("rejectedWrongAnchor", rejectedWrongAnchor);
        smoke.put("rejectedNoEndToEnd", rejectedNoEndToEnd);
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
