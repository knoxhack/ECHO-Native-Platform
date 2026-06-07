package dev.echo.nativeplatform.contracts;

import java.util.ArrayList;
import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public final class EchoNativeMutationLedger {
    private final List<EchoNativeMutationReceipt> receipts = new ArrayList<>();

    public synchronized EchoNativeMutationReceipt append(EchoNativeMutationReceipt receipt) {
        EchoNativeMutationReceipt next = receipt.sequence() == receipts.size() + 1
                ? receipt
                : new EchoNativeMutationReceipt(
                        receipt.moduleId(),
                        receipt.serviceId(),
                        receipt.surface(),
                        receipt.action(),
                        receipt.target(),
                        receipt.status(),
                        receipt.side(),
                        receipt.receiptId(),
                        receipts.size() + 1L,
                        receipt.evidence());
        receipts.add(next);
        return next;
    }

    public synchronized EchoNativeMutationReceipt append(
            String serviceId,
            EchoNativeServiceMutation mutation,
            EchoNativeLoadStatus status
    ) {
        return append(EchoNativeMutationReceipt.from(serviceId, mutation, status, receipts.size() + 1L));
    }

    public synchronized List<EchoNativeMutationReceipt> receipts() {
        return List.copyOf(receipts);
    }

    public synchronized boolean hasMutations() {
        return receipts.stream().anyMatch(EchoNativeMutationReceipt::mutated);
    }
}
