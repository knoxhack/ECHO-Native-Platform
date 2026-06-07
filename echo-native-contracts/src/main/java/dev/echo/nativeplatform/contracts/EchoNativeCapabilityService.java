package dev.echo.nativeplatform.contracts;

import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeCapabilityService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.capabilities";
    }

    default EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt mutate(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerIntegration(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeCapabilityNegotiation negotiate(EchoNativeServiceMutation mutation) {
        return EchoNativeCapabilityNegotiation.unsupported(serviceId(), mutation);
    }

    default Map<String, Object> read(EchoNativeServiceMutation mutation) {
        return Map.of("status", EchoNativeLoadStatus.UNSUPPORTED.name(), "serviceId", serviceId());
    }
}
