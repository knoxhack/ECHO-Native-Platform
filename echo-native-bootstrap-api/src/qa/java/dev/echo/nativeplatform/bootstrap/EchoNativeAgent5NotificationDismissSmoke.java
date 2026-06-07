package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5NotificationDismissSmoke {
    private EchoNativeAgent5NotificationDismissSmoke() {
    }

    public static Map<String, Object> capture(String packId, int moduleCount, int itemCount, int missionCount, int regionCount) {
        List<Map<String, Object>> notifications = EchoNativeAgent5UiHandlerRegistry.notificationQueue();
        Map<String, Object> before = EchoNativeAgent5ScreenHostModel.render(
                "TERMINAL",
                Map.of("notifications", notifications),
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        Map<String, Object> dismissed = EchoNativeAgent5UiActionRouter.routeNotificationDismiss(notifications);
        List<Map<String, Object>> remaining = notificationList(dismissed.get("remainingNotifications"));
        Map<String, Object> after = EchoNativeAgent5ScreenHostModel.render(
                "TERMINAL",
                Map.of("notifications", remaining),
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        List<String> beforeHeader = strings(before.get("headerLines"));
        List<String> afterHeader = strings(after.get("headerLines"));
        List<String> expectedMessages = EchoNativeAgent5UiExpectedValues.notificationMessages();
        String dismissedMessage = expectedMessages.isEmpty() ? "" : expectedMessages.get(0);
        List<String> remainingMessages = expectedMessages.size() <= 1
                ? List.of()
                : expectedMessages.subList(1, expectedMessages.size());
        String beforeSummary = String.join(" / ", expectedMessages);
        String afterSummary = String.join(" / ", remainingMessages);
        boolean passed = Boolean.TRUE.equals(dismissed.get("handled"))
                && String.valueOf(dismissed.get("dismissedId")).startsWith("echoterminal:")
                && dismissedMessage.equals(dismissed.get("dismissedMessage"))
                && strings(dismissed.get("remainingMessages")).equals(remainingMessages)
                && beforeHeader.stream().anyMatch(line -> line.contains(beforeSummary))
                && afterHeader.stream().anyMatch(line -> line.contains("Notifications: " + afterSummary))
                && afterHeader.stream().noneMatch(line -> line.contains(dismissedMessage + " /"));

        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("notificationDismissSmokeClass", EchoNativeAgent5NotificationDismissSmoke.class.getSimpleName());
        smoke.put("serviceCodeExecuted", true);
        smoke.put("adapterCoreBridge", true);
        smoke.put("dismissedId", dismissed.get("dismissedId"));
        smoke.put("dismissedMessage", dismissed.get("dismissedMessage"));
        smoke.put("remainingMessages", dismissed.get("remainingMessages"));
        smoke.put("beforeHeaderLines", beforeHeader);
        smoke.put("afterHeaderLines", afterHeader);
        smoke.put("effect", dismissed.get("effect"));
        smoke.put("passed", passed);
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> notificationList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .map(Map::copyOf)
                    .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
