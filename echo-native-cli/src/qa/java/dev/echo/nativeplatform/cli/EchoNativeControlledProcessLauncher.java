package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain;
import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class EchoNativeControlledProcessLauncher {
    private static final long TIMEOUT_MILLIS = 90000L;
    private static final String PLACEHOLDER_MAIN_CLASS = "minecraft-client-main-class";

    EchoNativeControlledProcessLaunchOutcome launch(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> preview = data(reports.get("real-process-command-line-preview.json"));
        Map<String, Object> environment = data(reports.get("real-process-environment-plan.json"));
        String executable = String.valueOf(preview.getOrDefault("executable", "java"));
        String previewMainClass = String.valueOf(preview.getOrDefault("mainClass", ""));
        String resolvedMainClass = resolveMainClass(fixture, previewMainClass, diagnostics, packId);
        boolean mainClassResolved = !resolvedMainClass.isBlank() && !PLACEHOLDER_MAIN_CLASS.equals(resolvedMainClass);
        String classpath = previewArgument(preview, "classpath");
        String libraryPath = previewArgument(preview, "libraryPath");
        String gameDirArgument = previewArgument(preview, "gameDir");
        String workingDirectory = String.valueOf(environment.getOrDefault("workingDirectory", fixture.resolve("isolated-runtime/game").toString()));
        Path workingDirectoryPath = resolveWorkspacePath(workingDirectory);
        Path nativesDirectory = workingDirectoryPath.getParent().resolve("natives").normalize();
        LiveClientLaunchInputs liveClientInputs = liveClientLaunchInputs(gameDirArgument, workingDirectoryPath);

        boolean upstreamReady = diagnostics.stream().noneMatch(EchoNativeControlledProcessLauncher::isBlocking);
        boolean canStartProcess = upstreamReady
                && mainClassResolved
                && !classpath.isBlank()
                && Files.isDirectory(workingDirectoryPath);

        ProcessRun run = ProcessRun.skipped();
        NativeExtractionRun extraction = NativeExtractionRun.skipped(nativesDirectory);
        if (canStartProcess) {
            extraction = materializeNatives(fixture, nativesDirectory, packId, diagnostics);
            canStartProcess = extraction.ready();
        }
        if (canStartProcess) {
            try {
                run = runProcess(executable, libraryPath, classpath, resolvedMainClass, liveClientInputs, workingDirectoryPath, descriptors, packId);
            } catch (IOException exception) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-CONTROLLED-LAUNCH-PROCESS-START-FAILED",
                        EchoNativeIssueSeverity.ERROR,
                        "Controlled native launch process could not start",
                        "The authorized launch command could not start the bounded Java process.",
                        null,
                        packId,
                        List.of("reports/echo-native/" + packId + "/controlled-process-launch-result.json"),
                        "Verify the local Java executable and fixture-local runtime inputs before retrying the controlled launch."
                ));
            }
        }

        if (run.started() && (run.exitCode() != 0 || run.timedOut())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-NONZERO-EXIT",
                    EchoNativeIssueSeverity.WARNING,
                    "Controlled native launch process did not complete successfully",
                    "The explicitly authorized process boundary started and was captured, but it exited nonzero or timed out before a playable state.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/controlled-process-launch-result.json"),
                    "Use the captured failure stage to add the next missing runtime dependency or launch argument before another controlled launch."
            ));
        }
        if (!mainClassResolved) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-MAIN-CLASS-UNRESOLVED",
                    EchoNativeIssueSeverity.WARNING,
                    "Controlled launch main class is unresolved",
                    "The launch preview still contains a placeholder and no fixture-local version manifest supplied a replacement.",
                    null,
                    packId,
                    List.of(relativePath(fixture.resolve("local-runtime/minecraft/26.1.2/metadata/26.1.2.json"))),
                    "Stage a fixture-local version manifest with a reviewed mainClass value before starting the controlled process."
            ));
        }
        if (upstreamReady && !Files.isDirectory(workingDirectoryPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-WORKDIR-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled launch working directory is missing",
                    "The authorized launch command requires the fixture-local isolated runtime game directory.",
                    null,
                    packId,
                    List.of(relativePath(workingDirectoryPath)),
                    "Run phase13 prepare isolated-runtime <fixture> before starting the controlled launch."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean blocking = sortedDiagnostics.stream().anyMatch(EchoNativeControlledProcessLauncher::isBlocking);

        return new EchoNativeControlledProcessLaunchOutcome(
                packId,
                controlledProcessLaunchPlan(packId, fixture, resolvedMainClass, mainClassResolved, classpath, workingDirectoryPath, liveClientInputs, canStartProcess, extraction, sortedDiagnostics),
                controlledProcessLaunchSafetyGate(packId, upstreamReady, canStartProcess, extraction, sortedDiagnostics),
                controlledProcessLaunchResult(packId, run, workingDirectoryPath, liveClientInputs, mainClassResolved, blocking, extraction, sortedDiagnostics),
                controlledProcessOutputCapture(packId, run, workingDirectoryPath, liveClientInputs, extraction, sortedDiagnostics),
                controlledProcessRollbackStatus(packId, run, extraction, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static NativeExtractionRun materializeNatives(
            Path fixture,
            Path nativesDirectory,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Path archive = fixture.resolve("local-runtime/minecraft/26.1.2/natives/minecraft-26.1.2-natives.zip");
        if (!Files.isRegularFile(archive)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-NATIVE-ARCHIVE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled launch native archive is missing",
                    "The authorized controlled launch command requires the fixture-local native archive before it can materialize isolated native libraries.",
                    null,
                    packId,
                    List.of(relativePath(archive)),
                    "Stage the reviewed fixture-local native archive before retrying the controlled launch."
            ));
            return new NativeExtractionRun(true, false, archive, nativesDirectory, List.of());
        }

        Files.createDirectories(nativesDirectory);
        List<String> extracted = new ArrayList<>();
        try (ZipInputStream outer = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry nativeJarEntry;
            while ((nativeJarEntry = outer.getNextEntry()) != null) {
                if (nativeJarEntry.isDirectory() || !nativeJarEntry.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    continue;
                }
                byte[] jarBytes = readAll(outer);
                try (ZipInputStream nativeJar = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
                    ZipEntry nativeEntry;
                    while ((nativeEntry = nativeJar.getNextEntry()) != null) {
                        if (nativeEntry.isDirectory() || !isNativeLibrary(nativeEntry.getName())) {
                            continue;
                        }
                        String fileName = Path.of(nativeEntry.getName()).getFileName().toString();
                        Path target = nativesDirectory.resolve(fileName).normalize();
                        if (!target.startsWith(nativesDirectory)) {
                            throw new IOException("Native archive entry escaped isolated natives directory: " + nativeEntry.getName());
                        }
                        writeNativeFileIfChanged(target, readAll(nativeJar));
                        extracted.add(relativePath(target));
                    }
                }
            }
        } catch (IOException exception) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-NATIVE-EXTRACTION-FAILED",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled launch native extraction failed",
                    "The authorized controlled launch command could not materialize native libraries inside the fixture-local isolated runtime workspace.",
                    null,
                    packId,
                    List.of(relativePath(archive), relativePath(nativesDirectory)),
                    "Verify the fixture-local native archive contents and retry the controlled launch."
            ));
            return new NativeExtractionRun(true, false, archive, nativesDirectory, List.of());
        }

        List<String> sortedExtracted = extracted.stream().sorted().toList();
        if (sortedExtracted.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-NATIVE-EXTRACTION-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled launch native extraction produced no native libraries",
                    "The fixture-local native archive was present, but no supported native library files were materialized.",
                    null,
                    packId,
                    List.of(relativePath(archive)),
                    "Rebuild the fixture-local native archive from reviewed local native artifacts."
            ));
        }
        return new NativeExtractionRun(true, !sortedExtracted.isEmpty(), archive, nativesDirectory, sortedExtracted);
    }

    private static boolean isNativeLibrary(String entryName) {
        String lower = entryName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".dll") || lower.endsWith(".so") || lower.endsWith(".dylib");
    }

    private static byte[] readAll(ZipInputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        stream.transferTo(output);
        return output.toByteArray();
    }

    private static void writeNativeFileIfChanged(Path target, byte[] bytes) throws IOException {
        if (Files.isRegularFile(target)) {
            try {
                if (Arrays.equals(Files.readAllBytes(target), bytes)) {
                    return;
                }
            } catch (IOException ignored) {
                // Try the write below so locked or unreadable changed natives still fail with the target path.
            }
        }
        Files.write(target, bytes);
    }

    private static ProcessRun runProcess(
            String executable,
            String libraryPath,
            String classpath,
            String mainClass,
            LiveClientLaunchInputs liveClientInputs,
            Path workingDirectory,
            List<EchoNativeAddonDescriptor> descriptors,
            String packId
    ) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(executable.isBlank() ? "java" : executable);
        if (!libraryPath.isBlank()) {
            command.add(resolveLibraryPath(libraryPath));
        }
        command.add("-Decho.native.loader=true");
        command.add("-D" + EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_PROPERTY
                + "=" + EchoNativeBootstrapMain.AUTHORIZED_HANDOFF_VALUE);
        nativeBootstrapProfile(descriptors).ifPresent(profile ->
                command.add("-Decho.native.bootstrap.profileClass=" + profile));
        Path gameDirectory = liveClientInputs.gameDirectory();
        Path assetsDirectory = workingDirectory.getParent().resolve("assets").toAbsolutePath().normalize();
        Files.createDirectories(gameDirectory);
        Files.createDirectories(assetsDirectory);
        Files.createDirectories(liveClientInputs.quickPlayPath().getParent());
        boolean useQuickPlay = quickPlayEnabled(packId, descriptors);
        if (useQuickPlay) {
            materializeQuickPlay(liveClientInputs);
        }

        command.add("-cp");
        command.add(resolveClasspath(classpath));
        command.add(EchoNativeBootstrapMain.MAIN_CLASS);
        command.add("--echo-marker");
        command.add(workingDirectory.resolve("echo-native/module-activation.json").toString());
        command.add("--echo-pack-id");
        command.add(packId);
        command.add("--echo-real-main");
        command.add(mainClass);
        command.add("--echo-handoff");
        descriptors.stream()
                .map(EchoNativeAddonDescriptor::id)
                .sorted()
                .forEach(moduleId -> {
                    command.add("--echo-module");
                    command.add(moduleId);
                });
        descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .forEach(descriptor -> nativeEntrypoint(descriptor).ifPresent(entrypoint -> {
                    command.add("--echo-native-entrypoint");
                    command.add(descriptor.id() + "=" + entrypoint);
                }));
        command.add("--username");
        command.add("EchoNativeTester");
        command.add("--version");
        command.add("26.1.2");
        command.add("--gameDir");
        command.add(gameDirectory.toString());
        command.add("--assetsDir");
        command.add(assetsDirectory.toString());
        command.add("--assetIndex");
        command.add("30");
        command.add("--uuid");
        command.add("00000000000000000000000000000000");
        command.add("--accessToken");
        command.add("0");
        command.add("--clientId");
        command.add("0");
        command.add("--xuid");
        command.add("0");
        command.add("--versionType");
        command.add("echo-native-beta");
        if (useQuickPlay) {
            command.add("--quickPlayPath");
            command.add(liveClientInputs.quickPlayPath().toString());
            liveClientInputs.quickPlaySave().ifPresent(save -> {
                command.add("--quickPlaySingleplayer");
                command.add(save.getFileName().toString());
            });
        }

        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(false)
                .start();
        ByteArrayOutputStream stdoutBytes = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBytes = new ByteArrayOutputStream();
        Thread stdoutCapture = startCaptureThread(process.getInputStream(), stdoutBytes, "EchoNativeControlledStdoutCapture");
        Thread stderrCapture = startCaptureThread(process.getErrorStream(), stderrBytes, "EchoNativeControlledStderrCapture");
        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            finished = false;
        }
        List<String> threadDump = List.of();
        if (!finished) {
            threadDump = captureThreadDump(executable, process.pid(), packId);
            process.destroyForcibly();
            try {
                process.waitFor(5L, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        joinCaptureThread(stdoutCapture);
        joinCaptureThread(stderrCapture);
        String stdout = new String(stdoutBytes.toByteArray(), StandardCharsets.UTF_8);
        String stderr = new String(stderrBytes.toByteArray(), StandardCharsets.UTF_8);
        int exitCode = finished ? process.exitValue() : -1;
        return new ProcessRun(true, finished, !finished, exitCode, sanitizeLines(stdout, packId), sanitizeLines(stderr, packId), threadDump);
    }

    private static Thread startCaptureThread(InputStream stream, ByteArrayOutputStream output, String name) {
        Thread thread = new Thread(() -> {
            try (InputStream input = stream) {
                input.transferTo(output);
            } catch (IOException ignored) {
                // The bounded process may be destroyed during timeout handling; partial output is still useful evidence.
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void joinCaptureThread(Thread thread) {
        try {
            thread.join(5000L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> captureThreadDump(String executable, long pid, String packId) {
        List<String> command = new ArrayList<>();
        command.add(resolveJcmdExecutable(executable));
        command.add(Long.toString(pid));
        command.add("Thread.print");
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5L, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return List.of("jcmd Thread.print timed out for controlled process " + pid);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return sanitizeOrderedLines(output, packId);
        } catch (IOException exception) {
            return List.of("jcmd Thread.print failed: " + exception.getClass().getSimpleName());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return List.of("jcmd Thread.print interrupted");
        }
    }

    private static String resolveJcmdExecutable(String executable) {
        if (!executable.isBlank()) {
            Path javaPath = Path.of(executable);
            Path sibling = javaPath.resolveSibling(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                    ? "jcmd.exe"
                    : "jcmd");
            if (Files.isRegularFile(sibling)) {
                return sibling.toString();
            }
        }
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "jcmd.exe" : "jcmd";
    }

    private static Optional<String> nativeEntrypoint(EchoNativeAddonDescriptor descriptor) {
        Object raw = descriptor.access().get("nativeEntrypoint");
        String value = raw == null ? "" : String.valueOf(raw).trim();
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static Optional<String> nativeBootstrapProfile(List<EchoNativeAddonDescriptor> descriptors) {
        return descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> descriptor.access().get("nativeBootstrapProfile"))
                .map(raw -> raw == null ? "" : String.valueOf(raw).trim())
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static boolean quickPlayEnabled(String packId, List<EchoNativeAddonDescriptor> descriptors) {
        String property = System.getProperty("echo.native.tester.quickPlay", "").trim();
        if (!property.isBlank()) {
            return Boolean.parseBoolean(property);
        }
        String env = System.getenv("ECHO_NATIVE_TESTER_QUICKPLAY");
        if (env != null && !env.isBlank()) {
            return Boolean.parseBoolean(env);
        }
        return !"ashfall".equalsIgnoreCase(packId)
                && nativeBootstrapProfile(descriptors)
                .map(profile -> !profile.toLowerCase(Locale.ROOT).contains("ashfall"))
                .orElse(true);
    }

    private static Optional<Path> latestSave(Path savesDirectory) {
        if (!Files.isDirectory(savesDirectory)) {
            return Optional.empty();
        }
        try (var stream = Files.list(savesDirectory)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve("level.dat")))
                    .max(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path.resolve("level.dat"));
                        } catch (IOException exception) {
                            return java.nio.file.attribute.FileTime.fromMillis(0L);
                        }
                    }));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static LiveClientLaunchInputs liveClientLaunchInputs(String gameDirArgument, Path workingDirectory) {
        Path gameDirectory = resolveArgumentPath(
                gameDirArgument.startsWith("--gameDir=") ? gameDirArgument.substring("--gameDir=".length()) : workingDirectory.toString()
        );
        Path quickPlayPath = gameDirectory.resolve("echo-native/quickplay.json").normalize();
        return new LiveClientLaunchInputs(gameDirectory, quickPlayPath, latestSave(gameDirectory.resolve("saves")));
    }

    private static void materializeQuickPlay(LiveClientLaunchInputs inputs) throws IOException {
        if (inputs.quickPlaySave().isEmpty()) {
            return;
        }
        Path save = inputs.quickPlaySave().orElseThrow();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "singleplayer");
        entry.put("id", save.getFileName().toString());
        entry.put("name", save.getFileName().toString());
        entry.put("gamemode", "creative");
        entry.put("source", "EchoNativeControlledProcessLauncher.materializeQuickPlay");
        entry.put("levelDatLastModified", Files.getLastModifiedTime(save.resolve("level.dat")).toInstant().toString());
        Files.createDirectories(inputs.quickPlayPath().getParent());
        Files.writeString(inputs.quickPlayPath(), EchoNativeJson.write(List.of(entry)), StandardCharsets.UTF_8);
    }

    private static String resolveMainClass(
            Path fixture,
            String previewMainClass,
            List<EchoNativeDiagnostic> diagnostics,
            String packId
    ) {
        if (!previewMainClass.isBlank() && !PLACEHOLDER_MAIN_CLASS.equals(previewMainClass)) {
            return previewMainClass;
        }
        Path versionManifest = fixture.resolve("local-runtime/minecraft/26.1.2/metadata/26.1.2.json");
        if (!Files.isRegularFile(versionManifest)) {
            return previewMainClass;
        }
        try {
            Map<String, Object> manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(versionManifest)));
            String mainClass = String.valueOf(manifest.getOrDefault("mainClass", ""));
            if (!mainClass.isBlank()) {
                return mainClass;
            }
        } catch (RuntimeException | IOException exception) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Fixture-local version manifest could not be parsed",
                    "The controlled launch command could not parse the fixture-local version manifest for the client main class.",
                    null,
                    packId,
                    List.of(relativePath(versionManifest)),
                    "Replace the fixture-local version manifest with reviewed valid JSON before controlled launch."
            ));
        }
        return previewMainClass;
    }

    private static String previewArgument(Map<String, Object> preview, String id) {
        Object arguments = preview.get("commandLinePreview");
        if (!(arguments instanceof List<?> list)) {
            return "";
        }
        for (Object raw : list) {
            Map<String, Object> item = EchoNativeJson.asObject(raw);
            if (id.equals(String.valueOf(item.get("id")))) {
                return String.valueOf(item.getOrDefault("value", ""));
            }
        }
        return "";
    }

    private static String resolveClasspath(String classpath) {
        Path root = Path.of("").toAbsolutePath().normalize();
        List<String> entries = new ArrayList<>();
        for (String raw : classpath.split(";")) {
            if (raw.isBlank()) {
                continue;
            }
            Path path = Path.of(raw);
            entries.add(path.isAbsolute() ? path.normalize().toString() : root.resolve(path).normalize().toString());
        }
        return String.join(System.getProperty("path.separator"), entries);
    }

    private static String resolveLibraryPath(String libraryPath) {
        String prefix = "-Djava.library.path=";
        if (!libraryPath.startsWith(prefix)) {
            return libraryPath;
        }
        String raw = libraryPath.substring(prefix.length());
        Path path = Path.of(raw);
        Path root = Path.of("").toAbsolutePath().normalize();
        Path resolved = path.isAbsolute() ? path.normalize() : root.resolve(path).normalize();
        return prefix + resolved;
    }

    private static Path resolveArgumentPath(String value) {
        Path path = Path.of(value);
        Path root = Path.of("").toAbsolutePath().normalize();
        return path.isAbsolute() ? path.normalize() : root.resolve(path).normalize();
    }

    private static Map<String, Object> controlledProcessLaunchPlan(
            String packId,
            Path fixture,
            String mainClass,
            boolean mainClassResolved,
            String classpath,
            Path workingDirectory,
            LiveClientLaunchInputs liveClientInputs,
            boolean canStartProcess,
            NativeExtractionRun extraction,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_controlled_process_launch_plan", diagnostics);
        data.put("authorizationFlag", "--authorized");
        data.put("classpathEntryCount", classpath.isBlank() ? 0 : classpath.split(";").length);
        data.put("controlledLaunchCommandReady", canStartProcess);
        data.put("fixture", relativePath(fixture));
        data.put("bootstrapMainClass", EchoNativeBootstrapMain.MAIN_CLASS);
        data.put("mainClass", mainClass);
        data.put("mainClassResolved", mainClassResolved);
        data.put("processEntrypoint", "echo_native_bootstrap_wrapper");
        putLiveClientLaunchInputs(data, liveClientInputs);
        putNativeExtraction(data, extraction);
        data.put("operatorAuthorizationAccepted", true);
        data.put("packId", packId);
        data.put("processStartEligible", canStartProcess);
        data.put("timeoutMillis", TIMEOUT_MILLIS);
        data.put("workingDirectory", relativePath(workingDirectory));
        data.put("summary", canStartProcess
                ? "Authorized controlled launch command is eligible to start the bounded process."
                : "Authorized controlled launch command stopped before process start because a required launch input is missing.");
        return data;
    }

    private static Map<String, Object> controlledProcessLaunchSafetyGate(
            String packId,
            boolean upstreamReady,
            boolean canStartProcess,
            NativeExtractionRun extraction,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_controlled_process_launch_safety_gate", diagnostics);
        data.put("automaticLaunchAllowed", false);
        data.put("boundedTimeoutMillis", TIMEOUT_MILLIS);
        putNativeExtraction(data, extraction);
        data.put("operatorAuthorizationAccepted", true);
        data.put("packId", packId);
        data.put("processExecutionAllowed", canStartProcess);
        data.put("safetyGatePassed", upstreamReady);
        data.put("userCacheMutationAllowed", false);
        data.put("userInstallMutationAllowed", false);
        data.put("summary", upstreamReady
                ? "Controlled launch safety gate accepted explicit authorization for an isolated bounded process."
                : "Controlled launch safety gate is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> controlledProcessLaunchResult(
            String packId,
            ProcessRun run,
            Path workingDirectory,
            LiveClientLaunchInputs liveClientInputs,
            boolean mainClassResolved,
            boolean blocking,
            NativeExtractionRun extraction,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_controlled_process_launch_result", diagnostics);
        Path marker = workingDirectory.resolve("echo-native/module-activation.json").normalize();
        Path probe = marker.getParent().resolve("live-client-probe.json").normalize();
        boolean runtimeMainClassReached = runtimeMainClassReached(run);
        boolean markerWritten = Files.isRegularFile(marker);
        Map<String, Object> markerData = markerWritten ? readJsonObject(marker) : Map.of();
        Map<String, Object> probeData = Files.isRegularFile(probe) ? readJsonObject(probe) : Map.of();
        data.put("commandExecuted", run.started());
        data.put("controlledFailure", run.started() && (run.exitCode() != 0 || run.timedOut()));
        data.put("exitCode", run.exitCode());
        data.put("gameClassesResolved", runtimeMainClassReached);
        data.put("gameProcessLaunched", false);
        data.put("launchAttemptBlocked", !run.started());
        putLiveClientLaunchInputs(data, liveClientInputs);
        putLiveClientProbe(data, probe, probeData);
        data.put("bootstrapMainClass", EchoNativeBootstrapMain.MAIN_CLASS);
        data.put("mainClassResolved", mainClassResolved);
        data.put("mainMenuReached", false);
        data.put("minecraftLaunchAttempted", run.started());
        data.put("nativeBootstrapHandoffRequested", Boolean.TRUE.equals(markerData.get("handoffRequested")));
        data.put("nativeBootstrapMarkerPath", relativePath(marker));
        data.put("nativeBootstrapMarkerWritten", markerWritten);
        data.put("nativeModuleActivationCount", activatedModuleCount(markerData));
        putNativeExtraction(data, extraction);
        data.put("packId", packId);
        data.put("processEntrypoint", run.started() ? "echo_native_bootstrap_wrapper" : "not_started");
        data.put("processFinished", run.finished());
        data.put("processLaunched", run.started());
        data.put("resultBlocking", blocking);
        data.put("runtimeMainClassReached", runtimeMainClassReached);
        data.put("timedOut", run.timedOut());
        data.put("summary", run.started()
                ? "Controlled Java process was started, captured, and stopped within the native loader boundary."
                : "Controlled launch stopped before process start.");
        return data;
    }

    private static Map<String, Object> controlledProcessOutputCapture(
            String packId,
            ProcessRun run,
            Path workingDirectory,
            LiveClientLaunchInputs liveClientInputs,
            NativeExtractionRun extraction,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_controlled_process_output_capture", diagnostics);
        Path marker = workingDirectory.resolve("echo-native/module-activation.json").normalize();
        Path probe = marker.getParent().resolve("live-client-probe.json").normalize();
        Map<String, Object> probeData = Files.isRegularFile(probe) ? readJsonObject(probe) : Map.of();
        data.put("commandExecuted", run.started());
        data.put("gameClassesResolved", runtimeMainClassReached(run));
        data.put("nativeBootstrapMarkerPath", relativePath(marker));
        data.put("nativeBootstrapMarkerWritten", Files.isRegularFile(marker));
        putLiveClientLaunchInputs(data, liveClientInputs);
        putLiveClientProbe(data, probe, probeData);
        putNativeExtraction(data, extraction);
        data.put("packId", packId);
        data.put("processLaunched", run.started());
        data.put("secretSafe", true);
        data.put("stderrLineCount", run.stderr().size());
        data.put("stderrTail", tail(run.stderr()));
        data.put("stdoutLineCount", run.stdout().size());
        data.put("stdoutTail", tail(run.stdout()));
        data.put("threadDumpCaptured", !run.threadDump().isEmpty());
        data.put("threadDumpLineCount", run.threadDump().size());
        data.put("threadDumpTail", tail(run.threadDump()));
        data.put("summary", run.started()
                ? "Controlled process output was captured with workspace and user paths redacted."
                : "No process output was captured because launch stopped before process start.");
        return data;
    }

    private static Map<String, Object> controlledProcessRollbackStatus(
            String packId,
            ProcessRun run,
            NativeExtractionRun extraction,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_controlled_process_rollback_status", diagnostics);
        putNativeExtraction(data, extraction);
        data.put("packId", packId);
        data.put("processLaunched", run.started());
        data.put("rollbackNeeded", false);
        data.put("rollbackPlanReady", true);
        data.put("userCacheMutated", false);
        data.put("userInstallMutated", false);
        data.put("summary", "No rollback action was required; controlled launch did not mutate user installs, caches, saves, or configs.");
        return data;
    }

    private static void putNativeExtraction(Map<String, Object> data, NativeExtractionRun extraction) {
        data.put("nativeArchive", relativePath(extraction.archive()));
        data.put("nativeExtractionScope", "fixture_isolated_runtime");
        data.put("nativeExtractionStarted", extraction.started());
        data.put("nativeExtractionReady", extraction.ready());
        data.put("nativeFilesExtracted", extraction.extractedFiles().size());
        data.put("nativesDirectory", relativePath(extraction.nativesDirectory()));
        data.put("isolatedRuntimeFilesystemMutated", extraction.started() && extraction.ready());
        data.put("userCacheMutated", false);
        data.put("userInstallMutated", false);
    }

    private static void putLiveClientLaunchInputs(Map<String, Object> data, LiveClientLaunchInputs inputs) {
        data.put("gameDir", relativePath(inputs.gameDirectory()));
        data.put("quickPlayPath", relativePath(inputs.quickPlayPath()));
        data.put("quickPlayPathParentExists", Files.isDirectory(inputs.quickPlayPath().getParent()));
        data.put("quickPlaySingleplayer", inputs.quickPlaySave()
                .map(path -> path.getFileName().toString())
                .orElse(""));
        data.put("quickPlaySelectedSavePath", inputs.quickPlaySave()
                .map(EchoNativeControlledProcessLauncher::relativePath)
                .orElse(""));
        data.put("quickPlayJsonMaterialized", Files.isRegularFile(inputs.quickPlayPath()));
        data.put("quickPlaySaveSelected", inputs.quickPlaySave().isPresent());
        data.put("quickPlaySaveHasLevelDat", inputs.quickPlaySave()
                .map(path -> Files.isRegularFile(path.resolve("level.dat")))
                .orElse(false));
    }

    private static void putLiveClientProbe(Map<String, Object> data, Path probe, Map<String, Object> probeData) {
        data.put("liveClientProbePath", relativePath(probe));
        data.put("liveClientProbeWritten", Files.isRegularFile(probe));
        data.put("liveClientProbeExecuted", Boolean.TRUE.equals(probeData.get("executed")));
        data.put("liveClientProbeState", String.valueOf(probeData.getOrDefault("state", "")));
        data.put("liveClientProbeAttempt", intValue(probeData.get("attempt"), -1));
        data.put("liveClientProbeClaimsGameplayParity", Boolean.TRUE.equals(probeData.get("moduleGameplayParityClaimed")));
        data.put("liveClientProbePlayerPresent", Boolean.TRUE.equals(probeData.get("playerPresent")));
        data.put("liveClientProbeGuiPresent", Boolean.TRUE.equals(probeData.get("guiPresent")));
        data.put("liveClientProbeLevelPresent", Boolean.TRUE.equals(probeData.get("levelPresent")));
        data.put("liveClientProbeScreenPresent", Boolean.TRUE.equals(probeData.get("screenPresent")));
        data.put("liveClientProbeScreenClass", String.valueOf(probeData.getOrDefault("screenClass", "")));
        data.put("liveClientProbeClientStateSummary", String.valueOf(probeData.getOrDefault("clientStateSummary", "")));
        data.put("liveClientProbeRuntimeAccessed", Boolean.TRUE.equals(probeData.get("clientRuntimeAccessed")));
        data.put("liveClientProbeClientThreadScheduled", Boolean.TRUE.equals(probeData.get("clientThreadScheduled")));
        data.put("liveClientProbeLifecycleHookAttached", Boolean.TRUE.equals(probeData.get("liveClientLifecycleHookAttached")));
        data.put("liveClientProbeGameLoadFinished", Boolean.TRUE.equals(probeData.get("gameLoadFinished")));
        data.put("liveClientProbeMinecraftRunning", Boolean.TRUE.equals(probeData.get("minecraftRunning")));
        data.put("liveClientProbeWindowPresent", Boolean.TRUE.equals(probeData.get("windowPresent")));
        data.put("liveClientProbeSingleplayerServerPresent", Boolean.TRUE.equals(probeData.get("singleplayerServerPresent")));
        data.put("liveClientProbeConnectionPresent", Boolean.TRUE.equals(probeData.get("connectionPresent")));
        Object threadSnapshot = probeData.get("threadSnapshot");
        data.put("liveClientProbeThreadSnapshotCount", threadSnapshot instanceof List<?> list ? list.size() : 0);
        data.put("liveClientProbeThreadSnapshotNames", threadSnapshotNames(threadSnapshot));
    }

    private static List<String> threadSnapshotNames(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> thread = EchoNativeJson.asObject(item);
            String name = String.valueOf(thread.getOrDefault("name", ""));
            String state = String.valueOf(thread.getOrDefault("state", ""));
            if (!name.isBlank()) {
                names.add(state.isBlank() ? name : name + " [" + state + "]");
            }
        }
        return names.stream().limit(12).toList();
    }

    private static List<String> sanitizeLines(String text, String packId) {
        if (text.isBlank()) {
            return List.of();
        }
        return sanitizeOrderedLines(text, packId).stream()
                .map(line -> line.length() > 240 ? line.substring(0, 240) + "..." : line)
                .filter(line -> !line.isBlank())
                .limit(20)
                .toList();
    }

    private static List<String> sanitizeOrderedLines(String text, String packId) {
        if (text.isBlank()) {
            return List.of();
        }
        String workspace = Path.of("").toAbsolutePath().normalize().toString();
        String home = System.getProperty("user.home", "");
        return text.lines()
                .map(line -> line.replace(workspace, "<native-workspace>")
                        .replace(home, "<user-home>"))
                .map(line -> line.length() > 240 ? line.substring(0, 240) + "..." : line)
                .filter(line -> !line.isBlank())
                .limit(200)
                .toList();
    }

    private static List<String> tail(List<String> lines) {
        if (lines.size() <= 8) {
            return lines;
        }
        return lines.subList(lines.size() - 8, lines.size());
    }

    private static boolean runtimeMainClassReached(ProcessRun run) {
        return run.started()
                && run.stderr().stream().noneMatch(line -> line.contains("Could not find or load main class"));
    }

    private static Map<String, Object> readJsonObject(Path path) {
        try {
            return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(path)));
        } catch (RuntimeException | IOException exception) {
            return Map.of();
        }
    }

    private static int activatedModuleCount(Map<String, Object> markerData) {
        Object raw = markerData.get("modules");
        if (!(raw instanceof List<?> list)) {
            return 0;
        }
        int count = 0;
        for (Object item : list) {
            Map<String, Object> module = EchoNativeJson.asObject(item);
            if (Boolean.TRUE.equals(module.get("nativeModuleActivated"))) {
                count++;
            }
        }
        return count;
    }

    private static int intValue(Object raw, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException exception) {
            return fallback;
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
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
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
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled launch required report missing",
                    "The authorized controlled launch command requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate native loader beta readiness reports before controlled launch."
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
        if (!"PASS".equals(String.valueOf(report.getOrDefault("status", "MISSING")))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTROLLED-LAUNCH-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled launch upstream report is not PASS",
                    "The authorized launch command requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(path)),
                    "Resolve upstream native loader beta diagnostics before controlled launch."
            ));
        }
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static Path resolveWorkspacePath(String path) {
        Path value = Path.of(path);
        if (value.isAbsolute()) {
            return value.normalize();
        }
        return Path.of("").toAbsolutePath().normalize().resolve(value).normalize();
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record ProcessRun(
            boolean started,
            boolean finished,
            boolean timedOut,
            int exitCode,
            List<String> stdout,
            List<String> stderr,
            List<String> threadDump
    ) {
        private static ProcessRun skipped() {
            return new ProcessRun(false, false, false, -1, List.of(), List.of(), List.of());
        }
    }

    private record NativeExtractionRun(
            boolean started,
            boolean ready,
            Path archive,
            Path nativesDirectory,
            List<String> extractedFiles
    ) {
        private static NativeExtractionRun skipped(Path nativesDirectory) {
            return new NativeExtractionRun(false, false, Path.of(""), nativesDirectory, List.of());
        }
    }

    private record LiveClientLaunchInputs(
            Path gameDirectory,
            Path quickPlayPath,
            Optional<Path> quickPlaySave
    ) {
    }
}
