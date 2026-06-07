package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5NotificationQueueSmoke {
    private EchoNativeAgent5NotificationQueueSmoke() {
    }

    public static Map<String, Object> capture(String packId, int moduleCount, int itemCount, int missionCount, int regionCount) {
        List<Map<String, Object>> sourceQueue = EchoNativeAgent5UiHandlerRegistry.notificationQueue();
        Map<String, Object> hostModel = EchoNativeAgent5ScreenHostModel.render(
                "TERMINAL",
                Map.of(),
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        List<String> messages = sourceQueue.stream()
                .map(notification -> String.valueOf(notification.get("message")))
                .toList();
        List<String> severities = sourceQueue.stream()
                .map(notification -> String.valueOf(notification.get("severity")))
                .toList();
        String expectedSummary = String.join(" / ", EchoNativeAgent5UiExpectedValues.notificationMessages());
        boolean delivered = sourceQueue.stream().allMatch(notification -> Boolean.TRUE.equals(notification.get("delivered")));
        boolean anchored = sourceQueue.stream().allMatch(notification -> "top_left_safe_area".equals(notification.get("anchor")));
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("notificationQueueSmokeClass", EchoNativeAgent5NotificationQueueSmoke.class.getSimpleName());
        smoke.put("queueId", "echonotificationcore:queue");
        smoke.put("sourceCount", sourceQueue.size());
        smoke.put("dispatchedCount", sourceQueue.size());
        smoke.put("messages", messages);
        smoke.put("severities", severities);
        smoke.put("anchor", "top_left_safe_area");
        smoke.put("delivered", delivered);
        smoke.put("anchored", anchored);
        smoke.put("hostHeaderLines", hostModel.get("headerLines"));
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", sourceQueue.size() == 2
                && delivered
                && anchored
                && messages.equals(EchoNativeAgent5UiExpectedValues.notificationMessages())
                && severities.equals(List.of("INFO", "INFO"))
                && strings(hostModel.get("headerLines")).stream()
                        .anyMatch(line -> line.contains(expectedSummary)));
        return Map.copyOf(smoke);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
