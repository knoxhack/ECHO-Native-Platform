package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeScreenService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.screens";
    }

    default EchoNativeMutationReceipt registerSurface(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerMenu(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerKeybind(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt open(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt close(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
