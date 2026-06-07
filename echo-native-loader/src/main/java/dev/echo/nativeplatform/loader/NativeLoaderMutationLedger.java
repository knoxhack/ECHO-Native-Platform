package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderMutationLedger {
    private final List<MutationRecord> records = new ArrayList<>();

    public MutationRecord record(
            String surface,
            String action,
            String target,
            EchoNativeLoadStatus status,
            Object before,
            Object after,
            String serviceId,
            NativeLoaderResolvedRuntimeService resolvedService,
            String backendClass,
            String runtimeHostClass,
            String runtimeLane,
            String runtimeHostId,
            boolean runtimeHostRegistered,
            List<NativeLoaderResolvedRuntimeService> activeSurfaceServices,
            Map<String, Object> runtimeEvidence
    ) {
        Map<String, Object> safeRuntimeEvidence = runtimeEvidence == null ? Map.of() : Map.copyOf(runtimeEvidence);
        boolean surfaceReleaseProofSatisfied = releaseProofSurface(surface)
                && liveRuntimeReleaseProofSatisfied(surface, safeRuntimeEvidence);
        MutationRecord record = new MutationRecord(
                records.size() + 1,
                surface,
                action,
                target,
                status,
                value(before),
                value(after),
                serviceId,
                resolvedService == null ? "" : resolvedService.moduleId(),
                resolvedService == null ? "" : resolvedService.implementationClass(),
                backendClass,
                runtimeHostClass,
                runtimeLane,
                runtimeHostId,
                runtimeHostRegistered,
                bool(safeRuntimeEvidence.get("liveRuntimeAccessed")),
                bool(safeRuntimeEvidence.get("minecraftRuntimeAccessed")),
                bool(safeRuntimeEvidence.get("liveRuntimeMutationSupported")),
                bool(safeRuntimeEvidence.get("mirrorOnlyReleaseProof"))
                        && !surfaceReleaseProofSatisfied,
                surfaceReleaseProofSatisfied,
                liveRuntimeSurfaceMutationSatisfied(surface, safeRuntimeEvidence),
                intValue(safeRuntimeEvidence.get("liveRuntimeBridgeMutationCount")),
                activeSurfaceServices == null
                        ? List.of()
                        : activeSurfaceServices.stream()
                        .map(NativeLoaderResolvedRuntimeService::toReport)
                        .toList(),
                safeRuntimeEvidence
        );
        records.add(record);
        return record;
    }

    public List<MutationRecord> records() {
        return List.copyOf(records);
    }

    public int recordCount() {
        return records.size();
    }

    public int mutatedRecordCount() {
        int count = 0;
        for (MutationRecord record : records) {
            if (record.status() == EchoNativeLoadStatus.MUTATED) {
                count++;
            }
        }
        return count;
    }

    public int mutatedRecordCountBySurface(String surface) {
        String targetSurface = value(surface);
        if (targetSurface.isBlank()) {
            return 0;
        }
        int count = 0;
        for (MutationRecord record : records) {
            if (targetSurface.equals(record.surface()) && record.status() == EchoNativeLoadStatus.MUTATED) {
                count++;
            }
        }
        return count;
    }

    public int liveRuntimeProofRecordCount() {
        int count = 0;
        for (MutationRecord record : records) {
            if (liveRuntimeProofRecord(record)) {
                count++;
            }
        }
        return count;
    }

    public int liveRuntimeProofRecordCountBySurface(String surface) {
        String targetSurface = value(surface);
        if (targetSurface.isBlank()) {
            return 0;
        }
        int count = 0;
        for (MutationRecord record : records) {
            if (targetSurface.equals(record.surface()) && liveRuntimeProofRecord(record)) {
                count++;
            }
        }
        return count;
    }

    public List<Map<String, Object>> toReport() {
        return records.stream().map(MutationRecord::toReport).toList();
    }

    public List<EchoNativeMutationReceipt> receipts() {
        return records.stream().map(MutationRecord::toMutationReceipt).toList();
    }

    private static String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean liveRuntimeSurfaceMutationSatisfied(String surface, Map<String, Object> runtimeEvidence) {
        if (evidenceSurfaceMatches(surface, runtimeEvidence)
                && bool(runtimeEvidence.get("liveRuntimeSurfaceMutationSatisfied"))
                && concreteSurfaceProofSatisfied(surface, runtimeEvidence)) {
            return true;
        }
        if (evidenceSurfaceMatches(surface, runtimeEvidence)
                && bool(runtimeEvidence.get("surfaceLiveRuntimeReleaseProofSatisfied"))
                && bool(runtimeEvidence.get("surfaceLiveRuntimeAccessed"))
                && bool(runtimeEvidence.get("surfaceMinecraftRuntimeAccessed"))
                && concreteSurfaceProofSatisfied(surface, runtimeEvidence)) {
            return true;
        }
        return false;
    }

    private static boolean liveRuntimeReleaseProofSatisfied(String surface, Map<String, Object> runtimeEvidence) {
        return evidenceSurfaceMatches(surface, runtimeEvidence)
                && bool(runtimeEvidence.get("liveRuntimeAccessed"))
                && bool(runtimeEvidence.get("minecraftRuntimeAccessed"))
                && bool(runtimeEvidence.get("liveRuntimeMutationSupported"))
                && liveRuntimeSurfaceMutationSatisfied(surface, runtimeEvidence)
                && concreteSurfaceProofSatisfied(surface, runtimeEvidence)
                && (bool(runtimeEvidence.get("liveRuntimeReleaseProofSatisfied"))
                || bool(runtimeEvidence.get("surfaceLiveRuntimeReleaseProofSatisfied")));
    }

    private static boolean releaseProofSurface(String surface) {
        return !"feedback".equals(value(surface));
    }

    private static boolean liveRuntimeProofRecord(MutationRecord record) {
        return record != null
                && record.status() == EchoNativeLoadStatus.MUTATED
                && releaseProofSurface(record.surface())
                && record.liveRuntimeAccessed()
                && record.minecraftRuntimeAccessed()
                && record.liveRuntimeMutationSupported()
                && !record.mirrorOnlyReleaseProof()
                && record.liveRuntimeReleaseProofSatisfied()
                && record.liveRuntimeSurfaceMutationSatisfied()
                && concreteSurfaceProofSatisfied(record.surface(), record.runtimeEvidence());
    }

    private static boolean evidenceSurfaceMatches(String surface, Map<String, Object> runtimeEvidence) {
        return value(surface).equals(value(runtimeEvidence.get("adapterCoreSurface")));
    }

    private static boolean concreteSurfaceProofSatisfied(String surface, Map<String, Object> runtimeEvidence) {
        Map<String, Object> proof = objectMap(runtimeEvidence.get("surfaceLiveRuntimeProofEvidence"));
        boolean dispatchProof = bool(proof.get("subsystemLiveRuntimeDispatchProofSatisfied"))
                || bool(proof.get("liveRuntimeDispatchProofSatisfied"));
        boolean minecraftAccess = bool(proof.get("minecraftRuntimeAccessed"))
                || bool(proof.get("liveRuntimeDispatchMinecraftAccessed"));
        boolean liveMutation = bool(proof.get("liveMinecraftMutation"))
                || bool(proof.get("liveRuntimeDispatchLiveMutation"));
        return !proof.isEmpty()
                && dispatchProof
                && minecraftAccess
                && bool(proof.get("liveRuntimeDispatchMutationSupported"))
                && liveMutation
                && !value(proof.get("liveRuntimeDispatchId")).isBlank()
                && value(proof.get("liveRuntimeDispatchId")).equals(value(runtimeEvidence.get("adapterCoreSurfaceDispatchId")))
                && value(surface).equals(value(proof.get("liveRuntimeSurface")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, item) -> copy.put(String.valueOf(key), item));
            return Map.copyOf(copy);
        }
        return Map.of();
    }

    public record MutationRecord(
            int sequence,
            String surface,
            String action,
            String target,
            EchoNativeLoadStatus status,
            String before,
            String after,
            String serviceId,
            String resolvedModuleId,
            String resolvedServiceClass,
            String backendClass,
            String runtimeHostClass,
            String runtimeLane,
            String runtimeHostId,
            boolean runtimeHostRegistered,
            boolean liveRuntimeAccessed,
            boolean minecraftRuntimeAccessed,
            boolean liveRuntimeMutationSupported,
            boolean mirrorOnlyReleaseProof,
            boolean liveRuntimeReleaseProofSatisfied,
            boolean liveRuntimeSurfaceMutationSatisfied,
            int liveRuntimeBridgeMutationCount,
            List<Map<String, Object>> activeSurfaceServices,
            Map<String, Object> runtimeEvidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> safeRuntimeEvidence = runtimeEvidence == null ? Map.of() : Map.copyOf(runtimeEvidence);
            Map<String, Object> surfaceProofEvidence =
                    objectMap(safeRuntimeEvidence.get("surfaceLiveRuntimeProofEvidence"));
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("surface", surface);
            report.put("action", action);
            report.put("target", target);
            report.put("status", status.name());
            report.put("before", before);
            report.put("after", after);
            report.put("serviceId", serviceId);
            report.put("resolvedModuleId", resolvedModuleId);
            report.put("resolvedServiceClass", resolvedServiceClass);
            report.put("backendClass", backendClass);
            report.put("runtimeHostClass", runtimeHostClass);
            report.put("runtimeLane", runtimeLane);
            report.put("runtimeHostId", runtimeHostId);
            report.put("runtimeHostRegistered", runtimeHostRegistered);
            report.put("liveRuntimeAccessed", liveRuntimeAccessed);
            report.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
            report.put("liveRuntimeMutationSupported", liveRuntimeMutationSupported);
            report.put("mirrorOnlyReleaseProof", mirrorOnlyReleaseProof);
            report.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied);
            report.put("liveRuntimeSurfaceMutationSatisfied", liveRuntimeSurfaceMutationSatisfied);
            report.put("liveRuntimeBridgeMutationCount", liveRuntimeBridgeMutationCount);
            report.put("surfaceLiveRuntimeProofEvidence", surfaceProofEvidence);
            report.put("liveRuntimeDispatchId", value(surfaceProofEvidence.get("liveRuntimeDispatchId")));
            report.put("liveRuntimeSurface", value(surfaceProofEvidence.get("liveRuntimeSurface")));
            report.put("activeSurfaceServiceCount", activeSurfaceServices == null ? 0 : activeSurfaceServices.size());
            report.put("activeSurfaceServices", activeSurfaceServices == null ? List.of() : List.copyOf(activeSurfaceServices));
            report.put("runtimeEvidence", safeRuntimeEvidence);
            report.put("typedMutationReceipt", toMutationReceipt().toReport());
            return report;
        }

        public EchoNativeMutationReceipt toMutationReceipt() {
            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("before", before);
            evidence.put("after", after);
            evidence.put("resolvedModuleId", resolvedModuleId);
            evidence.put("resolvedServiceClass", resolvedServiceClass);
            evidence.put("backendClass", backendClass);
            evidence.put("runtimeHostClass", runtimeHostClass);
            evidence.put("runtimeLane", runtimeLane);
            evidence.put("runtimeHostId", runtimeHostId);
            evidence.put("runtimeHostRegistered", runtimeHostRegistered);
            evidence.put("liveRuntimeAccessed", liveRuntimeAccessed);
            evidence.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
            evidence.put("liveRuntimeMutationSupported", liveRuntimeMutationSupported);
            evidence.put("mirrorOnlyReleaseProof", mirrorOnlyReleaseProof);
            evidence.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied);
            evidence.put("liveRuntimeSurfaceMutationSatisfied", liveRuntimeSurfaceMutationSatisfied);
            evidence.put("liveRuntimeBridgeMutationCount", liveRuntimeBridgeMutationCount);
            evidence.put("activeSurfaceServices", activeSurfaceServices == null ? List.of() : List.copyOf(activeSurfaceServices));
            evidence.put("runtimeEvidence", runtimeEvidence == null ? Map.of() : Map.copyOf(runtimeEvidence));
            EchoNativeServiceMutation mutation = new EchoNativeServiceMutation(
                    resolvedModuleId == null || resolvedModuleId.isBlank() ? "unknown" : resolvedModuleId,
                    surface,
                    action,
                    target,
                    EchoNativeRuntimeSide.COMMON,
                    evidence
            );
            return new EchoNativeMutationReceipt(
                    mutation.moduleId(),
                    serviceId == null || serviceId.isBlank() ? "unknown" : serviceId,
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
    }
}
