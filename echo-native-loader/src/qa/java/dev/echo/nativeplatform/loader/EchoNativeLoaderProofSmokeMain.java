package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadResult;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeLoaderProofSmokeMain {
    private EchoNativeLoaderProofSmokeMain() {
    }

    public static void main(String[] args) throws Exception {
        Path fixtureRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("fixtures/native-loader-sample").toAbsolutePath().normalize();
        Path output = args.length > 1
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : Path.of("build/native-loader-proof/native-loader-proof.json").toAbsolutePath().normalize();

        EchoNativeScanResult scan = new EchoNativeDescriptorScanner().scan(fixtureRoot);
        EchoNativeAddonDescriptor descriptor = scan.descriptors().stream()
                .filter(candidate -> "echoashfallnativeproof".equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sample native proof descriptor was not discovered."));

        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();
        EchoNativeModuleLoader moduleLoader = new EchoNativeModuleLoader();
        EchoNativeModuleLoadResult loadResult = moduleLoader.load(descriptor, serviceRegistry);
        require(loadResult.loaded(), "Sample module class did not load.");
        require(loadResult.registered(), "Sample module did not register services.");
        EchoNativeModuleLoadResult releaseFallbackRejection = new EchoNativeModuleLoader()
                .loadRelease(releaseFallbackDescriptor(descriptor), new EchoNativeServiceRegistry());
        require(releaseFallbackRejection.status() == EchoNativeLoadStatus.FAILED,
                "Release loading must reject compatibility classpath fallback.");
        require(!releaseFallbackRejection.loaded(),
                "Release fallback rejection must occur before class loading.");
        EchoNativeModuleLoadResult releaseSideGateRejection = new EchoNativeModuleLoader()
                .loadRelease(sideGateDescriptor(descriptor), new EchoNativeServiceRegistry(), EchoNativeRuntimeSide.SERVER);
        require(releaseSideGateRejection.status() == EchoNativeLoadStatus.FAILED,
                "Release loading must reject client-only modules on a server host.");
        require(!releaseSideGateRejection.loaded(),
                "Release side-gate rejection must occur before class loading.");
        EchoNativeLoadedModuleStateStore.StoredState loadedModuleState = new EchoNativeLoadedModuleStateStore()
                .write(output.getParent().resolve("loaded-modules"), loadResult);

        NativeLoaderMutationLedger ledger = new NativeLoaderMutationLedger();
        NativeLoaderRuntimeHost host = new NativeLoaderRuntimeHost(new NativeLoaderRuntimeHostContext(
                scan.packProfile().id(),
                descriptor.id(),
                serviceRegistry,
                null,
                "echoashfallnativeproof:native_loader_runtime_host",
                true
        ));
        NativeLoaderServiceBridge serviceBridge = new NativeLoaderServiceBridge(serviceRegistry);
        NativeLoaderAdapterCoreBackend backend = new NativeLoaderAdapterCoreBackend(
                host,
                serviceBridge,
                ledger
        );
        serviceRegistry.register(
                "echo-native-loader",
                NativeLoaderAdapterCoreBackend.SERVICE_ID,
                backend,
                List.of(
                        "inventory",
                        "player_state",
                        "world_blocks",
                        "world_state",
                        "structures",
                        "block_entities",
                        "capabilities",
                        "events",
                        "packets_hud",
                        "hud",
                        "save_data"
                ),
                NativeLoaderAdapterCoreBackend.class.getName()
        );

        backend.grantItem("player:local-proof", "echoashfallprotocol:drop_pod_beacon", 1);
        backend.updatePlayerState("player:local-proof", "spawn_phase", "drop_pod_linked");
        backend.placeBlock("minecraft:overworld", 0, 80, 0, "echoashfallprotocol:drop_pod_marker");
        backend.updateWorldState("minecraft:overworld", "ashfall.crash_zone", "materialized");
        backend.placeStructure("minecraft:overworld", "echoashfallprotocol:starter_crash_zone", 0, 80, 0);
        backend.updateBlockEntity("minecraft:overworld", 0, 80, 0, "telemetry", "online");
        backend.updateCapability("player:local-proof", "echoashfallprotocol:radiation_resistance", "1");
        backend.emitEvent("ashfall.first_spawn", "drop_pod_beacon_granted");
        backend.sendPacketHud("ashfall.hud.packet", "Drop pod packet telemetry linked.");
        backend.writeSaveData("ashfall.first_spawn.drop_pod", "complete");
        backend.emitHud("ashfall.welcome", "Drop pod telemetry linked.");

        NativeLoaderMutationLedger.MutationRecord noOpBlock = backend.placeBlock(
                "minecraft:overworld",
                0,
                80,
                0,
                "echoashfallprotocol:drop_pod_marker"
        );
        NativeLoaderMutationLedger.MutationRecord noOpSave = backend.writeSaveData(
                "ashfall.first_spawn.drop_pod",
                "complete"
        );

        boolean mutated = ledger.records().stream()
                .filter(record -> record != noOpBlock && record != noOpSave)
                .allMatch(record -> record.status() == EchoNativeLoadStatus.MUTATED);
        boolean noOpMutationGuard = noOpBlock.status() == EchoNativeLoadStatus.RESOLVED
                && noOpSave.status() == EchoNativeLoadStatus.RESOLVED;
        require(mutated, "Native Loader proof did not mutate every required host surface.");
        require(noOpMutationGuard, "Native Loader backend must not report MUTATED when state is unchanged.");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.native.loader_proof.v1");
        report.put("runtimeLane", "Native Loader");
        report.put("laneRole", "primary future mod loader");
        report.put("fallbackLane", "NeoForge compatibility backend");
        report.put("parityLane", "Standalone Runtime parity/runtime harness");
        report.put("proofScope", "native_loader_host_harness");
        report.put("status", "MUTATED");
        report.put("fixtureRoot", fixtureRoot.toString().replace('\\', '/'));
        report.put("module", EchoNativeModuleLoader.toReport(loadResult));
        report.put("releaseFallbackRejection", EchoNativeModuleLoader.toReport(releaseFallbackRejection));
        Map<String, Object> releaseSideGateReport = new LinkedHashMap<>(
                EchoNativeModuleLoader.toReport(releaseSideGateRejection)
        );
        releaseSideGateReport.put("releaseHostSide", EchoNativeRuntimeSide.SERVER.name());
        report.put("releaseSideGateRejection", releaseSideGateReport);
        report.put("loadedModuleStatePath", loadedModuleState.normalizedPath());
        report.put("loadedModuleState", loadedModuleState.state());
        report.put("serviceBridge", serviceBridge.toReport(NativeLoaderAdapterCoreBackend.SERVICE_ID));
        report.put("mutationLedger", ledger.toReport());
        report.put("typedHostMutationReceipts", ledger.receipts().stream()
                .map(receipt -> receipt.toReport())
                .toList());
        report.put("hostSnapshot", host.snapshot());
        report.put("requiredAdapterCoreSurfaces", List.of(
                "inventory",
                "player_state",
                "world_blocks",
                "world_state",
                "structures",
                "block_entities",
                "capabilities",
                "events",
                "packets_hud",
                "hud",
                "save_data"
        ));
        EchoNativeLiveClientProof liveProof = new EchoNativeLiveClientProof();
        liveProof.satisfyModuleLoad(descriptor.id(), loadResult.loadedClassName());
        liveProof.satisfyBackendLoaded(serviceBridge.hasService(NativeLoaderAdapterCoreBackend.SERVICE_ID));
        for (NativeLoaderMutationLedger.MutationRecord record : ledger.records()) {
            if (record.status() == EchoNativeLoadStatus.MUTATED) {
                liveProof.satisfyMutation(record.surface());
            }
        }
        liveProof.block("nativeLoaderStartsClient", "Local harness does not launch Minecraft client.");
        liveProof.block("bootstrapEnteredLiveClient", "Local harness does not attach to live client.");
        liveProof.block("uiHostOpenedOrAttached", "Local harness has no live UI host.");
        report.put("liveClientProof", liveProof.toReport());
        EchoNativeLoadStatus shutdownStatus = moduleLoader.shutdown(loadResult, serviceRegistry);
        EchoNativeLoadStatus secondShutdownStatus = moduleLoader.shutdown(loadResult, serviceRegistry);
        EchoNativeLoadStatus unknownShutdownStatus = moduleLoader.shutdown(releaseFallbackRejection, serviceRegistry);
        require(shutdownStatus == EchoNativeLoadStatus.MUTATED,
                "Loaded module shutdown must complete exactly once.");
        require(secondShutdownStatus == EchoNativeLoadStatus.UNSUPPORTED,
                "Second shutdown must not reuse a closed module handle.");
        require(unknownShutdownStatus == EchoNativeLoadStatus.UNSUPPORTED,
                "Unsupported/failed load results must not be shutdown as loaded modules.");
        report.put("shutdownStatus", shutdownStatus.name());
        report.put("secondShutdownStatus", secondShutdownStatus.name());
        report.put("unknownShutdownStatus", unknownShutdownStatus.name());
        report.put("stableShutdownUnloadBehavior", true);
        report.put("successRules", List.of(
                "DISCOVERED = found module metadata",
                "RESOLVED = dependency/classpath planned",
                "LOADED = module class actually loaded",
                "REGISTERED = services/content registered",
                "MUTATED = runtime/game state changed",
                "FAILED = attempted and failed",
                "UNSUPPORTED = not implemented"
        ));
        report.put("activationClaimAllowed", loadResult.loaded() && loadResult.registered());
        report.put("nativeHostMutationClaimAllowed", mutated);
        report.put("noOpMutationGuard", noOpMutationGuard);
        report.put("gameplayReadyClaimAllowed", false);
        report.put("liveClientGameplayReadyClaimAllowed", false);

        Files.createDirectories(output.getParent());
        Files.writeString(output, EchoNativeJson.write(report), StandardCharsets.UTF_8);
        System.out.println("Native Loader proof wrote " + output);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static EchoNativeAddonDescriptor releaseFallbackDescriptor(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = new LinkedHashMap<>(descriptor.access());
        access.remove("nativeClasspath");
        access.put("releaseFallbackProbe", true);
        return new EchoNativeAddonDescriptor(
                descriptor.schema(),
                descriptor.id() + "_release_fallback_probe",
                descriptor.name() + " Release Fallback Probe",
                descriptor.version(),
                descriptor.kind(),
                descriptor.role(),
                descriptor.entrypoint(),
                descriptor.side(),
                descriptor.trustLevel(),
                descriptor.apiStability(),
                descriptor.official(),
                descriptor.standalone(),
                descriptor.requires(),
                descriptor.optional(),
                descriptor.provides(),
                descriptor.consumes(),
                descriptor.transforms(),
                access,
                descriptor.descriptorPath()
        );
    }

    private static EchoNativeAddonDescriptor sideGateDescriptor(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = new LinkedHashMap<>(descriptor.access());
        access.put("releaseSideGateProbe", true);
        return new EchoNativeAddonDescriptor(
                descriptor.schema(),
                descriptor.id() + "_client_side_gate_probe",
                descriptor.name() + " Client Side Gate Probe",
                descriptor.version(),
                descriptor.kind(),
                descriptor.role(),
                descriptor.entrypoint(),
                EchoNativeRuntimeSide.CLIENT,
                descriptor.trustLevel(),
                descriptor.apiStability(),
                descriptor.official(),
                descriptor.standalone(),
                descriptor.requires(),
                descriptor.optional(),
                descriptor.provides(),
                descriptor.consumes(),
                descriptor.transforms(),
                access,
                descriptor.descriptorPath()
        );
    }
}
