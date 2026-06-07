package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeTransformCompatibilityPolicy;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.loader.EchoNativeDescriptorScanner;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.packos.EchoNativePackProfileLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

public final class EchoNativeProductPackager {
    private final EchoNativeDescriptorScanner scanner = new EchoNativeDescriptorScanner();
    private final EchoNativePackProfileLoader profileLoader = new EchoNativePackProfileLoader();

    public EchoNativeProductPackageOutcome packageProduct(Path sourceRoot, Path outputRoot) throws IOException {
        return packageProduct(sourceRoot, null, outputRoot);
    }

    public EchoNativeProductPackageOutcome packageProduct(
            Path sourceRoot,
            Path packProfileRoot,
            Path outputRoot
    ) throws IOException {
        Path normalizedSourceRoot = sourceRoot.toAbsolutePath().normalize();
        Path normalizedOutputRoot = outputRoot.toAbsolutePath().normalize();
        Path normalizedPackProfileRoot = packProfileRoot == null
                ? null
                : normalizePackProfileRoot(packProfileRoot);
        EchoNativeScanResult scanResult = normalizedPackProfileRoot == null
                ? scanner.scanProduct(normalizedSourceRoot)
                : scanner.scanProduct(normalizedSourceRoot, profileLoader.load(normalizedPackProfileRoot));
        List<String> diagnostics = new ArrayList<>();
        if (scanResult.packProfile() == null) {
            diagnostics.add("Product profile could not be resolved for " + normalizedSourceRoot);
            return new EchoNativeProductPackageOutcome("", 0, 0, false, normalizedOutputRoot, List.copyOf(diagnostics));
        }

        cleanPackagedOutput(normalizedSourceRoot, normalizedOutputRoot);
        Files.createDirectories(normalizedOutputRoot.resolve("modules"));
        writePackProfile(
                normalizedPackProfileRoot == null ? normalizedSourceRoot : normalizedPackProfileRoot,
                normalizedOutputRoot,
                scanResult
        );

        List<EchoNativeAddonDescriptor> packageDescriptors = packageDescriptors(scanResult);
        Map<String, EchoNativeAddonDescriptor> descriptorsById = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : packageDescriptors) {
            descriptorsById.put(descriptor.id(), descriptor);
        }
        diagnostics.addAll(missingRequiredModuleDiagnostics(scanResult, descriptorsById));

        int packaged = 0;
        for (EchoNativeAddonDescriptor descriptor : packageDescriptors) {
            Path moduleOutput = normalizedOutputRoot.resolve("modules").resolve(descriptor.id());
            Path libOutput = moduleOutput.resolve("lib");
            Files.createDirectories(moduleOutput.resolve("META-INF"));
            Files.createDirectories(libOutput);

            Path moduleJar = libOutput.resolve(descriptor.id() + ".jar");
            try {
                materializeModuleJar(normalizedSourceRoot, descriptor, moduleJar);
                validatePackagedNativeEntrypoint(descriptor, moduleJar);
                writePackagedDescriptor(descriptor, descriptorsById, moduleOutput.resolve("META-INF/echo.mod.json"));
                packaged++;
            } catch (RuntimeException | IOException exception) {
                diagnostics.add("Module " + descriptor.id() + " was not packaged: " + exception.getMessage());
            }
        }

        if (packaged == packageDescriptors.size() && diagnostics.isEmpty()) {
            diagnostics.addAll(packagedProductPreflightDiagnostics(normalizedOutputRoot));
        }
        writePackageManifest(normalizedOutputRoot, scanResult.packProfile().id(), packaged, packageDescriptors.size(), diagnostics);
        return new EchoNativeProductPackageOutcome(
                scanResult.packProfile().id(),
                packageDescriptors.size(),
                packaged,
                packaged == packageDescriptors.size() && diagnostics.isEmpty(),
                normalizedOutputRoot,
                List.copyOf(diagnostics)
        );
    }

    private static void cleanPackagedOutput(Path sourceRoot, Path outputRoot) throws IOException {
        if (sourceRoot.startsWith(outputRoot)) {
            throw new IOException("package output root must not be the source root or one of its ancestors: " + outputRoot);
        }
        deleteTree(outputRoot.resolve("modules"));
        Files.deleteIfExists(outputRoot.resolve("echo.pack.json"));
        Files.deleteIfExists(outputRoot.resolve("echo-native-product-package.json"));
    }

    private static Path normalizePackProfileRoot(Path packProfileRoot) {
        Path normalized = packProfileRoot.toAbsolutePath().normalize();
        return Files.isRegularFile(normalized) && "echo.pack.json".equals(normalized.getFileName().toString())
                ? normalized.getParent()
                : normalized;
    }

    private static List<EchoNativeAddonDescriptor> packageDescriptors(EchoNativeScanResult scanResult) {
        Map<String, EchoNativeAddonDescriptor> availableById = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : scanResult.descriptors()) {
            availableById.put(descriptor.id(), descriptor);
        }
        LinkedHashSet<String> selectedIds = new LinkedHashSet<>(scanResult.packProfile().requiredModules());
        if (scanResult.packProfile().rootModule() != null && !scanResult.packProfile().rootModule().isBlank()) {
            selectedIds.add(scanResult.packProfile().rootModule());
        }
        if (selectedIds.isEmpty()) {
            return scanResult.descriptors();
        }
        for (String baseModule : List.of("echocore", "echoplatformcore", "echoadaptercore")) {
            if (availableById.containsKey(baseModule)) {
                selectedIds.add(baseModule);
            }
        }
        boolean changed;
        do {
            changed = false;
            for (String selectedId : List.copyOf(selectedIds)) {
                EchoNativeAddonDescriptor descriptor = availableById.get(selectedId);
                if (descriptor == null) {
                    continue;
                }
                for (String required : descriptor.requires()) {
                    if (availableById.containsKey(required) && selectedIds.add(required)) {
                        changed = true;
                    }
                }
            }
        } while (changed);
        return scanResult.descriptors().stream()
                .filter(descriptor -> selectedIds.contains(descriptor.id()))
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .toList();
    }

    private static List<String> missingRequiredModuleDiagnostics(
            EchoNativeScanResult scanResult,
            Map<String, EchoNativeAddonDescriptor> descriptorsById
    ) {
        if (scanResult.packProfile().requiredModules().isEmpty()) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        for (String moduleId : scanResult.packProfile().requiredModules()) {
            if (!descriptorsById.containsKey(moduleId)) {
                missing.add("Required module " + moduleId + " has no descriptor in the product source.");
            }
        }
        for (EchoNativeAddonDescriptor descriptor : descriptorsById.values()) {
            for (String required : descriptor.requires()) {
                if (!descriptorsById.containsKey(required)) {
                    missing.add("Module " + descriptor.id() + " requires " + required
                            + ", but that dependency is not present in the packaged descriptor set.");
                }
            }
        }
        return List.copyOf(missing);
    }

    private static void writePackProfile(Path sourceRoot, Path outputRoot, EchoNativeScanResult scanResult) throws IOException {
        Path sourceProfile = sourceRoot.resolve("echo.pack.json");
        Path targetProfile = outputRoot.resolve("echo.pack.json");
        if (Files.isRegularFile(sourceProfile)) {
            Files.copy(sourceProfile, targetProfile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("schema", scanResult.packProfile().schema());
        profile.put("id", scanResult.packProfile().id());
        profile.put("name", scanResult.packProfile().name());
        profile.put("status", scanResult.packProfile().status());
        profile.put("rootModule", scanResult.packProfile().rootModule());
        profile.put("minecraftVersion", scanResult.packProfile().minecraftVersion());
        profile.put("loader", Map.of(
                "kind", scanResult.packProfile().loaderKind(),
                "version", scanResult.packProfile().loaderVersion()
        ));
        profile.put("requiredModules", scanResult.packProfile().requiredModules());
        profile.put("requiredFeatures", scanResult.packProfile().requiredFeatures());
        profile.put("optionalFeatures", scanResult.packProfile().optionalFeatures());
        Files.writeString(targetProfile, EchoNativeJson.write(profile), StandardCharsets.UTF_8);
    }

    private static void writePackagedDescriptor(
            EchoNativeAddonDescriptor descriptor,
            Map<String, EchoNativeAddonDescriptor> descriptorsById,
            Path target
    ) throws IOException {
        Map<String, Object> json = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(descriptor.descriptorPath())));
        Map<String, Object> access = new LinkedHashMap<>(EchoNativeJson.asObject(json.get("access")));
        access.put("nativeClasspath", packagedClasspath(descriptor, descriptorsById));
        access.put("nativeTransformCompatibilityPolicy", transformCompatibilityPolicy(descriptor));
        json.put("access", access);
        Files.writeString(target, EchoNativeJson.write(json), StandardCharsets.UTF_8);
    }

    private static Map<String, Object> transformCompatibilityPolicy(EchoNativeAddonDescriptor descriptor) {
        EchoNativeTransformCompatibilityPolicy.TransformCompatibilityReport report =
                EchoNativeTransformCompatibilityPolicy.evaluate("", descriptor);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("policyId", report.policyId());
        policy.put("compatible", report.compatible());
        policy.put("decision", report.policyDecision());
        policy.put("bytecodeMutationAllowed", report.bytecodeMutationAllowed());
        policy.put("minecraftBytecodeMutationAllowed", report.minecraftBytecodeMutationAllowed());
        policy.put("addonBytecodeMutationAllowed", report.addonBytecodeMutationAllowed());
        policy.put("declaredForgeStyleTransforms", report.declaredForgeStyleTransforms());
        policy.put("declaredNativeReplacements", report.declaredNativeReplacements());
        policy.put("declaredReplacementMappings", report.declaredReplacementMappings());
        policy.put("supportedNativeDeclarations", packagedSupportedNativeDeclarations(descriptor, report));
        policy.put("replacementCoverageComplete", report.replacementCoverageComplete());
        policy.put("nativeProjectionReplacementPlanned", report.nativeProjectionReplacementPlanned());
        policy.put("plannedNativeProjectionCount", report.plannedNativeProjectionCount());
        policy.put("unmappedForgeStyleTransforms", report.unmappedForgeStyleTransforms());
        policy.put("unknownMappedForgeStyleTransforms", report.unknownMappedForgeStyleTransforms());
        policy.put("releasePolicySummary", report.releasePolicySummary());
        policy.put("diagnosticPathSummary", EchoNativeTransformCompatibilityPolicy.diagnosticPathSummary());
        return policy;
    }

    private static List<String> packagedSupportedNativeDeclarations(
            EchoNativeAddonDescriptor descriptor,
            EchoNativeTransformCompatibilityPolicy.TransformCompatibilityReport report
    ) {
        if (!report.supportedNativeDeclarations().isEmpty()) {
            return report.supportedNativeDeclarations();
        }
        Object nativeEntrypoint = descriptor.access() == null ? null : descriptor.access().get("nativeEntrypoint");
        if (nativeEntrypoint != null && !String.valueOf(nativeEntrypoint).trim().isBlank()) {
            return List.of("native:descriptor_metadata_projection");
        }
        return List.of();
    }

    private static List<String> packagedClasspath(
            EchoNativeAddonDescriptor descriptor,
            Map<String, EchoNativeAddonDescriptor> descriptorsById
    ) {
        LinkedHashSet<String> entries = new LinkedHashSet<>();
        entries.add("lib/" + descriptor.id() + ".jar");
        for (String baseModule : List.of("echocore", "echoplatformcore", "echoadaptercore")) {
            addDependencyClasspathEntry(entries, descriptor, descriptorsById, baseModule);
        }
        for (String required : descriptor.requires()) {
            addDependencyClasspathEntry(entries, descriptor, descriptorsById, required);
        }
        for (String optional : descriptor.optional()) {
            addDependencyClasspathEntry(entries, descriptor, descriptorsById, optional);
        }
        return List.copyOf(entries);
    }

    private static void addDependencyClasspathEntry(
            LinkedHashSet<String> entries,
            EchoNativeAddonDescriptor descriptor,
            Map<String, EchoNativeAddonDescriptor> descriptorsById,
            String dependencyId
    ) {
        if (dependencyId == null || dependencyId.isBlank() || dependencyId.equals(descriptor.id())) {
            return;
        }
        if (descriptorsById.containsKey(dependencyId)) {
            entries.add("../" + dependencyId + "/lib/" + dependencyId + ".jar");
        }
    }

    private static void materializeModuleJar(
            Path sourceRoot,
            EchoNativeAddonDescriptor descriptor,
            Path targetJar
    ) throws IOException {
        List<Path> attemptedRoots = new ArrayList<>();
        List<String> rejectedArtifacts = new ArrayList<>();
        for (Path moduleRoot : candidateModuleRoots(sourceRoot, descriptor)) {
            attemptedRoots.add(moduleRoot);
            for (Path buildOutputRoot : candidateBuildOutputRoots(moduleRoot)) {
                attemptedRoots.add(buildOutputRoot);
                Path existingJar = existingJarFromBuildRoot(buildOutputRoot);
                if (existingJar != null && jarContainsRequiredModuleClasses(descriptor, moduleRoot, existingJar)) {
                    Files.copy(existingJar, targetJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return;
                } else if (existingJar != null) {
                    rejectedArtifacts.add(existingJar.toString());
                }
                Path classes = buildOutputRoot.resolve("classes/java/main");
                Path resources = buildOutputRoot.resolve("resources/main");
                if (Files.isDirectory(classes) || Files.isDirectory(resources)) {
                    writeJarFromOutputs(classes, resources, targetJar);
                    if (jarContainsRequiredModuleClasses(descriptor, moduleRoot, targetJar)) {
                        return;
                    }
                    rejectedArtifacts.add(buildOutputRoot.toString());
                }
            }
        }
        throw new IOException("no complete jar or build/classes/java/main output exists under candidate roots "
                + attemptedRoots + "; rejected incomplete artifacts " + rejectedArtifacts);
    }

    private static boolean jarContainsRequiredModuleClasses(
            EchoNativeAddonDescriptor descriptor,
            Path moduleRoot,
            Path moduleJar
    ) throws IOException {
        String entrypoint = nativeEntrypoint(descriptor);
        String entryName = entrypoint.isBlank() ? "" : entrypoint.replace('.', '/') + ".class";
        List<String> sourceClasses = sourceClassEntries(descriptor, moduleRoot);
        try (ZipFile zip = new ZipFile(moduleJar.toFile())) {
            if (!entryName.isBlank() && zip.getEntry(entryName) == null) {
                return false;
            }
            for (String sourceClass : sourceClasses) {
                if (zip.getEntry(sourceClass) == null) {
                    return false;
                }
            }
            return true;
        }
    }

    private static List<String> sourceClassEntries(EchoNativeAddonDescriptor descriptor, Path moduleRoot) throws IOException {
        Path sourceRoot = moduleRoot.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) {
            return List.of();
        }
        String entrypoint = nativeEntrypoint(descriptor);
        if (entrypoint.isBlank()) {
            return List.of();
        }
        int classNameIndex = entrypoint.lastIndexOf('.');
        if (classNameIndex < 0) {
            return List.of();
        }
        String packagePath = entrypoint.substring(0, classNameIndex).replace('.', '/');
        String entrypointSimpleName = entrypoint.substring(classNameIndex + 1);
        Path packageRoot = sourceRoot.resolve(packagePath);
        Path entrypointSource = packageRoot.resolve(entrypointSimpleName + ".java");
        if (!Files.isRegularFile(entrypointSource)) {
            return List.of();
        }
        String entrypointText = Files.readString(entrypointSource);
        try (var stream = Files.list(packageRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> !"module-info.java".equals(path.getFileName().toString()))
                    .filter(path -> !"package-info.java".equals(path.getFileName().toString()))
                    .map(path -> path.getFileName().toString().substring(
                            0,
                            path.getFileName().toString().length() - ".java".length()
                    ))
                    .filter(simpleName -> !entrypointSimpleName.equals(simpleName))
                    .filter(entrypointText::contains)
                    .map(simpleName -> packagePath + "/" + simpleName + ".class")
                    .sorted()
                    .toList();
        }
    }

    private static void validatePackagedNativeEntrypoint(
            EchoNativeAddonDescriptor descriptor,
            Path moduleJar
    ) throws IOException {
        String entrypoint = nativeEntrypoint(descriptor);
        if (entrypoint.isBlank()) {
            throw new IOException("descriptor does not declare access.nativeEntrypoint");
        }
        String entryName = entrypoint.replace('.', '/') + ".class";
        try (ZipFile zip = new ZipFile(moduleJar.toFile())) {
            if (zip.getEntry(entryName) == null) {
                throw new IOException("packaged jar " + moduleJar
                        + " does not contain declared nativeEntrypoint class " + entryName);
            }
        }
    }

    private static String nativeEntrypoint(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
        Object value = access.get("nativeEntrypoint");
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static void writeJarFromOutputs(Path classes, Path resources, Path targetJar) throws IOException {
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(targetJar))) {
            Set<String> entries = new LinkedHashSet<>();
            addDirectoryToJar(jar, entries, classes);
            addDirectoryToJar(jar, entries, resources);
        }
    }

    private static List<Path> candidateModuleRoots(Path sourceRoot, EchoNativeAddonDescriptor descriptor) throws IOException {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        roots.add(descriptorModuleRoot(descriptor.descriptorPath()));
        Path workspaceRoot = echoWorkspaceRoot(sourceRoot);
        if (workspaceRoot != null) {
            addProjectRootByDescriptorId(roots, workspaceRoot.resolve("core"), descriptor.id());
            addProjectRootByDescriptorId(roots, workspaceRoot.resolve("addons"), descriptor.id());
            roots.add(workspaceRoot.resolve("core").resolve(descriptor.id()).normalize());
            roots.add(workspaceRoot.resolve("addons").resolve(descriptor.id()).normalize());
        }
        return roots.stream().filter(Files::isDirectory).toList();
    }

    private static void addProjectRootByDescriptorId(Set<Path> roots, Path container, String moduleId) throws IOException {
        if (!Files.isDirectory(container)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(container)) {
            for (Path candidate : stream) {
                if (!Files.isDirectory(candidate)) {
                    continue;
                }
                Path descriptorPath = candidate.resolve("src/main/resources/META-INF/echo.mod.json");
                if (Files.isRegularFile(descriptorPath)) {
                    Map<String, Object> descriptor = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(descriptorPath)));
                    if (moduleId.equals(String.valueOf(descriptor.getOrDefault("id", "")))) {
                        roots.add(candidate.normalize());
                    }
                }
            }
        }
    }

    private static List<Path> candidateBuildOutputRoots(Path moduleRoot) {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        roots.add(moduleRoot.resolve("build").toAbsolutePath().normalize());
        String projectDirectoryName = moduleRoot.getFileName() == null ? "" : moduleRoot.getFileName().toString();
        if (!projectDirectoryName.isBlank()) {
            for (Path buildRoot : localGradleBuildRoots()) {
                roots.add(buildRoot.resolve(projectDirectoryName).toAbsolutePath().normalize());
            }
        }
        return List.copyOf(roots);
    }

    private static List<Path> localGradleBuildRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        addExplicitBuildRoot(roots, System.getenv("ECHO_GRADLE_BUILD_DIR"));
        addExplicitBuildRoot(roots, System.getenv("ECHO_NATIVE_GRADLE_BUILD_ROOT"));
        addExplicitBuildRoot(roots, System.getProperty("echo.native.gradleBuildRoot", ""));
        if (Boolean.getBoolean("echo.native.allowLocalBuildClasspath")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                roots.add(Path.of(localAppData).resolve("EchoGradleBuild").resolve("Echo"));
            }
            roots.add(Path.of(System.getProperty("java.io.tmpdir")).resolve("EchoGradleBuild").resolve("Echo"));
        }
        return List.copyOf(roots);
    }

    private static void addExplicitBuildRoot(Set<Path> roots, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Path explicitRoot = Path.of(value);
        roots.add(explicitRoot);
        roots.add(explicitRoot.resolve("Echo"));
    }

    private static Path existingJarFromBuildRoot(Path buildRoot) throws IOException {
        Path libs = buildRoot.resolve("libs");
        if (!Files.isDirectory(libs)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libs, "*.jar")) {
            List<Path> jars = new ArrayList<>();
            for (Path jar : stream) {
                String fileName = jar.getFileName().toString();
                if (!fileName.endsWith("-sources.jar") && !fileName.endsWith("-javadoc.jar")) {
                    jars.add(jar);
                }
            }
            jars.sort(Comparator.comparing(path -> path.getFileName().toString()));
            return jars.isEmpty() ? null : jars.getFirst();
        }
    }

    private static void addDirectoryToJar(JarOutputStream jar, Set<String> entries, Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(file -> {
                        String name = root.relativize(file).toString().replace('\\', '/');
                        if (!entries.add(name)) {
                            return;
                        }
                        try {
                            jar.putNextEntry(new JarEntry(name));
                            Files.copy(file, jar);
                            jar.closeEntry();
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private static List<String> packagedProductPreflightDiagnostics(Path outputRoot) {
        try {
            EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome =
                    new EchoNativeProductLauncher().launch(
                            outputRoot,
                            new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(true, false, false)
                    );
            if (outcome.accepted()) {
                return List.of();
            }
            List<String> diagnostics = new ArrayList<>();
            diagnostics.add("Packaged product module preflight failed for " + outcome.packId() + ": "
                    + outcome.loadedModules() + "/" + outcome.totalModules() + " loaded, "
                    + outcome.registeredModules() + "/" + outcome.totalModules() + " registered, "
                    + outcome.mutatedModules() + "/" + outcome.totalModules() + " mutated, "
                    + outcome.failedModules() + " failed.");
            for (EchoNativeProductLauncher.EchoNativeProductModuleLaunch module : outcome.modules()) {
                if (module.accepted()) {
                    continue;
                }
                diagnostics.add("Module " + module.moduleId() + " failed packaged module preflight: claimed="
                        + module.claimedStatus() + ", honest=" + module.honestStatus()
                        + ", loaded=" + module.loaded()
                        + ", registered=" + module.registered()
                        + ", mutated=" + module.mutated() + ".");
                if (!module.failures().isEmpty()) {
                    diagnostics.add("Module " + module.moduleId() + " preflight failures: "
                            + String.join("; ", module.failures()));
                }
                if (!module.diagnostics().isEmpty()) {
                    diagnostics.add("Module " + module.moduleId() + " preflight diagnostics: "
                            + String.join("; ", module.diagnostics()));
                }
            }
            outcome.diagnostics().stream()
                    .filter(EchoNativeProductPackager::isBlocking)
                    .map(diagnostic -> "Preflight " + diagnostic.severity() + " " + diagnostic.code()
                            + ": " + diagnostic.summary())
                    .forEach(diagnostics::add);
            return List.copyOf(diagnostics);
        } catch (RuntimeException | IOException exception) {
            return List.of("Packaged product release preflight could not run: " + exception.getMessage());
        }
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static Path descriptorModuleRoot(Path descriptorPath) {
        Path normalized = descriptorPath.toAbsolutePath().normalize();
        Path current = normalized.getParent();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    || Files.isRegularFile(current.resolve("build.gradle.kts"))
                    || descriptorUnderSourceResources(normalized, current)) {
                return current;
            }
            current = current.getParent();
        }
        Path parent = normalized.getParent();
        return parent == null || parent.getParent() == null ? Path.of("") : parent.getParent();
    }

    private static Path echoWorkspaceRoot(Path sourceRoot) {
        Path current = sourceRoot.toAbsolutePath().normalize();
        while (current != null) {
            if ((Files.isRegularFile(current.resolve("settings.gradle"))
                    || Files.isRegularFile(current.resolve("settings.gradle.kts")))
                    && Files.isDirectory(current.resolve("addons"))
                    && Files.isDirectory(current.resolve("core"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean descriptorUnderSourceResources(Path descriptorPath, Path candidateRoot) {
        return descriptorPath.startsWith(candidateRoot.resolve("src/main/resources").toAbsolutePath().normalize());
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                deletePathWithRetries(path);
            }
        }
    }

    private static void deletePathWithRetries(Path path) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Files.delete(path);
                return;
            } catch (IOException exception) {
                lastFailure = exception;
                try {
                    Thread.sleep(250L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
        }
        throw lastFailure == null ? new IOException("Unable to delete " + path) : lastFailure;
    }

    private static void writePackageManifest(
            Path outputRoot,
            String packId,
            int packaged,
            int total,
            List<String> diagnostics
    ) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", "echo.native.product_package.v1");
        manifest.put("packId", packId);
        manifest.put("packagedModules", packaged);
        manifest.put("totalModules", total);
        manifest.put("releaseClasspath", "explicit-packaged-artifacts");
        manifest.put("diagnostics", diagnostics);
        Files.writeString(outputRoot.resolve("echo-native-product-package.json"),
                EchoNativeJson.write(manifest), StandardCharsets.UTF_8);
    }

    public record EchoNativeProductPackageOutcome(
            String packId,
            int totalModules,
            int packagedModules,
            boolean packaged,
            Path outputRoot,
            List<String> diagnostics
    ) {
    }
}
