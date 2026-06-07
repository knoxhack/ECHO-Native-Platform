package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class NativeLoaderModuleSurfaceFlow {
    public static final String SERVICE_ID = "echo.native.module_surface_flow";
    private static final Map<String, Map<String, Object>> CONTEXTS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private NativeLoaderModuleSurfaceFlow() {
    }

    public static boolean open(String namespace, String path, String contentId, Object level, Object pos, Object player, Context context) {
        String surface = surface(namespace, path, context);
        if (surface.isBlank()) {
            return false;
        }
        Map<String, Object> gameplayContext = gameplayContext(surface, namespace, path, contentId, level, pos, player, context);
        if (!gameplayContext.isEmpty()) {
            record(surface, gameplayContext, context);
        }
        Map<String, Object> route = context.openSurface(surface, gameplayContext);
        if (!gameplayContext.isEmpty()) {
            Map<String, Object> enriched = new LinkedHashMap<>(gameplayContext);
            enriched.put("route", route);
            enriched.put("screenOpened", Boolean.TRUE.equals(route.get("screenOpened")));
            enriched.put("routeBound", Boolean.TRUE.equals(route.get("routeBound")));
            enriched.put("handled", Boolean.TRUE.equals(route.get("handled")));
            record(surface, enriched, context);
        }
        return Boolean.TRUE.equals(route.get("screenOpened"))
                || ("LENS".equals(surface)
                && Boolean.TRUE.equals(route.get("handled"))
                && route.containsKey("scan"));
    }

    public static Map<String, Object> contextForMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return Map.of();
        }
        Map<String, Object> context = CONTEXTS.get(mode.trim().toUpperCase(java.util.Locale.ROOT));
        return context == null ? Map.of() : Map.copyOf(context);
    }

    public static String surface(String namespace, String path, Context context) {
        String safeNamespace = lowerContentId(namespace);
        String safePath = lowerContentId(path);
        if ("echoterminal".equals(safeNamespace)
                || "signalos".equals(safeNamespace)
                || hasAny(safePath, "terminal", "workstation", "server_rack", "data_drive")) {
            return "TERMINAL";
        }
        if ("echowiki".equals(safeNamespace) || hasAny(safePath, "wiki", "guide", "manual")) {
            return "WIKI";
        }
        if ("echoindex".equals(safeNamespace)
                || hasAny(safePath, "index", "archive", "codex", "research_lab", "rare_tech_schematic")) {
            return "INDEX";
        }
        if ("echolens".equals(safeNamespace) || hasAny(safePath, "lens", "scanner", "scan", "visor")) {
            return "LENS";
        }
        if ("echoholomap".equals(safeNamespace) || hasAny(safePath, "holomap", "map_table", "waypoint", "route_map")) {
            return "HOLOMAP";
        }
        if (context.isActiveProductNamespace(safeNamespace) && context.isProductMachinePath(safePath)) {
            return "MACHINE";
        }
        return "";
    }

    private static Map<String, Object> gameplayContext(
            String surface,
            String namespace,
            String path,
            String contentId,
            Object level,
            Object pos,
            Object player,
            Context context
    ) {
        String safeSurface = surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT);
        String safeNamespace = lowerContentId(namespace);
        String safePath = lowerContentId(path);
        String safeContentId = firstNonBlank(lowerContentId(contentId), safeNamespace + ":" + safePath);
        if (safeSurface.isBlank()) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("surface", safeSurface);
        data.put("moduleId", safeNamespace);
        data.put("contentPath", safePath);
        data.put("contentId", safeContentId);
        data.put("blockId", safeContentId);
        data.put("machineId", machineIdForBlock(safeNamespace, safePath, safeContentId, context));
        data.put("position", context.blockPositionText(pos));
        data.put("clientSide", context.isClientSideLevel(level));
        data.put("player", playerName(player, context));
        data.put("openedAtMillis", System.currentTimeMillis());
        try {
            data.put("x", context.intMethod(pos, "getX"));
            data.put("y", context.intMethod(pos, "getY"));
            data.put("z", context.intMethod(pos, "getZ"));
        } catch (Throwable ignored) {
            data.put("x", "");
            data.put("y", "");
            data.put("z", "");
        }
        return Map.copyOf(data);
    }

    private static String machineIdForBlock(String namespace, String path, String fallback, Context context) {
        String safeNamespace = lowerContentId(namespace);
        String safePath = lowerContentId(path);
        if (context.isActiveProductNamespace(safeNamespace) && context.isProductMachinePath(safePath)) {
            return safeNamespace + ":" + safePath;
        }
        return fallback == null ? "" : fallback;
    }

    private static void record(String surface, Map<String, Object> contextData, Context context) {
        if (surface == null || surface.isBlank() || contextData == null || contextData.isEmpty()) {
            return;
        }
        String safeSurface = surface.trim().toUpperCase(java.util.Locale.ROOT);
        Map<String, Object> snapshot = Map.copyOf(new LinkedHashMap<>(contextData));
        CONTEXTS.put(safeSurface, snapshot);
        String gameDir = context.gameDir();
        if (gameDir == null || gameDir.isBlank()) {
            return;
        }
        try {
            Map<String, Object> allContexts = new TreeMap<>(CONTEXTS);
            context.writeJsonAtomically(
                    Path.of(gameDir).resolve("echo-native").resolve("native-gameplay-surface-context.json"),
                    allContexts
            );
        } catch (Throwable ignored) {
            // Gameplay interaction must not fail because evidence persistence is unavailable.
        }
    }

    private static String playerName(Object player, Context context) {
        if (player == null) {
            return "";
        }
        try {
            Object name = context.methodValue(player, "getName");
            if (name != null) {
                Object text = context.methodValue(name, "getString");
                if (text != null) {
                    return String.valueOf(text);
                }
                return String.valueOf(name);
            }
        } catch (Throwable ignored) {
            // Best effort only.
        }
        return "";
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? (second == null ? "" : second) : first;
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = lowerContentId(value);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(lowerContentId(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static final class Context {
        private final String gameDirProperty;
        private final ProductNamespacePredicate activeProductNamespace;
        private final ProductMachinePathPredicate productMachinePath;
        private final BlockPositionFormatter blockPositionFormatter;
        private final ClientSideLevelPredicate clientSideLevelPredicate;
        private final MethodReader methodReader;
        private final IntMethodReader intMethodReader;
        private final JsonWriter jsonWriter;
        private final SurfaceOpener surfaceOpener;

        public Context(
                String gameDirProperty,
                ProductNamespacePredicate activeProductNamespace,
                ProductMachinePathPredicate productMachinePath,
                BlockPositionFormatter blockPositionFormatter,
                ClientSideLevelPredicate clientSideLevelPredicate,
                MethodReader methodReader,
                IntMethodReader intMethodReader,
                JsonWriter jsonWriter,
                SurfaceOpener surfaceOpener
        ) {
            this.gameDirProperty = gameDirProperty == null ? "" : gameDirProperty;
            this.activeProductNamespace = activeProductNamespace;
            this.productMachinePath = productMachinePath;
            this.blockPositionFormatter = blockPositionFormatter;
            this.clientSideLevelPredicate = clientSideLevelPredicate;
            this.methodReader = methodReader;
            this.intMethodReader = intMethodReader;
            this.jsonWriter = jsonWriter;
            this.surfaceOpener = surfaceOpener;
        }

        private String gameDir() {
            return gameDirProperty.isBlank() ? "" : System.getProperty(gameDirProperty, "");
        }

        private boolean isActiveProductNamespace(String namespace) {
            return activeProductNamespace.test(namespace);
        }

        private boolean isProductMachinePath(String path) {
            return productMachinePath.test(path);
        }

        private String blockPositionText(Object pos) {
            return blockPositionFormatter.format(pos);
        }

        private boolean isClientSideLevel(Object level) {
            return clientSideLevelPredicate.test(level);
        }

        private Object methodValue(Object target, String methodName) {
            return methodReader.get(target, methodName);
        }

        private int intMethod(Object target, String methodName) throws ReflectiveOperationException {
            return intMethodReader.get(target, methodName);
        }

        private void writeJsonAtomically(Path path, Object value) throws IOException {
            jsonWriter.write(path, value);
        }

        private Map<String, Object> openSurface(String surface, Map<String, Object> gameplayContext) {
            return surfaceOpener.open(surface, gameplayContext);
        }
    }

    @FunctionalInterface
    public interface ProductNamespacePredicate {
        boolean test(String namespace);
    }

    @FunctionalInterface
    public interface ProductMachinePathPredicate {
        boolean test(String path);
    }

    @FunctionalInterface
    public interface BlockPositionFormatter {
        String format(Object pos);
    }

    @FunctionalInterface
    public interface ClientSideLevelPredicate {
        boolean test(Object level);
    }

    @FunctionalInterface
    public interface MethodReader {
        Object get(Object target, String methodName);
    }

    @FunctionalInterface
    public interface IntMethodReader {
        int get(Object target, String methodName) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface JsonWriter {
        void write(Path path, Object value) throws IOException;
    }

    @FunctionalInterface
    public interface SurfaceOpener {
        Map<String, Object> open(String surface, Map<String, Object> gameplayContext);
    }
}
