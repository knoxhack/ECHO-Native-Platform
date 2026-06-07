package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

final class EchoNativeArtifactPackagingAuditor {
    EchoNativeArtifactPackagingAuditOutcome audit(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics);
        }

        Path workspace = Path.of("").toAbsolutePath().normalize();
        Path repoRoot = workspace.getParent() == null ? workspace : workspace.getParent();
        List<Map<String, Object>> blockers = blockers(reports.getOrDefault("phase13-m17-artifact-blockers.json", Map.of()));
        List<Map<String, Object>> findings = new ArrayList<>();
        for (Map<String, Object> blocker : blockers) {
            findings.add(packagingFinding(repoRoot, workspace, blocker));
        }
        findings.sort(Comparator.comparing(item -> String.valueOf(item.get("artifactId"))));

        List<Map<String, Object>> actions = resolutionActions(findings);
        boolean auditComplete = diagnostics.isEmpty();
        return new EchoNativeArtifactPackagingAuditOutcome(
                packId,
                artifactPackagingAudit(packId, auditComplete, findings, diagnostics),
                artifactPackagingResolutionPlan(packId, auditComplete, actions, diagnostics),
                diagnostics.stream()
                        .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                                .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                                .thenComparing(EchoNativeDiagnostic::summary))
                        .toList()
        );
    }

    private static Map<String, Object> packagingFinding(
            Path repoRoot,
            Path workspace,
            Map<String, Object> blocker
    ) throws IOException {
        String artifactId = String.valueOf(blocker.getOrDefault("artifactId", "unknown"));
        String rawId = artifactId.contains(":") ? artifactId.substring(artifactId.indexOf(':') + 1) : artifactId;
        String blockerType = String.valueOf(blocker.getOrDefault("blockerType", "unknown"));
        Map<String, Object> finding = new LinkedHashMap<>();
        finding.put("artifactId", artifactId);
        finding.put("blockerType", blockerType);
        finding.put("classloaderCreated", false);
        finding.put("commandExecuted", false);
        finding.put("downloadAllowed", false);
        finding.put("filesystemMutated", false);
        finding.put("nativeExtractionStarted", false);
        finding.put("plannedPath", String.valueOf(blocker.getOrDefault("plannedPath", "")));
        finding.put("processLaunched", false);
        finding.put("registryInjected", false);
        finding.put("registryMutated", false);
        finding.put("sourceReport", String.valueOf(blocker.getOrDefault("sourceReport", "")));

        if (artifactId.startsWith("native:")) {
            finding.put("approvedCandidateCount", 0);
            finding.put("approvedCandidates", List.of());
            finding.put("moduleDirectoryPresent", false);
            finding.put("packagingStatus", "native_fixture_required");
            finding.put("runtimeFixtureContract", runtimeFixtureContract(artifactId));
            finding.put("recommendedNextStep", "Keep launch blocked until a reviewed local native fixture is explicitly provided; do not extract natives in the native CLI.");
            return finding;
        }
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            finding.put("approvedCandidateCount", 0);
            finding.put("approvedCandidates", List.of());
            finding.put("moduleDirectoryPresent", false);
            finding.put("packagingStatus", "external_runtime_fixture_required");
            finding.put("runtimeFixtureContract", runtimeFixtureContract(artifactId));
            finding.put("recommendedNextStep", "Keep launch blocked until a reviewed local Minecraft client fixture is explicitly provided; do not download it in the native CLI.");
            return finding;
        }
        if ("classpath:echo-native-bootstrap-api".equals(artifactId)) {
            List<Map<String, Object>> candidates = findJarCandidates(repoRoot, rawId, List.of(
                    new SearchRoot(workspace.resolve("echo-native-contracts").resolve("build").resolve("libs").normalize(), 1, "native-contracts-build-libs"),
                    new SearchRoot(workspace.resolve("echo-native-loader").resolve("build").resolve("libs").normalize(), 1, "native-loader-build-libs"),
                    new SearchRoot(workspace.resolve("echo-native-cli").resolve("build").resolve("libs").normalize(), 1, "native-cli-build-libs")
            ));
            finding.put("approvedCandidateCount", candidates.size());
            finding.put("approvedCandidates", candidates);
            finding.put("moduleDirectoryPresent", false);
            finding.put("packagingStatus", candidates.isEmpty() ? "native_bootstrap_artifact_missing" : "native_bootstrap_candidate_review_required");
            finding.put("recommendedNextStep", candidates.isEmpty()
                    ? "Add a dedicated repo-local bootstrap API artifact or update the classpath plan to reference an existing native contract artifact after review."
                    : "Review the native candidate artifact, then add an explicit fixture runtime-artifacts.json mapping if it is the intended bootstrap API.");
            return finding;
        }

        Path moduleDir = repoRoot.resolve("addons").resolve(rawId).normalize();
        Path buildFile = moduleDir.resolve("build.gradle");
        Path gradleProperties = moduleDir.resolve("gradle.properties");
        Properties properties = readProperties(gradleProperties);
        String declaredModId = properties.getProperty("mod_id", rawId);
        String declaredModVersion = properties.getProperty("mod_version", "");
        String expectedJar = declaredModVersion.isBlank() ? declaredModId + ".jar" : declaredModId + "-" + declaredModVersion + ".jar";
        Path releaseJar = repoRoot.resolve("build").resolve("tmp").resolve("echo-1.1.3-mods").resolve(expectedJar).normalize();
        Path libsDir = moduleDir.resolve("build").resolve("libs").normalize();
        List<Map<String, Object>> candidates = findJarCandidates(repoRoot, rawId, List.of(
                new SearchRoot(libsDir, 1, "module-build-libs"),
                new SearchRoot(repoRoot.resolve("build").resolve("tmp").resolve("echo-1.1.3-mods").normalize(), 1, "root-build-mods")
        ));
        List<String> moduleLibJars = jarNames(libsDir);

        finding.put("approvedCandidateCount", candidates.size());
        finding.put("approvedCandidates", candidates);
        finding.put("declaredModId", declaredModId);
        finding.put("declaredModVersion", declaredModVersion);
        finding.put("expectedReleaseJarName", expectedJar);
        finding.put("expectedReleaseJarPath", relativePath(repoRoot, releaseJar));
        finding.put("gradleBuildFilePresent", Files.isRegularFile(buildFile));
        finding.put("gradlePropertiesPresent", Files.isRegularFile(gradleProperties));
        finding.put("moduleBuildLibJarCount", moduleLibJars.size());
        finding.put("moduleBuildLibJars", moduleLibJars);
        finding.put("moduleBuildLibsPresent", Files.isDirectory(libsDir));
        finding.put("moduleDirectory", relativePath(repoRoot, moduleDir));
        finding.put("moduleDirectoryPresent", Files.isDirectory(moduleDir));
        finding.put("rootReleaseJarPresent", Files.isRegularFile(releaseJar));
        finding.put("packagingStatus", packagingStatus(finding, candidates));
        finding.put("recommendedNextStep", recommendedNextStep(String.valueOf(finding.get("packagingStatus")), rawId));
        return finding;
    }

    private static String packagingStatus(Map<String, Object> finding, List<Map<String, Object>> candidates) {
        if (Boolean.TRUE.equals(finding.get("rootReleaseJarPresent"))) {
            return "root_release_jar_available_review_mapping";
        }
        if (!candidates.isEmpty()) {
            return "repo_local_candidate_review_required";
        }
        if (Boolean.TRUE.equals(finding.get("moduleDirectoryPresent"))
                && Boolean.TRUE.equals(finding.get("gradleBuildFilePresent"))
                && !Boolean.TRUE.equals(finding.get("moduleBuildLibsPresent"))) {
            return "module_jar_not_emitted";
        }
        if (Boolean.TRUE.equals(finding.get("moduleBuildLibsPresent"))
                && number(finding.get("moduleBuildLibJarCount")) == 0) {
            return "module_build_libs_empty";
        }
        if (!Boolean.TRUE.equals(finding.get("moduleDirectoryPresent"))) {
            return "module_directory_missing";
        }
        return "artifact_not_packaged";
    }

    private static String recommendedNextStep(String status, String moduleId) {
        return switch (status) {
            case "root_release_jar_available_review_mapping", "repo_local_candidate_review_required" ->
                    "Review the repo-local candidate jar for " + moduleId + ", then add an explicit fixture runtime-artifacts.json mapping if it is approved for isolated launch testing.";
            case "module_jar_not_emitted" ->
                    "Investigate why the " + moduleId + " Gradle project does not emit build/libs output during the beta packaging path before mapping launch artifacts.";
            case "module_build_libs_empty" ->
                    "Inspect " + moduleId + " build outputs and Gradle task wiring; build/libs exists but contains no jar candidates.";
            case "module_directory_missing" ->
                    "Confirm whether " + moduleId + " is still a real addon module or update the classpath plan to remove the stale artifact requirement.";
            default ->
                    "Resolve " + moduleId + " through normal repo-local packaging outputs before adding any launch mapping.";
        };
    }

    private static Map<String, Object> runtimeFixtureContract(String artifactId) {
        if ("classpath:minecraft-client-coordinate".equals(artifactId)) {
            return fixtureContract(
                    artifactId,
                    "minecraft_client_jar",
                    "local-runtime/minecraft/26.1.2/client/minecraft-client-26.1.2.jar",
                    "minecraft-runtime-fixture",
                    false,
                    "Copy an already-authorized local Minecraft client jar into the fixture path outside the native CLI, then add the approved mapping. The native CLI must not download it."
            );
        }
        if ("native:minecraft-26.1.2-natives".equals(artifactId)) {
            return fixtureContract(
                    artifactId,
                    "minecraft_native_archive",
                    "local-runtime/minecraft/26.1.2/natives/minecraft-26.1.2-natives.zip",
                    "minecraft-native-fixture",
                    false,
                    "Copy an already-authorized local native archive into the fixture path outside the native CLI, then add the approved mapping. The native CLI must not extract it."
            );
        }
        return Map.of();
    }

    private static Map<String, Object> fixtureContract(
            String artifactId,
            String artifactKind,
            String expectedLocalPath,
            String mappingSource,
            boolean extractionAllowed,
            String operatorAction
    ) {
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("artifactKind", artifactKind);
        contract.put("expectedFixturePath", expectedLocalPath);
        contract.put("operatorAction", operatorAction);
        contract.put("requiredRuntimeArtifactsJsonEntry", runtimeArtifactEntry(artifactId, expectedLocalPath, mappingSource, extractionAllowed));
        contract.put("reviewRequirements", List.of(
                "reviewed=true",
                "approved=true",
                "reviewStatus=approved",
                "downloadsAllowed=false",
                "extractionAllowed=" + extractionAllowed,
                "localPath is fixture-relative",
                "localPath does not point to a user home, launcher cache, .minecraft directory, or Gradle cache"
        ));
        contract.put("safeToAutoPopulate", false);
        return contract;
    }

    private static Map<String, Object> runtimeArtifactEntry(
            String artifactId,
            String localPath,
            String source,
            boolean extractionAllowed
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", artifactId);
        entry.put("localPath", localPath);
        entry.put("source", source);
        entry.put("reviewed", true);
        entry.put("approved", true);
        entry.put("reviewStatus", "approved");
        entry.put("downloadsAllowed", false);
        entry.put("extractionAllowed", extractionAllowed);
        return entry;
    }

    private static List<Map<String, Object>> blockers(Map<String, Object> report) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object raw = data.get("blockers");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> blockers = new ArrayList<>();
        for (Object item : list) {
            blockers.add(EchoNativeJson.asObject(item));
        }
        blockers.sort(Comparator.comparing(blocker -> String.valueOf(blocker.get("artifactId"))));
        return List.copyOf(blockers);
    }

    private static List<Map<String, Object>> resolutionActions(List<Map<String, Object>> findings) {
        List<Map<String, Object>> actions = new ArrayList<>();
        int order = 0;
        for (Map<String, Object> finding : findings) {
            String artifactId = String.valueOf(finding.get("artifactId"));
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("actionId", "phase13.m17.audit.resolve." + safeId(artifactId));
            action.put("actionOrder", order++);
            action.put("artifactId", artifactId);
            action.put("downloadAllowed", false);
            action.put("extractionAllowed", false);
            action.put("filesystemMutationAllowed", false);
            action.put("packagingStatus", finding.get("packagingStatus"));
            action.put("recommendedNextStep", finding.get("recommendedNextStep"));
            action.put("requiresHumanReview", true);
            actions.add(action);
        }
        return List.copyOf(actions);
    }

    private static Map<String, Object> artifactPackagingAudit(
            String packId,
            boolean auditComplete,
            List<Map<String, Object>> findings,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_artifact_packaging_audit", diagnostics);
        data.put("artifactBlockerCount", findings.size());
        data.put("auditComplete", auditComplete);
        data.put("findings", findings);
        data.put("packId", packId);
        data.put("packagingAuditReady", auditComplete);
        data.put("phase13M17LaunchBlocked", true);
        data.put("summary", auditComplete
                ? "M17 artifact blockers were audited against repo-local packaging outputs."
                : "M17 artifact packaging audit is blocked by upstream artifact blocker diagnostics.");
        return data;
    }

    private static Map<String, Object> artifactPackagingResolutionPlan(
            String packId,
            boolean auditComplete,
            List<Map<String, Object>> actions,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_artifact_packaging_resolution_plan", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("auditComplete", auditComplete);
        data.put("downloadsAllowed", false);
        data.put("extractionAllowed", false);
        data.put("packId", packId);
        data.put("summary", actions.isEmpty()
                ? "No artifact packaging actions are needed."
                : "Resolve M17 blockers through reviewed repo-local packaging fixes or explicit future runtime fixtures.");
        return data;
    }

    private static void checkReport(
            String reportName,
            Map<String, Object> report,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if (("phase13-m17-artifact-blockers.json".equals(reportName)
                || "phase13-m17-blocker-resolution-plan.json".equals(reportName))
                && !"PASS".equals(report.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-PACKAGING-AUDIT-UPSTREAM-FAILED",
                    EchoNativeIssueSeverity.ERROR,
                    "M17 artifact packaging audit upstream blocker report is not PASS",
                    "Artifact packaging audit requires PASS " + reportName + " before classifying remaining blockers.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve Phase 13 M17 artifact blocker documentation before auditing packaging evidence."
            ));
            diagnostics.addAll(reportDiagnostics(report, packId));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-PACKAGING-AUDIT-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "M17 artifact packaging audit input contains unsafe runtime work",
                    reportName + " indicates work that is not allowed while auditing packaging blockers.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep the packaging audit report-only: no launch, command execution, downloads, extraction, classloader, transforms, registry injection, or mutation."
            ));
        }
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
                    "ECHO-NATIVE-M17-PACKAGING-AUDIT-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "M17 artifact packaging audit required report missing",
                    "Artifact packaging audit requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M17 artifact reports before auditing packaging blockers."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<Map<String, Object>> findJarCandidates(
            Path repoRoot,
            String rawId,
            List<SearchRoot> roots
    ) throws IOException {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (SearchRoot root : roots) {
            if (!Files.isDirectory(root.path())) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root.path(), root.depth())) {
                List<Path> matches = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .filter(path -> matchesArtifact(rawId, path.getFileName().toString()))
                        .sorted(Comparator.comparing(path -> relativePath(repoRoot, path)))
                        .toList();
                for (Path match : matches) {
                    Map<String, Object> candidate = new LinkedHashMap<>();
                    candidate.put("approvedSourceRoot", root.kind());
                    candidate.put("path", relativePath(repoRoot, match));
                    candidate.put("trustedForAutoLaunch", false);
                    candidates.add(candidate);
                }
            }
        }
        candidates.sort(Comparator.comparing(item -> String.valueOf(item.get("path"))));
        return List.copyOf(candidates);
    }

    private static List<String> jarNames(Path libsDir) throws IOException {
        if (!Files.isDirectory(libsDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(libsDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".jar"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean matchesArtifact(String rawId, String fileName) {
        String normalized = rawId.replace("-api", "");
        return fileName.equals(rawId + ".jar")
                || fileName.startsWith(rawId + "-")
                || fileName.equals(normalized + ".jar")
                || fileName.startsWith(normalized + "-");
    }

    private static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) {
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        return properties;
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
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
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("minecraftResourceManagerTouched"))
                || Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
                || Boolean.TRUE.equals(data.get("socketOpened"))
                || Boolean.TRUE.equals(data.get("clientConnectionOpened"))
                || Boolean.TRUE.equals(data.get("serverConnectionOpened"))
                || Boolean.TRUE.equals(data.get("packetSent"))
                || Boolean.TRUE.equals(data.get("packetReceived"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("minecraftBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("addonBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(EchoNativeJson::asObject)
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

    private static String safeId(String value) {
        return value.replace(':', '.').replace('/', '.').replace('\\', '.').replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private static String relativePath(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        if (normalizedPath.startsWith(normalizedRoot)) {
            return normalizedRoot.relativize(normalizedPath).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record SearchRoot(Path path, int depth, String kind) {
    }
}
