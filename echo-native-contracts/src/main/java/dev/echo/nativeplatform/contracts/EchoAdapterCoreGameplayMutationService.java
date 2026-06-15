package dev.echo.nativeplatform.contracts;

/**
 * Typed backend for AdapterCore gameplay mutations.
 *
 * <p>Implementations should return host-created receipts for real runtime
 * state changes. Unsupported, queued, or diagnostic-only handoffs must not be
 * represented as successful {@code MUTATED} proof.</p>
 */
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoAdapterCoreGameplayMutationService extends EchoNativeTypedServiceSupport {
    @Override
    default String serviceId() {
        return "echo.adaptercore.gameplay_mutation";
    }

    default EchoNativeMutationReceipt mutate(EchoNativeServiceMutation mutation) {
        return unsupported(mutation);
    }

    default EchoNativeMutationReceipt inventory(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt playerState(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt worldBlock(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt structure(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt blockEntity(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt capability(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt saveData(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }

    default EchoNativeMutationReceipt hudOrEvent(EchoNativeServiceMutation mutation) {
        return mutate(mutation);
    }
}
