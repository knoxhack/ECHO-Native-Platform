package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeTransformAllowlistValidation(
        String validationId,
        boolean valid,
        boolean transformPlanningOnly,
        boolean transformsEnabled,
        boolean minecraftTransformAllowed,
        boolean addonTransformAllowed,
        int transformCount,
        List<String> allowlistedTypes,
        List<Map<String, Object>> transforms
) {
}
