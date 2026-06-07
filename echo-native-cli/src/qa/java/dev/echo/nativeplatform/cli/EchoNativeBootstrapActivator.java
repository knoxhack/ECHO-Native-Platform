package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain;
import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class EchoNativeBootstrapActivator {
    EchoNativeBootstrapActivationOutcome activate(
            String packId,
            Path fixture,
            List<EchoNativeAddonDescriptor> descriptors,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> readiness = data(reports.get("phase13-m22-readiness.json"));
        Map<String, Object> safety = data(reports.get("native-live-activation-safety-gate.json"));
        Map<String, Object> markerContract = data(reports.get("native-live-activation-marker-contract.json"));
        Map<String, Object> bootstrapStatus = data(reports.get("native-module-bootstrap-status.json"));
        boolean ready = bool(readiness, "phase13M22Ready")
                && bool(safety, "bootstrapWrapperAllowedNext")
                && bool(markerContract, "markerContractReady")
                && diagnostics.stream().noneMatch(EchoNativeBootstrapActivator::isBlocking);

        String markerPathText = String.valueOf(markerContract.getOrDefault("markerPath", "isolated-runtime/game/echo-native/module-activation.json"));
        Path markerPath = fixture.resolve(markerPathText).normalize();
        List<String> modules = moduleIds(bootstrapStatus);
        Map<String, String> nativeEntrypoints = nativeEntrypoints(descriptors, modules, fixture, bootstrapStatus);
        String moduleClasspath = moduleClasspath(bootstrapStatus, fixture);
        String gameDir = fixture.resolve("isolated-runtime/game").toAbsolutePath().normalize().toString();

        MarkerWrite markerWrite = MarkerWrite.skipped(markerPath);
        if (ready) {
            String previousClasspath = System.getProperty("echo.native.moduleClasspath");
            String previousGameDir = System.getProperty("echo.native.gameDir");
            try {
                if (!moduleClasspath.isBlank()) {
                    System.setProperty("echo.native.moduleClasspath", moduleClasspath);
                }
                System.setProperty("echo.native.gameDir", gameDir);
                EchoNativeBootstrapMain.writeActivationMarker(markerPath, packId, "", modules, nativeEntrypoints);
            } finally {
                if (previousClasspath == null) {
                    System.clearProperty("echo.native.moduleClasspath");
                } else {
                    System.setProperty("echo.native.moduleClasspath", previousClasspath);
                }
                if (previousGameDir == null) {
                    System.clearProperty("echo.native.gameDir");
                } else {
                    System.setProperty("echo.native.gameDir", previousGameDir);
                }
            }
            markerWrite = MarkerWrite.written(markerPath);
        } else if (diagnostics.stream().noneMatch(EchoNativeBootstrapActivator::isBlocking)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BOOTSTRAP-ACTIVATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Bootstrap activation marker is blocked",
                    "The authorized bootstrap activation command requires PASS M22 readiness, safety gate, marker contract, and bootstrap status reports.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/phase13-m22-readiness.json"),
                    "Regenerate phase13 plan live-activation before activation."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean complete = markerWrite.written() && sortedDiagnostics.stream().noneMatch(EchoNativeBootstrapActivator::isBlocking);

        return new EchoNativeBootstrapActivationOutcome(
                packId,
                activationResult(packId, modules.size(), markerWrite, complete, sortedDiagnostics),
                markerReport(packId, modules, markerWrite, complete, sortedDiagnostics),
                safetyStatus(packId, complete, sortedDiagnostics),
                m22Completion(packId, modules.size(), complete, sortedDiagnostics),
                m23Readiness(packId, modules.size(), complete, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> activationResult(
            String packId,
            int moduleCount,
            MarkerWrite markerWrite,
            boolean complete,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Map<String, Object> data = base("phase13_m22_native_bootstrap_activation_result", diagnostics);
        data.put("activationMarkerWritten", markerWrite.written());
        data.put("bootstrapMain", EchoNativeBootstrapMain.MAIN_CLASS);
        data.put("externalCommandExecuted", false);
        data.put("gameMainHandedOff", false);
        data.put("markerByteSize", markerWrite.byteSize());
        data.put("markerPath", relativePath(markerWrite.path()));
        data.put("markerSha256", markerWrite.sha256());
        data.put("moduleCount", moduleCount);
        data.put("phase13M22Complete", complete);
        data.put("summary", complete
                ? "Authorized bootstrap activation marker was written to the isolated runtime."
                : "Authorized bootstrap activation marker was not written because prerequisite reports are blocked.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> markerReport(
            String packId,
            List<String> modules,
            MarkerWrite markerWrite,
            boolean complete,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Map<String, Object> data = base("phase13_m22_native_live_activation_marker", diagnostics);
        data.put("activationMarkerPresent", markerWrite.written() && Files.isRegularFile(markerWrite.path()));
        data.put("activationMarkerWritten", markerWrite.written());
        data.put("gameplayHooksVerified", false);
        data.put("markerByteSize", markerWrite.byteSize());
        data.put("markerPath", relativePath(markerWrite.path()));
        data.put("markerSha256", markerWrite.sha256());
        data.put("moduleCount", modules.size());
        data.put("modules", modules.stream()
                .sorted(String::compareTo)
                .map(EchoNativeBootstrapActivator::moduleMarker)
                .toList());
        data.put("phase13M22Complete", complete);
        data.put("summary", "Activation marker proves native bootstrap marker visibility, not registry-backed gameplay hooks.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> safetyStatus(
            String packId,
            boolean complete,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m22_native_live_activation_safety_status", diagnostics);
        data.put("activationMarkerWritten", complete);
        data.put("addonCodeExecuted", false);
        data.put("externalCommandExecuted", false);
        data.put("gameMainHandedOff", false);
        data.put("isolatedRuntimeMutationOnly", complete);
        data.put("phase13M22Complete", complete);
        data.put("userInstallMutationAllowed", false);
        data.put("summary", complete
                ? "M22 wrote only an isolated-runtime activation marker and left unsafe runtime work disabled."
                : "M22 activation remains blocked.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> m22Completion(
            String packId,
            int moduleCount,
            boolean complete,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m22_completion", diagnostics);
        data.put("activationMarkerModuleCount", complete ? moduleCount : 0);
        data.put("ashfallPlayableBetaReady", false);
        data.put("phase13M22Complete", complete);
        data.put("phase13M23Ready", complete);
        data.put("summary", complete
                ? "M22 is complete: the isolated native bootstrap activation marker exists."
                : "M22 is not complete.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> m23Readiness(
            String packId,
            int moduleCount,
            boolean complete,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m23_readiness", diagnostics);
        data.put("activationMarkerModuleCount", complete ? moduleCount : 0);
        data.put("ashfallPlayableBetaReady", false);
        data.put("nextCommand", "phase13 verify gameplay-hooks <fixture>");
        data.put("nextMilestone", "phase13.m23.gameplay_hook_evidence");
        data.put("phase13M23Ready", complete);
        data.put("summary", complete
                ? "M23 may verify gameplay hook evidence after the bootstrap marker has landed."
                : "M23 remains blocked until M22 writes an activation marker.");
        data.put("packId", packId);
        return data;
    }

    private static Map<String, Object> moduleMarker(String id) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("activationMarkerWritten", true);
        item.put("id", id);
        item.put("liveGameplayHookVerified", false);
        item.put("state", "activation_marker_written");
        return item;
    }

    private static Map<String, String> nativeEntrypoints(
            List<EchoNativeAddonDescriptor> descriptors,
            List<String> modules,
            Path fixture,
            Map<String, Object> bootstrapStatus
    ) {
        Map<String, String> entrypoints = new LinkedHashMap<>();
        descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .forEach(descriptor -> {
                    Object raw = descriptor.access().get("nativeEntrypoint");
                    String entrypoint = raw == null ? "" : String.valueOf(raw).trim();
                    if (!entrypoint.isBlank()) {
                        entrypoints.put(descriptor.id(), entrypoint);
                    }
        });
        workspaceAddonNativeEntrypoints(fixture, modules).forEach(entrypoints::putIfAbsent);
        workspaceAddonNativeModuleClasses(fixture, modules).forEach(entrypoints::putIfAbsent);
        moduleJarNativeEntrypoints(fixture, bootstrapStatus).forEach(entrypoints::putIfAbsent);
        return Map.copyOf(entrypoints);
    }

    private static Map<String, String> workspaceAddonNativeEntrypoints(Path fixture, List<String> modules) {
        Path workspaceRoot = workspaceRoot(fixture);
        if (workspaceRoot == null) {
            return Map.of();
        }
        Path addonsRoot = workspaceRoot.resolve("addons");
        if (!Files.isDirectory(addonsRoot)) {
            return Map.of();
        }
        List<String> moduleIds = modules.stream().distinct().toList();
        Map<String, String> entrypoints = new LinkedHashMap<>();
        try (var stream = Files.walk(addonsRoot)) {
            stream.filter(path -> path.getFileName().toString().equals("echo.mod.json"))
                    .sorted()
                    .forEach(descriptor -> readWorkspaceNativeEntrypoint(descriptor, moduleIds)
                            .forEach(entrypoints::putIfAbsent));
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.copyOf(entrypoints);
    }

    private static Map<String, String> readWorkspaceNativeEntrypoint(Path descriptor, List<String> moduleIds) {
        try {
            Map<String, Object> json = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(descriptor)));
            String id = String.valueOf(json.getOrDefault("id", "")).trim();
            if (id.isBlank() || !moduleIds.contains(id)) {
                return Map.of();
            }
            Map<String, Object> access = EchoNativeJson.asObject(json.get("access"));
            String entrypoint = String.valueOf(access.getOrDefault("nativeEntrypoint", "")).trim();
            if (entrypoint.isBlank()) {
                return Map.of();
            }
            return Map.of(id, entrypoint);
        } catch (IOException | RuntimeException ignored) {
            return Map.of();
        }
    }

    private static Map<String, String> workspaceAddonNativeModuleClasses(Path fixture, List<String> modules) {
        Path workspaceRoot = workspaceRoot(fixture);
        if (workspaceRoot == null) {
            return Map.of();
        }
        Path addonsRoot = workspaceRoot.resolve("addons");
        if (!Files.isDirectory(addonsRoot)) {
            return Map.of();
        }
        List<String> moduleIds = modules.stream().distinct().toList();
        Map<String, String> entrypoints = new LinkedHashMap<>();
        try (var stream = Files.walk(addonsRoot)) {
            stream.filter(path -> path.getFileName().toString().endsWith("NativeModule.java"))
                    .sorted()
                    .forEach(source -> readWorkspaceNativeModuleClass(source, moduleIds)
                            .forEach(entrypoints::putIfAbsent));
        } catch (IOException ignored) {
            return Map.of();
        }
        return Map.copyOf(entrypoints);
    }

    private static Map<String, String> moduleJarNativeEntrypoints(Path fixture, Map<String, Object> bootstrapStatus) {
        Map<String, Path> buildOutputJars = workspaceBuildOutputJars(fixture);
        Map<String, String> entrypoints = new LinkedHashMap<>();
        for (Map<String, Object> module : moduleList(bootstrapStatus)) {
            String id = String.valueOf(module.getOrDefault("id", "")).trim();
            String localPath = String.valueOf(module.getOrDefault("localPath", "")).trim();
            if (id.isBlank() || localPath.isBlank()) {
                continue;
            }
            Path local = resolveModuleClasspath(localPath, fixture);
            List<Path> candidates = new ArrayList<>();
            if (Files.isRegularFile(local)) {
                candidates.add(local);
            }
            Path buildOutput = local.getFileName() == null ? null : buildOutputJars.get(local.getFileName().toString());
            if (buildOutput != null && Files.isRegularFile(buildOutput)) {
                candidates.add(buildOutput);
            }
            for (Path candidate : candidates) {
                String entrypoint = nativeModuleClassInJar(candidate);
                if (!entrypoint.isBlank()) {
                    entrypoints.putIfAbsent(id, entrypoint);
                    break;
                }
            }
        }
        return Map.copyOf(entrypoints);
    }

    private static String nativeModuleClassInJar(Path jar) {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()
                        || !name.endsWith("NativeModule.class")
                        || name.contains("$")
                        || !name.startsWith("com/")) {
                    continue;
                }
                return name.substring(0, name.length() - ".class".length()).replace('/', '.');
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }

    private static Map<String, String> readWorkspaceNativeModuleClass(Path source, List<String> moduleIds) {
        try {
            Path projectRoot = source;
            while (projectRoot != null && !Files.isRegularFile(projectRoot.resolve("src/main/resources/META-INF/echo.mod.json"))) {
                projectRoot = projectRoot.getParent();
            }
            if (projectRoot == null) {
                return Map.of();
            }
            Map<String, Object> descriptor = EchoNativeJson.asObject(EchoNativeJson.parse(
                    Files.readString(projectRoot.resolve("src/main/resources/META-INF/echo.mod.json"))));
            String id = String.valueOf(descriptor.getOrDefault("id", "")).trim();
            if (id.isBlank() || !moduleIds.contains(id)) {
                return Map.of();
            }
            String text = Files.readString(source);
            Matcher packageMatcher = Pattern.compile("(?m)^package\\s+([\\w.]+);").matcher(text);
            if (!packageMatcher.find()) {
                return Map.of();
            }
            String className = source.getFileName().toString().replaceFirst("\\.java$", "");
            return Map.of(id, packageMatcher.group(1) + "." + className);
        } catch (IOException | RuntimeException ignored) {
            return Map.of();
        }
    }

    private static Path workspaceRoot(Path fixture) {
        Path current = fixture.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("addons"))
                    && Files.isDirectory(current.resolve("echo-native-platform"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String moduleClasspath(Map<String, Object> bootstrapStatus, Path fixture) {
        Map<String, Path> buildOutputJars = workspaceBuildOutputJars(fixture);
        Map<String, Path> classpath = new LinkedHashMap<>();
        for (Map<String, Object> module : moduleList(bootstrapStatus)) {
            String localPath = String.valueOf(module.getOrDefault("localPath", "")).trim();
            if (localPath.isBlank()) {
                continue;
            }
            Path local = resolveModuleClasspath(localPath, fixture);
            if (Files.isRegularFile(local)) {
                classpath.put(local.toAbsolutePath().normalize().toString(), local);
            }
            Path buildOutput = buildOutputJars.get(local.getFileName().toString());
            if (buildOutput != null && Files.isRegularFile(buildOutput)) {
                classpath.put(buildOutput.toAbsolutePath().normalize().toString(), buildOutput);
            }
        }
        return classpath.keySet().stream().collect(Collectors.joining(java.io.File.pathSeparator));
    }

    private static Map<String, Path> workspaceBuildOutputJars(Path fixture) {
        Map<String, Path> jars = new LinkedHashMap<>();
        Path workspaceRoot = workspaceRoot(fixture);
        if (workspaceRoot != null) {
            collectJarNames(workspaceRoot.resolve("build").resolve("tmp").resolve("echo-native-m17-mods"), jars);
        }
        Path userHome = Path.of(System.getProperty("user.home", ""));
        if (!userHome.toString().isBlank()) {
            collectJarNames(userHome.resolve("AppData").resolve("Local").resolve("EchoGradleBuild").resolve("Echo"), jars);
        }
        return Map.copyOf(jars);
    }

    private static void collectJarNames(Path root, Map<String, Path> jars) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var stream = Files.walk(root, 3)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(path -> jars.put(path.getFileName().toString(), path));
        } catch (IOException ignored) {
            // Missing or unreadable build-output caches should not block fixture-local activation.
        }
    }

    private static Path resolveModuleClasspath(String value, Path fixture) {
        Path raw = Path.of(value);
        if (Files.isRegularFile(raw)) {
            return raw;
        }
        Path fixtureRelative = fixture.resolve(value).normalize();
        if (Files.isRegularFile(fixtureRelative)) {
            return fixtureRelative;
        }
        return raw.toAbsolutePath().normalize();
    }

    private static List<String> moduleIds(Map<String, Object> bootstrapStatus) {
        List<String> modules = new ArrayList<>();
        for (Map<String, Object> module : moduleList(bootstrapStatus)) {
            String id = String.valueOf(module.getOrDefault("id", ""));
            if (!id.isBlank()) {
                modules.add(id);
            }
        }
        modules.sort(String::compareTo);
        return List.copyOf(modules);
    }

    private static List<Map<String, Object>> moduleList(Map<String, Object> bootstrapStatus) {
        Object value = bootstrapStatus.get("modules");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> modules = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            if (!module.isEmpty()) {
                modules.add(module);
            }
        }
        return List.copyOf(modules);
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BOOTSTRAP-ACTIVATION-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Bootstrap activation required report missing",
                    "Bootstrap activation requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Run phase13 plan live-activation before activation."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkReport(
            String reportName,
            Path path,
            Map<String, Object> report,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-BOOTSTRAP-ACTIVATION-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Bootstrap activation upstream report is not PASS",
                    "Bootstrap activation requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Resolve upstream M22 planning reports before activation."
            ));
        }
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("externalCommandExecuted", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("nativeExtractionStarted", false);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String sha256Of(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path))).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record MarkerWrite(Path path, boolean written, long byteSize, String sha256) {
        private static MarkerWrite skipped(Path path) {
            return new MarkerWrite(path, false, 0, "");
        }

        private static MarkerWrite written(Path path) throws IOException {
            return new MarkerWrite(path, true, Files.size(path), sha256Of(path));
        }
    }
}
