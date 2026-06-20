package dev.echo.nativeplatform.bootstrap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent5HudOverlaySmoke {
    private EchoNativeAgent5HudOverlaySmoke() {
    }

    public static Map<String, Object> capture(
            boolean clientUiHostAttached,
            boolean overlayRendered,
            String trigger,
            String screenClass,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        Map<String, Object> dataSources = EchoNativeAgent5UiHandlerRegistry.dataSources();
        Map<String, Object> hud = object(dataSources.get("hud"));
        Map<String, Object> hostModel = EchoNativeAgent5ScreenHostModel.render(
                "TERMINAL",
                Map.of(),
                packId,
                moduleCount,
                itemCount,
                missionCount,
                regionCount
        );
        List<String> overlayLines = List.of(
                "Health " + hud.get("health"),
                "Hazard " + hud.get("hazard"),
                "Mission " + hud.get("mission"),
                "Notifications " + notificationSummary(dataSources.get("notifications")),
                "Anchor top_left_safe_area"
        );
        Map<String, Object> smoke = new LinkedHashMap<>();
        smoke.put("hudOverlaySmokeClass", EchoNativeAgent5HudOverlaySmoke.class.getSimpleName());
        smoke.put("overlayLayerId", "echohudcore:hud");
        smoke.put("trigger", trigger == null ? "" : trigger);
        smoke.put("screenClass", screenClass);
        smoke.put("clientUiHostAttached", clientUiHostAttached);
        smoke.put("overlayRendered", overlayRendered);
        smoke.put("overlayMessage", overlayMessage(hud));
        smoke.put("overlayLines", overlayLines);
        smoke.put("hostHeaderLines", hostModel.get("headerLines"));
        smoke.put("hudValues", hud);
        smoke.put("notificationAnchor", "top_left_safe_area");
        smoke.put("adapterCoreBridge", true);
        smoke.put("serviceCodeExecuted", true);
        smoke.put("passed", clientUiHostAttached
                && overlayRendered
                && number(EchoNativeAgent5UiExpectedValues.hud().get("health")).equals(number(hud.get("health")))
                && "echoashfallprotocol:secure_crash_outpost".equals(hud.get("missionId"))
                && "TRACKED".equals(hud.get("missionStatus"))
                && String.valueOf(hud.get("mission")).equals(
                String.valueOf(EchoNativeAgent5UiExpectedValues.hud().get("mission"))));
        return Map.copyOf(smoke);
    }

    public static String overlayMessage(Map<String, Object> hud) {
        Map<String, Object> safeHud = object(hud);
        return "ECHO HUD Health " + safeHud.get("health")
                + " | " + safeHud.get("hazard")
                + " | " + safeHud.get("mission");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static String notificationSummary(Object value) {
        if (!(value instanceof List<?> notifications)) {
            return "";
        }
        return notifications.stream()
                .map(EchoNativeAgent5HudOverlaySmoke::message)
                .filter(message -> !message.isBlank())
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
    }

    private static String message(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object message = map.get("message");
            return message == null ? "" : String.valueOf(message);
        }
        return "";
    }
}
