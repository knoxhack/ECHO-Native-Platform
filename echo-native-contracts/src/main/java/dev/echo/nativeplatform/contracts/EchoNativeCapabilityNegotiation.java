package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeCapabilityNegotiation(
        String moduleId,
        String serviceId,
        String featureId,
        String requestedVersion,
        String selectedVersion,
        boolean supported,
        List<String> alternatives,
        Map<String, Object> evidence
) {
    public EchoNativeCapabilityNegotiation {
        moduleId = optionalText(moduleId);
        serviceId = requireText(serviceId, "serviceId");
        featureId = optionalText(featureId);
        requestedVersion = optionalText(requestedVersion);
        selectedVersion = optionalText(selectedVersion);
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        evidence = Map.copyOf(Objects.requireNonNullElse(evidence, Map.of()));
    }

    public static EchoNativeCapabilityNegotiation unsupported(String serviceId, EchoNativeServiceMutation mutation) {
        return new EchoNativeCapabilityNegotiation(
                mutation == null ? "" : mutation.moduleId(),
                serviceId,
                mutation == null ? "" : mutation.target(),
                "",
                "",
                false,
                List.of(),
                mutation == null ? Map.of() : mutation.evidence()
        );
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
