package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeTypedServiceSupport {
    String serviceId();

    default EchoNativeMutationReceipt unsupported(EchoNativeServiceMutation mutation) {
        return EchoNativeMutationReceipt.unsupported(serviceId(), mutation);
    }
}
