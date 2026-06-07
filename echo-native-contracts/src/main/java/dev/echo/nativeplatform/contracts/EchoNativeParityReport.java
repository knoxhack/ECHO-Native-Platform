package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeParityReport(
        String moduleId,
        List<String> requiredSurfaces,
        List<String> mutatedSurfaces,
        List<String> missingSurfaces,
        boolean passed,
        Map<String, Object> evidence
) {
    public EchoNativeParityReport {
        moduleId = optionalText(moduleId);
        requiredSurfaces = stringList(requiredSurfaces);
        mutatedSurfaces = stringList(mutatedSurfaces);
        missingSurfaces = stringList(missingSurfaces);
        evidence = Map.copyOf(Objects.requireNonNullElse(evidence, Map.of()));
    }

    public static EchoNativeParityReport empty(String moduleId) {
        return new EchoNativeParityReport(moduleId, List.of(), List.of(), List.of(), true, Map.of());
    }

    private static List<String> stringList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
