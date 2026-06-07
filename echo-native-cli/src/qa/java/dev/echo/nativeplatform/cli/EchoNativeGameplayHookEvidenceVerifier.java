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

final class EchoNativeGameplayHookEvidenceVerifier {
    EchoNativeGameplayHookEvidenceOutcome verify(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> completion = data(reports.get("phase13-m22-completion.json"));
        Map<String, Object> markerReport = data(reports.get("native-live-activation-marker.json"));
        Map<String, Object> baseline = data(reports.get("minecraft-baseline-playability.json"));
        boolean m22Complete = bool(completion, "phase13M22Complete");
        boolean markerPresent = bool(markerReport, "activationMarkerPresent");
        boolean baselinePlayable = bool(baseline, "baselinePlayable") || bool(baseline, "minecraftBaselinePlayable");

        Path markerPath = fixture.resolve("isolated-runtime/game/echo-native/module-activation.json").normalize();
        Path latestLog = fixture.resolve("isolated-runtime/game/logs/latest.log").normalize();
        String latestLogText = Files.isRegularFile(latestLog) ? Files.readString(latestLog) : "";
        boolean playerJoinedWorld = latestLogText.contains("EchoNativeTester joined the game");
        boolean survivalModeObserved = latestLogText.contains("Set own game mode to Survival Mode");
        boolean playerDeathObserved = latestLogText.contains("EchoNativeTester was slain by");
        boolean cleanShutdownObserved = latestLogText.contains("Stopping!") || latestLogText.contains("Stopping server");
        boolean chunksSaved = latestLogText.contains("All chunks are saved");
        boolean ashfallHookObserved = containsAny(latestLogText, List.of(
                "EchoAshfall",
                "ashfall.",
                "[Ashfall]",
                "Ashfall Protocol",
                "echoashfallprotocol"
        ));
        boolean echoHookObserved = containsAny(latestLogText, List.of(
                "EchoCore",
                "EchoPlatform",
                "EchoPack",
                "EchoTerminal",
                "EchoHoloMap",
                "EchoMission",
                "ECHO module"
        ));
        List<String> modules = markerModules(markerPath);
        Map<String, Object> agent7WorldLiveHostHookEvidence = markerAgent7WorldLiveHostHookEvidence(markerPath);
        String agent7ExactLiveHookEvidenceSource = markerAgent7ExactLiveHookEvidenceSource(markerPath);
        boolean agent7WorldLiveHostHooksVerified = agent7ExactWorldLiveHostHooksVerified(agent7WorldLiveHostHookEvidence);
        int agent7WorldLiveHostVerifiedHookCount = agent7WorldLiveHostHooksVerified
                ? intValue(agent7WorldLiveHostHookEvidence, "verifiedHookCount")
                : 0;
        int agent7WorldLiveHostRequiredHookCount = intValue(agent7WorldLiveHostHookEvidence, "requiredHookCount");
        boolean prerequisitesSatisfied = m22Complete
                && markerPresent
                && baselinePlayable
                && !modules.isEmpty()
                && diagnostics.stream().noneMatch(EchoNativeGameplayHookEvidenceVerifier::isBlocking);
        long verifiedHookCount = modules.stream()
                .filter(module -> moduleHasHookEvidence(module, latestLogText))
                .count();
        boolean gameplayHooksVerified = verifiedHookCount == modules.size() && verifiedHookCount > 0;

        if (prerequisitesSatisfied && !gameplayHooksVerified) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-EVIDENCE-PENDING",
                    EchoNativeIssueSeverity.WARNING,
                    "Ashfall gameplay hook evidence is still pending",
                    "The native baseline and activation marker are present, but fixture-local logs do not prove live Ashfall/ECHO gameplay hooks for the required modules.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/ashfall-gameplay-hook-evidence.json"),
                    "Add controlled native hook instrumentation or log markers, then rerun phase13 verify gameplay-hooks."
            ));
        } else if (!prerequisitesSatisfied && diagnostics.stream().noneMatch(EchoNativeGameplayHookEvidenceVerifier::isBlocking)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-EVIDENCE-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook evidence verification is missing prerequisites",
                    "M23 requires PASS M22 completion, activation marker evidence, and baseline playability reports.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-m22-completion.json"),
                    "Complete M22 and baseline evidence before verifying gameplay hooks."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean complete = prerequisitesSatisfied && sortedDiagnostics.stream().noneMatch(EchoNativeGameplayHookEvidenceVerifier::isBlocking);
        boolean betaReady = complete && gameplayHooksVerified;

        return new EchoNativeGameplayHookEvidenceOutcome(
                packId,
                gameplayEvidence(packId, latestLog, markerPath, modules, playerJoinedWorld, survivalModeObserved, playerDeathObserved, cleanShutdownObserved, chunksSaved, ashfallHookObserved, echoHookObserved, gameplayHooksVerified, agent7WorldLiveHostHookEvidence, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, agent7ExactLiveHookEvidenceSource, sortedDiagnostics),
                moduleHookStatus(packId, modules, latestLogText, agent7WorldLiveHostHookEvidence, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, agent7ExactLiveHookEvidenceSource, sortedDiagnostics),
                betaReadiness(packId, modules.size(), verifiedHookCount, betaReady, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, agent7ExactLiveHookEvidenceSource, sortedDiagnostics),
                m23Completion(packId, modules.size(), verifiedHookCount, complete, betaReady, agent7WorldLiveHostHooksVerified, agent7WorldLiveHostVerifiedHookCount, agent7WorldLiveHostRequiredHookCount, agent7ExactLiveHookEvidenceSource, sortedDiagnostics),
                m24Readiness(packId, complete, betaReady, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> gameplayEvidence(
            String packId,
            Path latestLog,
            Path markerPath,
            List<String> modules,
            boolean playerJoinedWorld,
            boolean survivalModeObserved,
            boolean playerDeathObserved,
            boolean cleanShutdownObserved,
            boolean chunksSaved,
            boolean ashfallHookObserved,
            boolean echoHookObserved,
            boolean gameplayHooksVerified,
            Map<String, Object> agent7WorldLiveHostHookEvidence,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            String agent7ExactLiveHookEvidenceSource,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_ashfall_gameplay_hook_evidence", diagnostics);
        data.put("activationMarkerPresent", Files.isRegularFile(markerPath));
        data.put("activationMarkerPath", relativePath(markerPath));
        data.put("ashfallHookObserved", ashfallHookObserved);
        data.put("chunksSaved", chunksSaved);
        data.put("cleanShutdownObserved", cleanShutdownObserved);
        data.put("echoHookObserved", echoHookObserved);
        data.put("gameplayHooksVerified", gameplayHooksVerified);
        data.put("agent7WorldLiveHostHookEvidence", agent7WorldLiveHostHookEvidence);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("agent7ExactLiveHookEvidenceSource", agent7ExactLiveHookEvidenceSource);
        data.put("latestLog", relativePath(latestLog));
        data.put("latestLogPresent", Files.isRegularFile(latestLog));
        data.put("moduleCount", modules.size());
        data.put("playerDeathObserved", playerDeathObserved);
        data.put("playerJoinedWorld", playerJoinedWorld);
        data.put("survivalModeObserved", survivalModeObserved);
        data.put("summary", gameplayHooksVerified
                ? "Fixture-local evidence proves Ashfall gameplay hooks for all activation-marked modules."
                : "Fixture-local evidence proves vanilla gameplay/world interaction, but not Ashfall module gameplay hooks yet.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> moduleHookStatus(
            String packId,
            List<String> modules,
            String latestLogText,
            Map<String, Object> agent7WorldLiveHostHookEvidence,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            String agent7ExactLiveHookEvidenceSource,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Map<String, Object>> statuses = modules.stream()
                .sorted(String::compareTo)
                .map(module -> {
                    boolean hookVerified = moduleHasHookEvidence(module, latestLogText);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("activationMarkerWritten", true);
                    item.put("id", module);
                    item.put("liveGameplayHookVerified", hookVerified);
                    item.put("status", hookVerified ? "gameplay_hook_verified" : "gameplay_hook_pending");
                    return item;
                })
                .toList();
        long verified = statuses.stream()
                .filter(status -> Boolean.TRUE.equals(status.get("liveGameplayHookVerified")))
                .count();
        Map<String, Object> data = base("phase13_m23_native_module_gameplay_hook_status", diagnostics);
        data.put("gameplayHookVerifiedCount", verified);
        data.put("agent7WorldLiveHostHookEvidence", agent7WorldLiveHostHookEvidence);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("agent7ExactLiveHookEvidenceSource", agent7ExactLiveHookEvidenceSource);
        data.put("gameplayHookPendingCount", modules.size() - verified);
        data.put("moduleCount", modules.size());
        data.put("modules", statuses);
        data.put("summary", verified == modules.size() && verified > 0
                ? "All activation-marked modules have gameplay hook evidence."
                : "Activation-marked modules still need gameplay hook evidence.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> betaReadiness(
            String packId,
            int moduleCount,
            long verifiedHookCount,
            boolean betaReady,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            String agent7ExactLiveHookEvidenceSource,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_ashfall_playable_beta_readiness", diagnostics);
        data.put("ashfallPlayableBetaReady", betaReady);
        data.put("activationMarkedModuleCount", moduleCount);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("agent7ExactLiveHookEvidenceSource", agent7ExactLiveHookEvidenceSource);
        data.put("gameplayHookVerifiedCount", verifiedHookCount);
        data.put("remainingGameplayHookCount", moduleCount - verifiedHookCount);
        data.put("summary", betaReady
                ? "Ashfall playable beta gate has module gameplay hook evidence."
                : "Ashfall playable beta remains blocked until module gameplay hooks are verified.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> m23Completion(
            String packId,
            int moduleCount,
            long verifiedHookCount,
            boolean complete,
            boolean betaReady,
            boolean agent7WorldLiveHostHooksVerified,
            int agent7WorldLiveHostVerifiedHookCount,
            int agent7WorldLiveHostRequiredHookCount,
            String agent7ExactLiveHookEvidenceSource,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_completion", diagnostics);
        data.put("phase13M23Complete", complete);
        data.put("phase13M24Ready", complete && !betaReady);
        data.put("ashfallPlayableBetaReady", betaReady);
        data.put("activationMarkedModuleCount", moduleCount);
        data.put("agent7WorldLiveHostHooksVerified", agent7WorldLiveHostHooksVerified);
        data.put("agent7WorldLiveHostHookVerifiedCount", agent7WorldLiveHostVerifiedHookCount);
        data.put("agent7WorldLiveHostRequiredHookCount", agent7WorldLiveHostRequiredHookCount);
        data.put("agent7ExactLiveHookEvidenceSource", agent7ExactLiveHookEvidenceSource);
        data.put("gameplayHookVerifiedCount", verifiedHookCount);
        data.put("summary", complete
                ? "M23 verification completed; next work is controlled gameplay-hook instrumentation."
                : "M23 is blocked by missing prerequisite evidence.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> m24Readiness(
            String packId,
            boolean complete,
            boolean betaReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m24_readiness", diagnostics);
        data.put("phase13M24Ready", complete && !betaReady);
        data.put("ashfallPlayableBetaReady", betaReady);
        data.put("nextCommand", "phase13 instrument gameplay-hooks --authorized <fixture>");
        data.put("nextMilestone", "phase13.m24.controlled_gameplay_hook_instrumentation");
        data.put("summary", complete && !betaReady
                ? "M24 may add controlled native gameplay-hook instrumentation."
                : "M24 is blocked or unnecessary.");
        data.put("packId", packId);
        return data;
    }

    private static List<String> markerModules(Path markerPath) throws IOException {
        Map<String, Object> marker = markerFile(markerPath);
        Object modulesValue = marker.get("modules");
        if (!(modulesValue instanceof List<?> list)) {
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

    private static Map<String, Object> markerAgent7WorldLiveHostHookEvidence(Path markerPath) throws IOException {
        Map<String, Object> marker = markerFile(markerPath);
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(marker.get("runtimeBridge"));
        Map<String, Object> gameplayBridge = markerProductGameplayBridge(runtimeBridge);
        Map<String, Object> markerEvidence = EchoNativeJson.asObject(gameplayBridge.get("agent7WorldLiveHostHookEvidence"));
        if (agent7ExactWorldLiveHostHooksVerified(markerEvidence)) {
            return markerEvidence;
        }
        Map<String, Object> sidecarEvidence = agent7WorldLiveHostHookEvidenceFromSidecar(markerPath);
        return sidecarEvidence.isEmpty() ? markerEvidence : sidecarEvidence;
    }

    private static String markerAgent7ExactLiveHookEvidenceSource(Path markerPath) throws IOException {
        Map<String, Object> marker = markerFile(markerPath);
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(marker.get("runtimeBridge"));
        Map<String, Object> gameplayBridge = markerProductGameplayBridge(runtimeBridge);
        if (!agent7WorldLiveHostHookEvidenceFromSidecar(markerPath).isEmpty()) {
            return "EchoNativeAgent7LiveHookEvidenceBridge.directSidecar";
        }
        return String.valueOf(gameplayBridge.getOrDefault("agent7ExactLiveHookEvidenceSource", ""));
    }

    private static Map<String, Object> markerProductGameplayBridge(Map<String, Object> runtimeBridge) {
        Map<String, Object> bridge = EchoNativeJson.asObject(runtimeBridge.get("nativeProductGameplayBridge"));
        return bridge.isEmpty() ? EchoNativeJson.asObject(runtimeBridge.get("ashfallGameplayBridge")) : bridge;
    }

    private static Map<String, Object> agent7WorldLiveHostHookEvidenceFromSidecar(Path markerPath) throws IOException {
        Path sidecar = agent7SidecarPath(markerPath);
        if (!Files.isRegularFile(sidecar)) {
            return Map.of();
        }
        if (!Files.isRegularFile(markerPath)) {
            return Map.of();
        }
        long sidecarLastModifiedMillis = Files.getLastModifiedTime(sidecar).toMillis();
        long markerLastModifiedMillis = Files.getLastModifiedTime(markerPath).toMillis();
        boolean sidecarFreshForMarker = sidecarLastModifiedMillis >= markerLastModifiedMillis;
        if (!sidecarFreshForMarker) {
            return Map.of();
        }
        Map<String, Object> exactSnapshot = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(sidecar)));
        if (!"echo.agent7.native_exact_live_hook_evidence.v1".equals(String.valueOf(exactSnapshot.getOrDefault("schema", "")))
                || !Boolean.TRUE.equals(exactSnapshot.get("directPersistenceWritten"))) {
            return Map.of();
        }
        return agent7WorldLiveHostHookEvidenceFromExactSnapshot(
                exactSnapshot,
                "direct_sidecar",
                sidecarLastModifiedMillis,
                markerLastModifiedMillis
        );
    }

    private static Path agent7SidecarPath(Path markerPath) throws IOException {
        Map<String, Object> marker = markerFile(markerPath);
        Map<String, Object> runtimeBridge = EchoNativeJson.asObject(marker.get("runtimeBridge"));
        Map<String, Object> gameplayBridge = markerProductGameplayBridge(runtimeBridge);
        String configured = String.valueOf(gameplayBridge.getOrDefault(
                "agent7DirectLiveHookEvidencePath",
                marker.getOrDefault("agent7DirectLiveHookEvidencePath", "")
        ));
        if (!configured.isBlank()) {
            Path configuredPath = Path.of(configured);
            if (configuredPath.isAbsolute()) {
                return configuredPath.normalize();
            }
            Path parent = markerPath.toAbsolutePath().normalize().getParent();
            return parent == null ? configuredPath.toAbsolutePath().normalize() : parent.resolve(configuredPath).normalize();
        }
        Path parent = markerPath.toAbsolutePath().normalize().getParent();
        return parent == null
                ? Path.of("agent7-live-hook-evidence.json").toAbsolutePath().normalize()
                : parent.resolve("agent7-live-hook-evidence.json");
    }

    private static Map<String, Object> agent7WorldLiveHostHookEvidenceFromExactSnapshot(
            Map<String, Object> exactSnapshot,
            String source,
            long sidecarLastModifiedMillis,
            long markerLastModifiedMillis
    ) {
        int requiredCount = intValue(exactSnapshot, "requiredHookCount");
        int verifiedCount = intValue(exactSnapshot, "verifiedHookCount");
        List<Map<String, Object>> hooks = new ArrayList<>();
        for (Map<String, Object> exactHook : objectList(exactSnapshot.get("hooks"))) {
            boolean verified = Boolean.TRUE.equals(exactHook.get("liveGameplayHookVerified"));
            Map<String, Object> hook = new LinkedHashMap<>(exactHook);
            hook.put("candidateLiveRuntimeSignalObserved", verified);
            hook.put("blockedReason", verified ? "" : "exact_neoforge_callback_not_observed");
            hooks.add(hook);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", "echo.agent7.world_live_host_hook_evidence.v1");
        evidence.put("source", source);
        evidence.put("sourceSchema", exactSnapshot.getOrDefault("schema", ""));
        evidence.put("directPersistenceWritten", Boolean.TRUE.equals(exactSnapshot.get("directPersistenceWritten")));
        evidence.put("sidecarFreshForMarker", sidecarLastModifiedMillis >= markerLastModifiedMillis);
        evidence.put("sidecarLastModifiedMillis", sidecarLastModifiedMillis);
        evidence.put("markerLastModifiedMillis", markerLastModifiedMillis);
        evidence.put("minecraftRuntimeAccessed", hooks.stream()
                .anyMatch(hook -> Boolean.TRUE.equals(hook.get("minecraftRuntimeAccessed"))));
        evidence.put("requiredHookCount", requiredCount);
        evidence.put("candidateLiveSignalCount", verifiedCount);
        evidence.put("exactCallbackEvidenceCount", verifiedCount);
        evidence.put("verifiedHookCount", verifiedCount);
        evidence.put("allRequiredHooksVerified", requiredCount > 0 && verifiedCount == requiredCount);
        evidence.put("hooks", hooks);
        evidence.put("summary", requiredCount > 0 && verifiedCount == requiredCount
                ? "Agent 7 world/weather/hazard live host hooks were verified from direct exact callback sidecar evidence."
                : "Agent 7 direct exact callback sidecar evidence is incomplete.");
        return evidence;
    }

    private static Map<String, Object> markerFile(Path markerPath) throws IOException {
        if (!Files.isRegularFile(markerPath)) {
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(markerPath)));
    }

    private static boolean agent7ExactWorldLiveHostHooksVerified(Map<String, Object> evidence) {
        int requiredCount = intValue(evidence, "requiredHookCount");
        int verifiedCount = intValue(evidence, "verifiedHookCount");
        int exactCallbackEvidenceCount = intValue(evidence, "exactCallbackEvidenceCount");
        if (requiredCount <= 0
                || verifiedCount < requiredCount
                || exactCallbackEvidenceCount < requiredCount
                || !Boolean.TRUE.equals(evidence.get("allRequiredHooksVerified"))
                || !"echo.agent7.native_exact_live_hook_evidence.v1".equals(String.valueOf(evidence.getOrDefault("sourceSchema", "")))) {
            return false;
        }
        List<Map<String, Object>> hooks = objectList(evidence.get("hooks"));
        if (hooks.size() < requiredCount) {
            return false;
        }
        int exactVerifiedHooks = 0;
        for (Map<String, Object> hook : hooks) {
            boolean exactHookVerified = Boolean.TRUE.equals(hook.get("minecraftRuntimeAccessed"))
                    && Boolean.TRUE.equals(hook.get("liveGameplayHookVerified"))
                    && "exact_neoforge_callback_observed".equals(String.valueOf(hook.getOrDefault("evidenceMode", "")));
            if (exactHookVerified) {
                exactVerifiedHooks++;
            }
        }
        return exactVerifiedHooks >= requiredCount;
    }

    private static List<Map<String, Object>> objectList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> objects = new ArrayList<>();
        for (Object item : list) {
            objects.add(EchoNativeJson.asObject(item));
        }
        return List.copyOf(objects);
    }

    private static boolean moduleHasHookEvidence(String module, String latestLogText) {
        return latestLogText.contains("echo-native-hook:" + module)
                || latestLogText.contains("ECHO_NATIVE_HOOK " + module)
                || latestLogText.contains("[" + module + "] gameplay hook");
    }

    private static boolean containsAny(String text, List<String> needles) {
        return needles.stream().anyMatch(text::contains);
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
                    "ECHO-NATIVE-GAMEPLAY-HOOK-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook evidence required report missing",
                    "Gameplay hook verification requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Run the M22 activation command before gameplay hook verification."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
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
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-GAMEPLAY-HOOK-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Gameplay hook evidence upstream report is not PASS",
                    "Gameplay hook verification requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Resolve upstream M22 and baseline reports before gameplay hook verification."
            ));
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
        data.put("externalCommandExecuted", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
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
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
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
