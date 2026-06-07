package dev.echo.nativeplatform.contracts;

import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeServiceMutation(
        String moduleId,
        String surface,
        String action,
        String target,
        EchoNativeRuntimeSide side,
        Map<String, Object> evidence
) {
    public EchoNativeServiceMutation {
        moduleId = requireText(moduleId, "moduleId");
        surface = requireText(surface, "surface");
        action = requireText(action, "action");
        target = optionalText(target);
        side = side == null ? EchoNativeRuntimeSide.UNKNOWN : side;
        evidence = Map.copyOf(Objects.requireNonNullElse(evidence, Map.of()));
    }

    public static EchoNativeServiceMutation of(
            String moduleId,
            String surface,
            String action,
            String target,
            EchoNativeRuntimeSide side
    ) {
        return new EchoNativeServiceMutation(moduleId, surface, action, target, side, Map.of());
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
