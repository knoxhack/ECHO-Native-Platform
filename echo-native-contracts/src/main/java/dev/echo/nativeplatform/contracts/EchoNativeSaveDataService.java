package dev.echo.nativeplatform.contracts;

import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeSaveDataService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.save_data";
    }

    default EchoNativeMutationReceipt write(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt delete(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default Map<String, Object> read(EchoNativeServiceMutation mutation) {
        return Map.of("status", EchoNativeLoadStatus.UNSUPPORTED.name(), "serviceId", serviceId());
    }
}
