package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeFailureContainmentMatrix(
        String matrixId,
        boolean contained,
        boolean deterministicDiagnostics,
        boolean supportBundlePlannedOnly,
        int failureCaseCount,
        List<Map<String, Object>> failureCases
) {
}
