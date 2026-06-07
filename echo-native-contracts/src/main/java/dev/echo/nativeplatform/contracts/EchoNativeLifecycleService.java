package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeLifecycleService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.lifecycle";
    }

    default EchoNativeMutationReceipt phase(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerGameTest(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt runGameTest(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeRuntimeLane runtimeLane(EchoNativeServiceMutation mutation) {
        return EchoNativeRuntimeLane.UNKNOWN;
    }

    default EchoNativeParityReport parityReport(EchoNativeServiceMutation mutation) {
        return EchoNativeParityReport.empty(mutation == null ? "" : mutation.moduleId());
    }

    default EchoNativeModuleHealthTelemetry healthTelemetry(EchoNativeServiceMutation mutation) {
        return EchoNativeModuleHealthTelemetry.empty(mutation == null ? "" : mutation.moduleId());
    }

    default EchoNativeDependencyGraphDiagnostics dependencyGraph(EchoNativeServiceMutation mutation) {
        return EchoNativeDependencyGraphDiagnostics.empty(mutation == null ? "" : mutation.moduleId());
    }

    default EchoNativeMutationReceipt shutdown(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
