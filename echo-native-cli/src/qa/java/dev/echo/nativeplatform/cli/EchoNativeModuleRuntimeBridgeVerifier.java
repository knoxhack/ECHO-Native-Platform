package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;

final class EchoNativeModuleRuntimeBridgeVerifier {
    EchoNativeModuleRuntimeBridgeOutcome verify(
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
            checkAcceptedReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, String> artifactPaths = artifactPaths(reports.get("local-runtime-artifact-map.json"));
        Path workspace = Path.of("").toAbsolutePath().normalize();
        List<ModuleBridgeEvidence> moduleEvidence = new ArrayList<>();
        for (EchoNativeAddonDescriptor descriptor : descriptors.stream().sorted(Comparator.comparing(EchoNativeAddonDescriptor::id)).toList()) {
            String entrypoint = descriptorEntrypoint(descriptor.descriptorPath());
            String nativeEntrypoint = nativeEntrypoint(descriptor);
            String localPath = artifactPaths.getOrDefault("classpath:" + descriptor.id(), "");
            Path jarPath = localPath.isBlank() ? Path.of("") : workspace.resolve(localPath).normalize();
            boolean classpathPresent = !localPath.isBlank() && Files.isRegularFile(jarPath);
            boolean jarReadable = classpathPresent && isReadableZip(jarPath);
            boolean descriptorPresentInJar = jarReadable && zipContains(jarPath, "META-INF/echo.mod.json");
            boolean entrypointDeclared = !entrypoint.isBlank();
            boolean entrypointClassPresent = entrypointDeclared && jarReadable && zipContains(jarPath, entrypoint.replace('.', '/') + ".class");
            boolean nativeEntrypointDeclared = !nativeEntrypoint.isBlank();
            boolean nativeEntrypointClassPresent = nativeEntrypointDeclared && jarReadable && zipContains(jarPath, nativeEntrypoint.replace('.', '/') + ".class");
            String state = classpathPresent && jarReadable && entrypointDeclared && entrypointClassPresent
                    ? "bootstrap_visible"
                    : "descriptor_discovered";
            if (!classpathPresent) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-MODULE-BRIDGE-CLASSPATH-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Module classpath artifact is missing",
                        "The native module runtime bridge cannot see a reviewed classpath artifact for " + descriptor.id() + ".",
                        descriptor.id(),
                        packId,
                        List.of("reports/echo-native/" + packId + "/local-runtime-artifact-map.json"),
                        "Regenerate or review local runtime artifact mappings before module bridge verification."
                ));
            } else if (!jarReadable) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-MODULE-BRIDGE-JAR-UNREADABLE",
                        EchoNativeIssueSeverity.ERROR,
                        "Module classpath artifact is not a readable jar",
                        "The native module runtime bridge could not inspect the reviewed artifact for " + descriptor.id() + ".",
                        descriptor.id(),
                        packId,
                        List.of(relativePath(jarPath)),
                        "Rebuild or replace the reviewed module artifact before module bridge verification."
                ));
            } else if (!entrypointDeclared || !entrypointClassPresent) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-MODULE-BRIDGE-ENTRYPOINT-NOT-VISIBLE",
                        EchoNativeIssueSeverity.WARNING,
                        "Module entrypoint is not yet bootstrap-visible",
                        "The module artifact is present, but the descriptor entrypoint could not be proven visible for " + descriptor.id() + ".",
                        descriptor.id(),
                        packId,
                        List.of(relativePath(descriptor.descriptorPath()), relativePath(jarPath)),
                        "Confirm the descriptor entrypoint and module jar contents before marking this module bootstrap-visible."
                ));
            }
            moduleEvidence.add(new ModuleBridgeEvidence(
                    descriptor.id(),
                    descriptor.kind(),
                    descriptor.role(),
                    entrypoint,
                    nativeEntrypoint,
                    localPath,
                    classpathPresent,
                    jarReadable,
                    descriptorPresentInJar,
                    entrypointDeclared,
                    entrypointClassPresent,
                    nativeEntrypointDeclared,
                    nativeEntrypointClassPresent,
                    state
            ));
        }

        int classpathPresentCount = (int) moduleEvidence.stream().filter(ModuleBridgeEvidence::classpathPresent).count();
        int bootstrapVisibleCount = (int) moduleEvidence.stream().filter(evidence -> "bootstrap_visible".equals(evidence.bootstrapState())).count();
        int nativeBootstrapVisibleCount = (int) moduleEvidence.stream().filter(ModuleBridgeEvidence::nativeEntrypointClassPresent).count();
        int liveGameplayActiveCount = 0;
        diagnostics.add(new EchoNativeDiagnostic(
                "ECHO-NATIVE-PRODUCT-LIVE-GAMEPLAY-ACTIVATION-PENDING",
                EchoNativeIssueSeverity.WARNING,
                "Native product live gameplay activation is still pending",
                "All visible module evidence is classpath and jar-entrypoint based; no addon code was executed and no gameplay hooks were verified live.",
                null,
                packId,
                List.of("reports/echo-native/" + packId + "/native-product-playable-gate.json"),
                "Implement the next controlled runtime bridge slice that can verify live module activation without transforms or registry mutation."
        ));
        boolean bridgeEvidenceReady = diagnostics.stream().noneMatch(EchoNativeModuleRuntimeBridgeVerifier::isBlocking);
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();

        return new EchoNativeModuleRuntimeBridgeOutcome(
                packId,
                bridgePlan(packId, descriptors.size(), classpathPresentCount, bootstrapVisibleCount, nativeBootstrapVisibleCount, sortedDiagnostics),
                bridgeSafetyGate(packId, bridgeEvidenceReady, sortedDiagnostics),
                bootstrapStatus(packId, moduleEvidence, classpathPresentCount, bootstrapVisibleCount, nativeBootstrapVisibleCount, sortedDiagnostics),
                liveActivationStatus(packId, moduleEvidence, bootstrapVisibleCount, liveGameplayActiveCount, sortedDiagnostics),
                playableBetaGate(packId, descriptors.size(), bootstrapVisibleCount, liveGameplayActiveCount, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> bridgePlan(
            String packId,
            int descriptorCount,
            int classpathPresentCount,
            int bootstrapVisibleCount,
            int nativeBootstrapVisibleCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_native_module_runtime_bridge_plan", diagnostics);
        data.put("bridgeEvidenceMode", "descriptor_classpath_jar_introspection");
        data.put("descriptorCount", descriptorCount);
        data.put("classpathPresentCount", classpathPresentCount);
        data.put("bootstrapVisibleCount", bootstrapVisibleCount);
        data.put("nativeBootstrapVisibleCount", nativeBootstrapVisibleCount);
        data.put("liveGameplayActivationAllowed", false);
        data.put("moduleBridgeStarted", true);
        data.put("packId", packId);
        data.put("plannedStates", List.of("descriptor_discovered", "classpath_present", "bootstrap_visible", "live_gameplay_active"));
        data.put("summary", "M21 verifies module bridge evidence without loading classes or claiming gameplay activation.");
        return data;
    }

    private static Map<String, Object> bridgeSafetyGate(
            String packId,
            boolean bridgeEvidenceReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_native_module_runtime_bridge_safety_gate", diagnostics);
        data.put("bridgeEvidenceReady", bridgeEvidenceReady);
        data.put("classInspectionOnly", true);
        data.put("classloaderCreated", false);
        data.put("gameClassesResolved", false);
        data.put("liveGameplayActivationAllowed", false);
        data.put("packId", packId);
        data.put("serviceCodeExecuted", false);
        data.put("summary", bridgeEvidenceReady
                ? "Module bridge safety gate allows descriptor and jar visibility evidence only."
                : "Module bridge safety gate is blocked by missing or unreadable module evidence.");
        return data;
    }

    private static Map<String, Object> bootstrapStatus(
            String packId,
            List<ModuleBridgeEvidence> moduleEvidence,
            int classpathPresentCount,
            int bootstrapVisibleCount,
            int nativeBootstrapVisibleCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_native_module_bootstrap_status", diagnostics);
        data.put("bootstrapVisibleCount", bootstrapVisibleCount);
        data.put("classpathPresentCount", classpathPresentCount);
        data.put("descriptorCount", moduleEvidence.size());
        data.put("modules", moduleEvidence.stream().map(EchoNativeModuleRuntimeBridgeVerifier::moduleData).toList());
        data.put("nativeBootstrapVisibleCount", nativeBootstrapVisibleCount);
        data.put("packId", packId);
        data.put("summary", "Module bootstrap visibility is based on reviewed classpath artifacts, ordinary entrypoint inspection, and native entrypoint class inspection.");
        return data;
    }

    private static Map<String, Object> liveActivationStatus(
            String packId,
            List<ModuleBridgeEvidence> moduleEvidence,
            int bootstrapVisibleCount,
            int liveGameplayActiveCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_native_product_live_module_activation_status", diagnostics);
        data.put("bootstrapVisibleCount", bootstrapVisibleCount);
        data.put("descriptorCount", moduleEvidence.size());
        data.put("liveGameplayActiveCount", liveGameplayActiveCount);
        data.put("modules", moduleEvidence.stream().map(evidence -> {
            Map<String, Object> item = moduleData(evidence);
            item.put("liveGameplayActive", false);
            item.put("liveActivationState", "bootstrap_visible".equals(evidence.bootstrapState())
                    ? "bootstrap_visible_gameplay_pending"
                    : evidence.bootstrapState());
            return item;
        }).toList());
        data.put("packId", packId);
        data.put("playableNativeProductModules", false);
        data.put("summary", "Native product modules now have bridge visibility evidence, but live gameplay activation remains pending.");
        return data;
    }

    private static Map<String, Object> playableBetaGate(
            String packId,
            int descriptorCount,
            int bootstrapVisibleCount,
            int liveGameplayActiveCount,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_native_product_playable_gate", diagnostics);
        data.put("nativeProductPlayableReady", false);
        data.put("bootstrapVisibleCount", bootstrapVisibleCount);
        data.put("descriptorCount", descriptorCount);
        data.put("liveGameplayActiveCount", liveGameplayActiveCount);
        data.put("minecraftBaselinePlayable", true);
        data.put("packId", packId);
        data.put("remainingActivationCount", descriptorCount - liveGameplayActiveCount);
        data.put("summary", "Native product playability remains blocked until live gameplay activation is verified for the required modules.");
        return data;
    }

    private static Map<String, Object> moduleData(ModuleBridgeEvidence evidence) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bootstrapState", evidence.bootstrapState());
        data.put("classpathPresent", evidence.classpathPresent());
        data.put("descriptorDiscovered", true);
        data.put("descriptorPresentInJar", evidence.descriptorPresentInJar());
        data.put("entrypoint", evidence.entrypoint());
        data.put("entrypointClassPresent", evidence.entrypointClassPresent());
        data.put("entrypointDeclared", evidence.entrypointDeclared());
        data.put("id", evidence.id());
        data.put("jarReadable", evidence.jarReadable());
        data.put("kind", evidence.kind());
        data.put("localPath", evidence.localPath());
        data.put("nativeBootstrapState", evidence.nativeEntrypointClassPresent()
                ? "native_bootstrap_visible"
                : evidence.nativeEntrypointDeclared() ? "native_entrypoint_declared_class_missing" : "native_entrypoint_not_declared");
        data.put("nativeEntrypoint", evidence.nativeEntrypoint());
        data.put("nativeEntrypointClassPresent", evidence.nativeEntrypointClassPresent());
        data.put("nativeEntrypointDeclared", evidence.nativeEntrypointDeclared());
        data.put("role", evidence.role());
        return data;
    }

    private static Map<String, String> artifactPaths(Map<String, Object> report) {
        Map<String, Object> data = EchoNativeJson.asObject(report == null ? null : report.get("data"));
        Object artifacts = data.get("artifacts");
        if (!(artifacts instanceof List<?> list)) {
            return Map.of();
        }
        Map<String, String> paths = new LinkedHashMap<>();
        for (Object raw : list) {
            Map<String, Object> artifact = EchoNativeJson.asObject(raw);
            String id = String.valueOf(artifact.getOrDefault("id", ""));
            String localPath = String.valueOf(artifact.getOrDefault("localPath", ""));
            if (!id.isBlank() && !localPath.isBlank()) {
                paths.put(id, localPath);
            }
        }
        return paths;
    }

    private static String descriptorEntrypoint(Path descriptorPath) {
        if (!Files.isRegularFile(descriptorPath)) {
            return "";
        }
        try {
            Map<String, Object> descriptor = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(descriptorPath)));
            return String.valueOf(descriptor.getOrDefault("entrypoint", ""));
        } catch (RuntimeException | IOException exception) {
            return "";
        }
    }

    private static String nativeEntrypoint(EchoNativeAddonDescriptor descriptor) {
        Object raw = descriptor.access().get("nativeEntrypoint");
        String value = raw == null ? "" : String.valueOf(raw).trim();
        if (!value.isBlank()) {
            return value;
        }
        return "";
    }

    private static boolean isReadableZip(Path jarPath) {
        try (ZipFile ignored = new ZipFile(jarPath.toFile())) {
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean zipContains(Path jarPath, String entryName) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            return zip.getEntry(entryName) != null;
        } catch (IOException exception) {
            return false;
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
                    "ECHO-NATIVE-MODULE-BRIDGE-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Module bridge required report missing",
                    "The module bridge verifier requires " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(fixture)),
                    "Regenerate native M20 and artifact mapping reports before module bridge verification."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkAcceptedReport(
            String reportName,
            Path reportPath,
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
                    "ECHO-NATIVE-MODULE-BRIDGE-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Module bridge upstream report is not accepted",
                    "The module bridge verifier requires PASS or PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativePath(reportPath)),
                    "Resolve upstream diagnostics before module bridge verification."
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
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("generatedEvidenceAt", Instant.EPOCH.toString());
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String relativePath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record ModuleBridgeEvidence(
            String id,
            String kind,
            String role,
            String entrypoint,
            String nativeEntrypoint,
            String localPath,
            boolean classpathPresent,
            boolean jarReadable,
            boolean descriptorPresentInJar,
            boolean entrypointDeclared,
            boolean entrypointClassPresent,
            boolean nativeEntrypointDeclared,
            boolean nativeEntrypointClassPresent,
            String bootstrapState
    ) {
    }
}
