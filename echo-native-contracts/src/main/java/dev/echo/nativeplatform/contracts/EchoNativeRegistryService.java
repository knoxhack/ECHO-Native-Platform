package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeRegistryService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.native.registry";
    }

    default EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt deferredRegister(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerDataComponent(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerBlockEntity(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerCreativeTab(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerLootModifier(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerRecipe(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt registerTag(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeRegistryContentSnapshot snapshot(String moduleId) {
        return new EchoNativeRegistryContentSnapshot(java.util.List.of(), java.util.List.of(), java.util.List.of());
    }
}
