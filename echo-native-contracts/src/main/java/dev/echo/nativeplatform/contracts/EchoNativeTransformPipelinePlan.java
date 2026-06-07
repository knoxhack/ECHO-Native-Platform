package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeTransformPipelinePlan(
        String planId,
        boolean planned,
        boolean transformPlanningOnly,
        boolean transformsEnabled,
        boolean bytecodeMutated,
        int plannedTransformCount,
        List<Map<String, Object>> plannedTransforms
) {
}
