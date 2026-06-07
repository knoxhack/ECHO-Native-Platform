package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.ai.EchoNativeAiPlan;
import dev.echo.nativeplatform.ai.EchoNativeAiPlanner;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeTransformCompatibilityPolicy;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EchoNativeCli {
    private final EchoNativeDescriptorScanner scanner = new EchoNativeDescriptorScanner();
    private final EchoNativeValidator validator = new EchoNativeValidator();
    private final EchoNativeGraphPlanner graphPlanner = new EchoNativeGraphPlanner();
    private final EchoNativeLockfileGenerator lockfileGenerator = new EchoNativeLockfileGenerator();
    private final EchoNativeLockfileVerifier lockfileVerifier = new EchoNativeLockfileVerifier();
    private final EchoNativeRepairPlanGenerator repairPlanGenerator = new EchoNativeRepairPlanGenerator();
    private final EchoNativeAiPlanner aiPlanner = new EchoNativeAiPlanner();
    private final EchoNativeRuntimeCli runtimeCli = new EchoNativeRuntimeCli();
    private final EchoNativeProductLauncher productLauncher = new EchoNativeProductLauncher();

    public static void main(String[] args) throws Exception {
        int exitCode = new EchoNativeCli().run(args);
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
            case "validate" -> validate(Path.of(args[1]), List.of(args).contains("--release"));
            case "graph" -> graph(Path.of(args[1]));
            case "features" -> features(Path.of(args[1]));
            case "lock" -> lock(args);
            case "repair" -> repair(args);
            case "ai" -> ai(args);
            case "launch" -> launchNativeProduct(args, 1);
            case "native" -> nativeCommand(args);
            default -> {
                printHelp();
                yield 2;
            }
        };
    }

    private int nativeCommand(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: native <discover|resolve|load|module-status|prove-live|launch|transform-policy> <product-root> [--require-mutation] [--release] [--require-live-runtime]");
            return 2;
        }
        Path productRoot = Path.of(args[2]);
        return switch (args[1]) {
            case "discover" -> runtimeCli.discover(productRoot);
            case "resolve" -> runtimeCli.resolve(productRoot);
            case "load" -> runtimeCli.load(productRoot);
            case "module-status" -> runtimeCli.moduleStatus(productRoot);
            case "prove-live" -> runtimeCli.proveLive(productRoot);
            case "launch" -> launchNativeProduct(args, 2);
            case "transform-policy" -> transformPolicy(productRoot);
            default -> {
                System.out.println("Unknown native subcommand: " + args[1]);
                yield 2;
            }
        };
    }

    private int launchNativeProduct(String[] args, int productRootIndex) throws IOException {
        if (args.length <= productRootIndex || args[productRootIndex].startsWith("--")) {
            System.out.println("Usage: echo-native launch <product-root> [--require-mutation] [--release] [--require-live-runtime]");
            return 2;
        }
        Path productRoot = Path.of(args[productRootIndex]);
        List<String> arguments = List.of(args);
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = productLauncher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        arguments.contains("--require-mutation"),
                        arguments.contains("--release"),
                        arguments.contains("--require-live-runtime")));
        System.out.println("Native product launch for " + outcome.packId() + ": "
                + outcome.loadedModules() + "/" + outcome.totalModules() + " loaded, "
                + outcome.registeredModules() + "/" + outcome.totalModules() + " registered, "
                + outcome.mutatedModules() + "/" + outcome.totalModules() + " mutated, "
                + outcome.failedModules() + " failed"
                + (outcome.requireMutation() ? " [mutation required]" : "")
                + (outcome.releaseMode() ? " [release]" : "")
                + (outcome.requireLiveRuntime() ? " [live runtime required]" : ""));
        EchoNativeProductLauncher.EchoNativeProductBootstrapProfileReport profile = outcome.bootstrapProfile();
        System.out.println("  profile: rootModule=" + profile.rootModuleId()
                + ", bootstrapProfile=" + (profile.bootstrapProfileClass().isBlank() ? "<generic>" : profile.bootstrapProfileClass())
                + ", descriptor=" + (profile.descriptorId().isBlank() ? "<none>" : profile.descriptorId())
                + (profile.profileDriven() ? " [addon-driven]" : " [generic]"));
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

    private int transformPolicy(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(result.diagnostics());
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

    private int scan(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        System.out.println("Scanned " + result.descriptors().size() + " descriptors for " + packId(result) + ".");
        result.descriptors().forEach(descriptor -> System.out.println("  " + descriptor.id()
                + " [" + descriptor.kind() + "/" + descriptor.role() + "]"));
        printDiagnostics(result.diagnostics());
        return hasBlocking(result.diagnostics()) ? 1 : 0;
    }

    private int validate(Path productRoot, boolean releaseMode) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(
                result,
                releaseMode ? EchoNativeValidator.ValidationMode.RELEASE : EchoNativeValidator.ValidationMode.COMPATIBILITY
        );
        System.out.println("Validated " + packId(result) + " with " + diagnostics.size() + " diagnostics"
                + (releaseMode ? " [release]" : "") + ".");
        printDiagnostics(diagnostics);
        return hasBlocking(diagnostics) ? 1 : 0;
    }

    private int graph(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        System.out.println("Planned native graph for " + packId(result) + ".");
        printDiagnostics(graphPlan.diagnostics());
        return hasBlocking(graphPlan.diagnostics()) ? 1 : 0;
    }

    private int features(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        System.out.println("Planned native feature graph for " + packId(result) + ".");
        printDiagnostics(graphPlan.diagnostics());
        return hasBlocking(graphPlan.diagnostics()) ? 1 : 0;
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

    private int lockGenerate(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), diagnostics);
        System.out.println("Generated native lock model for " + packId(result) + ": "
                + lockfilePlan.lockfile().size() + " top-level fields.");
        printDiagnostics(lockfilePlan.diagnostics());
        return hasBlocking(lockfilePlan.diagnostics()) ? 1 : 0;
    }

    private int lockVerify(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), diagnostics);
        EchoNativeLockfileVerificationPlan verificationPlan =
                lockfileVerifier.verify(result.packProfile(), lockfilePlan, productRoot.resolve("lockfile.json"));
        System.out.println("Verified native lock model for " + packId(result) + ": " + verificationPlan.status().get("status"));
        printDiagnostics(verificationPlan.diagnostics());
        return hasBlocking(verificationPlan.diagnostics()) ? 1 : 0;
    }

    private int repairPlan(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        List<EchoNativeDiagnostic> diagnostics = validator.validate(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), diagnostics);
        EchoNativeLockfileVerificationPlan verificationPlan =
                lockfileVerifier.verify(result.packProfile(), lockfilePlan, productRoot.resolve("lockfile.json"));
        EchoNativeRepairPlan repairPlan = repairPlanGenerator.plan(packId(result), verificationPlan.diagnostics());
        System.out.println("Planned native repair actions for " + packId(result) + ".");
        printDiagnostics(repairPlan.diagnostics());
        return hasBlocking(repairPlan.diagnostics()) ? 1 : 0;
    }

    private int aiGraph(Path productRoot) throws IOException {
        EchoNativeScanResult result = scanner.scan(productRoot);
        EchoNativeGraphPlan graphPlan = graphPlanner.plan(result);
        EchoNativeLockfilePlan lockfilePlan = lockfileGenerator.generate(result.packProfile(), result.descriptors(), graphPlan.diagnostics());
        EchoNativeLockfileVerificationPlan lockfileStatus =
                lockfileVerifier.verify(result.packProfile(), lockfilePlan, productRoot.resolve("lockfile.json"));
        EchoNativeRepairPlan repairPlan = repairPlanGenerator.plan(packId(result), lockfileStatus.diagnostics());
        EchoNativeAiPlan aiPlan = aiPlanner.plan(packId(result), result.descriptors(), repairPlan.diagnostics(),
                graphPlan.moduleGraph(), graphPlan.featureGraph(), graphPlan.serviceGraph(),
                lockfileStatus.status(), repairPlan.repairPlan());
        System.out.println("Planned native AI graph for " + packId(result) + ": "
                + aiPlan.aiTasks().size() + " tasks.");
        printDiagnostics(aiPlan.diagnostics());
        return hasBlocking(aiPlan.diagnostics()) ? 1 : 0;
    }

    private static void printDiagnostics(List<EchoNativeDiagnostic> diagnostics) {
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.WARNING
                        || diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL)
                .forEach(diagnostic -> System.out.println("  [" + diagnostic.severity() + "] "
                        + diagnostic.code() + ": " + diagnostic.summary()));
    }

    private static boolean hasBlocking(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream().anyMatch(EchoNativeCli::isBlocking);
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private static String packId(EchoNativeScanResult result) {
        return result.packProfile() == null ? "" : result.packProfile().id();
    }

    private static void printHelp() {
        System.err.println("Usage: echo-native launch <product-root> [--require-mutation] [--release] [--require-live-runtime] | scan <product-root> | validate <product-root> [--release] | graph <product-root> | features <product-root> | lock generate <product-root> | lock verify <product-root> | repair plan <product-root> | ai graph <product-root> | native <discover|resolve|load|module-status|prove-live|launch|transform-policy> <product-root>");
        System.err.println("Internal phase, report, fixture, and soak workflows live in the QA CLI sidecar.");
    }
}
