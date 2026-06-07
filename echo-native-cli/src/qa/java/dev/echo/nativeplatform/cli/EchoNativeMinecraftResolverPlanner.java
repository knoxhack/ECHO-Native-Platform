package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeMinecraftResolverSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeMinecraftVersionResolverPlan;
import dev.echo.nativeplatform.contracts.EchoNativeMinecraftVersionSourcePolicy;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeMinecraftResolverPlanner {
    EchoNativeMinecraftResolverPlanningOutcome plan(
            String packId,
            Path fixture,
            EchoNativePackProfile profile,
            Path phase13M2ReadinessPath,
            Path prototypeSafetyGatePath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> m2Readiness = readRequiredReport(phase13M2ReadinessPath, fixture, packId, "ECHO-NATIVE-PHASE13-M2-READINESS-MISSING", "Phase 13 M2 readiness report missing", diagnostics);
        Map<String, Object> prototypeSafetyGate = readRequiredReport(prototypeSafetyGatePath, fixture, packId, "ECHO-NATIVE-PROTOTYPE-SAFETY-GATE-MISSING", "Phase 13 prototype safety gate report missing", diagnostics);
        Map<String, Object> m2Data = EchoNativeJson.asObject(m2Readiness.get("data"));
        Map<String, Object> safetyData = EchoNativeJson.asObject(prototypeSafetyGate.get("data"));

        checkM2Readiness(m2Readiness, m2Data, phase13M2ReadinessPath, packId, diagnostics);
        checkPrototypeSafetyGate(prototypeSafetyGate, safetyData, prototypeSafetyGatePath, packId, diagnostics);
        checkProfile(profile, fixture, packId, diagnostics);

        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        String targetVersion = profile == null ? "" : profile.minecraftVersion();
        String profilePath = profile == null ? "" : relativeReportPath(profile.profilePath());
        EchoNativeMinecraftVersionResolverPlan resolverPlan = new EchoNativeMinecraftVersionResolverPlan(
                "phase13.m2.minecraft_version_resolver.plan",
                targetVersion,
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
                ready ? List.of(profilePath, "reports/echo-native/" + packId + "/phase13-m2-readiness.json") : List.of(),
                List.of("remote.version_manifest", "network.download", "local.cache.write", "minecraft.runtime.classes")
        );
        EchoNativeMinecraftVersionSourcePolicy sourcePolicy = new EchoNativeMinecraftVersionSourcePolicy(
                "phase13.m2.minecraft_version_source.policy",
                true,
                false,
                false,
                false,
                false,
                ready ? List.of("fixture.echo.pack.json", "phase13.m2.readiness.report") : List.of(),
                List.of("mojang.version_manifest.remote", "launcher.remote_metadata", "library.download_cache")
        );
        EchoNativeMinecraftResolverSafetyStatus safetyStatus = new EchoNativeMinecraftResolverSafetyStatus(
                "phase13.m2.minecraft_resolver.safety.status",
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
                ready ? List.of("phase13_m2_readiness_gate", "prototype_safety_gate", "fixture_pack_profile_version") : List.of()
        );

        return new EchoNativeMinecraftResolverPlanningOutcome(
                packId,
                minecraftVersionResolverPlan(packId, resolverPlan, diagnostics),
                minecraftVersionSourcePolicy(packId, sourcePolicy, diagnostics),
                minecraftResolverSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static void checkM2Readiness(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean ready = Boolean.TRUE.equals(data.get("phase13M2Ready"));
        if (!pass || !ready || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M2-READINESS-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M2 readiness is not safe for Minecraft resolver planning",
                    "Minecraft resolver planning requires PASS phase13-m2-readiness.json with phase13M2Ready=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 verify m1 before Minecraft resolver planning."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkPrototypeSafetyGate(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean passed = Boolean.TRUE.equals(data.get("passed"));
        if (!pass || !passed || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PROTOTYPE-SAFETY-GATE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 prototype safety gate is not safe for Minecraft resolver planning",
                    "Minecraft resolver planning requires PASS phase13-prototype-safety-gate.json with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the M1 closeout reports and keep resolver work planning-only."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkProfile(
            EchoNativePackProfile profile,
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (profile == null || profile.minecraftVersion().isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-MINECRAFT-VERSION-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Minecraft version target missing",
                    "Minecraft resolver planning requires a local minecraftVersion in the fixture pack profile.",
                    null,
                    packId,
                    List.of(fixture.resolve("echo.pack.json").toString().replace('\\', '/')),
                    "Add a local minecraftVersion to echo.pack.json before resolver planning."
            ));
        }
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("minecraftResolverStarted"))
                || Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
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

    private static Map<String, Object> minecraftVersionResolverPlan(
            String packId,
            EchoNativeMinecraftVersionResolverPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m2_minecraft_version_resolver_plan", diagnostics);
        data.put("blockedSources", plan.blockedSources());
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("commandExecuted", plan.commandExecuted());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("libraryDownloadStarted", plan.libraryDownloadStarted());
        data.put("minecraftResolverStarted", plan.minecraftResolverStarted());
        data.put("nativeExtractionStarted", plan.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("planningOnly", plan.planningOnly());
        data.put("processLaunched", plan.processLaunched());
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("remoteManifestDownloaded", plan.remoteManifestDownloaded());
        data.put("resolverMode", "local_data_only");
        data.put("summary", diagnostics.isEmpty()
                ? "Minecraft version resolver planning used local fixture metadata only; no resolver, network, download, classloader, or process work started."
                : "Minecraft version resolver planning is blocked by upstream diagnostics.");
        data.put("targetMinecraftVersion", plan.targetMinecraftVersion());
        data.put("trustedLocalSources", plan.trustedLocalSources());
        return data;
    }

    private static Map<String, Object> minecraftVersionSourcePolicy(
            String packId,
            EchoNativeMinecraftVersionSourcePolicy policy,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m2_minecraft_version_source_policy", diagnostics);
        data.put("allowedSources", policy.allowedSources());
        data.put("blockedSources", policy.blockedSources());
        data.put("cacheMutationAllowed", policy.cacheMutationAllowed());
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("filesystemMutated", policy.filesystemMutated());
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("localSourcesOnly", policy.localSourcesOnly());
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("networkAllowed", policy.networkAllowed());
        data.put("packId", packId);
        data.put("policyId", policy.policyId());
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("remoteManifestDownloaded", policy.remoteManifestDownloaded());
        data.put("summary", diagnostics.isEmpty()
                ? "Minecraft version source policy allows only local fixture/report data during M2.1."
                : "Minecraft version source policy is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> minecraftResolverSafetyStatus(
            String packId,
            EchoNativeMinecraftResolverSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m2_minecraft_resolver_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("minecraftResolverStarted", status.minecraftResolverStarted());
        data.put("nativeExtractionStarted", status.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("remoteManifestDownloaded", status.remoteManifestDownloaded());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "Minecraft resolver planning remains safe for data-only M2 work."
                : "Minecraft resolver planning is blocked by diagnostics.");
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
                    "Required Minecraft resolver planning input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 M1 closeout reports before Minecraft resolver planning."
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
