package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeDependencyGraphDiagnostics(
        String moduleId,
        List<String> resolvedOrder,
        List<String> missingDependencies,
        List<String> cycles,
        boolean deterministic,
        Map<String, Object> evidence
) {
    public EchoNativeDependencyGraphDiagnostics {
        moduleId = optionalText(moduleId);
        resolvedOrder = stringList(resolvedOrder);
        missingDependencies = stringList(missingDependencies);
        cycles = stringList(cycles);
        evidence = Map.copyOf(Objects.requireNonNullElse(evidence, Map.of()));
    }

    public static EchoNativeDependencyGraphDiagnostics empty(String moduleId) {
        return new EchoNativeDependencyGraphDiagnostics(moduleId, List.of(), List.of(), List.of(), true, Map.of());
    }

    private static List<String> stringList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
