package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLaunchArgumentPlan;
import dev.echo.nativeplatform.contracts.EchoNativeLaunchArgumentSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeLaunchArgumentSourcePolicy;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeLaunchArgumentPlanner {
    EchoNativeLaunchArgumentPlanningOutcome plan(
            String packId,
            Path fixture,
            Path packProfilePath,
            Path minecraftResolverPlanPath,
            Path minecraftSourcePolicyPath,
            Path minecraftSafetyStatusPath,
            Path classpathPlanPath,
            Path classpathSourcePolicyPath,
            Path classpathSafetyStatusPath,
            Path nativeExtractionPlanPath,
            Path nativeExtractionSourcePolicyPath,
            Path nativeExtractionSafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> packProfile = readRequiredReport(packProfilePath, fixture, packId, "ECHO-NATIVE-PACK-PROFILE-MISSING", "Pack profile missing", diagnostics);
        Map<String, Object> minecraftPlan = readRequiredReport(minecraftResolverPlanPath, fixture, packId, "ECHO-NATIVE-MINECRAFT-PLAN-MISSING", "Minecraft resolver plan missing", diagnostics);
        Map<String, Object> minecraftPolicy = readRequiredReport(minecraftSourcePolicyPath, fixture, packId, "ECHO-NATIVE-MINECRAFT-SOURCE-POLICY-MISSING", "Minecraft source policy missing", diagnostics);
        Map<String, Object> minecraftSafety = readRequiredReport(minecraftSafetyStatusPath, fixture, packId, "ECHO-NATIVE-MINECRAFT-SAFETY-MISSING", "Minecraft resolver safety missing", diagnostics);
        Map<String, Object> classpathPlan = readRequiredReport(classpathPlanPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-PLAN-MISSING", "Classpath builder plan missing", diagnostics);
        Map<String, Object> classpathPolicy = readRequiredReport(classpathSourcePolicyPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-SOURCE-POLICY-MISSING", "Classpath source policy missing", diagnostics);
        Map<String, Object> classpathSafety = readRequiredReport(classpathSafetyStatusPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-SAFETY-MISSING", "Classpath builder safety missing", diagnostics);
        Map<String, Object> nativePlan = readRequiredReport(nativeExtractionPlanPath, fixture, packId, "ECHO-NATIVE-EXTRACTION-PLAN-MISSING", "Native extraction plan missing", diagnostics);
        Map<String, Object> nativePolicy = readRequiredReport(nativeExtractionSourcePolicyPath, fixture, packId, "ECHO-NATIVE-EXTRACTION-SOURCE-POLICY-MISSING", "Native extraction source policy missing", diagnostics);
        Map<String, Object> nativeSafety = readRequiredReport(nativeExtractionSafetyStatusPath, fixture, packId, "ECHO-NATIVE-EXTRACTION-SAFETY-MISSING", "Native extraction safety missing", diagnostics);

        checkUpstreamReport(minecraftPlan, EchoNativeJson.asObject(minecraftPlan.get("data")), minecraftResolverPlanPath, packId, "ECHO-NATIVE-MINECRAFT-PLAN-BLOCKED", "Minecraft resolver plan is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(minecraftPolicy, EchoNativeJson.asObject(minecraftPolicy.get("data")), minecraftSourcePolicyPath, packId, "ECHO-NATIVE-MINECRAFT-SOURCE-POLICY-BLOCKED", "Minecraft source policy is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(minecraftSafety, EchoNativeJson.asObject(minecraftSafety.get("data")), minecraftSafetyStatusPath, packId, "ECHO-NATIVE-MINECRAFT-SAFETY-BLOCKED", "Minecraft resolver safety is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(classpathPlan, EchoNativeJson.asObject(classpathPlan.get("data")), classpathPlanPath, packId, "ECHO-NATIVE-CLASSPATH-PLAN-BLOCKED", "Classpath builder plan is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(classpathPolicy, EchoNativeJson.asObject(classpathPolicy.get("data")), classpathSourcePolicyPath, packId, "ECHO-NATIVE-CLASSPATH-SOURCE-POLICY-BLOCKED", "Classpath source policy is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(classpathSafety, EchoNativeJson.asObject(classpathSafety.get("data")), classpathSafetyStatusPath, packId, "ECHO-NATIVE-CLASSPATH-SAFETY-BLOCKED", "Classpath builder safety is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(nativePlan, EchoNativeJson.asObject(nativePlan.get("data")), nativeExtractionPlanPath, packId, "ECHO-NATIVE-EXTRACTION-PLAN-BLOCKED", "Native extraction plan is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(nativePolicy, EchoNativeJson.asObject(nativePolicy.get("data")), nativeExtractionSourcePolicyPath, packId, "ECHO-NATIVE-EXTRACTION-SOURCE-POLICY-BLOCKED", "Native extraction source policy is not safe for launch argument planning", diagnostics);
        checkUpstreamReport(nativeSafety, EchoNativeJson.asObject(nativeSafety.get("data")), nativeExtractionSafetyStatusPath, packId, "ECHO-NATIVE-EXTRACTION-SAFETY-BLOCKED", "Native extraction safety is not safe for launch argument planning", diagnostics);

        String targetMinecraftVersion = String.valueOf(EchoNativeJson.asObject(minecraftPlan.get("data")).getOrDefault("targetMinecraftVersion", ""));
        if (targetMinecraftVersion.isBlank()) {
            targetMinecraftVersion = String.valueOf(packProfile.getOrDefault("minecraftVersion", ""));
        }

        List<Map<String, Object>> plannedArguments = launchArguments(packId, targetMinecraftVersion, packProfile, classpathPlan, nativePlan);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        EchoNativeLaunchArgumentPlan launchPlan = new EchoNativeLaunchArgumentPlan(
                "phase13.m6.launch_argument.plan",
                true,
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
                ready ? plannedArguments : List.of()
        );
        EchoNativeLaunchArgumentSourcePolicy sourcePolicy = new EchoNativeLaunchArgumentSourcePolicy(
                "phase13.m6.launch_argument_source.policy",
                true,
                true,
                false,
                false,
                false,
                ready ? List.of(
                        "fixtures/" + fixture.getFileName() + "/echo.pack.json",
                        "reports/echo-native/" + packId + "/minecraft-version-resolver-plan.json",
                        "reports/echo-native/" + packId + "/classpath-builder-plan.json",
                        "reports/echo-native/" + packId + "/native-extraction-plan.json"
                ) : List.of(),
                List.of("process.launch", "command.execution", "runtime.classpath", "runtime.natives", "minecraft.main_class")
        );
        EchoNativeLaunchArgumentSafetyStatus safetyStatus = new EchoNativeLaunchArgumentSafetyStatus(
                "phase13.m6.launch_argument.safety.status",
                ready,
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
                ready ? List.of("minecraft_resolver_plan", "classpath_builder_plan", "native_extraction_plan", "planned_arguments_only") : List.of()
        );

        return new EchoNativeLaunchArgumentPlanningOutcome(
                packId,
                launchArgumentPlan(packId, launchPlan, targetMinecraftVersion, diagnostics),
                launchArgumentSourcePolicy(packId, sourcePolicy, diagnostics),
                launchArgumentSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static List<Map<String, Object>> launchArguments(
            String packId,
            String minecraftVersion,
            Map<String, Object> packProfile,
            Map<String, Object> classpathPlan,
            Map<String, Object> nativePlan
    ) {
        List<Map<String, Object>> args = new ArrayList<>();
        addArg(args, "minecraftVersion", "--version", minecraftVersion);
        addArg(args, "packId", "--echo-pack", packId);
        addArg(args, "gameDir", "--gameDir", "planned://runtime/" + packId + "/gameDir");
        addArg(args, "assetsDir", "--assetsDir", "planned://runtime/" + packId + "/assets");
        addArg(args, "classpath", "--classpath", "planned://reports/echo-native/" + packId + "/classpath-builder-plan.json");
        addArg(args, "nativesDirectory", "-Djava.library.path", "planned://reports/echo-native/" + packId + "/native-extraction-plan.json");
        addArg(args, "rootModule", "--echo-root-module", String.valueOf(packProfile.getOrDefault("rootModule", "")));
        addArg(args, "releaseChannel", "--echo-channel", String.valueOf(packProfile.getOrDefault("releaseChannel", "")));
        addArg(args, "runtimeMode", "--echo-runtime-mode", "native-loader-prototype-plan");
        addArg(args, "launchBlocked", "--echo-launch-blocked", "true");
        addArg(args, "classpathEntryCount", "--echo-classpath-entry-count", String.valueOf(EchoNativeJson.asObject(classpathPlan.get("data")).getOrDefault("entryCount", 0)));
        addArg(args, "nativeEntryCount", "--echo-native-entry-count", String.valueOf(EchoNativeJson.asObject(nativePlan.get("data")).getOrDefault("entryCount", 0)));
        args.sort(Comparator.comparing(item -> String.valueOf(item.get("orderKey"))));
        return args;
    }

    private static void addArg(List<Map<String, Object>> args, String id, String name, String value) {
        Map<String, Object> arg = new LinkedHashMap<>();
        arg.put("argumentKind", "planned_launch_argument");
        arg.put("commandLineMaterialized", false);
        arg.put("id", id);
        arg.put("name", name);
        arg.put("order", args.size());
        arg.put("orderKey", "0-arg-" + String.format("%04d", args.size()) + "-" + id);
        arg.put("plannedOnly", true);
        arg.put("value", value);
        args.add(arg);
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
                    "Launch argument planning requires PASS upstream reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate upstream Phase 13 planning reports before launch argument planning."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> launchArgumentPlan(
            String packId,
            EchoNativeLaunchArgumentPlan plan,
            String targetMinecraftVersion,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m6_launch_argument_plan", diagnostics);
        data.put("argumentCount", plan.plannedArguments().size());
        data.put("cacheMutated", false);
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("classpathBuilderStarted", false);
        data.put("commandExecuted", plan.commandExecuted());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("launchArgumentsPlannedOnly", plan.launchArgumentsPlannedOnly());
        data.put("libraryDownloadStarted", plan.libraryDownloadStarted());
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionAllowed", false);
        data.put("nativeExtractionStarted", plan.nativeExtractionStarted());
        data.put("nativeFilesExtracted", plan.nativeFilesExtracted());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("plannedArguments", plan.plannedArguments());
        data.put("planningOnly", plan.planningOnly());
        data.put("processLaunched", plan.processLaunched());
        data.put("productionClassloader", plan.productionClassloader());
        data.put("registryInjected", plan.registryInjected());
        data.put("registryMutated", plan.registryMutated());
        data.put("remoteManifestDownloaded", false);
        data.put("targetMinecraftVersion", targetMinecraftVersion);
        data.put("summary", diagnostics.isEmpty()
                ? "Launch argument planning created planned argument descriptors only; no command or process was materialized."
                : "Launch argument planning is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> launchArgumentSourcePolicy(
            String packId,
            EchoNativeLaunchArgumentSourcePolicy policy,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m6_launch_argument_source_policy", diagnostics);
        data.put("blockedSources", policy.blockedSources());
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("classpathBuilderStarted", false);
        data.put("commandExecuted", false);
        data.put("commandExecutionAllowed", policy.commandExecutionAllowed());
        data.put("filesystemMutated", false);
        data.put("filesystemMutationAllowed", policy.filesystemMutationAllowed());
        data.put("gameClassesResolved", false);
        data.put("launchArgumentsPlannedOnly", policy.launchArgumentsPlannedOnly());
        data.put("libraryDownloadStarted", false);
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionAllowed", false);
        data.put("nativeExtractionStarted", false);
        data.put("nativeFilesExtracted", false);
        data.put("packId", packId);
        data.put("policyId", policy.policyId());
        data.put("processLaunchAllowed", policy.processLaunchAllowed());
        data.put("processLaunched", false);
        data.put("productionClassloader", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("remoteManifestDownloaded", false);
        data.put("reportInputsOnly", policy.reportInputsOnly());
        data.put("summary", diagnostics.isEmpty()
                ? "Launch argument source policy allows only deterministic report and fixture inputs during M6."
                : "Launch argument source policy is blocked by upstream diagnostics.");
        data.put("trustedSources", policy.trustedSources());
        return data;
    }

    private static Map<String, Object> launchArgumentSafetyStatus(
            String packId,
            EchoNativeLaunchArgumentSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m6_launch_argument_safety_status", diagnostics);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("classpathBuilderStarted", false);
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("launchArgumentsPlannedOnly", status.launchArgumentsPlannedOnly());
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("libraryResolverStarted", false);
        data.put("minecraftResolverStarted", false);
        data.put("nativeExtractionAllowed", false);
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
                ? "Launch argument planning remains safe for data-only M6 work."
                : "Launch argument planning is blocked by diagnostics.");
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
                    "Required launch argument planning input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate upstream Phase 13 planning reports before launch argument planning."
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
