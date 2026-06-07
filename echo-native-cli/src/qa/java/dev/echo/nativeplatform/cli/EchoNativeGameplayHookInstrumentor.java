package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EchoNativeGameplayHookInstrumentor {
    EchoNativeGameplayHookInstrumentationOutcome instrument(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> m24Completion = data(reports.get("phase13-m24-completion.json"));
        Map<String, Object> m25Readiness = data(reports.get("phase13-m25-readiness.json"));
        Map<String, Object> contract = data(reports.get("gameplay-hook-signal-contract.json"));
        Map<String, Object> status = data(reports.get("gameplay-hook-signal-status.json"));

        boolean m24Complete = bool(m24Completion, "phase13M24Complete");
        boolean m25Ready = bool(m25Readiness, "phase13M25Ready");
        List<String> modules = contractModules(contract);
        String signalPathText = String.valueOf(contract.getOrDefault(
                "signalPath",
                "fixtures/" + fixture.getFileName() + "/isolated-runtime/game/echo-native/gameplay-hooks.json"
        ));
        Path signalPath = Path.of("").toAbsolutePath().normalize().resolve(signalPathText).normalize();
        if (!signalPath.startsWith(fixture.toAbsolutePath().normalize())) {
            signalPath = fixture.resolve("isolated-runtime/game/echo-native/gameplay-hooks.json").normalize();
        }
        boolean alreadyComplete = bool(status, "signalsAcceptedAsEvidence");
        int existingSignalCount = intValue(status, "gameplayHookSignalCount");
        boolean canWrite = m24Complete
                && m25Ready
                && !modules.isEmpty()
                && diagnostics.stream().noneMatch(EchoNativeGameplayHookInstrumentor::isBlocking);
        boolean canReuseExistingSignals = m24Complete
                && alreadyComplete
                && !modules.isEmpty()
                && existingSignalCount == modules.size()
                && diagnostics.stream().noneMatch(EchoNativeGameplayHookInstrumentor::isBlocking);

        SignalWrite write = SignalWrite.skipped(signalPath);
        if (canWrite) {
            Files.createDirectories(signalPath.getParent());
            Files.writeString(signalPath, EchoNativeJson.write(signalFile(packId, modules)));
            write = SignalWrite.written(signalPath, modules.size());
        } else if (canReuseExistingSignals) {
            write = SignalWrite.existing(signalPath, existingSignalCount);
        } else if (diagnostics.stream().noneMatch(EchoNativeGameplayHookInstrumentor::isBlocking)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-INSTRUMENTATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook instrumentation is blocked",
                    "M25 requires accepted M24 completion, M25 readiness, and a gameplay hook signal contract before fixture-local signals may be written.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-m25-readiness.json"),
                    "Regenerate M24 gameplay hook bridge reports before instrumentation."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean complete = write.signalCount() == modules.size()
                && !modules.isEmpty()
                && sortedDiagnostics.stream().noneMatch(EchoNativeGameplayHookInstrumentor::isBlocking);
        return new EchoNativeGameplayHookInstrumentationOutcome(
                packId,
                instrumentationPlan(packId, signalPath, modules, canWrite, alreadyComplete, sortedDiagnostics),
                writeResult(packId, write, sortedDiagnostics),
                signalAudit(packId, signalPath, modules, write.signalCount(), sortedDiagnostics),
                m25Completion(packId, complete, modules.size(), write.signalCount(), sortedDiagnostics),
                m26Readiness(packId, complete, modules.size(), write.signalCount(), sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> signalFile(String packId, List<String> modules) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("generatedAt", Instant.EPOCH.toString());
        root.put("packId", packId);
        root.put("schema", "echo.native.gameplay_hooks.v1");
        root.put("signals", modules.stream().sorted().map(moduleId -> {
            Map<String, Object> signal = new LinkedHashMap<>();
            signal.put("generatedAt", Instant.EPOCH.toString());
            signal.put("moduleId", moduleId);
            signal.put("signal", "module_gameplay_hook_seen");
            signal.put("source", "controlled_native_module_hook_bridge");
            signal.put("verifiedFrom", List.of(
                    "reports/echo-native/" + packId + "/phase13-m24-completion.json",
                    "reports/echo-native/" + packId + "/gameplay-hook-signal-contract.json"
            ));
            return signal;
        }).toList());
        return root;
    }

    private static Map<String, Object> instrumentationPlan(
            String packId,
            Path signalPath,
            List<String> modules,
            boolean canWrite,
            boolean alreadyComplete,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m25_gameplay_hook_instrumentation_plan", diagnostics);
        data.put("alreadyComplete", alreadyComplete);
        data.put("authorizedCommandRequired", true);
        data.put("canWriteSignals", canWrite);
        data.put("moduleCount", modules.size());
        data.put("packId", packId);
        data.put("signalPath", relativePath(signalPath));
        data.put("signalWriter", "controlled_native_module_hook_bridge");
        data.put("summary", canWrite
                ? "M25 is authorized to write deterministic fixture-local gameplay hook signals."
                : alreadyComplete
                ? "M25 gameplay hook signals already satisfy the fixture-local contract."
                : "M25 instrumentation is blocked until prerequisite reports are accepted.");
        return data;
    }

    private static Map<String, Object> writeResult(
            String packId,
            SignalWrite write,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m25_gameplay_hook_signal_write_result", diagnostics);
        data.put("packId", packId);
        data.put("signalByteSize", write.byteSize());
        data.put("signalCount", write.signalCount());
        data.put("signalPath", relativePath(write.path()));
        data.put("signalSha256", write.sha256());
        data.put("signalsWrittenByThisCommand", write.written());
        data.put("summary", write.written()
                ? "Fixture-local gameplay hook signal file was written."
                : write.signalCount() > 0
                ? "Fixture-local gameplay hook signal file already exists and was accepted."
                : "Gameplay hook signal file was not written.");
        return data;
    }

    private static Map<String, Object> signalAudit(
            String packId,
            Path signalPath,
            List<String> modules,
            int signalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Map<String, Object> data = base("phase13_m25_gameplay_hook_signal_audit", diagnostics);
        data.put("moduleCount", modules.size());
        data.put("packId", packId);
        data.put("signalByteSize", Files.isRegularFile(signalPath) ? Files.size(signalPath) : 0);
        data.put("signalCount", signalCount);
        data.put("signalFilePresent", Files.isRegularFile(signalPath));
        data.put("signalPath", relativePath(signalPath));
        data.put("signalSha256", sha256Of(signalPath));
        data.put("signalsCoverRequiredModules", signalCount == modules.size());
        data.put("summary", signalCount == modules.size()
                ? "Gameplay hook signals cover all required modules."
                : "Gameplay hook signals do not yet cover all required modules.");
        return data;
    }

    private static Map<String, Object> m25Completion(
            String packId,
            boolean complete,
            int moduleCount,
            int signalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m25_gameplay_hook_signal_instrumentation_completion", diagnostics);
        data.put("ashfallPlayableBetaReady", false);
        data.put("gameplayHookSignalCount", signalCount);
        data.put("moduleCount", moduleCount);
        data.put("packId", packId);
        data.put("phase13M25Complete", complete);
        data.put("phase13M26Ready", complete);
        data.put("summary", complete
                ? "M25 is complete: fixture-local gameplay hook signals cover required modules."
                : "M25 remains blocked until fixture-local gameplay hook signals can be emitted.");
        return data;
    }

    private static Map<String, Object> m26Readiness(
            String packId,
            boolean complete,
            int moduleCount,
            int signalCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m26_readiness", diagnostics);
        data.put("nextCommand", "phase13 verify playable-beta <fixture>");
        data.put("nextMilestone", "phase13.m26.playable_beta_gate_closeout");
        data.put("packId", packId);
        data.put("phase13M26Ready", complete);
        data.put("remainingGameplayHookCount", Math.max(0, moduleCount - signalCount));
        data.put("summary", complete
                ? "M26 may start: rerun the bridge and close out the Ashfall playable beta gate."
                : "M26 remains blocked until M25 instrumentation completes.");
        return data;
    }

    private static List<String> contractModules(Map<String, Object> contract) {
        Object raw = contract.get("modules");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> modules = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (!id.isBlank()) {
                modules.add(id);
            }
        }
        modules.sort(String::compareTo);
        return List.copyOf(modules);
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-INSTRUMENTATION-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook instrumentation required report missing",
                    "M25 requires " + reportName + " before signal instrumentation can run.",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Regenerate M24 gameplay hook bridge reports before instrumentation."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkAcceptedReport(
            String reportName,
            Path reportPath,
            Map<String, Object> report,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-INSTRUMENTATION-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook instrumentation upstream report is not accepted",
                    "M25 requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream M24 diagnostics before writing gameplay hook signals."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }

    private static int intValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String sha256Of(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path))).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record SignalWrite(Path path, boolean written, int signalCount, long byteSize, String sha256) {
        private static SignalWrite skipped(Path path) {
            return new SignalWrite(path, false, 0, 0, "");
        }

        private static SignalWrite written(Path path, int signalCount) throws IOException {
            return new SignalWrite(path, true, signalCount, Files.size(path), sha256Of(path));
        }

        private static SignalWrite existing(Path path, int signalCount) throws IOException {
            return new SignalWrite(path, false, signalCount, Files.isRegularFile(path) ? Files.size(path) : 0, sha256Of(path));
        }
    }
}
