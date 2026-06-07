package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeCommandService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.commands";
    }

    default EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt execute(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
