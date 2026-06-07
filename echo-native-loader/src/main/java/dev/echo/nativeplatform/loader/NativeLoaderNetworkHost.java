package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Native product network host. It binds declared AdapterCore packets and
 * consumers into a native dispatch table without claiming Minecraft networking
 * mutation.
 */
public final class NativeLoaderNetworkHost {
    public static final String SERVICE_ID = "echo.native.network_host";

    private final Map<String, PacketEntry> packets = new LinkedHashMap<>();
    private final List<Map<String, Object>> sourceReports = new ArrayList<>();
    private final List<HostFailure> failures = new ArrayList<>();
    private final NativeLoaderLiveRuntimeBridge liveRuntimeBridge;
    private int liveRuntimeDispatchCount = 0;
    private int liveRuntimeMutationCount = 0;
    private int sequence = 0;
    private long liveRuntimeDispatchSequence = 0L;

    public NativeLoaderNetworkHost() {
        this(NativeLoaderLiveRuntimeBridge.UNATTACHED);
    }

    public NativeLoaderNetworkHost(NativeLoaderLiveRuntimeBridge liveRuntimeBridge) {
        this.liveRuntimeBridge = liveRuntimeBridge == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : liveRuntimeBridge;
    }

    public synchronized EchoNativeLoadStatus registerDescriptorDomain(
            String moduleId,
            String domain,
            Map<String, Object> evidence
    ) {
        if (!isNetworkDomain(domain)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return registerPacket(
                moduleId,
                "descriptor." + normalized(domain),
                normalized(domain),
                "descriptor.adapter_core_domain",
                List.of(),
                evidenceWith(evidence, "source", "descriptor.adapterCore.domain")
        );
    }

    public synchronized EchoNativeLoadStatus registerPacketReport(String moduleId, Map<String, Object> report) {
        if (report == null || report.isEmpty()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        sourceReports.add(Map.copyOf(report));
        int registered = 0;
        int resolved = 0;
        for (Map<String, Object> packet : objectList(report.get("packets"))) {
            EchoNativeLoadStatus status = registerPacket(
                    moduleId,
                    string(packet.get("id")),
                    string(packet.get("surface")),
                    string(packet.get("sourceRuntimeTarget")),
                    stringList(packet.get("consumers")),
                    evidenceWith(packet, "sourceReportId", string(report.get("id")))
            );
            if (status == EchoNativeLoadStatus.REGISTERED || status == EchoNativeLoadStatus.MUTATED) {
                registered++;
            } else if (status == EchoNativeLoadStatus.RESOLVED) {
                resolved++;
            }
        }
        if (registered > 0) {
            return EchoNativeLoadStatus.MUTATED;
        }
        return resolved > 0 ? EchoNativeLoadStatus.RESOLVED : EchoNativeLoadStatus.UNSUPPORTED;
    }

    public synchronized EchoNativeLoadStatus registerDeclaredPacket(
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        return registerPacket(
                moduleId,
                packetId,
                surface,
                sourceRuntimeTarget,
                consumers == null ? List.of() : List.copyOf(consumers),
                evidence
        );
    }

    public synchronized List<PacketEntry> packets() {
        return List.copyOf(packets.values());
    }

    public synchronized int boundPacketCount() {
        return packets.size();
    }

    public synchronized int packetFailureCount() {
        return failures.size();
    }

    public synchronized int liveRuntimeMutationCount() {
        return liveRuntimeMutationCount;
    }

    public synchronized boolean liveRuntimeMutationCoverageSatisfied() {
        return liveRuntimeDispatchCount > 0
                && liveRuntimeDispatchCount == boundPacketCount()
                && liveRuntimeMutationCount == liveRuntimeDispatchCount
                && packets.values().stream()
                .filter(packet -> packet.evidence().containsKey("liveRuntimeDispatchId"))
                .allMatch(packet -> Boolean.TRUE.equals(packet.evidence().get("liveMinecraftMutation")));
    }

    public synchronized boolean liveRuntimeReleaseProofSatisfied() {
        return liveRuntimeBridge.attached()
                && liveRuntimeBridge.liveRuntimeAccessed()
                && liveRuntimeBridge.minecraftRuntimeAccessed()
                && liveRuntimeBridge.liveRuntimeMutationSupported()
                && liveRuntimeMutationCoverageSatisfied();
    }

    public synchronized Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serviceId", SERVICE_ID);
        report.put("boundPacketCount", boundPacketCount());
        report.put("sourceReportCount", sourceReports.size());
        report.put("failureCount", packetFailureCount());
        report.put("packets", packets.values().stream().map(PacketEntry::toReport).toList());
        report.put("failures", failures.stream().map(HostFailure::toReport).toList());
        report.put("liveRuntimeBridgeAttached", liveRuntimeBridge.attached());
        report.put("liveRuntimeDispatchCount", liveRuntimeDispatchCount);
        report.put("liveRuntimeMutationCount", liveRuntimeMutationCount);
        report.put("liveRuntimeUnmutatedPacketCount", Math.max(0, liveRuntimeDispatchCount - liveRuntimeMutationCount));
        report.put("liveRuntimeUndispatchedPacketCount", Math.max(0, boundPacketCount() - liveRuntimeDispatchCount));
        report.put("liveRuntimeMutationCoverageSatisfied", liveRuntimeMutationCoverageSatisfied());
        report.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        report.put("partialLiveMinecraftMutation", liveRuntimeBridge.minecraftRuntimeAccessed() && liveRuntimeMutationCount > 0);
        report.put("liveMinecraftMutation", liveRuntimeReleaseProofSatisfied());
        report.put("minecraftRuntimeAccessed", liveRuntimeBridge.minecraftRuntimeAccessed());
        report.put("mirrorOnlyReleaseProof", boundPacketCount() > 0 && liveRuntimeMutationCount == 0);
        report.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied());
        return Map.copyOf(report);
    }

    private EchoNativeLoadStatus registerPacket(
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        String safeModuleId = value(moduleId, "unknown_module");
        String safePacketId = value(packetId, "");
        if (safePacketId.isBlank()) {
            failures.add(new HostFailure(++sequence, safeModuleId, "", "packet id is required"));
            return EchoNativeLoadStatus.FAILED;
        }
        String key = safeModuleId + ":" + safePacketId;
        if (packets.containsKey(key)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        Map<String, Object> safeEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        clearLiveDispatchProof(safeEvidence);
        String liveRuntimeDispatchId = beginLiveRuntimeDispatch("network_channels", key, safeEvidence);
        EchoNativeLoadStatus liveStatus = dispatchLive(
                safeModuleId,
                safePacketId,
                surface,
                sourceRuntimeTarget,
                consumers,
                safeEvidence
        );
        boolean liveDispatchProofSatisfied = liveDispatchProofSatisfied(liveStatus, safeEvidence, liveRuntimeDispatchId, "network_channels");
        if (liveDispatchProofSatisfied) {
            liveRuntimeMutationCount++;
        }
        safeEvidence.put("nativeNetworkHostBound", true);
        safeEvidence.put("nativeNetworkHostExecutionMode", "native_product_packet_dispatch_table");
        safeEvidence.put("runtimePacketConsumedByNativeHost", true);
        safeEvidence.put("liveRuntimeBridgeStatus", liveStatus.name());
        safeEvidence.put("subsystemLiveRuntimeDispatchProofSatisfied", liveDispatchProofSatisfied);
        safeEvidence.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        safeEvidence.put("liveMinecraftMutation", liveDispatchProofSatisfied);
        safeEvidence.put("minecraftRuntimeAccessed", liveDispatchProofSatisfied);
        packets.put(key, new PacketEntry(
                ++sequence,
                safeModuleId,
                safePacketId,
                value(surface, "networking"),
                value(sourceRuntimeTarget, ""),
                consumers == null ? List.of() : List.copyOf(consumers),
                Map.copyOf(safeEvidence)
        ));
        return EchoNativeLoadStatus.MUTATED;
    }

    private EchoNativeLoadStatus dispatchLive(
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        if (!liveRuntimeBridge.attached()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        liveRuntimeDispatchCount++;
        EchoNativeLoadStatus status;
        try {
            status = liveRuntimeBridge.registerNetworkPacket(
                    moduleId,
                    packetId,
                    surface,
                    sourceRuntimeTarget,
                    consumers == null ? List.of() : List.copyOf(consumers),
                    evidence
            );
        } catch (RuntimeException exception) {
            return EchoNativeLoadStatus.FAILED;
        }
        return status == null ? EchoNativeLoadStatus.FAILED : status;
    }

    private boolean liveDispatchProofSatisfied(
            EchoNativeLoadStatus status,
            Map<String, Object> evidence,
            String dispatchId,
            String surface
    ) {
        return status == EchoNativeLoadStatus.MUTATED
                && liveRuntimeBridge.liveRuntimeAccessed()
                && liveRuntimeBridge.minecraftRuntimeAccessed()
                && liveRuntimeBridge.liveRuntimeMutationSupported()
                && bool(evidence.get("liveRuntimeDispatchProofSatisfied"))
                && bool(evidence.get("liveRuntimeDispatchMinecraftAccessed"))
                && bool(evidence.get("liveRuntimeDispatchMutationSupported"))
                && bool(evidence.get("liveRuntimeDispatchLiveMutation"))
                && dispatchId != null
                && dispatchId.equals(String.valueOf(evidence.getOrDefault("liveRuntimeDispatchId", "")))
                && liveRuntimeSurfaceMatches(surface, evidence)
                && subsystemRuntimeSideEffectSatisfied(surface, evidence);
    }

    private static void clearLiveDispatchProof(Map<String, Object> evidence) {
        evidence.remove("liveRuntimeDispatchProofSatisfied");
        evidence.remove("liveRuntimeDispatchMinecraftAccessed");
        evidence.remove("liveRuntimeDispatchMutationSupported");
        evidence.remove("liveRuntimeDispatchLiveMutation");
        evidence.remove("liveRuntimeDispatchId");
        evidence.remove("liveRuntimeSurface");
        evidence.remove("liveMinecraftMutation");
        evidence.remove("minecraftRuntimeAccessed");
        clearRuntimeSideEffectProof(evidence);
    }

    private String beginLiveRuntimeDispatch(String surface, String key, Map<String, Object> evidence) {
        String dispatchId = SERVICE_ID + ":" + surface + ":" + (++liveRuntimeDispatchSequence);
        evidence.put("liveRuntimeDispatchId", dispatchId);
        liveRuntimeBridge.beginLiveRuntimeSurfaceDispatch(surface, dispatchId);
        return dispatchId;
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static boolean liveRuntimeSurfaceMatches(String surface, Map<String, Object> evidence) {
        String actual = String.valueOf(evidence.getOrDefault("liveRuntimeSurface", "")).trim();
        return !actual.isBlank() && actual.equals(surface == null ? "" : surface);
    }

    private static boolean subsystemRuntimeSideEffectSatisfied(String surface, Map<String, Object> evidence) {
        if (!"network_channels".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank()
                && Boolean.TRUE.equals(evidence.get("runtimeSurfacePacketSent"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfacePacketMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeNetworkChannelTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeNetworkChannelMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeNetworkPacketSent"));
    }

    private static void clearRuntimeSideEffectProof(Map<String, Object> evidence) {
        evidence.remove("runtimeSurfaceSaveTouched");
        evidence.remove("runtimeSurfaceSaveMutated");
        evidence.remove("runtimeSaveDataTouched");
        evidence.remove("runtimeSaveDataMutated");
        evidence.remove("liveSaveDataFileTouched");
        evidence.remove("runtimeSaveDataBackend");
        evidence.remove("saveFile");
        evidence.remove("runtimeSurfacePacketSent");
        evidence.remove("runtimeSurfacePacketMutated");
        evidence.remove("runtimeNetworkChannelTouched");
        evidence.remove("runtimeNetworkChannelMutated");
        evidence.remove("runtimeNetworkPacketSent");
        evidence.remove("runtimeNetworkChannelId");
        evidence.remove("runtimeNetworkModuleId");
        evidence.remove("runtimeNetworkConsumers");
    }

    private static boolean isNetworkDomain(String domain) {
        String normalized = normalized(domain);
        return normalized.equals("networking")
                || normalized.equals("network")
                || normalized.equals("net")
                || normalized.equals("network.payload")
                || normalized.equals("network.payloads")
                || normalized.equals("packet")
                || normalized.equals("packets")
                || normalized.equals("payload")
                || normalized.equals("payloads")
                || normalized.endsWith(".packet")
                || normalized.endsWith(".packets")
                || normalized.endsWith(".payload")
                || normalized.endsWith(".payloads");
    }

    private static Map<String, Object> evidenceWith(Map<String, Object> evidence, String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        result.put(key, value);
        return Map.copyOf(result);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> object = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        object.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
                result.add(Map.copyOf(object));
            }
        }
        return List.copyOf(result);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            String text = string(item);
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return List.copyOf(result);
    }

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalized(String value) {
        return string(value).toLowerCase(Locale.ROOT).replace('-', '.').replace('_', '.');
    }

    public record PacketEntry(
            int sequence,
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> safeEvidence = evidence == null ? Map.of() : Map.copyOf(evidence);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("moduleId", moduleId);
            report.put("packetId", packetId);
            report.put("surface", surface);
            report.put("sourceRuntimeTarget", sourceRuntimeTarget);
            report.put("consumers", consumers == null ? List.of() : List.copyOf(consumers));
            report.put("liveRuntimeDispatchId", string(safeEvidence.get("liveRuntimeDispatchId")));
            report.put("liveRuntimeSurface", string(safeEvidence.get("liveRuntimeSurface")));
            report.put("subsystemLiveRuntimeDispatchProofSatisfied",
                    Boolean.TRUE.equals(safeEvidence.get("subsystemLiveRuntimeDispatchProofSatisfied")));
            report.put("liveMinecraftMutation", Boolean.TRUE.equals(safeEvidence.get("liveMinecraftMutation")));
            report.put("minecraftRuntimeAccessed", Boolean.TRUE.equals(safeEvidence.get("minecraftRuntimeAccessed")));
            report.put("evidence", safeEvidence);
            return Map.copyOf(report);
        }
    }

    public record HostFailure(int sequence, String moduleId, String target, String reason) {
        public Map<String, Object> toReport() {
            return Map.of(
                    "sequence", sequence,
                    "moduleId", moduleId,
                    "target", target,
                    "reason", reason
            );
        }
    }
}
