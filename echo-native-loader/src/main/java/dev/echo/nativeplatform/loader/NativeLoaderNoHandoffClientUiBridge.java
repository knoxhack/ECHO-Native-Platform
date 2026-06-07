package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NativeLoaderNoHandoffClientUiBridge {
    public static final String SERVICE_ID = "echo.native.no_handoff_client_ui_bridge";
    private static final String LIVE_UI_BRIDGE_FILE = "live-ui-bridge.json";

    private NativeLoaderNoHandoffClientUiBridge() {
    }

    public static void markGap(Map<String, Object> runtimeBridge) {
        Map<String, Object> bridge = new LinkedHashMap<>(object(runtimeBridge.get("nativeClientUiBridge")));
        bridge.put("nativeNoHandoffClientUiBridgeServiceId", SERVICE_ID);
        bridge.put("installed", true);
        bridge.put("clientUiHostAttached", false);
        bridge.put("clientRuntimeClassAvailable", false);
        bridge.put("clientRuntimeAccessed", false);
        bridge.put("screenGenerationAttempted", false);
        bridge.put("generatedScreenClassCompiled", false);
        bridge.put("noScreenCrash", true);
        bridge.put("clientAttachmentBlockedReason", "minecraft_handoff_not_requested");
        bridge.put("failureKind", "");
        bridge.put("failureMessage", "");
        bridge.put("pendingClass", "");
        bridge.put("summary", "Native bootstrap ran in no-handoff mode; live client UI/keybind attachment requires a product launcher handoff with a trusted client bridge.");
        runtimeBridge.put("nativeClientUiBridge", bridge);
    }

    public static Map<String, Object> initialBridge(Path markerPath, JsonValueParser parser) {
        Path uiPath = bridgePath(markerPath);
        if (Files.isRegularFile(uiPath)) {
            try {
                String raw = Files.readString(uiPath, StandardCharsets.UTF_8);
                Map<String, Object> preserved = object(parser.parse(raw));
                if (Boolean.TRUE.equals(preserved.get("clientUiHostAttached"))
                        && Boolean.TRUE.equals(preserved.get("clientThreadAccepted"))
                        && Boolean.TRUE.equals(preserved.get("liveWindowHandlePresent"))) {
                    Map<String, Object> bridge = new LinkedHashMap<>(preserved);
                    bridge.put("nativeNoHandoffClientUiBridgeServiceId", SERVICE_ID);
                    bridge.put("preservedExistingLiveUiEvidence", true);
                    bridge.put("summary", text(bridge.get("summary")).isBlank()
                            ? "Existing live UI/keybind host evidence was preserved during no-handoff activation."
                            : bridge.get("summary"));
                    return bridge;
                }
            } catch (Throwable ignored) {
                // Invalid or partial UI bridge snapshots are replaced with the no-handoff gap marker.
            }
        }
        return Map.of();
    }

    public static void writeSidecar(Path markerPath, Map<String, Object> bridge, JsonWriter writer) {
        try {
            Path sidecarPath = bridgePath(markerPath);
            Files.createDirectories(sidecarPath.getParent());
            writer.write(sidecarPath, bridge);
        } catch (IOException ignored) {
            // The activation marker still carries the exact no-handoff gap if this sidecar cannot be written.
        }
    }

    private static Path bridgePath(Path markerPath) {
        return markerPath.toAbsolutePath().normalize().getParent().resolve(LIVE_UI_BRIDGE_FILE);
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

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public interface JsonValueParser {
        Object parse(String text);
    }

    public interface JsonWriter {
        void write(Path path, Object value) throws IOException;
    }
}
