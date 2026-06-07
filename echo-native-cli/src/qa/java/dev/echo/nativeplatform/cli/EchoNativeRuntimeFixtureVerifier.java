package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeRuntimeFixtureVerifier {
    EchoNativeRuntimeFixtureVerificationOutcome verify(
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

        Map<String, Map<String, Object>> runtimeArtifactMappings = runtimeArtifactMappings(fixture.resolve("runtime-artifacts.json"), diagnostics, packId);
        Map<String, RuntimeFixtureContract> contracts = contracts(reports);
        boolean upstreamReady = diagnostics.stream().noneMatch(diagnostic -> diagnostic.code().startsWith("ECHO-NATIVE-RUNTIME-FIXTURE-UPSTREAM"));
        if (upstreamReady && contracts.isEmpty()) {
            collectReviewedRuntimeArtifactContracts(runtimeArtifactMappings, contracts);
        }
        List<Map<String, Object>> fixtureChecks = new ArrayList<>();
        List<Map<String, Object>> mappingChecks = new ArrayList<>();
        for (RuntimeFixtureContract contract : contracts.values()) {
            fixtureChecks.add(fixtureCheck(fixture, packId, contract, diagnostics));
            mappingChecks.add(mappingCheck(fixture, packId, contract, runtimeArtifactMappings, diagnostics));
        }

        int missingFixtureCount = (int) fixtureChecks.stream()
                .filter(check -> !Boolean.TRUE.equals(check.get("fixturePresent")))
                .count();
        int readyMappingCount = (int) mappingChecks.stream()
                .filter(check -> Boolean.TRUE.equals(check.get("mappingReady")))
                .count();
        boolean fixturePresenceReady = upstreamReady && !contracts.isEmpty() && missingFixtureCount == 0;
        boolean mappingReady = fixturePresenceReady && readyMappingCount == contracts.size();

        return new EchoNativeRuntimeFixtureVerificationOutcome(
                packId,
                runtimeFixturePresence(packId, fixturePresenceReady, fixtureChecks, diagnostics),
                runtimeFixtureMappingReadiness(packId, mappingReady, fixturePresenceReady, mappingChecks, diagnostics),
                diagnostics.stream()
                        .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                                .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                                .thenComparing(EchoNativeDiagnostic::summary))
                        .toList()
        );
    }

    private static Map<String, RuntimeFixtureContract> contracts(Map<String, Map<String, Object>> reports) {
        Map<String, RuntimeFixtureContract> contracts = new LinkedHashMap<>();
        collectBlockerContracts(reports.getOrDefault("phase13-m17-artifact-blockers.json", Map.of()), contracts);
        collectPackagingContracts(reports.getOrDefault("phase13-m17-artifact-packaging-audit.json", Map.of()), contracts);
        return contracts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    private static void collectBlockerContracts(Map<String, Object> report, Map<String, RuntimeFixtureContract> contracts) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object raw = data.get("blockers");
        if (!(raw instanceof List<?> blockers)) {
            return;
        }
        for (Object item : blockers) {
            Map<String, Object> blocker = EchoNativeJson.asObject(item);
            addContract(String.valueOf(blocker.getOrDefault("artifactId", "")), blocker.get("runtimeFixtureContract"), contracts);
        }
    }

    private static void collectPackagingContracts(Map<String, Object> report, Map<String, RuntimeFixtureContract> contracts) {
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        Object raw = data.get("findings");
        if (!(raw instanceof List<?> findings)) {
            return;
        }
        for (Object item : findings) {
            Map<String, Object> finding = EchoNativeJson.asObject(item);
            addContract(String.valueOf(finding.getOrDefault("artifactId", "")), finding.get("runtimeFixtureContract"), contracts);
        }
    }

    private static void addContract(String artifactId, Object rawContract, Map<String, RuntimeFixtureContract> contracts) {
        Map<String, Object> contract = EchoNativeJson.asObject(rawContract);
        if (artifactId.isBlank() || contract.isEmpty()) {
            return;
        }
        String expectedFixturePath = String.valueOf(contract.getOrDefault("expectedFixturePath", ""));
        if (expectedFixturePath.isBlank()) {
            return;
        }
        contracts.putIfAbsent(artifactId, new RuntimeFixtureContract(
                artifactId,
                String.valueOf(contract.getOrDefault("artifactKind", "runtime_fixture")),
                expectedFixturePath,
                Boolean.TRUE.equals(contract.get("safeToAutoPopulate")),
                EchoNativeJson.asObject(contract.get("requiredRuntimeArtifactsJsonEntry")),
                String.valueOf(contract.getOrDefault("operatorAction", "Provide a reviewed local runtime fixture outside the native CLI."))
        ));
    }

    private static void collectReviewedRuntimeArtifactContracts(
            Map<String, Map<String, Object>> runtimeArtifactMappings,
            Map<String, RuntimeFixtureContract> contracts
    ) {
        runtimeArtifactMappings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> addReviewedRuntimeArtifactContract(entry.getKey(), entry.getValue(), contracts));
    }

    private static void addReviewedRuntimeArtifactContract(
            String artifactId,
            Map<String, Object> mapping,
            Map<String, RuntimeFixtureContract> contracts
    ) {
        String localPath = String.valueOf(mapping.getOrDefault("localPath", ""));
        String source = String.valueOf(mapping.getOrDefault("source", ""));
        if (artifactId.isBlank() || localPath.isBlank()) {
            return;
        }
        if (!isRuntimeFixtureMapping(artifactId, source)) {
            return;
        }
        Path path = Path.of(localPath);
        if (path.isAbsolute() || localPath.contains("..")) {
            return;
        }
        Map<String, Object> requiredMapping = new LinkedHashMap<>();
        requiredMapping.put("id", artifactId);
        requiredMapping.put("localPath", localPath);
        requiredMapping.put("source", source);
        requiredMapping.put("reviewed", true);
        requiredMapping.put("approved", true);
        requiredMapping.put("reviewStatus", "approved");
        requiredMapping.put("downloadsAllowed", false);
        requiredMapping.put("extractionAllowed", false);

        contracts.putIfAbsent(artifactId, new RuntimeFixtureContract(
                artifactId,
                artifactKind(artifactId, source),
                localPath,
                false,
                requiredMapping,
                "Use the reviewed fixture-local runtime artifact mapping already present in runtime-artifacts.json."
        ));
    }

    private static boolean isRuntimeFixtureMapping(String artifactId, String source) {
        return "classpath:minecraft-client-coordinate".equals(artifactId)
                || "native:minecraft-26.1.2-natives".equals(artifactId)
                || "minecraft-runtime-fixture".equals(source)
                || "minecraft-native-fixture".equals(source);
    }

    private static String artifactKind(String artifactId, String source) {
        if (artifactId.startsWith("native:") || "minecraft-native-fixture".equals(source)) {
            return "minecraft_native_archive";
        }
        return "minecraft_client_jar";
    }

    private static Map<String, Map<String, Object>> runtimeArtifactMappings(
            Path runtimeArtifactsPath,
            List<EchoNativeDiagnostic> diagnostics,
            String packId
    ) throws IOException {
        if (!Files.isRegularFile(runtimeArtifactsPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-MAPPING-FILE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture mapping file is missing",
                    "Runtime fixture verification requires fixture-local runtime-artifacts.json.",
                    null,
                    packId,
                    List.of(relativeReportPath(runtimeArtifactsPath)),
                    "Create reviewed fixture-local runtime-artifacts.json mappings outside this verifier; the verifier must not auto-populate them."
            ));
            return Map.of();
        }
        Map<String, Object> root = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(runtimeArtifactsPath)));
        Object rawArtifacts = root.get("artifacts");
        if (!(rawArtifacts instanceof List<?> artifacts)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-MAPPING-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture mapping file has no artifacts array",
                    "runtime-artifacts.json must include an artifacts array for fixture mapping readiness checks.",
                    null,
                    packId,
                    List.of(relativeReportPath(runtimeArtifactsPath)),
                    "Keep runtime-artifacts.json deterministic and add reviewed artifact mappings."
            ));
            return Map.of();
        }
        Map<String, Map<String, Object>> mappings = new LinkedHashMap<>();
        for (Object raw : artifacts) {
            Map<String, Object> artifact = EchoNativeJson.asObject(raw);
            String id = String.valueOf(artifact.getOrDefault("id", ""));
            if (!id.isBlank()) {
                mappings.put(id, artifact);
            }
        }
        return mappings;
    }

    private static Map<String, Object> fixtureCheck(
            Path fixture,
            String packId,
            RuntimeFixtureContract contract,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Path relativePath = Path.of(contract.expectedFixturePath());
        boolean relative = !relativePath.isAbsolute() && !contract.expectedFixturePath().contains("..");
        Path expectedPath = fixture.resolve(relativePath).normalize();
        boolean present = relative && Files.isRegularFile(expectedPath);
        long byteSize = present ? Files.size(expectedPath) : 0L;
        if (!relative || !present) {
            diagnostics.add(new EchoNativeDiagnostic(
                    relative
                            ? "ECHO-NATIVE-RUNTIME-FIXTURE-MISSING"
                            : "ECHO-NATIVE-RUNTIME-FIXTURE-PATH-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    relative ? "Runtime fixture file is missing" : "Runtime fixture path is not repo-relative",
                    contract.artifactId() + " requires " + contract.expectedFixturePath() + " before M17 can advance.",
                    null,
                    packId,
                    List.of(fixture.resolve(contract.expectedFixturePath()).toString().replace('\\', '/')),
                    contract.operatorAction()
            ));
        }
        Map<String, Object> check = baseCheck(contract);
        check.put("byteSize", byteSize);
        check.put("expectedFixturePath", contract.expectedFixturePath());
        check.put("fixturePresent", present);
        check.put("pathRelative", relative);
        check.put("safeToAutoPopulate", contract.safeToAutoPopulate());
        check.put("verifiedLocalPath", relativePath.toString().replace('\\', '/'));
        return check;
    }

    private static Map<String, Object> mappingCheck(
            Path fixture,
            String packId,
            RuntimeFixtureContract contract,
            Map<String, Map<String, Object>> runtimeArtifactMappings,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> actualMapping = runtimeArtifactMappings.getOrDefault(contract.artifactId(), Map.of());
        boolean mappingPresent = !actualMapping.isEmpty();
        boolean localPathMatches = contract.expectedFixturePath().equals(String.valueOf(actualMapping.getOrDefault("localPath", "")));
        boolean reviewed = Boolean.TRUE.equals(actualMapping.get("reviewed"));
        boolean approved = Boolean.TRUE.equals(actualMapping.get("approved"));
        boolean reviewStatusApproved = "approved".equals(actualMapping.get("reviewStatus"));
        boolean downloadsBlocked = Boolean.FALSE.equals(actualMapping.get("downloadsAllowed"));
        boolean extractionBlocked = Boolean.FALSE.equals(actualMapping.get("extractionAllowed"));
        boolean mappingReady = mappingPresent
                && localPathMatches
                && reviewed
                && approved
                && reviewStatusApproved
                && downloadsBlocked
                && extractionBlocked;
        if (!mappingReady) {
            diagnostics.add(new EchoNativeDiagnostic(
                    mappingPresent
                            ? "ECHO-NATIVE-RUNTIME-FIXTURE-MAPPING-NOT-READY"
                            : "ECHO-NATIVE-RUNTIME-FIXTURE-MAPPING-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    mappingPresent ? "Runtime fixture mapping is not ready" : "Runtime fixture mapping is missing",
                    contract.artifactId() + " must have a reviewed, approved, non-downloading, non-extracting runtime-artifacts.json entry.",
                    null,
                    packId,
                    List.of(fixture.resolve("runtime-artifacts.json").toString().replace('\\', '/')),
                    "Add the required mapping exactly as documented in the runtime fixture contract; this verifier must not write it."
            ));
        }
        Map<String, Object> check = baseCheck(contract);
        check.put("actualMapping", mappingPresent ? actualMapping : Map.of());
        check.put("downloadsAllowed", false);
        check.put("extractionAllowed", false);
        check.put("localPathMatchesContract", localPathMatches);
        check.put("mappingPresent", mappingPresent);
        check.put("mappingReady", mappingReady);
        check.put("requiredRuntimeArtifactsJsonEntry", contract.requiredMapping());
        check.put("reviewedAndApproved", reviewed && approved && reviewStatusApproved);
        check.put("safeToAutoPopulate", contract.safeToAutoPopulate());
        return check;
    }

    private static Map<String, Object> baseCheck(RuntimeFixtureContract contract) {
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("artifactId", contract.artifactId());
        check.put("artifactKind", contract.artifactKind());
        check.put("classloaderCreated", false);
        check.put("commandExecuted", false);
        check.put("downloadsAllowed", false);
        check.put("filesystemMutated", false);
        check.put("gameClassesResolved", false);
        check.put("nativeExtractionStarted", false);
        check.put("processLaunched", false);
        check.put("registryInjected", false);
        check.put("registryMutated", false);
        return check;
    }

    private static Map<String, Object> runtimeFixturePresence(
            String packId,
            boolean ready,
            List<Map<String, Object>> fixtureChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        int missingCount = (int) fixtureChecks.stream()
                .filter(check -> !Boolean.TRUE.equals(check.get("fixturePresent")))
                .count();
        Map<String, Object> data = base("phase13_m17_runtime_fixture_presence", diagnostics);
        data.put("fixtureCheckCount", fixtureChecks.size());
        data.put("fixtureChecks", fixtureChecks);
        data.put("missingFixtureCount", missingCount);
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", !ready);
        data.put("runtimeFixturesPresent", ready);
        data.put("safeToAutoPopulate", false);
        data.put("summary", ready
                ? "All reviewed runtime fixture contract files are present."
                : "M17 remains blocked until reviewed runtime fixture files are present.");
        return data;
    }

    private static Map<String, Object> runtimeFixtureMappingReadiness(
            String packId,
            boolean ready,
            boolean fixturePresenceReady,
            List<Map<String, Object>> mappingChecks,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        int readyCount = (int) mappingChecks.stream()
                .filter(check -> Boolean.TRUE.equals(check.get("mappingReady")))
                .count();
        Map<String, Object> data = base("phase13_m17_runtime_fixture_mapping_readiness", diagnostics);
        data.put("mappingCheckCount", mappingChecks.size());
        data.put("mappingChecks", mappingChecks);
        data.put("readyMappingCount", readyCount);
        data.put("missingOrUnreadyMappingCount", mappingChecks.size() - readyCount);
        data.put("packId", packId);
        data.put("phase13M17LaunchBlocked", !ready);
        data.put("runtimeFixtureMappingsReady", ready);
        data.put("runtimeFixturesPresent", fixturePresenceReady);
        data.put("safeToAutoPopulate", false);
        data.put("summary", ready
                ? "All runtime fixture mappings are reviewed, approved, and ready."
                : "M17 remains blocked until runtime fixture files and reviewed mappings are ready.");
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
                || "phase13-m17-artifact-packaging-audit.json".equals(reportName))
                && !"PASS".equals(report.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-UPSTREAM-FAILED",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture verification upstream report is not PASS",
                    "Runtime fixture verification requires PASS " + reportName + " before checking fixture files.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve upstream M17 artifact blocker reports before verifying runtime fixtures."
            ));
            diagnostics.addAll(reportDiagnostics(report, packId));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RUNTIME-FIXTURE-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture verification input contains unsafe runtime work",
                    reportName + " indicates work that is not allowed while checking runtime fixture contracts.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep runtime fixture verification report-only: no launch, command execution, downloads, extraction, classloader, transforms, registry injection, or mutation."
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
                    "ECHO-NATIVE-RUNTIME-FIXTURE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Runtime fixture verification required report missing",
                    "Runtime fixture verification requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M17 artifact blocker and packaging reports before verifying runtime fixture presence."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
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
        data.put("downloadsAllowed", false);
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
        data.put("safeToAutoPopulate", false);
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
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
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

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record RuntimeFixtureContract(
            String artifactId,
            String artifactKind,
            String expectedFixturePath,
            boolean safeToAutoPopulate,
            Map<String, Object> requiredMapping,
            String operatorAction
    ) {
    }
}
