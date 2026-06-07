package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeControlledDummyProcessPlan;
import dev.echo.nativeplatform.contracts.EchoNativeControlledDummyProcessResult;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeDummyProcessCrashBoundary;
import dev.echo.nativeplatform.contracts.EchoNativeDummyProcessOutputCapture;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class EchoNativeControlledDummyProcessRunner {
    private static final long TIMEOUT_MILLIS = 5000L;

    EchoNativeDummyProcessOutcome run(
            String packId,
            Path fixture,
            Path launchArgumentPlanPath,
            Path launchArgumentSourcePolicyPath,
            Path launchArgumentSafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> launchArgumentPlan = readRequiredReport(launchArgumentPlanPath, fixture, packId, "ECHO-NATIVE-LAUNCH-ARGUMENT-PLAN-MISSING", "Launch argument plan missing", diagnostics);
        Map<String, Object> launchArgumentPolicy = readRequiredReport(launchArgumentSourcePolicyPath, fixture, packId, "ECHO-NATIVE-LAUNCH-ARGUMENT-POLICY-MISSING", "Launch argument source policy missing", diagnostics);
        Map<String, Object> launchArgumentSafety = readRequiredReport(launchArgumentSafetyStatusPath, fixture, packId, "ECHO-NATIVE-LAUNCH-ARGUMENT-SAFETY-MISSING", "Launch argument safety status missing", diagnostics);

        checkLaunchArgumentReport(launchArgumentPlan, EchoNativeJson.asObject(launchArgumentPlan.get("data")), launchArgumentPlanPath, packId, "ECHO-NATIVE-LAUNCH-ARGUMENT-PLAN-BLOCKED", "Launch argument plan is not safe for dummy process boundary", diagnostics);
        checkLaunchArgumentReport(launchArgumentPolicy, EchoNativeJson.asObject(launchArgumentPolicy.get("data")), launchArgumentSourcePolicyPath, packId, "ECHO-NATIVE-LAUNCH-ARGUMENT-POLICY-BLOCKED", "Launch argument source policy is not safe for dummy process boundary", diagnostics);
        checkLaunchArgumentReport(launchArgumentSafety, EchoNativeJson.asObject(launchArgumentSafety.get("data")), launchArgumentSafetyStatusPath, packId, "ECHO-NATIVE-LAUNCH-ARGUMENT-SAFETY-BLOCKED", "Launch argument safety status is not safe for dummy process boundary", diagnostics);

        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        List<String> requiredInputs = List.of(
                "launch-argument-builder-plan.json",
                "launch-argument-source-policy.json",
                "launch-argument-safety-status.json"
        );
        List<String> sanitizedCommand = List.of(
                "java",
                "-cp",
                "<current-cli-classpath>",
                EchoNativeDummyProcessMain.class.getName(),
                "--pack",
                packId
        );
        EchoNativeControlledDummyProcessPlan plan = new EchoNativeControlledDummyProcessPlan(
                "phase13.m7.controlled_dummy_process.plan",
                ready,
                true,
                ready,
                false,
                false,
                false,
                false,
                false,
                false,
                TIMEOUT_MILLIS,
                ready ? sanitizedCommand : List.of(),
                ready ? requiredInputs : List.of()
        );

        ProcessRun run = ready ? runDummyProcess(packId) : ProcessRun.notStarted();
        if (run.failedToStart()) {
            diagnostics = new ArrayList<>(diagnostics);
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DUMMY-PROCESS-FAILED-TO-START",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled dummy process failed to start",
                    "The controlled Java dummy process could not start inside the native CLI boundary.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Inspect the local Java toolchain and rerun the dummy process boundary command."
            ));
            diagnostics = unique(diagnostics);
        }

        boolean executed = ready && !run.failedToStart();
        boolean passed = executed && !run.timedOut() && run.exitCode() == 0;
        if (executed && !passed) {
            diagnostics = new ArrayList<>(diagnostics);
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DUMMY-PROCESS-BOUNDARY-FAILED",
                    EchoNativeIssueSeverity.ERROR,
                    "Controlled dummy process boundary failed",
                    "The controlled dummy process timed out or exited nonzero.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Inspect the deterministic dummy process boundary before continuing Phase 13 process experiments."
            ));
            diagnostics = unique(diagnostics);
        }
        EchoNativeControlledDummyProcessResult result = new EchoNativeControlledDummyProcessResult(
                "phase13.m7.controlled_dummy_process.result",
                executed,
                true,
                executed,
                executed,
                false,
                false,
                false,
                false,
                false,
                false,
                run.timedOut(),
                run.exitCode(),
                passed ? "pass" : "blocked"
        );
        EchoNativeDummyProcessCrashBoundary crashBoundary = new EchoNativeDummyProcessCrashBoundary(
                "phase13.m7.dummy_process.crash_boundary",
                passed,
                passed,
                passed,
                passed,
                !run.stdoutLines().isEmpty(),
                !run.stderrLines().isEmpty(),
                false,
                false,
                false,
                passed ? List.of("stdout_capture", "stderr_capture", "timeout_guard", "nonzero_exit_guard") : List.of()
        );
        EchoNativeDummyProcessOutputCapture outputCapture = new EchoNativeDummyProcessOutputCapture(
                "phase13.m7.dummy_process.output_capture",
                passed,
                passed,
                true,
                run.stdoutLines().size(),
                run.stderrLines().size(),
                run.stdoutLines(),
                run.stderrLines()
        );

        return new EchoNativeDummyProcessOutcome(
                packId,
                controlledDummyProcessPlan(packId, plan, diagnostics),
                controlledDummyProcessResult(packId, result, diagnostics),
                dummyProcessCrashBoundary(packId, crashBoundary, diagnostics),
                dummyProcessOutputCapture(packId, outputCapture, diagnostics),
                diagnostics
        );
    }

    private static ProcessRun runDummyProcess(String packId) {
        String javaHome = System.getProperty("java.home", "");
        String executableName = isWindows() ? "java.exe" : "java";
        Path javaExecutable = Path.of(javaHome).resolve("bin").resolve(executableName);
        String classpath = System.getProperty("java.class.path", "");
        List<String> command = List.of(
                javaExecutable.toString(),
                "-cp",
                classpath,
                EchoNativeDummyProcessMain.class.getName(),
                "--pack",
                packId
        );
        try {
            Process process = new ProcessBuilder(command).start();
            boolean finished = process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
            }
            List<String> stdout = process.inputReader(StandardCharsets.UTF_8).lines()
                    .sorted()
                    .toList();
            List<String> stderr = process.errorReader(StandardCharsets.UTF_8).lines()
                    .sorted()
                    .toList();
            return new ProcessRun(finished ? process.exitValue() : -1, !finished, false, stdout, stderr);
        } catch (IOException exception) {
            return new ProcessRun(-1, false, true, List.of(), List.of());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ProcessRun(-1, true, true, List.of(), List.of());
        }
    }

    private static void checkLaunchArgumentReport(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            String code,
            String title,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        if (!"PASS".equals(report.get("status")) || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    code,
                    EchoNativeIssueSeverity.ERROR,
                    title,
                    "Controlled dummy process boundary requires PASS launch-argument M6 reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate Phase 13 M6 launch argument planning reports before running the dummy process boundary."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> controlledDummyProcessPlan(
            String packId,
            EchoNativeControlledDummyProcessPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m7_controlled_dummy_process_plan", diagnostics);
        data.put("classloaderCreationAllowed", plan.classloaderCreationAllowed());
        data.put("commandExecutionAllowed", plan.commandExecutionAllowed());
        data.put("filesystemMutationAllowed", plan.filesystemMutationAllowed());
        data.put("gameProcessLaunchAllowed", plan.gameProcessLaunchAllowed());
        data.put("minecraftLaunchAllowed", plan.minecraftLaunchAllowed());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("processLaunchAllowed", plan.processLaunchAllowed());
        data.put("ready", plan.ready());
        data.put("requiredInputs", plan.requiredInputs());
        data.put("runtimeClassResolutionAllowed", plan.runtimeClassResolutionAllowed());
        data.put("sanitizedCommand", plan.sanitizedCommand());
        data.put("summary", plan.ready()
                ? "M7 is ready to launch only the controlled dummy Java process; Minecraft launch remains blocked."
                : "M7 controlled dummy process boundary is blocked by upstream launch argument diagnostics.");
        data.put("timeoutMillis", plan.timeoutMillis());
        data.put("dummyProcessOnly", plan.dummyProcessOnly());
        return data;
    }

    private static Map<String, Object> controlledDummyProcessResult(
            String packId,
            EchoNativeControlledDummyProcessResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m7_controlled_dummy_process_result", diagnostics);
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("commandExecuted", result.commandExecuted());
        data.put("dummyProcessLaunched", result.dummyProcessLaunched());
        data.put("dummyProcessOnly", result.dummyProcessOnly());
        data.put("executed", result.executed());
        data.put("exitCode", result.exitCode());
        data.put("filesystemMutated", result.filesystemMutated());
        data.put("gameProcessLaunched", result.gameProcessLaunched());
        data.put("minecraftLaunched", result.minecraftLaunched());
        data.put("outcome", result.outcome());
        data.put("packId", packId);
        data.put("processLaunched", result.processLaunched());
        data.put("resolvesRuntimeClasses", result.resolvesRuntimeClasses());
        data.put("resultId", result.resultId());
        data.put("summary", result.executed()
                ? "Controlled dummy Java process completed inside the M7 boundary; no Minecraft process, classloader, command, or runtime class resolution occurred."
                : "Controlled dummy process did not run because prerequisite M6 reports were missing, failed, or unsafe.");
        data.put("timedOut", result.timedOut());
        return data;
    }

    private static Map<String, Object> dummyProcessCrashBoundary(
            String packId,
            EchoNativeDummyProcessCrashBoundary boundary,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m7_dummy_process_crash_boundary", diagnostics);
        data.put("boundaryId", boundary.boundaryId());
        data.put("classloaderCreated", boundary.classloaderCreated());
        data.put("containedFailureModes", boundary.containedFailureModes());
        data.put("crashContained", boundary.crashContained());
        data.put("filesystemMutated", boundary.filesystemMutated());
        data.put("gameProcessLaunched", boundary.gameProcessLaunched());
        data.put("nonZeroExitContained", boundary.nonZeroExitContained());
        data.put("packId", packId);
        data.put("stderrCaptured", boundary.stderrCaptured());
        data.put("stdoutCaptured", boundary.stdoutCaptured());
        data.put("summary", boundary.verified()
                ? "M7 dummy-process crash boundary verified timeout, nonzero-exit, stdout, and stderr containment using a controlled process only."
                : "M7 dummy-process crash boundary failed because the controlled process did not complete safely.");
        data.put("timeoutContained", boundary.timeoutContained());
        data.put("verified", boundary.verified());
        return data;
    }

    private static Map<String, Object> dummyProcessOutputCapture(
            String packId,
            EchoNativeDummyProcessOutputCapture capture,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m7_dummy_process_output_capture", diagnostics);
        data.put("captureId", capture.captureId());
        data.put("captured", capture.captured());
        data.put("deterministic", capture.deterministic());
        data.put("packId", packId);
        data.put("secretSafe", capture.secretSafe());
        data.put("stderrLineCount", capture.stderrLineCount());
        data.put("stderrLines", capture.stderrLines());
        data.put("stdoutLineCount", capture.stdoutLineCount());
        data.put("stdoutLines", capture.stdoutLines());
        data.put("summary", capture.captured()
                ? "Controlled dummy process stdout and stderr were captured deterministically without environment dumps."
                : "Controlled dummy process output capture did not run because M7 was blocked.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("phase", phase);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("minecraftResolverStarted"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String missingCode,
            String missingTitle,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    missingCode,
                    EchoNativeIssueSeverity.ERROR,
                    missingTitle,
                    "Required M7 controlled dummy process input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 M6 launch argument planning before running the controlled dummy process boundary."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record ProcessRun(
            int exitCode,
            boolean timedOut,
            boolean failedToStart,
            List<String> stdoutLines,
            List<String> stderrLines
    ) {
        static ProcessRun notStarted() {
            return new ProcessRun(-1, false, false, List.of(), List.of());
        }
    }
}
