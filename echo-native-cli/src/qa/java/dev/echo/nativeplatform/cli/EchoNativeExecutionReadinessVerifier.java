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
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class EchoNativeExecutionReadinessVerifier {
    private static final String NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY = "nativeProductGameplayBridge";

    EchoNativeExecutionReadinessOutcome verify(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports,
            EchoNativeStaticSafetyScan staticSafetyScan
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(staticSafetyScan.diagnostics());
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        List<Map<String, Object>> gates = new ArrayList<>(gates(packId, reports, staticSafetyScan, diagnostics));
        Map<String, Object> liveMarker = readLiveActivationMarker(fixture);
        boolean playableContentReady = playableContentReady(liveMarker);
        gates.add(gate("isolated-runtime/game/echo-native/module-activation.json", "playableContentReady", true, playableContentReady, playableContentReady));
        if (!playableContentReady) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BETA-LIVE-CONTENT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native loader beta live content is not ready",
                    "The beta gate requires the running isolated client to prove registry mutation, resource application, AdapterCore bridge activation, creative/in-game content visibility, native block registration, product mission/world/progression data visibility, native event bridge attachment, native service bridge startup, Agent 3 gameplay surface coverage, and a complete Native Loader live proof with mutation-ledger records for inventory, world blocks, save data, and HUD.",
                    null,
                    packId,
                    List.of("fixtures/" + packId + "/isolated-runtime/game/echo-native/module-activation.json"),
                    "Run the live Native Loader client proof until module-activation.json contains a complete nativeLoaderLiveProof and MUTATED ledger records for every required runtime host surface."
            ));
        }
        gates = gates.stream()
                .sorted(Comparator.comparing(gate -> String.valueOf(gate.get("report")) + ":" + gate.get("field")))
                .toList();
        boolean ready = diagnostics.stream().noneMatch(EchoNativeExecutionReadinessVerifier::isBlocking)
                && gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("pass")));
        List<String> completedChecks = ready ? List.of(
                "real_process_harness_plan_pass",
                "command_line_preview_materialized",
                "isolated_workspace_ready",
                "runtime_fixture_integrity_verified",
                "support_bundle_ready",
                "rollback_notes_ready",
                "crash_collection_ready",
                "forbidden_import_scan_clean",
                "execution_disabled_until_explicit_launch_command"
        ) : List.of();
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeExecutionReadinessOutcome(
                packId,
                processExecutionReadiness(packId, fixture, ready, gates, completedChecks, reports, staticSafetyScan, sortedDiagnostics),
                controlledLaunchOperatorChecklist(packId, ready, completedChecks, sortedDiagnostics),
                controlledLaunchRollbackPlan(packId, ready, reports, sortedDiagnostics),
                phase13NativeLoaderBetaGate(packId, liveMarker, ready, completedChecks, staticSafetyScan, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static List<Map<String, Object>> gates(
            String packId,
            Map<String, Map<String, Object>> reports,
            EchoNativeStaticSafetyScan staticSafetyScan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> gates = new ArrayList<>();
        gates.add(booleanGate(packId, reports, "real-process-launch-harness-plan.json", "phase3Complete", true, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-launch-harness-plan.json", "processLaunchHarnessReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-launch-harness-plan.json", "realProcessLaunchImplemented", false, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-launch-safety-gate.json", "safeForCommandPreview", true, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-launch-safety-gate.json", "safeForProcessExecution", false, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-launch-safety-gate.json", "launchExecutionAllowed", false, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-command-line-preview.json", "commandLineMaterialized", true, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-command-line-preview.json", "commandPreviewOnly", true, diagnostics));
        gates.add(numberAtLeastGate(packId, reports, "real-process-command-line-preview.json", "argumentCount", 1, diagnostics));
        gates.add(numberAtLeastGate(packId, reports, "real-process-command-line-preview.json", "classpathEntryCount", 1, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-environment-plan.json", "secretSafe", true, diagnostics));
        gates.add(booleanGate(packId, reports, "real-process-environment-plan.json", "redactsSecrets", true, diagnostics));
        gates.add(booleanGate(packId, reports, "isolated-runtime-workspace-safety-status.json", "isolatedRuntimeWorkspaceReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "isolated-runtime-workspace-materialization.json", "fixtureWorkspaceMaterialized", true, diagnostics));
        gates.add(booleanGate(packId, reports, "runtime-fixture-integrity-audit.json", "integrityReady", true, diagnostics));
        gates.add(numberAtLeastGate(packId, reports, "runtime-fixture-integrity-audit.json", "hashVerifiedCount", 2, diagnostics));
        gates.add(booleanGate(packId, reports, "launch-safety-gate.json", "safeForIsolatedLaunchAttempt", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleExportReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleLocalOnly", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-support-bundle.json", "supportBundleUploadsAutomatically", false, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-rollback-notes.json", "rollbackNotesReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-known-limitations.json", "knownLimitationsReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-crash-report-collection.json", "crashReportCollectionReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "experimental-native-loader-label.json", "experimentalNativeLoaderLabelReady", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "safeToOpenFirstPlaytest", true, diagnostics));
        gates.add(booleanGate(packId, reports, "first-playtest-open-gate.json", "publicPlaytestOpen", false, diagnostics));
        gates.add(staticSafetyGate(packId, staticSafetyScan, diagnostics));
        return gates.stream()
                .sorted(Comparator.comparing(gate -> String.valueOf(gate.get("report")) + ":" + gate.get("field")))
                .toList();
    }

    private static Map<String, Object> processExecutionReadiness(
            String packId,
            Path fixture,
            boolean ready,
            List<Map<String, Object>> gates,
            List<String> completedChecks,
            Map<String, Map<String, Object>> reports,
            EchoNativeStaticSafetyScan staticSafetyScan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> commandPreview = data(reports.get("real-process-command-line-preview.json"));
        Map<String, Object> environmentPlan = data(reports.get("real-process-environment-plan.json"));
        Map<String, Object> supportBundle = EchoNativeJson.asObject(data(reports.get("first-playtest-support-bundle.json")).get("supportBundle"));
        Map<String, Object> data = base("native_loader_process_execution_readiness", diagnostics);
        data.put("completedChecks", completedChecks);
        data.put("commandLinePreviewReady", Boolean.TRUE.equals(commandPreview.get("commandLineMaterialized")));
        data.put("controlledLaunchAuthorizationRequired", true);
        data.put("environmentPlanReady", Boolean.TRUE.equals(environmentPlan.get("secretSafe")));
        data.put("executionGateReady", ready);
        data.put("executionReadinessGateReady", ready);
        data.put("fixture", relativePath(fixture));
        data.put("gateCount", gates.size());
        data.put("gates", gates);
        data.put("operatorAuthorizationRequired", true);
        data.put("packId", packId);
        data.put("phase4Complete", ready);
        data.put("processExecutionEnabled", false);
        data.put("requiresSeparateLaunchCommand", true);
        data.put("safeForAutomaticExecution", false);
        data.put("safeForControlledExecutionAuthorization", ready);
        data.put("staticSafetyChecked", true);
        data.put("forbiddenImportMatchCount", staticSafetyMatchCount(staticSafetyScan));
        data.put("supportBundlePath", supportBundle.getOrDefault("path", ""));
        data.put("summary", ready
                ? "Phase 4 execution-readiness gate is ready; launch remains disabled until an explicit controlled launch command."
                : "Phase 4 execution-readiness gate is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> controlledLaunchOperatorChecklist(
            String packId,
            boolean ready,
            List<String> completedChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> items = List.of(
                checklist(1, "confirm_internal_tester", "Confirm the tester understands this is an experimental native loader path."),
                checklist(2, "confirm_fixture_runtime", "Confirm fixture-local runtime artifacts and hashes are the reviewed inputs."),
                checklist(3, "confirm_isolated_workspace", "Confirm the launch target is the fixture isolated runtime workspace."),
                checklist(4, "preserve_support_bundle", "Preserve the support bundle path before and after any launch attempt."),
                checklist(5, "watch_crash_reports", "Collect isolated workspace logs and crash reports only."),
                checklist(6, "avoid_user_mutation", "Do not touch user launcher installs, saves, configs, jars, or caches."),
                checklist(7, "use_rollback_notes", "Rollback to the stable NeoForge beta workspace if the native launch path fails."),
                checklist(8, "require_explicit_launch_command", "Run only the next explicit controlled launch command, not this verifier.")
        );
        Map<String, Object> data = base("native_loader_controlled_launch_operator_checklist", diagnostics);
        data.put("checklistItemCount", items.size());
        data.put("completedChecks", completedChecks);
        data.put("items", items);
        data.put("operatorChecklistReady", ready);
        data.put("packId", packId);
        data.put("summary", ready
                ? "Operator checklist is ready for the next explicit controlled launch command."
                : "Operator checklist is blocked until execution readiness passes.");
        return data;
    }

    private static Map<String, Object> controlledLaunchRollbackPlan(
            String packId,
            boolean ready,
            Map<String, Map<String, Object>> reports,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> rollback = data(reports.get("first-playtest-rollback-notes.json"));
        Map<String, Object> data = base("native_loader_controlled_launch_rollback_plan", diagnostics);
        data.put("packId", packId);
        data.put("rollbackPlanReady", ready && Boolean.TRUE.equals(rollback.get("rollbackNotesReady")));
        data.put("rollbackRequiredBeforeExternalRelease", true);
        data.put("stableFallback", "NeoForge beta workspace");
        data.put("steps", rollback.getOrDefault("notes", List.of()));
        data.put("summary", ready
                ? "Rollback plan is ready for a controlled native loader beta launch attempt."
                : "Rollback plan remains blocked by upstream readiness diagnostics.");
        return data;
    }

    private static Map<String, Object> phase13NativeLoaderBetaGate(
            String packId,
            Map<String, Object> liveMarker,
            boolean ready,
            List<String> completedChecks,
            EchoNativeStaticSafetyScan staticSafetyScan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        boolean liveRegistryMutated = Boolean.TRUE.equals(liveMarker.get("registryMutated"));
        boolean liveResourcesApplied = Boolean.TRUE.equals(liveMarker.get("minecraftResourcesApplied"));
        boolean liveCreativeVisible = Boolean.TRUE.equals(liveMarker.get("creativeContentVisible"));
        boolean liveAdapterBridgeActive = Boolean.TRUE.equals(liveMarker.get("adapterCoreRuntimeBridgeActive"));
        boolean liveEventBridgeApplied = Boolean.TRUE.equals(liveMarker.get("nativeEventBridgeApplied"));
        boolean liveServiceBridgeApplied = Boolean.TRUE.equals(liveMarker.get("nativeServiceBridgeApplied"));
        Map<String, Object> productGameplayEvidence = productGameplayEvidence(liveMarker);
        boolean liveNativeProductGameplayContentApplied = Boolean.TRUE.equals(productGameplayEvidence.get("applied"));
        boolean liveNativeProductFirstPlayableLoopReady = Boolean.TRUE.equals(productGameplayEvidence.get("firstPlayableLoopReady"));
        long liveRegisteredBlockCount = registeredBlockCount(liveMarker);
        long liveRuntimeInitializedServiceCount = runtimeInitializedServiceCount(liveMarker);
        long liveSafeLifecycleHookRunCount = safeLifecycleHookRunCount(liveMarker);
        long liveSafeEventHookRunCount = safeEventHookRunCount(liveMarker);
        long liveNativeProductMissionDefinitionCount = liveMarkerCount(productGameplayEvidence, "missionDefinitionCount");
        long liveNativeProductWorldRegionCount = liveMarkerCount(productGameplayEvidence, "worldRegionCount");
        long liveNativeProductProgressionAdvancementCount = liveMarkerCount(productGameplayEvidence, "progressionAdvancementCount");
        long liveAugmentedCreativeTabCount = augmentedCreativeTabCount(liveMarker);
        long liveVisibleCreativeTabPathCount = visibleCreativeTabPathCount(liveMarker);
        boolean liveNativeCreativeTabBridgeApplied = nativeCreativeTabBridgeApplied(liveMarker);
        boolean liveNativeCreativeModuleTabContentVisible = nativeCreativeModuleTabContentVisible(liveMarker);
        boolean liveNativeCreativeModuleTabRegistryBacked = nativeCreativeModuleTabRegistryBacked(liveMarker);
        long liveNativeCreativeModuleTabVisibleItemCount = nativeCreativeModuleTabVisibleItemCount(liveMarker);
        boolean liveAgent3GameplaySurfaceCoverageReady = agent3GameplaySurfaceCoverageReady(liveMarker);
        long liveAgent3GameplaySurfaceReadyCount = agent3GameplaySurfaceReadyCount(liveMarker);
        Map<String, Object> nativeLoaderLiveProof = EchoNativeJson.asObject(liveMarker.get("nativeLoaderLiveProof"));
        boolean liveNativeLoaderProofComplete = nativeLoaderLiveProofComplete(liveMarker);
        boolean liveRequiredMutationSurfacesMutated =
                Boolean.TRUE.equals(nativeLoaderLiveProof.get("requiredMutationSurfacesMutated"));
        boolean playableContentReady = playableContentReady(liveMarker);
        boolean betaReady = ready && playableContentReady;
        Map<String, Object> data = base("phase13_native_loader_beta_gate", diagnostics);
        data.put("completedChecks", completedChecks);
        data.put("controlledLaunchReadyForAuthorization", ready);
        data.put("internalTesterBetaReady", betaReady);
        data.put("liveAdapterCoreRuntimeBridgeActive", liveAdapterBridgeActive);
        data.put("liveCreativeContentVisible", liveCreativeVisible);
        data.put("liveEventBridgeApplied", liveEventBridgeApplied);
        data.put("liveMinecraftResourcesApplied", liveResourcesApplied);
        data.put("liveNativeProductGameplayContentApplied", liveNativeProductGameplayContentApplied);
        data.put("liveNativeProductGameplayEvidenceSource", productGameplayEvidence.getOrDefault("source", ""));
        data.put("liveNativeProductFirstPlayableLoopReady", liveNativeProductFirstPlayableLoopReady);
        data.put("liveNativeProductMissionDefinitionCount", liveNativeProductMissionDefinitionCount);
        data.put("liveNativeProductWorldRegionCount", liveNativeProductWorldRegionCount);
        data.put("liveNativeProductProgressionAdvancementCount", liveNativeProductProgressionAdvancementCount);
        data.put("liveRegistryMutated", liveRegistryMutated);
        data.put("liveRegisteredBlockCount", liveRegisteredBlockCount);
        data.put("liveRegisteredBlocksPresent", liveRegisteredBlockCount > 0);
        data.put("liveRuntimeInitializedServiceCount", liveRuntimeInitializedServiceCount);
        data.put("liveRuntimeInitializedServicesPresent", liveRuntimeInitializedServiceCount > 0);
        data.put("liveAgent3GameplaySurfaceCoverageReady", liveAgent3GameplaySurfaceCoverageReady);
        data.put("liveAgent3GameplaySurfaceReadyCount", liveAgent3GameplaySurfaceReadyCount);
        data.put("liveSafeLifecycleHookRunCount", liveSafeLifecycleHookRunCount);
        data.put("liveSafeLifecycleHooksPresent", liveSafeLifecycleHookRunCount > 0);
        data.put("liveSafeEventHookRunCount", liveSafeEventHookRunCount);
        data.put("liveSafeEventHooksPresent", liveSafeEventHookRunCount > 0);
        data.put("liveAugmentedCreativeTabCount", liveAugmentedCreativeTabCount);
        data.put("liveVisibleCreativeTabPathCount", liveVisibleCreativeTabPathCount);
        data.put("liveNativeCreativeTabBridgeApplied", liveNativeCreativeTabBridgeApplied);
        data.put("liveNativeCreativeModuleTabContentVisible", liveNativeCreativeModuleTabContentVisible);
        data.put("liveNativeCreativeModuleTabRegistryBacked", liveNativeCreativeModuleTabRegistryBacked);
        data.put("liveNativeCreativeModuleTabVisibleItemCount", liveNativeCreativeModuleTabVisibleItemCount);
        data.put("liveServiceBridgeApplied", liveServiceBridgeApplied);
        data.put("liveNativeLoaderProofComplete", liveNativeLoaderProofComplete);
        data.put("liveRequiredMutationSurfacesMutated", liveRequiredMutationSurfacesMutated);
        data.put("liveNativeLoaderProofStatus", nativeLoaderLiveProof.getOrDefault("status", ""));
        data.put("liveNativeLoaderProofMissingTargets", nativeLoaderLiveProof.getOrDefault("missingTargets", List.of()));
        data.put("liveNativeLoaderProofRequiredMutationSurfaces", nativeLoaderLiveProof.getOrDefault("requiredMutationSurfaces", List.of()));
        data.put("liveNativeLoaderProofMutatedSurfaces", nativeLoaderLiveProof.getOrDefault("mutationLedgerMutatedSurfaces", List.of()));
        data.put("nativeLoaderBetaGateReady", betaReady);
        data.put("nextImplementationPhase", betaReady ? "phase5.controlled_authorized_process_launch" : "adaptercore.native_runtime_bridge");
        data.put("packId", packId);
        data.put("playableContentReady", playableContentReady);
        data.put("playableContentRequired", true);
        data.put("publicBetaReady", false);
        data.put("staticSafetyChecked", true);
        data.put("forbiddenImportMatchCount", staticSafetyMatchCount(staticSafetyScan));
        data.put("summary", betaReady
                ? "Native loader beta gate is ready with live AdapterCore runtime bridge evidence."
                : "Native loader beta gate is blocked until the running client proves the full AdapterCore native runtime bridge.");
        return data;
    }

    private static Map<String, Object> readLiveActivationMarker(Path fixture) {
        Path marker = fixture.resolve("isolated-runtime/game/echo-native/module-activation.json");
        if (!Files.isRegularFile(marker)) {
            return Map.of();
        }
        try {
            return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(marker)));
        } catch (IOException | RuntimeException exception) {
            return Map.of();
        }
    }

    private static boolean playableContentReady(Map<String, Object> liveMarker) {
        Map<String, Object> productGameplayEvidence = productGameplayEvidence(liveMarker);
        return Boolean.TRUE.equals(liveMarker.get("registryMutated"))
                && Boolean.TRUE.equals(liveMarker.get("minecraftResourcesApplied"))
                && Boolean.TRUE.equals(liveMarker.get("creativeContentVisible"))
                && Boolean.TRUE.equals(liveMarker.get("adapterCoreRuntimeBridgeActive"))
                && registeredBlockCount(liveMarker) > 0
                && nativeCreativeTabBridgeApplied(liveMarker)
                && nativeCreativeModuleTabContentVisible(liveMarker)
                && nativeCreativeModuleTabRegistryBacked(liveMarker)
                && nativeCreativeModuleTabVisibleItemCount(liveMarker) > 0
                && visibleCreativeTabPathCount(liveMarker) > 0
                && Boolean.TRUE.equals(productGameplayEvidence.get("applied"))
                && Boolean.TRUE.equals(productGameplayEvidence.get("firstPlayableLoopReady"))
                && liveMarkerCount(productGameplayEvidence, "missionDefinitionCount") > 0
                && liveMarkerCount(productGameplayEvidence, "worldRegionCount") > 0
                && liveMarkerCount(productGameplayEvidence, "progressionAdvancementCount") > 0
                && Boolean.TRUE.equals(liveMarker.get("nativeEventBridgeApplied"))
                && safeEventHookRunCount(liveMarker) > 0
                && safeLifecycleHookRunCount(liveMarker) > 0
                && Boolean.TRUE.equals(liveMarker.get("nativeServiceBridgeApplied"))
                && runtimeInitializedServiceCount(liveMarker) > 0
                && agent3GameplaySurfaceCoverageReady(liveMarker)
                && agent3GameplaySurfaceReadyCount(liveMarker) >= 4
                && nativeLoaderLiveProofComplete(liveMarker);
    }

    private static boolean nativeLoaderLiveProofComplete(Map<String, Object> liveMarker) {
        Map<String, Object> proof = EchoNativeJson.asObject(liveMarker.get("nativeLoaderLiveProof"));
        if (!proof.isEmpty()) {
            return Boolean.TRUE.equals(proof.get("complete"))
                    && Boolean.TRUE.equals(proof.get("requiredMutationSurfacesMutated"))
                    && Boolean.TRUE.equals(proof.get("gameplayReadyClaimAllowed"));
        }
        return Boolean.TRUE.equals(liveMarker.get("nativeLoaderLiveClientGameplayReady"))
                && Boolean.TRUE.equals(liveMarker.get("nativeLoaderLiveProofComplete"));
    }

    private static Map<String, Object> productGameplayEvidence(Map<String, Object> liveMarker) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> productGameplayBridge = EchoNativeJson.asObject(runtimeBridge.get(NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY));
        if (!productGameplayBridge.isEmpty()) {
            return productGameplayEvidence(
                    "runtime_bridge",
                    liveMarkerCount(productGameplayBridge, "missionDefinitionCount"),
                    liveMarkerCount(productGameplayBridge, "worldRegionCount"),
                    liveMarkerCount(productGameplayBridge, "progressionAdvancementCount"),
                    Boolean.TRUE.equals(productGameplayBridge.get("applied"))
                            || Boolean.TRUE.equals(liveMarker.get("nativeProductGameplayContentApplied")),
                    Boolean.TRUE.equals(productGameplayBridge.get("firstPlayableLoopReady"))
                            || Boolean.TRUE.equals(liveMarker.get("nativeFirstPlayableLoopReady")));
        }
        long markerMissions = liveMarkerCount(liveMarker, "nativeProductMissionDefinitionCount");
        long markerWorldRegions = liveMarkerCount(liveMarker, "nativeProductWorldRegionCount");
        long markerProgression = liveMarkerCount(liveMarker, "nativeProductProgressionAdvancementCount");
        if (liveMarker.containsKey("nativeProductGameplayContentApplied")
                || liveMarker.containsKey("nativeFirstPlayableLoopReady")
                || liveMarker.containsKey("nativeGameplayHandlerExecuted")
                || markerMissions > 0
                || markerWorldRegions > 0
                || markerProgression > 0) {
            return productGameplayEvidence(
                    "live_marker",
                    markerMissions,
                    markerWorldRegions,
                    markerProgression,
                    Boolean.TRUE.equals(liveMarker.get("nativeProductGameplayContentApplied")),
                    Boolean.TRUE.equals(liveMarker.get("nativeFirstPlayableLoopReady")));
        }
        Path resourcePack = liveResourcePack(liveMarker);
        if (resourcePack == null || !Files.isRegularFile(resourcePack)) {
            return productGameplayEvidence("missing", 0, 0, 0);
        }
        Map<String, String> contentPrefixes = productGameplayContentDataPrefixes(runtimeBridge);
        if (contentPrefixes.isEmpty()) {
            return productGameplayEvidence("missing_content_prefixes", 0, 0, 0);
        }
        Set<String> missions = new TreeSet<>();
        Set<String> worldRegions = new TreeSet<>();
        Set<String> progression = new TreeSet<>();
        String missionsPrefix = contentPrefix(contentPrefixes, "missions");
        String worldRegionsPrefix = contentPrefix(contentPrefixes, "world_regions", "worldRegions");
        String progressionPrefix = contentPrefix(contentPrefixes, "progression_advancements", "progressionAdvancements", "advancements");
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(resourcePack))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.endsWith(".json")) {
                    continue;
                }
                if (!missionsPrefix.isBlank() && name.startsWith(missionsPrefix)) {
                    addDataPath(missions, name, missionsPrefix);
                } else if (!worldRegionsPrefix.isBlank() && name.startsWith(worldRegionsPrefix)) {
                    addDataPath(worldRegions, name, worldRegionsPrefix);
                } else if (!progressionPrefix.isBlank() && name.startsWith(progressionPrefix)) {
                    addDataPath(progression, name, progressionPrefix);
                }
            }
        } catch (IOException exception) {
            return productGameplayEvidence("resource_pack_unreadable", 0, 0, 0);
        }
        return productGameplayEvidence("resource_pack_prefix_map", missions.size(), worldRegions.size(), progression.size());
    }

    private static Map<String, Object> productGameplayEvidence(
            String source,
            long missionDefinitionCount,
            long worldRegionCount,
            long progressionAdvancementCount
    ) {
        return productGameplayEvidence(source, missionDefinitionCount, worldRegionCount, progressionAdvancementCount, false, false);
    }

    private static Map<String, Object> productGameplayEvidence(
            String source,
            long missionDefinitionCount,
            long worldRegionCount,
            long progressionAdvancementCount,
            boolean applied,
            boolean firstPlayableLoopReady
    ) {
        boolean dataDiscovered = missionDefinitionCount > 0 && worldRegionCount > 0 && progressionAdvancementCount > 0;
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("applied", applied);
        evidence.put("dataDiscovered", dataDiscovered);
        evidence.put("source", source);
        evidence.put("firstPlayableLoopReady", firstPlayableLoopReady);
        evidence.put("missionDefinitionCount", missionDefinitionCount);
        evidence.put("worldRegionCount", worldRegionCount);
        evidence.put("progressionAdvancementCount", progressionAdvancementCount);
        return evidence;
    }

    private static Path liveResourcePack(Map<String, Object> liveMarker) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> resourceBridge = EchoNativeJson.asObject(runtimeBridge.get("resourceBridge"));
        Object raw = resourceBridge.get("resourcePack");
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        return Path.of(String.valueOf(raw));
    }

    private static Map<String, String> productGameplayContentDataPrefixes(Map<String, Object> runtimeBridge) {
        Map<String, Object> productGameplayBridge = EchoNativeJson.asObject(runtimeBridge.get(NATIVE_PRODUCT_GAMEPLAY_BRIDGE_KEY));
        Map<String, String> prefixes = stringMap(productGameplayBridge.get("contentDataPrefixes"));
        if (!prefixes.isEmpty()) {
            return prefixes;
        }
        Map<String, Object> resourceBridge = EchoNativeJson.asObject(runtimeBridge.get("resourceBridge"));
        return stringMap(firstPresent(resourceBridge, "nativeProductGameplayContentDataPrefixes", "contentDataPrefixes"));
    }

    private static Map<String, String> stringMap(Object value) {
        Map<String, Object> raw = EchoNativeJson.asObject(value);
        Map<String, String> strings = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() == null || String.valueOf(entry.getValue()).isBlank()) {
                continue;
            }
            strings.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return strings;
    }

    private static Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    private static String contentPrefix(Map<String, String> prefixes, String... keys) {
        for (String key : keys) {
            String direct = normalizedContentPrefix(prefixes.get(key));
            if (!direct.isBlank()) {
                return direct;
            }
            String normalizedKey = key.toLowerCase(java.util.Locale.ROOT);
            for (Map.Entry<String, String> entry : prefixes.entrySet()) {
                if (entry.getKey().toLowerCase(java.util.Locale.ROOT).equals(normalizedKey)) {
                    return normalizedContentPrefix(entry.getValue());
                }
            }
        }
        return "";
    }

    private static String normalizedContentPrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String normalized = prefix.trim().replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private static void addDataPath(Set<String> ids, String name, String prefix) {
        String path = name.substring(prefix.length(), name.length() - ".json".length());
        if (!path.isBlank()) {
            ids.add(path);
        }
    }

    private static long liveMarkerCount(Map<String, Object> liveMarker, String field) {
        Object raw = liveMarker.get(field);
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static long registeredBlockCount(Map<String, Object> liveMarker) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        Object raw = registryBridge.get("registeredBlockCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static boolean nativeCreativeTabBridgeApplied(Map<String, Object> liveMarker) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        return Boolean.TRUE.equals(registryBridge.get("nativeCreativeTabBridgeApplied"));
    }

    private static long runtimeInitializedServiceCount(Map<String, Object> liveMarker) {
        Object markerRaw = liveMarker.get("nativeRuntimeInitializedServiceCount");
        if (markerRaw instanceof Number number) {
            long value = number.longValue();
            if (value > 0) {
                return value;
            }
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(runtimeBridge.get("serviceBridge"));
        Object raw = serviceBridge.get("runtimeInitializedServiceCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        Object started = serviceBridge.get("startedServiceCount");
        if (started instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static boolean agent3GameplaySurfaceCoverageReady(Map<String, Object> liveMarker) {
        if (Boolean.TRUE.equals(liveMarker.get("nativeAgent3GameplaySurfaceCoverageReady"))) {
            return true;
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(runtimeBridge.get("serviceBridge"));
        return Boolean.TRUE.equals(serviceBridge.get("agent3GameplaySurfaceCoverageReady"));
    }

    private static long agent3GameplaySurfaceReadyCount(Map<String, Object> liveMarker) {
        Object markerRaw = liveMarker.get("nativeAgent3GameplaySurfaceReadyCount");
        if (markerRaw instanceof Number number) {
            return number.longValue();
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> serviceBridge = EchoNativeJson.asObject(runtimeBridge.get("serviceBridge"));
        Object raw = serviceBridge.get("agent3ReadySurfaceCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static long safeEventHookRunCount(Map<String, Object> liveMarker) {
        Object markerRaw = liveMarker.get("nativeSafeEventHookRunCount");
        if (markerRaw instanceof Number number) {
            return number.longValue();
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> eventBridge = EchoNativeJson.asObject(runtimeBridge.get("eventBridge"));
        Object raw = eventBridge.get("safeEventHookRunCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static long safeLifecycleHookRunCount(Map<String, Object> liveMarker) {
        Object markerRaw = liveMarker.get("nativeSafeLifecycleHookRunCount");
        if (markerRaw instanceof Number number) {
            return number.longValue();
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> lifecycleBridge = EchoNativeJson.asObject(runtimeBridge.get("lifecycleBridge"));
        Object raw = lifecycleBridge.get("safeLifecycleHookRunCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static long augmentedCreativeTabCount(Map<String, Object> liveMarker) {
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        Object raw = registryBridge.get("augmentedCreativeTabCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static long visibleCreativeTabPathCount(Map<String, Object> liveMarker) {
        Object markerRaw = liveMarker.get("nativeVisibleCreativeTabPathCount");
        if (markerRaw instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        Object raw = registryBridge.get("visibleCreativeTabPathCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return augmentedCreativeTabCount(liveMarker);
    }

    private static boolean nativeCreativeModuleTabContentVisible(Map<String, Object> liveMarker) {
        if (Boolean.TRUE.equals(liveMarker.get("nativeCreativeModuleTabContentVisible"))) {
            return true;
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        return Boolean.TRUE.equals(registryBridge.get("nativeCreativeModuleTabContentVisible"));
    }

    private static boolean nativeCreativeModuleTabRegistryBacked(Map<String, Object> liveMarker) {
        if (Boolean.TRUE.equals(liveMarker.get("nativeCreativeModuleTabRegistryBacked"))) {
            return true;
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        return Boolean.TRUE.equals(registryBridge.get("nativeCreativeModuleTabRegistryBacked"));
    }

    private static long nativeCreativeModuleTabVisibleItemCount(Map<String, Object> liveMarker) {
        Object markerRaw = liveMarker.get("nativeCreativeModuleTabVisibleItemCount");
        if (markerRaw instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(liveMarker.get("runtimeBridge"));
        Map<String, Object> registryBridge = EchoNativeJson.asObject(runtimeBridge.get("registryBridge"));
        Object raw = registryBridge.get("nativeCreativeModuleTabVisibleItemCount");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private static Map<String, Object> checklist(int order, String id, String summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("order", order);
        item.put("required", true);
        item.put("summary", summary);
        return item;
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
                    "ECHO-NATIVE-EXECUTION-READINESS-GATE-FIELD-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Execution readiness gate field is not ready",
                    reportName + " must report " + field + "=" + expected + " before controlled launch authorization can be prepared.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate upstream Phase 13 reports and resolve diagnostics before execution readiness."
            ));
        }
        return gate(reportName, field, expected, actual, pass);
    }

    private static Map<String, Object> numberAtLeastGate(
            String packId,
            Map<String, Map<String, Object>> reports,
            String reportName,
            String field,
            long minimum,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Long actual = asLong(nestedDataValue(reports, reportName, field));
        boolean pass = actual != null && actual >= minimum;
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-EXECUTION-READINESS-GATE-COUNT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Execution readiness gate count is not ready",
                    reportName + " must report " + field + ">=" + minimum + " before controlled launch authorization can be prepared.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/" + reportName),
                    "Regenerate upstream launch preview and fixture integrity reports before execution readiness."
            ));
        }
        return gate(reportName, field, ">=" + minimum, actual == null ? "" : actual, pass);
    }

    private static Map<String, Object> staticSafetyGate(
            String packId,
            EchoNativeStaticSafetyScan staticSafetyScan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        long matchCount = staticSafetyMatchCount(staticSafetyScan);
        boolean pass = matchCount == 0L;
        if (!pass) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-EXECUTION-READINESS-FORBIDDEN-IMPORTS",
                    EchoNativeIssueSeverity.ERROR,
                    "Execution readiness static safety scan found forbidden imports",
                    "Native loader execution readiness requires zero forbidden native runtime imports.",
                    null,
                    packId,
                    List.of("echo-native-platform"),
                    "Remove forbidden native runtime references before controlled launch readiness."
            ));
        }
        return gate("static-safety-scan", "forbiddenImportMatchCount", 0L, matchCount, pass);
    }

    private static Map<String, Object> gate(String reportName, String field, Object expected, Object actual, boolean pass) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("actual", actual == null ? "" : actual);
        gate.put("classloaderCreated", false);
        gate.put("commandExecuted", false);
        gate.put("downloadAllowed", false);
        gate.put("expected", expected);
        gate.put("field", field);
        gate.put("filesystemMutated", false);
        gate.put("nativeExtractionStarted", false);
        gate.put("pass", pass);
        gate.put("processLaunched", false);
        gate.put("report", reportName);
        return gate;
    }

    private static void checkReport(
            String reportName,
            Path path,
            Map<String, Object> report,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-EXECUTION-READINESS-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Execution readiness upstream report is not PASS",
                    "Execution readiness requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Regenerate upstream Phase 13 reports and resolve diagnostics before the controlled launch readiness gate."
            ));
        }
        if (hasUnsafeRuntimeWork(data(report))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-EXECUTION-READINESS-UNSAFE-WORK",
                    EchoNativeIssueSeverity.ERROR,
                    "Execution readiness upstream report contains unsafe runtime work",
                    reportName + " indicates unsafe runtime work that cannot be carried into the execution-readiness gate.",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Keep downloads, classloading, native extraction, registry mutation, process launch, command execution, and user install mutation out of this gate."
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
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-EXECUTION-READINESS-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Execution readiness required report missing",
                    "Execution readiness requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate real-process harness, isolated workspace, first-playtest, and runtime fixture reports before execution readiness."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static Object nestedDataValue(Map<String, Map<String, Object>> reports, String reportName, String field) {
        Object current = data(reports.get(reportName));
        for (String part : field.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("configMutationAllowed", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("downloadsAllowed", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("packMutationAllowed", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("saveMutationAllowed", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userInstallMutationAllowed", false);
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

    private static long staticSafetyMatchCount(EchoNativeStaticSafetyScan scan) {
        return asLong(scan.data().get("matchCount")) == null ? 0L : asLong(scan.data().get("matchCount"));
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
