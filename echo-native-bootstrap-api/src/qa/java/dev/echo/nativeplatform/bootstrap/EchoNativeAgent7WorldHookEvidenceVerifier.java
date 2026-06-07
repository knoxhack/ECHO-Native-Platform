package dev.echo.nativeplatform.bootstrap;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAgent7WorldHookEvidenceVerifier {
    private static final String[][] REQUIRED_HOOKS = {
            {"echoworldcore", "player_tick.post"},
            {"echoweathercore", "level_tick.post"},
            {"echoatmospherecore", "level_tick.post"},
            {"echobiomecore", "level_tick.post"},
            {"echostructurecore", "level_tick.post"},
            {"echospawncore", "finalize_spawn"},
            {"echodifficultycore", "server_starting"},
            {"echostatuscore", "server_starting"}
    };
    private static final String DIRECT_EVIDENCE_PATH_PROPERTY = "echo.agent7.liveHookEvidencePath";

    private EchoNativeAgent7WorldHookEvidenceVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> bridgeClass = Class.forName("com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge");
        bridgeClass.getMethod("resetForTest").invoke(null);
        Method record = bridgeClass.getMethod(
                "recordExactCallback",
                String.class,
                String.class,
                long.class,
                String.class
        );
        Path directEvidencePath = Files.createTempFile("agent7-direct-live-hook-evidence", ".json");
        System.setProperty(DIRECT_EVIDENCE_PATH_PROPERTY, directEvidencePath.toString());
        recordAllCallbacks(record, "agent7-world-hook-verifier");
        String directEvidenceText = Files.readString(directEvidencePath);
        require(directEvidenceText.contains("\"directPersistenceWritten\": true"),
                "Agent 7 exact hook bridge must persist direct callback evidence when configured.");
        require(directEvidenceText.contains("\"verifiedHookCount\": 8"),
                "Agent 7 direct callback evidence file must contain all verified hooks.");

        Method apply = EchoNativeBootstrapMain.class.getDeclaredMethod("applyExactAgent7WorldHookEvidence", Map.class);
        apply.setAccessible(true);
        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("firstPlayableLoopReady", false);
        existing.put("liveGameplayHandlersAttached", false);
        @SuppressWarnings("unchecked")
        Map<String, Object> merged = (Map<String, Object>) apply.invoke(null, existing);
        Map<String, Object> evidence = object(merged.get("agent7WorldLiveHostHookEvidence"));
        require(Boolean.TRUE.equals(evidence.get("allRequiredHooksVerified")),
                "Agent 7 exact hook evidence must verify all required hooks after callback records exist.");
        require(Integer.valueOf(REQUIRED_HOOKS.length).equals(evidence.get("verifiedHookCount")),
                "Agent 7 exact hook evidence must count all required hooks.");
        require(Boolean.FALSE.equals(merged.get("firstPlayableLoopReady")),
                "Agent 7 exact hook evidence must not flip Ashfall first playable loop readiness by itself.");
        require(Boolean.FALSE.equals(merged.get("liveGameplayHandlersAttached")),
                "Agent 7 exact hook evidence must not flip global Ashfall gameplay handler readiness by itself.");

        bridgeClass.getMethod("resetForTest").invoke(null);
        Path directSidecarMarkerPath = Files.createTempFile("agent7-direct-sidecar-marker", ".json");
        Files.writeString(directSidecarMarkerPath, "{\"schema\":\"marker\"}\n");
        Files.setLastModifiedTime(directSidecarMarkerPath, FileTime.fromMillis(1_700_000_010_000L));
        recordAllCallbacks(record, "agent7-world-hook-direct-sidecar-verifier");
        Files.setLastModifiedTime(directEvidencePath, FileTime.fromMillis(1_700_000_020_000L));
        bridgeClass.getMethod("resetForTest").invoke(null);
        Method applyWithMarker = EchoNativeBootstrapMain.class.getDeclaredMethod(
                "applyExactAgent7WorldHookEvidence",
                Map.class,
                Path.class
        );
        applyWithMarker.setAccessible(true);
        Map<String, Object> sidecarExisting = new LinkedHashMap<>();
        sidecarExisting.put("firstPlayableLoopReady", false);
        sidecarExisting.put("liveGameplayHandlersAttached", false);
        @SuppressWarnings("unchecked")
        Map<String, Object> sidecarMerged = (Map<String, Object>) applyWithMarker.invoke(
                null,
                sidecarExisting,
                directSidecarMarkerPath
        );
        Map<String, Object> sidecarEvidence = object(sidecarMerged.get("agent7WorldLiveHostHookEvidence"));
        require(Boolean.TRUE.equals(sidecarEvidence.get("allRequiredHooksVerified")),
                "Agent 7 bootstrap must recover exact hooks from the fresh direct evidence file.");
        require(Boolean.TRUE.equals(sidecarEvidence.get("directPersistenceWritten")),
                "Agent 7 bootstrap must expose direct evidence persistence.");
        require(Boolean.TRUE.equals(sidecarEvidence.get("directEvidenceFileFreshForMarker")),
                "Agent 7 bootstrap must require a fresh direct evidence file for marker-bound hook verification.");
        require(Boolean.FALSE.equals(sidecarMerged.get("firstPlayableLoopReady")),
                "Agent 7 direct sidecar evidence must not flip Ashfall first playable loop readiness by itself.");

        bridgeClass.getMethod("resetForTest").invoke(null);
        Path markerPath = Files.createTempFile("agent7-live-hook-snapshot", ".json");
        Map<String, Object> runtimeBridge = new LinkedHashMap<>();
        Map<String, Object> ashfallBridge = new LinkedHashMap<>();
        ashfallBridge.put("firstPlayableLoopReady", false);
        ashfallBridge.put("liveGameplayHandlersAttached", false);
        ashfallBridge.put("agent7WorldLiveHostHookEvidence", Map.of("verifiedHookCount", 0));
        runtimeBridge.put("ashfallGameplayBridge", ashfallBridge);
        System.setProperty("echo.agent7.liveHookSnapshotMaxPolls", "80");
        System.setProperty("echo.agent7.liveHookSnapshotPollMillis", "10");
        Method startSnapshotBridge = EchoNativeBootstrapMain.class.getDeclaredMethod(
                "startAgent7WorldLiveHookEvidenceSnapshotBridge",
                Path.class,
                String.class,
                String.class,
                List.class,
                Map.class,
                Map.class,
                Map.class
        );
        startSnapshotBridge.setAccessible(true);
        startSnapshotBridge.invoke(
                null,
                markerPath,
                "ashfall",
                "agent7.snapshot.verifier",
                List.of("echoworldcore", "echoweathercore", "echoatmospherecore", "echobiomecore",
                        "echostructurecore", "echospawncore", "echodifficultycore", "echostatuscore"),
                Map.of(),
                runtimeBridge,
                Map.of()
        );
        recordAllCallbacks(record, "agent7-world-hook-snapshot-verifier");
        String markerText = waitForSnapshot(markerPath);
        require(markerText.contains("\"agent7WorldLiveHostHooksVerified\": true"),
                "Agent 7 snapshot bridge must persist all-required hook verification to the activation marker.");
        require(markerText.contains("\"agent7WorldLiveHostHookVerifiedCount\": 8"),
                "Agent 7 snapshot bridge must persist the verified hook count.");
        require(markerText.contains("\"agent7LiveHookSnapshotBridgeActive\": true"),
                "Agent 7 snapshot bridge must mark the live-hook snapshot bridge active.");
        require(markerText.contains("\"nativeLiveGameplayHandlersAttached\": false"),
                "Agent 7 snapshot bridge must not flip global live gameplay handler readiness.");
        Files.deleteIfExists(markerPath);
        Files.deleteIfExists(directSidecarMarkerPath);
        Files.deleteIfExists(directEvidencePath);
        System.clearProperty("echo.agent7.liveHookSnapshotMaxPolls");
        System.clearProperty("echo.agent7.liveHookSnapshotPollMillis");
        System.clearProperty(DIRECT_EVIDENCE_PATH_PROPERTY);
        System.out.println("agent7 world hook evidence verifier PASS verifiedHooks=8 globalLiveClaim=false snapshotPersisted=true directPersistenceWritten=true directSidecarRecovered=true");
    }

    private static void recordAllCallbacks(Method record, String sourceReason) throws Exception {
        for (int index = 0; index < REQUIRED_HOOKS.length; index++) {
            record.invoke(
                    null,
                    REQUIRED_HOOKS[index][0],
                    REQUIRED_HOOKS[index][1],
                    9200L + index,
                    sourceReason
            );
        }
    }

    private static String waitForSnapshot(Path markerPath) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (Files.isRegularFile(markerPath)) {
                String text = Files.readString(markerPath);
                if (text.contains("\"agent7WorldLiveHostHookVerifiedCount\": 8")) {
                    return text;
                }
            }
            Thread.sleep(10L);
        }
        return Files.isRegularFile(markerPath) ? Files.readString(markerPath) : "";
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
