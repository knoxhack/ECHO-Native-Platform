package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeLoaderLiveClientProbeBridge {
    private static final int MAX_ATTEMPTS = 28800;
    private static final long POLL_MILLIS = 250L;
    private static final int REPORT_EVERY_ATTEMPTS = 40;
    private static final Set<Path> ACTIVE_MARKERS = ConcurrentHashMap.newKeySet();

    private NativeLoaderLiveClientProbeBridge() {
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
            ProbeExecutor probeExecutor,
            ProbeFactory probeFactory,
            ProductGameplayEvidenceUpdater productGameplayEvidenceUpdater,
            ProbeWriter probeWriter,
            MarkerSnapshotWriter snapshotWriter
    ) {
        Path markerKey = markerPath.toAbsolutePath().normalize();
        if (!ACTIVE_MARKERS.add(markerKey)) {
            Map<String, Object> probe = new LinkedHashMap<>(object(runtimeBridge.get("liveClientProbe")));
            probe.put("liveClientProbeStartSkipped", true);
            probe.put("liveClientProbeStartSkippedReason", "already_running_for_marker");
            runtimeBridge.put("liveClientProbe", probe);
            return;
        }
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                try {
                    Map<String, Object> probe = probeExecutor.apply(markerPath, attempt);
                    if (Boolean.TRUE.equals(probe.get("executed"))) {
                        runtimeBridge.put("liveClientProbe", probe);
                        Map<String, Object> liveProductGameplayBridge = productGameplayEvidenceUpdater.apply(
                                object(runtimeBridge.get(productGameplayBridgeKey)),
                                probe
                        );
                        runtimeBridge.put(productGameplayBridgeKey, liveProductGameplayBridge);
                        snapshotWriter.write(
                                markerPath,
                                packId,
                                realMainClass,
                                modules,
                                nativeEntrypoints,
                                runtimeBridge,
                                nativeActivations
                        );
                        if (releaseReadyProbe(probe)
                                || mutatingPlayableRuntimeAttempted(probe)
                                || attempt + 1 >= MAX_ATTEMPTS) {
                            return;
                        }
                        Thread.sleep(POLL_MILLIS);
                        continue;
                    }
                    if (shouldReportAttempt(attempt)) {
                        runtimeBridge.put("liveClientProbe", probe);
                        probeWriter.write(markerPath, probe);
                    }
                    Thread.sleep(POLL_MILLIS);
                } catch (ClassNotFoundException exception) {
                    recordPendingMinecraftClass(markerPath, runtimeBridge, attempt, exception, probeFactory, probeWriter);
                    try {
                        Thread.sleep(POLL_MILLIS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        recordFailure(
                                runtimeBridge,
                                markerPath,
                                "InterruptedException",
                                "Live client probe was interrupted.",
                                probeFactory,
                                probeWriter
                        );
                        return;
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    recordFailure(
                            runtimeBridge,
                            markerPath,
                            "InterruptedException",
                            "Live client probe was interrupted.",
                            probeFactory,
                            probeWriter
                    );
                    return;
                } catch (Throwable exception) {
                    recordFailure(
                            runtimeBridge,
                            markerPath,
                            exception.getClass().getSimpleName(),
                            failureMessage(exception),
                            probeFactory,
                            probeWriter
                    );
                    try {
                        snapshotWriter.write(
                                markerPath,
                                packId,
                                realMainClass,
                                modules,
                                nativeEntrypoints,
                                runtimeBridge,
                                nativeActivations
                        );
                    } catch (IOException ignored) {
                        // The separate live-client probe report is best-effort.
                    }
                    return;
                }
            }
            recordFailure(
                    runtimeBridge,
                    markerPath,
                    "Timeout",
                    "Live Minecraft client/player was not ready before the probe timed out.",
                    probeFactory,
                    probeWriter
            );
            try {
                snapshotWriter.write(
                        markerPath,
                        packId,
                        realMainClass,
                        modules,
                        nativeEntrypoints,
                        runtimeBridge,
                        nativeActivations
                );
            } catch (IOException ignored) {
                // The marker will remain at the last snapshot.
            }
        }, "EchoNativeLiveClientProbe");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean shouldReportAttempt(int attempt) {
        return attempt == 0 || attempt % REPORT_EVERY_ATTEMPTS == 0;
    }

    private static boolean releaseReadyProbe(Map<String, Object> probe) {
        return Boolean.TRUE.equals(probe.get("nativeProductWorldOpened"))
                && nestedBoolean(probe, "hudNotificationEmitted")
                && nestedBoolean(probe, "allRequiredMutationSurfacesMutated")
                && nestedBoolean(probe, "mutationLedgerRecorded");
    }

    private static boolean nestedBoolean(Map<String, Object> probe, String key) {
        if (Boolean.TRUE.equals(probe.get(key))) {
            return true;
        }
        for (Object value : probe.values()) {
            if (value instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get(key))) {
                return true;
            }
        }
        return false;
    }

    private static boolean mutatingPlayableRuntimeAttempted(Map<String, Object> probe) {
        if (!Boolean.TRUE.equals(probe.get("executed"))) {
            return false;
        }
        for (Object value : probe.values()) {
            if (value instanceof Map<?, ?> map
                    && Boolean.TRUE.equals(map.get("actionsEnabled"))
                    && Boolean.TRUE.equals(map.get("attempted"))) {
                return true;
            }
        }
        return false;
    }

    private static void recordPendingMinecraftClass(
            Path markerPath,
            Map<String, Object> runtimeBridge,
            int attempt,
            ClassNotFoundException exception,
            ProbeFactory probeFactory,
            ProbeWriter probeWriter
    ) {
        if (!shouldReportAttempt(attempt)) {
            return;
        }
        Map<String, Object> probe = probeFactory.create(
                false,
                false,
                false,
                attempt,
                "minecraft_classes_not_ready",
                ""
        );
        probe.put("pendingClass", exception.getMessage());
        runtimeBridge.put("liveClientProbe", probe);
        try {
            probeWriter.write(markerPath, probe);
        } catch (IOException ignored) {
            // The activation marker will be refreshed when the class appears or the probe times out.
        }
    }

    private static void recordFailure(
            Map<String, Object> runtimeBridge,
            Path markerPath,
            String failureKind,
            String message,
            ProbeFactory probeFactory,
            ProbeWriter probeWriter
    ) {
        Map<String, Object> previous = object(runtimeBridge.get("liveClientProbe"));
        Map<String, Object> probe = previous.isEmpty()
                ? probeFactory.create(false, false, false, -1, "failed", "")
                : new LinkedHashMap<>(previous);
        probe.put("lastProbeFailureKind", failureKind);
        probe.put("lastProbeFailureMessage", message);
        if (!Boolean.TRUE.equals(probe.get("executed"))) {
            probe.put("state", "failed");
            probe.put("failureKind", failureKind);
            probe.put("failureMessage", message);
        } else {
            probe.put("state", "executed_with_late_probe_failure");
            probe.put("lateProbeFailurePreservedEvidence", true);
        }
        runtimeBridge.put("liveClientProbe", probe);
        try {
            probeWriter.write(markerPath, probe);
        } catch (IOException ignored) {
            // The activation marker still carries the failure fields when possible.
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

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    @FunctionalInterface
    public interface ProbeExecutor {
        Map<String, Object> apply(Path markerPath, int attempt) throws ReflectiveOperationException, IOException;
    }

    @FunctionalInterface
    public interface ProbeFactory {
        Map<String, Object> create(
                boolean executed,
                boolean hudSent,
                boolean chatSent,
                int attempt,
                String state,
                String playerClass
        );
    }

    @FunctionalInterface
    public interface ProductGameplayEvidenceUpdater {
        Map<String, Object> apply(Map<String, Object> productBridge, Map<String, Object> probe);
    }

    @FunctionalInterface
    public interface ProbeWriter {
        void write(Path markerPath, Map<String, Object> probe) throws IOException;
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
