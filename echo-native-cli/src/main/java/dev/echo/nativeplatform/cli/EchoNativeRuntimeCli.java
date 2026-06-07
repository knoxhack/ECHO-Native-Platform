package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeReportStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.diagnostics.EchoNativeReportWriter;
import dev.echo.nativeplatform.loader.EchoNativeDescriptorScanner;
import dev.echo.nativeplatform.loader.EchoNativeModuleClassLoader;
import dev.echo.nativeplatform.loader.EchoNativeModuleLoadTruthGate;
import dev.echo.nativeplatform.loader.EchoNativeModuleLoader;
import dev.echo.nativeplatform.loader.EchoNativeRegistryHost;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.loader.NativeLoaderAdapterCoreBackend;
import dev.echo.nativeplatform.loader.NativeLoaderClientUiHost;
import dev.echo.nativeplatform.loader.NativeLoaderCommandHost;
import dev.echo.nativeplatform.loader.NativeLoaderConfigHost;
import dev.echo.nativeplatform.loader.NativeLoaderLifecycleEventHost;
import dev.echo.nativeplatform.loader.NativeLoaderMutationLedger;
import dev.echo.nativeplatform.loader.NativeLoaderNetworkHost;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeHost;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeHostContext;
import dev.echo.nativeplatform.loader.NativeLoaderServiceBridge;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI handler for real native runtime commands.
 *
 * <p>These commands perform actual module loading, service registration,
 * and state mutation. They are not report generators.</p>
 */
public final class EchoNativeRuntimeCli {
    private final EchoNativeDescriptorScanner scanner = new EchoNativeDescriptorScanner();
    private final EchoNativeModuleLoader moduleLoader = new EchoNativeModuleLoader();
    private final EchoNativeModuleLoadTruthGate truthGate = new EchoNativeModuleLoadTruthGate();

    public int discover(Path fixture) throws IOException {
        EchoNativeScanResult scanResult = scanner.scan(fixture);
        List<EchoNativeAddonDescriptor> descriptors = scanResult.descriptors();

        String packId = scanResult.packProfile().id();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("packId", packId);
        report.put("moduleCount", descriptors.size());
        report.put("modules", descriptors.stream().map(d -> Map.of(
                "id", d.id(),
                "name", d.name(),
                "version", d.version(),
                "role", d.role(),
                "kind", d.kind(),
                "entrypoint", d.entrypoint(),
                "hasNativeEntrypoint", d.access().containsKey("nativeEntrypoint")
                        && !String.valueOf(d.access().get("nativeEntrypoint")).isBlank()
        )).toList());

        Path reportPath = reportDir(fixture).resolve("native-discover.json");
        Files.createDirectories(reportPath.getParent());
        EchoNativeReportWriter.writeReport(reportPath, "echo.native.discover.v1",
                "echo-native-cli", packId, EchoNativeReportStatus.PASS,
                Map.of("message", "Discovered " + descriptors.size() + " modules"), List.of(), report);

        System.out.println("Discovered " + descriptors.size() + " modules for " + packId);
        for (EchoNativeAddonDescriptor d : descriptors) {
            System.out.println("  " + d.id() + " [" + d.kind() + "/" + d.role() + "]");
        }
        return 0;
    }

    public int resolve(Path fixture) throws IOException {
        EchoNativeScanResult scanResult = scanner.scan(fixture);
        List<EchoNativeAddonDescriptor> descriptors = scanResult.descriptors();
        Map<String, EchoNativeAddonDescriptor> byId = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor d : descriptors) {
            byId.put(d.id(), d);
        }

        List<Map<String, Object>> resolved = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            EchoNativeModuleDescriptor md = EchoNativeModuleDescriptor.fromAddon(descriptor);
            List<String> missing = new ArrayList<>();
            for (String req : md.requires()) {
                if (!byId.containsKey(req)) {
                    missing.add(req);
                }
            }
            resolved.add(Map.of(
                    "id", md.id(),
                    "classpathEntries", md.classpath().size(),
                    "dependenciesRequired", md.requires().size(),
                    "dependenciesResolved", md.requires().size() - missing.size(),
                    "missingDependencies", missing,
                    "resolved", missing.isEmpty()
            ));
            if (!missing.isEmpty()) {
                failures.add(md.id() + " missing: " + String.join(", ", missing));
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("packId", scanResult.packProfile().id());
        report.put("resolved", resolved);

        Path reportPath = reportDir(fixture).resolve("native-resolve.json");
        Files.createDirectories(reportPath.getParent());
        EchoNativeReportWriter.writeReport(reportPath, "echo.native.resolve.v1",
                "echo-native-cli", scanResult.packProfile().id(),
                failures.isEmpty() ? EchoNativeReportStatus.PASS : EchoNativeReportStatus.FAILED,
                Map.of("message", "Resolved " + descriptors.size() + " modules, " + failures.size() + " failures"),
                List.of(), report);

        System.out.println("Resolved " + descriptors.size() + " modules for " + scanResult.packProfile().id()
                + (failures.isEmpty() ? "" : ", " + failures.size() + " failures"));
        return failures.isEmpty() ? 0 : 1;
    }

    public int load(Path fixture) throws IOException {
        EchoNativeScanResult scanResult = scanner.scan(fixture);
        List<EchoNativeAddonDescriptor> descriptors = scanResult.descriptors();
        Map<String, EchoNativeAddonDescriptor> byId = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor d : descriptors) {
            byId.put(d.id(), d);
        }

        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        EchoNativeRegistryHost registryHost = new EchoNativeRegistryHost();
        serviceRegistry.register("echocore", EchoNativeRegistryHost.SERVICE_ID, registryHost,
                List.of("registry", "items", "blocks", "entities", "menus", "sounds", "particles", "creative_tabs", "commands", "data_components"),
                EchoNativeRegistryHost.class.getName());

        NativeLoaderRuntimeHostContext hostContext = new NativeLoaderRuntimeHostContext(
                "echo-native-loader-cli", "echocore", serviceRegistry, null);
        NativeLoaderRuntimeHost runtimeHost = new NativeLoaderRuntimeHost(hostContext);
        NativeLoaderServiceBridge serviceBridge = new NativeLoaderServiceBridge(serviceRegistry);
        NativeLoaderMutationLedger ledger = new NativeLoaderMutationLedger();
        NativeRuntimeServices runtimeServices = registerNativeRuntimeServices(
                serviceRegistry,
                runtimeHost,
                serviceBridge,
                ledger
        );
        NativeLoaderAdapterCoreBackend backend = runtimeServices.backend();

        NativeLoaderClientUiHost clientUiHost = new NativeLoaderClientUiHost();
        Map<String, Object> clientAssessment = clientAttachmentUnavailableAssessment();
        clientUiHost.attach(clientAssessment);
        serviceRegistry.register("echocore", NativeLoaderClientUiHost.SERVICE_ID, clientUiHost,
                List.of("ui", "surfaces", "client", "hud", "screens", "overlays"),
                NativeLoaderClientUiHost.class.getName());

        List<Map<String, Object>> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;
        int unsupported = 0;

        for (EchoNativeAddonDescriptor descriptor : byId.values()) {
            EchoNativeModuleLoadResult result = moduleLoader.load(descriptor, serviceRegistry, byId);
            EchoNativeModuleLoadTruthGate.TruthReport truth = truthGate.verify(result);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", result.descriptor().id());
            entry.put("claimedStatus", result.status().name());
            entry.put("honestStatus", truth.honestStatus().name());
            entry.put("loaded", truth.loaded());
            entry.put("registered", truth.registered());
            entry.put("mutated", truth.mutated());
            entry.put("statusAccurate", truth.statusAccurate());
            entry.put("passed", truth.passed());
            entry.put("phases", result.phases().stream().map(Enum::name).toList());
            entry.put("services", result.registeredServices().size());
            entry.put("mutations", result.mutations().size());
            entry.put("diagnostics", result.diagnostics());
            entry.put("failures", truth.failures());
            results.add(entry);

            if (truth.passed()) {
                passed++;
            } else if (result.status() == EchoNativeLoadStatus.UNSUPPORTED) {
                unsupported++;
            } else {
                failed++;
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("packId", scanResult.packProfile().id());
        report.put("total", descriptors.size());
        report.put("passed", passed);
        report.put("failed", failed);
        report.put("unsupported", unsupported);
        report.put("results", results);
        report.put("registryHost", registryHost.toReport());
        report.put("clientUiHost", Map.of(
                "attached", clientUiHost.attached(),
                "surfaceCount", clientUiHost.surfaceCount(),
                "surfaces", clientUiHost.surfaces(),
                "clientAssessment", clientUiHost.clientAssessment()
        ));
        report.put("serviceRegistry", serviceRegistry.registeredServices().stream()
                .map(s -> Map.of("module", s.moduleId(), "service", s.serviceId(), "surfaces", s.surfaces()))
                .toList());

        Path reportPath = reportDir(fixture).resolve("native-load.json");
        Files.createDirectories(reportPath.getParent());
        EchoNativeReportWriter.writeReport(reportPath, "echo.native.load.v1",
                "echo-native-cli", scanResult.packProfile().id(),
                failed == 0 ? EchoNativeReportStatus.PASS : EchoNativeReportStatus.FAILED,
                Map.of("message", "Loaded " + passed + " passed, " + failed + " failed, " + unsupported + " unsupported"),
                List.of(), report);

        System.out.println("Loaded " + descriptors.size() + " modules: " + passed + " passed, " + failed + " failed, " + unsupported + " unsupported");
        for (Map<String, Object> entry : results) {
            String status = Boolean.TRUE.equals(entry.get("passed")) ? "PASS" : "FAIL";
            System.out.println("  [" + status + "] " + entry.get("id") + " -> " + entry.get("honestStatus"));
        }
        return failed == 0 ? 0 : 1;
    }

    public int moduleStatus(Path fixture) throws IOException {
        EchoNativeScanResult scanResult = scanner.scan(fixture);
        Path loadReport = reportDir(fixture).resolve("native-load.json");
        if (!Files.isRegularFile(loadReport)) {
            System.out.println("No native-load.json report found. Run 'native load' first.");
            return 1;
        }
        Map<String, Object> loadData = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(loadReport)));
        Object results = loadData.get("results");

        System.out.println("Module status for " + scanResult.packProfile().id() + ":");
        if (results instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String id = String.valueOf(map.get("id"));
                    String honest = String.valueOf(map.get("honestStatus"));
                    String claimed = String.valueOf(map.get("claimedStatus"));
                    boolean accurate = Boolean.TRUE.equals(map.get("statusAccurate"));
                    System.out.println("  " + id + ": " + honest + " (claimed: " + claimed + ")"
                            + (accurate ? "" : " [MISMATCH]"));
                }
            }
        }
        return 0;
    }

    public int proveLive(Path fixture) throws IOException {
        EchoNativeScanResult scanResult = scanner.scan(fixture);
        List<EchoNativeAddonDescriptor> descriptors = scanResult.descriptors();
        Map<String, EchoNativeAddonDescriptor> byId = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor d : descriptors) {
            byId.put(d.id(), d);
        }

        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        EchoNativeRegistryHost registryHost = new EchoNativeRegistryHost();
        serviceRegistry.register("echocore", EchoNativeRegistryHost.SERVICE_ID, registryHost,
                List.of("registry", "items", "blocks", "entities", "menus", "sounds", "particles", "creative_tabs", "commands", "data_components"),
                EchoNativeRegistryHost.class.getName());

        Path proveLiveSaves = Path.of(System.getProperty("java.io.tmpdir"), "echo-prove-live-saves-" + System.currentTimeMillis());
        NativeLoaderRuntimeHostContext hostContext = new NativeLoaderRuntimeHostContext(
                "echo-native-loader-prove-live", "echocore", serviceRegistry,
                proveLiveSaves);
        NativeLoaderRuntimeHost runtimeHost = new NativeLoaderRuntimeHost(hostContext);
        NativeLoaderServiceBridge serviceBridge = new NativeLoaderServiceBridge(serviceRegistry);
        NativeLoaderMutationLedger ledger = new NativeLoaderMutationLedger();
        NativeRuntimeServices runtimeServices = registerNativeRuntimeServices(
                serviceRegistry,
                runtimeHost,
                serviceBridge,
                ledger
        );
        NativeLoaderAdapterCoreBackend backend = runtimeServices.backend();

        // Prove live mutations
        EchoNativeLoadStatus itemStatus = backend.grantItem("player1", "echo:test_item", 1).status();
        EchoNativeLoadStatus blockStatus = backend.placeBlock("minecraft:overworld", 0, 64, 0, "echo:test_block").status();
        EchoNativeLoadStatus saveStatus = backend.writeSaveData("echo.prove.live", "true").status();
        EchoNativeLoadStatus hudStatus = backend.emitHud("prove_live", "Native Loader is live").status();
        EchoNativeLoadStatus commandStatus = backend.registerCommand(
                "echo-native-loader-prove-live",
                "echo.prove_live",
                "commands",
                "native_cli",
                Map.of("source", "native.prove-live")).status();
        EchoNativeLoadStatus packetStatus = backend.registerNetworkPacket(
                "echo-native-loader-prove-live",
                "echo:prove_live",
                "networking",
                "native_cli",
                List.of("native_cli"),
                Map.of("source", "native.prove-live")).status();
        EchoNativeLoadStatus configStatus = backend.reloadConfig(
                "echo-native-loader-prove-live",
                "prove-live",
                "server.config",
                Map.of("source", "native.prove-live")).status();
        EchoNativeLoadStatus lifecycleStatus = backend.lifecyclePhase(
                "echo-native-loader-prove-live",
                "prove_live",
                Map.of("source", "native.prove-live")).status();
        EchoNativeLoadStatus runtimeEventStatus = backend.publishRuntimeEvent(
                "echo-native-loader-prove-live",
                "echo.prove_live",
                Map.of("source", "native.prove-live"),
                EchoNativeLoadStatus.MUTATED.name()).status();

        boolean nativeMirrorMutationsProven = itemStatus == EchoNativeLoadStatus.MUTATED
                && blockStatus == EchoNativeLoadStatus.MUTATED
                && saveStatus == EchoNativeLoadStatus.MUTATED
                && hudStatus == EchoNativeLoadStatus.MUTATED
                && commandStatus == EchoNativeLoadStatus.MUTATED
                && packetStatus == EchoNativeLoadStatus.MUTATED
                && configStatus == EchoNativeLoadStatus.MUTATED
                && lifecycleStatus == EchoNativeLoadStatus.MUTATED
                && runtimeEventStatus == EchoNativeLoadStatus.MUTATED;
        Map<String, Object> runtimeReport = runtimeHost.runtimeHostReport();
        boolean proven = nativeMirrorMutationsProven
                && Boolean.TRUE.equals(runtimeReport.get("liveRuntimeReleaseProofSatisfied"))
                && ledger.liveRuntimeProofRecordCount() > 0
                && runtimeServices.commandHost().liveRuntimeMutationCount() > 0
                && runtimeServices.networkHost().liveRuntimeMutationCount() > 0
                && runtimeServices.configHost().liveRuntimeMutationCount() > 0
                && runtimeServices.lifecycleEventHost().liveRuntimeMutationCount() > 0;

        // Attempt to load at least one module and verify it triggers real registry/content registration
        boolean moduleProven = false;
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            EchoNativeModuleDescriptor md = EchoNativeModuleDescriptor.fromAddon(descriptor);
            if (!md.hasEntrypoint()) continue;
            try (EchoNativeModuleClassLoader classLoader = new EchoNativeModuleClassLoader(md.classpath(),
                    Thread.currentThread().getContextClassLoader())) {
                Class<?> type = classLoader.loadClass(md.entrypoint());
                if (EchoNativeModuleEntrypoint.class.isAssignableFrom(type)) {
                    Constructor<?> constructor = type.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    EchoNativeModuleEntrypoint entrypoint = (EchoNativeModuleEntrypoint) constructor.newInstance();
                    EchoNativeModuleLoadContext context = new EchoNativeModuleLoadContext(md, serviceRegistry,
                            Map.of("loader", "echo-native-loader-prove-live"));
                    entrypoint.registerContent(context);
                    if (!context.mutations().isEmpty()) {
                        moduleProven = true;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("packId", scanResult.packProfile().id());
        report.put("proven", proven);
        report.put("nativeMirrorMutationsProven", nativeMirrorMutationsProven);
        report.put("liveRuntimeReleaseProofSatisfied", runtimeReport.get("liveRuntimeReleaseProofSatisfied"));
        report.put("mirrorOnlyReleaseProof", runtimeReport.get("mirrorOnlyReleaseProof"));
        report.put("liveRuntimeBridgeAttached", runtimeReport.get("liveRuntimeBridgeAttached"));
        report.put("liveRuntimeProofRecordCount", ledger.liveRuntimeProofRecordCount());
        report.put("moduleProven", moduleProven);
        report.put("itemMutation", itemStatus.name());
        report.put("blockMutation", blockStatus.name());
        report.put("saveMutation", saveStatus.name());
        report.put("hudMutation", hudStatus.name());
        report.put("commandMutation", commandStatus.name());
        report.put("packetMutation", packetStatus.name());
        report.put("configMutation", configStatus.name());
        report.put("lifecycleMutation", lifecycleStatus.name());
        report.put("runtimeEventMutation", runtimeEventStatus.name());
        report.put("commandHost", runtimeServices.commandHost().toReport());
        report.put("networkHost", runtimeServices.networkHost().toReport());
        report.put("configHost", runtimeServices.configHost().toReport());
        report.put("lifecycleEventHost", runtimeServices.lifecycleEventHost().toReport());
        report.put("mutationRecords", ledger.toReport());
        report.put("runtimeHostSnapshot", runtimeHost.snapshot());
        report.put("runtimeHostReport", runtimeReport);
        report.put("registryHost", registryHost.toReport());

        Path reportPath = reportDir(fixture).resolve("native-prove-live.json");
        Files.createDirectories(reportPath.getParent());
        EchoNativeReportWriter.writeReport(reportPath, "echo.native.prove_live.v1",
                "echo-native-cli", scanResult.packProfile().id(),
                proven ? EchoNativeReportStatus.PASS : EchoNativeReportStatus.FAILED,
                Map.of("message", proven
                        ? "Live proof passed: live runtime mutations verified"
                        : "Live proof failed: native mirror mutation is not live runtime release proof"),
                List.of(), report);

        System.out.println(proven ? "PROOF PASSED: Native Loader produced live runtime mutations."
                : "PROOF FAILED: Native Loader only produced native mirror mutations; no live runtime proof was attached.");
        System.out.println("  item: " + itemStatus + " | block: " + blockStatus
                + " | save: " + saveStatus + " | hud: " + hudStatus);
        System.out.println("  command: " + commandStatus + " | packet: " + packetStatus
                + " | config: " + configStatus + " | lifecycle: " + lifecycleStatus
                + " | event: " + runtimeEventStatus);
        System.out.println("  module content proven: " + moduleProven);
        return proven ? 0 : 1;
    }

    private static NativeRuntimeServices registerNativeRuntimeServices(
            EchoNativeServiceRegistry serviceRegistry,
            NativeLoaderRuntimeHost runtimeHost,
            NativeLoaderServiceBridge serviceBridge,
            NativeLoaderMutationLedger ledger
    ) {
        NativeLoaderCommandHost commandHost = new NativeLoaderCommandHost();
        NativeLoaderNetworkHost networkHost = new NativeLoaderNetworkHost();
        NativeLoaderConfigHost configHost = new NativeLoaderConfigHost();
        NativeLoaderLifecycleEventHost lifecycleEventHost = new NativeLoaderLifecycleEventHost();
        NativeLoaderAdapterCoreBackend backend = new NativeLoaderAdapterCoreBackend(
                runtimeHost,
                serviceBridge,
                ledger,
                commandHost,
                networkHost,
                configHost,
                lifecycleEventHost
        );
        serviceRegistry.register("echocore", NativeLoaderCommandHost.SERVICE_ID, commandHost,
                List.of("commands", "command", "server.commands", "command.queue", "adaptercore.native_command"),
                NativeLoaderCommandHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderNetworkHost.SERVICE_ID, networkHost,
                List.of("network", "networking", "network_channels", "network_payload", "packet", "payload",
                        "packets", "channels", "adaptercore.native_runtime_packet", "packets_hud",
                        "server_client_sync"),
                NativeLoaderNetworkHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderConfigHost.SERVICE_ID, configHost,
                List.of("config", "configs", "configuration", "config_schema", "config_reloads",
                        "client.config", "server.config"),
                NativeLoaderConfigHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderLifecycleEventHost.LIFECYCLE_SERVICE_ID, lifecycleEventHost,
                List.of("lifecycle", "lifecycle_phases", "lifecycle.phases", "events", "adaptercore"),
                NativeLoaderLifecycleEventHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderLifecycleEventHost.EVENT_SERVICE_ID, lifecycleEventHost,
                List.of("events", "event", "runtime.spine", "adaptercore"),
                NativeLoaderLifecycleEventHost.class.getName());
        serviceRegistry.register("echocore", NativeLoaderAdapterCoreBackend.SERVICE_ID, backend,
                List.of("adaptercore", "inventory", "player_state", "world_blocks", "world_state", "structures",
                        "block_entities", "capabilities", "events", "packets_hud", "hud", "save_data",
                        "commands", "network_channels", "config_reloads", "resource_reloads", "save_hooks",
                        "lifecycle_phases", "server_client_sync"),
                NativeLoaderAdapterCoreBackend.class.getName());
        return new NativeRuntimeServices(backend, commandHost, networkHost, configHost, lifecycleEventHost);
    }

    private static Map<String, Object> clientAttachmentUnavailableAssessment() {
        Map<String, Object> assessment = new LinkedHashMap<>();
        assessment.put("accepted", false);
        assessment.put("minecraftClientReady", false);
        assessment.put("dashboardScreenCompiled", false);
        assessment.put("clientThreadAccepted", false);
        assessment.put("physicalHotkeyPollingReady", false);
        assessment.put("windowHandlePresent", false);
        assessment.put("compiledScreenClass", "");
        assessment.put("expectedScreenClass", "");
        assessment.put("screenClassMatches", false);
        assessment.put("effect", "live_client_attachment:unavailable_cli_runtime");
        assessment.put("adapterCoreBridge", true);
        assessment.put("serviceCodeExecuted", true);
        return Map.copyOf(assessment);
    }

    private static Path reportDir(Path fixture) {
        return fixture.resolve("reports").resolve("echo-native").resolve("runtime");
    }

    private record NativeRuntimeServices(
            NativeLoaderAdapterCoreBackend backend,
            NativeLoaderCommandHost commandHost,
            NativeLoaderNetworkHost networkHost,
            NativeLoaderConfigHost configHost,
            NativeLoaderLifecycleEventHost lifecycleEventHost
    ) {
    }
}
