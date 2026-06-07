package dev.echo.nativeplatform.contracts;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeMutationReceipt(
        String moduleId,
        String serviceId,
        String surface,
        String action,
        String target,
        EchoNativeLoadStatus status,
        EchoNativeRuntimeSide side,
        String receiptId,
        long sequence,
        Map<String, Object> evidence
) {
    public EchoNativeMutationReceipt {
        moduleId = requireText(moduleId, "moduleId");
        serviceId = requireText(serviceId, "serviceId");
        surface = requireText(surface, "surface");
        action = requireText(action, "action");
        target = optionalText(target);
        status = Objects.requireNonNull(status, "status");
        side = side == null ? EchoNativeRuntimeSide.UNKNOWN : side;
        receiptId = receiptId == null || receiptId.isBlank()
                ? moduleId + ":" + serviceId + ":" + surface + ":" + action + ":" + Math.max(0, sequence)
                : receiptId.trim();
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must not be negative");
        }
        evidence = Map.copyOf(Objects.requireNonNullElse(evidence, Map.of()));
    }

    public static EchoNativeMutationReceipt mutated(String serviceId, EchoNativeServiceMutation mutation, long sequence) {
        return from(serviceId, mutation, EchoNativeLoadStatus.MUTATED, sequence);
    }

    public static EchoNativeMutationReceipt registered(String serviceId, EchoNativeServiceMutation mutation, long sequence) {
        return from(serviceId, mutation, EchoNativeLoadStatus.REGISTERED, sequence);
    }

    public static EchoNativeMutationReceipt unsupported(String serviceId, EchoNativeServiceMutation mutation) {
        return from(serviceId, mutation, EchoNativeLoadStatus.UNSUPPORTED, 0);
    }

    public static EchoNativeMutationReceipt failed(String serviceId, EchoNativeServiceMutation mutation, String reason) {
        Map<String, Object> evidence = new LinkedHashMap<>(mutation == null ? Map.of() : mutation.evidence());
        evidence.put("failureReason", optionalText(reason));
        EchoNativeServiceMutation failed = mutation == null
                ? new EchoNativeServiceMutation("unknown", "unknown", "unknown", "", EchoNativeRuntimeSide.UNKNOWN, evidence)
                : new EchoNativeServiceMutation(mutation.moduleId(), mutation.surface(), mutation.action(), mutation.target(), mutation.side(), evidence);
        return from(serviceId, failed, EchoNativeLoadStatus.FAILED, 0);
    }

    public static EchoNativeMutationReceipt from(
            String serviceId,
            EchoNativeServiceMutation mutation,
            EchoNativeLoadStatus status,
            long sequence
    ) {
        Objects.requireNonNull(mutation, "mutation");
        return new EchoNativeMutationReceipt(
                mutation.moduleId(),
                serviceId,
                mutation.surface(),
                mutation.action(),
                mutation.target(),
                status,
                mutation.side(),
                "",
                sequence,
                mutation.evidence()
        );
    }

    public boolean mutated() {
        return status == EchoNativeLoadStatus.MUTATED;
    }

    public Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("serviceId", serviceId);
        report.put("surface", surface);
        report.put("action", action);
        report.put("target", target);
        report.put("status", status.name());
        report.put("side", side.name());
        report.put("receiptId", receiptId);
        report.put("sequence", sequence);
        report.put("evidence", evidence);
        return Map.copyOf(report);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }
}
