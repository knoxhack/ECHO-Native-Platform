package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLibraryResolutionPlan;
import dev.echo.nativeplatform.contracts.EchoNativeLibraryResolverSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeLibrarySourcePolicy;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeLibraryResolverPlanner {
    EchoNativeLibraryResolverPlanningOutcome plan(
            String packId,
            Path fixture,
            Path minecraftResolverPlanPath,
            Path minecraftSourcePolicyPath,
            Path minecraftSafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> minecraftPlan = readRequiredReport(minecraftResolverPlanPath, fixture, packId, "ECHO-NATIVE-MINECRAFT-RESOLVER-PLAN-MISSING", "Minecraft resolver plan missing", diagnostics);
        Map<String, Object> sourcePolicy = readRequiredReport(minecraftSourcePolicyPath, fixture, packId, "ECHO-NATIVE-MINECRAFT-SOURCE-POLICY-MISSING", "Minecraft source policy missing", diagnostics);
        Map<String, Object> safetyStatus = readRequiredReport(minecraftSafetyStatusPath, fixture, packId, "ECHO-NATIVE-MINECRAFT-SAFETY-STATUS-MISSING", "Minecraft resolver safety status missing", diagnostics);
        checkUpstreamReport(minecraftPlan, EchoNativeJson.asObject(minecraftPlan.get("data")), minecraftResolverPlanPath, packId, "ECHO-NATIVE-MINECRAFT-RESOLVER-BLOCKED", "Minecraft resolver planning is not safe for library planning", diagnostics);
        checkUpstreamReport(sourcePolicy, EchoNativeJson.asObject(sourcePolicy.get("data")), minecraftSourcePolicyPath, packId, "ECHO-NATIVE-MINECRAFT-SOURCE-POLICY-BLOCKED", "Minecraft source policy is not safe for library planning", diagnostics);
        checkUpstreamReport(safetyStatus, EchoNativeJson.asObject(safetyStatus.get("data")), minecraftSafetyStatusPath, packId, "ECHO-NATIVE-MINECRAFT-SAFETY-BLOCKED", "Minecraft resolver safety status is not safe for library planning", diagnostics);

        Path manifestPath = fixture.resolve("libraries").resolve("echo.native.libraries.json");
        LibraryManifest manifest = readLibraryManifest(manifestPath, fixture, packId, diagnostics);
        List<Map<String, Object>> plannedLibraries = manifest.libraries();
        List<String> missingLibraries = manifest.missingLibraries();

        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        EchoNativeLibraryResolutionPlan resolutionPlan = new EchoNativeLibraryResolutionPlan(
                "phase13.m3.library_resolution.plan",
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
                ready ? plannedLibraries : List.of(),
                missingLibraries
        );
        EchoNativeLibrarySourcePolicy librarySourcePolicy = new EchoNativeLibrarySourcePolicy(
                "phase13.m3.library_source.policy",
                true,
                false,
                false,
                false,
                false,
                ready ? List.of(relativeReportPath(manifestPath), "reports/echo-native/" + packId + "/minecraft-version-resolver-plan.json") : List.of(),
                List.of("remote.library.repository", "network.download", "launcher.cache.write", "minecraft.runtime.classes")
        );
        EchoNativeLibraryResolverSafetyStatus librarySafetyStatus = new EchoNativeLibraryResolverSafetyStatus(
                "phase13.m3.library_resolver.safety.status",
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
                ready ? List.of("minecraft_resolver_plan", "library_manifest", "library_source_policy") : List.of()
        );

        return new EchoNativeLibraryResolverPlanningOutcome(
                packId,
                libraryResolutionPlan(packId, resolutionPlan, diagnostics),
                librarySourcePolicy(packId, librarySourcePolicy, diagnostics),
                libraryResolverSafetyStatus(packId, librarySafetyStatus, diagnostics),
                diagnostics
        );
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
                    "Library resolver planning requires PASS upstream Minecraft resolver reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the Minecraft resolver planning reports before library planning."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static LibraryManifest readLibraryManifest(
            Path manifestPath,
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIBRARY-MANIFEST-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native library manifest missing",
                    "Library resolver planning requires a fixture-local libraries/echo.native.libraries.json manifest.",
                    null,
                    packId,
                    List.of(fixture.resolve("libraries/echo.native.libraries.json").toString().replace('\\', '/')),
                    "Add a fixture-local library manifest or keep the library resolver blocked."
            ));
            return new LibraryManifest(List.of(), List.of("libraries/echo.native.libraries.json"));
        }
        Map<String, Object> manifest;
        try {
            manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIBRARY-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native library manifest is invalid JSON",
                    ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Fix the fixture-local library manifest JSON."
            ));
            return new LibraryManifest(List.of(), List.of(relativeReportPath(manifestPath)));
        }
        if (!"echo.native.library_manifest.v1".equals(manifest.get("schema"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIBRARY-MANIFEST-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported native library manifest schema",
                    "Library manifest schema was '" + manifest.get("schema") + "'.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use schema echo.native.library_manifest.v1."
            ));
        }
        List<Map<String, Object>> libraries = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Object rawLibraries = manifest.get("libraries");
        if (rawLibraries instanceof List<?> items) {
            for (Object item : items) {
                Map<String, Object> library = EchoNativeJson.asObject(item);
                String id = String.valueOf(library.getOrDefault("id", "")).trim();
                String coordinate = String.valueOf(library.getOrDefault("coordinate", "")).trim();
                String sha256 = String.valueOf(library.getOrDefault("sha256", "")).trim();
                if (id.isBlank() || coordinate.isBlank() || sha256.isBlank()) {
                    String missingId = id.isBlank() ? "unknown-library" : id;
                    missing.add(missingId);
                    diagnostics.add(new EchoNativeDiagnostic(
                            "ECHO-NATIVE-LIBRARY-DESCRIPTOR-INCOMPLETE",
                            EchoNativeIssueSeverity.ERROR,
                            "Native library descriptor is incomplete",
                            "Each fixture library requires id, coordinate, and sha256 metadata.",
                            missingId,
                            packId,
                            List.of(relativeReportPath(manifestPath)),
                            "Complete the fixture library descriptor or remove it from required planning."
                    ));
                    continue;
                }
                Map<String, Object> planned = new LinkedHashMap<>();
                planned.put("artifactAvailable", false);
                planned.put("coordinate", coordinate);
                planned.put("downloadAllowed", false);
                planned.put("id", id);
                planned.put("required", Boolean.TRUE.equals(library.get("required")));
                planned.put("scope", String.valueOf(library.getOrDefault("scope", "runtime-plan")));
                planned.put("sha256", sha256);
                planned.put("source", "fixture-local-manifest");
                planned.put("trustLevel", String.valueOf(library.getOrDefault("trustLevel", "unknown")));
                libraries.add(planned);
            }
        }
        libraries.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        missing.sort(String::compareTo);
        if (libraries.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIBRARY-MANIFEST-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native library manifest has no usable libraries",
                    "Library resolver planning needs at least one local library descriptor.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add fixture-local library descriptors with id, coordinate, scope, trustLevel, and sha256."
            ));
        }
        return new LibraryManifest(List.copyOf(libraries), List.copyOf(missing));
    }

    private static Map<String, Object> libraryResolutionPlan(
            String packId,
            EchoNativeLibraryResolutionPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m3_library_resolver_plan", diagnostics);
        data.put("cacheMutated", plan.cacheMutated());
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("commandExecuted", plan.commandExecuted());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("libraryDownloadStarted", plan.libraryDownloadStarted());
        data.put("libraryResolverStarted", plan.libraryResolverStarted());
        data.put("minecraftResolverStarted", false);
        data.put("missingLibraries", plan.missingLibraries());
        data.put("missingLibraryCount", plan.missingLibraries().size());
        data.put("nativeExtractionStarted", plan.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("plannedLibraries", plan.plannedLibraries());
        data.put("plannedLibraryCount", plan.plannedLibraries().size());
        data.put("planId", plan.planId());
        data.put("planningOnly", plan.planningOnly());
        data.put("processLaunched", plan.processLaunched());
        data.put("registryInjected", plan.registryInjected());
        data.put("registryMutated", plan.registryMutated());
        data.put("remoteManifestDownloaded", plan.remoteManifestDownloaded());
        data.put("summary", diagnostics.isEmpty()
                ? "Library resolver planning used fixture-local metadata only; no resolver, download, cache mutation, classloader, or process work started."
                : "Library resolver planning is blocked by upstream diagnostics or missing local library metadata.");
        return data;
    }

    private static Map<String, Object> librarySourcePolicy(
            String packId,
            EchoNativeLibrarySourcePolicy policy,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m3_library_source_policy", diagnostics);
        data.put("blockedSources", policy.blockedSources());
        data.put("cacheMutationAllowed", policy.cacheMutationAllowed());
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("downloadsAllowed", policy.downloadsAllowed());
        data.put("filesystemMutated", policy.filesystemMutated());
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("libraryResolverStarted", false);
        data.put("localManifestOnly", policy.localManifestOnly());
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("packId", packId);
        data.put("policyId", policy.policyId());
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("remoteManifestAllowed", policy.remoteManifestAllowed());
        data.put("remoteManifestDownloaded", false);
        data.put("summary", diagnostics.isEmpty()
                ? "Library source policy allows only fixture-local manifest data during M3."
                : "Library source policy is blocked by upstream diagnostics or missing local library metadata.");
        data.put("trustedSources", policy.trustedSources());
        return data;
    }

    private static Map<String, Object> libraryResolverSafetyStatus(
            String packId,
            EchoNativeLibraryResolverSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m3_library_resolver_safety_status", diagnostics);
        data.put("cacheMutated", status.cacheMutated());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("libraryResolverStarted", status.libraryResolverStarted());
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionStarted", status.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("remoteManifestDownloaded", status.remoteManifestDownloaded());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "Library resolver planning remains safe for data-only M3 work."
                : "Library resolver planning is blocked by diagnostics.");
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
        return Boolean.TRUE.equals(data.get("minecraftResolverStarted"))
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
                    "Required library resolver planning input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Minecraft resolver planning reports before library resolver planning."
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

    private record LibraryManifest(List<Map<String, Object>> libraries, List<String> missingLibraries) {
    }
}
