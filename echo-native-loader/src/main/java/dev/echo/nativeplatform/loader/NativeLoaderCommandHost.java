package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Native product command host. It consumes descriptor and AdapterCore command
 * declarations into a launcher-owned command queue without claiming live
 * Minecraft command execution.
 */
public final class NativeLoaderCommandHost {
    public static final String SERVICE_ID = "echo.native.command_host";

    private final Map<String, CommandEntry> commands = new LinkedHashMap<>();
    private final List<Map<String, Object>> sourceReports = new ArrayList<>();
    private final List<HostFailure> failures = new ArrayList<>();
    private final NativeLoaderLiveRuntimeBridge liveRuntimeBridge;
    private int liveRuntimeDispatchCount = 0;
    private int liveRuntimeMutationCount = 0;
    private int sequence = 0;
    private long liveRuntimeDispatchSequence = 0L;

    public NativeLoaderCommandHost() {
        this(NativeLoaderLiveRuntimeBridge.UNATTACHED);
    }

    public NativeLoaderCommandHost(NativeLoaderLiveRuntimeBridge liveRuntimeBridge) {
        this.liveRuntimeBridge = liveRuntimeBridge == null
                ? NativeLoaderLiveRuntimeBridge.UNATTACHED
                : liveRuntimeBridge;
    }

    public synchronized EchoNativeLoadStatus registerDescriptorDomain(
            String moduleId,
            String domain,
            Map<String, Object> evidence
    ) {
        if (!isCommandDomain(domain)) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        return registerDeclaredCommand(
                moduleId,
                "descriptor." + normalized(domain),
                normalized(domain),
                "descriptor.adapter_core_domain",
                evidenceWith(evidence, "source", "descriptor.adapterCore.domain")
        );
    }

    public synchronized EchoNativeLoadStatus registerDeclaredCommand(
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            Map<String, Object> evidence
    ) {
        return registerCommand(
                moduleId,
                commandId,
                targetSurface,
                targetBridge,
                "declared",
                evidence
        );
    }

    public synchronized EchoNativeLoadStatus registerCommandReport(String moduleId, Map<String, Object> report) {
        if (report == null || report.isEmpty()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        sourceReports.add(Map.copyOf(report));
        int registered = 0;
        int resolved = 0;
        for (Map<String, Object> command : objectList(report.get("commands"))) {
            String commandId = string(command.get("operationId"));
            if (commandId.isBlank()) {
                commandId = string(command.get("id"));
            }
            EchoNativeLoadStatus status = registerCommand(
                    moduleId,
                    commandId,
                    string(command.get("targetSurface")),
                    string(command.get("targetBridge")),
                    string(command.get("status")),
                    evidenceWith(command, "sourceReportId", string(report.get("id")))
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

    public synchronized List<CommandEntry> commands() {
        return List.copyOf(commands.values());
    }

    public synchronized int queuedCommandCount() {
        return commands.size();
    }

    public synchronized int commandFailureCount() {
        return failures.size();
    }

    public synchronized int liveRuntimeMutationCount() {
        return liveRuntimeMutationCount;
    }

    public synchronized boolean liveRuntimeMutationCoverageSatisfied() {
        return liveRuntimeDispatchCount > 0
                && liveRuntimeDispatchCount == queuedCommandCount()
                && liveRuntimeMutationCount == liveRuntimeDispatchCount
                && commands.values().stream()
                .filter(command -> command.evidence().containsKey("liveRuntimeDispatchId"))
                .allMatch(command -> Boolean.TRUE.equals(command.evidence().get("liveMinecraftMutation")));
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
        report.put("queuedCommandCount", queuedCommandCount());
        report.put("sourceReportCount", sourceReports.size());
        report.put("failureCount", commandFailureCount());
        report.put("commands", commands.values().stream().map(CommandEntry::toReport).toList());
        report.put("failures", failures.stream().map(HostFailure::toReport).toList());
        report.put("liveRuntimeBridgeAttached", liveRuntimeBridge.attached());
        report.put("liveRuntimeDispatchCount", liveRuntimeDispatchCount);
        report.put("liveRuntimeMutationCount", liveRuntimeMutationCount);
        report.put("liveRuntimeUnmutatedCommandCount", Math.max(0, liveRuntimeDispatchCount - liveRuntimeMutationCount));
        report.put("liveRuntimeUndispatchedCommandCount", Math.max(0, queuedCommandCount() - liveRuntimeDispatchCount));
        report.put("liveRuntimeMutationCoverageSatisfied", liveRuntimeMutationCoverageSatisfied());
        report.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        report.put("partialLiveMinecraftMutation", liveRuntimeBridge.minecraftRuntimeAccessed() && liveRuntimeMutationCount > 0);
        report.put("liveMinecraftMutation", liveRuntimeReleaseProofSatisfied());
        report.put("minecraftRuntimeAccessed", liveRuntimeBridge.minecraftRuntimeAccessed());
        report.put("mirrorOnlyReleaseProof", queuedCommandCount() > 0 && liveRuntimeMutationCount == 0);
        report.put("liveRuntimeReleaseProofSatisfied", liveRuntimeReleaseProofSatisfied());
        return Map.copyOf(report);
    }

    private EchoNativeLoadStatus registerCommand(
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            String state,
            Map<String, Object> evidence
    ) {
        String safeModuleId = value(moduleId, "unknown_module");
        String safeCommandId = value(commandId, "");
        if (safeCommandId.isBlank()) {
            failures.add(new HostFailure(++sequence, safeModuleId, "", "command id is required"));
            return EchoNativeLoadStatus.FAILED;
        }
        String key = safeModuleId + ":" + safeCommandId;
        if (commands.containsKey(key)) {
            return EchoNativeLoadStatus.RESOLVED;
        }
        Map<String, Object> safeEvidence = new LinkedHashMap<>(evidence == null ? Map.of() : evidence);
        clearLiveDispatchProof(safeEvidence);
        String liveRuntimeDispatchId = beginLiveRuntimeDispatch("commands", key, safeEvidence);
        EchoNativeLoadStatus liveStatus = dispatchLive(safeModuleId, safeCommandId, targetSurface, targetBridge, safeEvidence);
        boolean liveDispatchProofSatisfied = liveDispatchProofSatisfied(liveStatus, safeEvidence, liveRuntimeDispatchId, "commands");
        if (liveDispatchProofSatisfied) {
            liveRuntimeMutationCount++;
        }
        safeEvidence.put("nativeCommandHostQueued", true);
        safeEvidence.put("nativeCommandHostExecutionMode", "native_product_command_queue");
        safeEvidence.put("commandQueueConsumedByNativeHost", true);
        safeEvidence.put("liveRuntimeBridgeStatus", liveStatus.name());
        safeEvidence.put("subsystemLiveRuntimeDispatchProofSatisfied", liveDispatchProofSatisfied);
        safeEvidence.put("liveRuntimeAccessed", liveRuntimeBridge.liveRuntimeAccessed());
        safeEvidence.put("liveMinecraftMutation", liveDispatchProofSatisfied);
        safeEvidence.put("minecraftRuntimeAccessed", liveDispatchProofSatisfied);
        commands.put(key, new CommandEntry(
                ++sequence,
                safeModuleId,
                safeCommandId,
                value(targetSurface, "commands"),
                value(targetBridge, ""),
                value(state, "declared"),
                Map.copyOf(safeEvidence)
        ));
        return EchoNativeLoadStatus.MUTATED;
    }

    private EchoNativeLoadStatus dispatchLive(
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            Map<String, Object> evidence
    ) {
        if (!liveRuntimeBridge.attached()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        liveRuntimeDispatchCount++;
        EchoNativeLoadStatus status;
        try {
            status = liveRuntimeBridge.registerCommand(moduleId, commandId, targetSurface, targetBridge, evidence);
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
        if (!"commands".equals(surface)) {
            return true;
        }
        return Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeSurfaceSaveMutated"))
                && Boolean.TRUE.equals(evidence.get("runtimeSaveDataTouched"))
                && Boolean.TRUE.equals(evidence.get("liveSaveDataFileTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeCommandRegistryTouched"))
                && Boolean.TRUE.equals(evidence.get("runtimeCommandRegistryMutated"))
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
        evidence.remove("runtimeCommandRegistryTouched");
        evidence.remove("runtimeCommandRegistryMutated");
        evidence.remove("runtimeCommandId");
        evidence.remove("runtimeCommandModuleId");
        evidence.remove("runtimeCommandTargetSurface");
        evidence.remove("runtimeCommandTargetBridge");
    }

    private static boolean isCommandDomain(String domain) {
        String normalized = normalized(domain);
        return normalized.equals("command")
                || normalized.equals("commands")
                || normalized.equals("server.commands")
                || normalized.endsWith(".commands")
                || normalized.equals("commands.register");
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

    private static String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String normalized(String value) {
        return string(value).toLowerCase(Locale.ROOT).replace('-', '.').replace('_', '.');
    }

    public record CommandEntry(
            int sequence,
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            String state,
            Map<String, Object> evidence
    ) {
        public Map<String, Object> toReport() {
            Map<String, Object> safeEvidence = evidence == null ? Map.of() : Map.copyOf(evidence);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("sequence", sequence);
            report.put("moduleId", moduleId);
            report.put("commandId", commandId);
            report.put("targetSurface", targetSurface);
            report.put("targetBridge", targetBridge);
            report.put("state", state);
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
