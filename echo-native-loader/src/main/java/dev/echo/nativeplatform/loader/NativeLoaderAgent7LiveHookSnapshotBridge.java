package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderAgent7LiveHookSnapshotBridge {
    public static final String SERVICE_ID = "echo.native.agent7_live_hook_snapshot_bridge";

    private NativeLoaderAgent7LiveHookSnapshotBridge() {
    }

    public static void start(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations,
            String productGameplayBridgeKey,
            String forceProperty,
            String maxPollsProperty,
            String pollMillisProperty,
            ExactSnapshotReader snapshotReader,
            ExactEvidenceMapper evidenceMapper,
            ProductBridgeUpdater productBridgeUpdater,
            MarkerSnapshotWriter snapshotWriter
    ) {
        if (realMainClass.isBlank() && !Boolean.parseBoolean(System.getProperty(forceProperty, "false"))) {
            return;
        }
        Thread thread = new Thread(() -> {
            int lastVerifiedCount = integer(object(object(runtimeBridge.get(productGameplayBridgeKey))
                    .get("agent7WorldLiveHostHookEvidence")).get("verifiedHookCount"));
            int maxPolls = positiveIntProperty(maxPollsProperty, 1200);
            long pollMillis = positiveLongProperty(pollMillisProperty, 250L);
            for (int attempt = 0; attempt < maxPolls; attempt++) {
                try {
                    Map<String, Object> exactSnapshot = snapshotReader.read();
                    if (!exactSnapshot.isEmpty()) {
                        Map<String, Object> exactEvidence = evidenceMapper.map(exactSnapshot);
                        int verifiedCount = integer(exactEvidence.get("verifiedHookCount"));
                        if (verifiedCount > lastVerifiedCount
                                || Boolean.TRUE.equals(exactEvidence.get("allRequiredHooksVerified"))) {
                            Map<String, Object> productBridge = productBridgeUpdater.apply(
                                    object(runtimeBridge.get(productGameplayBridgeKey)),
                                    markerPath
                            );
                            productBridge.put("agent7LiveHookSnapshotBridgeServiceId", SERVICE_ID);
                            productBridge.put("agent7LiveHookSnapshotBridgeActive", true);
                            productBridge.put("agent7LiveHookSnapshotLastAttempt", attempt);
                            productBridge.put("agent7LiveHookSnapshotLastVerifiedCount", verifiedCount);
                            runtimeBridge.put(productGameplayBridgeKey, productBridge);
                            snapshotWriter.write(
                                    markerPath,
                                    packId,
                                    realMainClass,
                                    modules,
                                    nativeEntrypoints,
                                    runtimeBridge,
                                    nativeActivations
                            );
                            lastVerifiedCount = verifiedCount;
                            if (Boolean.TRUE.equals(exactEvidence.get("allRequiredHooksVerified"))) {
                                return;
                            }
                        }
                    }
                    Thread.sleep(pollMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    recordFailure(
                            runtimeBridge,
                            markerPath,
                            packId,
                            realMainClass,
                            modules,
                            nativeEntrypoints,
                            nativeActivations,
                            productGameplayBridgeKey,
                            snapshotWriter,
                            "InterruptedException",
                            "Agent 7 live-hook evidence snapshot bridge was interrupted."
                    );
                    return;
                } catch (Throwable exception) {
                    recordFailure(
                            runtimeBridge,
                            markerPath,
                            packId,
                            realMainClass,
                            modules,
                            nativeEntrypoints,
                            nativeActivations,
                            productGameplayBridgeKey,
                            snapshotWriter,
                            exception.getClass().getSimpleName(),
                            failureMessage(exception)
                    );
                    return;
                }
            }
            recordFailure(
                    runtimeBridge,
                    markerPath,
                    packId,
                    realMainClass,
                    modules,
                    nativeEntrypoints,
                    nativeActivations,
                    productGameplayBridgeKey,
                    snapshotWriter,
                    "Timeout",
                    "Agent 7 exact live-hook callbacks were not fully observed before the snapshot bridge timed out."
            );
        }, "NativeLoaderAgent7LiveHookEvidenceSnapshotBridge");
        thread.setDaemon(true);
        thread.start();
    }

    private static void recordFailure(
            Map<String, Object> runtimeBridge,
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Map<String, Object>> nativeActivations,
            String productGameplayBridgeKey,
            MarkerSnapshotWriter snapshotWriter,
            String failureKind,
            String failureMessage
    ) {
        Map<String, Object> productBridge = new LinkedHashMap<>(object(runtimeBridge.get(productGameplayBridgeKey)));
        productBridge.put("agent7LiveHookSnapshotBridgeServiceId", SERVICE_ID);
        productBridge.put("agent7LiveHookSnapshotBridgeActive", false);
        productBridge.put("agent7LiveHookSnapshotFailureKind", failureKind);
        productBridge.put("agent7LiveHookSnapshotFailureMessage", failureMessage);
        runtimeBridge.put(productGameplayBridgeKey, productBridge);
        try {
            snapshotWriter.write(markerPath, packId, realMainClass, modules, nativeEntrypoints, runtimeBridge, nativeActivations);
        } catch (IOException ignored) {
            // The existing marker remains the last durable evidence if the failure snapshot cannot be written.
        }
    }

    private static int positiveIntProperty(String key, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(System.getProperty(key, String.valueOf(fallback))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long positiveLongProperty(String key, long fallback) {
        try {
            return Math.max(1L, Long.parseLong(System.getProperty(key, String.valueOf(fallback))));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
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

    @FunctionalInterface
    public interface ExactSnapshotReader {
        Map<String, Object> read();
    }

    @FunctionalInterface
    public interface ExactEvidenceMapper {
        Map<String, Object> map(Map<String, Object> exactSnapshot);
    }

    @FunctionalInterface
    public interface ProductBridgeUpdater {
        Map<String, Object> apply(Map<String, Object> productBridge, Path markerPath);
    }

    @FunctionalInterface
    public interface MarkerSnapshotWriter {
        void write(
                Path markerPath,
                String packId,
                String realMainClass,
                List<String> modules,
                Map<String, String> nativeEntrypoints,
                Map<String, Object> runtimeBridge,
                Map<String, Map<String, Object>> nativeActivations
        ) throws IOException;
    }
}
