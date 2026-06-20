package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderScreenHostModel {
    public static final String SERVICE_ID = "echo.native.screen_host_model";
    private static final String FOOTER_LINE =
            "ASHFALL ROUTE // Enter action  Esc back  Native route table preserves product world ownership";
    private static volatile Provider provider = Provider.empty();

    private NativeLoaderScreenHostModel() {
    }

    public static void configure(Provider modelProvider) {
        provider = modelProvider == null ? Provider.empty() : modelProvider;
    }

    public static Map<String, Object> render(
            String mode,
            Map<String, Object> state,
            String packId,
            int moduleCount,
            int itemCount,
            int missionCount,
            int regionCount
    ) {
        String normalizedMode = normalizeMode(mode);
        Provider current = provider;
        Map<String, Object> dataSources = object(current.dataSources());
        Map<String, Object> hud = hudValues(state, object(dataSources.get("hud")));
        Map<String, Object> surface = object(current.renderSurface(normalizedMode, state));
        NativeLoaderTheme theme = NativeLoaderThemeResolver.activeTheme();
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("nativeScreenHostModelServiceId", SERVICE_ID);
        model.put("screenTitle", screenTitle(normalizedMode, theme));
        model.put("headerLines", List.of(
                theme.token("identityLabel") + "     Theme: " + theme.id() + " / " + theme.source(),
                "Pack: " + fallback(packId, current.productNamespace()) + "     Shell: Ashfall terminal",
                "Registered content: " + itemCount + " items/blocks     Modules: " + moduleCount,
                "Route data: " + missionCount + " missions / " + regionCount + " regions",
                "Notifications: " + notificationSummary(stateValue(state, "notifications", dataSources.get("notifications"))),
                "HUD: Health " + hud.get("health") + " / " + hud.get("hazard")
        ));
        model.put("surfaceLines", lines(surface));
        model.put("footerLine", FOOTER_LINE);
        model.put("hudValues", hud);
        model.put("notificationAnchor", "top_left_safe_area");
        model.put("adapterCoreBridge", true);
        model.put("serviceCodeExecuted", true);
        model.put("hostModelClass", NativeLoaderScreenHostModel.class.getSimpleName());
        model.putAll(theme.evidence());
        return Map.copyOf(model);
    }

    private static String screenTitle(String mode, NativeLoaderTheme theme) {
        return switch (mode) {
            case "MAIN_MENU" -> theme.token("mainMenuTitle");
            case "WORLD_SETUP" -> theme.token("worldSetupTitle");
            default -> "ECHO NATIVE // " + mode;
        };
    }

    @SuppressWarnings("unchecked")
    private static List<String> lines(Map<String, Object> model) {
        Object value = model.get("lines");
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String notificationSummary(Object value) {
        if (!(value instanceof List<?> notifications)) {
            return "";
        }
        return notifications.stream()
                .map(NativeLoaderScreenHostModel::message)
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

    private static Object stateValue(Map<String, Object> state, String key, Object fallback) {
        if (state == null || !state.containsKey(key)) {
            return fallback;
        }
        return state.get(key);
    }

    private static Map<String, Object> hudValues(Map<String, Object> state, Map<String, Object> fallbackHud) {
        Map<String, Object> hud = new LinkedHashMap<>(fallbackHud);
        hud.put("health", stateValue(state, "hudHealth", fallbackHud.get("health")));
        hud.put("hazard", stateValue(state, "hudHazard", fallbackHud.get("hazard")));
        hud.put("mission", stateValue(state, "hudMission", fallbackHud.get("mission")));
        return Map.copyOf(hud);
    }

    private static String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "TERMINAL" : normalized;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public interface Provider {
        Map<String, Object> dataSources();

        Map<String, Object> renderSurface(String mode, Map<String, Object> state);

        String productNamespace();

        static Provider empty() {
            return new Provider() {
                @Override
                public Map<String, Object> dataSources() {
                    return Map.of();
                }

                @Override
                public Map<String, Object> renderSurface(String mode, Map<String, Object> state) {
                    return Map.of("lines", List.of());
                }

                @Override
                public String productNamespace() {
                    return "";
                }
            };
        }
    }
}
