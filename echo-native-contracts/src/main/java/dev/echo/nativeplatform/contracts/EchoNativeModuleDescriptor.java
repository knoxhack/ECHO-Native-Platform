package dev.echo.nativeplatform.contracts;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeModuleDescriptor(
        String id,
        String name,
        String version,
        String kind,
        String role,
        String entrypoint,
        EchoNativeRuntimeSide side,
        List<String> requires,
        List<String> optional,
        List<String> provides,
        Path descriptorPath,
        List<Path> classpath,
        List<Path> declaredClasspath,
        List<Path> generatedClasspath,
        boolean nativeClasspathDeclared,
        boolean inferredClasspathRequested,
        boolean compatibilityClasspathFallback
) {
    public static final String INFERRED_CLASSPATH_TOKEN = "echo.native:inferred-classpath";
    private static final Pattern DESCRIPTOR_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    public EchoNativeModuleDescriptor {
        requires = stringList(requires);
        optional = stringList(optional);
        provides = stringList(provides);
        classpath = pathList(classpath);
        declaredClasspath = pathList(declaredClasspath);
        generatedClasspath = pathList(generatedClasspath);
    }

    public EchoNativeModuleDescriptor(
            String id,
            String name,
            String version,
            String kind,
            String role,
            String entrypoint,
            EchoNativeRuntimeSide side,
            List<String> requires,
            List<String> optional,
            List<String> provides,
            Path descriptorPath,
            List<Path> classpath
    ) {
        this(
                id,
                name,
                version,
                kind,
                role,
                entrypoint,
                side,
                requires,
                optional,
                provides,
                descriptorPath,
                classpath,
                classpath,
                List.of(),
                classpath != null && !classpath.isEmpty(),
                false,
                false
        );
    }

    public static EchoNativeModuleDescriptor fromAddon(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
        String nativeEntrypoint = string(access.get("nativeEntrypoint"));
        String entrypoint = nativeEntrypoint.isBlank() ? descriptor.entrypoint() : nativeEntrypoint;
        NativeClasspathResolution nativeClasspath = nativeClasspath(descriptor, access.get("nativeClasspath"));
        return new EchoNativeModuleDescriptor(
                descriptor.id(),
                descriptor.name(),
                descriptor.version(),
                descriptor.kind(),
                descriptor.role(),
                entrypoint,
                descriptor.side(),
                descriptor.requires(),
                descriptor.optional(),
                descriptor.provides(),
                descriptor.descriptorPath(),
                nativeClasspath.classpath(),
                nativeClasspath.declaredClasspath(),
                nativeClasspath.generatedClasspath(),
                nativeClasspath.nativeClasspathDeclared(),
                nativeClasspath.inferredClasspathRequested(),
                nativeClasspath.compatibilityClasspathFallback()
        );
    }

    public boolean hasEntrypoint() {
        return entrypoint != null && !entrypoint.isBlank();
    }

    private static NativeClasspathResolution nativeClasspath(EchoNativeAddonDescriptor descriptor, Object value) {
        ExplicitClasspath explicitClasspath = explicitClasspath(descriptor, value);
        LinkedHashSet<Path> paths = new LinkedHashSet<>(explicitClasspath.paths());
        List<Path> generatedClasspath = generatedClasspath(descriptor);
        paths.addAll(generatedClasspath);
        LinkedHashSet<Path> declaredClasspath = new LinkedHashSet<>(explicitClasspath.paths());
        if (explicitClasspath.includeInferredClasspath()) {
            declaredClasspath.addAll(generatedClasspath);
        }
        boolean compatibilityFallback = paths.isEmpty()
                || (explicitClasspath.nativeClasspathDeclared() && declaredClasspath.isEmpty());
        return new NativeClasspathResolution(
                List.copyOf(paths),
                List.copyOf(declaredClasspath),
                generatedClasspath,
                explicitClasspath.nativeClasspathDeclared(),
                explicitClasspath.includeInferredClasspath(),
                compatibilityFallback
        );
    }

    private static ExplicitClasspath explicitClasspath(EchoNativeAddonDescriptor descriptor, Object value) {
        Path moduleRoot = descriptorModuleRoot(descriptor.descriptorPath());
        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        if (!(value instanceof List<?> list)) {
            return new ExplicitClasspath(List.of(), false, false);
        }
        boolean includeInferredClasspath = false;
        for (Object item : list) {
            if (item != null && !String.valueOf(item).isBlank()) {
                String raw = String.valueOf(item).trim();
                if (INFERRED_CLASSPATH_TOKEN.equals(raw)) {
                    includeInferredClasspath = true;
                    continue;
                }
                Path path = Path.of(raw);
                paths.add(path.isAbsolute() ? path.normalize() : moduleRoot.resolve(path).normalize());
            }
        }
        return new ExplicitClasspath(List.copyOf(paths), true, includeInferredClasspath);
    }

    private static List<Path> generatedClasspath(EchoNativeAddonDescriptor descriptor) {
        if (descriptor.descriptorPath() == null) {
            return List.of();
        }
        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        Path moduleRoot = descriptorModuleRoot(descriptor.descriptorPath());
        addPackagedArtifacts(paths, moduleRoot, descriptor.id());

        if (packagedModuleRoot(descriptor.descriptorPath().toAbsolutePath().normalize()) != null) {
            return List.copyOf(paths);
        }

        addProjectOutputs(paths, moduleRoot);

        Path workspaceRoot = echoWorkspaceRoot(descriptor.descriptorPath());
        if (workspaceRoot != null) {
            addProjectOutputs(paths, workspaceRoot.resolve("echo-native-platform").resolve("echo-native-contracts"));
            addProjectOutputs(paths, workspaceRoot.resolve("core").resolve("echocore"));
            addProjectOutputs(paths, workspaceRoot.resolve("addons").resolve("echoplatformcore"));
            addProjectOutputs(paths, workspaceRoot.resolve("addons").resolve("echoadaptercore"));
            addProjectOutputs(paths, workspaceRoot.resolve("addons").resolve(descriptor.id()));
            addProjectOutputs(paths, workspaceRoot.resolve("core").resolve(descriptor.id()));
            addDependencyOutputs(paths, workspaceRoot, descriptor.requires());
            addDependencyOutputs(paths, workspaceRoot, descriptor.optional());
        }
        return List.copyOf(paths);
    }

    private static void addPackagedArtifacts(Set<Path> paths, Path moduleRoot, String moduleId) {
        if (moduleRoot == null || !Files.isDirectory(moduleRoot)) {
            return;
        }
        addIfPresent(paths, moduleRoot.resolve("addon.jar"));
        if (moduleId != null && !moduleId.isBlank()) {
            addIfPresent(paths, moduleRoot.resolve(moduleId + ".jar"));
        }
        Path libs = moduleRoot.resolve("lib");
        if (Files.isDirectory(libs)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(libs, "*.jar")) {
                List<Path> jars = new ArrayList<>();
                stream.forEach(jars::add);
                jars.stream()
                        .sorted()
                        .forEach(path -> addIfPresent(paths, path));
            } catch (Exception ignored) {
                // Missing or unreadable packaged artifacts should leave class loading to fail normally.
            }
        }
    }

    private static Path descriptorModuleRoot(Path descriptorPath) {
        if (descriptorPath == null) {
            return Path.of("");
        }
        Path normalized = descriptorPath.toAbsolutePath().normalize();
        Path packagedModuleRoot = packagedModuleRoot(normalized);
        if (packagedModuleRoot != null) {
            return packagedModuleRoot;
        }
        Path current = normalized.getParent();
        while (current != null) {
            if (isGradleProjectRoot(current) || descriptorUnderSourceResources(normalized, current)) {
                return current;
            }
            current = current.getParent();
        }
        Path parent = normalized.getParent();
        return parent == null || parent.getParent() == null ? Path.of("") : parent.getParent();
    }

    private static Path packagedModuleRoot(Path descriptorPath) {
        Path metaInf = descriptorPath.getParent();
        if (metaInf == null || !metaInf.getFileName().toString().equals("META-INF")) {
            return null;
        }
        Path moduleRoot = metaInf.getParent();
        if (moduleRoot == null || moduleRoot.getParent() == null) {
            return null;
        }
        return moduleRoot.getParent().getFileName().toString().equals("modules") ? moduleRoot : null;
    }

    private static boolean descriptorUnderSourceResources(Path descriptorPath, Path candidateRoot) {
        Path sourceResources = candidateRoot.resolve("src/main/resources").toAbsolutePath().normalize();
        return descriptorPath.startsWith(sourceResources);
    }

    private static boolean isGradleProjectRoot(Path path) {
        return Files.isRegularFile(path.resolve("build.gradle"))
                || Files.isRegularFile(path.resolve("build.gradle.kts"))
                || Files.isRegularFile(path.resolve("settings.gradle"))
                || Files.isRegularFile(path.resolve("settings.gradle.kts"));
    }

    private static Path echoWorkspaceRoot(Path descriptorPath) {
        Path current = descriptorPath.toAbsolutePath().normalize();
        while (current != null) {
            if (isEchoWorkspaceRoot(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean isEchoWorkspaceRoot(Path path) {
        return (Files.isRegularFile(path.resolve("settings.gradle"))
                || Files.isRegularFile(path.resolve("settings.gradle.kts")))
                && Files.isDirectory(path.resolve("echo-native-platform"))
                && Files.isDirectory(path.resolve("addons"))
                && Files.isDirectory(path.resolve("core"));
    }

    private static void addProjectOutputs(Set<Path> paths, Path projectRoot) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) {
            return;
        }
        int runtimeOutputCount = paths.size();
        addIfPresent(paths, projectRoot.resolve("build/classes/java/main"));
        addIfPresent(paths, projectRoot.resolve("build/resources/main"));
        Path libs = projectRoot.resolve("build/libs");
        if (Files.isDirectory(libs)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(libs, "*.jar")) {
                List<Path> jars = new ArrayList<>();
                stream.forEach(jars::add);
                jars.stream()
                        .sorted()
                        .forEach(path -> addIfPresent(paths, path));
            } catch (Exception ignored) {
                // Missing or unreadable build outputs should leave class loading to fail normally.
            }
        }
        addLocalGradleBuildOutputs(paths, projectRoot);
        if (paths.size() == runtimeOutputCount) {
            addIfPresent(paths, projectRoot.resolve("src/main/java"));
            addIfPresent(paths, projectRoot.resolve("src/main/resources"));
        }
    }

    private static void addLocalGradleBuildOutputs(Set<Path> paths, Path projectRoot) {
        String projectDirectoryName = projectRoot.getFileName() == null ? "" : projectRoot.getFileName().toString();
        if (projectDirectoryName.isBlank()) {
            return;
        }
        for (Path buildRoot : localGradleBuildRoots()) {
            addBuildDirectoryOutputs(paths, buildRoot.resolve(projectDirectoryName));
            if (Files.isRegularFile(projectRoot.resolve("settings.gradle"))) {
                addBuildDirectoryOutputs(paths, buildRoot.resolve("root"));
            }
        }
    }

    private static List<Path> localGradleBuildRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        addExplicitBuildRoot(roots, System.getProperty("echo.native.gradleBuildRoot", ""));
        addExplicitBuildRoot(roots, System.getenv("ECHO_GRADLE_BUILD_DIR"));
        addExplicitBuildRoot(roots, System.getenv("ECHO_NATIVE_GRADLE_BUILD_ROOT"));
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

    private static void addBuildDirectoryOutputs(Set<Path> paths, Path buildDirectory) {
        addIfPresent(paths, buildDirectory.resolve("classes/java/main"));
        addIfPresent(paths, buildDirectory.resolve("resources/main"));
        Path libs = buildDirectory.resolve("libs");
        if (Files.isDirectory(libs)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(libs, "*.jar")) {
                List<Path> jars = new ArrayList<>();
                stream.forEach(jars::add);
                jars.stream()
                        .sorted()
                        .forEach(path -> addIfPresent(paths, path));
            } catch (Exception ignored) {
                // Missing or unreadable local build outputs should leave class loading to fail normally.
            }
        }
    }

    private static void addDependencyOutputs(Set<Path> paths, Path workspaceRoot, List<String> moduleIds) {
        for (String moduleId : moduleIds) {
            if (moduleId == null || moduleId.isBlank()) {
                continue;
            }
            addProjectOutputsForModuleId(paths, workspaceRoot.resolve("addons"), moduleId);
            addProjectOutputsForModuleId(paths, workspaceRoot.resolve("core"), moduleId);
        }
    }

    private static void addProjectOutputsForModuleId(Set<Path> paths, Path projectContainer, String moduleId) {
        Path namedProjectRoot = projectContainer.resolve(moduleId);
        addPackagedArtifacts(paths, namedProjectRoot, moduleId);
        addProjectOutputs(paths, namedProjectRoot);
        Path descriptorMatchedRoot = projectRootByDescriptorId(projectContainer, moduleId);
        if (descriptorMatchedRoot != null) {
            addPackagedArtifacts(paths, descriptorMatchedRoot, moduleId);
            addProjectOutputs(paths, descriptorMatchedRoot);
        }
    }

    private static Path projectRootByDescriptorId(Path projectContainer, String moduleId) {
        if (!Files.isDirectory(projectContainer)) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(projectContainer)) {
            for (Path projectRoot : stream) {
                if (!Files.isDirectory(projectRoot)) {
                    continue;
                }
                Path descriptorPath = projectRoot.resolve("src/main/resources/META-INF/echo.mod.json");
                if (moduleId.equals(descriptorId(descriptorPath))) {
                    return projectRoot;
                }
            }
        } catch (Exception ignored) {
            // Missing or unreadable descriptors should leave class loading to fail normally.
        }
        return null;
    }

    private static String descriptorId(Path descriptorPath) {
        if (!Files.isRegularFile(descriptorPath)) {
            return "";
        }
        try {
            Matcher matcher = DESCRIPTOR_ID_PATTERN.matcher(Files.readString(descriptorPath, StandardCharsets.UTF_8));
            return matcher.find() ? matcher.group(1) : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void addIfPresent(Set<Path> paths, Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.exists(normalized)) {
            paths.add(normalized);
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static List<String> stringList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<Path> pathList(List<Path> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record ExplicitClasspath(
            List<Path> paths,
            boolean nativeClasspathDeclared,
            boolean includeInferredClasspath
    ) {
    }

    private record NativeClasspathResolution(
            List<Path> classpath,
            List<Path> declaredClasspath,
            List<Path> generatedClasspath,
            boolean nativeClasspathDeclared,
            boolean inferredClasspathRequested,
            boolean compatibilityClasspathFallback
    ) {
    }
}
