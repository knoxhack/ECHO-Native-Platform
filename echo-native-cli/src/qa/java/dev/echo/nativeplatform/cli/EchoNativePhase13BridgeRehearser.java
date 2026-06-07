package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativePhase13BridgeSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryBridgePolicyRehearsal;
import dev.echo.nativeplatform.contracts.EchoNativeResourceBridgePolicyRehearsal;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13BridgeRehearser {
    EchoNativePhase13BridgeRehearsalOutcome rehearse(
            String packId,
            Path fixture,
            Path phase13PlanPath,
            Path loaderBoundaryVerificationPath,
            Path testProcessBoundaryVerificationPath,
            Path phase13M1SafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> phase13Plan = readRequiredReport(phase13PlanPath, fixture, packId, "ECHO-NATIVE-PHASE13-PLAN-MISSING", "Phase 13 plan report missing", diagnostics);
        Map<String, Object> loaderBoundaryVerification = readRequiredReport(loaderBoundaryVerificationPath, fixture, packId, "ECHO-NATIVE-LOADER-BOUNDARY-VERIFICATION-MISSING", "Loader boundary verification report missing", diagnostics);
        Map<String, Object> testProcessBoundaryVerification = readRequiredReport(testProcessBoundaryVerificationPath, fixture, packId, "ECHO-NATIVE-TEST-PROCESS-BOUNDARY-MISSING", "Test-process boundary verification report missing", diagnostics);
        Map<String, Object> phase13M1SafetyStatus = readRequiredReport(phase13M1SafetyStatusPath, fixture, packId, "ECHO-NATIVE-PHASE13-M1-SAFETY-MISSING", "Phase 13 M1 safety status report missing", diagnostics);

        Map<String, Object> phase13Data = EchoNativeJson.asObject(phase13Plan.get("data"));
        Map<String, Object> loaderBoundaryData = EchoNativeJson.asObject(loaderBoundaryVerification.get("data"));
        Map<String, Object> testProcessData = EchoNativeJson.asObject(testProcessBoundaryVerification.get("data"));
        Map<String, Object> m1SafetyData = EchoNativeJson.asObject(phase13M1SafetyStatus.get("data"));

        checkPhase13Plan(phase13Plan, phase13Data, phase13PlanPath, packId, diagnostics);
        checkLoaderBoundaryVerification(loaderBoundaryVerification, loaderBoundaryData, loaderBoundaryVerificationPath, packId, diagnostics);
        checkTestProcessBoundary(testProcessBoundaryVerification, testProcessData, testProcessBoundaryVerificationPath, packId, diagnostics);
        checkM1SafetyStatus(phase13M1SafetyStatus, m1SafetyData, phase13M1SafetyStatusPath, packId, diagnostics);

        diagnostics = unique(diagnostics);
        boolean rehearsed = diagnostics.isEmpty();
        List<String> resourceScopes = rehearsed
                ? List.of("pack.resource_index", "module.resource_mounts", "namespace.resource_lookup")
                : List.of();
        List<String> registryScopes = rehearsed
                ? List.of("block.registry.plan", "item.registry.plan", "service.registry.plan")
                : List.of();
        List<String> resourceBlocked = List.of(
                "minecraft.runtime.resources",
                "game.class.resolution",
                "production.classloader",
                "resource.filesystem.mutation"
        );
        List<String> registryBlocked = List.of(
                "registry.injection",
                "registry.mutation",
                "game.class.resolution",
                "bytecode.transforms"
        );
        EchoNativeResourceBridgePolicyRehearsal resourceRehearsal = new EchoNativeResourceBridgePolicyRehearsal(
                "phase13.resource_bridge.policy.rehearsal",
                rehearsed,
                false,
                false,
                false,
                false,
                false,
                false,
                resourceScopes,
                resourceBlocked
        );
        EchoNativeRegistryBridgePolicyRehearsal registryRehearsal = new EchoNativeRegistryBridgePolicyRehearsal(
                "phase13.registry_bridge.policy.rehearsal",
                rehearsed,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                registryScopes,
                registryBlocked
        );
        EchoNativePhase13BridgeSafetyStatus bridgeSafetyStatus = new EchoNativePhase13BridgeSafetyStatus(
                "phase13.bridge.safety.status",
                rehearsed,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                rehearsed ? List.of(
                        "phase13_plan_gate",
                        "loader_boundary_gate",
                        "test_process_boundary_gate",
                        "phase13_m1_safety_gate",
                        "resource_bridge_policy_rehearsal",
                        "registry_bridge_policy_rehearsal"
                ) : List.of()
        );

        return new EchoNativePhase13BridgeRehearsalOutcome(
                packId,
                resourceBridgePolicyRehearsal(packId, resourceRehearsal, diagnostics),
                registryBridgePolicyRehearsal(packId, registryRehearsal, diagnostics),
                phase13BridgeSafetyStatus(packId, bridgeSafetyStatus, diagnostics),
                diagnostics
        );
    }

    private static void checkPhase13Plan(
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
        boolean planningStarted = Boolean.TRUE.equals(data.get("phase13PlanningStarted"));
        boolean planOnly = Boolean.TRUE.equals(data.get("planOnly"));
        boolean prototypeRuntimeStarted = Boolean.TRUE.equals(data.get("prototypeRuntimeStarted"));
        boolean unsafeRuntimeWorkStarted = Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
        if (!pass || !planningStarted || !planOnly || prototypeRuntimeStarted || unsafeRuntimeWorkStarted) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-PLAN-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 plan is not ready for bridge rehearsal",
                    "Bridge policy rehearsal requires a PASS phase13-plan.json with planOnly=true and no runtime work started.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 plan for a fixture with PASS Phase 13 readiness first."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkLoaderBoundaryVerification(
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
        boolean verified = Boolean.TRUE.equals(data.get("verified"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean resolvesRuntimeClasses = Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        boolean mutatedFilesystem = Boolean.TRUE.equals(data.get("mutatedFilesystem"));
        if (!pass || !verified || classloaderCreated || resolvesRuntimeClasses || processLaunched || mutatedFilesystem) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LOADER-BOUNDARY-VERIFICATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Loader boundary verification is not safe for bridge rehearsal",
                    "Bridge policy rehearsal requires PASS loader-boundary-verification.json with no classloader, runtime class resolution, process launch, or filesystem mutation.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 verify boundaries before bridge rehearsal."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkTestProcessBoundary(
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
        boolean verified = Boolean.TRUE.equals(data.get("verified"));
        boolean commandExecuted = Boolean.TRUE.equals(data.get("commandExecuted"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        boolean gameProcessLaunched = Boolean.TRUE.equals(data.get("gameProcessLaunched"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean resolvesRuntimeClasses = Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"));
        boolean filesystemMutated = Boolean.TRUE.equals(data.get("filesystemMutated"));
        if (!pass || !verified || commandExecuted || processLaunched || gameProcessLaunched || classloaderCreated || resolvesRuntimeClasses || filesystemMutated) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TEST-PROCESS-BOUNDARY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Test-process boundary verification is not safe for bridge rehearsal",
                    "Bridge policy rehearsal requires PASS test-process-boundary-verification.json with no command, process, classloader, runtime class resolution, or filesystem mutation.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 verify test-process before bridge rehearsal."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkM1SafetyStatus(
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
        boolean safeToContinue = Boolean.TRUE.equals(data.get("safeToContinue"));
        boolean commandExecuted = Boolean.TRUE.equals(data.get("commandExecuted"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        boolean gameProcessLaunched = Boolean.TRUE.equals(data.get("gameProcessLaunched"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean resolvesRuntimeClasses = Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"));
        boolean filesystemMutated = Boolean.TRUE.equals(data.get("filesystemMutated"));
        boolean unsafeRuntimeWorkStarted = Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
        if (!pass || !safeToContinue || commandExecuted || processLaunched || gameProcessLaunched || classloaderCreated || resolvesRuntimeClasses || filesystemMutated || unsafeRuntimeWorkStarted) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-M1-SAFETY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M1 safety status is not safe for bridge rehearsal",
                    "Bridge policy rehearsal requires PASS phase13-m1-safety-status.json with safeToContinue=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run the Phase 13 M1 boundary checks before bridge rehearsal."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> resourceBridgePolicyRehearsal(
            String packId,
            EchoNativeResourceBridgePolicyRehearsal rehearsal,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_resource_bridge_policy_rehearsal", diagnostics);
        data.put("blockedCapabilities", rehearsal.blockedCapabilities());
        data.put("classloaderCreated", rehearsal.classloaderCreated());
        data.put("commandExecuted", rehearsal.commandExecuted());
        data.put("filesystemMutated", rehearsal.filesystemMutated());
        data.put("gameProcessLaunched", false);
        data.put("gameClassesResolved", rehearsal.gameClassesResolved());
        data.put("packId", packId);
        data.put("plannedResourceScopes", rehearsal.plannedResourceScopes());
        data.put("processLaunched", rehearsal.processLaunched());
        data.put("rehearsalId", rehearsal.rehearsalId());
        data.put("rehearsed", rehearsal.rehearsed());
        data.put("resourceRuntimeAccessed", rehearsal.resourceRuntimeAccessed());
        data.put("summary", rehearsal.rehearsed()
                ? "Resource bridge policy rehearsal passed as data only; no runtime resources were accessed."
                : "Resource bridge policy rehearsal is blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> registryBridgePolicyRehearsal(
            String packId,
            EchoNativeRegistryBridgePolicyRehearsal rehearsal,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_registry_bridge_policy_rehearsal", diagnostics);
        data.put("blockedCapabilities", rehearsal.blockedCapabilities());
        data.put("classloaderCreated", rehearsal.classloaderCreated());
        data.put("commandExecuted", rehearsal.commandExecuted());
        data.put("filesystemMutated", rehearsal.filesystemMutated());
        data.put("gameProcessLaunched", false);
        data.put("gameClassesResolved", rehearsal.gameClassesResolved());
        data.put("packId", packId);
        data.put("plannedRegistryScopes", rehearsal.plannedRegistryScopes());
        data.put("processLaunched", rehearsal.processLaunched());
        data.put("registryInjected", rehearsal.registryInjected());
        data.put("registryMutated", rehearsal.registryMutated());
        data.put("rehearsalId", rehearsal.rehearsalId());
        data.put("rehearsed", rehearsal.rehearsed());
        data.put("summary", rehearsal.rehearsed()
                ? "Registry bridge policy rehearsal passed as data only; no registry injection or mutation occurred."
                : "Registry bridge policy rehearsal is blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> phase13BridgeSafetyStatus(
            String packId,
            EchoNativePhase13BridgeSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_bridge_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedBridgeChecks", status.completedBridgeChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameProcessLaunched", false);
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("resourceRuntimeAccessed", status.resourceRuntimeAccessed());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "Phase 13 bridge rehearsal remains safe to continue with data-only prototype work."
                : "Phase 13 bridge rehearsal is blocked by upstream safety diagnostics.");
        data.put("unsafeRuntimeWorkStarted", status.unsafeRuntimeWorkStarted());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
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
                    "Required Phase 13 bridge rehearsal input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 planning, boundary verification, and test-process reports before bridge rehearsal."
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
