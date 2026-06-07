package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderLiveClientProbeSeed {
    public static final String SERVICE_ID = "echo.native.live_client_probe_seed";

    private NativeLoaderLiveClientProbeSeed() {
    }

    public static Map<String, Object> initialProbe(
            Path markerPath,
            ProbePathResolver pathResolver,
            JsonValueParser parser,
            WaitingProbeFactory waitingProbeFactory
    ) {
        Path probePath = pathResolver.resolve(markerPath);
        if (Files.isRegularFile(probePath)) {
            try {
                String raw = Files.readString(probePath, StandardCharsets.UTF_8);
                Map<String, Object> preserved = object(parser.parse(raw));
                if (Boolean.TRUE.equals(preserved.get("executed"))) {
                    Map<String, Object> probe = new LinkedHashMap<>(preserved);
                    probe.put("nativeLiveClientProbeSeedServiceId", SERVICE_ID);
                    probe.put("preservedExistingLiveEvidence", true);
                    probe.putIfAbsent("schema", "echo.native.live_client_probe.v1");
                    probe.putIfAbsent("installed", true);
                    probe.putIfAbsent("strategy", "minecraft_client_thread_reflection_probe");
                    probe.putIfAbsent("summary", "Preserved live Minecraft client/player evidence from a previous handoff.");
                    return probe;
                }
            } catch (IOException | IllegalArgumentException ignored) {
                // Fall back to a fresh waiting probe; the live client can replace it.
            }
        }
        Map<String, Object> waiting = waitingProbeFactory.create(
                false,
                false,
                false,
                0,
                "probe_installed_waiting_for_live_client",
                ""
        );
        waiting.put("nativeLiveClientProbeSeedServiceId", SERVICE_ID);
        return waiting;
    }

    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    @FunctionalInterface
    public interface ProbePathResolver {
        Path resolve(Path markerPath);
    }

    @FunctionalInterface
    public interface JsonValueParser {
        Object parse(String text);
    }

    @FunctionalInterface
    public interface WaitingProbeFactory {
        Map<String, Object> create(
                boolean executed,
                boolean hudSent,
                boolean chatSent,
                int attempt,
                String state,
                String playerClass
        );
    }
}
