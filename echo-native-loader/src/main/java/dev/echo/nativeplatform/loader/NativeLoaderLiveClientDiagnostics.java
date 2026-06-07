package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderLiveClientDiagnostics {
    public static final String SERVICE_ID = "echo.native.live_client_diagnostics";

    private NativeLoaderLiveClientDiagnostics() {
    }

    public static void addDiagnostics(
            Map<String, Object> probe,
            Object minecraft,
            boolean includeThreads,
            OptionalMethodReader optionalMethodReader
    ) {
        probe.put("gameLoadFinished", Boolean.TRUE.equals(optionalMethodReader.read(minecraft, "isGameLoadFinished")));
        probe.put("minecraftRunning", Boolean.TRUE.equals(optionalMethodReader.read(minecraft, "isRunning")));
        probe.put("windowPresent", optionalMethodReader.read(minecraft, "getWindow") != null);
        probe.put("singleplayerServerPresent", optionalMethodReader.read(minecraft, "getSingleplayerServer") != null);
        probe.put("connectionPresent", optionalMethodReader.read(minecraft, "getConnection") != null);
        if (includeThreads) {
            probe.put("threadSnapshot", liveClientThreadSnapshot());
        }
    }

    public static void addState(
            Map<String, Object> probe,
            Object minecraft,
            Object player,
            Object gui,
            Object level,
            Object screen
    ) {
        probe.put("minecraftClass", minecraft == null ? "" : minecraft.getClass().getName());
        probe.put("playerPresent", player != null);
        probe.put("guiPresent", gui != null);
        probe.put("levelPresent", level != null);
        probe.put("screenPresent", screen != null);
        probe.put("screenClass", screen == null ? "" : screen.getClass().getName());
        probe.put("clientStateSummary", "player=" + (player != null)
                + ", gui=" + (gui != null)
                + ", level=" + (level != null)
                + ", screen=" + (screen == null ? "none" : screen.getClass().getName()));
    }

    public static void setWindowTitle(Object minecraft, String windowTitle) throws ReflectiveOperationException {
        Object window = minecraft.getClass().getMethod("getWindow").invoke(minecraft);
        if (window == null) {
            throw new NoSuchMethodException("Minecraft window is not available");
        }
        window.getClass().getMethod("setTitle", String.class).invoke(window, windowTitle);
    }

    private static List<Map<String, Object>> liveClientThreadSnapshot() {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        Thread.getAllStackTraces().entrySet().stream()
                .filter(entry -> isRelevantLiveClientThread(entry.getKey().getName()))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Thread::getName)))
                .limit(12)
                .forEach(entry -> {
                    Thread thread = entry.getKey();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", thread.getName());
                    item.put("state", thread.getState().name());
                    item.put("daemon", thread.isDaemon());
                    item.put("topFrames", topStackFrames(entry.getValue(), 8));
                    snapshot.add(item);
                });
        return snapshot;
    }

    private static boolean isRelevantLiveClientThread(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("render")
                || lower.contains("minecraft")
                || lower.contains("download")
                || lower.contains("resource")
                || lower.contains("datafixer")
                || lower.contains("echo");
    }

    private static List<String> topStackFrames(StackTraceElement[] frames, int limit) {
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < frames.length && index < limit; index++) {
            StackTraceElement frame = frames[index];
            lines.add(frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber());
        }
        return lines;
    }

    @FunctionalInterface
    public interface OptionalMethodReader {
        Object read(Object target, String methodName);
    }
}
