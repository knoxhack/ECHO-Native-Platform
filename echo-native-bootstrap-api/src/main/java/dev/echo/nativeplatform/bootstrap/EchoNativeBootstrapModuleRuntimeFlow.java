package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeIntegrationHook;
import dev.echo.nativeplatform.loader.NativeLoaderInfoModuleRuntimeInvoker;
import dev.echo.nativeplatform.loader.NativeLoaderModuleActivationInvoker;
import dev.echo.nativeplatform.loader.NativeLoaderModuleClassResolver;
import dev.echo.nativeplatform.loader.NativeLoaderModuleSurfaceFlow;
import dev.echo.nativeplatform.loader.NativeLoaderModuleRuntimeServices;
import dev.echo.nativeplatform.loader.NativeLoaderProductFeedbackSupport;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeReflectionSupport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

final class EchoNativeBootstrapModuleRuntimeFlow {
    private final EchoNativeBootstrapProductProfile profile;
    private final EchoNativeProductProfileCore productProfile;
    private final Context context;
    private final NativeLoaderInfoModuleRuntimeInvoker.Context infoRuntimeContext;
    private final NativeLoaderModuleRuntimeServices.Context moduleRuntimeServicesContext;

    EchoNativeBootstrapModuleRuntimeFlow(
            EchoNativeBootstrapProductProfile profile,
            EchoNativeProductProfileCore productProfile,
            Context context
    ) {
        this.profile = profile;
        this.productProfile = productProfile;
        this.context = context;
        this.infoRuntimeContext = new NativeLoaderInfoModuleRuntimeInvoker.Context(
                profile,
                context.runtimeClass()::apply,
                this::moduleClassFor,
                this::invokeModuleActivation,
                this::invokeProductHook,
                EchoNativeAgent5UiHandlerRegistry::dataSources,
                EchoNativeAgent5UiHandlerRegistry::searchIndex,
                EchoNativeAgent5UiHandlerRegistry::openHolomap,
                EchoNativeAgent5UiHandlerRegistry::openWiki
        );
        this.moduleRuntimeServicesContext = new NativeLoaderModuleRuntimeServices.Context(
                profile,
                context.runtimeClass()::apply,
                this::invokeProductHook
        );
    }

    Map<String, Object> invoke(
            String namespace,
            String path,
            Object level,
            Object pos,
            Object player,
            boolean blockRoute
    ) {
        String ns = lowerContentId(namespace);
        String safePath = lowerContentId(path);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", ns);
        report.put("contentPath", safePath);
        report.put("routeKind", blockRoute ? "block" : "item");
        report.put("bridgeKind", "real_echo_module_runtime");
        String moduleClass = moduleClassFor(ns);
        report.put("nativeModuleClass", moduleClass);
        if (!moduleClass.isBlank()) {
            Map<String, Object> activation = invokeModuleActivationOnce(ns, moduleClass);
            report.put("nativeActivationBound", !activation.isEmpty());
            report.put("nativeActivationValue", NativeLoaderRuntimeReflectionSupport.valueSummary(activation));
        } else {
            report.put("nativeActivationBound", false);
        }
        if (!moduleSurface(ns, safePath).isBlank()
                || hasAny(ns, "echoterminal", "echoindex", "echolens", "echoholomap", "echowiki", "signalos")) {
            Map<String, Object> infoRuntime = invokeInfoModuleRuntime(ns, player);
            report.put("infoRuntimeBound", integer(infoRuntime.get("successfulCallCount")) > 0
                    || Boolean.TRUE.equals(infoRuntime.get("screenOpened")));
            report.put("infoRuntimeSummary", infoModuleSummary(ns, infoRuntime));
        }
        NativeLoaderModuleRuntimeServices.invoke(moduleRuntimeServicesContext, ns, report, level, player);
        String requestedId = ns + ":" + safePath;
        String resolvedContent = blockRoute
                ? context.resolveBlockId().apply(requestedId)
                : context.resolveItemId().apply(requestedId);
        report.put("registeredContentResolved", !resolvedContent.isBlank());
        report.put("registeredContentId", resolvedContent);
        int callCount = NativeLoaderRuntimeReflectionSupport.successfulCalls(report);
        report.put("successfulCallCount", callCount);
        report.put("runtimeBound", Boolean.TRUE.equals(report.get("nativeActivationBound"))
                || Boolean.TRUE.equals(report.get("infoRuntimeBound"))
                || callCount > 0);
        return report;
    }

    Map<String, Object> invokeModuleActivation(String moduleId, String className) {
        return NativeLoaderModuleActivationInvoker.invoke(profile, moduleId, className);
    }

    void invokeBetaModuleActivation(String namespace) {
        String className = moduleClassFor(namespace);
        if (className.isBlank() || !NativeLoaderProductFeedbackSupport.oneShot("module-activation:" + className)) {
            return;
        }
        invokeModuleActivation(namespace, className);
    }

    Map<String, Object> invokeInfoModuleRuntime(String namespace, Object player) {
        return NativeLoaderInfoModuleRuntimeInvoker.invoke(infoRuntimeContext, namespace, player);
    }

    String infoModuleSummary(String namespace, Map<String, Object> report) {
        String summary = String.valueOf(report.getOrDefault("summary", ""));
        if (!summary.isBlank() && !"null".equals(summary)) {
            return summary;
        }
        return infoDisplayName(namespace) + " real runtime route executed.";
    }

    String genericModuleSummary(String namespace, String path, boolean blockRoute) {
        String module = moduleDisplayName(namespace);
        String action = productProfile.actionKey(path);
        String subject = blockRoute ? "block" : "item";
        return module + " " + subject + " routed through discovered " + lowerContentId(namespace)
                + " content for " + action + ".";
    }

    boolean invokeProductHook(Map<String, Object> report, String role) {
        String normalizedRole = lowerContentId(role);
        boolean invoked = false;
        for (NativeIntegrationHook hook : profile.nativeIntegrationHooks()) {
            if (!normalizedRole.equals(lowerContentId(hook.role()))) {
                continue;
            }
            String reportKey = hook.reportKey() == null || hook.reportKey().isBlank()
                    ? serviceKey(hook.className(), hook.methodName())
                    : hook.reportKey();
            invoked |= NativeLoaderRuntimeReflectionSupport.invokeStaticNoArg(
                    report,
                    reportKey,
                    hook.className(),
                    hook.methodName()
            );
        }
        return invoked;
    }

    String moduleClassFor(String namespace) {
        return NativeLoaderModuleClassResolver.resolve(profile, namespace);
    }

    private Map<String, Object> invokeModuleActivationOnce(String namespace, String className) {
        if (className == null || className.isBlank()) {
            return Map.of();
        }
        String key = "module-activation:" + lowerContentId(namespace) + ":" + className;
        if (!NativeLoaderProductFeedbackSupport.oneShot(key)) {
            Map<String, Object> already = new LinkedHashMap<>();
            already.put("moduleClass", className);
            already.put("alreadyActivated", true);
            return already;
        }
        Map<String, Object> activation = invokeModuleActivation(namespace, className);
        if (activation.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copy = new LinkedHashMap<>(activation);
        copy.put("moduleClass", className);
        return copy;
    }

    private String moduleSurface(String namespace, String path) {
        return NativeLoaderModuleSurfaceFlow.surface(namespace, path, context.moduleSurfaceContext());
    }

    private String moduleDisplayName(String namespace) {
        String id = lowerContentId(namespace);
        String known = infoDisplayName(id);
        if (!"ECHO".equals(known)) {
            return known;
        }
        return titleFromId(id);
    }

    private String infoDisplayName(String namespace) {
        String id = lowerContentId(namespace);
        if (lowerContentId(profile.namespace()).equals(id)) {
            return profile.nativeGameplayDisplayName();
        }
        return profile.nativeModuleDisplayNames().getOrDefault(id, "ECHO");
    }

    private static String serviceKey(String className, String methodName) {
        String safe = (className + "." + methodName).replaceAll("[^A-Za-z0-9_]+", "_");
        return safe.length() <= 80 ? safe : safe.substring(Math.max(0, safe.length() - 80));
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String titleFromId(String id) {
        int separator = id.indexOf(':');
        String path = separator >= 0 ? id.substring(separator + 1) : id;
        String[] words = path.replace('/', '_').replace('-', '_').split("_+");
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.isEmpty() ? id : title.toString();
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = lowerContentId(value);
        for (String needle : needles) {
            if (!lowerContentId(needle).isBlank() && haystack.contains(lowerContentId(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    record Context(
            NativeLoaderModuleSurfaceFlow.Context moduleSurfaceContext,
            Function<String, String> runtimeClass,
            Function<String, String> resolveItemId,
            Function<String, String> resolveBlockId
    ) {
    }
}
