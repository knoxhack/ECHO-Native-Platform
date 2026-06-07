package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticFieldArgumentInvocation;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticFieldValue;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticInvocation;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeInfoModuleStaticValueInvocation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NativeLoaderInfoModuleRuntimeInvoker {
    private NativeLoaderInfoModuleRuntimeInvoker() {
    }

    public static Map<String, Object> invoke(Context context, String namespace, Object player) {
        String ns = lowerContentId(namespace);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", ns);
        report.put("bridgeKind", "real_echo_info_module_runtime");
        if (isActiveProductNamespace(context, ns)) {
            invokeProductRuntime(context, report, player);
            report.put("successfulCallCount", NativeLoaderRuntimeReflectionSupport.successfulCalls(report));
            return report;
        }
        String route = lowerContentId(context.profile().nativeInfoModuleRuntimeRoutes().getOrDefault(ns, ""));
        switch (route) {
            case "terminal":
                invokeTerminalRuntime(context, report);
                break;
            case "index":
                invokeIndexRuntime(context, report, player);
                break;
            case "lens":
                invokeLensRuntime(context, report);
                break;
            case "holomap":
                invokeHoloMapRuntime(context, report, player);
                break;
            case "wiki":
                invokeWikiRuntime(context, report);
                break;
            case "signalos":
                invokeSignalOsRuntime(context, report, player);
                break;
            default:
                report.put("runtimeBound", false);
                report.put("runtimeRoute", route);
                report.put("summary", "No dedicated info runtime route for " + ns + ".");
                break;
        }
        report.put("successfulCallCount", NativeLoaderRuntimeReflectionSupport.successfulCalls(report));
        return report;
    }

    private static void invokeTerminalRuntime(Context context, Map<String, Object> report) {
        invokeInfoModuleStaticInvocations(context, "echoterminal", report);
        Map<String, Object> staticFields = invokeInfoModuleStaticFieldValues(context, "echoterminal", report);
        Map<String, Object> staticValues = invokeInfoModuleStaticValueInvocations(context, "echoterminal", report);
        invokeInfoModuleStaticFieldArgumentInvocations(context, "echoterminal", report, staticFields);
        Object tabs = staticValues.get("terminalTabs");
        report.put("terminalTabCount", NativeLoaderRuntimeReflectionSupport.sizeOf(tabs));
        report.put("summary", "Terminal tabs, actions, mission providers, and reward services were registered.");
    }

    private static void invokeIndexRuntime(Context context, Map<String, Object> report, Object player) {
        invokeInfoModuleStaticInvocations(context, "echoindex", report);
        Map<String, Object> staticFields = invokeInfoModuleStaticFieldValues(context, "echoindex", report);
        invokeNativeProductHooks(context, report, "index_provider");
        Object service = staticFields.get("indexServiceInstance");
        NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "indexRecipesRebuilt", service, "rebuildRecipes",
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")},
                player, "echo_native_runtime");
        Object snapshot = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "indexRecipeSnapshot", service, "recipeSnapshot",
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")}, player);
        Object providerCount = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "indexProviderCount", service, "providerCount",
                new Class<?>[0]);
        Object catalogCount = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "indexCatalogCount", service, "catalogCount",
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")}, player);
        Map<String, Object> dataSources = context.uiDataSources().get();
        Map<String, Object> nativeIndex = object(dataSources.get("index"));
        Map<String, Object> nativeSearch = context.indexSearcher().apply(
                context.profile().nativeIndexSearchQuery());
        report.put("recipeSnapshotBuilt", snapshot != null);
        report.put("providerCount", providerCount == null ? 0 : providerCount);
        report.put("catalogCount", catalogCount == null ? 0 : catalogCount);
        report.put("nativeEntryCount", NativeLoaderRuntimeReflectionSupport.sizeOf(nativeIndex.get("entries")));
        report.put("dataBackedAction", Boolean.TRUE.equals(nativeSearch.get("handled")));
        report.put("screenOpened", false);
        report.put("summary", "Index recipe/search service was rebuilt and verified against the native data-backed catalog.");
    }

    private static void invokeLensRuntime(Context context, Map<String, Object> report) {
        invokeInfoModuleStaticInvocations(context, "echolens", report);
        Map<String, Object> staticValues = invokeInfoModuleStaticValueInvocations(context, "echolens", report);
        invokeNativeProductHooks(context, report, "lens_integration");
        Object providerCount = staticValues.get("lensProviderCount");
        Object serverProviders = staticValues.get("lensServerProviders");
        Object diagnostics = staticValues.get("lensDiagnostics");
        report.put("providerCount", providerCount == null ? 0 : providerCount);
        report.put("serverProviderCount", NativeLoaderRuntimeReflectionSupport.sizeOf(serverProviders));
        report.put("diagnosticCount", NativeLoaderRuntimeReflectionSupport.sizeOf(diagnostics));
        report.put("summary", "Lens provider registry, inspection service, and core integration were registered.");
    }

    private static void invokeHoloMapRuntime(Context context, Map<String, Object> report, Object player) {
        invokeInfoModuleStaticInvocations(context, "echoholomap", report);
        Map<String, Object> staticFields = invokeInfoModuleStaticFieldValues(context, "echoholomap", report);
        Object service = staticFields.get("holoMapServiceInstance");
        NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "holoMapBuiltinsRegistered", service, "registerBuiltins", new Class<?>[0]);
        Object providerCount = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "holoMapProviderCount", service, "providerCount", new Class<?>[0]);
        Object layers = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "holoMapLayers", service, "richLayers",
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")}, player);
        Object markers = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "holoMapMarkers", service, "richMarkers",
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")}, player);
        Object routes = NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "holoMapRoutes", service, "richRoutes",
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")}, player);
        Class<?> serverPlayerClass = tryRuntimeClass(context, "server.level.ServerPlayer");
        if (player != null && serverPlayerClass != null && serverPlayerClass.isInstance(player)) {
            NativeLoaderRuntimeReflectionSupport.invokeMethodValue(report, "holoMapRefreshed", service, "refresh",
                    new Class<?>[]{serverPlayerClass, String.class}, player, "echo_native_runtime");
            invokeStaticServerPlayerValue(context, report, "holoMapSyncSent",
                    "com.knoxhack.echoholomap.network.HoloMapSync", "send", player);
        }
        Map<String, Object> nativeHoloMap = object(context.uiDataSources().get().get("holomap"));
        Map<String, Object> nativeOpen = context.holoMapOpener().apply(
                String.valueOf(nativeHoloMap.getOrDefault("layer", "")),
                String.valueOf(nativeHoloMap.getOrDefault("marker", ""))
        );
        report.put("providerCount", providerCount == null ? 0 : providerCount);
        report.put("layerCount", Math.max(NativeLoaderRuntimeReflectionSupport.sizeOf(layers), NativeLoaderRuntimeReflectionSupport.sizeOf(nativeHoloMap.get("layers"))));
        report.put("markerCount", Math.max(NativeLoaderRuntimeReflectionSupport.sizeOf(markers), NativeLoaderRuntimeReflectionSupport.sizeOf(nativeHoloMap.get("markers"))));
        report.put("routeCount", NativeLoaderRuntimeReflectionSupport.sizeOf(routes));
        report.put("dataBackedAction", Boolean.TRUE.equals(nativeOpen.get("handled")));
        report.put("screenOpened", false);
        report.put("summary", "HoloMap builtins, terrain layers, markers, and native data-backed map route were verified.");
    }

    private static void invokeWikiRuntime(Context context, Map<String, Object> report) {
        invokeInfoModuleStaticInvocations(context, "echowiki", report);
        Map<String, Object> wiki = object(context.uiDataSources().get().get("wiki"));
        Map<String, Object> nativeOpen = context.wikiOpener().apply(
                String.valueOf(wiki.getOrDefault("guide", "")),
                String.valueOf(wiki.getOrDefault("page", ""))
        );
        report.put("articleCount", NativeLoaderRuntimeReflectionSupport.sizeOf(wiki.get("articles")));
        report.put("dataBackedAction", Boolean.TRUE.equals(nativeOpen.get("handled")));
        report.put("screenOpened", false);
        report.put("summary", "Wiki defaults, terminal integration, and native data-backed article route were verified.");
    }

    private static void invokeSignalOsRuntime(Context context, Map<String, Object> report, Object player) {
        invokeInfoModuleStaticInvocations(context, "signalos", report);
        Map<String, Object> staticValues = invokeInfoModuleStaticValueInvocations(context, "signalos", report);
        invokeStaticServerPlayerValue(context, report, "signalOsTerminalSyncSent",
                "com.knoxhack.signalos.network.SignalOsTerminalSync", "send", player);
        Object chapters = staticValues.get("signalOsChapters");
        Object missions = staticValues.get("signalOsMissions");
        Object archives = staticValues.get("signalOsArchives");
        Object apps = staticValues.get("signalOsApps");
        invokeStaticPlayerValue(context, report, "signalOsDataRecords",
                "com.knoxhack.signalos.content.SignalOsContentRegistry", "dataRecords", player);
        report.put("chapterCount", NativeLoaderRuntimeReflectionSupport.sizeOf(chapters));
        report.put("missionCount", NativeLoaderRuntimeReflectionSupport.sizeOf(missions));
        report.put("archiveCount", NativeLoaderRuntimeReflectionSupport.sizeOf(archives));
        report.put("appCount", NativeLoaderRuntimeReflectionSupport.sizeOf(apps));
        report.put("summary", "SignalOS built-in content, app actions, sync, and registry content were invoked.");
    }

    private static void invokeInfoModuleStaticInvocations(Context context, String namespace, Map<String, Object> report) {
        for (NativeInfoModuleStaticInvocation invocation : context.profile().nativeInfoModuleStaticInvocations()
                .getOrDefault(lowerContentId(namespace), List.of())) {
            NativeLoaderRuntimeReflectionSupport.invokeStaticNoArg(report, invocation.reportKey(), invocation.className(), invocation.methodName());
        }
    }

    private static Map<String, Object> invokeInfoModuleStaticValueInvocations(
            Context context,
            String namespace,
            Map<String, Object> report
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (NativeInfoModuleStaticValueInvocation invocation : context.profile().nativeInfoModuleStaticValueInvocations()
                .getOrDefault(lowerContentId(namespace), List.of())) {
            values.put(invocation.reportKey(), NativeLoaderRuntimeReflectionSupport.invokeStaticNoArgValue(
                    report,
                    invocation.reportKey(),
                    invocation.className(),
                    invocation.methodName()));
        }
        return values;
    }

    private static Map<String, Object> invokeInfoModuleStaticFieldValues(
            Context context,
            String namespace,
            Map<String, Object> report
    ) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (NativeInfoModuleStaticFieldValue field : context.profile().nativeInfoModuleStaticFieldValues()
                .getOrDefault(lowerContentId(namespace), List.of())) {
            Object value = NativeLoaderRuntimeReflectionSupport.staticFieldValue(field.className(), field.fieldName());
            values.put(field.reportKey(), value);
            report.put(field.reportKey(), value != null);
            if (value == null) {
                report.put(field.reportKey() + "Failure", "No static field value");
            } else {
                report.put(field.reportKey() + "Value", NativeLoaderRuntimeReflectionSupport.valueSummary(value));
            }
        }
        return values;
    }

    private static void invokeInfoModuleStaticFieldArgumentInvocations(
            Context context,
            String namespace,
            Map<String, Object> report,
            Map<String, Object> fieldValues
    ) {
        for (NativeInfoModuleStaticFieldArgumentInvocation invocation : context.profile()
                .nativeInfoModuleStaticFieldArgumentInvocations()
                .getOrDefault(lowerContentId(namespace), List.of())) {
            NativeLoaderRuntimeReflectionSupport.invokeStaticOneArgValue(
                    report,
                    invocation.reportKey(),
                    invocation.className(),
                    invocation.methodName(),
                    fieldValues.get(invocation.fieldValueKey()));
        }
    }

    private static void invokeProductRuntime(Context context, Map<String, Object> report, Object player) {
        Map<String, Object> activation = context.moduleActivation().apply(
                context.profile().namespace(),
                context.moduleClassFor().apply(context.profile().namespace()));
        report.put("productNativeModuleActivated", !activation.isEmpty());
        report.put("productNativeModuleActivationValue", NativeLoaderRuntimeReflectionSupport.valueSummary(activation));
        invokeNativeProductHooks(context, report,
                "core_services",
                "companion_drone_data_key",
                "drone_map_provider",
                "world_core_builtins",
                "mission_core_integration");
        invokeProductInfoModuleRuntimes(context, report, player);
        report.put("nativeUiDataSourcesBound", !context.uiDataSources().get().isEmpty());
        invokeNativeProductHooks(context, report, "render_static_surfaces");
        report.put("summary", context.profile().nativeGameplayDisplayName()
                + " native module, Terminal, Index, Lens, HoloMap, Wiki, drone data, and world-core service providers were bound through real module surfaces.");
    }

    private static void invokeProductInfoModuleRuntimes(Context context, Map<String, Object> report, Object player) {
        Map<String, Map<String, Object>> moduleReports = new LinkedHashMap<>();
        int boundCount = 0;
        for (String moduleId : context.profile().nativeInfoModuleNamespaces()) {
            String namespace = lowerContentId(moduleId);
            if (namespace.isBlank() || isActiveProductNamespace(context, namespace)) {
                continue;
            }
            Map<String, Object> moduleReport = invoke(context, namespace, player);
            moduleReports.put(namespace, moduleReport);
            if (integer(moduleReport.get("successfulCallCount")) > 0
                    || Boolean.TRUE.equals(moduleReport.get("screenOpened"))
                    || Boolean.TRUE.equals(moduleReport.get("dataBackedAction"))) {
                boundCount++;
            }
        }
        report.put("productInfoModuleRuntimes", Map.copyOf(moduleReports));
        report.put("productInfoModuleRuntimeBoundCount", boundCount);
        publishProductInfoModuleCompatibilityFields(report, moduleReports);
    }

    private static void publishProductInfoModuleCompatibilityFields(
            Map<String, Object> report,
            Map<String, Map<String, Object>> moduleReports
    ) {
        copyRuntimeFields(report, moduleReports.get("echoterminal"),
                "terminalCoreServicesRegistered",
                "terminalCommonIntegrationRegistered",
                "terminalBuiltinTabsRegistered",
                "terminalTabCount");
        copyRuntimeFields(report, moduleReports.get("echoindex"),
                "indexTerminalCommonIntegrationRegistered",
                "indexMissionCoreIntegrationRegistered",
                "indexProviderCount",
                "indexProviderCountValue");
        copyRuntimeFields(report, moduleReports.get("echolens"),
                "lensBuiltinsRegistered",
                "lensCoreIntegrationRegistered",
                "lensMissionCoreIntegrationRegistered",
                "lensProviderCount",
                "lensProviderCountValue");
        copyRuntimeFields(report, moduleReports.get("echoholomap"),
                "holoMapTerminalCommonIntegrationRegistered",
                "holoMapTerminalClientIntegrationRegistered",
                "holoMapMissionCoreIntegrationRegistered",
                "holoMapIndexIntegrationRegistered",
                "holoMapBuiltinsRegistered",
                "holoMapProviderCount",
                "holoMapProviderCountValue");
        copyRuntimeFields(report, moduleReports.get("echowiki"),
                "wikiDefaultsEnsured",
                "wikiTerminalClientIntegrationRegistered",
                "articleCount");
        copyRuntimeFields(report, moduleReports.get("signalos"),
                "signalOsBuiltinContentRegistered",
                "signalOsBuiltinActionsRegistered",
                "chapterCount",
                "missionCount",
                "archiveCount",
                "appCount");
        Map<String, Object> index = moduleReports.get("echoindex");
        if (!report.containsKey("indexProviderCountValue")
                && index != null
                && index.get("providerCount") instanceof Number number) {
            report.put("indexProviderCountValue", number);
        }
        Map<String, Object> holoMap = moduleReports.get("echoholomap");
        if (!report.containsKey("holoMapProviderCountValue")
                && holoMap != null
                && holoMap.get("providerCount") instanceof Number number) {
            report.put("holoMapProviderCountValue", number);
        }
    }

    private static void copyRuntimeFields(Map<String, Object> target, Map<String, Object> source, String... keys) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (String key : keys) {
            if (source.containsKey(key)) {
                target.put(key, source.get(key));
            }
        }
    }

    private static Object invokeStaticPlayerValue(
            Context context,
            Map<String, Object> report,
            String key,
            String className,
            String methodName,
            Object player
    ) {
        return NativeLoaderRuntimeReflectionSupport.invokeStaticMethodValue(report, key, className, methodName,
                new Class<?>[]{tryRuntimeClass(context, "world.entity.player.Player")}, player);
    }

    private static Object invokeStaticServerPlayerValue(
            Context context,
            Map<String, Object> report,
            String key,
            String className,
            String methodName,
            Object player
    ) {
        Class<?> serverPlayerClass = tryRuntimeClass(context, "server.level.ServerPlayer");
        if (player == null || serverPlayerClass == null || !serverPlayerClass.isInstance(player)) {
            report.put(key, false);
            report.put(key + "Failure", "No ServerPlayer available");
            return null;
        }
        return NativeLoaderRuntimeReflectionSupport.invokeStaticMethodValue(report, key, className, methodName,
                new Class<?>[]{serverPlayerClass}, player);
    }

    private static Class<?> tryRuntimeClass(Context context, String suffix) {
        return NativeLoaderRuntimeReflectionSupport.tryClass(context.runtimeClass().apply(suffix));
    }

    private static int invokeNativeProductHooks(Context context, Map<String, Object> report, String... roles) {
        int invoked = 0;
        for (String role : roles) {
            if (context.productHookInvoker().invoke(report, role)) {
                invoked++;
            }
        }
        return invoked;
    }

    private static boolean isActiveProductNamespace(Context context, String namespace) {
        return lowerContentId(context.profile().namespace()).equals(lowerContentId(namespace));
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

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record Context(
            EchoNativeBootstrapProductProfile profile,
            Function<String, String> runtimeClass,
            Function<String, String> moduleClassFor,
            BiFunction<String, String, Map<String, Object>> moduleActivation,
            ProductHookInvoker productHookInvoker,
            Supplier<Map<String, Object>> uiDataSources,
            Function<String, Map<String, Object>> indexSearcher,
            BiFunction<String, String, Map<String, Object>> holoMapOpener,
            BiFunction<String, String, Map<String, Object>> wikiOpener
    ) {
    }

    @FunctionalInterface
    public interface ProductHookInvoker {
        boolean invoke(Map<String, Object> report, String role);
    }
}
