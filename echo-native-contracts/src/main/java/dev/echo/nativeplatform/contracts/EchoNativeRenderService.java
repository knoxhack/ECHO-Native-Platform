package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeRenderService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.render";
    }

    default EchoNativeMutationReceipt registerLayer(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerRenderHook(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerHudOverlay(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt renderTick(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
