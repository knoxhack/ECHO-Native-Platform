package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Native product config host. It records config surfaces and policy gates as
 * native product declarations, leaving actual user/server config persistence to
 * a live runtime adapter.
 */
public final class NativeLoaderConfigHost {
    public static final String SERVICE_ID = "echo.native.config_host";

    private final Map<String, ConfigEntry> configs = new LinkedHashMap<>();
    private final NativeLoaderLiveRuntimeBridge liveRuntimeBridge;
    private int liveRuntimeDispatchCount = 0;
    private int liveRuntimeMutationCount = 0;
    private int sequence = 0;
    private long liveRuntimeDispatchSequence = 0L;

    public NativeLoaderConfigHost() {
        this(NativeLoaderLiveRuntimeBridge.UNATTACHED);
    }

    public NativeLoaderConfigHost(NativeLoaderLiveRuntimeBridge liveRuntimeBridge) {
        this.liveRuntimeBridge = liveRuntimeBridge == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : liveRuntimeBridge;
    }

    public synchronized EchoNativeLoadStatus registerDescriptorDomain(
            String moduleId,
            String domain,
            Map<String, Object> evidence
    ) {
        if (!isConfigDomain(domain)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return registerConfig(
                moduleId,
                "descriptor." + normalized(domain),
                normalized(domain),
                evidenceWith(evidence, "source", "descriptor.adapterCore.domain")
        );
    }

    public synchronized EchoNativeLoadStatus registerConfig(
            String moduleId,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        String safeModuleId = value(moduleId, "unknown_module");
        String safeConfigId = value(configId, "");
        if (safeConfigId.isBlank()) {
            return EchoNativeLoadStatus.FAILED;
        }
        String key = safeModuleId + ":" + safeConfigId;
        if (configs.containsKey(key)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        Map<String, Object> safeEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        clearLiveDispatchProof(safeEvidence);
        String liveRuntimeDispatchId = beginLiveRuntimeDispatch("config_reloads", key, safeEvidence);
        EchoNativeLoadStatus liveStatus = dispatchLive(safeModuleId, safeConfigId, scope, safeEvidence);
        boolean liveDispatchProofSatisfied = liveDispatchProofSatisfied(liveStatus, safeEvidence, liveRuntimeDispatchId, "config_reloads");
        if (liveDispatchProofSatisfied) {
            liveRuntimeMutationCount++;
        }
        safeEvidence.put("nativeConfigHostRegistered", true);
        safeEvidence.put("nativeConfigHostExecutionMode", "native_product_config_declaration");
        safeEvidence.put("liveRuntimeBridgeStatus", liveStatus.name());
        safeEvidence.put("subsystemLiveRuntimeDispatchProofSatisfied", liveDispatchProofSatisfied);
        safeEvidence.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        safeEvidence.put("liveMinecraftMutation", liveDispatchProofSatisfied);
        safeEvidence.put("minecraftRuntimeAccessed", liveDispatchProofSatisfied);
        configs.put(key, new ConfigEntry(
                ++sequence,
                safeModuleId,
                safeConfigId,
                value(scope, "config"),
                Map.copyOf(safeEvidence)
        ));
        return EchoNativeLoadStatus.MUTATED;
    }

    public synchronized int registeredConfigCount() {
        return configs.size();
    }

    public synchronized int liveRuntimeMutationCount() {
        return liveRuntimeMutationCount;
    }

    public synchronized boolean liveRuntimeMutationCoverageSatisfied() {
        return liveRuntimeDispatchCount > 0
                && liveRuntimeDispatchCount == registeredConfigCount()
                && liveRuntimeMutationCount == liveRuntimeDispatchCount
                && configs.values().stream()
                .filter(config -> config.evidence().containsKey("liveRuntimeDispatchId"))
                .allMatch(config -> Boolean.TRUE.equals(config.evidence().get("liveMinecraftMutation")));
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
        report.put("registeredConfigCount", registeredConfigCount());
        report.put("configs", configs.values().stream().map(ConfigEntry::toReport).toList());
        report.put("liveRuntimeBridgeAttached", liveRuntimeBridge.attached());
        report.put("liveRuntimeDispatchCount", liveRuntimeDispatchCount);
        report.put("liveRuntimeMutationCount", liveRuntimeMutationCount);
        report.put("liveRuntimeUnmutatedConfigCount", Math.max(0, liveRuntimeDispatchCount - liveRuntimeMutationCount));
        report.put("liveRuntimeUndispatchedConfigCount", Math.max(0, registeredConfigCount() - liveRuntimeDispatchCount));
        report.put("liveRuntimeMutationCoverageSatisfied", liveRuntimeMutationCoverageSatisfied());
        report.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        report.put("partialLiveMinecraftMutation", liveRuntimeBridge.minecraftRuntimeAccessed() && liveRuntimeMutationCount > 0);
        report.put("liveMinecraftMutation", liveRuntimeReleaseProofSatisfied());
        report.put("minecraftRuntimeAccessed", liveRuntimeBridge.minecraftRuntimeAccessed());
        report.put("mirrorOnlyReleaseProof", registeredConfigCount() > 0 && liveRuntimeMutationCount == 0);
        report.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied());
        return Map.copyOf(report);
    }

    private EchoNativeLoadStatus dispatchLive(
            String moduleId,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        if (!liveRuntimeBridge.attached()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        liveRuntimeDispatchCount++;
        EchoNativeLoadStatus status;
        try {
            status = liveRuntimeBridge.reloadConfig(moduleId, configId, scope, evidence);
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
        if (!"config_reloads".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeConfigReloadTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeConfigReloadMutated"))
                && "world_save_file".equals(String.valueOf(evidence.get("runtimeSaveDataBackend")))
                && evidence.get("saveFile") instanceof String saveFile
                && !saveFile.isBlank();
    }

    private static void clearRuntimeSideEffectProof(Map<String, Object> evidence) {
        evidence.remove("runtimeSurfaceSaveTouched");
        evidence.remove("runtimeSurfaceSaveMutated");
        evidence.remove("runtimeSaveDataTouched");
        evidence.remove("runtimeSaveDataMutated");
        evidence.remove("liveSaveDataFileTouched");
        evidence.remove("runtimeSaveDataBackend");
        evidence.remove("saveFile");
        evidence.remove("runtimeConfigReloadTouched");
        evidence.remove("runtimeConfigReloadMutated");
        evidence.remove("runtimeConfigId");
        evidence.remove("runtimeConfigModuleId");
        evidence.remove("runtimeConfigScope");
    }

    private static boolean isConfigDomain(String domain) {
        String normalized = normalized(domain);
        return normalized.equals("config")
                || normalized.equals("configs")
                || normalized.equals("configuration")
                || normalized.equals("client.config")
                || normalized.equals("server.config")
                || normalized.endsWith(".config")
                || normalized.contains("config.");
    }

    private static Map<String, Object> evidenceWith(Map<String, Object> evidence, String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        result.put(key, value);
        return Map.copyOf(result);
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

    public record ConfigEntry(
            int sequence,
            String moduleId,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> safeEvidence = evidence == null ? Map.of() : Map.copyOf(evidence);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("moduleId", moduleId);
            report.put("configId", configId);
            report.put("scope", scope);
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
}
