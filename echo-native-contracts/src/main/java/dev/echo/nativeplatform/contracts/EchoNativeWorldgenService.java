package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeWorldgenService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.worldgen";
    }

    default EchoNativeMutationReceipt registerFeature(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt placeStructure(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
