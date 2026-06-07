package dev.echo.nativeplatform.loader;

import java.util.List;
import java.util.Map;

public final class NativeLoaderUiExpectedValues {
    public static final String SERVICE_ID = "echo.native.ui_expected_values";
    private static volatile Provider provider = Provider.empty();

    private NativeLoaderUiExpectedValues() {
    }

    public static void configure(Provider valuesProvider) {
        provider = valuesProvider == null ? Provider.empty() : valuesProvider;
    }

    public static Map<String, Object> dataSources() {
        return object(provider.dataSources());
    }

    public static Map<String, Object> terminal() {
        return object(dataSources().get("terminal"));
    }

    public static Map<String, Object> index() {
        return object(dataSources().get("index"));
    }

    public static Map<String, Object> lens() {
        return object(dataSources().get("lens"));
    }

    public static Map<String, Object> hud() {
        return object(dataSources().get("hud"));
    }

    public static Map<String, Object> mission() {
        return object(dataSources().get("missionLog"));
    }

    public static Map<String, Object> holomap() {
        return object(dataSources().get("holomap"));
    }

    public static Map<String, Object> wiki() {
        return object(dataSources().get("wiki"));
    }

    public static Map<String, Object> deathRecovery() {
        return object(dataSources().get("deathRecovery"));
    }

    public static String terminalCommand() {
        return text(terminal().get("command"));
    }

    public static String terminalOutput() {
        return text(terminal().get("readyLine"));
    }

    public static String indexQuery() {
        return text(index().get("query"));
    }

    public static String indexOutput() {
        return text(index().get("result"));
    }

    public static String indexSearchOutput() {
        return text(provider.searchIndex(indexQuery()).get("output"));
    }

    public static String lensTarget() {
        return text(lens().get("target"));
    }

    public static String lensOutput() {
        return text(lens().get("result"));
    }

    public static String hudLineToken() {
        Map<String, Object> hud = hud();
        return "Health " + hud.get("health") + " / " + hud.get("hazard");
    }

    public static String missionObjective() {
        return text(mission().get("objective"));
    }

    public static String missionTitle() {
        return text(mission().get("title"));
    }

    public static String holomapMarker() {
        return text(holomap().get("marker"));
    }

    public static String wikiLink() {
        return text(wiki().get("link"));
    }

    public static String recoveryOutput() {
        Map<String, Object> recovery = deathRecovery();
        return "Status: " + recovery.get("status") + "    Health: " + recovery.get("restoredHealth");
    }

    public static List<String> notificationMessages() {
        return notificationMessages(dataSources().get("notifications"));
    }

    public static Map<String, Object> terminalState() {
        return Map.of(
                "nativeUiExpectedValuesServiceId", SERVICE_ID,
                "focusedControl", "terminal:input",
                "mouseRouted", true,
                "terminalBuffer", terminalCommand(),
                "terminalOutput", terminalOutput(),
                "terminalCommandExecuted", true
        );
    }

    public static Map<String, Object> indexState() {
        return Map.of(
                "nativeUiExpectedValuesServiceId", SERVICE_ID,
                "focusedControl", "index:search",
                "mouseRouted", true,
                "indexBuffer", indexQuery(),
                "indexOutput", indexSearchOutput(),
                "indexSearchExecuted", true
        );
    }

    public static Map<String, Object> lensState() {
        return Map.of(
                "nativeUiExpectedValuesServiceId", SERVICE_ID,
                "focusedControl", "lens:scan",
                "mouseRouted", true,
                "lensOutput", lensOutput(),
                "lensScanExecuted", true
        );
    }

    public static Map<String, Object> recoveryState() {
        return Map.of(
                "nativeUiExpectedValuesServiceId", SERVICE_ID,
                "focusedControl", "recovery:recover",
                "mouseRouted", true,
                "recoveryOutput", recoveryOutput(),
                "recoveryActionExecuted", true
        );
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    static List<String> notificationMessages(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> (Map<String, Object>) entry)
                    .map(entry -> text(entry.get("message")))
                    .toList();
        }
        return List.of();
    }

    public static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public interface Provider {
        Map<String, Object> dataSources();

        Map<String, Object> searchIndex(String query);

        static Provider empty() {
            return new Provider() {
                @Override
                public Map<String, Object> dataSources() {
                    return Map.of();
                }

                @Override
                public Map<String, Object> searchIndex(String query) {
                    return Map.of();
                }
            };
        }
    }
}
