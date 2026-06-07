package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;

import java.nio.file.Path;
import java.util.List;

public final class EchoNativeProductLauncherMain {
    private EchoNativeProductLauncherMain() {
    }

    public static void main(String[] args) throws Exception {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) throws Exception {
        if (args.length > 0 && "package".equals(args[0])) {
            return packageProduct(args);
        }
        Path productRoot = null;
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                productRoot = Path.of(arg);
                break;
            }
        }
        if (productRoot == null) {
            printUsage();
            return 2;
        }

        List<String> arguments = List.of(args);
        boolean moduleOnly = arguments.contains("--module-only") || arguments.contains("--dev");
        boolean fullRelease = arguments.contains("--full-release");
        boolean releaseMode = fullRelease || arguments.contains("--release") || !moduleOnly;
        boolean requireMutation = releaseMode || arguments.contains("--require-mutation");
        boolean requireLiveRuntime = fullRelease || arguments.contains("--require-live-runtime");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome =
                new EchoNativeProductLauncher().launch(
                        productRoot,
                        new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                                requireMutation,
                                releaseMode,
                                requireLiveRuntime));
        printOutcome(outcome);
        return outcome.accepted() ? 0 : 1;
    }

    private static int packageProduct(String[] args) throws Exception {
        PackageArguments parsed = PackageArguments.parse(args);
        if (parsed == null) {
            printUsage();
            return 2;
        }
        if (parsed.gradleBuildRoot() != null) {
            System.setProperty("echo.native.gradleBuildRoot", parsed.gradleBuildRoot().toString());
        }
        EchoNativeProductPackager.EchoNativeProductPackageOutcome outcome =
                new EchoNativeProductPackager().packageProduct(
                        parsed.sourceRoot(),
                        parsed.packProfileRoot(),
                        parsed.outputRoot()
                );
        System.out.println("Native product package for " + outcome.packId() + ": "
                + outcome.packagedModules() + "/" + outcome.totalModules()
                + " modules packaged at " + outcome.outputRoot());
        outcome.diagnostics().forEach(diagnostic -> System.out.println("  [PACKAGE] " + diagnostic));
        return outcome.packaged() ? 0 : 1;
    }

    private static void printUsage() {
        System.out.println("Usage: echo-native-product-launcher <product-root> [--module-only] [--require-mutation] [--release] [--require-live-runtime] [--full-release]");
        System.out.println("       Product launch defaults to --release; --module-only opts into dev/preflight module loading.");
        System.out.println("       --release implies --require-mutation.");
        System.out.println("       --full-release implies --release and --require-live-runtime.");
        System.out.println("   or: echo-native-product-launcher package <source-root> <output-root> [--pack-profile <pack-profile-root-or-echo.pack.json>] [--gradle-build-root <root>]");
    }

    private static void printOutcome(EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome) {
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
        EchoNativeProductLauncher.EchoNativeProductPreWindowAssertionReport preWindow = outcome.preWindowAssertions();
        System.out.println("  pre-window: moduleReleaseReady=" + preWindow.moduleReleaseReady()
                + ", productWindowReady=" + preWindow.productWindowReady()
                + ", classpath=" + preWindow.classpathReadyModuleCount() + "/" + preWindow.nativeEntrypointModuleCount()
                + ", servicesMissing=" + preWindow.missingServiceIds().size()
                + ", profileReady=" + preWindow.productProfileReady()
                + ", ashfallProfileReady=" + preWindow.ashfallProfileReady()
                + ", handoffAuthorityReady=" + preWindow.bootstrapHandoffAuthorityReady()
                + ", blessedLaunch=" + preWindow.blessedLaunchTask()
                + ", handoff=" + preWindow.internalHandoffTask()
                + ", routes=" + preWindow.routeRegistryCount()
                + ", resources=" + preWindow.mountedResourceCount()
                + ", ashfallWorldReady=" + preWindow.ashfallDatapackWorldPresetReady());
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport capabilities = outcome.runtimeCapabilities();
        System.out.println("  runtime: firstClassNative=" + capabilities.firstClassNativeRuntime()
                + ", delegateRequired=" + capabilities.delegateRequired()
                + ", liveMinecraftAttached=" + capabilities.liveMinecraftAttached()
                + ", releaseRuntimeTrusted=" + capabilities.liveRuntimeTrusted()
                + ", liveRuntimeBridgeAttached=" + capabilities.liveRuntimeBridgeAttached()
                + ", nativeRuntimeDispatchReady=" + capabilities.nativeRuntimeDispatchReady()
                + ", liveRegistryBridgeAttached=" + capabilities.liveRegistryBridgeAttached()
                + ", registryEntries=" + capabilities.registryEntryCount()
                + ", liveRegistryMutations=" + capabilities.liveRegistryBridgeMutatedEntryCount()
                + ", savesDirectoryConfigured=" + capabilities.savesDirectoryConfigured());
        System.out.println("  hosts: lifecycleEvents=" + capabilities.lifecycleEventCount()
                + ", eventSubscriptions=" + capabilities.eventSubscriptionCount()
                + ", executedEventHandlers=" + capabilities.executedEventHandlerCount()
                + ", commands=" + capabilities.queuedCommandCount()
                + ", configs=" + capabilities.registeredConfigCount()
                + ", packets=" + capabilities.boundNetworkPacketCount()
                + ", adapterCoreMutations=" + capabilities.adapterCoreMutatedRecordCount()
                + ", saveDataMutations=" + capabilities.saveDataMutationCount());
        System.out.println("  resources: mounted=" + capabilities.mountedResourceCount()
                + ", worldStartup=" + capabilities.worldStartupResourceCount()
                + ", datapacks=" + capabilities.dataPackResourceCount()
                + ", worldgen=" + capabilities.worldgenResourceCount()
                + ", worldPresets=" + capabilities.worldPresetResourceCount()
                + ", resourcePacks=" + capabilities.resourcePackResourceCount());
        System.out.println("  client: uiHostAttached=" + capabilities.clientUiHostAttached()
                + ", liveClientAttached=" + capabilities.liveClientAttached()
                + ", releaseClientTrusted=" + capabilities.liveClientTrusted()
                + ", liveClientBridgeAttached=" + capabilities.liveClientBridgeAttached()
                + ", headlessClientSurface=" + capabilities.headlessClientSurface()
                + ", registeredSurfaces=" + capabilities.clientSurfaceCount()
                + ", liveAcceptedSurfaces=" + capabilities.liveClientBridgeAcceptedSurfaceCount()
                + ", liveMutatedSurfaces=" + capabilities.liveClientBridgeMutatedSurfaceCount());
        if (outcome.releaseMode() && !capabilities.fullReleaseRuntimeReady()) {
            System.out.println("Native product launch is module-ready, but runtime/client parity is not full release-ready yet.");
        }
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
            if (!module.diagnostics().isEmpty()) {
                System.out.println("      diagnostics: " + String.join("; ", module.diagnostics()));
            }
        }
        if (!outcome.accepted()) {
            System.out.println("Native product launch is not release-ready yet.");
        }
        printDiagnostics(outcome.diagnostics());
    }

    private static void printDiagnostics(List<EchoNativeDiagnostic> diagnostics) {
        diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.WARNING
                        || diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL)
                .forEach(diagnostic -> System.out.println("  [" + diagnostic.severity() + "] "
                        + diagnostic.code() + ": " + diagnostic.summary()));
    }

    private record PackageArguments(Path sourceRoot, Path packProfileRoot, Path outputRoot, Path gradleBuildRoot) {
        private static PackageArguments parse(String[] args) {
            if (args.length < 3 || !"package".equals(args[0])) {
                return null;
            }
            Path sourceRoot = null;
            Path outputRoot = null;
            Path packProfileRoot = null;
            Path gradleBuildRoot = null;
            for (int index = 1; index < args.length; index++) {
                String arg = args[index];
                if ("--pack-profile".equals(arg)) {
                    if (index + 1 >= args.length) {
                        return null;
                    }
                    packProfileRoot = Path.of(args[++index]);
                    continue;
                }
                if ("--gradle-build-root".equals(arg)) {
                    if (index + 1 >= args.length) {
                        return null;
                    }
                    gradleBuildRoot = Path.of(args[++index]);
                    continue;
                }
                if (arg.startsWith("--")) {
                    return null;
                }
                if (sourceRoot == null) {
                    sourceRoot = Path.of(arg);
                } else if (outputRoot == null) {
                    outputRoot = Path.of(arg);
                } else {
                    return null;
                }
            }
            return sourceRoot == null || outputRoot == null
                    ? null
                    : new PackageArguments(sourceRoot, packProfileRoot, outputRoot, gradleBuildRoot);
        }
    }
}
