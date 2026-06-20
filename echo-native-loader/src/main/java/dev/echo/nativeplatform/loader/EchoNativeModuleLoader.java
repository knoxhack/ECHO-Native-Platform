package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeLifecyclePhase;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleRecord;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class EchoNativeModuleLoader {
    private static final String RELEASE_ENTRYPOINT_POLICY = "release_requires_echo_native_module_entrypoint";
    private static final String LEGACY_COMPATIBILITY_SHIM_POLICY = "non_release_legacy_activate_native_map_compatibility_shim";
    private static final String LEGACY_ID_FIELD = "\"legacyId\"";

    private final ClassLoader parentClassLoader;
    private final Map<EchoNativeModuleLoadResult, LoadedModuleHandle> loadedHandles = new IdentityHashMap<>();

    public EchoNativeModuleLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public EchoNativeModuleLoader(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader == null ? EchoNativeModuleLoader.class.getClassLoader() : parentClassLoader;
    }

    public EchoNativeModuleLoadResult load(EchoNativeAddonDescriptor addonDescriptor, EchoNativeServiceRegistry serviceRegistry) {
        return load(addonDescriptor, serviceRegistry, Map.of());
    }

    public EchoNativeModuleLoadResult loadRelease(EchoNativeAddonDescriptor addonDescriptor, EchoNativeServiceRegistry serviceRegistry) {
        return loadRelease(addonDescriptor, serviceRegistry, Map.of());
    }

    public EchoNativeModuleLoadResult loadRelease(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeServiceRegistry serviceRegistry,
            EchoNativeRuntimeSide hostSide
    ) {
        return loadRelease(addonDescriptor, serviceRegistry, Map.of(), hostSide);
    }

    public EchoNativeModuleLoadResult loadRelease(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeServiceRegistry serviceRegistry,
            Map<String, EchoNativeAddonDescriptor> availableModules
    ) {
        return load(addonDescriptor, serviceRegistry, availableModules, EchoNativeModuleLoadPolicy.RELEASE, EchoNativeRuntimeSide.UNKNOWN);
    }

    public EchoNativeModuleLoadResult loadRelease(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeServiceRegistry serviceRegistry,
            Map<String, EchoNativeAddonDescriptor> availableModules,
            EchoNativeRuntimeSide hostSide
    ) {
        return load(addonDescriptor, serviceRegistry, availableModules, EchoNativeModuleLoadPolicy.RELEASE, hostSide);
    }

    public EchoNativeModuleLoadResult load(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeServiceRegistry serviceRegistry,
            Map<String, EchoNativeAddonDescriptor> availableModules
    ) {
        return load(addonDescriptor, serviceRegistry, availableModules, EchoNativeModuleLoadPolicy.DEVELOPMENT, EchoNativeRuntimeSide.UNKNOWN);
    }

    private EchoNativeModuleLoadResult load(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeServiceRegistry serviceRegistry,
            Map<String, EchoNativeAddonDescriptor> availableModules,
            EchoNativeModuleLoadPolicy loadPolicy,
            EchoNativeRuntimeSide hostSide
    ) {
        EchoNativeModuleDescriptor descriptor = EchoNativeModuleDescriptor.fromAddon(addonDescriptor);
        registerModuleAliases(addonDescriptor, serviceRegistry);
        EchoNativeRuntimeSide checkedHostSide = hostSide == null ? EchoNativeRuntimeSide.UNKNOWN : hostSide;
        List<EchoNativeLifecyclePhase> phases = new ArrayList<>();
        List<EchoNativeLifecycleRecord> lifecyclePhaseHistory = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> mutations = new ArrayList<>();
        String loadedClassName = "";
        String loadedClassLoaderName = "";
        boolean loadedByModuleClassLoader = false;
        String constructedEntrypointClassName = "";
        EchoNativeLoadStatus status = EchoNativeLoadStatus.DISCOVERED;
        EchoNativeLifecyclePhase[] activePhase = new EchoNativeLifecyclePhase[] { EchoNativeLifecyclePhase.DISCOVER };

        EchoNativeModuleLoadContext context = new EchoNativeModuleLoadContext(
                descriptor,
                serviceRegistry,
                Map.of("loader", "echo-native-loader")
        );

        recordLifecycle(
                phases,
                lifecyclePhaseHistory,
                EchoNativeLifecyclePhase.DISCOVER,
                EchoNativeLoadStatus.DISCOVERED,
                "Descriptor discovered: " + descriptor.id(),
                List.of());
        if (!descriptor.hasEntrypoint()) {
            String diagnostic = "Module " + descriptor.id() + " has no native entrypoint.";
            diagnostics.add(diagnostic);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.LOAD_CLASSES,
                    EchoNativeLoadStatus.UNSUPPORTED,
                    diagnostic,
                    List.of(diagnostic));
            return result(
                    descriptor,
                    EchoNativeLoadStatus.UNSUPPORTED,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
        }
        if (descriptor.compatibilityClasspathFallback()) {
            diagnostics.add("Module " + descriptor.id()
                    + " is using compatibility native classpath fallback; this is not release classpath truth.");
        }
        if (loadPolicy.release() && releaseRejectsSide(descriptor, checkedHostSide)) {
            String diagnostic = releaseSideDiagnostic(descriptor, checkedHostSide);
            diagnostics.add(diagnostic);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.RESOLVE,
                    EchoNativeLoadStatus.FAILED,
                    diagnostic,
                    List.of(diagnostic));
            return result(
                    descriptor,
                    EchoNativeLoadStatus.FAILED,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
        }
        if (loadPolicy.release() && releaseRejectsClasspath(descriptor)) {
            String diagnostic = releaseClasspathDiagnostic(descriptor);
            diagnostics.add(diagnostic);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.LOAD_CLASSES,
                    EchoNativeLoadStatus.FAILED,
                    diagnostic,
                    List.of(diagnostic));
            return result(
                    descriptor,
                    EchoNativeLoadStatus.FAILED,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
        }
        if (descriptor.classpath().isEmpty()) {
            String diagnostic = "Module " + descriptor.id()
                    + " declares native entrypoint " + descriptor.entrypoint()
                    + " but no native classpath could be resolved. Add access.nativeClasspath, use "
                    + EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN
                    + ", or build/package the module before release loading.";
            diagnostics.add(diagnostic);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.LOAD_CLASSES,
                    EchoNativeLoadStatus.FAILED,
                    diagnostic,
                    List.of(diagnostic));
            return result(
                    descriptor,
                    EchoNativeLoadStatus.FAILED,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
        }

        EchoNativeModuleClassLoader classLoader = new EchoNativeModuleClassLoader(descriptor.classpath(), parentClassLoader);
        boolean retainClassLoader = false;
        try {
            activePhase[0] = EchoNativeLifecyclePhase.RESOLVE;
            resolveDependencies(descriptor, availableModules, context, diagnostics);
            if (!context.missingDependencies().isEmpty()) {
                diagnostics.add("Missing required dependencies: " + String.join(", ", context.missingDependencies()));
                recordLifecycle(
                        phases,
                        lifecyclePhaseHistory,
                        EchoNativeLifecyclePhase.RESOLVE,
                        EchoNativeLoadStatus.FAILED,
                        "Dependency resolution failed for " + descriptor.id(),
                        diagnostics);
                return result(
                        descriptor,
                        EchoNativeLoadStatus.FAILED,
                        phases,
                        lifecyclePhaseHistory,
                        loadedClassName,
                        loadedClassLoaderName,
                        loadedByModuleClassLoader,
                        constructedEntrypointClassName,
                        context,
                        mutations,
                        diagnostics
                );
            }
            status = EchoNativeLoadStatus.RESOLVED;
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.RESOLVE,
                    EchoNativeLoadStatus.RESOLVED,
                    "Resolved " + context.resolvedDependencies().size()
                            + " dependency/dependencies and planned " + descriptor.classpath().size() + " classpath entry/entries.",
                    List.of());

            activePhase[0] = EchoNativeLifecyclePhase.LOAD_CLASSES;
            Class<?> type = classLoader.loadClass(descriptor.entrypoint());
            loadedClassName = type.getName();
            loadedClassLoaderName = type.getClassLoader() == null
                    ? "bootstrap"
                    : type.getClassLoader().getClass().getName();
            loadedByModuleClassLoader = type.getClassLoader() == classLoader;
            status = EchoNativeLoadStatus.LOADED;
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.LOAD_CLASSES,
                    EchoNativeLoadStatus.LOADED,
                    "Loaded " + loadedClassName + " with " + loadedClassLoaderName + ".",
                    List.of());

            activePhase[0] = EchoNativeLifecyclePhase.CONSTRUCT;
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object rawEntrypoint = constructor.newInstance();
            EchoNativeModuleEntrypoint entrypoint;
            String lifecycleEntrypointClassName;
            if (rawEntrypoint instanceof EchoNativeModuleEntrypoint nativeEntrypoint) {
                entrypoint = nativeEntrypoint;
                lifecycleEntrypointClassName = entrypoint.getClass().getName();
            } else {
                String diagnostic = "Native entrypoint " + loadedClassName
                        + " does not implement EchoNativeModuleEntrypoint.";
                if (isNonReleaseLegacyActivateNativeCompatibilityShim(type)) {
                    diagnostic += " Legacy activateNative(Map) adapters are not accepted by release loading; "
                            + "compatibility shim policy " + LEGACY_COMPATIBILITY_SHIM_POLICY
                            + " is non-release only. Implement EchoNativeModuleEntrypoint lifecycle methods instead.";
                }
                diagnostics.add(diagnostic);
                recordLifecycle(
                        phases,
                        lifecyclePhaseHistory,
                        EchoNativeLifecyclePhase.CONSTRUCT,
                        EchoNativeLoadStatus.FAILED,
                        diagnostic,
                        List.of(diagnostic));
                return result(
                        descriptor,
                        EchoNativeLoadStatus.FAILED,
                        phases,
                        lifecyclePhaseHistory,
                        loadedClassName,
                        loadedClassLoaderName,
                        loadedByModuleClassLoader,
                        constructedEntrypointClassName,
                        context,
                        mutations,
                        diagnostics
                );
            }
            constructedEntrypointClassName = rawEntrypoint.getClass().getName();

            activePhase[0] = EchoNativeLifecyclePhase.DISCOVER;
            entrypoint.discover(context);
            collectMutations(context, mutations);
            activePhase[0] = EchoNativeLifecyclePhase.RESOLVE;
            entrypoint.resolve(context);
            collectMutations(context, mutations);
            activePhase[0] = EchoNativeLifecyclePhase.LOAD_CLASSES;
            entrypoint.loadClasses(context);
            collectMutations(context, mutations);
            activePhase[0] = EchoNativeLifecyclePhase.CONSTRUCT;
            entrypoint.construct(context);
            collectMutations(context, mutations);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.CONSTRUCT,
                    EchoNativeLoadStatus.LOADED,
                    "Constructed entrypoint " + constructedEntrypointClassName
                            + " with lifecycle handler " + lifecycleEntrypointClassName + ".",
                    List.of());

            int serviceCountBefore = serviceRegistry.servicesForModule(descriptor.id()).size();
            activePhase[0] = EchoNativeLifecyclePhase.REGISTER_SERVICES;
            entrypoint.registerServices(context);
            collectMutations(context, mutations);
            int serviceCountAfter = serviceRegistry.servicesForModule(descriptor.id()).size();
            if (serviceCountAfter > serviceCountBefore) {
                status = EchoNativeLoadStatus.REGISTERED;
            }
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.REGISTER_SERVICES,
                    serviceCountAfter > serviceCountBefore ? EchoNativeLoadStatus.REGISTERED : EchoNativeLoadStatus.RESOLVED,
                    "Registered " + Math.max(0, serviceCountAfter - serviceCountBefore) + " service(s).",
                    List.of());

            int contentCountBefore = contentServiceCount(serviceRegistry.servicesForModule(descriptor.id()));
            activePhase[0] = EchoNativeLifecyclePhase.REGISTER_CONTENT;
            entrypoint.registerContent(context);
            collectMutations(context, mutations);
            int contentCountAfter = contentServiceCount(serviceRegistry.servicesForModule(descriptor.id()));
            if (contentCountAfter > contentCountBefore) {
                status = EchoNativeLoadStatus.REGISTERED;
            }
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.REGISTER_CONTENT,
                    contentCountAfter > contentCountBefore ? EchoNativeLoadStatus.REGISTERED : EchoNativeLoadStatus.RESOLVED,
                    "Registered " + Math.max(0, contentCountAfter - contentCountBefore) + " content service(s).",
                    List.of());
            activePhase[0] = EchoNativeLifecyclePhase.COMMON_SETUP;
            entrypoint.commonSetup(context);
            collectMutations(context, mutations);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.COMMON_SETUP,
                    status,
                    "Common setup completed.",
                    List.of());
            activePhase[0] = EchoNativeLifecyclePhase.CLIENT_SETUP;
            entrypoint.clientSetup(context);
            collectMutations(context, mutations);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.CLIENT_SETUP,
                    status,
                    "Client setup completed.",
                    List.of());
            activePhase[0] = EchoNativeLifecyclePhase.SERVER_SETUP;
            entrypoint.serverSetup(context);
            collectMutations(context, mutations);
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.SERVER_SETUP,
                    status,
                    "Server setup completed.",
                    List.of());
            activePhase[0] = EchoNativeLifecyclePhase.READY;
            entrypoint.ready(context);
            collectMutations(context, mutations);
            boolean hasMutated = context.mutationReceipts().stream()
                    .anyMatch(EchoNativeMutationReceipt::mutated);
            if (hasMutated) {
                status = EchoNativeLoadStatus.MUTATED;
            }
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    EchoNativeLifecyclePhase.READY,
                    status,
                    "Module ready with " + serviceRegistry.servicesForModule(descriptor.id()).size()
                            + " registered service/content record(s).",
                    List.of());

            EchoNativeModuleLoadResult loadResult = result(
                    descriptor,
                    status,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
            loadedHandles.put(loadResult, new LoadedModuleHandle(classLoader, entrypoint));
            retainClassLoader = true;
            return loadResult;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            diagnostics.add(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    activePhase[0],
                    EchoNativeLoadStatus.FAILED,
                    "Lifecycle phase " + activePhase[0].name() + " failed for " + descriptor.id() + ".",
                    diagnostics);
            return result(
                    descriptor,
                    EchoNativeLoadStatus.FAILED,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
        } catch (Throwable exception) {
            diagnostics.add(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            recordLifecycle(
                    phases,
                    lifecyclePhaseHistory,
                    activePhase[0],
                    EchoNativeLoadStatus.FAILED,
                    "Lifecycle phase " + activePhase[0].name() + " failed for " + descriptor.id() + ".",
                    diagnostics);
            return result(
                    descriptor,
                    EchoNativeLoadStatus.FAILED,
                    phases,
                    lifecyclePhaseHistory,
                    loadedClassName,
                    loadedClassLoaderName,
                    loadedByModuleClassLoader,
                    constructedEntrypointClassName,
                    context,
                    mutations,
                    diagnostics
            );
        } finally {
            if (!retainClassLoader) {
                closeQuietly(classLoader);
            }
        }
    }

    private static void registerModuleAliases(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeServiceRegistry serviceRegistry
    ) {
        for (String alias : moduleAliases(addonDescriptor)) {
            serviceRegistry.registerModuleAlias(alias, addonDescriptor.id());
        }
    }

    private static List<String> moduleAliases(EchoNativeAddonDescriptor addonDescriptor) {
        if (addonDescriptor == null || addonDescriptor.descriptorPath() == null
                || !Files.isRegularFile(addonDescriptor.descriptorPath())) {
            return List.of();
        }
        try {
            String json = Files.readString(addonDescriptor.descriptorPath(), StandardCharsets.UTF_8);
            if (!json.contains(LEGACY_ID_FIELD)) {
                return List.of();
            }
            LinkedHashSet<String> aliases = new LinkedHashSet<>();
            int searchStart = 0;
            while (searchStart >= 0 && searchStart < json.length()) {
                int legacyField = json.indexOf(LEGACY_ID_FIELD, searchStart);
                if (legacyField < 0) {
                    break;
                }
                String block = boundedObjectBlock(json, legacyField);
                String legacyId = captureJsonStringField(block, "legacyId");
                String replacementId = captureJsonStringField(block, "replacementId");
                String scope = captureJsonStringField(block, "scope");
                boolean moduleScope = scope.isBlank() || "module_id".equals(scope);
                boolean canonicalReplacement = replacementId.isBlank() || addonDescriptor.id().equals(replacementId);
                if (moduleScope && canonicalReplacement && !legacyId.isBlank()) {
                    aliases.add(legacyId);
                }
                searchStart = legacyField + LEGACY_ID_FIELD.length();
            }
            aliases.remove(addonDescriptor.id());
            return List.copyOf(aliases);
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static String boundedObjectBlock(String json, int anchor) {
        int start = json.lastIndexOf('{', anchor);
        int end = json.indexOf('}', anchor);
        if (start < 0) {
            start = Math.max(0, anchor - 256);
        }
        if (end < start) {
            end = Math.min(json.length() - 1, anchor + 1024);
        }
        return json.substring(start, Math.min(json.length(), end + 1));
    }

    private static String captureJsonStringField(String text, String fieldName) {
        String needle = "\"" + fieldName + "\"";
        int field = text.indexOf(needle);
        if (field < 0) {
            return "";
        }
        int colon = skipWhitespace(text, field + needle.length());
        if (colon >= text.length() || text.charAt(colon) != ':') {
            return "";
        }
        int valueStart = skipWhitespace(text, colon + 1);
        if (valueStart >= text.length() || text.charAt(valueStart) != '"') {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = valueStart + 1; index < text.length(); index++) {
            char current = text.charAt(index);
            if (escaped) {
                value.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }
        return "";
    }

    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isNonReleaseLegacyActivateNativeCompatibilityShim(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (!"activateNative".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            if (Map.class.isAssignableFrom(method.getParameterTypes()[0])) {
                return true;
            }
        }
        for (Method method : type.getDeclaredMethods()) {
            if (!"activateNative".equals(method.getName()) || method.getParameterCount() != 1) {
                continue;
            }
            if (Map.class.isAssignableFrom(method.getParameterTypes()[0])) {
                return true;
            }
        }
        return false;
    }

    private static boolean releaseRejectsClasspath(EchoNativeModuleDescriptor descriptor) {
        return descriptor.compatibilityClasspathFallback() || descriptor.inferredClasspathRequested();
    }

    private static boolean releaseRejectsSide(EchoNativeModuleDescriptor descriptor, EchoNativeRuntimeSide hostSide) {
        EchoNativeRuntimeSide moduleSide = descriptor.side() == null ? EchoNativeRuntimeSide.UNKNOWN : descriptor.side();
        EchoNativeRuntimeSide checkedHostSide = hostSide == null ? EchoNativeRuntimeSide.UNKNOWN : hostSide;
        if (moduleSide == EchoNativeRuntimeSide.COMMON || moduleSide == EchoNativeRuntimeSide.UNKNOWN) {
            return false;
        }
        if (checkedHostSide == EchoNativeRuntimeSide.UNKNOWN || checkedHostSide == EchoNativeRuntimeSide.COMMON) {
            return false;
        }
        return moduleSide != checkedHostSide;
    }

    private static String releaseSideDiagnostic(EchoNativeModuleDescriptor descriptor, EchoNativeRuntimeSide hostSide) {
        return "Release loading rejected module " + descriptor.id()
                + " because descriptor side " + sideName(descriptor.side())
                + " is not compatible with host side " + hostSide.name()
                + ". Package or route the module through a matching client/server/common lane.";
    }

    private static String sideName(EchoNativeRuntimeSide side) {
        return (side == null ? EchoNativeRuntimeSide.UNKNOWN : side).name();
    }

    private static String releaseClasspathDiagnostic(EchoNativeModuleDescriptor descriptor) {
        if (descriptor.inferredClasspathRequested()) {
            return "Release loading rejected module " + descriptor.id()
                    + " because its nativeClasspath uses " + EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN
                    + ". Package explicit module artifacts and point access.nativeClasspath at those artifacts.";
        }
        return "Release loading rejected module " + descriptor.id()
                + " because it would use compatibility native classpath fallback. Package explicit module artifacts and declare access.nativeClasspath.";
    }

    public EchoNativeLoadStatus shutdown(EchoNativeModuleLoadResult previousResult, EchoNativeServiceRegistry serviceRegistry) {
        if (previousResult == null || previousResult.loadedClassName().isBlank()) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        EchoNativeModuleDescriptor descriptor = previousResult.descriptor();
        LoadedModuleHandle handle = loadedHandles.remove(previousResult);
        if (handle == null) {
            return EchoNativeLoadStatus.UNSUPPORTED;
        }
        EchoNativeModuleLoadContext context = new EchoNativeModuleLoadContext(
                descriptor,
                serviceRegistry,
                Map.of("loader", "echo-native-loader", "phase", "shutdown")
        );
        try {
            handle.entrypoint().shutdown(context);
            return EchoNativeLoadStatus.MUTATED;
        } catch (RuntimeException exception) {
            return EchoNativeLoadStatus.FAILED;
        } finally {
            closeQuietly(handle.classLoader());
        }
    }

    private static void closeQuietly(EchoNativeModuleClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            classLoader.close();
        } catch (IOException ignored) {
            // Shutdown should report lifecycle status, not classloader close noise.
        }
    }

    private record LoadedModuleHandle(
            EchoNativeModuleClassLoader classLoader,
            EchoNativeModuleEntrypoint entrypoint
    ) {
    }

    private static void resolveDependencies(
            EchoNativeModuleDescriptor descriptor,
            Map<String, EchoNativeAddonDescriptor> availableModules,
            EchoNativeModuleLoadContext context,
            List<String> diagnostics
    ) {
        for (String required : descriptor.requires()) {
            if (availableModules.containsKey(required)) {
                context.resolveDependency(required);
            } else {
                context.missingDependency(required);
                diagnostics.add("Required dependency not available: " + required);
            }
        }
        for (String optional : descriptor.optional()) {
            if (availableModules.containsKey(optional)) {
                context.resolveDependency(optional);
            }
        }
    }

    private static void collectMutations(EchoNativeModuleLoadContext context, List<Map<String, Object>> mutations) {
        for (Map<String, Object> mutation : context.mutations()) {
            if (!mutations.contains(mutation)) {
                mutations.add(mutation);
            }
        }
    }

    private static int contentServiceCount(List<EchoNativeRegisteredService> services) {
        int count = 0;
        for (EchoNativeRegisteredService service : services) {
            if (service.serviceId().startsWith("content.")) {
                count++;
            }
        }
        return count;
    }

    private static void recordLifecycle(
            List<EchoNativeLifecyclePhase> phases,
            List<EchoNativeLifecycleRecord> lifecyclePhaseHistory,
            EchoNativeLifecyclePhase phase,
            EchoNativeLoadStatus status,
            String detail,
            List<String> failures
    ) {
        if (!phases.contains(phase)) {
            phases.add(phase);
        }
        lifecyclePhaseHistory.add(new EchoNativeLifecycleRecord(
                phase,
                status,
                detail == null ? "" : detail,
                status == EchoNativeLoadStatus.FAILED
                        || status == EchoNativeLoadStatus.UNSUPPORTED
                        || (failures != null && !failures.isEmpty()),
                failures == null ? List.of() : List.copyOf(failures)
        ));
    }

    private static EchoNativeModuleLoadResult result(
            EchoNativeModuleDescriptor descriptor,
            EchoNativeLoadStatus status,
            List<EchoNativeLifecyclePhase> phases,
            List<EchoNativeLifecycleRecord> lifecyclePhaseHistory,
            String loadedClassName,
            String loadedClassLoaderName,
            boolean loadedByModuleClassLoader,
            String constructedEntrypointClassName,
            EchoNativeModuleLoadContext context,
            List<Map<String, Object>> mutations,
            List<String> diagnostics
    ) {
        List<EchoNativeRegisteredService> services = context.serviceRegistry().servicesForModule(descriptor.id());
        return new EchoNativeModuleLoadResult(
                descriptor,
                status,
                List.copyOf(phases),
                List.copyOf(lifecyclePhaseHistory),
                loadedClassName,
                loadedClassLoaderName,
                loadedByModuleClassLoader,
                constructedEntrypointClassName,
                context.resolvedDependencies(),
                context.missingDependencies(),
                services,
                context.mutationReceipts(),
                List.copyOf(mutations),
                List.copyOf(diagnostics)
        );
    }

    public static Map<String, Object> toReport(EchoNativeModuleLoadResult result) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", result.descriptor().id());
        report.put("status", result.status().name());
        report.put("entrypoint", result.descriptor().entrypoint());
        report.put("side", sideName(result.descriptor().side()));
        report.put("nativeClasspathDeclared", result.descriptor().nativeClasspathDeclared());
        report.put("inferredClasspathRequested", result.descriptor().inferredClasspathRequested());
        report.put("compatibilityClasspathFallback", result.descriptor().compatibilityClasspathFallback());
        report.put("classpath", result.descriptor().classpath().stream()
                .map(path -> path.toString().replace('\\', '/'))
                .toList());
        report.put("loadedClassName", result.loadedClassName());
        report.put("loadedClassLoaderName", result.loadedClassLoaderName());
        report.put("loadedByModuleClassLoader", result.loadedByModuleClassLoader());
        report.put("constructedEntrypointClassName", result.constructedEntrypointClassName());
        report.put("entrypointPolicy", RELEASE_ENTRYPOINT_POLICY);
        report.put("legacyCompatibilityShimPolicy", LEGACY_COMPATIBILITY_SHIM_POLICY);
        report.put("legacyCompatibilityShimReleaseAllowed", false);
        report.put("resolvedDependencies", result.resolvedDependencies());
        report.put("missingDependencies", result.missingDependencies());
        report.put("phases", result.phases().stream().map(Enum::name).toList());
        report.put("lifecyclePhaseHistory", result.lifecyclePhaseHistory().stream()
                .map(EchoNativeLifecycleRecord::toReport)
                .toList());
        report.put("registeredServices", result.registeredServices().stream().map(service -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("moduleId", service.moduleId());
            item.put("serviceId", service.serviceId());
            item.put("implementationClass", service.implementationClass());
            item.put("surfaces", service.surfaces());
            return item;
        }).toList());
        report.put("moduleLifecycleRecords", result.mutations());
        report.put("mutations", result.mutations());
        report.put("typedMutationReceipts", result.mutationReceipts().stream()
                .map(EchoNativeMutationReceipt::toReport)
                .toList());
        report.put("typedMutationReceiptCount", result.mutationReceipts().size());
        report.put("typedHostMutationReceiptCount", result.mutationReceipts().stream()
                .filter(EchoNativeMutationReceipt::mutated)
                .count());
        report.put("metadataOnlyMutationClaimRejected", result.mutations().stream()
                .anyMatch(m -> EchoNativeLoadStatus.MUTATED.name().equals(String.valueOf(m.get("status"))))
                && result.mutationReceipts().stream().noneMatch(EchoNativeMutationReceipt::mutated));
        report.put("diagnostics", result.diagnostics());
        report.put("activationClaimAllowed", result.loaded() && result.registered());
        report.put("nativeHostMutationClaimAllowed", false);
        report.put("nativeHostMutationClaimBlocker",
                "Module lifecycle records are not host mutation evidence; only NativeLoaderMutationLedger records may unlock this claim.");
        report.put("gameplayReadyClaimAllowed", false);
        report.put("successRules", List.of(
                "DISCOVERED = found module metadata",
                "RESOLVED = dependency/classpath planned",
                "LOADED = module class actually loaded",
                "REGISTERED = services/content registered",
                "MUTATED = runtime/game state changed",
                "FAILED = attempted and failed",
                "UNSUPPORTED = not implemented"
        ));
        return report;
    }
}
