package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.loader.NativeLoaderModuleActionRouter;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeActionKeyPathHints;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBlockActionRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBlockFallbackRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeItemActionRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePhysicalActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;

import java.util.List;
import java.util.Map;

final class EchoNativeProductProfileCore {
    private final EchoNativeBootstrapProductProfile profile;

    EchoNativeProductProfileCore(EchoNativeBootstrapProductProfile profile) {
        this.profile = profile;
    }

    EchoNativeBootstrapProductProfile profile() {
        return profile;
    }

    String namespace() {
        return profile.namespace();
    }

    String id(String path) {
        return profile.id(path);
    }

    String path(String idOrPath) {
        String value = lowerContentId(idOrPath);
        if (value.isBlank()) {
            return "";
        }
        int namespaceSeparator = value.indexOf(':');
        return namespaceSeparator >= 0 ? value.substring(namespaceSeparator + 1) : value;
    }

    String configuredId(String idOrPath) {
        String value = lowerContentId(idOrPath);
        if (value.isBlank()) {
            return "";
        }
        return value.contains(":") ? value : id(value);
    }

    List<String> configuredIds(List<String> idsOrPaths) {
        if (idsOrPaths == null || idsOrPaths.isEmpty()) {
            return List.of();
        }
        return idsOrPaths.stream()
                .map(this::configuredId)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
    }

    String blockActionMachineId(String action, String fallbackPath) {
        String key = lowerContentId(action);
        Map<String, String> configuredIds = profile.nativeBlockActionMachineIds();
        String configured = configuredIds == null ? "" : lowerContentId(configuredIds.getOrDefault(key, ""));
        return configuredId(configured.isBlank() ? fallbackPath : configured);
    }

    String packKey() {
        return lowerContentId(profile.nativeGameplayPackId());
    }

    String terminalRoute(String path) {
        return "echoterminal:" + packKey() + "_" + path;
    }

    String holoMapRoute(String path) {
        return "echoholomap:" + packKey() + "_" + path;
    }

    String hudActionKey(String path) {
        return "native_client.hud_notification." + packKey() + "_" + path;
    }

    String[] ids(String... paths) {
        String[] ids = new String[paths.length];
        for (int index = 0; index < paths.length; index++) {
            ids[index] = id(paths[index]);
        }
        return ids;
    }

    boolean isMachinePath(String path) {
        String safePath = lowerContentId(path);
        if (safePath.isBlank()) {
            return false;
        }
        for (String machinePath : profile.nativeMachinePaths()) {
            if (safePath.equals(lowerContentId(machinePath))) {
                return true;
            }
        }
        return false;
    }

    String itemAction(String path) {
        for (NativeItemActionRule rule : profile.nativeItemActionRules()) {
            if (rule != null && hasAny(path, rule.pathHints())) {
                return lowerContentId(rule.action());
            }
        }
        return "";
    }

    String blockAction(String path) {
        for (NativeBlockActionRule rule : profile.nativeBlockActionRules()) {
            if (rule != null && hasAny(path, rule.pathHints())) {
                return lowerContentId(rule.action());
            }
        }
        return "";
    }

    String blockFallback(String blockId) {
        String path = path(lowerContentId(blockId));
        for (NativeBlockFallbackRule rule : profile.nativeBlockFallbackRules()) {
            if (rule != null && hasAny(path, rule.pathHints())) {
                String fallbackId = configuredId(rule.fallbackBlockId());
                if (!fallbackId.isBlank()) {
                    return fallbackId;
                }
            }
        }
        return configuredId(profile.nativeDefaultBlockFallbackId());
    }

    String actionKey(String path) {
        for (NativeActionKeyPathHints hints : profile.nativeActionKeyPathHints()) {
            List<String> pathHints = hints == null || hints.pathHints() == null ? List.of() : hints.pathHints();
            if (hints != null && !lowerContentId(hints.actionKey()).isBlank() && hasAny(path, pathHints)) {
                return lowerContentId(hints.actionKey());
            }
        }
        return path == null || path.isBlank() ? "module" : path;
    }

    String screenIdForSurface(String surface) {
        NativeUiSurfaceRoute productSurface = surfaceRoute(surface);
        if (productSurface != null && productSurface.screenId() != null && !productSurface.screenId().isBlank()) {
            return productSurface.screenId();
        }
        String fallback = fallbackScreenIdForSurface(surface);
        if (!fallback.isBlank()) {
            return fallback;
        }
        return "native_ui:" + compactActionKey(String.valueOf(surface));
    }

    String canonicalIdForSurface(String surface) {
        NativeUiSurfaceRoute productSurface = surfaceRoute(surface);
        if (productSurface != null && productSurface.canonicalId() != null && !productSurface.canonicalId().isBlank()) {
            return productSurface.canonicalId();
        }
        String fallback = fallbackCanonicalIdForSurface(surface);
        if (!fallback.isBlank()) {
            return fallback;
        }
        return "native_ui:" + compactActionKey(String.valueOf(surface));
    }

    String targetForSurface(String surface) {
        NativeUiSurfaceRoute productSurface = surfaceRoute(surface);
        if (productSurface != null) {
            return firstNonBlank(
                    productSurface.target(),
                    firstNonBlank(productSurface.canonicalId(), productSurface.screenId()));
        }
        String fallback = fallbackTargetForSurface(surface);
        if (!fallback.isBlank()) {
            return fallback;
        }
        return canonicalIdForSurface(surface);
    }

    private static String fallbackScreenIdForSurface(String surface) {
        return switch (normalizedSurface(surface)) {
            case "TERMINAL" -> "echoterminal:terminal";
            case "INDEX" -> "echoindex:index";
            case "LENS" -> "echolens:lens";
            case "HOLOMAP" -> "echoholomap:holomap";
            case "WIKI" -> "echowiki:wiki";
            case "SIGNALOS" -> "signalos:terminal";
            case "ASHFALL_DRONE" -> "echoashfallprotocol:drone";
            case "HUD" -> "echohudcore:hud";
            case "MISSION_LOG" -> "echoscreencore:mission_log";
            case "MAIN_MENU" -> "echo:main_menu";
            default -> "";
        };
    }

    private static String fallbackCanonicalIdForSurface(String surface) {
        return switch (normalizedSurface(surface)) {
            case "TERMINAL" -> "echoterminal:terminal";
            case "INDEX" -> "echoindex:index";
            case "LENS" -> "echoashfallprotocol:portable_signal_scanner";
            case "HOLOMAP" -> "echoholomap:ashfall_map";
            case "WIKI" -> "echowiki:ashfall";
            case "SIGNALOS" -> "signalos:terminal";
            case "ASHFALL_DRONE" -> "echoashfallprotocol:companion_drone";
            case "HUD" -> "echoashfallprotocol:runtime_hud_notification";
            case "MISSION_LOG" -> "echoashfallprotocol:secure_crash_outpost";
            case "MAIN_MENU" -> "echoashfallprotocol:echo_native_main_menu";
            default -> "";
        };
    }

    private static String fallbackTargetForSurface(String surface) {
        return fallbackCanonicalIdForSurface(surface);
    }

    private static String normalizedSurface(String surface) {
        return surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT);
    }

    List<NativeUiActionRoute> uiActionRoutes() {
        return profile.nativeUiActionRoutes();
    }

    Map<String, List<String>> uiDataSourceRoots() {
        return profile.nativeUiDataSourceRoots();
    }

    Map<String, String> uiDefaultContentIds() {
        return profile.nativeUiDefaultContentIds();
    }

    String uiDefaultContentId(String key, String fallback) {
        String value = uiDefaultContentIds().get(key);
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return value.trim();
    }

    String lensFallbackTarget() {
        return lowerContentId(profile.nativeLensFallbackTarget());
    }

    List<String> mainMenuOptions() {
        return profile.nativeMainMenuOptions();
    }

    List<NativePhysicalActionRoute> physicalActionRoutes() {
        return profile.nativePhysicalActionRoutes();
    }

    List<NativeUiSurfaceRoute> uiSurfaceRoutes() {
        return profile.nativeUiSurfaceRoutes();
    }

    Map<String, String> clientScreenClasses() {
        return profile.nativeClientScreenClasses();
    }

    String clientScreenClassName(String surface) {
        if (surface == null || surface.isBlank()) {
            return "";
        }
        Map<String, String> classes = clientScreenClasses();
        if (classes == null || classes.isEmpty()) {
            return "";
        }
        String direct = classes.get(surface);
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }
        String normalized = surface.trim().toUpperCase(java.util.Locale.ROOT);
        for (Map.Entry<String, String> entry : classes.entrySet()) {
            if (entry.getKey() != null
                    && normalized.equals(entry.getKey().trim().toUpperCase(java.util.Locale.ROOT))
                    && entry.getValue() != null
                    && !entry.getValue().isBlank()) {
                return entry.getValue().trim();
            }
        }
        return "";
    }

    Map<String, String> clientHudRendererClasses() {
        return profile.nativeClientHudRendererClasses();
    }

    List<String> clientHudRendererClassNames() {
        Map<String, String> classes = clientHudRendererClasses();
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        return classes.entrySet().stream()
                .filter(entry -> alwaysOnNativeHudRenderer(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getValue)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static boolean alwaysOnNativeHudRenderer(String surface, String className) {
        if (className == null || className.isBlank() || surface == null || surface.isBlank()) {
            return false;
        }
        String normalized = surface.trim().toUpperCase(java.util.Locale.ROOT);
        return "HUD".equals(normalized)
                || normalized.endsWith("_HUD")
                || normalized.contains(":HUD")
                || "INDEX".equals(normalized)
                || "LENS".equals(normalized)
                || "HOLOMAP".equals(normalized);
    }

    Map<String, String> clientLoadingRendererClasses() {
        return profile.nativeClientLoadingRendererClasses();
    }

    List<String> clientLoadingRendererClassNames() {
        Map<String, String> classes = clientLoadingRendererClasses();
        if (classes == null || classes.isEmpty()) {
            return List.of();
        }
        return classes.values().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    List<String> uiProductHotkeys() {
        return profile.nativeUiHotkeys();
    }

    NativeLoaderModuleActionRouter.Config moduleActionRouterConfig() {
        return new NativeLoaderModuleActionRouter.Config(
                profile.nativeInfoModulePlacementHints(),
                profile.nativeInfoModuleRewardItemHints(),
                profile.nativeInfoModuleFallbackBlockIds(),
                profile.nativeInfoModuleFallbackItemIds(),
                profile.nativeRecoveryPlacementHints(),
                profile.nativeRecoveryRewardItemHints(),
                profile.nativeRecoveryBlockPlacementHints(),
                profile.nativeArcanaPlacementHints(),
                profile.nativeArcanaRewardItemHints(),
                profile.nativeArcanaFallbackItemIds(),
                profile.nativeModuleActionRoutes()
        );
    }

    Map<String, String> uiHotkeyConflicts() {
        return profile.nativeUiHotkeyConflicts();
    }

    String recoveryItemId() {
        String itemId = profile.nativeRecoveryItemId();
        if (itemId == null || itemId.isBlank()) {
            return "echoashfallprotocol:portable_signal_scanner";
        }
        return itemId;
    }

    String indexSearchQuery() {
        return profile.nativeIndexSearchQuery();
    }

    String machineScreenId() {
        return profile.nativeMachineScreenId();
    }

    String machineEffectPrefix() {
        return profile.nativeMachineEffectPrefix();
    }

    String machineRecipeCatalogSourcePath() {
        return profile.nativeMachineRecipeCatalogSourcePath();
    }

    Map<String, Object> gameplayBridge(Map<String, Object> runtimeBridge, String primaryKey) {
        Map<String, Object> bridge = object(runtimeBridge == null ? null : runtimeBridge.get(primaryKey));
        if (!bridge.isEmpty()) {
            return bridge;
        }
        for (String legacyKey : profile.nativeLegacyGameplayBridgeKeys()) {
            bridge = object(runtimeBridge == null ? null : runtimeBridge.get(legacyKey));
            if (!bridge.isEmpty()) {
                return bridge;
            }
        }
        return Map.of();
    }

    private NativeUiSurfaceRoute surfaceRoute(String surface) {
        String safeSurface = surface == null ? "" : surface.trim().toUpperCase(java.util.Locale.ROOT);
        if (safeSurface.isBlank()) {
            return null;
        }
        for (NativeUiSurfaceRoute route : profile.nativeUiSurfaceRoutes()) {
            if (route != null && safeSurface.equals(String.valueOf(route.surface()).trim().toUpperCase(java.util.Locale.ROOT))) {
                return route;
            }
        }
        return null;
    }

    private static boolean hasAny(String value, List<String> needles) {
        return hasAny(value, needles == null ? new String[0] : needles.toArray(String[]::new));
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = lowerContentId(value);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String lowerContentId(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String compactActionKey(String value) {
        String safe = lowerContentId(value).replace(':', '_').replace('/', '_').replace('.', '_');
        StringBuilder builder = new StringBuilder();
        boolean previousUnderscore = false;
        for (int index = 0; index < safe.length(); index++) {
            char c = safe.charAt(index);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (allowed) {
                builder.append(c);
                previousUnderscore = false;
            } else if (!previousUnderscore) {
                builder.append('_');
                previousUnderscore = true;
            }
        }
        String result = builder.toString();
        while (result.startsWith("_")) {
            result = result.substring(1);
        }
        while (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isBlank() ? "unknown" : result;
    }

    private static String firstNonBlank(String first, String second) {
        String safeFirst = first == null ? "" : first.trim();
        return safeFirst.isBlank() ? (second == null ? "" : second.trim()) : safeFirst;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
