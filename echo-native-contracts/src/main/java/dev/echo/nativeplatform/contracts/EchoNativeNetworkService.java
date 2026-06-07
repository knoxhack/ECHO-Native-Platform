package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeNetworkService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.network";
    }

    default EchoNativeMutationReceipt registerPacket(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt sendToPlayer(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt broadcast(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
