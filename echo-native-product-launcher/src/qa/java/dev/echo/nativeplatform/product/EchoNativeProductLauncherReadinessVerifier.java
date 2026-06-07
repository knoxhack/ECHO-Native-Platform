package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.loader.EchoNativeDescriptorScanner;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.loader.NativeLoaderLiveClientBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRegistryBridge;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeAttachment;
import dev.echo.nativeplatform.loader.NativeLoaderLiveRuntimeBridge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public final class EchoNativeProductLauncherReadinessVerifier {
    private EchoNativeProductLauncherReadinessVerifier() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 3) {
            throw new IllegalArgumentException("Usage: EchoNativeProductLauncherReadinessVerifier <packaged-product-root> [source-workspace-root] [--phase=<all|product-hooks|release-mutation|trusted-live|trusted-evidence-without-bridges|trusted-bridges|profile-override>]");
        }
        Path productRoot = Path.of(args[0]).toAbsolutePath().normalize();
        String phase = "all";
        Path sourceRoot = null;
        for (int index = 1; index < args.length; index++) {
            String arg = args[index] == null ? "" : args[index].trim();
            if (arg.startsWith("--phase=")) {
                phase = arg.substring("--phase=".length()).trim();
            } else if (!arg.isBlank()) {
                sourceRoot = Path.of(arg).toAbsolutePath().normalize();
            }
        }
        trace("package manifest");
        verifyPackageManifest(productRoot);
        trace("descriptor scan");
        EchoNativeScanResult scanResult = new EchoNativeDescriptorScanner().scanProduct(productRoot);
        require(scanResult.packProfile() != null, "pack profile must be present in packaged product");
        require(scanResult.diagnostics().isEmpty(), "packaged descriptor scan must not emit diagnostics: " + scanResult.diagnostics());
        require(!scanResult.descriptors().isEmpty(), "packaged product must contain module descriptors");
        trace("packaged descriptors");
        verifyPackagedDescriptors(productRoot, scanResult.descriptors());

        EchoNativeProductLauncher launcher = new EchoNativeProductLauncher();
        if ("trusted-live".equals(phase)) {
            trace("trusted live runtime scenarios");
            verifyInjectedLiveRuntimeCanPassGate(productRoot, launcher);
            System.out.println("Native product launcher readiness phase verified: trusted-live");
            return;
        }
        if ("trusted-evidence-without-bridges".equals(phase)) {
            trace("trusted live runtime scenarios: trusted evidence without bridges");
            verifyTrustedEvidenceWithoutBridgesFailsGate(productRoot, launcher);
            System.out.println("Native product launcher readiness phase verified: trusted-evidence-without-bridges");
            return;
        }
        if ("trusted-bridges".equals(phase)) {
            trace("trusted live runtime scenarios: trusted bridges");
            verifyUnsupportedRuntimeHooksStayDiagnosticOnly(productRoot, launcher);
            verifyTrustedLiveBridgeCanPassGate(productRoot, launcher);
            trace("subsystem live release proof requirement");
            verifySubsystemReleaseProofRequired();
            System.out.println("Native product launcher readiness phase verified: trusted-bridges");
            return;
        }
        if ("profile-override".equals(phase)) {
            require(sourceRoot != null, "profile-override phase requires source-workspace-root");
            trace("pack profile override packaging");
            verifyPackProfileOverridePackaging(sourceRoot, productRoot);
            System.out.println("Native product launcher readiness phase verified: profile-override");
            return;
        }
        if ("release-mutation".equals(phase)) {
            trace("release mutation requirement");
            verifyReleaseModeRequiresMutation(productRoot, launcher);
            System.out.println("Native product launcher readiness phase verified: release-mutation");
            return;
        }
        if ("product-hooks".equals(phase)) {
            trace("product hook plan");
            verifyProductHookPlanRuns(productRoot, launcher);
            System.out.println("Native product launcher readiness phase verified: product-hooks");
            return;
        }
        require("all".equals(phase), "unknown readiness verifier phase: " + phase);
        trace("module release launch");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome moduleOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(true, true, false)
        );
        verifyModuleOutcome(moduleOutcome);
        printPreWindowSummary(moduleOutcome);
        trace("release mutation requirement");
        verifyReleaseModeRequiresMutation(productRoot, launcher);
        trace("product hook plan");
        verifyProductHookPlanRuns(productRoot, launcher);
        trace("subsystem live release proof requirement");
        verifySubsystemReleaseProofRequired();

        trace("live runtime fail-closed launch");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome liveOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(true, true, true)
        );
        verifyLiveRuntimeFailsClosed(liveOutcome);
        trace("trusted live runtime scenarios");
        verifyUnsupportedRuntimeHooksStayDiagnosticOnly(productRoot, launcher);
        verifyInjectedLiveRuntimeCanPassGate(productRoot, launcher);
        if (sourceRoot != null) {
            trace("pack profile override packaging");
            verifyPackProfileOverridePackaging(sourceRoot, productRoot);
        }

        System.out.println("Native product launcher readiness verified for " + moduleOutcome.packId()
                + ": " + moduleOutcome.loadedModules() + "/" + moduleOutcome.totalModules()
                + " modules loaded, registered, and mutated; product hooks execute; profile override packaging is generic; live-runtime gate fails closed by default and accepts explicit trusted live attachment.");
    }

    private static void trace(String phase) {
        System.out.println("[native-product-readiness] " + phase);
    }

    private static void verifyPackageManifest(Path productRoot) throws Exception {
        Path manifestPath = productRoot.resolve("echo-native-product-package.json");
        require(Files.isRegularFile(manifestPath), "packaged product manifest is missing: " + manifestPath);
        Map<String, Object> manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        require("echo.native.product_package.v1".equals(string(manifest.get("schema"))),
                "unexpected product package schema: " + manifest.get("schema"));
        require("explicit-packaged-artifacts".equals(string(manifest.get("releaseClasspath"))),
                "product package must use explicit packaged artifacts");
        int totalModules = number(manifest.get("totalModules"));
        int packagedModules = number(manifest.get("packagedModules"));
        require(totalModules > 0, "product package must include at least one module");
        require(packagedModules == totalModules,
                "product package did not package every selected module: " + packagedModules + "/" + totalModules);
        require(list(manifest.get("diagnostics")).isEmpty(),
                "product package manifest must not contain diagnostics: " + manifest.get("diagnostics"));
    }

    private static void verifyPackagedDescriptors(Path productRoot, List<EchoNativeAddonDescriptor> descriptors) {
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            require(isPackagedDescriptor(productRoot, descriptor.descriptorPath()),
                    "descriptor must be from packaged modules layout: " + descriptor.descriptorPath());
            Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
            String entrypoint = string(access.get("nativeEntrypoint"));
            require(!entrypoint.isBlank(), "packaged descriptor must declare nativeEntrypoint: " + descriptor.id());
            List<Object> classpath = list(access.get("nativeClasspath"));
            require(!classpath.isEmpty(), "packaged descriptor must declare nativeClasspath: " + descriptor.id());
            verifyTransformPolicy(descriptor.id(), object(access.get("nativeTransformCompatibilityPolicy")));
            List<String> missing = new ArrayList<>();
            for (Object entry : classpath) {
                String value = string(entry);
                require(!value.isBlank(), "nativeClasspath entry must not be blank for " + descriptor.id());
                require(!EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN.equals(value),
                        "release descriptor must not use inferred classpath token: " + descriptor.id());
                Path classpathEntry = descriptor.descriptorPath()
                        .getParent()
                        .getParent()
                        .resolve(value)
                        .normalize();
                if (!Files.exists(classpathEntry)) {
                    missing.add(classpathEntry.toString());
                }
            }
            require(missing.isEmpty(), "nativeClasspath entries must exist for " + descriptor.id() + ": " + missing);
            Path moduleJar = descriptor.descriptorPath()
                    .getParent()
                    .getParent()
                    .resolve("lib/" + descriptor.id() + ".jar")
                    .normalize();
            require(Files.isRegularFile(moduleJar), "packaged module jar is missing for " + descriptor.id() + ": " + moduleJar);
            String entrypointClass = entrypoint.replace('.', '/') + ".class";
            require(zipContains(moduleJar, entrypointClass),
                    "packaged module jar must contain nativeEntrypoint class for "
                            + descriptor.id() + ": " + entrypointClass);
        }
    }

    private static void verifyTransformPolicy(String moduleId, Map<String, Object> policy) {
        require(!policy.isEmpty(), "packaged descriptor must include nativeTransformCompatibilityPolicy: " + moduleId);
        require("echo-native-transform-compatibility.v1".equals(string(policy.get("policyId"))),
                "unexpected transform compatibility policy id for " + moduleId + ": " + policy.get("policyId"));
        require(Boolean.TRUE.equals(policy.get("compatible")),
                "packaged transform compatibility policy must be compatible for " + moduleId);
        require(!bool(policy.get("bytecodeMutationAllowed")),
                "packaged transform policy must not allow bytecode mutation for " + moduleId);
        require(!bool(policy.get("minecraftBytecodeMutationAllowed")),
                "packaged transform policy must not allow Minecraft bytecode mutation for " + moduleId);
        require(!bool(policy.get("addonBytecodeMutationAllowed")),
                "packaged transform policy must not allow addon bytecode mutation for " + moduleId);
        require(!list(policy.get("supportedNativeDeclarations")).isEmpty(),
                "packaged transform policy must name supported native declarations for " + moduleId);
        require(!string(policy.get("releasePolicySummary")).isBlank(),
                "packaged transform policy must include releasePolicySummary for " + moduleId);
        require(!string(policy.get("diagnosticPathSummary")).isBlank(),
                "packaged transform policy must include diagnosticPathSummary for " + moduleId);
    }

    private static void verifyModuleOutcome(EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome) {
        require(outcome.accepted(), "module release launch must be accepted: " + diagnosticCodes(outcome));
        require(outcome.requireMutation(), "module release launch must require mutation");
        require(outcome.releaseMode(), "module release launch must run in release mode");
        require(!outcome.requireLiveRuntime(), "module release launch must not require live runtime");
        require(outcome.totalModules() > 0, "module release launch must include modules");
        require(outcome.failedModules() == 0, "module release launch must have zero failed modules");
        require(outcome.loadedModules() == outcome.totalModules(), "not every module loaded");
        require(outcome.registeredModules() == outcome.totalModules(), "not every module registered");
        require(outcome.mutatedModules() == outcome.totalModules(), "not every module mutated");
        for (EchoNativeProductLauncher.EchoNativeProductModuleLaunch module : outcome.modules()) {
            require(module.accepted(), "module failed product truth gate: " + module.moduleId() + " " + module.failures());
            require(module.loaded(), "module did not load: " + module.moduleId());
            require(module.registered(), "module did not register: " + module.moduleId());
            require(module.mutated(), "module did not mutate: " + module.moduleId());
            require(module.diagnostics().stream().noneMatch(item -> item.contains("legacy activateNative(Map)")),
                    "product release module must not use legacy activateNative(Map) bridge: " + module.moduleId());
        }
        require(noBlockingDiagnostics(outcome.diagnostics()),
                "module release launch must not emit blocking diagnostics: " + diagnosticCodes(outcome));
        EchoNativeProductLauncher.EchoNativeProductPreWindowAssertionReport preWindow =
                outcome.preWindowAssertions();
        require(preWindow.moduleReleaseReady(), "module release pre-window assertions must pass: " + preWindow);
        require(preWindow.nativeModuleClasspathReady(), "pre-window classpath assertion must pass: " + preWindow);
        require(preWindow.classpathReadyModuleCount() == preWindow.nativeEntrypointModuleCount(),
                "every native entrypoint module must have pre-window classpath evidence: " + preWindow);
        require(preWindow.serviceProvidersReady(), "pre-window service-provider assertion must pass: " + preWindow);
        require(preWindow.missingServiceIds().isEmpty(), "pre-window service providers are missing: " + preWindow);
        require(preWindow.productProfileReady(), "pre-window product profile assertion must pass: " + preWindow);
        require(preWindow.ashfallProfileReady(), "pre-window Ashfall profile assertion must pass: " + preWindow);
        verifyPreWindowHandoffAuthority(preWindow);
        require(!preWindow.productWindowReady(),
                "module-only product launcher verifier must not claim product-window preflight without a trusted live client/window path: "
                        + preWindow);
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport runtime = outcome.runtimeCapabilities();
        require(runtime.runtimeHostRegistered(), "runtime host must be registered");
        require(runtime.firstClassNativeRuntime(), "runtime host must be first-class native runtime");
        require(!runtime.delegateRequired(), "runtime host must not require a delegate for module launch");
        require(runtime.savesDirectoryConfigured(), "runtime host must have saves directory configured");
    }

    private static void verifyReleaseModeRequiresMutation(
            Path productRoot,
            EchoNativeProductLauncher launcher
    ) throws Exception {
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(false, true, false)
        );
        trace("release mutation requirement returned accepted=" + outcome.accepted()
                + " loaded=" + outcome.loadedModules()
                + " registered=" + outcome.registeredModules()
                + " mutated=" + outcome.mutatedModules() + "/" + outcome.totalModules()
                + " diagnostics=" + diagnosticCodes(outcome));
        require(outcome.releaseMode(), "release normalization check must run in release mode");
        require(outcome.requireMutation(), "release mode must imply the mutation truth gate");
        require(outcome.accepted(), "release normalization launch must still be accepted: " + diagnosticCodes(outcome));
        require(outcome.mutatedModules() == outcome.totalModules(),
                "release normalization launch must require every module to mutate");
    }

    private static void verifyLiveRuntimeFailsClosed(EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome) {
        require(!outcome.accepted(), "live-runtime release launch must fail closed until live host/client attach");
        require(outcome.requireLiveRuntime(), "live-runtime check must require live runtime");
        require(outcome.loadedModules() == outcome.totalModules(), "live-runtime check should still load all modules");
        require(outcome.registeredModules() == outcome.totalModules(), "live-runtime check should still register all modules");
        require(outcome.mutatedModules() == outcome.totalModules(), "live-runtime check should still mutate all modules");
        require(hasDiagnostic(outcome.diagnostics(), "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE"),
                "live-runtime check must emit ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE: " + diagnosticCodes(outcome));
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport runtime = outcome.runtimeCapabilities();
        require(!runtime.fullReleaseRuntimeReady(), "live-runtime capability report must not claim full release readiness");
        require(!runtime.liveMinecraftAttached(), "live runtime must remain unattached in headless product verifier");
        require(!runtime.liveClientAttached(), "live client must remain unattached in headless product verifier");
        require(!runtime.liveClientTrusted(), "live client must remain untrusted in product verifier");
        EchoNativeProductLauncher.EchoNativeProductPreWindowAssertionReport preWindow =
                outcome.preWindowAssertions();
        require(preWindow.moduleReleaseReady(), "live-runtime check must retain module pre-window readiness: " + preWindow);
        verifyPreWindowHandoffAuthority(preWindow);
        require(!preWindow.productWindowReady(), "live-runtime check must fail product-window preflight until full routes/resources are present: " + preWindow);
    }

    private static void verifyPreWindowHandoffAuthority(
            EchoNativeProductLauncher.EchoNativeProductPreWindowAssertionReport preWindow
    ) {
        require(preWindow.bootstrapHandoffAuthorityReady(),
                "pre-window handoff authority must be release-ready: " + preWindow);
        require("startNativeClient".equals(preWindow.blessedLaunchTask()),
                "pre-window blessed launch task must remain startNativeClient: " + preWindow);
        require("runNativeBootstrapClient".equals(preWindow.internalHandoffTask()),
                "pre-window internal handoff task must remain runNativeBootstrapClient: " + preWindow);
        require("dev.echo.nativeplatform.bootstrap.EchoNativeBootstrapMain".equals(preWindow.bootstrapMainClass()),
                "pre-window bootstrap main must remain EchoNativeBootstrapMain: " + preWindow);
        require("net.minecraft.client.main.Main".equals(preWindow.realMainClass()),
                "pre-window real main handoff must remain the Minecraft client main: " + preWindow);
    }

    private static void verifyProductHookPlanRuns(
            Path productRoot,
            EchoNativeProductLauncher launcher
    ) throws Exception {
        String saveKey = "qa.product_hook.save." + Long.toUnsignedString(System.nanoTime());
        EchoNativeProductHookPlan hookPlan = new EchoNativeProductHookPlan(
                List.of(new EchoNativeProductHookPlan.RegistryHook(
                        "echocore",
                        "item",
                        "qa_product_hook_item",
                        Map.of(
                                "implementationClass", "qa.ProductHookItem",
                                "source", "product_launcher_readiness_verifier"
                        )
                )),
                List.of(new EchoNativeProductHookPlan.LifecycleHook(
                        "echocore",
                        "qa.product_hook.lifecycle",
                        Map.of("summary", "QA product lifecycle hook executed")
                )),
                List.of(new EchoNativeProductHookPlan.EventSubscriptionHook(
                        "echocore",
                        "qa.product_hook.event",
                        "qa.product_hook.handler",
                        Map.of("summary", "QA product event handler executed")
                )),
                List.of(new EchoNativeProductHookPlan.EventPublishHook(
                        "echocore",
                        "qa.product_hook.event",
                        Map.of("payload", "qa_product_hook_event")
                )),
                List.of(new EchoNativeProductHookPlan.CommandHook(
                        "echocore",
                        "qa.product_hook.command",
                        "commands",
                        "native_product_hook",
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.NetworkHook(
                        "echocore",
                        "qa.product_hook.packet",
                        "networking",
                        "native_product_hook",
                        List.of("qa.product_hook.consumer"),
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.ResourceHook(
                        "echocore",
                        "qa/product_hook/resource.json",
                        "data",
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.ConfigHook(
                        "echocore",
                        "qa.product_hook.config",
                        "server.config",
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.ClientSurfaceHook(
                        "echocore",
                        "qa.product_hook.surface",
                        "screen",
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.ProductWorldHook(
                        "echocore",
                        "qa.product_hook.world",
                        "create_or_open_product_world",
                        "qa:product_world",
                        "qa-product-datapack.zip",
                        "qa:product_resources",
                        "guard_existing_vanilla_saves_as_not_product_world",
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.ProductOnboardingHook(
                        "echocore",
                        "player:qa-product-hook",
                        "qa:product_onboarding",
                        "minecraft:overworld",
                        "qa:starter_structure",
                        "qa:starter_manual",
                        "qa:mission",
                        "started",
                        "qa.objective",
                        "qa.hud",
                        "QA product onboarding briefing",
                        Map.of("source", "product_launcher_readiness_verifier")
                )),
                List.of(new EchoNativeProductHookPlan.SaveDataHook(
                        saveKey,
                        "written",
                        false
                ))
        );

        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        false,
                        false,
                        NativeLoaderLiveRuntimeAttachment.unattached(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", false,
                                "headlessClientSurface", true,
                                "clientAttachment", "qa:headless-product-hook-plan"
                        ),
                        hookPlan
                )
        );
        require(outcome.accepted(), "product hook plan launch must remain accepted: " + diagnosticCodes(outcome));
        require(!outcome.runtimeCapabilities().agent5LiveRuntimeSurfaceProofReady(),
                "headless product hook plan must not satisfy Agent 5 live runtime proof without a live bridge");
        EchoNativeProductLauncher.EchoNativeProductHookReport report = outcome.hookReport();
        require(report.executionCount() >= 12, "product hook plan must execute every requested hook: " + report.executions());
        require(report.mutatedExecutionCount() < report.executionCount(),
                "headless product hook plan must not report every execution as live mutation proof: " + report.executions());
        require(report.publishedEventHandlerExecutionCount() >= 1,
                "product hook plan must execute an event handler: " + report.lifecycleEventHost());
        require(report.runtimeMutatedSurfaces().contains("saveData"),
                "product hook plan must mutate native save data: " + report.runtimeMutatedSurfaces());
        require(number(report.registryHost().get("totalRegistered")) >= 1,
                "product hook plan must register native content: " + report.registryHost());
        require(number(report.commandHost().get("queuedCommandCount")) >= 1,
                "product hook plan must queue a command hook: " + report.commandHost());
        require(number(report.networkHost().get("boundPacketCount")) >= 1,
                "product hook plan must bind a network hook: " + report.networkHost());
        require(number(report.resourceHost().get("mountedResourceCount")) >= 1,
                "product hook plan must mount a resource hook: " + report.resourceHost());
        require(number(report.configHost().get("registeredConfigCount")) >= 1,
                "product hook plan must register a config hook: " + report.configHost());
        require(number(report.clientUiHost().get("surfaceCount")) >= 1,
                "product hook plan must register a client UI surface: " + report.clientUiHost());
        require(hasHook(report, "registry", "item:qa_product_hook_item"),
                "product hook report must include registry hook execution");
        require(hasHookExecution(report, "lifecycle", "qa.product_hook.lifecycle"),
                "product hook report must include lifecycle hook execution");
        require(hasHook(report, "event_subscription", "qa.product_hook.event:qa.product_hook.handler"),
                "product hook report must include event subscription hook execution");
        require(hasHookExecution(report, "event_publish", "qa.product_hook.event"),
                "product hook report must include event publish hook execution");
        require(hasHookExecution(report, "command", "qa.product_hook.command"),
                "product hook report must include command hook execution");
        require(hasHookExecution(report, "network", "qa.product_hook.packet"),
                "product hook report must include network hook execution");
        require(hasHook(report, "resource", "qa/product_hook/resource.json"),
                "product hook report must include resource hook execution");
        require(hasHookExecution(report, "config", "qa.product_hook.config"),
                "product hook report must include config hook execution");
        require(hasHook(report, "client_ui", "qa.product_hook.surface"),
                "product hook report must include client UI hook execution");
        require(hasHook(report, "product_world", "qa.product_hook.world"),
                "product hook report must include product world hook execution");
        require(hasHookExecution(report, "product_onboarding", "qa:product_onboarding"),
                "product hook report must include product onboarding hook execution");
        require(hasHookExecution(report, "save_data", saveKey),
                "product hook report must include save data hook execution");
    }

    private static void verifySubsystemReleaseProofRequired() {
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport weakProof =
                subsystemProofRuntimeCapability(false);
        require(!weakProof.agent5LiveRuntimeSurfaceProofReady(),
                "Agent 5 proof must reject positive subsystem counts without subsystem release-proof booleans");
        require(!weakProof.nativeLifecycleReady(),
                "native lifecycle readiness must require lifecycle release proof");
        require(!weakProof.nativeCommandHostReady(),
                "native command readiness must require command release proof");
        require(!weakProof.nativeConfigHostReady(),
                "native config readiness must require config release proof");
        require(!weakProof.nativeNetworkHostReady(),
                "native network readiness must require network release proof");
        require(!weakProof.fullReleaseRuntimeReady(),
                "full release readiness must reject subsystem count-only live proof");

        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport strongProof =
                subsystemProofRuntimeCapability(true);
        require(strongProof.agent5LiveRuntimeSurfaceProofReady(),
                "Agent 5 proof fixture with subsystem release proof should pass the Agent 5 predicate");
        require(strongProof.nativeLifecycleReady(), "strict lifecycle proof fixture should be lifecycle ready");
        require(strongProof.nativeCommandHostReady(), "strict command proof fixture should be command ready");
        require(strongProof.nativeConfigHostReady(), "strict config proof fixture should be config ready");
        require(strongProof.nativeNetworkHostReady(), "strict network proof fixture should be network ready");
    }

    private static EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport subsystemProofRuntimeCapability(
            boolean subsystemReleaseProof
    ) {
        List<String> requiredAgent5Surfaces =
                EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport.requiredAgent5AdapterCoreLiveProofSurfaces();
        return new EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport(
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                1,
                1,
                1,
                1,
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "bridgeEvidenceMatchesTrustedEntries", true,
                        "missingFromBridgeEvidence", List.of(),
                        "bridgeEvidenceWithoutTrustedEntry", List.of()
                ),
                true,
                0,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                1,
                0,
                1,
                1,
                1,
                1,
                true,
                subsystemReleaseProof,
                1,
                0,
                1,
                true,
                subsystemReleaseProof,
                1,
                1,
                true,
                subsystemReleaseProof,
                1,
                0,
                1,
                true,
                subsystemReleaseProof,
                1,
                1,
                1,
                requiredAgent5Surfaces,
                1,
                List.of("client_tick"),
                List.of("client_tick"),
                true,
                true,
                true,
                false,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                1,
                1,
                1,
                1,
                1
        );
    }

    private static void verifyInjectedLiveRuntimeCanPassGate(
            Path productRoot,
            EchoNativeProductLauncher launcher
    ) throws Exception {
        verifyInjectedLiveRuntimeCanPassGate(productRoot, launcher, false);
    }

    private static void verifyTrustedLiveBridgeCanPassGate(
            Path productRoot,
            EchoNativeProductLauncher launcher
    ) throws Exception {
        verifyInjectedLiveRuntimeCanPassGate(productRoot, launcher, true);
    }

    private static void verifyUnsupportedRuntimeHooksStayDiagnosticOnly(
            Path productRoot,
            EchoNativeProductLauncher launcher
    ) throws Exception {
        NativeLoaderLiveRuntimeAttachment trustedRuntime = new NativeLoaderLiveRuntimeAttachment(
                "echo_native_first_class_runtime",
                "native_product_live_runtime",
                "echo_native_first_class_runtime",
                true,
                false,
                List.of("minecraft_runtime", "client_render", "server_world"),
                Map.of(
                        "source", "product_launcher_qa_unsupported_runtime_hook",
                        "realMinecraftProcess", true,
                        "firstClassNativeRuntime", true,
                        "nativeRuntimeProcess", true,
                        "releaseRuntimeTrusted", true,
                        "purpose", "prove unsupported runtime hooks remain diagnostic-only"
                )
        );
        EchoNativeProductHookPlan unsupportedHookPlan = new EchoNativeProductHookPlan(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EchoNativeProductHookPlan.RuntimeHook(
                        "echocore",
                        "unsupported_live_surface",
                        "qa.unsupported.runtime_hook",
                        "mutate",
                        Map.of("source", "qa:unsupported-runtime-hook")
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        false,
                        true,
                        trustedRuntime,
                        trustedQaRuntimeBridge(),
                        trustedQaRegistryBridge(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        trustedQaClientBridge(),
                        unsupportedHookPlan
                )
        );
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport runtime = outcome.runtimeCapabilities();
        require(hasHookStatus(
                        outcome.hookReport(),
                        "runtime",
                        "unsupported_live_surface:qa.unsupported.runtime_hook",
                        dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.UNSUPPORTED.name()),
                "unsupported runtime hook must be reported as unsupported instead of live-mutated");
        require(!runtime.adapterCoreLiveRuntimeProofSurfaces().contains("unsupported_live_surface"),
                "unsupported runtime hook surface must not become AdapterCore live proof: "
                        + runtime.adapterCoreLiveRuntimeProofSurfaces());
        require(!runtime.adapterCoreLiveRuntimeProofSurfaces().contains("feedback"),
                "unsupported runtime hook must not create feedback AdapterCore live proof: "
                        + runtime.adapterCoreLiveRuntimeProofSurfaces());
        require(!outcome.hookReport().runtimeMutatedSurfaces().contains("feedback"),
                "unsupported runtime hook must not mutate feedback as diagnostic proof: "
                        + outcome.hookReport().runtimeMutatedSurfaces());
    }

    private static void verifyTrustedEvidenceWithoutBridgesFailsGate(
            Path productRoot,
            EchoNativeProductLauncher launcher
    ) throws Exception {
        NativeLoaderLiveRuntimeAttachment trustedRuntime = new NativeLoaderLiveRuntimeAttachment(
                "qa:trusted-live-minecraft-runtime",
                "echo_native_first_class_runtime",
                "native_product_live_runtime",
                true,
                false,
                List.of("minecraft_runtime", "client_render", "server_world"),
                Map.of(
                        "source", "product_launcher_qa_trusted_attachment",
                        "realMinecraftProcess", true,
                        "firstClassNativeRuntime", true,
                        "nativeRuntimeProcess", true,
                        "releaseRuntimeTrusted", true,
                        "purpose", "prove trusted product launcher live-runtime release gate"
                )
        );
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        NativeLoaderLiveRuntimeBridge.UNATTACHED,
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        EchoNativeProductHookPlan.empty()
                )
        );
        trace("trusted live runtime scenarios: trusted evidence without bridges returned accepted="
                + outcome.accepted() + " diagnostics=" + diagnosticCodes(outcome));
        require(!outcome.accepted(), "trusted evidence without bridge must not satisfy product live-runtime gate: " + diagnosticCodes(outcome));
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport runtimeWithoutBridge = outcome.runtimeCapabilities();
        require(runtimeWithoutBridge.liveRuntimeTrusted(), "trusted evidence launch must report release runtime trusted");
        require(!runtimeWithoutBridge.liveRuntimeBridgeAttached(), "trusted evidence launch must not report live bridge attached");
        require(!runtimeWithoutBridge.agent5LiveRuntimeSurfaceProofReady(),
                "trusted evidence without bridge must not satisfy Agent 5 live runtime surface proof");
        require(!runtimeWithoutBridge.fullReleaseRuntimeReady(), "trusted evidence without bridge must not satisfy fullReleaseRuntimeReady");
        require(hasDiagnostic(outcome.diagnostics(), "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE"),
                "trusted evidence without bridge must emit ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE: " + diagnosticCodes(outcome));
    }

    private static void verifyInjectedLiveRuntimeCanPassGate(
            Path productRoot,
            EchoNativeProductLauncher launcher,
            boolean trustedBridgeOnly
    ) throws Exception {
        if (!trustedBridgeOnly) {
        NativeLoaderLiveRuntimeAttachment syntheticRuntime = new NativeLoaderLiveRuntimeAttachment(
                "qa:synthetic-live-minecraft-runtime",
                "echo_native_live_minecraft_runtime",
                "native_product_live_runtime",
                true,
                false,
                List.of("minecraft_runtime", "client_render", "server_world"),
                Map.of(
                        "source", "product_launcher_qa_synthetic_attachment",
                        "realMinecraftProcess", false,
                        "purpose", "prove generic product launcher attachment contract"
                )
        );
        trace("trusted live runtime scenarios: synthetic attachment");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome syntheticOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        syntheticRuntime,
                        NativeLoaderLiveRuntimeBridge.UNATTACHED,
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "clientAttachment", "qa:synthetic-live-client"
                        ),
                        EchoNativeProductHookPlan.empty()
                )
        );
        require(!syntheticOutcome.accepted(), "synthetic live attachment must not satisfy product live-runtime gate: " + diagnosticCodes(syntheticOutcome));
        require(syntheticOutcome.loadedModules() == syntheticOutcome.totalModules(),
                "synthetic live attachment should still load all modules: loaded="
                        + syntheticOutcome.loadedModules() + "/" + syntheticOutcome.totalModules()
                        + " diagnostics=" + diagnosticCodes(syntheticOutcome));
        require(syntheticOutcome.registeredModules() == syntheticOutcome.totalModules(),
                "synthetic live attachment should still register all modules: registered="
                        + syntheticOutcome.registeredModules() + "/" + syntheticOutcome.totalModules()
                        + " diagnostics=" + diagnosticCodes(syntheticOutcome));
        require(syntheticOutcome.mutatedModules() == syntheticOutcome.totalModules(),
                "synthetic live attachment should still mutate all modules: mutated="
                        + syntheticOutcome.mutatedModules() + "/" + syntheticOutcome.totalModules()
                        + " diagnostics=" + diagnosticCodes(syntheticOutcome));
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport syntheticRuntimeReport = syntheticOutcome.runtimeCapabilities();
        require(!syntheticRuntimeReport.liveMinecraftAttached(), "synthetic attachment must not report a live Minecraft process");
        require(!syntheticRuntimeReport.liveClientAttached(), "synthetic attachment must not report a live client");
        require(!syntheticRuntimeReport.liveClientTrusted(), "synthetic client must not be release-client trusted");
        require(!syntheticRuntimeReport.fullReleaseRuntimeReady(), "synthetic attachment must not satisfy fullReleaseRuntimeReady");
        require(hasDiagnostic(syntheticOutcome.diagnostics(), "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE"),
                "synthetic attachment must emit ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE: " + diagnosticCodes(syntheticOutcome));

        trace("trusted live runtime scenarios: synthetic attachment verified");
        }
        NativeLoaderLiveRuntimeAttachment trustedRuntime = new NativeLoaderLiveRuntimeAttachment(
                "qa:trusted-live-minecraft-runtime",
                "echo_native_first_class_runtime",
                "native_product_live_runtime",
                true,
                false,
                List.of("minecraft_runtime", "client_render", "server_world"),
                Map.of(
                        "source", "product_launcher_qa_trusted_attachment",
                        "realMinecraftProcess", true,
                        "firstClassNativeRuntime", true,
                        "nativeRuntimeProcess", true,
                        "releaseRuntimeTrusted", true,
                        "purpose", "prove trusted product launcher live-runtime release gate"
                )
        );
        if (!trustedBridgeOnly) {
        verifyTrustedEvidenceWithoutBridgesFailsGate(productRoot, launcher);

        trace("trusted live runtime scenarios: report projection bridge");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome projectionOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        reportProjectionRuntimeBridge(),
                        trustedQaRegistryBridge(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        trustedQaClientBridge(),
                        reportProjectionHookPlan()
                )
        );
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport reportProjectionRuntime =
                projectionOutcome.runtimeCapabilities();
        require(!projectionOutcome.accepted(),
                "report-only runtime bridge evidence must not satisfy product live-runtime gate: "
                        + diagnosticCodes(projectionOutcome));
        require(reportProjectionRuntime.liveRuntimeBridgeAttached(),
                "report projection check must attach the projection runtime bridge");
        require(reportProjectionRuntime.liveRuntimeTrusted(),
                "report projection check must keep trusted attachment metadata isolated from live mutation proof");
        require(!reportProjectionRuntime.agent5LiveRuntimeSurfaceProofReady(),
                "report projection bridge must not satisfy Agent 5 live runtime surface proof");
        require(reportProjectionRuntime.adapterCoreLiveRuntimeProofRecordCount() == 0,
                "report projection bridge must not create AdapterCore live proof records");
        require(reportProjectionRuntime.adapterCoreLiveRuntimeProofSurfaces().isEmpty(),
                "report projection bridge must not create AdapterCore live proof surfaces");
        require(hasDiagnostic(projectionOutcome.diagnostics(), "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE"),
                "report projection bridge must emit ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE: "
                        + diagnosticCodes(projectionOutcome));

        trace("trusted live runtime scenarios: stale dispatch proof bridge");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome staleDispatchOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        staleDispatchRuntimeBridge(),
                        trustedQaRegistryBridge(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        trustedQaClientBridge(),
                        reportProjectionHookPlan()
                )
        );
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport staleDispatchRuntime =
                staleDispatchOutcome.runtimeCapabilities();
        require(!staleDispatchOutcome.accepted(),
                "stale dispatch proof bridge must not satisfy product live-runtime gate: "
                        + diagnosticCodes(staleDispatchOutcome));
        require(staleDispatchRuntime.liveRuntimeBridgeAttached(),
                "stale dispatch check must attach the stale runtime bridge");
        require(staleDispatchRuntime.liveRuntimeTrusted(),
                "stale dispatch check must keep trusted attachment metadata isolated from dispatch proof");
        require(!staleDispatchRuntime.agent5LiveRuntimeSurfaceProofReady(),
                "stale dispatch bridge must not satisfy Agent 5 live runtime surface proof");
        require(staleDispatchRuntime.adapterCoreMutatedRecordCount() > 0,
                "stale dispatch bridge should still mutate native AdapterCore records for negative proof coverage");
        require(staleDispatchRuntime.adapterCoreLiveRuntimeProofRecordCount() == 0,
                "stale dispatch bridge must not create AdapterCore live proof records");
        require(staleDispatchRuntime.commandLiveRuntimeMutationCount() == 0,
                "stale dispatch bridge must not count command host live mutations");
        require(staleDispatchRuntime.configLiveRuntimeMutationCount() == 0,
                "stale dispatch bridge must not count config host live mutations");
        require(staleDispatchRuntime.networkLiveRuntimeMutationCount() == 0,
                "stale dispatch bridge must not count network host live mutations");
        require(staleDispatchRuntime.lifecycleLiveRuntimeMutationCount() == 0,
                "stale dispatch bridge must not count lifecycle host live mutations");
        require(hasDiagnostic(staleDispatchOutcome.diagnostics(), "ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING"),
                "stale dispatch bridge must emit ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING: "
                        + diagnosticCodes(staleDispatchOutcome));

        trace("trusted live runtime scenarios: wrong subsystem surface proof bridge");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome wrongSubsystemSurfaceOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        wrongSubsystemSurfaceRuntimeBridge(),
                        trustedQaRegistryBridge(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        trustedQaClientBridge(),
                        reportProjectionHookPlan()
                )
        );
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport wrongSubsystemSurfaceRuntime =
                wrongSubsystemSurfaceOutcome.runtimeCapabilities();
        require(!wrongSubsystemSurfaceOutcome.accepted(),
                "wrong subsystem surface proof bridge must not satisfy product live-runtime gate: "
                        + diagnosticCodes(wrongSubsystemSurfaceOutcome));
        require(wrongSubsystemSurfaceRuntime.liveRuntimeBridgeAttached(),
                "wrong subsystem surface check must attach the runtime bridge");
        require(wrongSubsystemSurfaceRuntime.liveRuntimeTrusted(),
                "wrong subsystem surface check must keep trusted attachment metadata isolated from surface proof");
        require(!wrongSubsystemSurfaceRuntime.agent5LiveRuntimeSurfaceProofReady(),
                "wrong subsystem surface bridge must not satisfy Agent 5 live runtime surface proof");
        require(wrongSubsystemSurfaceRuntime.adapterCoreMutatedRecordCount() > 0,
                "wrong subsystem surface bridge should still mutate native AdapterCore records for negative proof coverage");
        require(wrongSubsystemSurfaceRuntime.commandLiveRuntimeMutationCount() == 0,
                "wrong subsystem surface bridge must not count command host live mutations");
        require(wrongSubsystemSurfaceRuntime.configLiveRuntimeMutationCount() == 0,
                "wrong subsystem surface bridge must not count config host live mutations");
        require(wrongSubsystemSurfaceRuntime.networkLiveRuntimeMutationCount() == 0,
                "wrong subsystem surface bridge must not count network host live mutations");
        require(wrongSubsystemSurfaceRuntime.lifecycleLiveRuntimeMutationCount() == 0,
                "wrong subsystem surface bridge must not count lifecycle host live mutations");
        require(hasDiagnostic(wrongSubsystemSurfaceOutcome.diagnostics(), "ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING"),
                "wrong subsystem surface bridge must emit ECHO-NATIVE-RELEASE-RUNTIME-LIVE-PROOF-MISSING: "
                        + diagnosticCodes(wrongSubsystemSurfaceOutcome));

        trace("trusted live runtime scenarios: registry bridge missing");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome registryBridgeMissingOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        trustedQaRuntimeBridge(),
                        NativeLoaderLiveRegistryBridge.UNATTACHED,
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        trustedQaClientBridge(),
                        EchoNativeProductHookPlan.empty()
                )
        );
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport registryBridgeMissing =
                registryBridgeMissingOutcome.runtimeCapabilities();
        require(!registryBridgeMissingOutcome.accepted(),
                "trusted runtime/client bridge without registry bridge must not satisfy product live-runtime gate: "
                        + diagnosticCodes(registryBridgeMissingOutcome));
        boolean registryMissingPreflightBlocked = !registryBridgeMissing.runtimeHostRegistered();
        require(registryBridgeMissing.liveRuntimeBridgeAttached() || registryMissingPreflightBlocked,
                "runtime bridge should be attached or launch should fail before runtime host registration "
                        + "in registry bridge missing check: " + registryBridgeMissing);
        require(registryBridgeMissing.liveClientBridgeAttached() || registryMissingPreflightBlocked,
                "client bridge should be attached or launch should fail before runtime host registration "
                        + "in registry bridge missing check: " + registryBridgeMissing);
        require(!registryBridgeMissing.fullReleaseRuntimeReady(),
                "trusted registry evidence without bridge must not satisfy fullReleaseRuntimeReady");
        require(hasDiagnostic(registryBridgeMissingOutcome.diagnostics(), "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE")
                        || hasDiagnostic(registryBridgeMissingOutcome.diagnostics(), "ECHO-NATIVE-RELEASE-REGISTRY-BRIDGE-MISSING")
                        || hasDiagnostic(registryBridgeMissingOutcome.diagnostics(), "ECHO-NATIVE-LIVE-BRIDGE-PROVIDER-INVALID"),
                "trusted registry evidence without bridge must emit a runtime/registry bridge diagnostic: "
                        + diagnosticCodes(registryBridgeMissingOutcome));

        trace("trusted live runtime scenarios: client bridge missing");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome clientBridgeMissingOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        trustedQaRuntimeBridge(),
                        trustedQaRegistryBridge(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", false,
                                "headlessClientSurface", true,
                                "realClientProcess", false,
                                "releaseClientTrusted", false,
                                "clientAttachment", "qa:client-bridge-missing"
                        ),
                        NativeLoaderLiveClientBridge.UNATTACHED,
                        EchoNativeProductHookPlan.empty()
                )
        );
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport clientBridgeMissing =
                clientBridgeMissingOutcome.runtimeCapabilities();
        require(!clientBridgeMissingOutcome.accepted(),
                "trusted runtime bridge without trusted client render proof must not satisfy product live-runtime gate: "
                        + diagnosticCodes(clientBridgeMissingOutcome));
        require(clientBridgeMissing.liveRuntimeBridgeAttached(), "runtime bridge should be attached in client bridge missing check");
        require(clientBridgeMissing.liveRegistryBridgeAttached(), "registry bridge should be attached in client bridge missing check");
        require(clientBridgeMissing.liveClientBridgeAttached(),
                "default product provider should attach a typed client route bridge in client render trust check");
        require(!clientBridgeMissing.liveClientTrusted(),
                "client render trust check must not report trusted live client evidence");
        require(!clientBridgeMissing.trustedClientRenderPipelineReady(),
                "client render trust check must not report a trusted render pipeline without the windowed client");
        require(!clientBridgeMissing.fullReleaseRuntimeReady(), "untrusted client render proof must not satisfy fullReleaseRuntimeReady");
        require(hasDiagnostic(clientBridgeMissingOutcome.diagnostics(), "ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE"),
                "untrusted client render proof must emit ECHO-NATIVE-PRODUCT-RUNTIME-INCOMPLETE: "
                        + diagnosticCodes(clientBridgeMissingOutcome));
        }

        trace("trusted live runtime scenarios: trusted bridges");
        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome bridgeOutcome = launcher.launch(
                productRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(
                        true,
                        true,
                        true,
                        trustedRuntime,
                        trustedQaRuntimeBridge(),
                        trustedQaRegistryBridge(),
                        Map.of(
                                "launcher", "echo-native-product-launcher-qa",
                                "liveClientAttached", true,
                                "headlessClientSurface", false,
                                "realClientProcess", true,
                                "releaseClientTrusted", true,
                                "clientAttachment", "qa:trusted-live-client"
                        ),
                        trustedQaClientBridge(),
                        new EchoNativeProductHookPlan(
                                List.of(new EchoNativeProductHookPlan.RegistryHook(
                                        "echocore",
                                        "item",
                                        "qa:trusted_bridge_item",
                                        Map.of(
                                                "implementationClass", "qa.TrustedBridgeItem",
                                                "source", "qa:trusted-live-registry-bridge"
                                        )
                                )),
                                List.of(new EchoNativeProductHookPlan.LifecycleHook(
                                        "echocore",
                                        "qa.trusted_bridge.lifecycle",
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(new EchoNativeProductHookPlan.EventSubscriptionHook(
                                        "echocore",
                                        "qa.trusted_bridge.event",
                                        "qa.trusted_bridge.handler",
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(new EchoNativeProductHookPlan.EventPublishHook(
                                        "echocore",
                                        "qa.trusted_bridge.event",
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(new EchoNativeProductHookPlan.CommandHook(
                                        "echocore",
                                        "qa.trusted_bridge.command",
                                        "commands",
                                        "qa.trusted_bridge.command_bridge",
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(new EchoNativeProductHookPlan.NetworkHook(
                                        "echocore",
                                        "qa.trusted_bridge.packet",
                                        "networking",
                                        "qa.trusted_bridge.network_bridge",
                                        List.of("qa.trusted_bridge.consumer"),
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(new EchoNativeProductHookPlan.ResourceHook(
                                        "echocore",
                                        "qa/trusted_bridge/resource.json",
                                        "data",
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(new EchoNativeProductHookPlan.ConfigHook(
                                        "echocore",
                                        "qa.trusted_bridge.config",
                                        "server.config",
                                        Map.of("source", "qa:trusted-live-runtime-bridge")
                                )),
                                List.of(
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "inventory",
                                                "echoashfallprotocol:trusted_bridge_item",
                                                "grant",
                                                Map.of(
                                                        "playerId", "player:trusted-live",
                                                        "count", 1,
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "player_state",
                                                "qa.trusted_bridge.player_state",
                                                "write",
                                                Map.of(
                                                        "playerId", "player:trusted-live",
                                                        "value", "linked",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "client_tick",
                                                "qa.trusted_bridge.client_tick",
                                                "end",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "render_layers",
                                                "qa.trusted_bridge.render_layer",
                                                "render",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "screen_events",
                                                "qa.trusted_bridge.screen",
                                                "open",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "keybinds",
                                                "qa.trusted_bridge.keybind",
                                                "press",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "world_blocks",
                                                "echoashfallprotocol:trusted_bridge_marker",
                                                "place",
                                                Map.of(
                                                        "dimension", "minecraft:overworld",
                                                        "x", 0,
                                                        "y", 80,
                                                        "z", 0,
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "world_state",
                                                "qa.trusted_bridge.world_state",
                                                "write",
                                                Map.of(
                                                        "dimension", "minecraft:overworld",
                                                        "value", "linked",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "structures",
                                                "echoashfallprotocol:trusted_bridge_structure",
                                                "place",
                                                Map.of(
                                                        "dimension", "minecraft:overworld",
                                                        "x", 2,
                                                        "y", 80,
                                                        "z", 2,
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "block_entities",
                                                "qa.trusted_bridge.block_entity",
                                                "write",
                                                Map.of(
                                                        "dimension", "minecraft:overworld",
                                                        "x", 2,
                                                        "y", 80,
                                                        "z", 3,
                                                        "key", "runtimeProof",
                                                        "value", "linked",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "capabilities",
                                                "qa.trusted_bridge.capability",
                                                "charge",
                                                Map.of(
                                                        "target", "player:trusted-live",
                                                        "value", "linked",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "missions",
                                                "qa.trusted_bridge.mission",
                                                "started",
                                                Map.of(
                                                        "objectiveKey", "runtimeProof",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "events",
                                                "qa.trusted_bridge.runtime_event",
                                                "publish",
                                                Map.of(
                                                        "payload", "qa=trusted_live_bridge",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "packets_hud",
                                                "qa.trusted_bridge.packet_hud",
                                                "send",
                                                Map.of(
                                                        "payload", "qa=trusted_live_bridge",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "save_data",
                                                "qa.trusted_bridge.runtime_save",
                                                "write",
                                                Map.of(
                                                        "value", "written_through_live_bridge",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "hud",
                                                "qa.trusted_bridge.hud",
                                                "notify",
                                                Map.of(
                                                        "channel", "qa.trusted_bridge.hud",
                                                        "message", "Trusted live HUD",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "commands",
                                                "qa.trusted_bridge.runtime_command",
                                                "terminal",
                                                Map.of(
                                                        "targetSurface", "terminal",
                                                        "targetBridge", "qa:trusted-live-runtime-bridge",
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "network_channels",
                                                "qa.trusted_bridge.runtime_packet",
                                                "sync",
                                                Map.of(
                                                        "surface", "terminal",
                                                        "sourceRuntimeTarget", "qa:trusted-live-runtime-bridge",
                                                        "consumers", List.of("client", "server"),
                                                        "source", "qa:trusted-live-runtime-bridge"
                                                )
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "config_reloads",
                                                "qa.trusted_bridge.runtime_config",
                                                "server",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "lifecycle_phases",
                                                "qa.trusted_bridge.runtime_lifecycle",
                                                "ready",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "resource_reloads",
                                                "qa.trusted_bridge.reload",
                                                "data_pack",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "save_hooks",
                                                "qa.trusted_bridge.save_hook",
                                                "world_save",
                                                Map.of("source", "qa:trusted-live-runtime-bridge")
                                        ),
                                        new EchoNativeProductHookPlan.RuntimeHook(
                                                "echocore",
                                                "server_client_sync",
                                                "qa.trusted_bridge.sync",
                                                "sync",
                                                Map.of("payload", "qa=trusted_live_bridge")
                                        )
                                ),
                                List.of(new EchoNativeProductHookPlan.ClientSurfaceHook(
                                        "echocore",
                                        "qa.trusted_bridge.surface",
                                        "hud_overlay",
                                        Map.of("source", "qa:trusted-live-client-bridge")
                                )),
                                List.of(),
                                List.of(),
                                List.of(new EchoNativeProductHookPlan.SaveDataHook(
                                        "qa.trusted_bridge.save",
                                        "written_through_live_bridge",
                                        false
                                ))
                        )
                )
        );
        trace("trusted live runtime scenarios: trusted bridges returned accepted="
                + bridgeOutcome.accepted() + " diagnostics=" + diagnosticCodes(bridgeOutcome));
        printPreWindowSummary(bridgeOutcome);
        require(bridgeOutcome.accepted(), "trusted live bridge should satisfy product live-runtime gate: "
                + diagnosticCodes(bridgeOutcome) + " runtime=" + bridgeOutcome.runtimeCapabilities());
        require(bridgeOutcome.hookReport().runtimeMutatedSurfaces().contains("saveData"),
                "trusted live bridge launch must mutate save data through product hooks");
        require(Boolean.TRUE.equals(bridgeOutcome.hookReport().registryHost().get("liveRegistryBridgeAttached")),
                "trusted live bridge launch must report live registry bridge attached");
        require(hasHook(bridgeOutcome.hookReport(), "registry", "item:qa:trusted_bridge_item"),
                "trusted live bridge launch must mutate registry hook through product hooks");
        require(Boolean.TRUE.equals(bridgeOutcome.hookReport().clientUiHost().get("liveClientBridgeAttached")),
                "trusted live bridge launch must report live client bridge attached");
        require(hasHook(bridgeOutcome.hookReport(), "client_ui", "qa.trusted_bridge.surface"),
                "trusted live bridge launch must mutate client UI surface through product hooks");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "inventory:echoashfallprotocol:trusted_bridge_item"),
                "trusted live bridge launch must mutate inventory runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "player_state:qa.trusted_bridge.player_state"),
                "trusted live bridge launch must mutate player state runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "client_tick:qa.trusted_bridge.client_tick"),
                "trusted live bridge launch must mutate client tick runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "render_layers:qa.trusted_bridge.render_layer"),
                "trusted live bridge launch must mutate render layer runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "screen_events:qa.trusted_bridge.screen"),
                "trusted live bridge launch must mutate screen event runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "keybinds:qa.trusted_bridge.keybind"),
                "trusted live bridge launch must mutate keybind runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "world_blocks:echoashfallprotocol:trusted_bridge_marker"),
                "trusted live bridge launch must mutate world block runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "world_state:qa.trusted_bridge.world_state"),
                "trusted live bridge launch must mutate world state runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "structures:echoashfallprotocol:trusted_bridge_structure"),
                "trusted live bridge launch must mutate structure runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "block_entities:qa.trusted_bridge.block_entity"),
                "trusted live bridge launch must mutate block entity runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "capabilities:qa.trusted_bridge.capability"),
                "trusted live bridge launch must mutate capability runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "missions:qa.trusted_bridge.mission"),
                "trusted live bridge launch must mutate mission runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "events:qa.trusted_bridge.runtime_event"),
                "trusted live bridge launch must mutate runtime event hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "packets_hud:qa.trusted_bridge.packet_hud"),
                "trusted live bridge launch must mutate packet/HUD runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "save_data:qa.trusted_bridge.runtime_save"),
                "trusted live bridge launch must mutate save data runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "hud:qa.trusted_bridge.hud"),
                "trusted live bridge launch must mutate HUD runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "commands:qa.trusted_bridge.runtime_command"),
                "trusted live bridge launch must mutate command runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "network_channels:qa.trusted_bridge.runtime_packet"),
                "trusted live bridge launch must mutate network runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "config_reloads:qa.trusted_bridge.runtime_config"),
                "trusted live bridge launch must mutate config reload runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "lifecycle_phases:qa.trusted_bridge.runtime_lifecycle"),
                "trusted live bridge launch must mutate lifecycle runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "resource_reloads:qa.trusted_bridge.reload"),
                "trusted live bridge launch must mutate resource reload runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "save_hooks:qa.trusted_bridge.save_hook"),
                "trusted live bridge launch must mutate save hook runtime hook through AdapterCore backend");
        require(hasHook(bridgeOutcome.hookReport(), "runtime", "server_client_sync:qa.trusted_bridge.sync"),
                "trusted live bridge launch must mutate server/client sync runtime hook through AdapterCore backend");
        require(bridgeOutcome.loadedModules() == bridgeOutcome.totalModules(), "trusted live bridge should still load all modules");
        require(bridgeOutcome.registeredModules() == bridgeOutcome.totalModules(), "trusted live bridge should still register all modules");
        require(bridgeOutcome.mutatedModules() == bridgeOutcome.totalModules(), "trusted live bridge should still mutate all modules");
        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport runtime = bridgeOutcome.runtimeCapabilities();
        require(runtime.fullReleaseRuntimeReady(), "trusted live attachment must satisfy fullReleaseRuntimeReady");
        require(runtime.agent5LiveRuntimeSurfaceProofReady(),
                "trusted live attachment must satisfy Agent 5 live runtime surface proof");
        require(runtime.liveMinecraftAttached(), "trusted live attachment must report live Minecraft attached");
        require(runtime.liveRuntimeTrusted(), "trusted live attachment must report release runtime trusted");
        require(runtime.liveRuntimeBridgeAttached(), "trusted live attachment must report live runtime bridge attached");
        require(runtime.liveRegistryBridgeAttached(), "trusted live attachment must report live registry bridge attached");
        require(runtime.registeredOnlyFirstClassRegistryKinds().isEmpty(),
                "trusted live attachment must not report registered-only registry blockers");
        require(runtime.registeredOnlyFirstClassRegistryIds().isEmpty(),
                "trusted live attachment must not report concrete registered-only registry blocker ids");
        require(runtime.registeredOnlyFirstClassRegistryIdsByKind().isEmpty(),
                "trusted live attachment must not report registered-only registry blocker id maps");
        require(runtime.failedFirstClassRegistryKinds().isEmpty(),
                "trusted live attachment must not report failed first-class registry blockers");
        require(runtime.failedFirstClassRegistryIds().isEmpty(),
                "trusted live attachment must not report concrete failed first-class registry ids");
        require(runtime.failedFirstClassRegistryIdsByKind().isEmpty(),
                "trusted live attachment must not report failed first-class registry id maps");
        require(runtime.untrustedMutationFirstClassRegistryKinds().isEmpty(),
                "trusted live attachment must not report untrusted registry mutation blockers");
        require(runtime.untrustedMutationReasonCounts().isEmpty(),
                "trusted live attachment must not report untrusted registry mutation reason counts");
        require(runtime.untrustedMutationReasonCountsByKind().isEmpty(),
                "trusted live attachment must not report per-kind untrusted registry mutation reason counts");
        require(runtime.registryBridgeMutationEvidenceReconciled(),
                "trusted live attachment must reconcile registry bridge aggregate evidence with trusted host entries");
        require(runtime.registryBridgeMutationReconciliationList("missingFromBridgeEvidence").isEmpty(),
                "trusted live attachment must not miss trusted entries from registry bridge aggregate evidence");
        require(runtime.registryBridgeMutationReconciliationList("bridgeEvidenceWithoutTrustedEntry").isEmpty(),
                "trusted live attachment must not report stale registry bridge aggregate ids");
        require(runtime.liveClientBridgeAttached(), "trusted live attachment must report live client bridge attached");
        require(runtime.nativeLoaderOwnsClientHostServices(),
                "trusted live attachment must report Native Loader-owned client host services");
        require(!runtime.neoForgeClientEventsCompatibilityAdaptersOnly(),
                "trusted live attachment must reject NeoForge compatibility-only client events");
        require(runtime.trustedClientRenderPipelineReady(),
                "trusted live attachment must report a trusted client render pipeline");
        require(runtime.firstClassNativeClientRenderPipeline(),
                "trusted live attachment must report a first-class native client render pipeline");
        require(runtime.nativeClientRenderProcess(),
                "trusted live attachment must report native client render process evidence");
        require(runtime.releaseClientRenderTrusted(),
                "trusted live attachment must report release-trusted client render evidence");
        require(runtime.lifecycleLiveRuntimeMutationCount() > 0,
                "trusted live attachment must mutate lifecycle through live runtime bridge");
        require(runtime.lifecycleMinecraftRuntimeAccessed(),
                "trusted live attachment lifecycle host must record Minecraft runtime access");
        require(runtime.lifecycleLiveRuntimeReleaseProofSatisfied(),
                "trusted live attachment lifecycle host must satisfy strict live release proof");
        require(runtime.commandLiveRuntimeMutationCount() > 0,
                "trusted live attachment must mutate command host through live runtime bridge");
        require(runtime.commandMinecraftRuntimeAccessed(),
                "trusted live attachment command host must record Minecraft runtime access");
        require(runtime.commandLiveRuntimeReleaseProofSatisfied(),
                "trusted live attachment command host must satisfy strict live release proof");
        require(runtime.configLiveRuntimeMutationCount() > 0,
                "trusted live attachment must mutate config host through live runtime bridge");
        require(runtime.configMinecraftRuntimeAccessed(),
                "trusted live attachment config host must record Minecraft runtime access");
        require(runtime.configLiveRuntimeReleaseProofSatisfied(),
                "trusted live attachment config host must satisfy strict live release proof");
        require(runtime.networkLiveRuntimeMutationCount() > 0,
                "trusted live attachment must mutate network host through live runtime bridge");
        require(runtime.networkMinecraftRuntimeAccessed(),
                "trusted live attachment network host must record Minecraft runtime access");
        require(runtime.networkLiveRuntimeReleaseProofSatisfied(),
                "trusted live attachment network host must satisfy strict live release proof");
        requireSubsystemEntryLiveEvidence(
                bridgeOutcome.hookReport().commandHost(),
                "commands",
                List.of("runtimeSurfaceSaveTouched", "runtimeSurfaceSaveMutated", "runtimeSaveDataTouched",
                        "liveSaveDataFileTouched", "runtimeSaveDataBackend", "saveFile")
        );
        requireSubsystemEntryLiveEvidence(
                bridgeOutcome.hookReport().networkHost(),
                "packets",
                List.of("runtimeSurfaceSaveTouched", "runtimeSurfaceSaveMutated", "runtimeSaveDataTouched",
                        "liveSaveDataFileTouched", "runtimeSaveDataBackend", "saveFile",
                        "runtimeSurfacePacketSent", "runtimeSurfacePacketMutated")
        );
        requireSubsystemEntryLiveEvidence(
                bridgeOutcome.hookReport().configHost(),
                "configs",
                List.of("runtimeSurfaceSaveTouched", "runtimeSurfaceSaveMutated", "runtimeSaveDataTouched",
                        "liveSaveDataFileTouched", "runtimeSaveDataBackend", "saveFile")
        );
        require(runtime.adapterCoreLiveRuntimeProofRecordCount() > 0,
                "trusted live attachment must record AdapterCore live runtime proof records");
        require(runtime.adapterCoreLiveRuntimeProofSurfaces().containsAll(
                        EchoNativeProductLauncher.EchoNativeProductRuntimeCapabilityReport.requiredAgent5AdapterCoreLiveProofSurfaces()),
                "trusted live attachment must record AdapterCore live proof for every Agent 5 surface: "
                        + runtime.adapterCoreLiveRuntimeProofSurfaces());
        require(!runtime.headlessClientSurface(), "trusted live attachment must not report headless client surface");
        require(!runtime.delegateRequired(), "trusted live attachment must not require a delegate");
        require(noBlockingDiagnostics(bridgeOutcome.diagnostics()),
                "trusted live attachment must not emit blocking diagnostics: " + diagnosticCodes(bridgeOutcome));
    }

    private static void verifyPackProfileOverridePackaging(Path sourceRoot, Path productRoot) throws Exception {
        Path qaRoot = productRoot.getParent().resolve("native-product-profile-override-qa").toAbsolutePath().normalize();
        Path packProfileRoot = qaRoot.resolve("profile");
        Path outputRoot = qaRoot.resolve("package");
        deleteTree(qaRoot);
        Files.createDirectories(packProfileRoot);
        Files.createDirectories(outputRoot.resolve("modules/stale/META-INF"));
        Files.writeString(outputRoot.resolve("modules/stale/META-INF/echo.mod.json"), "{}");

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("schema", "echo.pack.profile.v1");
        profile.put("id", "generic_profile_override_qa");
        profile.put("name", "Generic Profile Override QA");
        profile.put("status", "active");
        profile.put("rootModule", "echocore");
        profile.put("minecraftVersion", "26.1.2");
        profile.put("loader", Map.of("kind", "echo_native", "version", "0.1.0"));
        profile.put("requiredModules", List.of("echocore", "echoplatformcore", "echoadaptercore"));
        profile.put("requiredFeatures", List.of("echo.core", "platform.contracts", "adapter.echo_native"));
        profile.put("optionalFeatures", List.of());
        Files.writeString(packProfileRoot.resolve("echo.pack.json"), EchoNativeJson.write(profile));

        EchoNativeProductPackager.EchoNativeProductPackageOutcome packageOutcome =
                new EchoNativeProductPackager().packageProduct(sourceRoot, packProfileRoot, outputRoot);
        require(packageOutcome.packaged(), "profile override package must be complete: " + packageOutcome.diagnostics());
        require(!Files.exists(outputRoot.resolve("modules/stale")),
                "profile override packaging must clean stale module output before writing the new package");
        require("generic_profile_override_qa".equals(packageOutcome.packId()),
                "profile override package used the wrong pack id: " + packageOutcome.packId());
        require(packageOutcome.totalModules() == 3,
                "profile override package should select only the QA core module set: " + packageOutcome.totalModules());

        EchoNativeScanResult scanResult = new EchoNativeDescriptorScanner().scanProduct(outputRoot);
        require(scanResult.diagnostics().isEmpty(),
                "profile override package scan must not emit diagnostics: " + scanResult.diagnostics());
        require(scanResult.descriptors().stream().noneMatch(descriptor -> "echoashfallprotocol".equals(descriptor.id())),
                "profile override package must not inherit the workspace Ashfall root profile");
        verifyPackagedDescriptors(outputRoot, scanResult.descriptors());

        EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome = new EchoNativeProductLauncher().launch(
                outputRoot,
                new EchoNativeProductLauncher.EchoNativeProductLaunchOptions(true, true, false)
        );
        verifyModuleOutcome(outcome);
    }

    private static NativeLoaderLiveRuntimeBridge reportProjectionRuntimeBridge() {
        return new NativeLoaderLiveRuntimeBridge() {
            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "qa:report-projection-runtime-bridge";
            }

            @Override
            public boolean liveRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean minecraftRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean liveRuntimeMutationSupported() {
                return true;
            }

            @Override
            public Map<String, Object> runtimeEvidence() {
                return Map.of(
                        "bridgeId", bridgeId(),
                        "attached", true,
                        "liveRuntimeAccessed", true,
                        "minecraftRuntimeAccessed", true,
                        "liveRuntimeMutationSupported", true,
                        "liveRuntimeReleaseProofSatisfied", true,
                        "liveRuntimeSurfaceMutationSatisfied", true,
                        "reportProjectionOnly", true
                );
            }
        };
    }

    private static EchoNativeProductHookPlan reportProjectionHookPlan() {
        return new EchoNativeProductHookPlan(
                List.of(),
                List.of(new EchoNativeProductHookPlan.LifecycleHook(
                        "echocore",
                        "qa.report_projection.lifecycle",
                        Map.of("source", "qa:report-projection-runtime-bridge")
                )),
                List.of(),
                List.of(),
                List.of(new EchoNativeProductHookPlan.CommandHook(
                        "echocore",
                        "qa.report_projection.command",
                        "commands",
                        "qa.report_projection.command_bridge",
                        Map.of("source", "qa:report-projection-runtime-bridge")
                )),
                List.of(new EchoNativeProductHookPlan.NetworkHook(
                        "echocore",
                        "qa.report_projection.packet",
                        "networking",
                        "qa.report_projection.network_bridge",
                        List.of("qa.report_projection.consumer"),
                        Map.of("source", "qa:report-projection-runtime-bridge")
                )),
                List.of(),
                List.of(new EchoNativeProductHookPlan.ConfigHook(
                        "echocore",
                        "qa.report_projection.config",
                        "server.config",
                        Map.of("source", "qa:report-projection-runtime-bridge")
                )),
                List.of(
                        new EchoNativeProductHookPlan.RuntimeHook(
                                "echocore",
                                "inventory",
                                "echoashfallprotocol:report_projection_item",
                                "grant",
                                Map.of(
                                        "playerId", "player:report-projection",
                                        "count", 1,
                                        "source", "qa:report-projection-runtime-bridge"
                                )
                        ),
                        new EchoNativeProductHookPlan.RuntimeHook(
                                "echocore",
                                "client_tick",
                                "qa.report_projection.client_tick",
                                "end",
                                Map.of("source", "qa:report-projection-runtime-bridge")
                        )
                ),
                List.of(new EchoNativeProductHookPlan.ClientSurfaceHook(
                        "echocore",
                        "qa.report_projection.surface",
                        "hud_overlay",
                        Map.of("source", "qa:report-projection-client-bridge")
                )),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private static NativeLoaderLiveRuntimeBridge staleDispatchRuntimeBridge() {
        return new NativeLoaderLiveRuntimeBridge() {
            private static final String STALE_DISPATCH_ID = "qa:stale-product-readiness-dispatch-id";

            private final Map<String, Map<String, Object>> surfaceEvidence = new LinkedHashMap<>();

            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "qa:stale-dispatch-runtime-bridge";
            }

            @Override
            public boolean liveRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean minecraftRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean liveRuntimeMutationSupported() {
                return true;
            }

            @Override
            public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
                return surfaceEvidence.getOrDefault(surface, Map.of());
            }

            @Override
            public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
                surfaceEvidence.remove(surface);
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
                recordStaleSurfaceEvidence("inventory", "inventory:" + playerId + ":" + itemId + ":" + count);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
                stampStaleDispatch(payload, "client_tick:" + phase);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerCommand(
                    String moduleId,
                    String commandId,
                    String targetSurface,
                    String targetBridge,
                    Map<String, Object> evidence
            ) {
                stampStaleDispatch(evidence, "command:" + moduleId + ":" + commandId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerNetworkPacket(
                    String moduleId,
                    String packetId,
                    String surface,
                    String sourceRuntimeTarget,
                    List<String> consumers,
                    Map<String, Object> evidence
            ) {
                stampStaleDispatch(evidence, "network:" + moduleId + ":" + packetId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus reloadConfig(
                    String moduleId,
                    String configId,
                    String scope,
                    Map<String, Object> evidence
            ) {
                stampStaleDispatch(evidence, "config:" + moduleId + ":" + configId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
                stampStaleDispatch(evidence, "lifecycle:" + moduleId + ":" + phaseId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            private void recordStaleSurfaceEvidence(String surface, String operation) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                stampStaleDispatch(evidence, operation);
                evidence.put("liveRuntimeSurface", surface);
                surfaceEvidence.put(surface, Map.copyOf(evidence));
            }

            private void stampStaleDispatch(Map<String, Object> evidence, String operation) {
                if (evidence == null) {
                    return;
                }
                evidence.put("liveRuntimeDispatchProofSatisfied", true);
                evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
                evidence.put("liveRuntimeDispatchMutationSupported", true);
                evidence.put("liveRuntimeDispatchLiveMutation", true);
                evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
                evidence.put("liveRuntimeDispatchOperation", operation);
                evidence.put("liveRuntimeDispatchId", STALE_DISPATCH_ID);
            }
        };
    }

    private static NativeLoaderLiveRuntimeBridge wrongSubsystemSurfaceRuntimeBridge() {
        return new NativeLoaderLiveRuntimeBridge() {
            private final Map<String, Map<String, Object>> surfaceEvidence = new LinkedHashMap<>();
            private final Map<String, String> activeDispatchIds = new LinkedHashMap<>();

            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "qa:wrong-subsystem-surface-runtime-bridge";
            }

            @Override
            public boolean liveRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean minecraftRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean liveRuntimeMutationSupported() {
                return true;
            }

            @Override
            public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
                return surfaceEvidence.getOrDefault(surface, Map.of());
            }

            @Override
            public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
                surfaceEvidence.remove(surface);
                activeDispatchIds.put(surface, dispatchId == null ? "" : dispatchId);
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
                recordSurfaceEvidence("inventory", "inventory:" + playerId + ":" + itemId + ":" + count);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
                stampDirectDispatch(payload, "client_tick", "client_tick:" + phase);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerCommand(
                    String moduleId,
                    String commandId,
                    String targetSurface,
                    String targetBridge,
                    Map<String, Object> evidence
            ) {
                stampWrongSubsystemDispatch(evidence, "commands", "network_channels", "command:" + moduleId + ":" + commandId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerNetworkPacket(
                    String moduleId,
                    String packetId,
                    String surface,
                    String sourceRuntimeTarget,
                    List<String> consumers,
                    Map<String, Object> evidence
            ) {
                stampWrongSubsystemDispatch(evidence, "network_channels", "config_reloads", "network:" + moduleId + ":" + packetId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus reloadConfig(
                    String moduleId,
                    String configId,
                    String scope,
                    Map<String, Object> evidence
            ) {
                stampWrongSubsystemDispatch(evidence, "config_reloads", "commands", "config:" + moduleId + ":" + configId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
                stampWrongSubsystemDispatch(evidence, "lifecycle_phases", "events", "lifecycle:" + moduleId + ":" + phaseId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            private void recordSurfaceEvidence(String surface, String operation) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                stampDirectDispatch(evidence, surface, operation);
                surfaceEvidence.put(surface, Map.copyOf(evidence));
            }

            private void stampDirectDispatch(Map<String, Object> evidence, String surface, String operation) {
                if (evidence == null) {
                    return;
                }
                evidence.put("liveRuntimeDispatchProofSatisfied", true);
                evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
                evidence.put("liveRuntimeDispatchMutationSupported", true);
                evidence.put("liveRuntimeDispatchLiveMutation", true);
                evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
                evidence.put("liveRuntimeDispatchOperation", operation);
                evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
                evidence.put("liveRuntimeSurface", surface);
            }

            private void stampWrongSubsystemDispatch(
                    Map<String, Object> evidence,
                    String actualSurface,
                    String wrongSurface,
                    String operation
            ) {
                if (evidence == null) {
                    return;
                }
                evidence.put("liveRuntimeDispatchProofSatisfied", true);
                evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
                evidence.put("liveRuntimeDispatchMutationSupported", true);
                evidence.put("liveRuntimeDispatchLiveMutation", true);
                evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
                evidence.put("liveRuntimeDispatchOperation", operation);
                evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(actualSurface, ""));
                evidence.put("liveRuntimeSurface", wrongSurface);
            }
        };
    }

    private static NativeLoaderLiveRuntimeBridge trustedQaRuntimeBridge() {
        return new NativeLoaderLiveRuntimeBridge() {
            private final Map<String, Map<String, Object>> surfaceEvidence = new LinkedHashMap<>();
            private final Map<String, String> activeDispatchIds = new LinkedHashMap<>();

            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "qa:trusted-live-runtime-bridge";
            }

            @Override
            public boolean liveRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean minecraftRuntimeAccessed() {
                return true;
            }

            @Override
            public boolean liveRuntimeMutationSupported() {
                return true;
            }

            @Override
            public Map<String, Object> liveRuntimeSurfaceEvidence(String surface) {
                return surfaceEvidence.getOrDefault(surface, Map.of());
            }

            @Override
            public void beginLiveRuntimeSurfaceDispatch(String surface, String dispatchId) {
                surfaceEvidence.remove(surface);
                activeDispatchIds.put(surface, dispatchId);
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus grantItem(String playerId, String itemId, int count) {
                recordSurfaceEvidence("inventory", "inventory:" + playerId + ":" + itemId + ":" + count);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus removeItem(String playerId, String itemId, int count) {
                recordSurfaceEvidence("inventory", "inventory_remove:" + playerId + ":" + itemId + ":" + count);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus updatePlayerState(String playerId, String key, String value) {
                recordSurfaceEvidence("player_state", "player_state:" + playerId + ":" + key);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus placeBlock(String dimension, int x, int y, int z, String blockId) {
                recordSurfaceEvidence("world_blocks", "world_blocks:" + dimension + ":" + x + "," + y + "," + z);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus placeStructure(String dimension, String structureId, int x, int y, int z) {
                recordSurfaceEvidence("structures", "structures:" + dimension + ":" + structureId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus updateBlockEntity(String dimension, int x, int y, int z, String key, String value) {
                recordSurfaceEvidence("block_entities", "block_entities:" + dimension + ":" + x + "," + y + "," + z + ":" + key);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus updateCapability(String target, String capability, String value) {
                recordSurfaceEvidence("capabilities", "capabilities:" + target + ":" + capability);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus updateWorldState(String dimension, String key, String value) {
                recordSurfaceEvidence("world_state", "world_state:" + dimension + ":" + key);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus emitEvent(String eventType, String payload) {
                recordSurfaceEvidence("events", "events:" + eventType);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus sendPacketHud(String channel, String payload) {
                recordSurfaceEvidence("packets_hud", "packets_hud:" + channel);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus writeSaveData(String key, String value) {
                recordSurfaceEvidence("save_data", "save_data:" + key);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus deleteSaveData(String key) {
                recordSurfaceEvidence("save_data", "save_data_delete:" + key);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus emitHud(String channel, String message) {
                recordSurfaceEvidence("hud", "hud:" + channel);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus updateMission(String missionId, String phase, String objectiveKey) {
                recordSurfaceEvidence("missions", "missions:" + missionId + ":" + phase);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus emitFeedback(String source, String message) {
                recordSurfaceEvidence("feedback", "feedback:" + source);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus clientTick(String phase, Map<String, Object> payload) {
                stampLiveDispatch(payload, "client_tick:" + phase);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus renderLayer(String layerId, Map<String, Object> payload) {
                stampLiveDispatch(payload, "render_layers:" + layerId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus screenEvent(String screenId, String eventType, Map<String, Object> payload) {
                stampLiveDispatch(payload, "screen_events:" + screenId + ":" + eventType);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus keybind(String keybindId, String action, Map<String, Object> payload) {
                stampLiveDispatch(payload, "keybinds:" + keybindId + ":" + action);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerCommand(
                    String moduleId,
                    String commandId,
                    String targetSurface,
                    String targetBridge,
                    Map<String, Object> evidence
            ) {
                stampLiveDispatch(evidence, "command:" + moduleId + ":" + commandId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerNetworkPacket(
                    String moduleId,
                    String packetId,
                    String surface,
                    String sourceRuntimeTarget,
                    List<String> consumers,
                    Map<String, Object> evidence
            ) {
                stampLiveDispatch(evidence, "network:" + moduleId + ":" + packetId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus reloadConfig(
                    String moduleId,
                    String configId,
                    String scope,
                    Map<String, Object> evidence
            ) {
                stampLiveDispatch(evidence, "config:" + moduleId + ":" + configId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus reloadResources(
                    String moduleId,
                    String resourceId,
                    String scope,
                    Map<String, Object> evidence
            ) {
                stampLiveDispatch(evidence, "resource_reloads:" + moduleId + ":" + resourceId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus saveHook(String hookId, Map<String, Object> payload) {
                stampLiveDispatch(payload, "save_hooks:" + hookId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus lifecyclePhase(String moduleId, String phaseId, Map<String, Object> evidence) {
                stampLiveDispatch(evidence, "lifecycle:" + moduleId + ":" + phaseId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus publishRuntimeEvent(
                    String sourceModule,
                    String eventId,
                    Map<String, Object> payload,
                    dev.echo.nativeplatform.contracts.EchoNativeLoadStatus status
            ) {
                stampLiveDispatch(payload, "event:" + sourceModule + ":" + eventId);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus syncServerClient(String channel, String payload) {
                recordSurfaceEvidence("server_client_sync", "server_client_sync:" + channel);
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }

            private void stampLiveDispatch(Map<String, Object> evidence, String operation) {
                if (evidence == null) {
                    return;
                }
                evidence.put("liveRuntimeDispatchProofSatisfied", true);
                evidence.put("liveRuntimeDispatchMinecraftAccessed", true);
                evidence.put("liveRuntimeDispatchMutationSupported", true);
                evidence.put("liveRuntimeDispatchLiveMutation", true);
                evidence.put("liveRuntimeDispatchBridgeId", bridgeId());
                evidence.put("liveRuntimeDispatchOperation", operation);
                String surface = surfaceForOperation(operation);
                if (!surface.isBlank()) {
                    evidence.put("liveRuntimeSurface", surface);
                    evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
                    stampRuntimeSurfaceSaveEvidence(evidence, surface, operation);
                    surfaceEvidence.put(surface, Map.copyOf(evidence));
                }
            }

            private void recordSurfaceEvidence(String surface, String operation) {
                Map<String, Object> evidence = new LinkedHashMap<>();
                stampLiveDispatch(evidence, operation);
                evidence.put("liveRuntimeSurface", surface);
                evidence.put("liveRuntimeDispatchId", activeDispatchIds.getOrDefault(surface, ""));
                stampRuntimeSurfaceSaveEvidence(evidence, surface, operation);
                surfaceEvidence.put(surface, Map.copyOf(evidence));
            }

            private void stampRuntimeSurfaceSaveEvidence(Map<String, Object> evidence, String surface, String operation) {
                if (!List.of(
                        "commands",
                        "network_channels",
                        "config_reloads",
                        "lifecycle_phases",
                        "events",
                        "packets_hud",
                        "resource_reloads",
                        "save_data",
                        "hud",
                        "save_hooks",
                        "server_client_sync",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "inventory",
                        "player_state",
                        "missions",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities").contains(surface)) {
                    return;
                }
                if ("inventory".equals(surface)) {
                    evidence.put("runtimeInventoryTouched", true);
                    evidence.put("runtimeInventoryMutated", true);
                }
                if ("player_state".equals(surface)) {
                    evidence.put("runtimePlayerStateTouched", true);
                    evidence.put("runtimePlayerStateMutated", true);
                }
                if ("missions".equals(surface)) {
                    evidence.put("runtimePlayerStateTouched", true);
                    evidence.put("runtimePlayerStateMutated", true);
                    evidence.put("runtimeMissionStateTouched", true);
                    evidence.put("runtimeMissionStateMutated", true);
                }
                if ("world_blocks".equals(surface)) {
                    evidence.put("runtimeWorldBlockTouched", true);
                    evidence.put("runtimeWorldBlockMutated", true);
                }
                if ("structures".equals(surface)) {
                    evidence.put("runtimeStructurePlaced", true);
                    evidence.put("runtimeStructureMutated", true);
                }
                if ("block_entities".equals(surface)) {
                    evidence.put("runtimeBlockEntityTouched", true);
                    evidence.put("runtimeBlockEntityMutated", true);
                }
                if ("capabilities".equals(surface)) {
                    evidence.put("runtimeCapabilityTouched", true);
                    evidence.put("runtimeCapabilityMutated", true);
                }
                if (List.of(
                        "commands",
                        "network_channels",
                        "config_reloads",
                        "lifecycle_phases",
                        "events",
                        "resource_reloads",
                        "client_tick",
                        "render_layers",
                        "screen_events",
                        "keybinds",
                        "save_data",
                        "save_hooks",
                        "world_state",
                        "structures",
                        "server_client_sync").contains(surface)) {
                    evidence.put("runtimeSurfaceSaveTouched", true);
                    evidence.put("runtimeSurfaceSaveMutated", true);
                }
                if ("commands".equals(surface)) {
                    evidence.put("runtimeCommandRegistryTouched", true);
                    evidence.put("runtimeCommandRegistryMutated", true);
                }
                if ("network_channels".equals(surface) || "packets_hud".equals(surface)) {
                    evidence.put("runtimeSurfacePacketSent", true);
                    evidence.put("runtimeSurfacePacketMutated", true);
                }
                if ("packets_hud".equals(surface)) {
                    evidence.put("runtimePacketSent", true);
                    evidence.put("runtimePacketMutated", true);
                }
                if ("network_channels".equals(surface)) {
                    evidence.put("runtimeNetworkChannelTouched", true);
                    evidence.put("runtimeNetworkChannelMutated", true);
                    evidence.put("runtimeNetworkPacketSent", true);
                }
                if ("config_reloads".equals(surface)) {
                    evidence.put("runtimeConfigReloadTouched", true);
                    evidence.put("runtimeConfigReloadMutated", true);
                }
                if ("lifecycle_phases".equals(surface)) {
                    evidence.put("runtimeLifecyclePhaseTouched", true);
                    evidence.put("runtimeLifecyclePhaseMutated", true);
                }
                if ("hud".equals(surface)) {
                    evidence.put("runtimeHudNotificationPublished", true);
                    evidence.put("runtimeHudNotificationMutated", true);
                }
                if ("server_client_sync".equals(surface)) {
                    evidence.put("runtimeSurfacePacketSent", true);
                    evidence.put("runtimeSurfacePacketMutated", true);
                    evidence.put("runtimeServerClientSyncPacketSent", true);
                    evidence.put("runtimeServerClientSyncMutated", true);
                }
                if ("events".equals(surface)) {
                    evidence.put("runtimeSurfaceEventPublished", true);
                    evidence.put("runtimeSurfaceEventMutated", true);
                    evidence.put("runtimeEventTouched", true);
                    evidence.put("runtimeEventMutated", true);
                    evidence.put("runtimeEventPublished", true);
                }
                evidence.put("runtimeSaveDataTouched", true);
                evidence.put("liveSaveDataFileTouched", true);
                evidence.put("runtimeSaveDataBackend", "world_save_file");
                evidence.put("saveFile", "qa/trusted-live-world-save/" + operation.replace(':', '_') + ".properties");
            }

            private String surfaceForOperation(String operation) {
                if (operation == null || operation.isBlank()) {
                    return "";
                }
                String prefix = operation.contains(":") ? operation.substring(0, operation.indexOf(':')) : operation;
                return switch (prefix) {
                    case "inventory_remove" -> "inventory";
                    case "save_data_delete" -> "save_data";
                    case "command" -> "commands";
                    case "network" -> "network_channels";
                    case "config" -> "config_reloads";
                    case "lifecycle" -> "lifecycle_phases";
                    case "event" -> "events";
                    default -> prefix;
                };
            }
        };
    }

    private static NativeLoaderLiveRegistryBridge trustedQaRegistryBridge() {
        return new NativeLoaderLiveRegistryBridge() {
            private final java.util.Map<String, Map<String, Object>> records = new java.util.LinkedHashMap<>();

            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "qa:trusted-live-registry-bridge";
            }

            @Override
            public boolean firstClassNativeRegistry() {
                return true;
            }

            @Override
            public Map<String, Object> registryEvidence() {
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("bridgeId", bridgeId());
                evidence.put("attached", true);
                evidence.put("firstClassNativeRegistry", true);
                evidence.put("nativeRegistryProcess", true);
                evidence.put("releaseRegistryTrusted", true);
                evidence.put("nativeRegistryMutationSupported", true);
                evidence.put("productNativeRegistryTableMutated", !records.isEmpty());
                evidence.put("mutatedRecordCount", records.size());
                evidence.put("mutatedRecordIds", records.keySet().stream().sorted().toList());
                evidence.put("mutatedRecords", Map.copyOf(records));
                return Map.copyOf(evidence);
            }

            @Override
            public Map<String, Object> registryMutationRecord(String registry, String namespace, String id) {
                Map<String, Object> record = records.get(registry + ":" + namespace + ":" + id);
                return record == null ? Map.of() : record;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus register(
                    String registry,
                    String namespace,
                    String id,
                    String implementationClass,
                    Map<String, Object> properties
            ) {
                records.put(registry + ":" + namespace + ":" + id, Map.ofEntries(
                        Map.entry("registry", registry),
                        Map.entry("namespace", namespace),
                        Map.entry("id", id),
                        Map.entry("fullId", namespace + ":" + id),
                        Map.entry("status", dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED.name()),
                        Map.entry("bridgeId", bridgeId()),
                        Map.entry("liveRegistryMutationApplied", true),
                        Map.entry("nativeRegistryTableMutated", true),
                        Map.entry("firstClassNativeRegistry", true),
                        Map.entry("nativeRegistryProcess", true),
                        Map.entry("releaseRegistryTrusted", true),
                        Map.entry("nativeRegistryMutationSupported", true)
                ));
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }
        };
    }

    private static NativeLoaderLiveClientBridge trustedQaClientBridge() {
        return new NativeLoaderLiveClientBridge() {
            @Override
            public boolean attached() {
                return true;
            }

            @Override
            public String bridgeId() {
                return "qa:trusted-live-client-bridge";
            }

            @Override
            public boolean firstClassNativeClientRouteTable() {
                return true;
            }

            @Override
            public boolean nativeClientRouteProcess() {
                return true;
            }

            @Override
            public boolean releaseClientRouteTrusted() {
                return true;
            }

            @Override
            public boolean clientRouteMutationSupported() {
                return true;
            }

            @Override
            public boolean firstClassNativeClientRenderPipeline() {
                return true;
            }

            @Override
            public boolean nativeClientRenderProcess() {
                return true;
            }

            @Override
            public boolean releaseClientRenderTrusted() {
                return true;
            }

            @Override
            public boolean clientRenderMutationSupported() {
                return true;
            }

            @Override
            public boolean nativeLoaderOwnsClientHostServices() {
                return true;
            }

            @Override
            public boolean neoForgeClientEventsCompatibilityAdaptersOnly() {
                return false;
            }

            @Override
            public dev.echo.nativeplatform.contracts.EchoNativeLoadStatus registerSurface(
                    String moduleId,
                    String surfaceId,
                    String surfaceType,
                    Map<String, Object> config
            ) {
                return dev.echo.nativeplatform.contracts.EchoNativeLoadStatus.MUTATED;
            }
        };
    }

    private static boolean isPackagedDescriptor(Path productRoot, Path descriptorPath) {
        Path relative = productRoot.relativize(descriptorPath.toAbsolutePath().normalize());
        return relative.getNameCount() >= 4
                && "modules".equals(relative.getName(0).toString())
                && "META-INF".equals(relative.getName(relative.getNameCount() - 2).toString())
                && "echo.mod.json".equals(relative.getFileName().toString());
    }

    private static boolean noBlockingDiagnostics(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream()
                .noneMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                        || diagnostic.severity() == EchoNativeIssueSeverity.FATAL);
    }

    private static boolean hasDiagnostic(List<EchoNativeDiagnostic> diagnostics, String code) {
        return diagnostics.stream().anyMatch(diagnostic -> code.equals(diagnostic.code()));
    }

    private static boolean zipContains(Path jarPath, String entryName) {
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            return zip.getEntry(entryName) != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private static List<String> diagnosticCodes(EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome) {
        return outcome.diagnostics().stream()
                .map(diagnostic -> diagnostic.severity() + ":" + diagnostic.code())
                .toList();
    }

    private static void printPreWindowSummary(EchoNativeProductLauncher.EchoNativeProductLaunchOutcome outcome) {
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
    }

    private static boolean hasHook(
            EchoNativeProductLauncher.EchoNativeProductHookReport report,
            String surface,
            String hookId
    ) {
        return report.executions().stream().anyMatch(execution ->
                surface.equals(execution.surface())
                        && hookId.equals(execution.hookId())
                        && successfulHookStatus(execution.status().name()));
    }

    private static boolean hasHookExecution(
            EchoNativeProductLauncher.EchoNativeProductHookReport report,
            String surface,
            String hookId
    ) {
        return report.executions().stream().anyMatch(execution ->
                surface.equals(execution.surface())
                        && hookId.equals(execution.hookId()));
    }

    private static boolean hasHookStatus(
            EchoNativeProductLauncher.EchoNativeProductHookReport report,
            String surface,
            String hookId,
            String status
    ) {
        return report.executions().stream().anyMatch(execution ->
                surface.equals(execution.surface())
                        && hookId.equals(execution.hookId())
                        && status.equals(execution.status().name()));
    }

    private static boolean successfulHookStatus(String status) {
        return "MUTATED".equals(status) || "REGISTERED".equals(status) || "RESOLVED".equals(status);
    }

    private static void requireSubsystemEntryLiveEvidence(
            Map<String, Object> hostReport,
            String entryKey,
            List<String> requiredEvidenceKeys
    ) {
        List<Object> entries = list(hostReport.get(entryKey));
        require(!entries.isEmpty(), "trusted bridge host report must include entries for " + entryKey + ": " + hostReport);
        Map<String, Object> firstEntry = object(entries.get(0));
        Map<String, Object> evidence = object(firstEntry.get("evidence"));
        require(bool(evidence.get("subsystemLiveRuntimeDispatchProofSatisfied")),
                "trusted bridge entry must expose dispatch proof for " + entryKey + ": " + firstEntry);
        require(bool(evidence.get("liveMinecraftMutation")),
                "trusted bridge entry must expose live Minecraft mutation for " + entryKey + ": " + firstEntry);
        require("world_save_file".equals(string(evidence.get("runtimeSaveDataBackend"))),
                "trusted bridge entry must expose live world-save backend for " + entryKey + ": " + firstEntry);
        for (String key : requiredEvidenceKeys) {
            require(evidence.containsKey(key),
                    "trusted bridge entry evidence missing " + key + " for " + entryKey + ": " + firstEntry);
        }
    }

    private static List<Object> list(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        return List.copyOf(items);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(string(value));
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
