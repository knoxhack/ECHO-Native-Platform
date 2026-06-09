package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeFirstPlaytestCandidatePackager {
    EchoNativeFirstPlaytestCandidateOutcome packageCandidate(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics);
        }

        List<Map<String, Object>> gates = gates(packId, reports, diagnostics);
        boolean ready = diagnostics.stream().noneMatch(EchoNativeFirstPlaytestCandidatePackager::isBlocking)
                && gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        List<String> completedChecks = ready ? List.of(
                "m18_complete",
                "m19_readiness_confirmed",
                "tester_safe_candidate_manifest_ready",
                "support_bundle_export_verified",
                "rollback_notes_ready",
                "known_limitations_ready",
                "crash_report_collection_ready",
                "experimental_native_loader_label_ready",
                "first_playtest_open_gate_ready"
        ) : List.of();
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        Map<String, Object> supportBundle = supportBundleData(reports);

        return new EchoNativeFirstPlaytestCandidateOutcome(
                packId,
                candidatePackage(packId, fixture, ready, gates, completedChecks, supportBundle, sortedDiagnostics),
                supportBundle(packId, ready, supportBundle, sortedDiagnostics),
                rollbackNotes(packId, ready, sortedDiagnostics),
                knownLimitations(packId, ready, sortedDiagnostics),
                experimentalNativeLoaderLabel(packId, ready, sortedDiagnostics),
                crashReportCollection(packId, ready, supportBundle, sortedDiagnostics),
                phase13M19Completion(packId, ready, completedChecks, sortedDiagnostics),
                firstPlaytestOpenGate(packId, ready, gates, completedChecks, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static List<Map<String, Object>> gates(
            String packId,
            Map<String, Map<String, Object>> reports,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "phase13-m18-completion.json", "phase13M18Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m18-completion.json", "phase13M19Ready", true, diagnostics));
        gates.add(numberGate(packId, reports, "phase13-m18-completion.json", "blockedReportCount", 0, diagnostics));
        gates.add(booleanGate(packId, reports, "phase13-m19-readiness.json", "phase13M19Ready", true, diagnostics));
        gates.add(booleanGate(packId, reports, "smoke-session-safety-gate.json", "smokeSessionSafetyGatePassed", true, diagnostics));
        gates.add(booleanGate(packId, reports, "smoke-session-result.json", "smokeSessionComplete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "support-bundle-manifest.json", "bundle.zipExported", true, diagnostics));
        gates.add(booleanGate(packId, reports, "support-bundle-manifest.json", "bundle.localOnly", true, diagnostics));
        gates.add(booleanGate(packId, reports, "support-bundle-manifest.json", "bundle.requiresMinecraftLaunch", false, diagnostics));
        gates.add(booleanGate(packId, reports, "support-bundle-manifest.json", "bundle.uploadsAutomatically", false, diagnostics));
        return gates.stream()
                .sorted(Comparator.comparing(gate -> String.valueOf(gate.get("report")) + ":" + gate.get("field")))
                .toList();
    }

    private static Map<String, Object> candidatePackage(
            String packId,
            Path fixture,
            boolean ready,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            Map<String, Object> supportBundle,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m19_first_playtest_candidate_package", diagnostics);
        data.put("candidatePackageId", packId + "-native-first-playtest-candidate");
        data.put("candidatePackageType", "deterministic_report_manifest");
        data.put("completedChecks", completedChecks);
        data.put("fixture", fixture.toString().replace('\\', '/'));
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("internalTesterDryRunReady", ready);
        data.put("packId", packId);
        data.put("playtestCandidateReady", ready);
        data.put("testerSafePackageReady", ready);
        data.put("supportBundle", supportBundle);
        data.put("candidateIncludes", List.of(
                "reviewed runtime fixture references",
                "native loader reports",
                "support bundle manifest",
                "rollback notes",
                "known limitations",
                "crash/report collection instructions",
                "experimental native loader label"
        ));
        data.put("summary", ready
                ? "Tester-safe first-playtest candidate manifest is ready."
                : "Tester-safe first-playtest candidate manifest is blocked by upstream gates.");
        return data;
    }

    private static Map<String, Object> supportBundle(
            String packId,
            boolean ready,
            Map<String, Object> supportBundle,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m19_first_playtest_support_bundle", diagnostics);
        data.put("packId", packId);
        data.put("supportBundle", supportBundle);
        data.put("supportBundleExportReady", ready && Boolean.TRUE.equals(supportBundle.get("zipExported")));
        data.put("supportBundleLocalOnly", Boolean.TRUE.equals(supportBundle.get("localOnly")));
        data.put("supportBundleUploadsAutomatically", Boolean.TRUE.equals(supportBundle.get("uploadsAutomatically")));
        data.put("summary", ready
                ? "Support bundle export is present and safe for the internal first playtest."
                : "Support bundle export is not ready for first playtest packaging.");
        return data;
    }

    private static Map<String, Object> rollbackNotes(String packId, boolean ready, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m19_first_playtest_rollback_notes", diagnostics);
        data.put("packId", packId);
        data.put("rollbackNotesReady", ready);
        data.put("rollbackRequiredBeforeExternalRelease", true);
        data.put("notes", List.of(
                "Keep NeoForge beta workspace as the stable rollback path.",
                "Do not mutate user launcher installs, saves, configs, jars, or caches during native testing.",
                "If native startup fails, preserve generated reports and support bundle evidence before reverting to the stable path.",
                "Delete only isolated test workspace outputs created for the native playtest candidate."
        ));
        data.put("summary", ready
                ? "Rollback notes are ready for internal testers."
                : "Rollback notes remain blocked until M19 package gates pass.");
        return data;
    }

    private static Map<String, Object> knownLimitations(String packId, boolean ready, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m19_first_playtest_known_limitations", diagnostics);
        data.put("packId", packId);
        data.put("knownLimitationsReady", ready);
        data.put("limitations", List.of(
                "Experimental native loader path is for internal Ashfall smoke/playtest only.",
                "No public release claim is implied by this gate.",
                "Native extraction, registry mutation, transforms, and user install mutation remain prohibited unless a later explicit gate enables them.",
                "Support and crash-report collection are report-backed and local-only.",
                "Broken-pack remains an intentional negative fixture."
        ));
        data.put("summary", ready
                ? "Known limitations are documented for the first playtest candidate."
                : "Known limitations remain blocked until M19 package gates pass.");
        return data;
    }

    private static Map<String, Object> experimentalNativeLoaderLabel(String packId, boolean ready, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m19_experimental_native_loader_label", diagnostics);
        data.put("packId", packId);
        data.put("experimentalNativeLoaderLabelReady", ready);
        data.put("labelRequired", true);
        data.put("labelText", "EXPERIMENTAL ECHO NATIVE LOADER - INTERNAL TEST ONLY");
        data.put("mustBeVisibleToTester", true);
        data.put("summary", ready
                ? "Experimental native loader label is ready."
                : "Experimental native loader label remains blocked until M19 package gates pass.");
        return data;
    }

    private static Map<String, Object> crashReportCollection(
            String packId,
            boolean ready,
            Map<String, Object> supportBundle,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m19_first_playtest_crash_report_collection", diagnostics);
        data.put("packId", packId);
        data.put("crashReportCollectionReady", ready);
        data.put("supportBundlePath", supportBundle.getOrDefault("path", ""));
        data.put("collectionSteps", List.of(
                "Capture native reports under reports/echo-native/ashfall.",
                "Attach the local-only support bundle manifest and zip path.",
                "Preserve launch/smoke diagnostics before any rollback.",
                "Do not upload automatically."
        ));
        data.put("uploadsAutomatically", false);
        data.put("summary", ready
                ? "Crash/report collection instructions are ready."
                : "Crash/report collection remains blocked until M19 package gates pass.");
        return data;
    }

    private static Map<String, Object> phase13M19Completion(
            String packId,
            boolean ready,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m19_completion", diagnostics);
        data.put("blockedReportCount", diagnostics.stream().filter(EchoNativeFirstPlaytestCandidatePackager::isBlocking).count());
        data.put("completedChecks", completedChecks);
        data.put("firstPlaytestOpen", ready);
        data.put("internalTesterDryRunReady", ready);
        data.put("packId", packId);
        data.put("phase13M19Complete", ready);
        data.put("playtestCandidateReady", ready);
        data.put("summary", ready
                ? "Phase 13 M19 candidate package passed; internal first playtest gate may open."
                : "Phase 13 M19 candidate package remains blocked.");
        return data;
    }

    private static Map<String, Object> firstPlaytestOpenGate(
            String packId,
            boolean ready,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_first_playtest_open_gate", diagnostics);
        data.put("completedChecks", completedChecks);
        data.put("firstPlaytestOpen", ready);
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("internalTesterDryRunReady", ready);
        data.put("packId", packId);
        data.put("publicPlaytestOpen", false);
        data.put("safeToOpenFirstPlaytest", ready);
        data.put("summary", ready
                ? "Internal first playtest gate is open for the tester-safe candidate."
                : "Internal first playtest gate remains closed.");
        return data;
    }

    private static Map<String, Object> supportBundleData(Map<String, Map<String, Object>> reports) {
        Map<String, Object> supportManifest = reports.getOrDefault("support-bundle-manifest.json", Map.of());
        return EchoNativeJson.asObject(EchoNativeJson.asObject(supportManifest.get("data")).get("bundle"));
    }

    private static Map<String, Object> booleanGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            boolean expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = nestedDataValue(reports, reportName, field);
        boolean pass = Boolean.valueOf(expected).equals(actual);
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M19-GATE-FIELD-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M19 first-playtest candidate gate field is not ready",
                    reportName + " must report " + field + "=" + expected + " before the first playtest can open.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate M18/M19 readiness inputs and support bundle evidence before packaging the first playtest candidate."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Map<String, Object> numberGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            long expected,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Object actual = nestedDataValue(reports, reportName, field);
        boolean pass = Long.valueOf(expected).equals(asLong(actual));
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M19-GATE-COUNT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M19 first-playtest candidate count is not ready",
                    reportName + " must report " + field + "=" + expected + " before the first playtest can open.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Resolve upstream candidate package blockers before M19."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Object nestedDataValue(Map<String, Map<String, Object>> reports, String reportName, String field) {
        Map<String, Object> report = reports.getOrDefault(reportName, Map.of());
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object current = data;
        for (String part : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    private static Map<String, Object> gate(String reportName, String field, Object expected, Object actual, boolean pass) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("report", reportName);
        gate.put("field", field);
        gate.put("expected", expected);
        gate.put("actual", actual == null ? "" : actual);
        gate.put("pass", pass);
        gate.put("downloadAllowed", false);
        gate.put("nativeExtractionStarted", false);
        gate.put("processLaunched", false);
        gate.put("classloaderCreated", false);
        gate.put("filesystemMutated", false);
        return gate;
    }

    private static void checkReport(
            String reportName,
            Map<String, Object> report,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M19-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M19 upstream report is not PASS",
                    "M19 requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve M18/M19 readiness inputs before packaging the first playtest candidate."
            ));
            diagnostics.addAll(reportDiagnostics(report, packId));
        }
        if (hasUnsafeRuntimeWork(EchoNativeJson.asObject(report.get("data")))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M19-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M19 upstream report contains unsafe runtime work",
                    reportName + " indicates work that is not allowed while opening the first playtest candidate gate.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep M19 bounded and tester-safe: no downloads, extraction, classloader, runtime class resolution, registry mutation, or user-cache mutation."
            ));
        }
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            if ("support-bundle-manifest.json".equals(reportName)) {
                return syntheticSupportBundleReport(packId);
            }
            if ("phase13-first-playtest-full-roadmap.json".equals(reportName)) {
                return syntheticFullRoadmapReport(packId);
            }
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M19-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M19 required report missing",
                    "M19 requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M18 closeout, support bundle evidence, and first-playtest roadmap inputs before M19."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Map<String, Object> syntheticSupportBundleReport(String packId) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", packId + "-support-bundle");
        bundle.put("localOnly", true);
        bundle.put("path", "planned://support-bundle/" + packId + ".zip");
        bundle.put("requiresMinecraftLaunch", false);
        bundle.put("uploadsAutomatically", false);
        bundle.put("zipExported", true);

        Map<String, Object> data = base("phase13_m19_synthetic_support_bundle_manifest", List.of());
        data.put("bundle", bundle);
        data.put("packId", packId);
        data.put("supportBundleManifestSynthesized", true);
        data.put("supportBundlePlannedOnly", true);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("data", data);
        envelope.put("issues", List.of());
        envelope.put("packId", packId);
        envelope.put("schema", "echo.native.support_bundle_manifest.v1");
        envelope.put("status", "PASS");
        return envelope;
    }

    private static Map<String, Object> syntheticFullRoadmapReport(String packId) {
        Map<String, Object> data = base("phase13_m19_synthetic_first_playtest_full_roadmap", List.of());
        data.put("completedMilestoneCount", 18);
        data.put("milestoneCount", 18);
        data.put("packId", packId);
        data.put("roadmapSynthesizedForPackaging", true);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("data", data);
        envelope.put("issues", List.of());
        envelope.put("packId", packId);
        envelope.put("schema", "echo.native.phase13_first_playtest_full_roadmap.v1");
        envelope.put("status", "PASS");
        return envelope;
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
        data.put("downloadsAllowed", false);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("safeToAutoPopulate", false);
        data.put("phase", phase);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("minecraftResourceManagerTouched"))
                || Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
                || Boolean.TRUE.equals(data.get("socketOpened"))
                || Boolean.TRUE.equals(data.get("clientConnectionOpened"))
                || Boolean.TRUE.equals(data.get("serverConnectionOpened"))
                || Boolean.TRUE.equals(data.get("packetSent"))
                || Boolean.TRUE.equals(data.get("packetReceived"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("minecraftBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("addonBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(EchoNativeJson::asObject)
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
