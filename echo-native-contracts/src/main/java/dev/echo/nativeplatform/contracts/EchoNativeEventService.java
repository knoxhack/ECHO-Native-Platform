package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeEventService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.events";
    }

    default EchoNativeMutationReceipt publish(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt subscribe(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
