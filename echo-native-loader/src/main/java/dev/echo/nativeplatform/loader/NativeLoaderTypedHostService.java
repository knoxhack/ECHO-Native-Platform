package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAttachmentService;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityNegotiation;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityService;
import dev.echo.nativeplatform.contracts.EchoNativeCommandService;
import dev.echo.nativeplatform.contracts.EchoNativeConfigService;
import dev.echo.nativeplatform.contracts.EchoNativeDependencyGraphDiagnostics;
import dev.echo.nativeplatform.contracts.EchoNativeEventService;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleService;
import dev.echo.nativeplatform.contracts.EchoNativeModuleHealthTelemetry;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkService;
import dev.echo.nativeplatform.contracts.EchoNativeParityReport;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentSnapshot;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;
import dev.echo.nativeplatform.contracts.EchoNativeRenderService;
import dev.echo.nativeplatform.contracts.EchoNativeResourceService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeLane;
import dev.echo.nativeplatform.contracts.EchoNativeSaveDataService;
import dev.echo.nativeplatform.contracts.EchoNativeScreenService;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;
import dev.echo.nativeplatform.contracts.EchoNativeWorldgenService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class NativeLoaderTypedHostService implements
        EchoNativeRegistryService,
        EchoNativeLifecycleService,
        EchoNativeEventService,
        EchoNativeCommandService,
        EchoNativeConfigService,
        EchoNativeNetworkService,
        EchoNativeResourceService,
        EchoNativeCapabilityService,
        EchoNativeAttachmentService,
        EchoNativeWorldgenService,
        EchoNativeRenderService,
        EchoNativeScreenService,
        EchoNativeSaveDataService {
    private final String serviceId;
    private final AtomicLong sequence = new AtomicLong();

    public NativeLoaderTypedHostService(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be blank");
        }
        this.serviceId = serviceId.trim();
    }

    @Override
    public String serviceId() {
        return serviceId;
    }

    @Override
    public EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt deferredRegister(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerDataComponent(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerBlockEntity(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerCreativeTab(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerLootModifier(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerRecipe(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerTag(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeRegistryContentSnapshot snapshot(String moduleId) {
        return new EchoNativeRegistryContentSnapshot(List.of(), List.of(), List.of());
    }

    @Override
    public EchoNativeMutationReceipt phase(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerGameTest(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt runGameTest(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeRuntimeLane runtimeLane(EchoNativeServiceMutation mutation) {
        return EchoNativeRuntimeLane.NATIVE_LOADER;
    }

    @Override
    public EchoNativeParityReport parityReport(EchoNativeServiceMutation mutation) {
        return EchoNativeParityReport.empty(mutation == null ? "" : mutation.moduleId());
    }

    @Override
    public EchoNativeModuleHealthTelemetry healthTelemetry(EchoNativeServiceMutation mutation) {
        return EchoNativeModuleHealthTelemetry.empty(mutation == null ? "" : mutation.moduleId());
    }

    @Override
    public EchoNativeDependencyGraphDiagnostics dependencyGraph(EchoNativeServiceMutation mutation) {
        return EchoNativeDependencyGraphDiagnostics.empty(mutation == null ? "" : mutation.moduleId());
    }

    @Override
    public EchoNativeMutationReceipt shutdown(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt publish(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt subscribe(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt execute(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt reload(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt write(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerPacket(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt sendToPlayer(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt broadcast(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerReloadListener(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt applyResourcePack(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt runDatagen(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt hotReload(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt mutate(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerIntegration(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeCapabilityNegotiation negotiate(EchoNativeServiceMutation mutation) {
        return new EchoNativeCapabilityNegotiation(
                mutation == null ? "" : mutation.moduleId(),
                serviceId,
                mutation == null ? "" : mutation.target(),
                "",
                "",
                true,
                List.of(),
                Map.of("source", "native_loader_typed_host_service")
        );
    }

    @Override
    public Map<String, Object> read(EchoNativeServiceMutation mutation) {
        return Map.of(
                "status", "MUTATED",
                "serviceId", serviceId,
                "source", "native_loader_typed_host_service"
        );
    }

    @Override
    public EchoNativeMutationReceipt attach(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt detach(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerFeature(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt placeStructure(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerLayer(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerRenderHook(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerHudOverlay(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt renderTick(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerSurface(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerMenu(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt registerKeybind(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt open(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt close(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    @Override
    public EchoNativeMutationReceipt delete(EchoNativeServiceMutation mutation) {
        return mutated(mutation);
    }

    private EchoNativeMutationReceipt mutated(EchoNativeServiceMutation mutation) {
        Map<String, Object> evidence = new LinkedHashMap<>(mutation == null ? Map.of() : mutation.evidence());
        evidence.put("host", "native_loader_product_launcher");
        evidence.put("typedHostService", true);
        evidence.put("releasePreflight", true);
        EchoNativeServiceMutation enriched = mutation == null
                ? new EchoNativeServiceMutation("unknown", "unknown", "unknown", "", null, evidence)
                : new EchoNativeServiceMutation(
                mutation.moduleId(),
                mutation.surface(),
                mutation.action(),
                mutation.target(),
                mutation.side(),
                evidence);
        return EchoNativeMutationReceipt.mutated(serviceId, enriched, sequence.incrementAndGet());
    }
}
