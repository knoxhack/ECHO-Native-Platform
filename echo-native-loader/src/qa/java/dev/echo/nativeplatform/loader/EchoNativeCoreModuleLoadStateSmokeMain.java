package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRegisteredService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EchoNativeCoreModuleLoadStateSmokeMain {
    private static final List<String> TARGET_ORDER = List.of(
            "echocore",
            "echoplatformcore",
            "echoadaptercore",
            "echopackcore",
            "echoschemacore",
            "echovalidationcore",
            "echometadatacore",
            "echomodulegraph",
            "echohealthcore",
            "echobridgecore",
            "echocontentcore",
            "echoassetcore",
            "echoreportcore",
            "echonetcore",
            "echodatacore",
            "echoruntimeguard",
            "echoinputcore",
            "echoscreencore",
            "echorendercore",
            "echothemecore",
            "echosoundcore",
            "echohudcore",
            "echonotificationcore",
            "echoguidecore",
            "echocodexcore",
            "echolorecore",
            "echowiki",
            "echoindex",
            "echolens",
            "echoholomap",
            "echoterminal",
            "echomissioncore",
            "echoworldcore",
            "echorecovery"
    );
    private static final List<TargetModule> SUPPORT_MODULES = List.of(
            new TargetModule("echoagentcore", "addons/echoagentcore", "com.knoxhack.echo.agentcore.EchoAgentCoreNativeModule")
    );

    private EchoNativeCoreModuleLoadStateSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("..").toAbsolutePath().normalize();
        Path classpathRoot = args.length > 1
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : defaultRootBuildDirectory();
        Path output = args.length > 2
                ? Path.of(args[2]).toAbsolutePath().normalize()
                : Path.of("build/native-core-module-load-state/native-core-module-load-state.json")
                        .toAbsolutePath()
                        .normalize();
        String scope = args.length > 3 ? args[3] : "focused-core";
        String classpathMode = args.length > 4 ? args[4] : "classes";

        List<TargetModule> targets = targets(repoRoot, scope);
        List<TargetModule> descriptorModules = descriptorModules(repoRoot, scope, targets);
        List<String> classpath = sharedNativeClasspath(repoRoot, classpathRoot, descriptorModules, classpathMode);
        Map<String, EchoNativeAddonDescriptor> descriptors = new LinkedHashMap<>();
        for (TargetModule module : descriptorModules) {
            descriptors.put(module.moduleId(), readDescriptor(repoRoot, module, classpath));
        }

        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        EchoNativeModuleLoader loader = new EchoNativeModuleLoader();
        EchoNativeLoadedModuleStateStore stateStore = new EchoNativeLoadedModuleStateStore();
        Path stateDirectory = output.getParent().resolve("loaded-modules");
        List<Map<String, Object>> modules = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (TargetModule target : targets) {
            EchoNativeAddonDescriptor descriptor = descriptors.get(target.moduleId());
            EchoNativeModuleLoadResult result = loader.load(descriptor, serviceRegistry, descriptors);
            EchoNativeLoadedModuleStateStore.StoredState storedState = stateStore.write(stateDirectory, result);
            Map<String, Object> moduleReport = EchoNativeModuleLoader.toReport(result);
            moduleReport.put("loadedModuleStatePath", storedState.normalizedPath());
            moduleReport.put("loadedModuleState", storedState.state());
            moduleReport.put("targetStatusBeforeLoad", "ECHO_NATIVE_MODULE_ENTRYPOINT_REQUIRED");
            moduleReport.put("targetStatusAfterLoad", result.status().name());
            modules.add(moduleReport);

            try {
                verifyTarget(target, result);
            } catch (AssertionError error) {
                failed.add(target.moduleId() + ": " + error.getMessage());
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.core_module_load_state_smoke.v1");
        report.put("runtimeLane", "Native Loader");
        report.put("laneRole", "primary future mod loader");
        report.put("fallbackLane", "NeoForge compatibility backend");
        report.put("parityLane", "Standalone Runtime parity/runtime harness");
        report.put("scope", scope);
        report.put("classpathMode", classpathMode);
        report.put("repoRoot", path(repoRoot));
        report.put("classpathRoot", path(classpathRoot));
        report.put("targetModules", targets.stream().map(TargetModule::moduleId).toList());
        report.put("targetModuleCount", targets.size());
        report.put("supportModules", "focused-core".equals(scope)
                ? SUPPORT_MODULES.stream().map(TargetModule::moduleId).toList()
                : List.of());
        report.put("sharedNativeClasspath", classpath);
        report.put("loadedModuleStateDirectory", path(stateDirectory));
        report.put("modules", modules);
        report.put("registeredServiceCount", serviceRegistry.registeredServices().size());
        report.put("uniqueRegisteredServiceCount", serviceRegistry.registeredServices().size());
        report.put("moduleRegisteredRecordCount", modules.stream()
                .map(EchoNativeJson::asObject)
                .mapToInt(module -> registeredServiceCount(module))
                .sum());
        report.put("loadedByModuleClassLoaderCount", modules.stream()
                .map(EchoNativeJson::asObject)
                .filter(module -> Boolean.TRUE.equals(module.get("loadedByModuleClassLoader")))
                .count());
        report.put("failedModuleCount", failed.size());
        report.put("status", failed.isEmpty() ? "MUTATED" : "FAILED");
        report.put("failures", failed);
        report.put("claimBoundary", Map.of(
                "activationClaimAllowed", failed.isEmpty(),
                "nativeLifecycleMutationClaimAllowed", failed.isEmpty(),
                "gameplayReadyClaimAllowed", false,
                "reason", "Core module lifecycle loading must execute real EchoNativeModuleEntrypoint mutation evidence; gameplay readiness still requires live runtime host evidence."
        ));

        Files.createDirectories(output.getParent());
        Files.writeString(output, EchoNativeJson.write(report), StandardCharsets.UTF_8);
        require(failed.isEmpty(), "Core module load-state failures: " + failed);
        System.out.println("native " + scope + " module load-state smoke PASS " + output);
    }

    private static int registeredServiceCount(Map<String, Object> module) {
        Object value = module.get("registeredServices");
        return value instanceof List<?> services ? services.size() : 0;
    }

    private static void verifyTarget(TargetModule target, EchoNativeModuleLoadResult result) {
        require(result.loaded(), target.moduleId() + " did not load.");
        require(result.registered(), target.moduleId() + " did not register services/content.");
        require(hasLifecycleMutationEvidence(result),
                target.moduleId() + " did not record native entrypoint lifecycle evidence.");
        require(target.nativeEntrypoint().equals(result.loadedClassName()),
                target.moduleId() + " loaded class drifted: " + result.loadedClassName());
        require(target.nativeEntrypoint().equals(result.constructedEntrypointClassName()),
                target.moduleId() + " constructed delegate drifted: " + result.constructedEntrypointClassName());
        require(result.loadedByModuleClassLoader(),
                target.moduleId() + " must load from descriptor nativeClasspath, not the app classpath.");
        require(result.diagnostics().stream().noneMatch(item -> item.contains("legacy activateNative(Map) lifecycle bridge")),
                target.moduleId() + " must not use the legacy lifecycle bridge.");
        Set<String> moduleServiceIds = result.registeredServices().stream()
                .filter(service -> target.moduleId().equals(service.moduleId()))
                .map(EchoNativeRegisteredService::serviceId)
                .collect(Collectors.toSet());
        require(moduleServiceIds.stream().anyMatch(serviceId -> !serviceId.startsWith("content.")),
                target.moduleId() + " did not register a module-owned native service.");
        require(hasLifecycleMutationEvidence(result),
                target.moduleId() + " lifecycle records must include MUTATED evidence.");
    }

    private static boolean hasLifecycleMutationEvidence(EchoNativeModuleLoadResult result) {
        return result.mutations().stream()
                .map(item -> String.valueOf(item.get("status")))
                .anyMatch(EchoNativeLoadStatus.MUTATED.name()::equals);
    }

    private static EchoNativeAddonDescriptor readDescriptor(
            Path repoRoot,
            TargetModule target,
            List<String> sharedClasspath
    ) throws Exception {
        Path descriptorPath = repoRoot.resolve(target.modulePath())
                .resolve("src/main/resources/META-INF/echo.mod.json")
                .toAbsolutePath()
                .normalize();
        Map<String, Object> json = readJsonObject(descriptorPath);
        Map<String, Object> access = new LinkedHashMap<>(EchoNativeJson.asObject(json.get("access")));
        access.put("nativeEntrypoint", target.nativeEntrypoint());
        access.put("nativeClasspath", sharedClasspath);
        return new EchoNativeAddonDescriptor(
                string(json.get("schema")),
                string(json.get("id")),
                string(json.get("name")),
                string(json.get("version")),
                string(json.get("kind")),
                string(json.get("role")),
                string(json.get("entrypoint")),
                EchoNativeRuntimeSide.from(string(json.get("side"))),
                EchoNativeTrustLevel.from(string(json.get("trustLevel"))),
                EchoNativeApiStability.from(string(json.get("apiStability"))),
                bool(json.get("official")),
                bool(json.get("standalone")),
                cleanList(EchoNativeJson.stringList(json.get("requires"))),
                cleanList(EchoNativeJson.stringList(json.get("optional"))),
                cleanList(EchoNativeJson.stringList(json.get("provides"))),
                cleanList(EchoNativeJson.stringList(json.get("consumes"))),
                cleanList(EchoNativeJson.stringList(json.get("transforms"))),
                Map.copyOf(access),
                descriptorPath
        );
    }

    private static List<TargetModule> targets(Path repoRoot, String scope) throws Exception {
        List<AuditModule> modules = auditModules(repoRoot);
        List<TargetModule> targets = new ArrayList<>();
        for (AuditModule module : modules) {
            if (!includedInScope(module.metadata(), scope)) {
                continue;
            }
            if (!module.moduleId().isBlank() && !module.directory().isBlank() && !module.nativeEntrypoint().isBlank()) {
                targets.add(module.toTargetModule());
            }
        }
        targets.sort(java.util.Comparator.comparing(TargetModuleLoadStateSmokeMainOrder::orderKey));
        if ("focused-core".equals(scope) && targets.size() < TARGET_ORDER.size()) {
            throw new IllegalStateException("Expected at least " + TARGET_ORDER.size()
                    + " focused core modules but found " + targets.size() + ".");
        }
        if ("all-bridgeable".equals(scope) && targets.size() < 90) {
            throw new IllegalStateException("Expected at least 90 bridgeable modules but found " + targets.size() + ".");
        }
        return List.copyOf(targets);
    }

    private static List<TargetModule> descriptorModules(
            Path repoRoot,
            String scope,
            List<TargetModule> targets
    ) throws Exception {
        List<TargetModule> seeds = new ArrayList<>(targets);
        if ("focused-core".equals(scope)) {
            seeds.addAll(SUPPORT_MODULES);
        }
        if (!"all-bridgeable".equals(scope)) {
            return uniqueModules(seeds);
        }
        Map<String, TargetModule> allModulesById = auditModules(repoRoot).stream()
                .filter(module -> !module.moduleId().isBlank() && !module.directory().isBlank())
                .collect(Collectors.toMap(
                        AuditModule::moduleId,
                        AuditModule::toTargetModule,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        LinkedHashMap<String, TargetModule> selected = new LinkedHashMap<>();
        for (TargetModule seed : seeds) {
            includeWithRequiredDependencies(allModulesById.getOrDefault(seed.moduleId(), seed), allModulesById, selected);
        }
        return List.copyOf(selected.values());
    }

    private static List<TargetModule> uniqueModules(List<TargetModule> modules) {
        LinkedHashMap<String, TargetModule> selected = new LinkedHashMap<>();
        for (TargetModule module : modules) {
            selected.putIfAbsent(module.moduleId(), module);
        }
        return List.copyOf(selected.values());
    }

    private static void includeWithRequiredDependencies(
            TargetModule module,
            Map<String, TargetModule> allModulesById,
            LinkedHashMap<String, TargetModule> selected
    ) {
        if (selected.containsKey(module.moduleId())) {
            return;
        }
        selected.put(module.moduleId(), module);
        for (String requiredModuleId : module.requires()) {
            TargetModule dependency = allModulesById.get(requiredModuleId);
            if (dependency != null) {
                includeWithRequiredDependencies(dependency, allModulesById, selected);
            }
        }
    }

    private static List<AuditModule> auditModules(Path repoRoot) throws Exception {
        Path auditPath = repoRoot.resolve("reports/echo-native/core-module-integration-audit.json")
                .toAbsolutePath()
                .normalize();
        Map<String, Object> audit = readJsonObject(auditPath);
        Object modulesValue = audit.get("modules");
        if (!(modulesValue instanceof List<?> modules)) {
            throw new IllegalStateException("Audit report has no modules list: " + auditPath);
        }
        Map<String, Integer> order = new LinkedHashMap<>();
        for (int index = 0; index < TARGET_ORDER.size(); index++) {
            order.put(TARGET_ORDER.get(index), index);
        }
        List<AuditModule> targets = new ArrayList<>();
        for (Object item : modules) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            String moduleId = string(module.get("moduleId"));
            String directory = string(module.get("directory"));
            String nativeEntrypoint = string(module.get("nativeEntrypoint"));
            targets.add(new AuditModule(
                    module,
                    moduleId,
                    directory,
                    nativeEntrypoint,
                    cleanList(EchoNativeJson.stringList(module.get("requires")))
            ));
        }
        return List.copyOf(targets);
    }

    private static boolean includedInScope(Map<String, Object> module, String scope) {
        return switch (scope) {
            case "focused-core" -> bool(module.get("inCoreSpineAudit"));
            case "all-bridgeable" -> "LEGACY_ADAPTER_BRIDGEABLE".equals(string(module.get("nativeIntegrationStatus")));
            default -> throw new IllegalArgumentException("Unsupported core module load-state scope: " + scope);
        };
    }

    private static List<String> sharedNativeClasspath(
            Path repoRoot,
            Path classpathRoot,
            List<TargetModule> targets,
            String classpathMode
    ) {
        if ("artifacts".equals(classpathMode)) {
            return artifactNativeClasspath(classpathRoot, targets);
        }
        if (!"classes".equals(classpathMode)) {
            throw new IllegalArgumentException("Unsupported native classpath mode: " + classpathMode);
        }
        List<String> classpath = new ArrayList<>();
        for (TargetModule target : targets) {
            Path sourceRoot = repoRoot.resolve(target.modulePath()).toAbsolutePath().normalize();
            addRequiredClasspath(classpath, List.of(
                    sourceRoot.resolve("build/classes/java/main"),
                    classpathRoot.resolve(target.buildDirectoryName()).resolve("classes/java/main")
            ));
            addOptionalClasspath(classpath, sourceRoot.resolve("build/resources/main"));
            addOptionalClasspath(classpath, classpathRoot.resolve(target.buildDirectoryName()).resolve("resources/main"));
        }
        return List.copyOf(classpath);
    }

    private static List<String> artifactNativeClasspath(Path artifactDirectory, List<TargetModule> targets) {
        List<String> classpath = new ArrayList<>();
        for (TargetModule target : targets) {
            addRequiredClasspath(classpath, artifactDirectory.resolve(target.moduleId() + ".jar"));
        }
        return List.copyOf(classpath);
    }

    private static void addRequiredClasspath(List<String> classpath, Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing required native classpath entry " + path
                    + ". Run root Gradle classes for the core native modules first.");
        }
        addClasspathEntry(classpath, path);
    }

    private static void addRequiredClasspath(List<String> classpath, List<Path> candidates) {
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                addClasspathEntry(classpath, candidate);
                return;
            }
        }
        throw new IllegalStateException("Missing required native classpath entry; checked " + candidates
                + ". Run root Gradle classes for the core native modules first.");
    }

    private static void addOptionalClasspath(List<String> classpath, Path path) {
        if (Files.isDirectory(path)) {
            addClasspathEntry(classpath, path);
        }
    }

    private static void addClasspathEntry(List<String> classpath, Path path) {
        String item = path.toString();
        if (!classpath.contains(item)) {
            classpath.add(item);
        }
    }

    private static Path defaultRootBuildDirectory() {
        return Path.of(System.getProperty("user.home"), "AppData", "Local", "EchoGradleBuild", "Echo")
                .toAbsolutePath()
                .normalize();
    }

    private static Map<String, Object> readJsonObject(Path path) throws Exception {
        String text = Files.readString(path);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(text));
    }

    private static String path(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private static List<String> cleanList(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .filter(value -> !"{}".equals(value) && !"[]".equals(value))
                .toList();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record TargetModule(String moduleId, String modulePath, String nativeEntrypoint, List<String> requires) {
        private TargetModule(String moduleId, String modulePath, String nativeEntrypoint) {
            this(moduleId, modulePath, nativeEntrypoint, List.of());
        }

        private TargetModule {
            requires = requires == null ? List.of() : List.copyOf(requires);
        }

        private String buildDirectoryName() {
            return sourceRoot().getFileName().toString();
        }

        private Path sourceRoot() {
            return Path.of(modulePath.replace('\\', '/'));
        }
    }

    private record AuditModule(
            Map<String, Object> metadata,
            String moduleId,
            String directory,
            String nativeEntrypoint,
            List<String> requires
    ) {
        private TargetModule toTargetModule() {
            return new TargetModule(moduleId, directory, nativeEntrypoint, requires);
        }
    }

    private static final class TargetModuleLoadStateSmokeMainOrder {
        private TargetModuleLoadStateSmokeMainOrder() {
        }

        private static String orderKey(TargetModule target) {
            int index = TARGET_ORDER.indexOf(target.moduleId());
            return (index >= 0 ? String.format("%04d", index) : "9999") + ":" + target.moduleId();
        }
    }
}
