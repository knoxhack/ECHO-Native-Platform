package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeConfigService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.config";
    }

    default EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt reload(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt write(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
