package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeClasspathBuilderPlan;
import dev.echo.nativeplatform.contracts.EchoNativeClasspathBuilderSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeClasspathSourcePolicy;
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

final class EchoNativeClasspathPlanner {
    EchoNativeClasspathPlanningOutcome plan(
            String packId,
            Path fixture,
            Path moduleLoadPlanPath,
            Path libraryPlanPath,
            Path librarySourcePolicyPath,
            Path librarySafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> moduleLoadPlan = readRequiredReport(moduleLoadPlanPath, fixture, packId, "ECHO-NATIVE-MODULE-LOAD-PLAN-MISSING", "Module load plan missing", diagnostics);
        Map<String, Object> libraryPlan = readRequiredReport(libraryPlanPath, fixture, packId, "ECHO-NATIVE-LIBRARY-PLAN-MISSING", "Library resolution plan missing", diagnostics);
        Map<String, Object> libraryPolicy = readRequiredReport(librarySourcePolicyPath, fixture, packId, "ECHO-NATIVE-LIBRARY-SOURCE-POLICY-MISSING", "Library source policy missing", diagnostics);
        Map<String, Object> librarySafety = readRequiredReport(librarySafetyStatusPath, fixture, packId, "ECHO-NATIVE-LIBRARY-SAFETY-MISSING", "Library resolver safety status missing", diagnostics);

        checkUpstreamReport(moduleLoadPlan, EchoNativeJson.asObject(moduleLoadPlan.get("data")), moduleLoadPlanPath, packId, "ECHO-NATIVE-MODULE-LOAD-PLAN-BLOCKED", "Module load plan is not safe for classpath planning", diagnostics);
        checkUpstreamReport(libraryPlan, EchoNativeJson.asObject(libraryPlan.get("data")), libraryPlanPath, packId, "ECHO-NATIVE-LIBRARY-PLAN-BLOCKED", "Library resolution plan is not safe for classpath planning", diagnostics);
        checkUpstreamReport(libraryPolicy, EchoNativeJson.asObject(libraryPolicy.get("data")), librarySourcePolicyPath, packId, "ECHO-NATIVE-LIBRARY-SOURCE-POLICY-BLOCKED", "Library source policy is not safe for classpath planning", diagnostics);
        checkUpstreamReport(librarySafety, EchoNativeJson.asObject(librarySafety.get("data")), librarySafetyStatusPath, packId, "ECHO-NATIVE-LIBRARY-SAFETY-BLOCKED", "Library resolver safety status is not safe for classpath planning", diagnostics);

        List<Map<String, Object>> entries = new ArrayList<>();
        entries.addAll(moduleEntries(moduleLoadPlan));
        entries.addAll(libraryEntries(libraryPlan));
        entries.sort(Comparator.comparing(item -> String.valueOf(item.get("orderKey"))));

        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        EchoNativeClasspathBuilderPlan classpathPlan = new EchoNativeClasspathBuilderPlan(
                "phase13.m4.classpath_builder.plan",
                true,
                false,
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
                ready ? entries : List.of()
        );
        EchoNativeClasspathSourcePolicy sourcePolicy = new EchoNativeClasspathSourcePolicy(
                "phase13.m4.classpath_source.policy",
                true,
                true,
                false,
                false,
                false,
                ready ? List.of(
                        "reports/echo-native/" + packId + "/module-load-plan.json",
                        "reports/echo-native/" + packId + "/library-resolution-plan.json"
                ) : List.of(),
                List.of("filesystem.classpath.scan", "classloader.define_class", "minecraft.runtime.classes", "remote.library.repository")
        );
        EchoNativeClasspathBuilderSafetyStatus safetyStatus = new EchoNativeClasspathBuilderSafetyStatus(
                "phase13.m4.classpath_builder.safety.status",
                ready,
                false,
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
                ready ? List.of("module_load_plan", "library_resolution_plan", "planned_entries_only") : List.of()
        );

        return new EchoNativeClasspathPlanningOutcome(
                packId,
                classpathBuilderPlan(packId, classpathPlan, diagnostics),
                classpathSourcePolicy(packId, sourcePolicy, diagnostics),
                classpathBuilderSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static List<Map<String, Object>> moduleEntries(Map<String, Object> report) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        List<Map<String, Object>> entries = new ArrayList<>();
        List<String> modules = EchoNativeJson.stringList(data.get("moduleLoadOrder"));
        for (int i = 0; i < modules.size(); i++) {
            String moduleId = modules.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("entryKind", "module");
            entry.put("id", moduleId);
            entry.put("order", i);
            entry.put("orderKey", "0-module-" + String.format("%04d", i) + "-" + moduleId);
            entry.put("plannedPath", "planned://module/" + moduleId);
            entry.put("runtimeResolved", false);
            entries.add(entry);
        }
        return entries;
    }

    private static List<Map<String, Object>> libraryEntries(Map<String, Object> report) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object rawLibraries = data.get("plannedLibraries");
        if (!(rawLibraries instanceof List<?> libraries)) {
            return List.of();
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Object item : libraries) {
            Map<String, Object> library = EchoNativeJson.asObject(item);
            String id = String.valueOf(library.getOrDefault("id", ""));
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("coordinate", String.valueOf(library.getOrDefault("coordinate", "")));
            entry.put("entryKind", "library");
            entry.put("id", id);
            entry.put("orderKey", "1-library-" + id);
            entry.put("plannedPath", "planned://library/" + id);
            entry.put("runtimeResolved", false);
            entry.put("scope", String.valueOf(library.getOrDefault("scope", "runtime-plan")));
            entry.put("sha256", String.valueOf(library.getOrDefault("sha256", "")));
            entries.add(entry);
        }
        return entries;
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
                    "Classpath builder planning requires PASS upstream reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate upstream Phase 13 planning reports before classpath planning."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> classpathBuilderPlan(
            String packId,
            EchoNativeClasspathBuilderPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m4_classpath_builder_plan", diagnostics);
        data.put("classpathBuilderStarted", plan.classpathBuilderStarted());
        data.put("classpathEntriesPlannedOnly", plan.classpathEntriesPlannedOnly());
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("commandExecuted", plan.commandExecuted());
        data.put("entryCount", plan.plannedEntries().size());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("cacheMutated", false);
        data.put("libraryDownloadStarted", plan.libraryDownloadStarted());
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionStarted", plan.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("plannedEntries", plan.plannedEntries());
        data.put("planId", plan.planId());
        data.put("planningOnly", plan.planningOnly());
        data.put("processLaunched", plan.processLaunched());
        data.put("productionClassloader", plan.productionClassloader());
        data.put("registryInjected", plan.registryInjected());
        data.put("registryMutated", plan.registryMutated());
        data.put("remoteManifestDownloaded", false);
        data.put("summary", diagnostics.isEmpty()
                ? "Classpath builder planning created planned:// entries only; no classloader or runtime class resolution started."
                : "Classpath builder planning is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> classpathSourcePolicy(
            String packId,
            EchoNativeClasspathSourcePolicy policy,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m4_classpath_source_policy", diagnostics);
        data.put("blockedSources", policy.blockedSources());
        data.put("classpathBuilderStarted", false);
        data.put("classpathEntriesPlannedOnly", policy.plannedEntriesOnly());
        data.put("classloaderCreated", false);
        data.put("classloaderCreationAllowed", policy.classloaderCreationAllowed());
        data.put("commandExecuted", false);
        data.put("filesystemMutated", policy.filesystemMutated());
        data.put("gameClassesResolved", false);
        data.put("cacheMutated", false);
        data.put("libraryDownloadStarted", false);
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("packId", packId);
        data.put("plannedEntriesOnly", policy.plannedEntriesOnly());
        data.put("policyId", policy.policyId());
        data.put("processLaunched", false);
        data.put("productionClassloader", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportInputsOnly", policy.reportInputsOnly());
        data.put("runtimeClassResolutionAllowed", policy.runtimeClassResolutionAllowed());
        data.put("remoteManifestDownloaded", false);
        data.put("summary", diagnostics.isEmpty()
                ? "Classpath source policy allows only deterministic report inputs during M4."
                : "Classpath source policy is blocked by upstream diagnostics.");
        data.put("trustedSources", policy.trustedSources());
        return data;
    }

    private static Map<String, Object> classpathBuilderSafetyStatus(
            String packId,
            EchoNativeClasspathBuilderSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m4_classpath_builder_safety_status", diagnostics);
        data.put("classpathBuilderStarted", status.classpathBuilderStarted());
        data.put("classpathEntriesPlannedOnly", status.classpathEntriesPlannedOnly());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("cacheMutated", false);
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionStarted", status.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("productionClassloader", status.productionClassloader());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("remoteManifestDownloaded", false);
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "Classpath builder planning remains safe for data-only M4 work."
                : "Classpath builder planning is blocked by diagnostics.");
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
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
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
                    "Required classpath planning input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate upstream Phase 13 planning reports before classpath planning."
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
