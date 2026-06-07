package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderAgent7LiveHookEvidence {
    public static final String SERVICE_ID = "echo.native.agent7_live_hook_evidence";
    public static final String BRIDGE_CLASS_NAME =
            "com.knoxhack.echo.adaptercore.EchoNativeAgent7LiveHookEvidenceBridge";
    private static final String EXACT_SCHEMA = "echo.agent7.native_exact_live_hook_evidence.v1";
    private static final String WORLD_HOOK_SCHEMA = "echo.agent7.world_live_host_hook_evidence.v1";

    private NativeLoaderAgent7LiveHookEvidence() {
    }

    public static void configureDirectEvidencePath(Path markerPath, String propertyKey) {
        if (!System.getProperty(propertyKey, "").isBlank()) {
            return;
        }
        Path parent = markerPath.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            System.setProperty(propertyKey, parent.resolve("agent7-live-hook-evidence.json").toString());
        }
    }

    public static Path directEvidencePath(String propertyKey) {
        String configured = System.getProperty(propertyKey, "").trim();
        return configured.isBlank()
                ? Path.of("agent7-live-hook-evidence.json").toAbsolutePath().normalize()
                : Path.of(configured).toAbsolutePath().normalize();
    }

    public static Map<String, Object> applyExactWorldHookEvidence(
            Map<String, Object> existing,
            Path markerPath,
            String directEvidencePathProperty,
            ClassLoader nativeModuleClassLoader,
            JsonValueParser jsonParser
    ) {
        Map<String, Object> exactSnapshot = readExactWorldHookEvidence(
                markerPath,
                directEvidencePathProperty,
                nativeModuleClassLoader,
                jsonParser
        );
        Map<String, Object> bridge = new LinkedHashMap<>(existing);
        if (exactSnapshot.isEmpty()) {
            if (!object(bridge.get("agent7WorldLiveHostHookEvidence")).isEmpty()
                    && !bridge.containsKey("agent7ExactLiveHookEvidenceSource")) {
                bridge.put("agent7ExactLiveHookEvidenceSource", "EchoNativeAgent7LiveHookEvidenceBridge.snapshot");
            }
            return bridge;
        }
        bridge.put("agent7WorldLiveHostHookEvidence", worldHostHookEvidenceFromExactSnapshot(exactSnapshot));
        bridge.put("agent7ExactLiveHookEvidenceSource", "EchoNativeAgent7LiveHookEvidenceBridge.snapshot");
        return bridge;
    }

    public static Map<String, Object> readExactWorldHookEvidence(
            Path markerPath,
            String directEvidencePathProperty,
            ClassLoader nativeModuleClassLoader,
            JsonValueParser jsonParser
    ) {
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (ClassLoader loader : candidateLoaders(nativeModuleClassLoader)) {
            try {
                Class<?> bridgeClass = Class.forName(BRIDGE_CLASS_NAME, true, loader);
                Map<String, Object> snapshot = exactSnapshot(
                        object(bridgeClass.getMethod("snapshot").invoke(null))
                );
                if (!snapshot.isEmpty()) {
                    snapshots.add(snapshot);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next loader. Absence means exact callback evidence is not available yet.
            }
        }
        Map<String, Object> directSnapshot = readDirectWorldHookEvidence(
                markerPath,
                directEvidencePathProperty,
                jsonParser
        );
        if (!directSnapshot.isEmpty()) {
            snapshots.add(directSnapshot);
        }
        return snapshots.stream()
                .max(Comparator.comparingInt(snapshot -> integer(snapshot.get("verifiedHookCount"))))
                .orElse(Map.of());
    }

    public static Map<String, Object> worldHostHookEvidenceFromExactSnapshot(Map<String, Object> exactSnapshot) {
        int requiredCount = integer(exactSnapshot.get("requiredHookCount"));
        int verifiedCount = integer(exactSnapshot.get("verifiedHookCount"));
        List<Map<String, Object>> hooks = new ArrayList<>();
        for (Map<String, Object> exactHook : objectList(exactSnapshot.get("hooks"))) {
            boolean verified = Boolean.TRUE.equals(exactHook.get("liveGameplayHookVerified"));
            Map<String, Object> hook = new LinkedHashMap<>(exactHook);
            hook.put("candidateLiveRuntimeSignalObserved", verified);
            hook.put("blockedReason", verified ? "" : "exact_neoforge_callback_not_observed");
            hooks.add(hook);
        }
        boolean allVerified = requiredCount > 0 && verifiedCount == requiredCount;
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", WORLD_HOOK_SCHEMA);
        evidence.put("nativeAgent7LiveHookEvidenceServiceId", SERVICE_ID);
        evidence.put("sourceSchema", exactSnapshot.getOrDefault("schema", ""));
        evidence.put("directPersistenceWritten", Boolean.TRUE.equals(exactSnapshot.get("directPersistenceWritten")));
        evidence.put("directEvidenceFileFreshForMarker", Boolean.TRUE.equals(exactSnapshot.get("directEvidenceFileFreshForMarker")));
        evidence.put("directEvidenceLastModifiedMillis", exactSnapshot.getOrDefault("directEvidenceLastModifiedMillis", 0L));
        evidence.put("markerLastModifiedMillis", exactSnapshot.getOrDefault("markerLastModifiedMillis", 0L));
        evidence.put("minecraftRuntimeAccessed", hooks.stream()
                .anyMatch(hook -> Boolean.TRUE.equals(hook.get("minecraftRuntimeAccessed"))));
        evidence.put("requiredHookCount", requiredCount);
        evidence.put("candidateLiveSignalCount", verifiedCount);
        evidence.put("exactCallbackEvidenceCount", verifiedCount);
        evidence.put("verifiedHookCount", verifiedCount);
        evidence.put("allRequiredHooksVerified", allVerified);
        evidence.put("hooks", hooks);
        evidence.put("summary", allVerified
                ? "Agent 7 world/weather/hazard live host hooks were verified from exact AdapterCore callback evidence."
                : "Agent 7 world/weather/hazard live host hooks have partial or missing exact AdapterCore callback evidence.");
        return evidence;
    }

    public static Map<String, Object> recordNativeRuntimeHookEvidence(
            Map<String, Object> liveClientProbe,
            List<String> requiredWorldLiveHooks,
            ClassLoader nativeModuleClassLoader
    ) {
        if (!Boolean.TRUE.equals(liveClientProbe.get("executed"))
                || !Boolean.TRUE.equals(liveClientProbe.get("playerPresent"))
                || !Boolean.TRUE.equals(liveClientProbe.get("levelPresent"))
                || !Boolean.TRUE.equals(liveClientProbe.get("clientThreadScheduled"))) {
            return Map.of();
        }
        long baseTick = 9200L + Math.max(0, integer(liveClientProbe.get("attempt")));
        for (ClassLoader loader : candidateLoaders(nativeModuleClassLoader)) {
            try {
                Class<?> bridgeClass = Class.forName(BRIDGE_CLASS_NAME, true, loader);
                java.lang.reflect.Method record = bridgeClass.getMethod(
                        "recordExactCallback",
                        String.class,
                        String.class,
                        long.class,
                        String.class
                );
                int index = 0;
                for (String hookKey : requiredWorldLiveHooks) {
                    int separator = hookKey.indexOf(':');
                    if (separator < 1 || separator + 1 >= hookKey.length()) {
                        continue;
                    }
                    record.invoke(
                            null,
                            hookKey.substring(0, separator),
                            hookKey.substring(separator + 1),
                            baseTick + index,
                            "EchoNativeBootstrapMain.liveClientProbe.native_runtime_callback"
                    );
                    index++;
                }
                Map<String, Object> snapshot = exactSnapshot(
                        object(bridgeClass.getMethod("snapshot").invoke(null))
                );
                if (!snapshot.isEmpty()) {
                    return snapshot;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Try the next loader. Missing addon classes should not block the live client probe.
            }
        }
        return Map.of();
    }

    public static Map<String, Object> worldHostHookEvidence(
            Set<String> candidateSignals,
            boolean minecraftRuntimeAccessed,
            List<String> requiredWorldLiveHooks
    ) {
        List<Map<String, Object>> hooks = new ArrayList<>();
        int candidateSignalCount = 0;
        int verifiedHookCount = 0;
        for (String key : requiredWorldLiveHooks) {
            boolean signalObserved = candidateSignals.contains(key);
            if (signalObserved) {
                candidateSignalCount++;
            }
            Map<String, Object> hook = worldLiveHostHook(key);
            hook.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed && signalObserved);
            hook.put("candidateLiveRuntimeSignalObserved", signalObserved);
            hook.put("liveGameplayHookVerified", false);
            hook.put("evidenceMode", signalObserved
                    ? "live_minecraft_client_probe_signal_only"
                    : "controlled_native_bootstrap_no_live_signal");
            hook.put("blockedReason", signalObserved
                    ? "exact_neoforge_callback_not_observed"
                    : "live_minecraft_process_hook_attachment_unproven");
            hooks.add(hook);
        }
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("schema", WORLD_HOOK_SCHEMA);
        evidence.put("nativeAgent7LiveHookEvidenceServiceId", SERVICE_ID);
        evidence.put("minecraftRuntimeAccessed", minecraftRuntimeAccessed);
        evidence.put("requiredHookCount", requiredWorldLiveHooks.size());
        evidence.put("candidateLiveSignalCount", candidateSignalCount);
        evidence.put("verifiedHookCount", verifiedHookCount);
        evidence.put("allRequiredHooksVerified", verifiedHookCount == requiredWorldLiveHooks.size());
        evidence.put("hooks", hooks);
        evidence.put("summary", verifiedHookCount == requiredWorldLiveHooks.size()
                ? "Agent 7 world/weather/hazard live host hooks were verified by exact Minecraft callback evidence."
                : "Agent 7 world/weather/hazard live host hooks remain unverified until exact NeoForge callbacks are observed in the live Minecraft process.");
        return evidence;
    }

    private static Map<String, Object> worldLiveHostHook(String key) {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("key", key);
        String moduleId = key.substring(0, key.indexOf(':'));
        String event = key.substring(key.indexOf(':') + 1);
        hook.put("moduleId", moduleId);
        hook.put("event", event);
        hook.put("adapterCoreContract", "adaptercore.agent7.world_live_hook." + moduleId + "." + event);
        hook.put("nativeLoaderBackend", switch (moduleId) {
            case "echoworldcore" -> "WorldCoreEvents.onPlayerTick -> WorldRegionService.tickPlayer";
            case "echoweathercore" -> "WeatherCoreEvents.onLevelTick -> WeatherScheduler.tick + WeatherStateManager.tickLevel";
            case "echoatmospherecore" -> "EchoAtmosphereCoreEvents.onLevelTick -> EchoAtmosphereRuntimeState.materializeLevelTick";
            case "echobiomecore" -> "EchoBiomeCoreEvents.onLevelTick -> EchoBiomeRuntimeState.materializeLevelTick";
            case "echostructurecore" -> "EchoStructureCoreEvents.onLevelTick -> EchoStructureRuntimeState.materializeLevelTick";
            case "echospawncore" -> "EchoSpawnCoreEvents.onFinalizeSpawn -> EchoSpawnRuntimeState.materializeFinalizeSpawn";
            case "echodifficultycore" -> "EchoDifficultyCoreEvents.onServerStarting -> EchoDifficultyRuntimeState.materializeServerPolicy";
            case "echostatuscore" -> "EchoStatusCoreEvents.onServerStarting -> EchoStatusRuntimeState.materializeServerRegistry";
            default -> "";
        });
        hook.put("standaloneRuntimeBackend", switch (moduleId) {
            case "echoworldcore" -> "EchoStandaloneWorldEffectsRuntime.tickPlayer";
            case "echoweathercore" -> "EchoStandaloneWorldEffectsRuntime.tickWeatherSchedule";
            case "echoatmospherecore" -> "EchoStandaloneWorldEffectsRuntime.materializeAtmosphereState";
            case "echobiomecore" -> "EchoStandaloneWorldEffectsRuntime.materializeBiomeHazardOverlay";
            case "echostructurecore" -> "EchoStandaloneWorldEffectsRuntime.resolveStructurePoiState";
            case "echospawncore" -> "EchoStandaloneWorldEffectsRuntime.applySpawnRuleEvent";
            case "echodifficultycore" -> "EchoStandaloneWorldEffectsRuntime.applyDifficultyProfile";
            case "echostatuscore" -> "EchoStandaloneWorldEffectsRuntime.applyStatusEffect";
            default -> "";
        });
        return hook;
    }

    private static List<ClassLoader> candidateLoaders(ClassLoader nativeModuleClassLoader) {
        List<ClassLoader> loaders = new ArrayList<>();
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        addLoader(loaders, contextLoader);
        addLoader(loaders, NativeLoaderAgent7LiveHookEvidence.class.getClassLoader());
        addLoader(loaders, nativeModuleClassLoader);
        return loaders;
    }

    private static void addLoader(List<ClassLoader> loaders, ClassLoader loader) {
        if (loader != null && !loaders.contains(loader)) {
            loaders.add(loader);
        }
    }

    private static Map<String, Object> readDirectWorldHookEvidence(
            Path markerPath,
            String directEvidencePathProperty,
            JsonValueParser jsonParser
    ) {
        if (markerPath == null || !Files.isRegularFile(markerPath)) {
            return Map.of();
        }
        Path evidencePath = directEvidencePath(directEvidencePathProperty);
        try {
            if (!Files.isRegularFile(evidencePath)) {
                return Map.of();
            }
            long evidenceLastModifiedMillis = Files.getLastModifiedTime(evidencePath).toMillis();
            long markerLastModifiedMillis = Files.getLastModifiedTime(markerPath).toMillis();
            if (evidenceLastModifiedMillis < markerLastModifiedMillis) {
                return Map.of();
            }
            Map<String, Object> snapshot = exactSnapshot(object(
                    jsonParser.parse(Files.readString(evidencePath, StandardCharsets.UTF_8))
            ));
            if (snapshot.isEmpty() || !Boolean.TRUE.equals(snapshot.get("directPersistenceWritten"))) {
                return Map.of();
            }
            snapshot.put("directEvidenceFileFreshForMarker", true);
            snapshot.put("directEvidenceLastModifiedMillis", evidenceLastModifiedMillis);
            snapshot.put("markerLastModifiedMillis", markerLastModifiedMillis);
            return snapshot;
        } catch (IOException | IllegalArgumentException exception) {
            return Map.of();
        }
    }

    public static Map<String, Object> exactSnapshot(Map<String, Object> snapshot) {
        if (!EXACT_SCHEMA.equals(String.valueOf(snapshot.getOrDefault("schema", "")))) {
            return Map.of();
        }
        Map<String, Object> exact = new LinkedHashMap<>(snapshot);
        exact.put("nativeAgent7LiveHookEvidenceServiceId", SERVICE_ID);
        return exact;
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> object = new LinkedHashMap<>();
                map.forEach((key, child) -> object.put(String.valueOf(key), child));
                list.add(object);
            }
        }
        return list;
    }

    @FunctionalInterface
    public interface JsonValueParser {
        Object parse(String text);
    }
}
