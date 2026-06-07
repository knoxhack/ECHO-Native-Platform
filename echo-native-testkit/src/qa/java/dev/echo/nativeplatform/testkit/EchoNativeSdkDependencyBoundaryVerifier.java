package dev.echo.nativeplatform.testkit;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class EchoNativeSdkDependencyBoundaryVerifier {
    private static final List<Path> API_BUILD_FILES = List.of(
            Path.of("addons", "echoaddonapi", "build.gradle"),
            Path.of("addons", "echoadaptercore", "build.gradle")
    );
    private static final List<Path> API_MAIN_SOURCE_ROOTS = List.of(
            Path.of("addons", "echoaddonapi", "src", "main", "java"),
            Path.of("addons", "echoadaptercore", "src", "main", "java"),
            Path.of("echo-native-platform", "echo-native-contracts", "src", "main", "java")
    );
    private static final Pattern LOADER_PROJECT_DEPENDENCY = Pattern.compile(
            "(api|implementation|compileOnly|runtimeOnly)\\s+project\\(['\\\"]:echo-native-loader['\\\"]\\)");
    private static final Pattern LOADER_IMPORT = Pattern.compile(
            "(?m)^import\\s+dev\\.echo\\.nativeplatform\\.loader\\.");

    private EchoNativeSdkDependencyBoundaryVerifier() {
    }

    public static void main(String[] args) throws Exception {
        Path repoRoot = args.length == 0 ? Path.of("..").toAbsolutePath().normalize() : Path.of(args[0]).toAbsolutePath().normalize();
        List<String> failures = new ArrayList<>();
        verifyBuildFiles(repoRoot, failures);
        verifyMainSources(repoRoot, failures);
        if (!failures.isEmpty()) {
            throw new IllegalStateException("Native SDK dependency boundary failures:\n" + String.join("\n", failures));
        }
    }

    private static void verifyBuildFiles(Path repoRoot, List<String> failures) throws Exception {
        for (Path relative : API_BUILD_FILES) {
            Path path = repoRoot.resolve(relative).normalize();
            if (!Files.isRegularFile(path)) {
                continue;
            }
            String text = Files.readString(path, StandardCharsets.UTF_8);
            var matcher = LOADER_PROJECT_DEPENDENCY.matcher(text);
            while (matcher.find()) {
                String prefix = text.substring(0, matcher.start());
                if (insideQaTaskOrSourceSet(prefix)) {
                    continue;
                }
                failures.add(relative + " has production dependency on :echo-native-loader via " + matcher.group(1));
            }
        }
    }

    private static boolean insideQaTaskOrSourceSet(String prefix) {
        int lastQa = Math.max(prefix.lastIndexOf("sourceSets {"), prefix.lastIndexOf("tasks.register(\"runAdapterCoreNativeLoaderRuntimeHostSmoke\""));
        int lastDependencies = prefix.lastIndexOf("dependencies {");
        return lastQa > lastDependencies;
    }

    private static void verifyMainSources(Path repoRoot, List<String> failures) throws Exception {
        for (Path relativeRoot : API_MAIN_SOURCE_ROOTS) {
            Path root = repoRoot.resolve(relativeRoot).normalize();
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                for (Path path : stream.filter(item -> item.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(path, StandardCharsets.UTF_8);
                    if (LOADER_IMPORT.matcher(text).find()) {
                        failures.add(repoRoot.relativize(path) + " imports echo-native-loader implementation classes");
                    }
                }
            }
        }
    }
}
