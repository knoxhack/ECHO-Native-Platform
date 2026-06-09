package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.ai.EchoNativeAiPlan;
import dev.echo.nativeplatform.ai.EchoNativeAiPlanner;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapPlan;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeReportStatus;
import dev.echo.nativeplatform.contracts.EchoNativeTransformCompatibilityPolicy;
import dev.echo.nativeplatform.diagnostics.EchoNativeReportWriter;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.loader.EchoNativeBootstrapPlanner;
import dev.echo.nativeplatform.loader.EchoNativeDescriptorScanner;
import dev.echo.nativeplatform.loader.EchoNativeGraphPlan;
import dev.echo.nativeplatform.loader.EchoNativeGraphPlanner;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.loader.EchoNativeValidator;
import dev.echo.nativeplatform.packos.EchoNativeLockfileGenerator;
import dev.echo.nativeplatform.packos.EchoNativeLockfilePlan;
import dev.echo.nativeplatform.packos.EchoNativeLockfileVerificationPlan;
import dev.echo.nativeplatform.packos.EchoNativeLockfileVerifier;
import dev.echo.nativeplatform.packos.EchoNativeRepairPlan;
import dev.echo.nativeplatform.packos.EchoNativeRepairPlanGenerator;
import dev.echo.nativeplatform.product.EchoNativeProductLauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeQaCli {
    private final EchoNativeDescriptorScanner scanner = new EchoNativeDescriptorScanner();
    private final EchoNativeValidator validator = new EchoNativeValidator();
    private final EchoNativeBootstrapPlanner bootstrapPlanner = new EchoNativeBootstrapPlanner();
    private final EchoNativeGraphPlanner graphPlanner = new EchoNativeGraphPlanner();
    private final EchoNativeLockfileGenerator lockfileGenerator = new EchoNativeLockfileGenerator();
    private final EchoNativeLockfileVerifier lockfileVerifier = new EchoNativeLockfileVerifier();
    private final EchoNativeRepairPlanGenerator repairPlanGenerator = new EchoNativeRepairPlanGenerator();
    private final EchoNativeAiPlanner aiPlanner = new EchoNativeAiPlanner();
    private final EchoNativePhase12GateVerifier phase12GateVerifier = new EchoNativePhase12GateVerifier();
    private final EchoNativePhase13Planner phase13Planner = new EchoNativePhase13Planner();
    private final EchoNativePhase13LifecycleSimulator phase13LifecycleSimulator = new EchoNativePhase13LifecycleSimulator();
    private final EchoNativePhase13ServiceSimulator phase13ServiceSimulator = new EchoNativePhase13ServiceSimulator();
    private final EchoNativePhase13CrashBoundarySimulator phase13CrashBoundarySimulator = new EchoNativePhase13CrashBoundarySimulator();
    private final EchoNativePhase13BoundaryVerifier phase13BoundaryVerifier = new EchoNativePhase13BoundaryVerifier();
    private final EchoNativePhase13TestProcessVerifier phase13TestProcessVerifier = new EchoNativePhase13TestProcessVerifier();
    private final EchoNativePhase13BridgeRehearser phase13BridgeRehearser = new EchoNativePhase13BridgeRehearser();
    private final EchoNativePhase13M1CloseoutVerifier phase13M1CloseoutVerifier = new EchoNativePhase13M1CloseoutVerifier();
    private final EchoNativeMinecraftResolverPlanner minecraftResolverPlanner = new EchoNativeMinecraftResolverPlanner();
    private final EchoNativeLibraryResolverPlanner libraryResolverPlanner = new EchoNativeLibraryResolverPlanner();
    private final EchoNativeClasspathPlanner classpathPlanner = new EchoNativeClasspathPlanner();
    private final EchoNativeNativeExtractionPlanner nativeExtractionPlanner = new EchoNativeNativeExtractionPlanner();
    private final EchoNativeLaunchArgumentPlanner launchArgumentPlanner = new EchoNativeLaunchArgumentPlanner();
    private final EchoNativeControlledDummyProcessRunner controlledDummyProcessRunner = new EchoNativeControlledDummyProcessRunner();
    private final EchoNativeAddonRuntimeDiscoveryPlanner addonRuntimeDiscoveryPlanner = new EchoNativeAddonRuntimeDiscoveryPlanner();
    private final EchoNativeLifecycleStubExecutor lifecycleStubExecutor = new EchoNativeLifecycleStubExecutor();
    private final EchoNativeServiceBusPrototype serviceBusPrototype = new EchoNativeServiceBusPrototype();
    private final EchoNativeConfigPrototype configPrototype = new EchoNativeConfigPrototype();
    private final EchoNativeResourcePrototype resourcePrototype = new EchoNativeResourcePrototype();
    private final EchoNativeRegistryPrototype registryPrototype = new EchoNativeRegistryPrototype();
    private final EchoNativeNetworkPrototype networkPrototype = new EchoNativeNetworkPrototype();
    private final EchoNativeTransformPrototype transformPrototype = new EchoNativeTransformPrototype();
    private final EchoNativeCrashHardeningVerifier crashHardeningVerifier = new EchoNativeCrashHardeningVerifier();
    private final EchoNativeLaunchPreflightVerifier launchPreflightVerifier = new EchoNativeLaunchPreflightVerifier();
    private final EchoNativeIsolatedLaunchAttemptRunner isolatedLaunchAttemptRunner = new EchoNativeIsolatedLaunchAttemptRunner();
    private final EchoNativeArtifactInventoryPlanner artifactInventoryPlanner = new EchoNativeArtifactInventoryPlanner();
    private final EchoNativeArtifactMapper artifactMapper = new EchoNativeArtifactMapper();
    private final EchoNativeArtifactBlockerVerifier artifactBlockerVerifier = new EchoNativeArtifactBlockerVerifier();
    private final EchoNativeArtifactPackagingAuditor artifactPackagingAuditor = new EchoNativeArtifactPackagingAuditor();
    private final EchoNativeRuntimeFixtureVerifier runtimeFixtureVerifier = new EchoNativeRuntimeFixtureVerifier();
    private final EchoNativeRuntimeFixtureIntakePlanner runtimeFixtureIntakePlanner = new EchoNativeRuntimeFixtureIntakePlanner();
    private final EchoNativeRuntimeFixtureApprovalAuditor runtimeFixtureApprovalAuditor = new EchoNativeRuntimeFixtureApprovalAuditor();
    private final EchoNativeRuntimeFixtureHandoffPreparer runtimeFixtureHandoffPreparer = new EchoNativeRuntimeFixtureHandoffPreparer();
    private final EchoNativeRuntimeFixtureApprovalDraftPlanner runtimeFixtureApprovalDraftPlanner = new EchoNativeRuntimeFixtureApprovalDraftPlanner();
    private final EchoNativeRuntimeFixtureIntegrityAuditor runtimeFixtureIntegrityAuditor = new EchoNativeRuntimeFixtureIntegrityAuditor();
    private final EchoNativeRuntimeFixtureOperatorPacketExporter runtimeFixtureOperatorPacketExporter = new EchoNativeRuntimeFixtureOperatorPacketExporter();
    private final EchoNativePhase13M17CloseoutVerifier phase13M17CloseoutVerifier = new EchoNativePhase13M17CloseoutVerifier();
    private final EchoNativePhase13M18SmokeSessionVerifier phase13M18SmokeSessionVerifier = new EchoNativePhase13M18SmokeSessionVerifier();
    private final EchoNativeFirstPlaytestCandidatePackager firstPlaytestCandidatePackager = new EchoNativeFirstPlaytestCandidatePackager();
    private final EchoNativeFirstPlaytestRoadmapPlanner firstPlaytestRoadmapPlanner = new EchoNativeFirstPlaytestRoadmapPlanner();
    private final EchoNativePhase14PreflightAuditor phase14PreflightAuditor = new EchoNativePhase14PreflightAuditor();
    private final EchoNativeLaunchRealityAuditor launchRealityAuditor = new EchoNativeLaunchRealityAuditor();
    private final EchoNativeIsolatedRuntimeWorkspacePreparer isolatedRuntimeWorkspacePreparer = new EchoNativeIsolatedRuntimeWorkspacePreparer();
    private final EchoNativeRealProcessLaunchHarnessPlanner realProcessLaunchHarnessPlanner = new EchoNativeRealProcessLaunchHarnessPlanner();
    private final EchoNativeExecutionReadinessVerifier executionReadinessVerifier = new EchoNativeExecutionReadinessVerifier();
    private final EchoNativeControlledProcessLauncher controlledProcessLauncher = new EchoNativeControlledProcessLauncher();
    private final EchoNativeTesterEvidenceIntake testerEvidenceIntake = new EchoNativeTesterEvidenceIntake();
    private final EchoNativeModuleRuntimeBridgeVerifier moduleRuntimeBridgeVerifier = new EchoNativeModuleRuntimeBridgeVerifier();
    private final EchoNativeLiveActivationPlanner liveActivationPlanner = new EchoNativeLiveActivationPlanner();
    private final EchoNativeBootstrapActivator bootstrapActivator = new EchoNativeBootstrapActivator();
    private final EchoNativeGameplayHookVerifier gameplayHookVerifier = new EchoNativeGameplayHookVerifier();
    private final EchoNativeGameplayHookBridgePlanner gameplayHookBridgePlanner = new EchoNativeGameplayHookBridgePlanner();
    private final EchoNativeGameplayHookInstrumentor gameplayHookInstrumentor = new EchoNativeGameplayHookInstrumentor();
    private final EchoNativePlayableBetaVerifier playableBetaVerifier = new EchoNativePlayableBetaVerifier();
    private final EchoNativeBetaFeedbackIntake betaFeedbackIntake = new EchoNativeBetaFeedbackIntake();
    private final EchoNativeBetaWideningVerifier betaWideningVerifier = new EchoNativeBetaWideningVerifier();
    private final EchoNativeBetaSoakIntake betaSoakIntake = new EchoNativeBetaSoakIntake();
    private final EchoNativePublicBetaCandidateVerifier publicBetaCandidateVerifier = new EchoNativePublicBetaCandidateVerifier();
    private final EchoNativePublicBetaOpeningVerifier publicBetaOpeningVerifier = new EchoNativePublicBetaOpeningVerifier();
    private final EchoNativeBetaSoakOperatorPacketExporter betaSoakOperatorPacketExporter = new EchoNativeBetaSoakOperatorPacketExporter();
    private final EchoNativeBetaSoakEvidenceAuditor betaSoakEvidenceAuditor = new EchoNativeBetaSoakEvidenceAuditor();
    private final EchoNativeBetaSessionDraftPreparer betaSessionDraftPreparer = new EchoNativeBetaSessionDraftPreparer();
    private final EchoNativeBetaSessionNoteValidator betaSessionNoteValidator = new EchoNativeBetaSessionNoteValidator();
    private final EchoNativeBetaSoakStatusReporter betaSoakStatusReporter = new EchoNativeBetaSoakStatusReporter();
    private final EchoNativeStaticSafetyScanner staticSafetyScanner = new EchoNativeStaticSafetyScanner();
    private final EchoNativeRuntimeCli runtimeCli = new EchoNativeRuntimeCli();
    private final EchoNativeProductLauncher productLauncher = new EchoNativeProductLauncher();

    public static void main(String[] args) throws Exception {
        int exitCode = new EchoNativeQaCli().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args) throws IOException {
        if (args.length < 2) {
            printHelp();
            return 2;
        }
        return switch (args[0]) {
            case "scan" -> scan(Path.of(args[1]));
            case "validate" -> validate(Path.of(args[1]));
            case "graph" -> graph(Path.of(args[1]));
            case "features" -> features(Path.of(args[1]));
            case "lock" -> lock(args);
            case "repair" -> repair(args);
            case "ai" -> ai(args);
            case "launch" -> launchNativeProduct(args, 1);
            case "phase12" -> phase12(args);
            case "phase13" -> phase13(args);
            case "phase14" -> phase14(args);
            case "bootstrap" -> bootstrap(args);
            case "report" -> report(Path.of(args[1]));
            case "native" -> nativeCommand(args);
            default -> {
                printHelp();
                yield 2;
            }
        };
    }

    private int nativeCommand(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: native <discover|resolve|load|module-status|prove-live|launch|transform-policy> <product-root> [--require-mutation] [--require-live-runtime]");
            return 2;
        }
        Path fixture = Path.of(args[2]);
        return switch (args[1]) {
            case "discover" -> runtimeCli.discover(fixture);
            case "resolve" -> runtimeCli.resolve(fixture);
            case "load" -> runtimeCli.load(fixture);
            case "module-status" -> runtimeCli.moduleStatus(fixture);
            case "prove-live" -> runtimeCli.proveLive(fixture);
            case "launch" -> launchNativeProduct(args, 2);
            case "transform-policy" -> transformPolicy(fixture);
            default -> {
                System.out.println("Unknown native subcommand: " + args[1]);
                yield 2;
            }
        };
    }

    private int launchNativeProduct(String[] args, int productRootIndex) throws IOException {
        if (args.length <= productRootIndex) {
            System.out.println("Usage: echo-native launch <product-root> [--require-mutation] [--release] [--require-live-runtime]");
            return 2;
        }
        if (args[productRootIndex].startsWith("--")) {
            System.out.println("Missing product root.");
            System.out.println("Usage: echo-native launch <product-root> [--require-mutation] [--release] [--require-live-runtime]");
            return 2;
        }
        Path productRoot = Path.of(args[productRootIndex]);
        boolean requireMutation = List.of(args).contains("--require-mutation");
        boolean releaseMode = List.of(args).contains("--release");
        boolean requireLiveRuntime = List.of(args).contains("--require-live-runtime");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = productLauncher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        requireMutation,
                        releaseMode,
                        requireLiveRuntime));
        System.out.println("Native product launch for " + outcome.packId() + ": "
                + outcome.loadedModules() + "/" + outcome.totalModules() + " loaded, "
                + outcome.registeredModules() + "/" + outcome.totalModules() + " registered, "
                + outcome.mutatedModules() + "/" + outcome.totalModules() + " mutated, "
                + outcome.failedModules() + " failed"
                + (outcome.requireMutation() ? " [mutation required]" : "")
                + (outcome.releaseMode() ? " [release]" : "")
                + (outcome.requireLiveRuntime() ? " [live runtime required]" : ""));
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities = outcome.runtimeCapabilities();
        System.out.println("  runtime: firstClassNative=" + capabilities.firstClassNativeRuntime()
                + ", delegateRequired=" + capabilities.delegateRequired()
                + ", liveMinecraftAttached=" + capabilities.liveMinecraftAttached()
                + ", releaseRuntimeTrusted=" + capabilities.liveRuntimeTrusted()
                + ", liveRuntimeBridgeAttached=" + capabilities.liveRuntimeBridgeAttached()
                + ", liveRegistryBridgeAttached=" + capabilities.liveRegistryBridgeAttached()
                + ", savesDirectoryConfigured=" + capabilities.savesDirectoryConfigured());
        System.out.println("  client: uiHostAttached=" + capabilities.clientUiHostAttached()
                + ", liveClientAttached=" + capabilities.liveClientAttached()
                + ", releaseClientTrusted=" + capabilities.liveClientTrusted()
                + ", liveClientBridgeAttached=" + capabilities.liveClientBridgeAttached()
                + ", headlessClientSurface=" + capabilities.headlessClientSurface()
                + ", registeredSurfaces=" + capabilities.clientSurfaceCount());
        for (EchoNativeProductLauncher.EchoNativeProductModuleLaunch module : outcome.modules()) {
            String status = module.accepted() ? "PASS" : "FAIL";
            System.out.println("  [" + status + "] " + module.moduleId()
                    + " claimed=" + module.claimedStatus()
                    + " honest=" + module.honestStatus()
                    + " loaded=" + module.loaded()
                    + " registered=" + module.registered()
                    + " mutated=" + module.mutated());
            if (!module.failures().isEmpty()) {
                System.out.println("      " + String.join("; ", module.failures()));
            }
        }
        if (!outcome.accepted()) {
            System.out.println("Native product launch is not release-ready yet.");
        }
        printDiagnostics(outcome.diagnostics());
        return outcome.accepted() ? 0 : 1;
    }

    private int transformPolicy(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        List<EchoNativeDiagnostic> diagnostics = new java.util.ArrayList<>(result.diagnostics());
        String packId = packId(result);
        int compatible = 0;
        int blocked = 0;
        int requested = 0;
        for (var descriptor : result.descriptors()) {
            EchoNativeTransformCompatibilityPolicy.TransformCompatibilityReport report =
                    EchoNativeTransformCompatibilityPolicy.evaluate(packId, descriptor);
            diagnostics.addAll(report.diagnostics());
            if (report.hasTransformRequests()) {
                requested++;
            }
            if (report.compatible()) {
                compatible++;
            } else {
                blocked++;
            }
            if (report.hasTransformRequests() || !report.compatible()) {
                System.out.println("  " + descriptor.id()
                        + ": policy=" + report.policyId()
                        + ", decision=" + report.policyDecision()
                        + ", compatible=" + report.compatible()
                        + ", bytecodeMutationAllowed=" + report.bytecodeMutationAllowed()
                        + ", minecraftBytecodeMutationAllowed=" + report.minecraftBytecodeMutationAllowed()
                        + ", addonBytecodeMutationAllowed=" + report.addonBytecodeMutationAllowed()
                        + ", supportedNativeDeclarations=" + report.supportedNativeDeclarations()
                        + ", declaredForgeStyleTransforms=" + report.declaredForgeStyleTransforms()
                        + ", declaredNativeReplacements=" + report.declaredNativeReplacements()
                        + ", declaredReplacementMappings=" + report.declaredReplacementMappings()
                        + ", incompatibleForgeStyleRequests=" + report.incompatibleForgeStyleRequests()
                        + ", unsupportedNativeRequests=" + report.unsupportedNativeRequests()
                        + ", unsupportedDeclaredNativeReplacements=" + report.unsupportedDeclaredNativeReplacements()
                        + ", unsupportedMappedNativeReplacements=" + report.unsupportedMappedNativeReplacements()
                        + ", unmappedForgeStyleTransforms=" + report.unmappedForgeStyleTransforms()
                        + ", unknownMappedForgeStyleTransforms=" + report.unknownMappedForgeStyleTransforms());
            }
        }
        System.out.println("Transform policy for " + packId + ": "
                + compatible + "/" + result.descriptors().size() + " compatible, "
                + requested + " requested transforms, "
                + blocked + " blocked.");
        System.out.println("Supported native transform declarations: "
                + EchoNativeTransformCompatibilityPolicy.supportedNativeDeclarations());
        System.out.println("Supported transform incompatibility policy: "
                + EchoNativeTransformCompatibilityPolicy.supportedIncompatibilityPolicy());
        System.out.println("Release transform policy: "
                + EchoNativeTransformCompatibilityPolicy.releasePolicySummary());
        System.out.println("Transform diagnostic path: "
                + EchoNativeTransformCompatibilityPolicy.diagnosticPathSummary());
        printDiagnostics(diagnostics);
        return hasBlocking(diagnostics) ? 1 : 0;
    }

    private static void printDiagnostics(List<EchoNativeDiagnostic> diagnostics) {
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.WARNING
                        || diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL)
                .forEach(diagnostic -> System.out.println("  [" + diagnostic.severity() + "] "
                        + diagnostic.code() + ": " + diagnostic.summary()));
    }

    private int scan(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        writeScanReports(fixture, result);
        System.out.println("Scanned " + result.descriptors().size() + " descriptors for " + packId(result) + ".");
        return hasBlocking(result.diagnostics()) ? 1 : 0;
    }

    private int validate(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, diagnostics);
        System.out.println("Validated " + packId(result) + " with " + diagnostics.size() + " diagnostics.");
        return hasBlocking(diagnostics) ? 1 : 0;
    }

    private int graph(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, graphPlan.diagnostics());
        writeGraphReports(fixture, result, graphPlan);
        System.out.println("Generated native graph reports for " + packId(result) + ".");
        return hasBlocking(graphPlan.diagnostics()) ? 1 : 0;
    }

    private int features(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, graphPlan.diagnostics());
        writeFeatureReport(fixture, result, graphPlan);
        System.out.println("Generated native feature graph for " + packId(result) + ".");
        return hasBlocking(graphPlan.diagnostics()) ? 1 : 0;
    }

    private int bootstrap(String[] args) throws IOException {
        if (args.length != 3 || !"--dry-run".equals(args[1])) {
            printHelp();
            return 2;
        }
        Path fixture = Path.of(args[2]);
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeBootstrapPlan plan = bootstrapPlanner.plan(result);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, plan.diagnostics());
        writeGraphReports(fixture, result, graphPlan);
        writeFeatureReport(fixture, result, graphPlan);
        writeBootstrapReports(fixture, result, plan);
        System.out.println("Generated dry-run bootstrap plan for " + plan.packId() + ".");
        return hasBlocking(plan.diagnostics()) ? 1 : 0;
    }

    private int report(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeBootstrapPlan plan = bootstrapPlanner.plan(result);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, plan.diagnostics());
        writeGraphReports(fixture, result, graphPlan);
        writeFeatureReport(fixture, result, graphPlan);
        writeBootstrapReports(fixture, result, plan);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), plan.diagnostics());
        writeLockfileReport(fixture, result, lockfilePlan);
        EchoNativeLockfileVerificationPlan lockfileStatus = lockfileVerifier.verify(result.packProfile(), lockfilePlan, reportPath(fixture, result, "lockfile.json"));
        writeLockfileStatusReport(fixture, result, lockfileStatus);
        EchoNativeRepairPlan repairPlan = repairPlanGenerator.plan(packId(result), lockfileStatus.diagnostics());
        writeRepairPlanReport(fixture, result, repairPlan);
        EchoNativeAiPlan aiPlan = aiPlanner.plan(packId(result), result.descriptors(), repairPlan.diagnostics(), graphPlan.moduleGraph(), graphPlan.featureGraph(), graphPlan.serviceGraph(), lockfileStatus.status(), repairPlan.repairPlan());
        writeAiReports(fixture, result, aiPlan);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("aiGraph", aiPlan.aiGraph());
        data.put("phase13Blocked", true);
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, "report.json"), "echo.native.report.v1", "echo-native-cli", plan.packId(), status(plan.diagnostics()), summary(result, plan.diagnostics()), plan.diagnostics(), data);
        System.out.println("Generated report set for " + plan.packId() + ".");
        return hasBlocking(plan.diagnostics()) ? 1 : 0;
    }

    private int lock(String[] args) throws IOException {
        if (args.length != 3) {
            printHelp();
            return 2;
        }
        return switch (args[1]) {
            case "generate" -> lockGenerate(Path.of(args[2]));
            case "verify" -> lockVerify(Path.of(args[2]));
            default -> {
                printHelp();
                yield 2;
            }
        };
    }

    private int repair(String[] args) throws IOException {
        if (args.length != 3 || !"plan".equals(args[1])) {
            printHelp();
            return 2;
        }
        return repairPlan(Path.of(args[2]));
    }

    private int ai(String[] args) throws IOException {
        if (args.length != 3 || !"graph".equals(args[1])) {
            printHelp();
            return 2;
        }
        return aiGraph(Path.of(args[2]));
    }

    private int phase12(String[] args) throws IOException {
        if (args.length != 3 || !"verify".equals(args[1])) {
            printHelp();
            return 2;
        }
        return phase12Verify(Path.of(args[2]));
    }

    private int phase13(String[] args) throws IOException {
        if (args.length == 3 && "plan".equals(args[1])) {
            return phase13Plan(Path.of(args[2]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "minecraft-resolver".equals(args[2])) {
            return phase13PlanMinecraftResolver(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "library-resolver".equals(args[2])) {
            return phase13PlanLibraryResolver(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "classpath".equals(args[2])) {
            return phase13PlanClasspath(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "native-extraction".equals(args[2])) {
            return phase13PlanNativeExtraction(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "launch-arguments".equals(args[2])) {
            return phase13PlanLaunchArguments(Path.of(args[3]));
        }
        if (args.length == 4 && "run".equals(args[1]) && "dummy-process".equals(args[2])) {
            return phase13RunDummyProcess(Path.of(args[3]));
        }
        if (args.length == 4 && "discover".equals(args[1]) && "addons".equals(args[2])) {
            return phase13DiscoverAddons(Path.of(args[3]));
        }
        if (args.length == 4 && "execute".equals(args[1]) && "lifecycle-stubs".equals(args[2])) {
            return phase13ExecuteLifecycleStubs(Path.of(args[3]));
        }
        if (args.length == 4 && "prototype".equals(args[1]) && "service-bus".equals(args[2])) {
            return phase13PrototypeServiceBus(Path.of(args[3]));
        }
        if (args.length == 4 && "prototype".equals(args[1]) && "config".equals(args[2])) {
            return phase13PrototypeConfig(Path.of(args[3]));
        }
        if (args.length == 4 && "prototype".equals(args[1]) && "resources".equals(args[2])) {
            return phase13PrototypeResources(Path.of(args[3]));
        }
        if (args.length == 4 && "prototype".equals(args[1]) && "registry".equals(args[2])) {
            return phase13PrototypeRegistry(Path.of(args[3]));
        }
        if (args.length == 4 && "prototype".equals(args[1]) && "network".equals(args[2])) {
            return phase13PrototypeNetwork(Path.of(args[3]));
        }
        if (args.length == 4 && "prototype".equals(args[1]) && "transforms".equals(args[2])) {
            return phase13PrototypeTransforms(Path.of(args[3]));
        }
        if (args.length == 4 && "simulate".equals(args[1]) && "lifecycle".equals(args[2])) {
            return phase13SimulateLifecycle(Path.of(args[3]));
        }
        if (args.length == 4 && "simulate".equals(args[1]) && "services".equals(args[2])) {
            return phase13SimulateServices(Path.of(args[3]));
        }
        if (args.length == 4 && "simulate".equals(args[1]) && "crash-boundary".equals(args[2])) {
            return phase13SimulateCrashBoundary(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "boundaries".equals(args[2])) {
            return phase13VerifyBoundaries(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "test-process".equals(args[2])) {
            return phase13VerifyTestProcess(Path.of(args[3]));
        }
        if (args.length == 4 && "rehearse".equals(args[1]) && "bridges".equals(args[2])) {
            return phase13RehearseBridges(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "m1".equals(args[2])) {
            return phase13VerifyM1(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "crash-hardening".equals(args[2])) {
            return phase13VerifyCrashHardening(Path.of(args[3]));
        }
        if (args.length == 4 && "launch".equals(args[1]) && "preflight".equals(args[2])) {
            return phase13LaunchPreflight(Path.of(args[3]));
        }
        if (args.length == 4 && "map".equals(args[1]) && "artifacts".equals(args[2])) {
            return phase13MapArtifacts(Path.of(args[3]));
        }
        if (args.length == 4 && "inventory".equals(args[1]) && "artifacts".equals(args[2])) {
            return phase13InventoryArtifacts(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "artifact-blockers".equals(args[2])) {
            return phase13VerifyArtifactBlockers(Path.of(args[3]));
        }
        if (args.length == 4 && "audit".equals(args[1]) && "artifact-packaging".equals(args[2])) {
            return phase13AuditArtifactPackaging(Path.of(args[3]));
        }
        if (args.length == 4 && "audit".equals(args[1]) && "launch-reality".equals(args[2])) {
            return phase13AuditLaunchReality(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "runtime-fixtures".equals(args[2])) {
            return phase13VerifyRuntimeFixtures(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "runtime-fixture-intake".equals(args[2])) {
            return phase13PlanRuntimeFixtureIntake(Path.of(args[3]));
        }
        if (args.length == 4 && "audit".equals(args[1]) && "runtime-fixture-approval".equals(args[2])) {
            return phase13AuditRuntimeFixtureApproval(Path.of(args[3]));
        }
        if (args.length == 4 && "prepare".equals(args[1]) && "runtime-fixture-handoff".equals(args[2])) {
            return phase13PrepareRuntimeFixtureHandoff(Path.of(args[3]));
        }
        if (args.length == 4 && "prepare".equals(args[1]) && "isolated-runtime".equals(args[2])) {
            return phase13PrepareIsolatedRuntime(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "real-process-launch".equals(args[2])) {
            return phase13PlanRealProcessLaunch(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "execution-readiness".equals(args[2])) {
            return phase13VerifyExecutionReadiness(Path.of(args[3]));
        }
        if (args.length == 5 && "launch".equals(args[1]) && "controlled".equals(args[2]) && "--authorized".equals(args[3])) {
            return phase13LaunchControlledAuthorized(Path.of(args[4]));
        }
        if (args.length == 5 && "launch".equals(args[1]) && "tester".equals(args[2]) && "--authorized".equals(args[3])) {
            return phase13LaunchTesterAuthorized(Path.of(args[4]));
        }
        if (args.length == 4 && "intake".equals(args[1]) && "tester-evidence".equals(args[2])) {
            return phase13IntakeTesterEvidence(Path.of(args[3]));
        }
        if (args.length == 4 && "bridge".equals(args[1]) && "modules".equals(args[2])) {
            return phase13BridgeModules(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "live-activation".equals(args[2])) {
            return phase13PlanLiveActivation(Path.of(args[3]));
        }
        if (args.length == 5 && "activate".equals(args[1]) && "bootstrap".equals(args[2]) && "--authorized".equals(args[3])) {
            return phase13ActivateBootstrapAuthorized(Path.of(args[4]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "gameplay-hooks".equals(args[2])) {
            return phase13VerifyGameplayHooks(Path.of(args[3]));
        }
        if (args.length == 4 && "bridge".equals(args[1]) && "gameplay-hooks".equals(args[2])) {
            return phase13BridgeGameplayHooks(Path.of(args[3]));
        }
        if (args.length == 5 && "instrument".equals(args[1]) && "gameplay-hooks".equals(args[2]) && "--authorized".equals(args[3])) {
            return phase13InstrumentGameplayHooksAuthorized(Path.of(args[4]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "playable-beta".equals(args[2])) {
            return phase13VerifyPlayableBeta(Path.of(args[3]));
        }
        if (args.length == 4 && "intake".equals(args[1]) && "beta-feedback".equals(args[2])) {
            return phase13IntakeBetaFeedback(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "m28".equals(args[2])) {
            return phase13VerifyM28(Path.of(args[3]));
        }
        if (args.length == 4 && "intake".equals(args[1]) && "beta-soak".equals(args[2])) {
            return phase13IntakeBetaSoak(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "m30".equals(args[2])) {
            return phase13VerifyM30(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "m31".equals(args[2])) {
            return phase13VerifyM31(Path.of(args[3]));
        }
        if (args.length == 4 && "export".equals(args[1]) && "beta-soak-packet".equals(args[2])) {
            return phase13ExportBetaSoakPacket(Path.of(args[3]));
        }
        if (args.length == 4 && "prepare".equals(args[1]) && "beta-session-drafts".equals(args[2])) {
            return phase13PrepareBetaSessionDrafts(Path.of(args[3]));
        }
        if (args.length == 4 && "validate".equals(args[1]) && "beta-session-notes".equals(args[2])) {
            return phase13ValidateBetaSessionNotes(Path.of(args[3]));
        }
        if (args.length == 4 && "status".equals(args[1]) && "beta-soak".equals(args[2])) {
            return phase13StatusBetaSoak(Path.of(args[3]));
        }
        if (args.length == 4 && "audit".equals(args[1]) && "beta-soak-evidence".equals(args[2])) {
            return phase13AuditBetaSoakEvidence(Path.of(args[3]));
        }
        if (args.length == 4 && "draft".equals(args[1]) && "runtime-fixture-approval".equals(args[2])) {
            return phase13DraftRuntimeFixtureApproval(Path.of(args[3]));
        }
        if (args.length == 4 && "audit".equals(args[1]) && "runtime-fixture-integrity".equals(args[2])) {
            return phase13AuditRuntimeFixtureIntegrity(Path.of(args[3]));
        }
        if (args.length == 4 && "export".equals(args[1]) && "runtime-fixture-operator-packet".equals(args[2])) {
            return phase13ExportRuntimeFixtureOperatorPacket(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "m17".equals(args[2])) {
            return phase13VerifyM17(Path.of(args[3]));
        }
        if (args.length == 4 && "verify".equals(args[1]) && "m18".equals(args[2])) {
            return phase13VerifyM18(Path.of(args[3]));
        }
        if (args.length == 4 && "package".equals(args[1]) && "first-playtest".equals(args[2])) {
            return phase13PackageFirstPlaytest(Path.of(args[3]));
        }
        if (args.length == 4 && "plan".equals(args[1]) && "first-playtest".equals(args[2])) {
            return phase13PlanFirstPlaytest(Path.of(args[3]));
        }
        if (args.length == 5 && "launch".equals(args[1]) && "attempt".equals(args[2]) && "--isolated".equals(args[3])) {
            return phase13LaunchAttemptIsolated(Path.of(args[4]));
        }
        if (args.length != 3 || !"plan".equals(args[1])) {
            printHelp();
            return 2;
        }
        return phase13Plan(Path.of(args[2]));
    }

    private int phase14(String[] args) throws IOException {
        if (args.length == 3 && "preflight".equals(args[1])) {
            return phase14Preflight(Path.of(args[2]));
        }
        printHelp();
        return 2;
    }

    private int lockGenerate(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), diagnostics);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, diagnostics);
        writeLockfileReport(fixture, result, lockfilePlan);
        System.out.println("Generated native dry-run lockfile for " + packId(result) + ".");
        return hasBlocking(lockfilePlan.diagnostics()) ? 1 : 0;
    }

    private int lockVerify(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), diagnostics);
        EchoNativeLockfileVerificationPlan verificationPlan = lockfileVerifier.verify(result.packProfile(), lockfilePlan, reportPath(fixture, result, "lockfile.json"));
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, diagnostics);
        writeLockfileStatusReport(fixture, result, verificationPlan);
        System.out.println("Verified native dry-run lockfile for " + packId(result) + ".");
        return hasBlocking(verificationPlan.diagnostics()) ? 1 : 0;
    }

    private int repairPlan(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), diagnostics);
        EchoNativeLockfileVerificationPlan verificationPlan = lockfileVerifier.verify(result.packProfile(), lockfilePlan, reportPath(fixture, result, "lockfile.json"));
        EchoNativeRepairPlan repairPlan = repairPlanGenerator.plan(packId(result), verificationPlan.diagnostics());
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, diagnostics);
        writeLockfileStatusReport(fixture, result, verificationPlan);
        writeRepairPlanReport(fixture, result, repairPlan);
        System.out.println("Generated native dry-run repair plan for " + packId(result) + ".");
        return hasBlocking(repairPlan.diagnostics()) ? 1 : 0;
    }

    private int aiGraph(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), graphPlan.diagnostics());
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, graphPlan.diagnostics());
        writeGraphReports(fixture, result, graphPlan);
        writeFeatureReport(fixture, result, graphPlan);
        writeLockfileReport(fixture, result, lockfilePlan);
        EchoNativeLockfileVerificationPlan lockfileStatus = lockfileVerifier.verify(result.packProfile(), lockfilePlan, reportPath(fixture, result, "lockfile.json"));
        writeLockfileStatusReport(fixture, result, lockfileStatus);
        EchoNativeRepairPlan repairPlan = repairPlanGenerator.plan(packId(result), lockfileStatus.diagnostics());
        writeRepairPlanReport(fixture, result, repairPlan);
        EchoNativeAiPlan aiPlan = aiPlanner.plan(packId(result), result.descriptors(), repairPlan.diagnostics(), graphPlan.moduleGraph(), graphPlan.featureGraph(), graphPlan.serviceGraph(), lockfileStatus.status(), repairPlan.repairPlan());
        writeAiReports(fixture, result, aiPlan);
        System.out.println("Generated native AI graph for " + packId(result) + ".");
        return hasBlocking(aiPlan.diagnostics()) ? 1 : 0;
    }

    private int phase12Verify(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeBootstrapPlan bootstrapPlan = bootstrapPlanner.plan(result);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, bootstrapPlan.diagnostics());
        writeGraphReports(fixture, result, graphPlan);
        writeFeatureReport(fixture, result, graphPlan);
        writeBootstrapReports(fixture, result, bootstrapPlan);

        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), bootstrapPlan.diagnostics());
        writeLockfileReport(fixture, result, lockfilePlan);
        EchoNativeLockfileVerificationPlan lockfileStatus = lockfileVerifier.verify(result.packProfile(), lockfilePlan, reportPath(fixture, result, "lockfile.json"));
        writeLockfileStatusReport(fixture, result, lockfileStatus);

        EchoNativeRepairPlan repairPlan = repairPlanGenerator.plan(packId(result), lockfileStatus.diagnostics());
        writeRepairPlanReport(fixture, result, repairPlan);

        EchoNativeAiPlan aiPlan = aiPlanner.plan(packId(result), result.descriptors(), repairPlan.diagnostics(), graphPlan.moduleGraph(), graphPlan.featureGraph(), graphPlan.serviceGraph(), lockfileStatus.status(), repairPlan.repairPlan());
        writeAiReports(fixture, result, aiPlan);

        EchoNativeStaticSafetyScan safetyScan = staticSafetyScanner.scan();
        EchoNativePhase12GatePlan gatePlan = phase12GateVerifier.verify(fixture, result, graphPlan, bootstrapPlan, lockfileStatus, repairPlan, aiPlan, safetyScan);
        writePhase12Reports(fixture, result, gatePlan);
        System.out.println("Verified Phase 12 dry-run completion gate for " + packId(result) + ".");
        return hasBlocking(gatePlan.diagnostics()) ? 1 : 0;
    }

    private int phase13Plan(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        EchoNativePhase13PrototypePlan plan = phase13Planner.plan(fixture, reportPath(fixture, result, "phase13-readiness.json"), result, graphPlan);
        writePhase13Reports(fixture, result, plan);
        System.out.println("Generated Phase 13 prototype plan for " + packId(result) + ".");
        return hasBlocking(plan.diagnostics()) ? 1 : 0;
    }

    private int phase13SimulateLifecycle(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativePhase13LifecycleSimulationOutcome outcome = phase13LifecycleSimulator.simulate(
                packId(result),
                fixture,
                reportPath(fixture, result, "phase13-plan.json"),
                reportPath(fixture, result, "lifecycle-simulation-plan.json")
        );
        writePhase13SimulationReports(fixture, result, outcome);
        System.out.println("Simulated Phase 13 lifecycle plan for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13SimulateServices(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativePhase13ServiceSimulationOutcome outcome = phase13ServiceSimulator.simulate(
                packId(result),
                fixture,
                reportPath(fixture, result, "phase13-plan.json"),
                reportPath(fixture, result, "service-graph.json"),
                reportPath(fixture, result, "lifecycle-simulation-result.json")
        );
        writePhase13ServiceSimulationReports(fixture, result, outcome);
        System.out.println("Simulated Phase 13 service attachment for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13SimulateCrashBoundary(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativePhase13CrashBoundarySimulationOutcome outcome = phase13CrashBoundarySimulator.simulate(
                packId(result),
                fixture,
                reportPath(fixture, result, "phase13-plan.json"),
                reportPath(fixture, result, "lifecycle-simulation-result.json"),
                reportPath(fixture, result, "service-attach-simulation-result.json"),
                reportPath(fixture, result, "classloader-boundary-plan.json")
        );
        writePhase13CrashBoundarySimulationReports(fixture, result, outcome);
        System.out.println("Simulated Phase 13 crash boundary for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyBoundaries(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativePhase13BoundaryVerificationOutcome outcome = phase13BoundaryVerifier.verify(
                packId(result),
                fixture,
                reportPath(fixture, result, "phase13-plan.json"),
                reportPath(fixture, result, "lifecycle-simulation-result.json"),
                reportPath(fixture, result, "service-attach-simulation-result.json"),
                reportPath(fixture, result, "crash-boundary-simulation-result.json"),
                reportPath(fixture, result, "classloader-boundary-rehearsal.json"),
                reportPath(fixture, result, "classpath-plan.json")
        );
        writePhase13BoundaryVerificationReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 loader boundaries for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyTestProcess(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativePhase13TestProcessVerificationOutcome outcome = phase13TestProcessVerifier.verify(
                packId(result),
                fixture,
                reportPath(fixture, result, "phase13-plan.json"),
                reportPath(fixture, result, "test-process-plan.json"),
                reportPath(fixture, result, "loader-boundary-verification.json"),
                reportPath(fixture, result, "classpath-classloader-compatibility.json")
        );
        writePhase13TestProcessVerificationReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 test-process boundary for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13RehearseBridges(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativePhase13BridgeRehearsalOutcome outcome = phase13BridgeRehearser.rehearse(
                packId(result),
                fixture,
                reportPath(fixture, result, "phase13-plan.json"),
                reportPath(fixture, result, "loader-boundary-verification.json"),
                reportPath(fixture, result, "test-process-boundary-verification.json"),
                reportPath(fixture, result, "phase13-m1-safety-status.json")
        );
        writePhase13BridgeRehearsalReports(fixture, result, outcome);
        System.out.println("Rehearsed Phase 13 bridge policies for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyM1(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-plan.json", reportPath(fixture, result, "phase13-plan.json"));
        requiredReports.put("lifecycle-simulation-result.json", reportPath(fixture, result, "lifecycle-simulation-result.json"));
        requiredReports.put("service-attach-simulation-result.json", reportPath(fixture, result, "service-attach-simulation-result.json"));
        requiredReports.put("crash-boundary-simulation-result.json", reportPath(fixture, result, "crash-boundary-simulation-result.json"));
        requiredReports.put("loader-boundary-verification.json", reportPath(fixture, result, "loader-boundary-verification.json"));
        requiredReports.put("classpath-classloader-compatibility.json", reportPath(fixture, result, "classpath-classloader-compatibility.json"));
        requiredReports.put("test-process-boundary-verification.json", reportPath(fixture, result, "test-process-boundary-verification.json"));
        requiredReports.put("phase13-m1-safety-status.json", reportPath(fixture, result, "phase13-m1-safety-status.json"));
        requiredReports.put("resource-bridge-policy-rehearsal.json", reportPath(fixture, result, "resource-bridge-policy-rehearsal.json"));
        requiredReports.put("registry-bridge-policy-rehearsal.json", reportPath(fixture, result, "registry-bridge-policy-rehearsal.json"));
        requiredReports.put("phase13-bridge-safety-status.json", reportPath(fixture, result, "phase13-bridge-safety-status.json"));
        EchoNativePhase13M1CloseoutOutcome outcome = phase13M1CloseoutVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writePhase13M1CloseoutReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M1 closeout for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyCrashHardening(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-prototype-safety-gate.json", reportPath(fixture, result, "phase13-prototype-safety-gate.json"));
        requiredReports.put("minecraft-resolver-safety-status.json", reportPath(fixture, result, "minecraft-resolver-safety-status.json"));
        requiredReports.put("library-resolver-safety-status.json", reportPath(fixture, result, "library-resolver-safety-status.json"));
        requiredReports.put("classpath-builder-safety-status.json", reportPath(fixture, result, "classpath-builder-safety-status.json"));
        requiredReports.put("native-extraction-safety-status.json", reportPath(fixture, result, "native-extraction-safety-status.json"));
        requiredReports.put("launch-argument-safety-status.json", reportPath(fixture, result, "launch-argument-safety-status.json"));
        requiredReports.put("service-bus-safety-status.json", reportPath(fixture, result, "service-bus-safety-status.json"));
        requiredReports.put("config-safety-status.json", reportPath(fixture, result, "config-safety-status.json"));
        requiredReports.put("resource-bridge-safety-status.json", reportPath(fixture, result, "resource-bridge-safety-status.json"));
        requiredReports.put("registry-bridge-safety-status.json", reportPath(fixture, result, "registry-bridge-safety-status.json"));
        requiredReports.put("network-bridge-safety-status.json", reportPath(fixture, result, "network-bridge-safety-status.json"));
        requiredReports.put("transform-safety-status.json", reportPath(fixture, result, "transform-safety-status.json"));
        requiredReports.put("transform-conflict-report.json", reportPath(fixture, result, "transform-conflict-report.json"));
        EchoNativeCrashHardeningOutcome outcome = crashHardeningVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writeCrashHardeningReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 crash-boundary hardening for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13LaunchPreflight(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m16-safety-status.json", reportPath(fixture, result, "phase13-m16-safety-status.json"));
        requiredReports.put("failure-containment-matrix.json", reportPath(fixture, result, "failure-containment-matrix.json"));
        requiredReports.put("support-bundle-dry-run-plan.json", reportPath(fixture, result, "support-bundle-dry-run-plan.json"));
        requiredReports.put("launch-argument-safety-status.json", reportPath(fixture, result, "launch-argument-safety-status.json"));
        requiredReports.put("launch-argument-builder-plan.json", reportPath(fixture, result, "launch-argument-builder-plan.json"));
        requiredReports.put("classpath-builder-safety-status.json", reportPath(fixture, result, "classpath-builder-safety-status.json"));
        requiredReports.put("classpath-builder-plan.json", reportPath(fixture, result, "classpath-builder-plan.json"));
        requiredReports.put("native-extraction-safety-status.json", reportPath(fixture, result, "native-extraction-safety-status.json"));
        EchoNativeLaunchPreflightOutcome outcome = launchPreflightVerifier.verify(
                packId(result),
                fixture,
                result.packProfile(),
                requiredReports
        );
        writeLaunchPreflightReports(fixture, result, outcome);
        System.out.println("Prepared Phase 13 isolated launch preflight for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13LaunchAttemptIsolated(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-readiness.json", reportPath(fixture, result, "phase13-m17-readiness.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("isolated-launch-environment-plan.json", reportPath(fixture, result, "isolated-launch-environment-plan.json"));
        requiredReports.put("controlled-launch-failure-capture-plan.json", reportPath(fixture, result, "controlled-launch-failure-capture-plan.json"));
        requiredReports.put("classpath-builder-plan.json", reportPath(fixture, result, "classpath-builder-plan.json"));
        requiredReports.put("native-extraction-plan.json", reportPath(fixture, result, "native-extraction-plan.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("phase13-m17-artifact-readiness.json", reportPath(fixture, result, "phase13-m17-artifact-readiness.json"));
        EchoNativeIsolatedLaunchAttemptOutcome outcome = isolatedLaunchAttemptRunner.attempt(
                packId(result),
                fixture,
                requiredReports
        );
        writeIsolatedLaunchAttemptReports(fixture, result, outcome);
        System.out.println("Evaluated Phase 13 isolated launch attempt for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13MapArtifacts(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-readiness.json", reportPath(fixture, result, "phase13-m17-readiness.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("isolated-launch-environment-plan.json", reportPath(fixture, result, "isolated-launch-environment-plan.json"));
        requiredReports.put("classpath-builder-plan.json", reportPath(fixture, result, "classpath-builder-plan.json"));
        requiredReports.put("native-extraction-plan.json", reportPath(fixture, result, "native-extraction-plan.json"));
        EchoNativeArtifactMappingOutcome outcome = artifactMapper.map(
                packId(result),
                fixture,
                requiredReports
        );
        writeArtifactMappingReports(fixture, result, outcome);
        System.out.println("Mapped Phase 13 launch artifacts for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13InventoryArtifacts(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-readiness.json", reportPath(fixture, result, "phase13-m17-readiness.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("isolated-launch-environment-plan.json", reportPath(fixture, result, "isolated-launch-environment-plan.json"));
        requiredReports.put("classpath-builder-plan.json", reportPath(fixture, result, "classpath-builder-plan.json"));
        requiredReports.put("native-extraction-plan.json", reportPath(fixture, result, "native-extraction-plan.json"));
        EchoNativeArtifactInventoryOutcome outcome = artifactInventoryPlanner.inventory(
                packId(result),
                fixture,
                requiredReports
        );
        writeArtifactInventoryReports(fixture, result, outcome);
        System.out.println("Inventoried Phase 13 launch artifact candidates for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyArtifactBlockers(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-readiness.json", reportPath(fixture, result, "phase13-m17-readiness.json"));
        requiredReports.put("local-runtime-artifact-inventory.json", reportPath(fixture, result, "local-runtime-artifact-inventory.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("launch-artifact-resolution-status.json", reportPath(fixture, result, "launch-artifact-resolution-status.json"));
        requiredReports.put("isolated-launch-execution-eligibility.json", reportPath(fixture, result, "isolated-launch-execution-eligibility.json"));
        requiredReports.put("local-runtime-artifact-check.json", reportPath(fixture, result, "local-runtime-artifact-check.json"));
        requiredReports.put("phase13-m17-launch-status.json", reportPath(fixture, result, "phase13-m17-launch-status.json"));
        EchoNativeArtifactBlockerOutcome outcome = artifactBlockerVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writeArtifactBlockerReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M17 artifact blockers for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13AuditArtifactPackaging(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-artifact-blockers.json", reportPath(fixture, result, "phase13-m17-artifact-blockers.json"));
        requiredReports.put("phase13-m17-blocker-resolution-plan.json", reportPath(fixture, result, "phase13-m17-blocker-resolution-plan.json"));
        requiredReports.put("local-runtime-artifact-inventory.json", reportPath(fixture, result, "local-runtime-artifact-inventory.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("phase13-m18-readiness.json", reportPath(fixture, result, "phase13-m18-readiness.json"));
        EchoNativeArtifactPackagingAuditOutcome outcome = artifactPackagingAuditor.audit(
                packId(result),
                fixture,
                requiredReports
        );
        writeArtifactPackagingAuditReports(fixture, result, outcome);
        System.out.println("Audited Phase 13 M17 artifact packaging for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyRuntimeFixtures(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-artifact-blockers.json", reportPath(fixture, result, "phase13-m17-artifact-blockers.json"));
        requiredReports.put("phase13-m17-artifact-packaging-audit.json", reportPath(fixture, result, "phase13-m17-artifact-packaging-audit.json"));
        requiredReports.put("phase13-m17-blocker-resolution-plan.json", reportPath(fixture, result, "phase13-m17-blocker-resolution-plan.json"));
        requiredReports.put("phase13-m17-artifact-packaging-resolution-plan.json", reportPath(fixture, result, "phase13-m17-artifact-packaging-resolution-plan.json"));
        EchoNativeRuntimeFixtureVerificationOutcome outcome = runtimeFixtureVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureVerificationReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M17 runtime fixtures for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanRuntimeFixtureIntake(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("runtime-fixture-presence.json", reportPath(fixture, result, "runtime-fixture-presence.json"));
        requiredReports.put("runtime-fixture-mapping-readiness.json", reportPath(fixture, result, "runtime-fixture-mapping-readiness.json"));
        EchoNativeRuntimeFixtureIntakeOutcome outcome = runtimeFixtureIntakePlanner.plan(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureIntakeReports(fixture, result, outcome);
        System.out.println("Planned Phase 13 M17 runtime fixture intake for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13AuditRuntimeFixtureApproval(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("runtime-fixture-intake-plan.json", reportPath(fixture, result, "runtime-fixture-intake-plan.json"));
        EchoNativeRuntimeFixtureApprovalAuditOutcome outcome = runtimeFixtureApprovalAuditor.audit(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureApprovalAuditReports(fixture, result, outcome);
        System.out.println("Audited Phase 13 M17 runtime fixture approvals for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrepareRuntimeFixtureHandoff(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("runtime-fixture-intake-plan.json", reportPath(fixture, result, "runtime-fixture-intake-plan.json"));
        requiredReports.put("runtime-fixture-approval-audit.json", reportPath(fixture, result, "runtime-fixture-approval-audit.json"));
        requiredReports.put("runtime-fixture-approval-template.json", reportPath(fixture, result, "runtime-fixture-approval-template.json"));
        requiredReports.put("phase13-m17-completion.json", reportPath(fixture, result, "phase13-m17-completion.json"));
        EchoNativeRuntimeFixtureHandoffOutcome outcome = runtimeFixtureHandoffPreparer.prepare(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureHandoffReports(fixture, result, outcome);
        System.out.println("Prepared Phase 13 M17 runtime fixture handoff for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13ExportRuntimeFixtureOperatorPacket(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("runtime-fixture-handoff.json", reportPath(fixture, result, "runtime-fixture-handoff.json"));
        requiredReports.put("runtime-fixture-validation-runbook.json", reportPath(fixture, result, "runtime-fixture-validation-runbook.json"));
        requiredReports.put("runtime-fixture-approval-draft.json", reportPath(fixture, result, "runtime-fixture-approval-draft.json"));
        requiredReports.put("runtime-fixture-hash-review.json", reportPath(fixture, result, "runtime-fixture-hash-review.json"));
        requiredReports.put("phase13-m17-completion.json", reportPath(fixture, result, "phase13-m17-completion.json"));
        requiredReports.put("phase13-first-playtest-full-roadmap.json", reportPath(fixture, result, "phase13-first-playtest-full-roadmap.json"));
        EchoNativeRuntimeFixtureOperatorPacketOutcome outcome = runtimeFixtureOperatorPacketExporter.export(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureOperatorPacketReports(fixture, result, outcome);
        System.out.println("Exported Phase 13 M17 runtime fixture operator packet for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13DraftRuntimeFixtureApproval(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("runtime-fixture-handoff.json", reportPath(fixture, result, "runtime-fixture-handoff.json"));
        EchoNativeRuntimeFixtureApprovalDraftOutcome outcome = runtimeFixtureApprovalDraftPlanner.plan(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureApprovalDraftReports(fixture, result, outcome);
        System.out.println("Drafted Phase 13 M17 runtime fixture approval evidence for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13AuditRuntimeFixtureIntegrity(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("runtime-fixture-handoff.json", reportPath(fixture, result, "runtime-fixture-handoff.json"));
        EchoNativeRuntimeFixtureIntegrityOutcome outcome = runtimeFixtureIntegrityAuditor.audit(
                packId(result),
                fixture,
                requiredReports
        );
        writeRuntimeFixtureIntegrityReports(fixture, result, outcome);
        System.out.println("Audited Phase 13 M17 runtime fixture integrity for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyM17(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-readiness.json", reportPath(fixture, result, "phase13-m17-readiness.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("phase13-m17-artifact-readiness.json", reportPath(fixture, result, "phase13-m17-artifact-readiness.json"));
        requiredReports.put("phase13-m17-launch-status.json", reportPath(fixture, result, "phase13-m17-launch-status.json"));
        requiredReports.put("phase13-m17-artifact-blockers.json", reportPath(fixture, result, "phase13-m17-artifact-blockers.json"));
        requiredReports.put("phase13-m17-artifact-packaging-audit.json", reportPath(fixture, result, "phase13-m17-artifact-packaging-audit.json"));
        requiredReports.put("runtime-fixture-presence.json", reportPath(fixture, result, "runtime-fixture-presence.json"));
        requiredReports.put("runtime-fixture-mapping-readiness.json", reportPath(fixture, result, "runtime-fixture-mapping-readiness.json"));
        requiredReports.put("runtime-fixture-approval-audit.json", reportPath(fixture, result, "runtime-fixture-approval-audit.json"));
        requiredReports.put("runtime-fixture-integrity-audit.json", reportPath(fixture, result, "runtime-fixture-integrity-audit.json"));
        requiredReports.put("phase13-m18-readiness.json", reportPath(fixture, result, "phase13-m18-readiness.json"));
        EchoNativePhase13M17CloseoutOutcome outcome = phase13M17CloseoutVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writePhase13M17CloseoutReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M17 closeout for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyM18(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-completion.json", reportPath(fixture, result, "phase13-m17-completion.json"));
        requiredReports.put("phase13-m18-readiness.json", reportPath(fixture, result, "phase13-m18-readiness.json"));
        requiredReports.put("phase13-m18-readiness-audit.json", reportPath(fixture, result, "phase13-m18-readiness-audit.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("isolated-launch-attempt-plan.json", reportPath(fixture, result, "isolated-launch-attempt-plan.json"));
        requiredReports.put("phase13-m17-launch-status.json", reportPath(fixture, result, "phase13-m17-launch-status.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("runtime-fixture-integrity-audit.json", reportPath(fixture, result, "runtime-fixture-integrity-audit.json"));
        EchoNativePhase13M18SmokeSessionOutcome outcome = phase13M18SmokeSessionVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writePhase13M18SmokeSessionReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M18 smoke session for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanFirstPlaytest(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        List<EchoNativeDiagnostic> validationDiagnostics = validator.validate(result);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-completion.json", reportPath(fixture, result, "phase13-m17-completion.json"));
        requiredReports.put("phase13-m18-readiness-audit.json", reportPath(fixture, result, "phase13-m18-readiness-audit.json"));
        requiredReports.put("phase13-m18-completion.json", reportPath(fixture, result, "phase13-m18-completion.json"));
        requiredReports.put("phase13-m19-readiness.json", reportPath(fixture, result, "phase13-m19-readiness.json"));
        requiredReports.put("phase13-first-playtest-blockers.json", reportPath(fixture, result, "phase13-first-playtest-blockers.json"));
        requiredReports.put("runtime-fixture-handoff.json", reportPath(fixture, result, "runtime-fixture-handoff.json"));
        requiredReports.put("runtime-fixture-integrity-audit.json", reportPath(fixture, result, "runtime-fixture-integrity-audit.json"));
        requiredReports.put("phase13-m19-completion.json", reportPath(fixture, result, "phase13-m19-completion.json"));
        requiredReports.put("first-playtest-open-gate.json", reportPath(fixture, result, "first-playtest-open-gate.json"));
        EchoNativeFirstPlaytestRoadmapOutcome outcome = firstPlaytestRoadmapPlanner.plan(
                packId(result),
                fixture,
                validationDiagnostics,
                requiredReports
        );
        writeFirstPlaytestRoadmapReports(fixture, result, outcome);
        System.out.println("Planned first-playtest roadmap for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PackageFirstPlaytest(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m18-completion.json", reportPath(fixture, result, "phase13-m18-completion.json"));
        requiredReports.put("phase13-m19-readiness.json", reportPath(fixture, result, "phase13-m19-readiness.json"));
        requiredReports.put("smoke-session-safety-gate.json", reportPath(fixture, result, "smoke-session-safety-gate.json"));
        requiredReports.put("smoke-session-result.json", reportPath(fixture, result, "smoke-session-result.json"));
        requiredReports.put("phase13-first-playtest-full-roadmap.json", reportPath(fixture, result, "phase13-first-playtest-full-roadmap.json"));
        requiredReports.put("support-bundle-manifest.json", rootEchoReportPath("support-bundle-manifest.json"));
        EchoNativeFirstPlaytestCandidateOutcome outcome = firstPlaytestCandidatePackager.packageCandidate(
                packId(result),
                fixture,
                requiredReports
        );
        writeFirstPlaytestCandidateReports(fixture, result, outcome);
        System.out.println("Packaged first-playtest candidate for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase14Preflight(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("first-playtest-open-gate.json", reportPath(fixture, result, "first-playtest-open-gate.json"));
        requiredReports.put("phase13-m19-completion.json", reportPath(fixture, result, "phase13-m19-completion.json"));
        requiredReports.put("first-playtest-candidate-package.json", reportPath(fixture, result, "first-playtest-candidate-package.json"));
        requiredReports.put("first-playtest-support-bundle.json", reportPath(fixture, result, "first-playtest-support-bundle.json"));
        requiredReports.put("first-playtest-rollback-notes.json", reportPath(fixture, result, "first-playtest-rollback-notes.json"));
        requiredReports.put("first-playtest-known-limitations.json", reportPath(fixture, result, "first-playtest-known-limitations.json"));
        requiredReports.put("first-playtest-crash-report-collection.json", reportPath(fixture, result, "first-playtest-crash-report-collection.json"));
        requiredReports.put("experimental-native-loader-label.json", reportPath(fixture, result, "experimental-native-loader-label.json"));
        requiredReports.put("phase13-first-playtest-full-roadmap.json", reportPath(fixture, result, "phase13-first-playtest-full-roadmap.json"));
        EchoNativePhase14PreflightOutcome outcome = phase14PreflightAuditor.audit(
                packId(result),
                fixture,
                requiredReports
        );
        writePhase14PreflightReports(fixture, result, outcome);
        System.out.println("Audited Phase 14 preflight for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13AuditLaunchReality(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m17-completion.json", reportPath(fixture, result, "phase13-m17-completion.json"));
        requiredReports.put("controlled-launch-attempt-result.json", reportPath(fixture, result, "controlled-launch-attempt-result.json"));
        requiredReports.put("phase13-m17-launch-status.json", reportPath(fixture, result, "phase13-m17-launch-status.json"));
        requiredReports.put("launch-output-capture.json", reportPath(fixture, result, "launch-output-capture.json"));
        requiredReports.put("phase13-m18-completion.json", reportPath(fixture, result, "phase13-m18-completion.json"));
        requiredReports.put("smoke-session-result.json", reportPath(fixture, result, "smoke-session-result.json"));
        requiredReports.put("phase13-m19-completion.json", reportPath(fixture, result, "phase13-m19-completion.json"));
        requiredReports.put("first-playtest-open-gate.json", reportPath(fixture, result, "first-playtest-open-gate.json"));
        EchoNativeLaunchRealityAuditOutcome outcome = launchRealityAuditor.audit(
                packId(result),
                fixture,
                requiredReports
        );
        writeLaunchRealityAuditReports(fixture, result, outcome);
        System.out.println("Audited native loader launch reality for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrepareIsolatedRuntime(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("native-loader-reality-audit.json", reportPath(fixture, result, "native-loader-reality-audit.json"));
        requiredReports.put("runtime-fixture-integrity-audit.json", reportPath(fixture, result, "runtime-fixture-integrity-audit.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("launch-argument-builder-plan.json", reportPath(fixture, result, "launch-argument-builder-plan.json"));
        requiredReports.put("native-extraction-plan.json", reportPath(fixture, result, "native-extraction-plan.json"));
        EchoNativeIsolatedRuntimeWorkspaceOutcome outcome = isolatedRuntimeWorkspacePreparer.prepare(
                packId(result),
                fixture,
                requiredReports
        );
        writeIsolatedRuntimeWorkspaceReports(fixture, result, outcome);
        System.out.println("Prepared isolated runtime workspace for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanRealProcessLaunch(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("native-loader-reality-audit.json", reportPath(fixture, result, "native-loader-reality-audit.json"));
        requiredReports.put("isolated-runtime-workspace-safety-status.json", reportPath(fixture, result, "isolated-runtime-workspace-safety-status.json"));
        requiredReports.put("isolated-runtime-workspace-materialization.json", reportPath(fixture, result, "isolated-runtime-workspace-materialization.json"));
        requiredReports.put("runtime-fixture-integrity-audit.json", reportPath(fixture, result, "runtime-fixture-integrity-audit.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("launch-argument-builder-plan.json", reportPath(fixture, result, "launch-argument-builder-plan.json"));
        requiredReports.put("classpath-builder-plan.json", reportPath(fixture, result, "classpath-builder-plan.json"));
        requiredReports.put("native-extraction-plan.json", reportPath(fixture, result, "native-extraction-plan.json"));
        EchoNativeRealProcessLaunchHarnessOutcome outcome = realProcessLaunchHarnessPlanner.plan(
                packId(result),
                fixture,
                requiredReports
        );
        writeRealProcessLaunchHarnessReports(fixture, result, outcome);
        System.out.println("Planned real-process launch harness for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyExecutionReadiness(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("real-process-launch-harness-plan.json", reportPath(fixture, result, "real-process-launch-harness-plan.json"));
        requiredReports.put("real-process-launch-safety-gate.json", reportPath(fixture, result, "real-process-launch-safety-gate.json"));
        requiredReports.put("real-process-command-line-preview.json", reportPath(fixture, result, "real-process-command-line-preview.json"));
        requiredReports.put("real-process-environment-plan.json", reportPath(fixture, result, "real-process-environment-plan.json"));
        requiredReports.put("isolated-runtime-workspace-safety-status.json", reportPath(fixture, result, "isolated-runtime-workspace-safety-status.json"));
        requiredReports.put("isolated-runtime-workspace-materialization.json", reportPath(fixture, result, "isolated-runtime-workspace-materialization.json"));
        requiredReports.put("runtime-fixture-integrity-audit.json", reportPath(fixture, result, "runtime-fixture-integrity-audit.json"));
        requiredReports.put("launch-safety-gate.json", reportPath(fixture, result, "launch-safety-gate.json"));
        requiredReports.put("first-playtest-support-bundle.json", reportPath(fixture, result, "first-playtest-support-bundle.json"));
        requiredReports.put("first-playtest-rollback-notes.json", reportPath(fixture, result, "first-playtest-rollback-notes.json"));
        requiredReports.put("first-playtest-known-limitations.json", reportPath(fixture, result, "first-playtest-known-limitations.json"));
        requiredReports.put("first-playtest-crash-report-collection.json", reportPath(fixture, result, "first-playtest-crash-report-collection.json"));
        requiredReports.put("experimental-native-loader-label.json", reportPath(fixture, result, "experimental-native-loader-label.json"));
        requiredReports.put("first-playtest-open-gate.json", reportPath(fixture, result, "first-playtest-open-gate.json"));
        EchoNativeExecutionReadinessOutcome outcome = executionReadinessVerifier.verify(
                packId(result),
                fixture,
                requiredReports,
                staticSafetyScanner.scan()
        );
        writeExecutionReadinessReports(fixture, result, outcome);
        System.out.println("Verified process execution readiness for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13LaunchControlledAuthorized(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-native-loader-beta-gate.json", reportPath(fixture, result, "phase13-native-loader-beta-gate.json"));
        requiredReports.put("process-execution-readiness.json", reportPath(fixture, result, "process-execution-readiness.json"));
        requiredReports.put("controlled-launch-operator-checklist.json", reportPath(fixture, result, "controlled-launch-operator-checklist.json"));
        requiredReports.put("controlled-launch-rollback-plan.json", reportPath(fixture, result, "controlled-launch-rollback-plan.json"));
        requiredReports.put("real-process-command-line-preview.json", reportPath(fixture, result, "real-process-command-line-preview.json"));
        requiredReports.put("real-process-environment-plan.json", reportPath(fixture, result, "real-process-environment-plan.json"));
        requiredReports.put("isolated-runtime-workspace-safety-status.json", reportPath(fixture, result, "isolated-runtime-workspace-safety-status.json"));
        EchoNativeControlledProcessLaunchOutcome outcome = controlledProcessLauncher.launch(
                packId(result),
                result.descriptors(),
                fixture,
                requiredReports
        );
        writeControlledProcessLaunchReports(fixture, result, outcome);
        System.out.println("Ran authorized controlled native launch boundary for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13LaunchTesterAuthorized(Path fixture) throws IOException {
        printInternalTesterLauncherBlocked();
        return 2;
    }

    private static void printInternalTesterLauncherBlocked() {
        System.out.println("phase13 launch tester was removed from the product CLI; it is not the release native client path.");
        System.out.println("Use startNativeClient or echo-native launch <product-root> for product/native client launch.");
        System.out.println("Internal tester process launch code lives in the QA source set.");
    }

    private int phase13IntakeTesterEvidence(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("tester-launch-process.json", reportPath(fixture, result, "tester-launch-process.json"));
        requiredReports.put("tester-launch-safety-gate.json", reportPath(fixture, result, "tester-launch-safety-gate.json"));
        requiredReports.put("native-product-module-activation-status.json", reportPath(fixture, result, "native-product-module-activation-status.json"));
        EchoNativeTesterEvidenceOutcome outcome = testerEvidenceIntake.intake(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeTesterEvidenceReports(fixture, result, outcome);
        System.out.println("Captured tester playable evidence for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13IntakeBetaFeedback(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m26-completion.json", reportPath(fixture, result, "phase13-m26-completion.json"));
        requiredReports.put("native-loader-playable-beta-readiness.json", reportPath(fixture, result, "native-loader-playable-beta-readiness.json"));
        requiredReports.put("native-product-loader-beta-status.json", reportPath(fixture, result, "native-product-loader-beta-status.json"));
        requiredReports.put("internal-tester-beta-gate.json", reportPath(fixture, result, "internal-tester-beta-gate.json"));
        requiredReports.put("tester-playable-evidence.json", reportPath(fixture, result, "tester-playable-evidence.json"));
        EchoNativeBetaFeedbackOutcome outcome = betaFeedbackIntake.intake(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeBetaFeedbackReports(fixture, result, outcome);
        System.out.println("Captured native loader beta feedback intake for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyM28(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m27-completion.json", reportPath(fixture, result, "phase13-m27-completion.json"));
        requiredReports.put("phase13-m28-readiness.json", reportPath(fixture, result, "phase13-m28-readiness.json"));
        requiredReports.put("native-loader-beta-feedback-inventory.json", reportPath(fixture, result, "native-loader-beta-feedback-inventory.json"));
        requiredReports.put("native-loader-beta-crash-intake.json", reportPath(fixture, result, "native-loader-beta-crash-intake.json"));
        requiredReports.put("native-loader-beta-next-action-queue.json", reportPath(fixture, result, "native-loader-beta-next-action-queue.json"));
        requiredReports.put("internal-tester-beta-gate.json", reportPath(fixture, result, "internal-tester-beta-gate.json"));
        EchoNativeBetaWideningOutcome outcome = betaWideningVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writeBetaWideningReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M28 internal beta widening for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13IntakeBetaSoak(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m28-completion.json", reportPath(fixture, result, "phase13-m28-completion.json"));
        requiredReports.put("phase13-m29-readiness.json", reportPath(fixture, result, "phase13-m29-readiness.json"));
        requiredReports.put("native-loader-beta-widening-plan.json", reportPath(fixture, result, "native-loader-beta-widening-plan.json"));
        requiredReports.put("native-loader-beta-widening-safety-gate.json", reportPath(fixture, result, "native-loader-beta-widening-safety-gate.json"));
        requiredReports.put("beta-tester-cohort-plan.json", reportPath(fixture, result, "beta-tester-cohort-plan.json"));
        requiredReports.put("native-loader-beta-feedback-inventory.json", reportPath(fixture, result, "native-loader-beta-feedback-inventory.json"));
        requiredReports.put("native-loader-beta-crash-intake.json", reportPath(fixture, result, "native-loader-beta-crash-intake.json"));
        requiredReports.put("native-loader-beta-known-issues.json", reportPath(fixture, result, "native-loader-beta-known-issues.json"));
        requiredReports.put("native-loader-beta-next-action-queue.json", reportPath(fixture, result, "native-loader-beta-next-action-queue.json"));
        EchoNativeBetaSoakOutcome outcome = betaSoakIntake.intake(
                packId(result),
                fixture,
                requiredReports
        );
        writeBetaSoakReports(fixture, result, outcome);
        System.out.println("Captured Phase 13 M29 native loader beta soak intake for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyM30(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m29-completion.json", reportPath(fixture, result, "phase13-m29-completion.json"));
        requiredReports.put("phase13-m30-readiness.json", reportPath(fixture, result, "phase13-m30-readiness.json"));
        requiredReports.put("native-loader-beta-soak-plan.json", reportPath(fixture, result, "native-loader-beta-soak-plan.json"));
        requiredReports.put("native-loader-beta-session-inventory.json", reportPath(fixture, result, "native-loader-beta-session-inventory.json"));
        requiredReports.put("native-loader-beta-issue-triage.json", reportPath(fixture, result, "native-loader-beta-issue-triage.json"));
        requiredReports.put("native-loader-beta-regression-watchlist.json", reportPath(fixture, result, "native-loader-beta-regression-watchlist.json"));
        requiredReports.put("native-loader-beta-widening-safety-gate.json", reportPath(fixture, result, "native-loader-beta-widening-safety-gate.json"));
        requiredReports.put("phase13-m29-note-validation-status.json", reportPath(fixture, result, "phase13-m29-note-validation-status.json"));
        EchoNativePublicBetaCandidateOutcome outcome = publicBetaCandidateVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writePublicBetaCandidateReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M30 public beta candidate gate for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyM31(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        writeScanReports(fixture, result);
        writeValidationReports(fixture, result, graphPlan.diagnostics());
        writeGraphReports(fixture, result, graphPlan);
        writeFeatureReport(fixture, result, graphPlan);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("validation.json", reportPath(fixture, result, "validation.json"));
        requiredReports.put("module-descriptors.json", reportPath(fixture, result, "module-descriptors.json"));
        requiredReports.put("module-load-plan.json", reportPath(fixture, result, "module-load-plan.json"));
        requiredReports.put("feature-graph.json", reportPath(fixture, result, "feature-graph.json"));
        requiredReports.put("phase13-m30-completion.json", reportPath(fixture, result, "phase13-m30-completion.json"));
        requiredReports.put("phase13-m31-readiness.json", reportPath(fixture, result, "phase13-m31-readiness.json"));
        requiredReports.put("native-loader-public-beta-candidate-audit.json", reportPath(fixture, result, "native-loader-public-beta-candidate-audit.json"));
        requiredReports.put("native-loader-public-beta-safety-gate.json", reportPath(fixture, result, "native-loader-public-beta-safety-gate.json"));
        requiredReports.put("public-beta-tester-readiness.json", reportPath(fixture, result, "public-beta-tester-readiness.json"));
        requiredReports.put("first-playtest-candidate-package.json", reportPath(fixture, result, "first-playtest-candidate-package.json"));
        requiredReports.put("first-playtest-support-bundle.json", reportPath(fixture, result, "first-playtest-support-bundle.json"));
        requiredReports.put("first-playtest-rollback-notes.json", reportPath(fixture, result, "first-playtest-rollback-notes.json"));
        requiredReports.put("first-playtest-known-limitations.json", reportPath(fixture, result, "first-playtest-known-limitations.json"));
        requiredReports.put("first-playtest-crash-report-collection.json", reportPath(fixture, result, "first-playtest-crash-report-collection.json"));
        requiredReports.put("first-playtest-open-gate.json", reportPath(fixture, result, "first-playtest-open-gate.json"));
        requiredReports.put("native-loader-beta-known-issues.json", reportPath(fixture, result, "native-loader-beta-known-issues.json"));
        requiredReports.put("native-loader-beta-crash-intake.json", reportPath(fixture, result, "native-loader-beta-crash-intake.json"));
        EchoNativePublicBetaOpeningOutcome outcome = publicBetaOpeningVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writePublicBetaOpeningReports(fixture, result, outcome);
        System.out.println("Verified Phase 13 M31 public beta opening gate for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13ExportBetaSoakPacket(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m29-completion.json", reportPath(fixture, result, "phase13-m29-completion.json"));
        requiredReports.put("phase13-m30-readiness.json", reportPath(fixture, result, "phase13-m30-readiness.json"));
        requiredReports.put("native-loader-beta-soak-plan.json", reportPath(fixture, result, "native-loader-beta-soak-plan.json"));
        requiredReports.put("native-loader-beta-session-inventory.json", reportPath(fixture, result, "native-loader-beta-session-inventory.json"));
        requiredReports.put("native-loader-beta-issue-triage.json", reportPath(fixture, result, "native-loader-beta-issue-triage.json"));
        requiredReports.put("native-loader-beta-regression-watchlist.json", reportPath(fixture, result, "native-loader-beta-regression-watchlist.json"));
        requiredReports.put("native-loader-public-beta-candidate-audit.json", reportPath(fixture, result, "native-loader-public-beta-candidate-audit.json"));
        requiredReports.put("phase13-m30-completion.json", reportPath(fixture, result, "phase13-m30-completion.json"));
        requiredReports.put("phase13-m29-note-validation-status.json", reportPath(fixture, result, "phase13-m29-note-validation-status.json"));
        EchoNativeBetaSoakOperatorPacketOutcome outcome = betaSoakOperatorPacketExporter.export(
                packId(result),
                fixture,
                requiredReports
        );
        writeBetaSoakOperatorPacketReports(fixture, result, outcome);
        System.out.println("Exported Phase 13 M29 beta soak operator packet for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrepareBetaSessionDrafts(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("native-loader-beta-session-note-drafts.json", reportPath(fixture, result, "native-loader-beta-session-note-drafts.json"));
        EchoNativeBetaSessionDraftOutcome outcome = betaSessionDraftPreparer.prepare(
                packId(result),
                fixture,
                requiredReports
        );
        writeBetaSessionDraftReports(fixture, result, outcome);
        System.out.println("Prepared Phase 13 M29 beta session draft files for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13ValidateBetaSessionNotes(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeBetaSessionNoteValidationOutcome outcome = betaSessionNoteValidator.validate(
                packId(result),
                fixture
        );
        writeBetaSessionNoteValidationReports(fixture, result, outcome);
        System.out.println("Validated Phase 13 M29 beta session notes for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13StatusBetaSoak(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m29-completion.json", reportPath(fixture, result, "phase13-m29-completion.json"));
        requiredReports.put("phase13-m30-readiness.json", reportPath(fixture, result, "phase13-m30-readiness.json"));
        requiredReports.put("native-loader-beta-session-inventory.json", reportPath(fixture, result, "native-loader-beta-session-inventory.json"));
        requiredReports.put("native-loader-beta-session-note-validation.json", reportPath(fixture, result, "native-loader-beta-session-note-validation.json"));
        requiredReports.put("phase13-m29-note-validation-status.json", reportPath(fixture, result, "phase13-m29-note-validation-status.json"));
        requiredReports.put("native-loader-beta-evidence-quality.json", reportPath(fixture, result, "native-loader-beta-evidence-quality.json"));
        EchoNativeBetaSoakStatusOutcome outcome = betaSoakStatusReporter.report(
                packId(result),
                fixture,
                requiredReports
        );
        writeBetaSoakStatusReports(fixture, result, outcome);
        System.out.println("Generated Phase 13 M29 beta soak status for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13AuditBetaSoakEvidence(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("native-loader-beta-feedback-inventory.json", reportPath(fixture, result, "native-loader-beta-feedback-inventory.json"));
        requiredReports.put("native-loader-beta-session-inventory.json", reportPath(fixture, result, "native-loader-beta-session-inventory.json"));
        requiredReports.put("tester-playable-evidence.json", reportPath(fixture, result, "tester-playable-evidence.json"));
        requiredReports.put("phase13-m29-completion.json", reportPath(fixture, result, "phase13-m29-completion.json"));
        EchoNativeBetaSoakEvidenceAuditOutcome outcome = betaSoakEvidenceAuditor.audit(
                packId(result),
                fixture,
                requiredReports
        );
        writeBetaSoakEvidenceAuditReports(fixture, result, outcome);
        System.out.println("Audited Phase 13 M29 beta soak evidence for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13BridgeModules(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m21-readiness.json", reportPath(fixture, result, "phase13-m21-readiness.json"));
        requiredReports.put("local-runtime-artifact-map.json", reportPath(fixture, result, "local-runtime-artifact-map.json"));
        requiredReports.put("minecraft-baseline-playability.json", reportPath(fixture, result, "minecraft-baseline-playability.json"));
        EchoNativeModuleRuntimeBridgeOutcome outcome = moduleRuntimeBridgeVerifier.verify(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeModuleRuntimeBridgeReports(fixture, result, outcome);
        System.out.println("Verified native module runtime bridge evidence for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanLiveActivation(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m21-readiness.json", reportPath(fixture, result, "phase13-m21-readiness.json"));
        requiredReports.put("minecraft-baseline-playability.json", reportPath(fixture, result, "minecraft-baseline-playability.json"));
        requiredReports.put("tester-launch-process.json", reportPath(fixture, result, "tester-launch-process.json"));
        requiredReports.put("native-module-bootstrap-status.json", reportPath(fixture, result, "native-module-bootstrap-status.json"));
        requiredReports.put("native-product-live-module-activation-status.json", reportPath(fixture, result, "native-product-live-module-activation-status.json"));
        requiredReports.put("native-product-playable-gate.json", reportPath(fixture, result, "native-product-playable-gate.json"));
        EchoNativeLiveActivationPlanOutcome outcome = liveActivationPlanner.plan(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeLiveActivationPlanReports(fixture, result, outcome);
        System.out.println("Planned native live activation bridge for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13ActivateBootstrapAuthorized(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m22-readiness.json", reportPath(fixture, result, "phase13-m22-readiness.json"));
        requiredReports.put("native-live-activation-wrapper-plan.json", reportPath(fixture, result, "native-live-activation-wrapper-plan.json"));
        requiredReports.put("native-live-activation-safety-gate.json", reportPath(fixture, result, "native-live-activation-safety-gate.json"));
        requiredReports.put("native-live-activation-marker-contract.json", reportPath(fixture, result, "native-live-activation-marker-contract.json"));
        requiredReports.put("native-module-bootstrap-status.json", reportPath(fixture, result, "native-module-bootstrap-status.json"));
        EchoNativeBootstrapActivationOutcome outcome = bootstrapActivator.activate(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeBootstrapActivationReports(fixture, result, outcome);
        writeProductModuleActivationStatusFromMarker(fixture, result, outcome.diagnostics());
        System.out.println("Activated native bootstrap marker for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyGameplayHooks(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m22-completion.json", reportPath(fixture, result, "phase13-m22-completion.json"));
        requiredReports.put("native-live-activation-marker.json", reportPath(fixture, result, "native-live-activation-marker.json"));
        requiredReports.put("native-live-activation-safety-status.json", reportPath(fixture, result, "native-live-activation-safety-status.json"));
        requiredReports.put("minecraft-baseline-playability.json", reportPath(fixture, result, "minecraft-baseline-playability.json"));
        requiredReports.put("adaptercore-runtime-gameplay-handler-evidence.json", reportPath(fixture, result, "adaptercore-runtime-gameplay-handler-evidence.json"));
        EchoNativeGameplayHookOutcome outcome = gameplayHookVerifier.verify(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeGameplayHookReports(fixture, result, outcome);
        System.out.println("Verified native gameplay hook evidence for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13BridgeGameplayHooks(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m23-completion.json", reportPath(fixture, result, "phase13-m23-completion.json"));
        requiredReports.put("phase13-m24-readiness.json", reportPath(fixture, result, "phase13-m24-readiness.json"));
        requiredReports.put("native-product-gameplay-hook-evidence.json", reportPath(fixture, result, "native-product-gameplay-hook-evidence.json"));
        requiredReports.put("native-module-gameplay-hook-status.json", reportPath(fixture, result, "native-module-gameplay-hook-status.json"));
        EchoNativeGameplayHookBridgeOutcome outcome = gameplayHookBridgePlanner.bridge(
                packId(result),
                fixture,
                result.descriptors(),
                requiredReports
        );
        writeGameplayHookBridgeReports(fixture, result, outcome);
        System.out.println("Bridged native gameplay hook signal contract for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13InstrumentGameplayHooksAuthorized(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m24-completion.json", reportPath(fixture, result, "phase13-m24-completion.json"));
        requiredReports.put("phase13-m25-readiness.json", reportPath(fixture, result, "phase13-m25-readiness.json"));
        requiredReports.put("gameplay-hook-signal-contract.json", reportPath(fixture, result, "gameplay-hook-signal-contract.json"));
        requiredReports.put("gameplay-hook-signal-status.json", reportPath(fixture, result, "gameplay-hook-signal-status.json"));
        EchoNativeGameplayHookInstrumentationOutcome outcome = gameplayHookInstrumentor.instrument(
                packId(result),
                fixture,
                requiredReports
        );
        writeGameplayHookInstrumentationReports(fixture, result, outcome);
        System.out.println("Instrumented native gameplay hook signals for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13VerifyPlayableBeta(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        Map<String, Path> requiredReports = new LinkedHashMap<>();
        requiredReports.put("phase13-m25-completion.json", reportPath(fixture, result, "phase13-m25-completion.json"));
        requiredReports.put("phase13-m26-readiness.json", reportPath(fixture, result, "phase13-m26-readiness.json"));
        requiredReports.put("gameplay-hook-signal-audit.json", reportPath(fixture, result, "gameplay-hook-signal-audit.json"));
        requiredReports.put("gameplay-hook-signal-status.json", reportPath(fixture, result, "gameplay-hook-signal-status.json"));
        requiredReports.put("native-product-playable-gate.json", reportPath(fixture, result, "native-product-playable-gate.json"));
        requiredReports.put("phase13-m19-completion.json", reportPath(fixture, result, "phase13-m19-completion.json"));
        requiredReports.put("first-playtest-open-gate.json", reportPath(fixture, result, "first-playtest-open-gate.json"));
        requiredReports.put("phase13-native-loader-beta-gate.json", reportPath(fixture, result, "phase13-native-loader-beta-gate.json"));
        requiredReports.put("first-playtest-support-bundle.json", reportPath(fixture, result, "first-playtest-support-bundle.json"));
        requiredReports.put("first-playtest-rollback-notes.json", reportPath(fixture, result, "first-playtest-rollback-notes.json"));
        requiredReports.put("first-playtest-known-limitations.json", reportPath(fixture, result, "first-playtest-known-limitations.json"));
        requiredReports.put("first-playtest-crash-report-collection.json", reportPath(fixture, result, "first-playtest-crash-report-collection.json"));
        EchoNativePlayableBetaOutcome outcome = playableBetaVerifier.verify(
                packId(result),
                fixture,
                requiredReports
        );
        writePlayableBetaReports(fixture, result, outcome);
        System.out.println("Verified Ashfall playable beta gate for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }


    private int phase13PlanMinecraftResolver(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeMinecraftResolverPlanningOutcome outcome = minecraftResolverPlanner.plan(
                packId(result),
                fixture,
                result.packProfile(),
                reportPath(fixture, result, "phase13-m2-readiness.json"),
                reportPath(fixture, result, "phase13-prototype-safety-gate.json")
        );
        writeMinecraftResolverPlanningReports(fixture, result, outcome);
        System.out.println("Planned Phase 13 Minecraft resolver for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanLibraryResolver(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeLibraryResolverPlanningOutcome outcome = libraryResolverPlanner.plan(
                packId(result),
                fixture,
                reportPath(fixture, result, "minecraft-version-resolver-plan.json"),
                reportPath(fixture, result, "minecraft-version-source-policy.json"),
                reportPath(fixture, result, "minecraft-resolver-safety-status.json")
        );
        writeLibraryResolverPlanningReports(fixture, result, outcome);
        System.out.println("Planned Phase 13 library resolver for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanClasspath(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeClasspathPlanningOutcome outcome = classpathPlanner.plan(
                packId(result),
                fixture,
                reportPath(fixture, result, "module-load-plan.json"),
                reportPath(fixture, result, "library-resolution-plan.json"),
                reportPath(fixture, result, "library-source-policy.json"),
                reportPath(fixture, result, "library-resolver-safety-status.json")
        );
        writeClasspathPlanningReports(fixture, result, outcome);
        System.out.println("Planned Phase 13 classpath builder for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanNativeExtraction(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeNativeExtractionPlanningOutcome outcome = nativeExtractionPlanner.plan(
                packId(result),
                fixture,
                reportPath(fixture, result, "native-library-plan.json"),
                reportPath(fixture, result, "library-resolution-plan.json"),
                reportPath(fixture, result, "library-source-policy.json"),
                reportPath(fixture, result, "library-resolver-safety-status.json"),
                reportPath(fixture, result, "classpath-builder-plan.json"),
                reportPath(fixture, result, "classpath-source-policy.json"),
                reportPath(fixture, result, "classpath-builder-safety-status.json")
        );
        writeNativeExtractionPlanningReports(fixture, result, outcome);
        System.out.println("Planned Phase 13 native extraction for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PlanLaunchArguments(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeLaunchArgumentPlanningOutcome outcome = launchArgumentPlanner.plan(
                packId(result),
                fixture,
                fixture.resolve("echo.pack.json"),
                reportPath(fixture, result, "minecraft-version-resolver-plan.json"),
                reportPath(fixture, result, "minecraft-version-source-policy.json"),
                reportPath(fixture, result, "minecraft-resolver-safety-status.json"),
                reportPath(fixture, result, "classpath-builder-plan.json"),
                reportPath(fixture, result, "classpath-source-policy.json"),
                reportPath(fixture, result, "classpath-builder-safety-status.json"),
                reportPath(fixture, result, "native-extraction-plan.json"),
                reportPath(fixture, result, "native-extraction-source-policy.json"),
                reportPath(fixture, result, "native-extraction-safety-status.json")
        );
        writeLaunchArgumentPlanningReports(fixture, result, outcome);
        System.out.println("Planned Phase 13 launch arguments for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13RunDummyProcess(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeDummyProcessOutcome outcome = controlledDummyProcessRunner.run(
                packId(result),
                fixture,
                reportPath(fixture, result, "launch-argument-builder-plan.json"),
                reportPath(fixture, result, "launch-argument-source-policy.json"),
                reportPath(fixture, result, "launch-argument-safety-status.json")
        );
        writeDummyProcessReports(fixture, result, outcome);
        System.out.println("Ran Phase 13 controlled dummy process boundary for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13DiscoverAddons(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeAddonRuntimeDiscoveryOutcome outcome = addonRuntimeDiscoveryPlanner.discover(
                packId(result),
                fixture,
                result.descriptors(),
                reportPath(fixture, result, "controlled-dummy-process-result.json"),
                reportPath(fixture, result, "dummy-process-crash-boundary.json"),
                reportPath(fixture, result, "dummy-process-output-capture.json")
        );
        writeAddonRuntimeDiscoveryReports(fixture, result, outcome);
        System.out.println("Discovered Phase 13 addon runtime descriptors for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13ExecuteLifecycleStubs(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeLifecycleStubExecutionOutcome outcome = lifecycleStubExecutor.execute(
                packId(result),
                fixture,
                reportPath(fixture, result, "addon-runtime-discovery-plan.json"),
                reportPath(fixture, result, "addon-runtime-descriptors.json"),
                reportPath(fixture, result, "addon-runtime-discovery-safety-status.json")
        );
        writeLifecycleStubExecutionReports(fixture, result, outcome);
        System.out.println("Executed Phase 13 lifecycle stubs for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrototypeServiceBus(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeServiceBusPrototypeOutcome outcome = serviceBusPrototype.prototype(
                packId(result),
                fixture,
                reportPath(fixture, result, "service-graph.json"),
                reportPath(fixture, result, "lifecycle-stub-execution-result.json"),
                reportPath(fixture, result, "lifecycle-stub-safety-status.json")
        );
        writeServiceBusPrototypeReports(fixture, result, outcome);
        System.out.println("Prototyped Phase 13 native service bus for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrototypeConfig(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeConfigPrototypeOutcome outcome = configPrototype.prototype(
                packId(result),
                fixture,
                reportPath(fixture, result, "service-bus-registry.json"),
                reportPath(fixture, result, "service-bus-simulation-result.json"),
                reportPath(fixture, result, "service-bus-safety-status.json")
        );
        writeConfigPrototypeReports(fixture, result, outcome);
        System.out.println("Prototyped Phase 13 native config system for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrototypeResources(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeResourcePrototypeOutcome outcome = resourcePrototype.prototype(
                packId(result),
                fixture,
                reportPath(fixture, result, "config-safety-status.json"),
                reportPath(fixture, result, "phase13-prototype-safety-gate.json")
        );
        writeResourcePrototypeReports(fixture, result, outcome);
        System.out.println("Prototyped Phase 13 native resource bridge for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrototypeRegistry(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeRegistryPrototypeOutcome outcome = registryPrototype.prototype(
                packId(result),
                fixture,
                reportPath(fixture, result, "resource-bridge-safety-status.json"),
                reportPath(fixture, result, "resource-conflict-report.json"),
                reportPath(fixture, result, "phase13-prototype-safety-gate.json")
        );
        writeRegistryPrototypeReports(fixture, result, outcome);
        System.out.println("Prototyped Phase 13 native registry bridge for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrototypeNetwork(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeNetworkPrototypeOutcome outcome = networkPrototype.prototype(
                packId(result),
                fixture,
                result.descriptors().stream().map(descriptor -> descriptor.id()).toList(),
                reportPath(fixture, result, "registry-bridge-safety-status.json"),
                reportPath(fixture, result, "registry-conflict-report.json"),
                reportPath(fixture, result, "phase13-prototype-safety-gate.json")
        );
        writeNetworkPrototypeReports(fixture, result, outcome);
        System.out.println("Prototyped Phase 13 native network bridge for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private int phase13PrototypeTransforms(Path fixture) throws IOException {
        EchoNativeScanResult result = scanner.scan(fixture);
        EchoNativeTransformPrototypeOutcome outcome = transformPrototype.prototype(
                packId(result),
                fixture,
                result.descriptors().stream().map(descriptor -> descriptor.id()).toList(),
                reportPath(fixture, result, "network-bridge-safety-status.json"),
                reportPath(fixture, result, "network-conflict-report.json"),
                reportPath(fixture, result, "phase13-prototype-safety-gate.json")
        );
        writeTransformPrototypeReports(fixture, result, outcome);
        System.out.println("Prototyped Phase 13 transform pipeline for " + packId(result) + ".");
        return hasBlocking(outcome.diagnostics()) ? 1 : 0;
    }

    private void writeScanReports(Path fixture, EchoNativeScanResult result) throws IOException {
        Map<String, Object> scanData = new LinkedHashMap<>();
        scanData.put("fixture", fixture.toString().replace('\\', '/'));
        scanData.put("descriptorCount", result.descriptors().size());
        scanData.put("descriptors", result.descriptors().stream().map(descriptor -> descriptor.id()).toList());
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, "scan.json"), "echo.native.scan.v1", "echo-native-cli", packId(result), status(result.diagnostics()), summary(result, result.diagnostics()), result.diagnostics(), scanData);

        Map<String, Object> descriptorData = new LinkedHashMap<>();
        descriptorData.put("modules", result.descriptors().stream().map(descriptor -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("apiStability", descriptor.apiStability().name());
            item.put("id", descriptor.id());
            item.put("kind", descriptor.kind());
            item.put("name", descriptor.name());
            item.put("path", descriptor.descriptorPath().toString().replace('\\', '/'));
            item.put("provides", descriptor.provides());
            item.put("requires", descriptor.requires());
            item.put("role", descriptor.role());
            item.put("side", descriptor.side().name());
            item.put("trustLevel", descriptor.trustLevel().name());
            item.put("version", descriptor.version());
            return item;
        }).toList());
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, "module-descriptors.json"), "echo.native.module_descriptors.v1", "echo-native-cli", packId(result), status(result.diagnostics()), summary(result, result.diagnostics()), result.diagnostics(), descriptorData);
    }

    private void writeValidationReports(Path fixture, EchoNativeScanResult result, List<EchoNativeDiagnostic> diagnostics) throws IOException {
        Map<String, Object> validationData = new LinkedHashMap<>();
        validationData.put("requiredModules", result.packProfile() == null ? List.of() : result.packProfile().requiredModules());
        validationData.put("requiredFeatures", result.packProfile() == null ? List.of() : result.packProfile().requiredFeatures());
        validationData.put("phase13Blocked", true);
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, "validation.json"), "echo.native.validation.v1", "echo-native-cli", packId(result), status(diagnostics), summary(result, diagnostics), diagnostics, validationData);
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, "diagnostics.json"), "echo.native.diagnostics.v1", "echo-native-cli", packId(result), status(diagnostics), summary(result, diagnostics), diagnostics, Map.of("diagnosticCount", diagnostics.size()));
    }

    private void writeGraphReports(Path fixture, EchoNativeScanResult result, EchoNativeGraphPlan graphPlan) throws IOException {
        writeSimple(fixture, result, "module-graph.json", "echo.native.module_graph.v1", graphPlan.diagnostics(), graphPlan.moduleGraph());
        writeSimple(fixture, result, "service-graph.json", "echo.native.service_graph.v1", graphPlan.diagnostics(), graphPlan.serviceGraph());
        writeSimple(fixture, result, "lifecycle-plan.json", "echo.native.lifecycle_plan.v1", graphPlan.diagnostics(), graphPlan.lifecyclePlan());
    }

    private void writeFeatureReport(Path fixture, EchoNativeScanResult result, EchoNativeGraphPlan graphPlan) throws IOException {
        writeSimple(fixture, result, "feature-graph.json", "echo.native.feature_graph.v1", graphPlan.diagnostics(), graphPlan.featureGraph());
    }

    private void writeBootstrapReports(Path fixture, EchoNativeScanResult result, EchoNativeBootstrapPlan plan) throws IOException {
        writeSimple(fixture, result, "minecraft-resolution.json", "echo.native.minecraft_resolution.v1", plan.diagnostics(), Map.of(
                "downloadAllowed", false,
                "minecraftVersion", plan.minecraftVersion(),
                "resolverMode", "dry_run"
        ));
        writeSimple(fixture, result, "classpath-plan.json", "echo.native.classpath_plan.v1", plan.diagnostics(), Map.of(
                "classloaderCreated", false,
                "entries", plan.classpathEntries()
        ));
        writeSimple(fixture, result, "native-library-plan.json", "echo.native.native_library_plan.v1", plan.diagnostics(), Map.of(
                "extractionAllowed", false,
                "entries", plan.nativeLibraryEntries()
        ));
        writeSimple(fixture, result, "launch-argument-plan.json", "echo.native.launch_argument_plan.v1", plan.diagnostics(), Map.of(
                "launchAllowed", false,
                "arguments", plan.launchArguments()
        ));
        writeSimple(fixture, result, "module-load-plan.json", "echo.native.module_load_plan.v1", plan.diagnostics(), Map.of(
                "moduleLoadOrder", plan.moduleLoadOrder(),
                "phase", "dry_run"
        ));
        writeSimple(fixture, result, "bootstrap-plan.json", "echo.native.bootstrap_plan.v1", plan.diagnostics(), Map.of(
                "accessPolicy", Map.of(
                        "blockedCapabilities", plan.accessPolicy().blockedCapabilities(),
                        "dryRunOnly", plan.accessPolicy().dryRunOnly(),
                        "launchBlocked", plan.accessPolicy().launchBlocked(),
                        "registryInjectionBlocked", plan.accessPolicy().registryInjectionBlocked(),
                        "transformsBlocked", plan.accessPolicy().transformsBlocked()
                ),
                "phase13Blocked", true,
                "summary", "Phase 12 produced a plan only; no Minecraft launch, classloader, transform, registry injection, download, or native extraction occurred."
        ));
    }

    private void writeLockfileReport(Path fixture, EchoNativeScanResult result, EchoNativeLockfilePlan plan) throws IOException {
        writeSimple(fixture, result, "lockfile.json", "echo.native.lockfile.v1", plan.diagnostics(), Map.of(
                "lockfile", plan.lockfile(),
                "phase13Blocked", true
        ));
    }

    private void writeLockfileStatusReport(Path fixture, EchoNativeScanResult result, EchoNativeLockfileVerificationPlan plan) throws IOException {
        writeSimple(fixture, result, "lockfile-status.json", "echo.native.lockfile_status.v1", plan.diagnostics(), plan.status());
    }

    private void writeRepairPlanReport(Path fixture, EchoNativeScanResult result, EchoNativeRepairPlan plan) throws IOException {
        writeSimple(fixture, result, "repair-plan.json", "echo.native.repair_plan.v1", plan.diagnostics(), plan.repairPlan());
    }

    private void writeAiReports(Path fixture, EchoNativeScanResult result, EchoNativeAiPlan plan) throws IOException {
        writeSimple(fixture, result, "ai-graph.json", "echo.native.ai_graph.v1", plan.diagnostics(), plan.aiGraph());
        writeSimple(fixture, result, "ai-tasks.json", "echo.native.ai_tasks.v1", plan.diagnostics(), plan.aiTasks());
    }

    private void writePhase12Reports(Path fixture, EchoNativeScanResult result, EchoNativePhase12GatePlan plan) throws IOException {
        writeSimple(fixture, result, "phase12-completion.json", "echo.native.phase12_completion.v1", plan.diagnostics(), plan.completion());
        writeSimple(fixture, result, "phase13-readiness.json", "echo.native.phase13_readiness.v1", plan.diagnostics(), plan.phase13Readiness());
    }

    private void writePhase13Reports(Path fixture, EchoNativeScanResult result, EchoNativePhase13PrototypePlan plan) throws IOException {
        writeSimple(fixture, result, "phase13-plan.json", "echo.native.phase13_plan.v1", plan.diagnostics(), plan.phase13Plan());
        writeSimple(fixture, result, "lifecycle-simulation-plan.json", "echo.native.lifecycle_simulation_plan.v1", plan.diagnostics(), plan.lifecycleSimulationPlan());
        writeSimple(fixture, result, "classloader-boundary-plan.json", "echo.native.classloader_boundary_plan.v1", plan.diagnostics(), plan.classloaderBoundaryPlan());
        writeSimple(fixture, result, "crash-boundary-plan.json", "echo.native.crash_boundary_plan.v1", plan.diagnostics(), plan.crashBoundaryPlan());
        writeSimple(fixture, result, "test-process-plan.json", "echo.native.test_process_plan.v1", plan.diagnostics(), plan.testProcessPlan());
    }

    private void writePhase13SimulationReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13LifecycleSimulationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "lifecycle-simulation-result.json", "echo.native.lifecycle_simulation_result.v1", outcome.diagnostics(), outcome.lifecycleSimulationResult());
        writeSimple(fixture, result, "crash-boundary-result.json", "echo.native.crash_boundary_result.v1", outcome.diagnostics(), outcome.crashBoundaryResult());
    }

    private void writePhase13ServiceSimulationReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13ServiceSimulationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "service-attach-simulation-result.json", "echo.native.service_attach_simulation_result.v1", outcome.diagnostics(), outcome.serviceAttachSimulationResult());
        writeSimple(fixture, result, "crash-boundary-verification.json", "echo.native.crash_boundary_verification.v1", outcome.diagnostics(), outcome.crashBoundaryVerification());
    }

    private void writePhase13CrashBoundarySimulationReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13CrashBoundarySimulationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "crash-boundary-simulation-result.json", "echo.native.crash_boundary_simulation_result.v1", outcome.diagnostics(), outcome.crashBoundarySimulationResult());
        writeSimple(fixture, result, "boundary-failure-cases.json", "echo.native.boundary_failure_cases.v1", outcome.diagnostics(), outcome.boundaryFailureCases());
        writeSimple(fixture, result, "classloader-boundary-rehearsal.json", "echo.native.classloader_boundary_rehearsal.v1", outcome.diagnostics(), outcome.classloaderBoundaryRehearsal());
    }

    private void writePhase13BoundaryVerificationReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13BoundaryVerificationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "loader-boundary-state-machine.json", "echo.native.loader_boundary_state_machine.v1", outcome.diagnostics(), outcome.loaderBoundaryStateMachine());
        writeSimple(fixture, result, "loader-boundary-verification.json", "echo.native.loader_boundary_verification.v1", outcome.diagnostics(), outcome.loaderBoundaryVerification());
        writeSimple(fixture, result, "classpath-classloader-compatibility.json", "echo.native.classpath_classloader_compatibility.v1", outcome.diagnostics(), outcome.classpathClassloaderCompatibility());
    }

    private void writePhase13TestProcessVerificationReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13TestProcessVerificationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "test-process-boundary-verification.json", "echo.native.test_process_boundary_verification.v1", outcome.diagnostics(), outcome.testProcessBoundaryVerification());
        writeSimple(fixture, result, "controlled-test-process-preflight.json", "echo.native.controlled_test_process_preflight.v1", outcome.diagnostics(), outcome.controlledTestProcessPreflight());
        writeSimple(fixture, result, "phase13-m1-safety-status.json", "echo.native.phase13_m1_safety_status.v1", outcome.diagnostics(), outcome.phase13M1SafetyStatus());
    }

    private void writePhase13BridgeRehearsalReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13BridgeRehearsalOutcome outcome) throws IOException {
        writeSimple(fixture, result, "resource-bridge-policy-rehearsal.json", "echo.native.resource_bridge_policy_rehearsal.v1", outcome.diagnostics(), outcome.resourceBridgePolicyRehearsal());
        writeSimple(fixture, result, "registry-bridge-policy-rehearsal.json", "echo.native.registry_bridge_policy_rehearsal.v1", outcome.diagnostics(), outcome.registryBridgePolicyRehearsal());
        writeSimple(fixture, result, "phase13-bridge-safety-status.json", "echo.native.phase13_bridge_safety_status.v1", outcome.diagnostics(), outcome.phase13BridgeSafetyStatus());
    }

    private void writePhase13M1CloseoutReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13M1CloseoutOutcome outcome) throws IOException {
        writeSimple(fixture, result, "phase13-m1-completion.json", "echo.native.phase13_m1_completion.v1", outcome.diagnostics(), outcome.phase13M1Completion());
        writeSimple(fixture, result, "phase13-m2-readiness.json", "echo.native.phase13_m2_readiness.v1", outcome.diagnostics(), outcome.phase13M2Readiness());
        writeSimple(fixture, result, "phase13-prototype-safety-gate.json", "echo.native.phase13_prototype_safety_gate.v1", outcome.diagnostics(), outcome.phase13PrototypeSafetyGate());
    }

    private void writeMinecraftResolverPlanningReports(Path fixture, EchoNativeScanResult result, EchoNativeMinecraftResolverPlanningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "minecraft-version-resolver-plan.json", "echo.native.minecraft_version_resolver_plan.v1", outcome.diagnostics(), outcome.minecraftVersionResolverPlan());
        writeSimple(fixture, result, "minecraft-version-source-policy.json", "echo.native.minecraft_version_source_policy.v1", outcome.diagnostics(), outcome.minecraftVersionSourcePolicy());
        writeSimple(fixture, result, "minecraft-resolver-safety-status.json", "echo.native.minecraft_resolver_safety_status.v1", outcome.diagnostics(), outcome.minecraftResolverSafetyStatus());
    }

    private void writeLibraryResolverPlanningReports(Path fixture, EchoNativeScanResult result, EchoNativeLibraryResolverPlanningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "library-resolution-plan.json", "echo.native.library_resolution_plan.v1", outcome.diagnostics(), outcome.libraryResolutionPlan());
        writeSimple(fixture, result, "library-source-policy.json", "echo.native.library_source_policy.v1", outcome.diagnostics(), outcome.librarySourcePolicy());
        writeSimple(fixture, result, "library-resolver-safety-status.json", "echo.native.library_resolver_safety_status.v1", outcome.diagnostics(), outcome.libraryResolverSafetyStatus());
    }

    private void writeClasspathPlanningReports(Path fixture, EchoNativeScanResult result, EchoNativeClasspathPlanningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "classpath-builder-plan.json", "echo.native.classpath_builder_plan.v1", outcome.diagnostics(), outcome.classpathBuilderPlan());
        writeSimple(fixture, result, "classpath-source-policy.json", "echo.native.classpath_source_policy.v1", outcome.diagnostics(), outcome.classpathSourcePolicy());
        writeSimple(fixture, result, "classpath-builder-safety-status.json", "echo.native.classpath_builder_safety_status.v1", outcome.diagnostics(), outcome.classpathBuilderSafetyStatus());
    }

    private void writeNativeExtractionPlanningReports(Path fixture, EchoNativeScanResult result, EchoNativeNativeExtractionPlanningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-extraction-plan.json", "echo.native.native_extraction_plan.v1", outcome.diagnostics(), outcome.nativeExtractionPlan());
        writeSimple(fixture, result, "native-extraction-source-policy.json", "echo.native.native_extraction_source_policy.v1", outcome.diagnostics(), outcome.nativeExtractionSourcePolicy());
        writeSimple(fixture, result, "native-extraction-safety-status.json", "echo.native.native_extraction_safety_status.v1", outcome.diagnostics(), outcome.nativeExtractionSafetyStatus());
    }

    private void writeLaunchArgumentPlanningReports(Path fixture, EchoNativeScanResult result, EchoNativeLaunchArgumentPlanningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "launch-argument-builder-plan.json", "echo.native.launch_argument_builder_plan.v1", outcome.diagnostics(), outcome.launchArgumentPlan());
        writeSimple(fixture, result, "launch-argument-source-policy.json", "echo.native.launch_argument_source_policy.v1", outcome.diagnostics(), outcome.launchArgumentSourcePolicy());
        writeSimple(fixture, result, "launch-argument-safety-status.json", "echo.native.launch_argument_safety_status.v1", outcome.diagnostics(), outcome.launchArgumentSafetyStatus());
    }

    private void writeDummyProcessReports(Path fixture, EchoNativeScanResult result, EchoNativeDummyProcessOutcome outcome) throws IOException {
        writeSimple(fixture, result, "controlled-dummy-process-plan.json", "echo.native.controlled_dummy_process_plan.v1", outcome.diagnostics(), outcome.controlledDummyProcessPlan());
        writeSimple(fixture, result, "controlled-dummy-process-result.json", "echo.native.controlled_dummy_process_result.v1", outcome.diagnostics(), outcome.controlledDummyProcessResult());
        writeSimple(fixture, result, "dummy-process-crash-boundary.json", "echo.native.dummy_process_crash_boundary.v1", outcome.diagnostics(), outcome.dummyProcessCrashBoundary());
        writeSimple(fixture, result, "dummy-process-output-capture.json", "echo.native.dummy_process_output_capture.v1", outcome.diagnostics(), outcome.dummyProcessOutputCapture());
    }

    private void writeAddonRuntimeDiscoveryReports(Path fixture, EchoNativeScanResult result, EchoNativeAddonRuntimeDiscoveryOutcome outcome) throws IOException {
        writeSimple(fixture, result, "addon-runtime-discovery-plan.json", "echo.native.addon_runtime_discovery_plan.v1", outcome.diagnostics(), outcome.addonRuntimeDiscoveryPlan());
        writeSimple(fixture, result, "addon-runtime-descriptors.json", "echo.native.addon_runtime_descriptors.v1", outcome.diagnostics(), outcome.addonRuntimeDescriptors());
        writeSimple(fixture, result, "addon-runtime-discovery-safety-status.json", "echo.native.addon_runtime_discovery_safety_status.v1", outcome.diagnostics(), outcome.addonRuntimeDiscoverySafetyStatus());
    }

    private void writeLifecycleStubExecutionReports(Path fixture, EchoNativeScanResult result, EchoNativeLifecycleStubExecutionOutcome outcome) throws IOException {
        writeSimple(fixture, result, "lifecycle-stub-execution-plan.json", "echo.native.lifecycle_stub_execution_plan.v1", outcome.diagnostics(), outcome.lifecycleStubExecutionPlan());
        writeSimple(fixture, result, "lifecycle-stub-execution-result.json", "echo.native.lifecycle_stub_execution_result.v1", outcome.diagnostics(), outcome.lifecycleStubExecutionResult());
        writeSimple(fixture, result, "lifecycle-stub-crash-boundary.json", "echo.native.lifecycle_stub_crash_boundary.v1", outcome.diagnostics(), outcome.lifecycleStubCrashBoundary());
        writeSimple(fixture, result, "lifecycle-stub-safety-status.json", "echo.native.lifecycle_stub_safety_status.v1", outcome.diagnostics(), outcome.lifecycleStubSafetyStatus());
    }

    private void writeServiceBusPrototypeReports(Path fixture, EchoNativeScanResult result, EchoNativeServiceBusPrototypeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "service-bus-plan.json", "echo.native.service_bus_plan.v1", outcome.diagnostics(), outcome.serviceBusPlan());
        writeSimple(fixture, result, "service-bus-registry.json", "echo.native.service_bus_registry.v1", outcome.diagnostics(), outcome.serviceBusRegistry());
        writeSimple(fixture, result, "service-bus-simulation-result.json", "echo.native.service_bus_simulation_result.v1", outcome.diagnostics(), outcome.serviceBusSimulationResult());
        writeSimple(fixture, result, "service-bus-safety-status.json", "echo.native.service_bus_safety_status.v1", outcome.diagnostics(), outcome.serviceBusSafetyStatus());
    }

    private void writeConfigPrototypeReports(Path fixture, EchoNativeScanResult result, EchoNativeConfigPrototypeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "config-source-inventory.json", "echo.native.config_source_inventory.v1", outcome.diagnostics(), outcome.configSourceInventory());
        writeSimple(fixture, result, "config-validation-result.json", "echo.native.config_validation_result.v1", outcome.diagnostics(), outcome.configValidationResult());
        writeSimple(fixture, result, "config-write-plan.json", "echo.native.config_write_plan.v1", outcome.diagnostics(), outcome.configWritePlan());
        writeSimple(fixture, result, "config-safety-status.json", "echo.native.config_safety_status.v1", outcome.diagnostics(), outcome.configSafetyStatus());
    }

    private void writeResourcePrototypeReports(Path fixture, EchoNativeScanResult result, EchoNativeResourcePrototypeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "resource-source-inventory.json", "echo.native.resource_source_inventory.v1", outcome.diagnostics(), outcome.resourceSourceInventory());
        writeSimple(fixture, result, "resource-namespace-validation.json", "echo.native.resource_namespace_validation.v1", outcome.diagnostics(), outcome.resourceNamespaceValidation());
        writeSimple(fixture, result, "resource-pack-order-plan.json", "echo.native.resource_pack_order_plan.v1", outcome.diagnostics(), outcome.resourcePackOrderPlan());
        writeSimple(fixture, result, "resource-conflict-report.json", "echo.native.resource_conflict_report.v1", outcome.diagnostics(), outcome.resourceConflictReport());
        writeSimple(fixture, result, "resource-bridge-safety-status.json", "echo.native.resource_bridge_safety_status.v1", outcome.diagnostics(), outcome.resourceBridgeSafetyStatus());
    }

    private void writeRegistryPrototypeReports(Path fixture, EchoNativeScanResult result, EchoNativeRegistryPrototypeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "registry-source-inventory.json", "echo.native.registry_source_inventory.v1", outcome.diagnostics(), outcome.registrySourceInventory());
        writeSimple(fixture, result, "registry-id-validation.json", "echo.native.registry_id_validation.v1", outcome.diagnostics(), outcome.registryIdValidation());
        writeSimple(fixture, result, "sandbox-registry-model.json", "echo.native.sandbox_registry_model.v1", outcome.diagnostics(), outcome.sandboxRegistryModel());
        writeSimple(fixture, result, "registry-conflict-report.json", "echo.native.registry_conflict_report.v1", outcome.diagnostics(), outcome.registryConflictReport());
        writeSimple(fixture, result, "registry-bridge-safety-status.json", "echo.native.registry_bridge_safety_status.v1", outcome.diagnostics(), outcome.registryBridgeSafetyStatus());
    }

    private void writeNetworkPrototypeReports(Path fixture, EchoNativeScanResult result, EchoNativeNetworkPrototypeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "network-channel-inventory.json", "echo.native.network_channel_inventory.v1", outcome.diagnostics(), outcome.networkChannelInventory());
        writeSimple(fixture, result, "network-packet-validation.json", "echo.native.network_packet_validation.v1", outcome.diagnostics(), outcome.networkPacketValidation());
        writeSimple(fixture, result, "network-schema-model.json", "echo.native.network_schema_model.v1", outcome.diagnostics(), outcome.networkSchemaModel());
        writeSimple(fixture, result, "network-conflict-report.json", "echo.native.network_conflict_report.v1", outcome.diagnostics(), outcome.networkConflictReport());
        writeSimple(fixture, result, "network-bridge-safety-status.json", "echo.native.network_bridge_safety_status.v1", outcome.diagnostics(), outcome.networkBridgeSafetyStatus());
    }

    private void writeTransformPrototypeReports(Path fixture, EchoNativeScanResult result, EchoNativeTransformPrototypeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "transform-source-inventory.json", "echo.native.transform_source_inventory.v1", outcome.diagnostics(), outcome.transformSourceInventory());
        writeSimple(fixture, result, "transform-allowlist-validation.json", "echo.native.transform_allowlist_validation.v1", outcome.diagnostics(), outcome.transformAllowlistValidation());
        writeSimple(fixture, result, "transform-pipeline-plan.json", "echo.native.transform_pipeline_plan.v1", outcome.diagnostics(), outcome.transformPipelinePlan());
        writeSimple(fixture, result, "transform-conflict-report.json", "echo.native.transform_conflict_report.v1", outcome.diagnostics(), outcome.transformConflictReport());
        writeSimple(fixture, result, "transform-safety-status.json", "echo.native.transform_safety_status.v1", outcome.diagnostics(), outcome.transformSafetyStatus());
    }

    private void writeCrashHardeningReports(Path fixture, EchoNativeScanResult result, EchoNativeCrashHardeningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "crash-hardening-coverage.json", "echo.native.crash_hardening_coverage.v1", outcome.diagnostics(), outcome.crashHardeningCoverage());
        writeSimple(fixture, result, "failure-containment-matrix.json", "echo.native.failure_containment_matrix.v1", outcome.diagnostics(), outcome.failureContainmentMatrix());
        writeSimple(fixture, result, "support-bundle-dry-run-plan.json", "echo.native.support_bundle_dry_run_plan.v1", outcome.diagnostics(), outcome.supportBundleDryRunPlan());
        writeSimple(fixture, result, "phase13-m16-safety-status.json", "echo.native.phase13_m16_safety_status.v1", outcome.diagnostics(), outcome.phase13M16SafetyStatus());
    }

    private void writeLaunchPreflightReports(Path fixture, EchoNativeScanResult result, EchoNativeLaunchPreflightOutcome outcome) throws IOException {
        writeSimple(fixture, result, "isolated-launch-environment-plan.json", "echo.native.isolated_launch_environment_plan.v1", outcome.diagnostics(), outcome.isolatedLaunchEnvironmentPlan());
        writeSimple(fixture, result, "minecraft-launch-preflight.json", "echo.native.minecraft_launch_preflight.v1", outcome.diagnostics(), outcome.minecraftLaunchPreflight());
        writeSimple(fixture, result, "launch-safety-gate.json", "echo.native.launch_safety_gate.v1", outcome.diagnostics(), outcome.launchSafetyGate());
        writeSimple(fixture, result, "controlled-launch-failure-capture-plan.json", "echo.native.controlled_launch_failure_capture_plan.v1", outcome.diagnostics(), outcome.controlledLaunchFailureCapturePlan());
        writeSimple(fixture, result, "phase13-m17-readiness.json", "echo.native.phase13_m17_readiness.v1", outcome.diagnostics(), outcome.phase13M17Readiness());
    }

    private void writeIsolatedLaunchAttemptReports(Path fixture, EchoNativeScanResult result, EchoNativeIsolatedLaunchAttemptOutcome outcome) throws IOException {
        writeSimple(fixture, result, "isolated-launch-attempt-plan.json", "echo.native.isolated_launch_attempt_plan.v1", outcome.diagnostics(), outcome.isolatedLaunchAttemptPlan());
        writeSimple(fixture, result, "local-runtime-artifact-check.json", "echo.native.local_runtime_artifact_check.v1", outcome.diagnostics(), outcome.localRuntimeArtifactCheck());
        writeSimple(fixture, result, "controlled-launch-attempt-result.json", "echo.native.controlled_launch_attempt_result.v1", outcome.diagnostics(), outcome.controlledLaunchAttemptResult());
        writeSimple(fixture, result, "launch-output-capture.json", "echo.native.launch_output_capture.v1", outcome.diagnostics(), outcome.launchOutputCapture());
        writeSimple(fixture, result, "phase13-m17-launch-status.json", "echo.native.phase13_m17_launch_status.v1", outcome.diagnostics(), outcome.phase13M17LaunchStatus());
    }

    private void writeArtifactMappingReports(Path fixture, EchoNativeScanResult result, EchoNativeArtifactMappingOutcome outcome) throws IOException {
        writeSimple(fixture, result, "local-runtime-artifact-map.json", "echo.native.local_runtime_artifact_map.v1", outcome.diagnostics(), outcome.localRuntimeArtifactMap());
        writeSimple(fixture, result, "launch-artifact-resolution-status.json", "echo.native.launch_artifact_resolution_status.v1", outcome.diagnostics(), outcome.launchArtifactResolutionStatus());
        writeSimple(fixture, result, "isolated-launch-execution-eligibility.json", "echo.native.isolated_launch_execution_eligibility.v1", outcome.diagnostics(), outcome.isolatedLaunchExecutionEligibility());
        writeSimple(fixture, result, "phase13-m17-artifact-readiness.json", "echo.native.phase13_m17_artifact_readiness.v1", outcome.diagnostics(), outcome.phase13M17ArtifactReadiness());
    }

    private void writeArtifactInventoryReports(Path fixture, EchoNativeScanResult result, EchoNativeArtifactInventoryOutcome outcome) throws IOException {
        writeSimple(fixture, result, "local-runtime-artifact-inventory.json", "echo.native.local_runtime_artifact_inventory.v1", outcome.diagnostics(), outcome.localRuntimeArtifactInventory());
    }

    private void writeArtifactBlockerReports(Path fixture, EchoNativeScanResult result, EchoNativeArtifactBlockerOutcome outcome) throws IOException {
        writeSimple(fixture, result, "phase13-m17-artifact-blockers.json", "echo.native.phase13_m17_artifact_blockers.v1", outcome.diagnostics(), outcome.phase13M17ArtifactBlockers());
        writeSimple(fixture, result, "phase13-m17-blocker-resolution-plan.json", "echo.native.phase13_m17_blocker_resolution_plan.v1", outcome.diagnostics(), outcome.phase13M17BlockerResolutionPlan());
        writeSimple(fixture, result, "phase13-m18-readiness.json", "echo.native.phase13_m18_readiness.v1", outcome.diagnostics(), outcome.phase13M18Readiness());
    }

    private void writeArtifactPackagingAuditReports(Path fixture, EchoNativeScanResult result, EchoNativeArtifactPackagingAuditOutcome outcome) throws IOException {
        writeSimple(fixture, result, "phase13-m17-artifact-packaging-audit.json", "echo.native.phase13_m17_artifact_packaging_audit.v1", outcome.diagnostics(), outcome.phase13M17ArtifactPackagingAudit());
        writeSimple(fixture, result, "phase13-m17-artifact-packaging-resolution-plan.json", "echo.native.phase13_m17_artifact_packaging_resolution_plan.v1", outcome.diagnostics(), outcome.phase13M17ArtifactPackagingResolutionPlan());
    }

    private void writeRuntimeFixtureVerificationReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureVerificationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-presence.json", "echo.native.runtime_fixture_presence.v1", outcome.diagnostics(), outcome.runtimeFixturePresence());
        writeSimple(fixture, result, "runtime-fixture-mapping-readiness.json", "echo.native.runtime_fixture_mapping_readiness.v1", outcome.diagnostics(), outcome.runtimeFixtureMappingReadiness());
    }

    private void writeRuntimeFixtureIntakeReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureIntakeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-intake-plan.json", "echo.native.runtime_fixture_intake_plan.v1", outcome.diagnostics(), outcome.runtimeFixtureIntakePlan());
        writeSimple(fixture, result, "runtime-fixture-intake-checklist.json", "echo.native.runtime_fixture_intake_checklist.v1", outcome.diagnostics(), outcome.runtimeFixtureIntakeChecklist());
    }

    private void writeRuntimeFixtureApprovalAuditReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureApprovalAuditOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-approval-audit.json", "echo.native.runtime_fixture_approval_audit.v1", outcome.diagnostics(), outcome.runtimeFixtureApprovalAudit());
        writeSimple(fixture, result, "runtime-fixture-approval-template.json", "echo.native.runtime_fixture_approval_template.v1", outcome.diagnostics(), outcome.runtimeFixtureApprovalTemplate());
    }

    private void writeRuntimeFixtureHandoffReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureHandoffOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-handoff.json", "echo.native.runtime_fixture_handoff.v1", outcome.diagnostics(), outcome.runtimeFixtureHandoff());
        writeSimple(fixture, result, "runtime-fixture-validation-runbook.json", "echo.native.runtime_fixture_validation_runbook.v1", outcome.diagnostics(), outcome.runtimeFixtureValidationRunbook());
    }

    private void writeRuntimeFixtureOperatorPacketReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureOperatorPacketOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-operator-packet.json", "echo.native.runtime_fixture_operator_packet.v1", outcome.diagnostics(), outcome.runtimeFixtureOperatorPacket());
    }

    private void writeRuntimeFixtureApprovalDraftReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureApprovalDraftOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-approval-draft.json", "echo.native.runtime_fixture_approval_draft.v1", outcome.diagnostics(), outcome.runtimeFixtureApprovalDraft());
        writeSimple(fixture, result, "runtime-fixture-hash-review.json", "echo.native.runtime_fixture_hash_review.v1", outcome.diagnostics(), outcome.runtimeFixtureHashReview());
    }

    private void writeRuntimeFixtureIntegrityReports(Path fixture, EchoNativeScanResult result, EchoNativeRuntimeFixtureIntegrityOutcome outcome) throws IOException {
        writeSimple(fixture, result, "runtime-fixture-integrity-audit.json", "echo.native.runtime_fixture_integrity_audit.v1", outcome.diagnostics(), outcome.runtimeFixtureIntegrityAudit());
        writeSimple(fixture, result, "runtime-fixture-integrity-manifest.json", "echo.native.runtime_fixture_integrity_manifest.v1", outcome.diagnostics(), outcome.runtimeFixtureIntegrityManifest());
    }

    private void writePhase13M17CloseoutReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13M17CloseoutOutcome outcome) throws IOException {
        writeSimple(fixture, result, "phase13-m17-completion.json", "echo.native.phase13_m17_completion.v1", outcome.diagnostics(), outcome.phase13M17Completion());
        writeSimple(fixture, result, "phase13-m18-readiness-audit.json", "echo.native.phase13_m18_readiness_audit.v1", outcome.diagnostics(), outcome.phase13M18ReadinessAudit());
        writeSimple(fixture, result, "phase13-first-playtest-blockers.json", "echo.native.phase13_first_playtest_blockers.v1", outcome.diagnostics(), outcome.phase13FirstPlaytestBlockers());
    }

    private void writePhase13M18SmokeSessionReports(Path fixture, EchoNativeScanResult result, EchoNativePhase13M18SmokeSessionOutcome outcome) throws IOException {
        writeSimple(fixture, result, "smoke-session-plan.json", "echo.native.smoke_session_plan.v1", outcome.diagnostics(), outcome.smokeSessionPlan());
        writeSimple(fixture, result, "smoke-session-safety-gate.json", "echo.native.smoke_session_safety_gate.v1", outcome.diagnostics(), outcome.smokeSessionSafetyGate());
        writeSimple(fixture, result, "smoke-session-result.json", "echo.native.smoke_session_result.v1", outcome.diagnostics(), outcome.smokeSessionResult());
        writeSimple(fixture, result, "smoke-session-diagnostics.json", "echo.native.smoke_session_diagnostics.v1", outcome.diagnostics(), outcome.smokeSessionDiagnostics());
        writeSimple(fixture, result, "phase13-m18-completion.json", "echo.native.phase13_m18_completion.v1", outcome.diagnostics(), outcome.phase13M18Completion());
        writeSimple(fixture, result, "phase13-m19-readiness.json", "echo.native.phase13_m19_readiness.v1", outcome.diagnostics(), outcome.phase13M19Readiness());
    }

    private void writeFirstPlaytestRoadmapReports(Path fixture, EchoNativeScanResult result, EchoNativeFirstPlaytestRoadmapOutcome outcome) throws IOException {
        writeSimple(fixture, result, "phase13-first-playtest-roadmap.json", "echo.native.phase13_first_playtest_roadmap.v1", outcome.diagnostics(), outcome.phase13FirstPlaytestRoadmap());
        writeSimple(fixture, result, "phase13-first-playtest-next-actions.json", "echo.native.phase13_first_playtest_next_actions.v1", outcome.diagnostics(), outcome.phase13FirstPlaytestNextActions());
        writeSimple(fixture, result, "phase13-first-playtest-full-roadmap.json", "echo.native.phase13_first_playtest_full_roadmap.v1", outcome.diagnostics(), outcome.phase13FirstPlaytestFullRoadmap());
    }

    private void writeFirstPlaytestCandidateReports(Path fixture, EchoNativeScanResult result, EchoNativeFirstPlaytestCandidateOutcome outcome) throws IOException {
        writeSimple(fixture, result, "first-playtest-candidate-package.json", "echo.native.first_playtest_candidate_package.v1", outcome.diagnostics(), outcome.firstPlaytestCandidatePackage());
        writeSimple(fixture, result, "first-playtest-support-bundle.json", "echo.native.first_playtest_support_bundle.v1", outcome.diagnostics(), outcome.firstPlaytestSupportBundle());
        writeSimple(fixture, result, "first-playtest-rollback-notes.json", "echo.native.first_playtest_rollback_notes.v1", outcome.diagnostics(), outcome.firstPlaytestRollbackNotes());
        writeSimple(fixture, result, "first-playtest-known-limitations.json", "echo.native.first_playtest_known_limitations.v1", outcome.diagnostics(), outcome.firstPlaytestKnownLimitations());
        writeSimple(fixture, result, "experimental-native-loader-label.json", "echo.native.experimental_native_loader_label.v1", outcome.diagnostics(), outcome.experimentalNativeLoaderLabel());
        writeSimple(fixture, result, "first-playtest-crash-report-collection.json", "echo.native.first_playtest_crash_report_collection.v1", outcome.diagnostics(), outcome.firstPlaytestCrashReportCollection());
        writeSimple(fixture, result, "phase13-m19-completion.json", "echo.native.phase13_m19_completion.v1", outcome.diagnostics(), outcome.phase13M19Completion());
        writeSimple(fixture, result, "first-playtest-open-gate.json", "echo.native.first_playtest_open_gate.v1", outcome.diagnostics(), outcome.firstPlaytestOpenGate());
    }

    private void writePhase14PreflightReports(Path fixture, EchoNativeScanResult result, EchoNativePhase14PreflightOutcome outcome) throws IOException {
        writeSimple(fixture, result, "first-playtest-post-open-intake.json", "echo.native.first_playtest_post_open_intake.v1", outcome.diagnostics(), outcome.firstPlaytestPostOpenIntake());
        writeSimple(fixture, result, "first-playtest-feedback-inventory.json", "echo.native.first_playtest_feedback_inventory.v1", outcome.diagnostics(), outcome.firstPlaytestFeedbackInventory());
        writeSimple(fixture, result, "first-playtest-waiting-checklist.json", "echo.native.first_playtest_waiting_checklist.v1", outcome.diagnostics(), outcome.firstPlaytestWaitingChecklist());
        writeSimple(fixture, result, "phase14-preflight-audit.json", "echo.native.phase14_preflight_audit.v1", outcome.diagnostics(), outcome.phase14PreflightAudit());
        writeSimple(fixture, result, "phase14-readiness.json", "echo.native.phase14_readiness.v1", outcome.diagnostics(), outcome.phase14Readiness());
        writeSimple(fixture, result, "phase14-next-actions.json", "echo.native.phase14_next_actions.v1", outcome.diagnostics(), outcome.phase14NextActions());
    }

    private void writeLaunchRealityAuditReports(Path fixture, EchoNativeScanResult result, EchoNativeLaunchRealityAuditOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-reality-audit.json", "echo.native.native_loader_reality_audit.v1", outcome.diagnostics(), outcome.nativeLoaderRealityAudit());
        writeSimple(fixture, result, "native-loader-launch-command-classification.json", "echo.native.native_loader_launch_command_classification.v1", outcome.diagnostics(), outcome.nativeLoaderLaunchCommandClassification());
        writeSimple(fixture, result, "native-loader-beta-implementation-next-actions.json", "echo.native.native_loader_beta_implementation_next_actions.v1", outcome.diagnostics(), outcome.nativeLoaderBetaImplementationNextActions());
    }

    private void writeIsolatedRuntimeWorkspaceReports(Path fixture, EchoNativeScanResult result, EchoNativeIsolatedRuntimeWorkspaceOutcome outcome) throws IOException {
        writeSimple(fixture, result, "isolated-runtime-workspace-plan.json", "echo.native.isolated_runtime_workspace_plan.v1", outcome.diagnostics(), outcome.isolatedRuntimeWorkspacePlan());
        writeSimple(fixture, result, "isolated-runtime-workspace-materialization.json", "echo.native.isolated_runtime_workspace_materialization.v1", outcome.diagnostics(), outcome.isolatedRuntimeWorkspaceMaterialization());
        writeSimple(fixture, result, "isolated-runtime-workspace-safety-status.json", "echo.native.isolated_runtime_workspace_safety_status.v1", outcome.diagnostics(), outcome.isolatedRuntimeWorkspaceSafetyStatus());
    }

    private void writeRealProcessLaunchHarnessReports(Path fixture, EchoNativeScanResult result, EchoNativeRealProcessLaunchHarnessOutcome outcome) throws IOException {
        writeSimple(fixture, result, "real-process-launch-harness-plan.json", "echo.native.real_process_launch_harness_plan.v1", outcome.diagnostics(), outcome.realProcessLaunchHarnessPlan());
        writeSimple(fixture, result, "real-process-launch-safety-gate.json", "echo.native.real_process_launch_safety_gate.v1", outcome.diagnostics(), outcome.realProcessLaunchSafetyGate());
        writeSimple(fixture, result, "real-process-command-line-preview.json", "echo.native.real_process_command_line_preview.v1", outcome.diagnostics(), outcome.realProcessCommandLinePreview());
        writeSimple(fixture, result, "real-process-environment-plan.json", "echo.native.real_process_environment_plan.v1", outcome.diagnostics(), outcome.realProcessEnvironmentPlan());
    }

    private void writeExecutionReadinessReports(Path fixture, EchoNativeScanResult result, EchoNativeExecutionReadinessOutcome outcome) throws IOException {
        writeSimple(fixture, result, "process-execution-readiness.json", "echo.native.process_execution_readiness.v1", outcome.diagnostics(), outcome.processExecutionReadiness());
        writeSimple(fixture, result, "controlled-launch-operator-checklist.json", "echo.native.controlled_launch_operator_checklist.v1", outcome.diagnostics(), outcome.controlledLaunchOperatorChecklist());
        writeSimple(fixture, result, "controlled-launch-rollback-plan.json", "echo.native.controlled_launch_rollback_plan.v1", outcome.diagnostics(), outcome.controlledLaunchRollbackPlan());
        writeSimple(fixture, result, "phase13-native-loader-beta-gate.json", "echo.native.phase13_native_loader_beta_gate.v1", outcome.diagnostics(), outcome.phase13NativeLoaderBetaGate());
    }

    private void writeControlledProcessLaunchReports(Path fixture, EchoNativeScanResult result, EchoNativeControlledProcessLaunchOutcome outcome) throws IOException {
        writeRuntimeAwareSimple(fixture, result, "controlled-process-launch-plan.json", "echo.native.controlled_process_launch_plan.v1", outcome.diagnostics(), outcome.controlledProcessLaunchPlan());
        writeRuntimeAwareSimple(fixture, result, "controlled-process-launch-safety-gate.json", "echo.native.controlled_process_launch_safety_gate.v1", outcome.diagnostics(), outcome.controlledProcessLaunchSafetyGate());
        writeRuntimeAwareSimple(fixture, result, "controlled-process-launch-result.json", "echo.native.controlled_process_launch_result.v1", outcome.diagnostics(), outcome.controlledProcessLaunchResult());
        writeRuntimeAwareSimple(fixture, result, "controlled-process-output-capture.json", "echo.native.controlled_process_output_capture.v1", outcome.diagnostics(), outcome.controlledProcessOutputCapture());
        writeRuntimeAwareSimple(fixture, result, "controlled-process-rollback-status.json", "echo.native.controlled_process_rollback_status.v1", outcome.diagnostics(), outcome.controlledProcessRollbackStatus());
    }

    private void writeProductModuleActivationStatusFromMarker(
            Path fixture,
            EchoNativeScanResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        Path markerPath = fixture.resolve("isolated-runtime/game/echo-native/module-activation.json").normalize();
        Map<String, Object> activationMarker = Files.isRegularFile(markerPath)
                ? EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(markerPath)))
                : Map.of();
        writeSimple(
                fixture,
                result,
                "native-product-module-activation-status.json",
                "echo.native.product_module_activation_status.v1",
                diagnostics,
                EchoNativeProductActivationStatus.productModuleActivationStatus(
                        packId(result),
                        result.descriptors(),
                        activationMarker,
                        diagnostics
                )
        );
    }

    private void writeTesterEvidenceReports(Path fixture, EchoNativeScanResult result, EchoNativeTesterEvidenceOutcome outcome) throws IOException {
        writeSimple(fixture, result, "tester-playable-evidence.json", "echo.native.tester_playable_evidence.v1", outcome.diagnostics(), outcome.testerPlayableEvidence());
        writeSimple(fixture, result, "minecraft-baseline-playability.json", "echo.native.minecraft_baseline_playability.v1", outcome.diagnostics(), outcome.minecraftBaselinePlayability());
        writeSimple(fixture, result, "native-product-playable-gap.json", "echo.native.product_playable_gap.v1", outcome.diagnostics(), outcome.nativeProductPlayableGap());
        writeSimple(fixture, result, "phase13-m20-completion.json", "echo.native.phase13_m20_completion.v1", outcome.diagnostics(), outcome.phase13M20Completion());
        writeSimple(fixture, result, "phase13-m21-readiness.json", "echo.native.phase13_m21_readiness.v1", outcome.diagnostics(), outcome.phase13M21Readiness());
    }

    private void writeModuleRuntimeBridgeReports(Path fixture, EchoNativeScanResult result, EchoNativeModuleRuntimeBridgeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-module-runtime-bridge-plan.json", "echo.native.module_runtime_bridge_plan.v1", outcome.diagnostics(), outcome.nativeModuleRuntimeBridgePlan());
        writeSimple(fixture, result, "native-module-runtime-bridge-safety-gate.json", "echo.native.module_runtime_bridge_safety_gate.v1", outcome.diagnostics(), outcome.nativeModuleRuntimeBridgeSafetyGate());
        writeSimple(fixture, result, "native-module-bootstrap-status.json", "echo.native.module_bootstrap_status.v1", outcome.diagnostics(), outcome.nativeModuleBootstrapStatus());
        writeSimple(fixture, result, "native-product-live-module-activation-status.json", "echo.native.product_live_module_activation_status.v1", outcome.diagnostics(), outcome.nativeProductLiveModuleActivationStatus());
        writeSimple(fixture, result, "native-product-playable-gate.json", "echo.native.product_playable_gate.v1", outcome.diagnostics(), outcome.nativeProductPlayableGate());
    }

    private void writeLiveActivationPlanReports(Path fixture, EchoNativeScanResult result, EchoNativeLiveActivationPlanOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-live-activation-wrapper-plan.json", "echo.native.live_activation_wrapper_plan.v1", outcome.diagnostics(), outcome.nativeLiveActivationWrapperPlan());
        writeSimple(fixture, result, "native-live-activation-safety-gate.json", "echo.native.live_activation_safety_gate.v1", outcome.diagnostics(), outcome.nativeLiveActivationSafetyGate());
        writeSimple(fixture, result, "native-live-activation-marker-contract.json", "echo.native.live_activation_marker_contract.v1", outcome.diagnostics(), outcome.nativeLiveActivationMarkerContract());
        writeSimple(fixture, result, "phase13-m22-readiness.json", "echo.native.phase13_m22_readiness.v1", outcome.diagnostics(), outcome.phase13M22Readiness());
    }

    private void writeBootstrapActivationReports(Path fixture, EchoNativeScanResult result, EchoNativeBootstrapActivationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-bootstrap-activation-result.json", "echo.native.bootstrap_activation_result.v1", outcome.diagnostics(), outcome.nativeBootstrapActivationResult());
        writeSimple(fixture, result, "native-live-activation-marker.json", "echo.native.live_activation_marker_report.v1", outcome.diagnostics(), outcome.nativeLiveActivationMarker());
        writeSimple(fixture, result, "native-live-activation-safety-status.json", "echo.native.live_activation_safety_status.v1", outcome.diagnostics(), outcome.nativeLiveActivationSafetyStatus());
        writeSimple(fixture, result, "phase13-m22-completion.json", "echo.native.phase13_m22_completion.v1", outcome.diagnostics(), outcome.phase13M22Completion());
        writeSimple(fixture, result, "phase13-m23-readiness.json", "echo.native.phase13_m23_readiness.v1", outcome.diagnostics(), outcome.phase13M23Readiness());
    }

    private void writeGameplayHookReports(Path fixture, EchoNativeScanResult result, EchoNativeGameplayHookOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-product-gameplay-hook-evidence.json", "echo.native.product_gameplay_hook_evidence.v1", outcome.diagnostics(), outcome.nativeProductGameplayHookEvidence());
        writeSimple(fixture, result, "native-module-gameplay-hook-status.json", "echo.native.module_gameplay_hook_status.v1", outcome.diagnostics(), outcome.nativeModuleGameplayHookStatus());
        writeSimple(fixture, result, "native-product-playable-readiness.json", "echo.native.product_playable_readiness.v1", outcome.diagnostics(), outcome.nativeProductPlayableReadiness());
        writeSimple(fixture, result, "phase13-m23-completion.json", "echo.native.phase13_m23_completion.v1", outcome.diagnostics(), outcome.phase13M23Completion());
        writeSimple(fixture, result, "phase13-m24-readiness.json", "echo.native.phase13_m24_readiness.v1", outcome.diagnostics(), outcome.phase13M24Readiness());
    }

    private void writeGameplayHookBridgeReports(Path fixture, EchoNativeScanResult result, EchoNativeGameplayHookBridgeOutcome outcome) throws IOException {
        writeSimple(fixture, result, "gameplay-hook-bridge-plan.json", "echo.native.gameplay_hook_bridge_plan.v1", outcome.diagnostics(), outcome.gameplayHookBridgePlan());
        writeSimple(fixture, result, "gameplay-hook-signal-contract.json", "echo.native.gameplay_hook_signal_contract.v1", outcome.diagnostics(), outcome.gameplayHookSignalContract());
        writeSimple(fixture, result, "gameplay-hook-signal-status.json", "echo.native.gameplay_hook_signal_status.v1", outcome.diagnostics(), outcome.gameplayHookSignalStatus());
        writeSimple(fixture, result, "native-product-module-gameplay-activation.json", "echo.native.product_module_gameplay_activation.v1", outcome.diagnostics(), outcome.nativeProductModuleGameplayActivation());
        writeSimple(fixture, result, "phase13-m24-completion.json", "echo.native.phase13_m24_completion.v1", outcome.diagnostics(), outcome.phase13M24Completion());
        writeSimple(fixture, result, "native-product-playable-gate.json", "echo.native.product_playable_gate.v1", outcome.diagnostics(), outcome.nativeProductPlayableGate());
        writeSimple(fixture, result, "phase13-m25-readiness.json", "echo.native.phase13_m25_readiness.v1", outcome.diagnostics(), outcome.phase13M25Readiness());
    }

    private void writeGameplayHookInstrumentationReports(Path fixture, EchoNativeScanResult result, EchoNativeGameplayHookInstrumentationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "gameplay-hook-instrumentation-plan.json", "echo.native.gameplay_hook_instrumentation_plan.v1", outcome.diagnostics(), outcome.gameplayHookInstrumentationPlan());
        writeSimple(fixture, result, "gameplay-hook-signal-write-result.json", "echo.native.gameplay_hook_signal_write_result.v1", outcome.diagnostics(), outcome.gameplayHookSignalWriteResult());
        writeSimple(fixture, result, "gameplay-hook-signal-audit.json", "echo.native.gameplay_hook_signal_audit.v1", outcome.diagnostics(), outcome.gameplayHookSignalAudit());
        writeSimple(fixture, result, "phase13-m25-completion.json", "echo.native.phase13_m25_completion.v1", outcome.diagnostics(), outcome.phase13M25Completion());
        writeSimple(fixture, result, "phase13-m26-readiness.json", "echo.native.phase13_m26_readiness.v1", outcome.diagnostics(), outcome.phase13M26Readiness());
    }

    private void writePlayableBetaReports(Path fixture, EchoNativeScanResult result, EchoNativePlayableBetaOutcome outcome) throws IOException {
        writeSimple(fixture, result, "phase13-m26-completion.json", "echo.native.phase13_m26_completion.v1", outcome.diagnostics(), outcome.phase13M26Completion());
        writeSimple(fixture, result, "native-loader-playable-beta-readiness.json", "echo.native.native_loader_playable_beta_readiness.v1", outcome.diagnostics(), outcome.nativeLoaderPlayableBetaReadiness());
        writeSimple(fixture, result, "native-product-loader-beta-status.json", "echo.native.product_loader_beta_status.v1", outcome.diagnostics(), outcome.nativeProductLoaderBetaStatus());
        writeSimple(fixture, result, "internal-tester-beta-gate.json", "echo.native.internal_tester_beta_gate.v1", outcome.diagnostics(), outcome.internalTesterBetaGate());
    }

    private void writeBetaFeedbackReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaFeedbackOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-feedback-inventory.json", "echo.native.native_loader_beta_feedback_inventory.v1", outcome.diagnostics(), outcome.nativeLoaderBetaFeedbackInventory());
        writeSimple(fixture, result, "native-loader-beta-crash-intake.json", "echo.native.native_loader_beta_crash_intake.v1", outcome.diagnostics(), outcome.nativeLoaderBetaCrashIntake());
        writeSimple(fixture, result, "native-loader-beta-known-issues.json", "echo.native.native_loader_beta_known_issues.v1", outcome.diagnostics(), outcome.nativeLoaderBetaKnownIssues());
        writeSimple(fixture, result, "native-loader-beta-next-action-queue.json", "echo.native.native_loader_beta_next_action_queue.v1", outcome.diagnostics(), outcome.nativeLoaderBetaNextActionQueue());
        writeSimple(fixture, result, "phase13-m27-completion.json", "echo.native.phase13_m27_completion.v1", outcome.diagnostics(), outcome.phase13M27Completion());
        writeSimple(fixture, result, "phase13-m28-readiness.json", "echo.native.phase13_m28_readiness.v1", outcome.diagnostics(), outcome.phase13M28Readiness());
    }

    private void writeBetaWideningReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaWideningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-widening-plan.json", "echo.native.native_loader_beta_widening_plan.v1", outcome.diagnostics(), outcome.nativeLoaderBetaWideningPlan());
        writeSimple(fixture, result, "native-loader-beta-widening-safety-gate.json", "echo.native.native_loader_beta_widening_safety_gate.v1", outcome.diagnostics(), outcome.nativeLoaderBetaWideningSafetyGate());
        writeSimple(fixture, result, "beta-tester-cohort-plan.json", "echo.native.beta_tester_cohort_plan.v1", outcome.diagnostics(), outcome.betaTesterCohortPlan());
        writeSimple(fixture, result, "phase13-m28-completion.json", "echo.native.phase13_m28_completion.v1", outcome.diagnostics(), outcome.phase13M28Completion());
        writeSimple(fixture, result, "phase13-m29-readiness.json", "echo.native.phase13_m29_readiness.v1", outcome.diagnostics(), outcome.phase13M29Readiness());
    }

    private void writeBetaSoakReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaSoakOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-soak-plan.json", "echo.native.native_loader_beta_soak_plan.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSoakPlan());
        writeSimple(fixture, result, "native-loader-beta-session-inventory.json", "echo.native.native_loader_beta_session_inventory.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSessionInventory());
        writeSimple(fixture, result, "native-loader-beta-issue-triage.json", "echo.native.native_loader_beta_issue_triage.v1", outcome.diagnostics(), outcome.nativeLoaderBetaIssueTriage());
        writeSimple(fixture, result, "native-loader-beta-regression-watchlist.json", "echo.native.native_loader_beta_regression_watchlist.v1", outcome.diagnostics(), outcome.nativeLoaderBetaRegressionWatchlist());
        writeSimple(fixture, result, "phase13-m29-completion.json", "echo.native.phase13_m29_completion.v1", outcome.diagnostics(), outcome.phase13M29Completion());
        writeSimple(fixture, result, "phase13-m30-readiness.json", "echo.native.phase13_m30_readiness.v1", outcome.diagnostics(), outcome.phase13M30Readiness());
    }

    private void writePublicBetaCandidateReports(Path fixture, EchoNativeScanResult result, EchoNativePublicBetaCandidateOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-public-beta-candidate-audit.json", "echo.native.native_loader_public_beta_candidate_audit.v1", outcome.diagnostics(), outcome.nativeLoaderPublicBetaCandidateAudit());
        writeSimple(fixture, result, "native-loader-public-beta-safety-gate.json", "echo.native.native_loader_public_beta_safety_gate.v1", outcome.diagnostics(), outcome.nativeLoaderPublicBetaSafetyGate());
        writeSimple(fixture, result, "public-beta-tester-readiness.json", "echo.native.public_beta_tester_readiness.v1", outcome.diagnostics(), outcome.publicBetaTesterReadiness());
        writeSimple(fixture, result, "phase13-m30-completion.json", "echo.native.phase13_m30_completion.v1", outcome.diagnostics(), outcome.phase13M30Completion());
        writeSimple(fixture, result, "phase13-m31-readiness.json", "echo.native.phase13_m31_readiness.v1", outcome.diagnostics(), outcome.phase13M31Readiness());
    }

    private void writePublicBetaOpeningReports(Path fixture, EchoNativeScanResult result, EchoNativePublicBetaOpeningOutcome outcome) throws IOException {
        writeSimple(fixture, result, "public-beta-opening-audit.json", "echo.native.public_beta_opening_audit.v1", outcome.diagnostics(), outcome.publicBetaOpeningAudit());
        writeSimple(fixture, result, "public-beta-safety-gate.json", "echo.native.public_beta_safety_gate.v1", outcome.diagnostics(), outcome.publicBetaSafetyGate());
        writeSimple(fixture, result, "public-beta-tester-package-readiness.json", "echo.native.public_beta_tester_package_readiness.v1", outcome.diagnostics(), outcome.publicBetaTesterPackageReadiness());
        writeSimple(fixture, result, "public-beta-module-coverage.json", "echo.native.public_beta_module_coverage.v1", outcome.diagnostics(), outcome.publicBetaModuleCoverage());
        writeSimple(fixture, result, "public-beta-rollback-readiness.json", "echo.native.public_beta_rollback_readiness.v1", outcome.diagnostics(), outcome.publicBetaRollbackReadiness());
        writeSimple(fixture, result, "public-beta-known-limitations.json", "echo.native.public_beta_known_limitations.v1", outcome.diagnostics(), outcome.publicBetaKnownLimitations());
        writeSimple(fixture, result, "phase13-m31-completion.json", "echo.native.phase13_m31_completion.v1", outcome.diagnostics(), outcome.phase13M31Completion());
        writeSimple(fixture, result, "phase13-m32-readiness.json", "echo.native.phase13_m32_readiness.v1", outcome.diagnostics(), outcome.phase13M32Readiness());
    }

    private void writeBetaSoakOperatorPacketReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaSoakOperatorPacketOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-soak-operator-packet.json", "echo.native.native_loader_beta_soak_operator_packet.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSoakOperatorPacket());
        writeSimple(fixture, result, "native-loader-beta-session-template.json", "echo.native.native_loader_beta_session_template.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSessionTemplate());
        writeSimple(fixture, result, "native-loader-beta-session-note-drafts.json", "echo.native.native_loader_beta_session_note_drafts.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSessionNoteDrafts());
        writeSimple(fixture, result, "native-loader-beta-evidence-checklist.json", "echo.native.native_loader_beta_evidence_checklist.v1", outcome.diagnostics(), outcome.nativeLoaderBetaEvidenceChecklist());
        writeSimple(fixture, result, "native-loader-beta-remaining-session-plan.json", "echo.native.native_loader_beta_remaining_session_plan.v1", outcome.diagnostics(), outcome.nativeLoaderBetaRemainingSessionPlan());
        writeSimple(fixture, result, "phase13-m29-soak-operator-status.json", "echo.native.phase13_m29_soak_operator_status.v1", outcome.diagnostics(), outcome.phase13M29SoakOperatorStatus());
    }

    private void writeBetaSessionDraftReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaSessionDraftOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-session-draft-files.json", "echo.native.native_loader_beta_session_draft_files.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSessionDraftFiles());
        writeSimple(fixture, result, "phase13-m29-session-draft-status.json", "echo.native.phase13_m29_session_draft_status.v1", outcome.diagnostics(), outcome.phase13M29SessionDraftStatus());
    }

    private void writeBetaSessionNoteValidationReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaSessionNoteValidationOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-session-note-validation.json", "echo.native.native_loader_beta_session_note_validation.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSessionNoteValidation());
        writeSimple(fixture, result, "phase13-m29-note-validation-status.json", "echo.native.phase13_m29_note_validation_status.v1", outcome.diagnostics(), outcome.phase13M29NoteValidationStatus());
    }

    private void writeBetaSoakStatusReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaSoakStatusOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-soak-status-dashboard.json", "echo.native.native_loader_beta_soak_status_dashboard.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSoakStatusDashboard());
        writeSimple(fixture, result, "native-loader-beta-next-session-checklist.json", "echo.native.native_loader_beta_next_session_checklist.v1", outcome.diagnostics(), outcome.nativeLoaderBetaNextSessionChecklist());
    }

    private void writeBetaSoakEvidenceAuditReports(Path fixture, EchoNativeScanResult result, EchoNativeBetaSoakEvidenceAuditOutcome outcome) throws IOException {
        writeSimple(fixture, result, "native-loader-beta-evidence-quality.json", "echo.native.native_loader_beta_evidence_quality.v1", outcome.diagnostics(), outcome.nativeLoaderBetaEvidenceQuality());
        writeSimple(fixture, result, "native-loader-beta-session-proof-matrix.json", "echo.native.native_loader_beta_session_proof_matrix.v1", outcome.diagnostics(), outcome.nativeLoaderBetaSessionProofMatrix());
        writeSimple(fixture, result, "phase13-m29-evidence-gap.json", "echo.native.phase13_m29_evidence_gap.v1", outcome.diagnostics(), outcome.phase13M29EvidenceGap());
    }

    private void writeSimple(Path fixture, EchoNativeScanResult result, String fileName, String schema, List<EchoNativeDiagnostic> diagnostics, Map<String, Object> data) throws IOException {
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, fileName), schema, "echo-native-cli", packId(result), status(diagnostics), summary(result, diagnostics), diagnostics, data);
    }

    private void writeRuntimeAwareSimple(Path fixture, EchoNativeScanResult result, String fileName, String schema, List<EchoNativeDiagnostic> diagnostics, Map<String, Object> data) throws IOException {
        EchoNativeReportWriter.writeReport(reportPath(fixture, result, fileName), schema, "echo-native-cli", packId(result), status(diagnostics), runtimeAwareSummary(result, diagnostics, data), diagnostics, data);
    }

    private static Path reportPath(Path fixture, EchoNativeScanResult result, String fileName) {
        Path workspace = Path.of("").toAbsolutePath().normalize();
        return workspace.resolve("reports").resolve("echo-native").resolve(packId(result).isBlank() ? fixture.getFileName().toString() : packId(result)).resolve(fileName);
    }

    private static Path rootEchoReportPath(String fileName) {
        Path workspace = Path.of("").toAbsolutePath().normalize();
        Path root = workspace.getFileName() != null && "echo-native-platform".equals(workspace.getFileName().toString())
                ? workspace.getParent()
                : workspace;
        return root.resolve("reports").resolve("echo").resolve(fileName);
    }

    private static Map<String, Object> summary(EchoNativeScanResult result, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("blockingDiagnostics", diagnostics.stream().filter(EchoNativeQaCli::isBlocking).count());
        summary.put("descriptorCount", result.descriptors().size());
        summary.put("diagnosticCount", diagnostics.size());
        summary.put("dryRunOnly", true);
        return summary;
    }

    private static Map<String, Object> runtimeAwareSummary(EchoNativeScanResult result, List<EchoNativeDiagnostic> diagnostics, Map<String, Object> data) {
        Map<String, Object> summary = summary(result, diagnostics);
        boolean processEvidence = Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftProcessStarted"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("liveClientProbeExecuted"));
        summary.put("dryRunOnly", !processEvidence);
        return summary;
    }

    private static EchoNativeReportStatus status(List<EchoNativeDiagnostic> diagnostics) {
        if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.FATAL)) {
            return EchoNativeReportStatus.BLOCKED;
        }
        if (diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.ERROR)) {
            return EchoNativeReportStatus.FAILED;
        }
        if (!diagnostics.isEmpty()) {
            return EchoNativeReportStatus.PASS_WITH_WARNINGS;
        }
        return EchoNativeReportStatus.PASS;
    }

    private static boolean hasBlocking(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(EchoNativeQaCli::isBlocking);
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String packId(EchoNativeScanResult result) {
        return result.packProfile() == null ? "" : result.packProfile().id();
    }

    private static void printHelp() {
        System.err.println("Usage: echo-native launch <product-root> [--require-mutation] [--release] [--require-live-runtime] | scan <product-root> | validate <product-root> | graph <product-root> | features <product-root> | lock generate <product-root> | lock verify <product-root> | repair plan <product-root> | ai graph <product-root> | phase12 verify <fixture> | phase13 plan <fixture> | phase13 plan minecraft-resolver <fixture> | phase13 plan library-resolver <fixture> | phase13 plan classpath <fixture> | phase13 plan native-extraction <fixture> | phase13 plan launch-arguments <fixture> | phase13 plan real-process-launch <fixture> | phase13 plan live-activation <fixture> | phase13 activate bootstrap --authorized <fixture> | phase13 verify gameplay-hooks <fixture> | phase13 bridge gameplay-hooks <fixture> | phase13 instrument gameplay-hooks --authorized <fixture> | phase13 verify playable-beta <fixture> | phase13 intake beta-feedback <fixture> | phase13 verify m28 <fixture> | phase13 intake beta-soak <fixture> | phase13 verify m30 <fixture> | phase13 verify m31 <fixture> | phase13 export beta-soak-packet <fixture> | phase13 prepare beta-session-drafts <fixture> | phase13 validate beta-session-notes <fixture> | phase13 status beta-soak <fixture> | phase13 audit beta-soak-evidence <fixture> | phase13 verify execution-readiness <fixture> | phase13 launch controlled --authorized <fixture> | phase13 intake tester-evidence <fixture> | phase13 bridge modules <fixture> | phase13 discover addons <fixture> | phase13 execute lifecycle-stubs <fixture> | phase13 prototype service-bus <fixture> | phase13 prototype config <fixture> | phase13 prototype resources <fixture> | phase13 prototype registry <fixture> | phase13 prototype network <fixture> | phase13 prototype transforms <fixture> | phase13 simulate lifecycle <fixture> | phase13 simulate services <fixture> | phase13 simulate crash-boundary <fixture> | phase13 verify boundaries <fixture> | phase13 verify test-process <fixture> | phase13 rehearse bridges <fixture> | phase13 verify m1 <fixture> | phase13 verify crash-hardening <fixture> | phase13 launch preflight <fixture> | phase13 inventory artifacts <fixture> | phase13 map artifacts <fixture> | phase13 verify artifact-blockers <fixture> | phase13 audit artifact-packaging <fixture> | phase13 audit launch-reality <fixture> | phase13 verify runtime-fixtures <fixture> | phase13 plan runtime-fixture-intake <fixture> | phase13 audit runtime-fixture-approval <fixture> | phase13 prepare runtime-fixture-handoff <fixture> | phase13 prepare isolated-runtime <fixture> | phase13 export runtime-fixture-operator-packet <fixture> | phase13 draft runtime-fixture-approval <fixture> | phase13 audit runtime-fixture-integrity <fixture> | phase13 verify m17 <fixture> | phase13 verify m18 <fixture> | phase13 package first-playtest <fixture> | phase13 plan first-playtest <fixture> | phase13 launch attempt --isolated <fixture> | phase14 preflight <fixture> | report <fixture> | bootstrap --dry-run <fixture> | native <discover|resolve|load|module-status|prove-live|launch|transform-policy> <product-root>");
        System.err.println("Release launch path: startNativeClient or echo-native launch <product-root>.");
    }
}
