package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeReportStatus {
    PASS,
    PASS_WITH_WARNINGS,
    DEGRADED,
    FAILED,
    BLOCKED,
    NOT_RUN,
    UNKNOWN
}
