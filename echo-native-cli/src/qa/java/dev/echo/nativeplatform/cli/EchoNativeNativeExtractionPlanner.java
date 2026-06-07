package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeNativeExtractionPlan;
import dev.echo.nativeplatform.contracts.EchoNativeNativeExtractionSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeNativeExtractionSourcePolicy;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeNativeExtractionPlanner {
    EchoNativeNativeExtractionPlanningOutcome plan(
            String packId,
            Path fixture,
            Path nativeLibraryPlanPath,
            Path libraryPlanPath,
            Path librarySourcePolicyPath,
            Path librarySafetyStatusPath,
            Path classpathPlanPath,
            Path classpathSourcePolicyPath,
            Path classpathSafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> nativeLibraryPlan = readRequiredReport(nativeLibraryPlanPath, fixture, packId, "ECHO-NATIVE-NATIVE-LIBRARY-PLAN-MISSING", "Native library plan missing", diagnostics);
        Map<String, Object> libraryPlan = readRequiredReport(libraryPlanPath, fixture, packId, "ECHO-NATIVE-LIBRARY-PLAN-MISSING", "Library resolution plan missing", diagnostics);
        Map<String, Object> libraryPolicy = readRequiredReport(librarySourcePolicyPath, fixture, packId, "ECHO-NATIVE-LIBRARY-SOURCE-POLICY-MISSING", "Library source policy missing", diagnostics);
        Map<String, Object> librarySafety = readRequiredReport(librarySafetyStatusPath, fixture, packId, "ECHO-NATIVE-LIBRARY-SAFETY-MISSING", "Library resolver safety status missing", diagnostics);
        Map<String, Object> classpathPlan = readRequiredReport(classpathPlanPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-PLAN-MISSING", "Classpath builder plan missing", diagnostics);
        Map<String, Object> classpathPolicy = readRequiredReport(classpathSourcePolicyPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-SOURCE-POLICY-MISSING", "Classpath source policy missing", diagnostics);
        Map<String, Object> classpathSafety = readRequiredReport(classpathSafetyStatusPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-SAFETY-MISSING", "Classpath builder safety status missing", diagnostics);

        checkUpstreamReport(nativeLibraryPlan, EchoNativeJson.asObject(nativeLibraryPlan.get("data")), nativeLibraryPlanPath, packId, "ECHO-NATIVE-NATIVE-LIBRARY-PLAN-BLOCKED", "Native library plan is not safe for extraction planning", diagnostics);
        checkUpstreamReport(libraryPlan, EchoNativeJson.asObject(libraryPlan.get("data")), libraryPlanPath, packId, "ECHO-NATIVE-LIBRARY-PLAN-BLOCKED", "Library resolution plan is not safe for extraction planning", diagnostics);
        checkUpstreamReport(libraryPolicy, EchoNativeJson.asObject(libraryPolicy.get("data")), librarySourcePolicyPath, packId, "ECHO-NATIVE-LIBRARY-SOURCE-POLICY-BLOCKED", "Library source policy is not safe for extraction planning", diagnostics);
        checkUpstreamReport(librarySafety, EchoNativeJson.asObject(librarySafety.get("data")), librarySafetyStatusPath, packId, "ECHO-NATIVE-LIBRARY-SAFETY-BLOCKED", "Library resolver safety status is not safe for extraction planning", diagnostics);
        checkUpstreamReport(classpathPlan, EchoNativeJson.asObject(classpathPlan.get("data")), classpathPlanPath, packId, "ECHO-NATIVE-CLASSPATH-PLAN-BLOCKED", "Classpath builder plan is not safe for extraction planning", diagnostics);
        checkUpstreamReport(classpathPolicy, EchoNativeJson.asObject(classpathPolicy.get("data")), classpathSourcePolicyPath, packId, "ECHO-NATIVE-CLASSPATH-SOURCE-POLICY-BLOCKED", "Classpath source policy is not safe for extraction planning", diagnostics);
        checkUpstreamReport(classpathSafety, EchoNativeJson.asObject(classpathSafety.get("data")), classpathSafetyStatusPath, packId, "ECHO-NATIVE-CLASSPATH-SAFETY-BLOCKED", "Classpath builder safety status is not safe for extraction planning", diagnostics);

        List<Map<String, Object>> plannedNativeEntries = nativeEntries(nativeLibraryPlan);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        EchoNativeNativeExtractionPlan extractionPlan = new EchoNativeNativeExtractionPlan(
                "phase13.m5.native_extraction.plan",
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ready ? plannedNativeEntries : List.of()
        );
        EchoNativeNativeExtractionSourcePolicy sourcePolicy = new EchoNativeNativeExtractionSourcePolicy(
                "phase13.m5.native_extraction_source.policy",
                true,
                false,
                false,
                false,
                false,
                ready ? List.of(
                        "reports/echo-native/" + packId + "/native-library-plan.json",
                        "reports/echo-native/" + packId + "/library-resolution-plan.json",
                        "reports/echo-native/" + packId + "/classpath-builder-plan.json"
                ) : List.of(),
                List.of("native.library.extraction", "runtime.native.lookup", "filesystem.native.cache", "remote.native.repository")
        );
        EchoNativeNativeExtractionSafetyStatus safetyStatus = new EchoNativeNativeExtractionSafetyStatus(
                "phase13.m5.native_extraction.safety.status",
                ready,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ready ? List.of("native_library_plan", "library_resolution_plan", "classpath_builder_plan", "planned_native_entries_only") : List.of()
        );

        return new EchoNativeNativeExtractionPlanningOutcome(
                packId,
                nativeExtractionPlan(packId, extractionPlan, diagnostics),
                nativeExtractionSourcePolicy(packId, sourcePolicy, diagnostics),
                nativeExtractionSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static List<Map<String, Object>> nativeEntries(Map<String, Object> report) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object rawEntries = data.get("entries");
        if (!(rawEntries instanceof List<?> entries)) {
            return List.of();
        }
        List<Map<String, Object>> planned = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            String source = String.valueOf(entries.get(i));
            String id = source.replace("planned://", "").replace('/', '-').replace(':', '-');
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("entryKind", "native_library");
            entry.put("extracted", false);
            entry.put("extractionAllowed", false);
            entry.put("id", id);
            entry.put("order", i);
            entry.put("orderKey", "0-native-" + String.format("%04d", i) + "-" + id);
            entry.put("plannedExtractionPath", "planned://native-extraction/" + id);
            entry.put("plannedSource", source);
            entry.put("runtimeResolved", false);
            planned.add(entry);
        }
        planned.sort(Comparator.comparing(item -> String.valueOf(item.get("orderKey"))));
        return planned;
    }

    private static void checkUpstreamReport(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            String code,
            String title,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        if (!"PASS".equals(report.get("status")) || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    code,
                    EchoNativeIssueSeverity.ERROR,
                    title,
                    "Native extraction planning requires PASS upstream reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate upstream Phase 13 planning reports before native extraction planning."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> nativeExtractionPlan(
            String packId,
            EchoNativeNativeExtractionPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m5_native_extraction_plan", diagnostics);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("commandExecuted", plan.commandExecuted());
        data.put("entryCount", plan.plannedNativeEntries().size());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("libraryDownloadStarted", plan.libraryDownloadStarted());
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionAllowed", plan.nativeExtractionAllowed());
        data.put("nativeExtractionStarted", plan.nativeExtractionStarted());
        data.put("nativeFilesExtracted", plan.nativeFilesExtracted());
        data.put("packId", packId);
        data.put("plannedNativeEntries", plan.plannedNativeEntries());
        data.put("planId", plan.planId());
        data.put("planningOnly", plan.planningOnly());
        data.put("processLaunched", plan.processLaunched());
        data.put("productionClassloader", plan.productionClassloader());
        data.put("registryInjected", plan.registryInjected());
        data.put("registryMutated", plan.registryMutated());
        data.put("remoteManifestDownloaded", false);
        data.put("summary", diagnostics.isEmpty()
                ? "Native extraction planning created planned:// extraction entries only; no native files were extracted."
                : "Native extraction planning is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> nativeExtractionSourcePolicy(
            String packId,
            EchoNativeNativeExtractionSourcePolicy policy,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m5_native_extraction_source_policy", diagnostics);
        data.put("blockedSources", policy.blockedSources());
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("filesystemMutated", false);
        data.put("filesystemMutationAllowed", policy.filesystemMutationAllowed());
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionAllowed", policy.nativeExtractionAllowed());
        data.put("nativeExtractionStarted", false);
        data.put("nativeFilesExtracted", policy.nativeFilesExtracted());
        data.put("packId", packId);
        data.put("policyId", policy.policyId());
        data.put("processLaunched", false);
        data.put("productionClassloader", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("remoteManifestDownloaded", false);
        data.put("reportInputsOnly", policy.reportInputsOnly());
        data.put("runtimeNativeLookupAllowed", policy.runtimeNativeLookupAllowed());
        data.put("summary", diagnostics.isEmpty()
                ? "Native extraction source policy allows only deterministic report inputs during M5."
                : "Native extraction source policy is blocked by upstream diagnostics.");
        data.put("trustedSources", policy.trustedSources());
        return data;
    }

    private static Map<String, Object> nativeExtractionSafetyStatus(
            String packId,
            EchoNativeNativeExtractionSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m5_native_extraction_safety_status", diagnostics);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionAllowed", status.nativeExtractionAllowed());
        data.put("nativeExtractionStarted", status.nativeExtractionStarted());
        data.put("nativeFilesExtracted", status.nativeFilesExtracted());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("productionClassloader", status.productionClassloader());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("remoteManifestDownloaded", false);
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "Native extraction planning remains safe for data-only M5 work."
                : "Native extraction planning is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("gameProcessLaunched", false);
        data.put("phase", phase);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("classpathBuilderStarted"))
                || Boolean.TRUE.equals(data.get("minecraftResolverStarted"))
                || Boolean.TRUE.equals(data.get("libraryResolverStarted"))
                || Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("extractionAllowed"))
                || Boolean.TRUE.equals(data.get("nativeExtractionAllowed"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("mutatedFilesystem"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String missingCode,
            String missingTitle,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    missingCode,
                    EchoNativeIssueSeverity.ERROR,
                    missingTitle,
                    "Required native extraction planning input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate upstream Phase 13 planning reports before native extraction planning."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
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

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
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
