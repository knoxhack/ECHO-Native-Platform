package dev.echo.nativeplatform.cli;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;

public final class EchoNativeAgent7SidecarEvidenceVerifier {
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

    private EchoNativeAgent7SidecarEvidenceVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("agent7-sidecar-cli");
        Path marker = directory.resolve("module-activation.json");
        Path sidecar = directory.resolve("custom-agent7-live-hook-evidence.json");
        Files.writeString(marker, staleMarkerJson(sidecar));
        Files.writeString(sidecar, sidecarJson());
        Files.setLastModifiedTime(sidecar, FileTime.fromMillis(1_700_000_000_000L));
        Files.setLastModifiedTime(marker, FileTime.fromMillis(1_700_000_010_000L));

        Method evidenceReader = EchoNativeGameplayHookEvidenceVerifier.class.getDeclaredMethod(
                "markerAgent7WorldLiveHostHookEvidence",
                Path.class
        );
        evidenceReader.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> staleEvidence = (Map<String, Object>) evidenceReader.invoke(null, marker);
        require(!Boolean.TRUE.equals(staleEvidence.get("allRequiredHooksVerified")),
                "GameplayHookEvidenceVerifier must reject stale direct Agent 7 sidecar evidence.");

        Method sourceReader = EchoNativeGameplayHookEvidenceVerifier.class.getDeclaredMethod(
                "markerAgent7ExactLiveHookEvidenceSource",
                Path.class
        );
        sourceReader.setAccessible(true);
        require(!"EchoNativeAgent7LiveHookEvidenceBridge.directSidecar".equals(sourceReader.invoke(null, marker)),
                "GameplayHookEvidenceVerifier must not report stale direct sidecar evidence as the source.");

        Files.setLastModifiedTime(sidecar, FileTime.fromMillis(1_700_000_020_000L));

        @SuppressWarnings("unchecked")
        Map<String, Object> evidence = (Map<String, Object>) evidenceReader.invoke(null, marker);
        require(Boolean.TRUE.equals(evidence.get("allRequiredHooksVerified")),
                "GameplayHookEvidenceVerifier must accept fully verified direct Agent 7 sidecar evidence.");
        require(Integer.valueOf(REQUIRED_HOOKS.length).equals(evidence.get("verifiedHookCount")),
                "GameplayHookEvidenceVerifier must preserve the sidecar verified hook count.");
        require(Boolean.TRUE.equals(evidence.get("directPersistenceWritten")),
                "GameplayHookEvidenceVerifier must expose direct sidecar persistence.");
        require(Boolean.TRUE.equals(evidence.get("sidecarFreshForMarker")),
                "GameplayHookEvidenceVerifier must expose sidecar freshness for marker-bound evidence.");
        require("EchoNativeAgent7LiveHookEvidenceBridge.directSidecar".equals(sourceReader.invoke(null, marker)),
                "GameplayHookEvidenceVerifier must report the direct sidecar source.");

        Method gameplaySidecarReader = EchoNativeGameplayHookVerifier.class.getDeclaredMethod(
                "agent7WorldLiveHostHookEvidenceFromSidecar",
                Path.class
        );
        gameplaySidecarReader.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> gameplayEvidence = (Map<String, Object>) gameplaySidecarReader.invoke(null, marker);
        require(Boolean.TRUE.equals(gameplayEvidence.get("sidecarFreshForMarker")),
                "GameplayHookVerifier must expose sidecar freshness for marker-bound evidence.");
        Method exactVerifier = EchoNativeGameplayHookVerifier.class.getDeclaredMethod(
                "agent7ExactWorldLiveHostHooksVerified",
                Map.class
        );
        exactVerifier.setAccessible(true);
        require(Boolean.TRUE.equals(exactVerifier.invoke(null, gameplayEvidence)),
                "GameplayHookVerifier must gate the direct sidecar on exact Agent 7 callback evidence.");

        Files.deleteIfExists(sidecar);
        Files.deleteIfExists(marker);
        Files.deleteIfExists(directory);
        System.out.println("agent7 sidecar evidence verifier PASS sidecarVerifiedHooks=8 staleSidecarRejected=true explicitMarkerPath=true globalLiveClaim=false");
    }

    private static String staleMarkerJson(Path sidecar) {
        String sidecarPath = sidecar.toAbsolutePath().normalize().toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return """
                {
                  "agent7DirectLiveHookEvidencePath": "__SIDECAR__",
                  "runtimeBridge": {
                    "ashfallGameplayBridge": {
                      "agent7DirectLiveHookEvidencePath": "__SIDECAR__",
                      "agent7ExactLiveHookEvidenceSource": "EchoNativeAgent7LiveHookEvidenceBridge.snapshot",
                      "agent7WorldLiveHostHookEvidence": {
                        "schema": "echo.agent7.world_live_host_hook_evidence.v1",
                        "sourceSchema": "echo.agent7.native_exact_live_hook_evidence.v1",
                        "requiredHookCount": 8,
                        "verifiedHookCount": 0,
                        "exactCallbackEvidenceCount": 0,
                        "allRequiredHooksVerified": false,
                        "hooks": []
                      }
                    }
                  }
                }
                """.replace("__SIDECAR__", sidecarPath);
    }

    private static String sidecarJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"schema\": \"echo.agent7.native_exact_live_hook_evidence.v1\",\n");
        builder.append("  \"requiredHookCount\": 8,\n");
        builder.append("  \"verifiedHookCount\": 8,\n");
        builder.append("  \"allRequiredHooksVerified\": true,\n");
        builder.append("  \"directPersistenceWritten\": true,\n");
        builder.append("  \"hooks\": [\n");
        for (int index = 0; index < REQUIRED_HOOKS.length; index++) {
            String moduleId = REQUIRED_HOOKS[index][0];
            String event = REQUIRED_HOOKS[index][1];
            builder.append("    {\n");
            builder.append("      \"moduleId\": \"").append(moduleId).append("\",\n");
            builder.append("      \"event\": \"").append(event).append("\",\n");
            builder.append("      \"key\": \"").append(moduleId).append(':').append(event).append("\",\n");
            builder.append("      \"gameTick\": ").append(9200 + index).append(",\n");
            builder.append("      \"sourceReason\": \"agent7-sidecar-cli-verifier\",\n");
            builder.append("      \"minecraftRuntimeAccessed\": true,\n");
            builder.append("      \"liveGameplayHookVerified\": true,\n");
            builder.append("      \"evidenceMode\": \"exact_neoforge_callback_observed\"\n");
            builder.append("    }");
            if (index + 1 < REQUIRED_HOOKS.length) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
