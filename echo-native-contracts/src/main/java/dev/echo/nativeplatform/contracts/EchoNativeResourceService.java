package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeResourceService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.resources";
    }

    default EchoNativeMutationReceipt registerReloadListener(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt reload(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt applyResourcePack(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt runDatagen(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt hotReload(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }
}
