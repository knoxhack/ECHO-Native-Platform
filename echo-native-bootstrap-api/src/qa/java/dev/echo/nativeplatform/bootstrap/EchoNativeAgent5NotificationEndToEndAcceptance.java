package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5NotificationEndToEndAcceptance {
    private EchoNativeAgent5NotificationEndToEndAcceptance() {
    }

    public static Map<String, Object> assess(
            Map<String, Object> queueSmoke,
            Map<String, Object> dismissSmoke,
            Map<String, Object> hudOverlaySmoke
    ) {
        Map<String, Object> queue = queueSmoke == null ? Map.of() : queueSmoke;
        Map<String, Object> dismiss = dismissSmoke == null ? Map.of() : dismissSmoke;
        Map<String, Object> hud = hudOverlaySmoke == null ? Map.of() : hudOverlaySmoke;
        List<String> messages = strings(queue.get("messages"));
        List<String> severities = strings(queue.get("severities"));
        List<String> beforeHeader = strings(dismiss.get("beforeHeaderLines"));
        List<String> afterHeader = strings(dismiss.get("afterHeaderLines"));
        List<String> overlayLines = strings(hud.get("overlayLines"));
        List<String> expectedMessages = EchoNativeAgent5UiExpectedValues.notificationMessages();
        String dismissedMessage = expectedMessages.isEmpty() ? "" : expectedMessages.get(0);
        List<String> remainingMessages = expectedMessages.size() <= 1
                ? List.of()
                : expectedMessages.subList(1, expectedMessages.size());
        String beforeSummary = String.join(" / ", expectedMessages);
        String afterSummary = String.join(" / ", remainingMessages);
        boolean queueAccepted = Boolean.TRUE.equals(queue.get("passed"))
                && "EchoNativeAgent5NotificationQueueSmoke".equals(queue.get("notificationQueueSmokeClass"))
                && "echonotificationcore:queue".equals(queue.get("queueId"))
                && Integer.valueOf(2).equals(number(queue.get("sourceCount")))
                && Integer.valueOf(2).equals(number(queue.get("dispatchedCount")))
                && Boolean.TRUE.equals(queue.get("delivered"))
                && Boolean.TRUE.equals(queue.get("anchored"))
                && messages.equals(expectedMessages)
                && severities.equals(List.of("INFO", "INFO"));
        boolean hudAccepted = Boolean.TRUE.equals(hud.get("passed"))
                && "top_left_safe_area".equals(hud.get("notificationAnchor"))
                && overlayLines.stream().anyMatch(line -> line.contains(beforeSummary));
        boolean dismissAccepted = Boolean.TRUE.equals(dismiss.get("passed"))
                && "EchoNativeAgent5NotificationDismissSmoke".equals(dismiss.get("notificationDismissSmokeClass"))
                && String.valueOf(dismiss.get("dismissedId")).startsWith("echoterminal:")
                && dismissedMessage.equals(dismiss.get("dismissedMessage"))
                && strings(dismiss.get("remainingMessages")).equals(remainingMessages)
                && "notification:dismiss-oldest".equals(dismiss.get("effect"))
                && beforeHeader.stream().anyMatch(line -> line.contains(beforeSummary))
                && afterHeader.stream().anyMatch(line -> line.contains("Notifications: " + afterSummary))
                && afterHeader.stream().noneMatch(line -> line.contains(dismissedMessage + " /"));
        boolean accepted = queueAccepted && hudAccepted && dismissAccepted;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", accepted);
        result.put("queueAccepted", queueAccepted);
        result.put("hudAccepted", hudAccepted);
        result.put("dismissAccepted", dismissAccepted);
        result.put("sourceCount", numberOrZero(queue.get("sourceCount")));
        result.put("dispatchedCount", numberOrZero(queue.get("dispatchedCount")));
        result.put("dismissedId", dismiss.getOrDefault("dismissedId", ""));
        result.put("remainingMessages", strings(dismiss.get("remainingMessages")));
        result.put("effect", accepted
                ? "notification_end_to_end:queue->hud:drop-oldest"
                : "notification_end_to_end:rejected");
        result.put("adapterCoreBridge", true);
        result.put("serviceCodeExecuted", true);
        return Map.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Integer numberOrZero(Object value) {
        Integer number = number(value);
        return number == null ? 0 : number;
    }
}
