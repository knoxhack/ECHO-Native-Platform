package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeCrashHardeningCoverage(
        String coverageId,
        boolean covered,
        boolean diagnosticsCaptured,
        boolean supportBundlePlannedOnly,
        int coveredReportCount,
        List<String> coveredReports
) {
}
