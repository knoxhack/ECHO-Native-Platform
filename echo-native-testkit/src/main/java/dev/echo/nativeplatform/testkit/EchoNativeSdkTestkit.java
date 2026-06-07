package dev.echo.nativeplatform.testkit;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeApiStatus;
import dev.echo.nativeplatform.contracts.EchoNativeAttachmentService;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityService;
import dev.echo.nativeplatform.contracts.EchoNativeCapabilityNegotiation;
import dev.echo.nativeplatform.contracts.EchoNativeCommandService;
import dev.echo.nativeplatform.contracts.EchoNativeConfigService;
import dev.echo.nativeplatform.contracts.EchoNativeDependencyGraphDiagnostics;
import dev.echo.nativeplatform.contracts.EchoNativeEventService;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleService;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeModuleHealthTelemetry;
import dev.echo.nativeplatform.contracts.EchoNativeMutationLedger;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeNetworkService;
import dev.echo.nativeplatform.contracts.EchoNativeParityReport;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentDefinition;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryContentSnapshot;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;
import dev.echo.nativeplatform.contracts.EchoNativeRenderService;
import dev.echo.nativeplatform.contracts.EchoNativeResourceService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeLane;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeSaveDataService;
import dev.echo.nativeplatform.contracts.EchoNativeScreenService;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.contracts.EchoNativeTypedServiceSupport;
import dev.echo.nativeplatform.contracts.EchoNativeWorldgenService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@EchoNativeApiStatus(value = EchoNativeApiStability.TEST_ONLY, since = "0.1.0-native-beta")
public final class EchoNativeSdkTestkit {
    private static final String TESTKIT_MODULE_ID = "echo-native-testkit";

    private EchoNativeSdkTestkit() {
    }

    public static Environment common(String moduleId) {
        return environment(moduleId, EchoNativeRuntimeSide.COMMON);
    }

    public static Environment client(String moduleId) {
        return environment(moduleId, EchoNativeRuntimeSide.CLIENT);
    }

    public static Environment server(String moduleId) {
        return environment(moduleId, EchoNativeRuntimeSide.SERVER);
    }

    public static Environment environment(String moduleId, EchoNativeRuntimeSide side) {
        String checkedModuleId = requireText(moduleId, "moduleId");
        EchoNativeRuntimeSide checkedSide = side == null ? EchoNativeRuntimeSide.UNKNOWN : side;
        EchoNativeMutationLedger ledger = new EchoNativeMutationLedger();
        EchoNativeServiceRegistry serviceRegistry = new EchoNativeServiceRegistry();

        FakeRegistryService registry = new FakeRegistryService(checkedModuleId, checkedSide, ledger);
        FakeLifecycleService lifecycle = new FakeLifecycleService(checkedModuleId, checkedSide, ledger);
        FakeEventService events = new FakeEventService(checkedModuleId, checkedSide, ledger);
        FakeCommandService commands = new FakeCommandService(checkedModuleId, checkedSide, ledger);
        FakeConfigService config = new FakeConfigService(checkedModuleId, checkedSide, ledger);
        FakeNetworkService network = new FakeNetworkService(checkedModuleId, checkedSide, ledger);
        FakeResourceService resources = new FakeResourceService(checkedModuleId, checkedSide, ledger);
        FakeCapabilityService capabilities = new FakeCapabilityService(checkedModuleId, checkedSide, ledger);
        FakeAttachmentService attachments = new FakeAttachmentService(checkedModuleId, checkedSide, ledger);
        FakeWorldgenService worldgen = new FakeWorldgenService(checkedModuleId, checkedSide, ledger);
        FakeRenderService render = new FakeRenderService(checkedModuleId, checkedSide, ledger);
        FakeScreenService screens = new FakeScreenService(checkedModuleId, checkedSide, ledger);
        FakeSaveDataService saveData = new FakeSaveDataService(checkedModuleId, checkedSide, ledger);

        register(serviceRegistry, registry, EchoNativeRegistryService.class,
                "register",
                "deferredRegister",
                "registerDataComponent",
                "registerBlockEntity",
                "registerCreativeTab",
                "registerLootModifier",
                "registerRecipe",
                "registerTag",
                "snapshot");
        register(serviceRegistry, lifecycle, EchoNativeLifecycleService.class,
                "phase",
                "registerGameTest",
                "runGameTest",
                "runtimeLane",
                "parityReport",
                "healthTelemetry",
                "dependencyGraph",
                "shutdown");
        register(serviceRegistry, events, EchoNativeEventService.class, "publish", "subscribe");
        register(serviceRegistry, commands, EchoNativeCommandService.class, "register", "execute");
        register(serviceRegistry, config, EchoNativeConfigService.class, "register", "reload", "write");
        register(serviceRegistry, network, EchoNativeNetworkService.class, "registerPacket", "sendToPlayer", "broadcast");
        register(serviceRegistry, resources, EchoNativeResourceService.class,
                "registerReloadListener",
                "reload",
                "applyResourcePack",
                "runDatagen",
                "hotReload");
        register(serviceRegistry, capabilities, EchoNativeCapabilityService.class,
                "register",
                "mutate",
                "registerIntegration",
                "negotiate",
                "read");
        register(serviceRegistry, attachments, EchoNativeAttachmentService.class, "attach", "detach");
        register(serviceRegistry, worldgen, EchoNativeWorldgenService.class, "registerFeature", "placeStructure");
        register(serviceRegistry, render, EchoNativeRenderService.class,
                "registerLayer",
                "registerRenderHook",
                "registerHudOverlay",
                "renderTick");
        register(serviceRegistry, screens, EchoNativeScreenService.class,
                "registerSurface",
                "registerMenu",
                "registerKeybind",
                "open",
                "close");
        register(serviceRegistry, saveData, EchoNativeSaveDataService.class, "write", "delete", "read");

        return new Environment(
                checkedModuleId,
                checkedSide,
                EchoNativeRuntimeLane.STANDALONE,
                ledger,
                serviceRegistry,
                registry,
                lifecycle,
                events,
                commands,
                config,
                network,
                resources,
                capabilities,
                attachments,
                worldgen,
                render,
                screens,
                saveData
        );
    }

    public static EchoNativeServiceMutation mutation(
            String moduleId,
            String surface,
            String action,
            String target,
            EchoNativeRuntimeSide side
    ) {
        return new EchoNativeServiceMutation(moduleId, surface, action, target, side, Map.of());
    }

    public static EchoNativeServiceMutation mutation(
            String moduleId,
            String surface,
            String action,
            String target,
            EchoNativeRuntimeSide side,
            Map<String, Object> evidence
    ) {
        return new EchoNativeServiceMutation(moduleId, surface, action, target, side, evidence);
    }

    public record Environment(
            String moduleId,
            EchoNativeRuntimeSide side,
            EchoNativeRuntimeLane lane,
            EchoNativeMutationLedger ledger,
            EchoNativeServiceRegistry serviceRegistry,
            FakeRegistryService registry,
            FakeLifecycleService lifecycle,
            FakeEventService events,
            FakeCommandService commands,
            FakeConfigService config,
            FakeNetworkService network,
            FakeResourceService resources,
            FakeCapabilityService capabilities,
            FakeAttachmentService attachments,
            FakeWorldgenService worldgen,
            FakeRenderService render,
            FakeScreenService screens,
            FakeSaveDataService saveData
    ) {
        public Environment {
            moduleId = requireText(moduleId, "moduleId");
            side = side == null ? EchoNativeRuntimeSide.UNKNOWN : side;
            lane = lane == null ? EchoNativeRuntimeLane.STANDALONE : lane;
            Objects.requireNonNull(ledger, "ledger");
            Objects.requireNonNull(serviceRegistry, "serviceRegistry");
            Objects.requireNonNull(registry, "registry");
            Objects.requireNonNull(lifecycle, "lifecycle");
            Objects.requireNonNull(events, "events");
            Objects.requireNonNull(commands, "commands");
            Objects.requireNonNull(config, "config");
            Objects.requireNonNull(network, "network");
            Objects.requireNonNull(resources, "resources");
            Objects.requireNonNull(capabilities, "capabilities");
            Objects.requireNonNull(attachments, "attachments");
            Objects.requireNonNull(worldgen, "worldgen");
            Objects.requireNonNull(render, "render");
            Objects.requireNonNull(screens, "screens");
            Objects.requireNonNull(saveData, "saveData");
        }

        public EchoNativeServiceMutation mutation(String surface, String action, String target) {
            return mutation(surface, action, target, Map.of());
        }

        public EchoNativeServiceMutation mutation(
                String surface,
                String action,
                String target,
                Map<String, Object> evidence
        ) {
            return EchoNativeSdkTestkit.mutation(moduleId, surface, action, target, side, evidence);
        }

        public List<EchoNativeMutationReceipt> receipts() {
            return ledger.receipts();
        }

        public List<EchoNativeMutationReceipt> mutatedReceipts() {
            return receipts().stream().filter(EchoNativeMutationReceipt::mutated).toList();
        }

        public GoldenParityAssertions goldenParity() {
            return new GoldenParityAssertions(this);
        }

        public ModuleFixture moduleFixture(String entrypoint) {
            return moduleFixture(entrypoint, List.of(), List.of(), List.of());
        }

        public ModuleFixture moduleFixture(
                String entrypoint,
                List<String> requires,
                List<String> optional,
                List<String> provides
        ) {
            Path descriptorPath = Path.of("build", "echo-native-testkit", "fixtures", moduleId, "META-INF", "echo.mod.json");
            EchoNativeAddonDescriptor addon = new EchoNativeAddonDescriptor(
                    "echo.native.addon.v1",
                    moduleId,
                    moduleId,
                    "0.0.0-test",
                    "native-addon",
                    "addon",
                    requireText(entrypoint, "entrypoint"),
                    side,
                    EchoNativeTrustLevel.LOCAL,
                    EchoNativeApiStability.TEST_ONLY,
                    false,
                    true,
                    requires == null ? List.of() : List.copyOf(requires),
                    optional == null ? List.of() : List.copyOf(optional),
                    provides == null ? List.of() : List.copyOf(provides),
                    List.of(),
                    List.of(),
                    Map.of("nativeEntrypoint", entrypoint),
                    descriptorPath
            );
            return new ModuleFixture(addon, EchoNativeModuleDescriptor.fromAddon(addon));
        }
    }

    public record ModuleFixture(
            EchoNativeAddonDescriptor addonDescriptor,
            EchoNativeModuleDescriptor moduleDescriptor
    ) {
        public ModuleFixture {
            Objects.requireNonNull(addonDescriptor, "addonDescriptor");
            Objects.requireNonNull(moduleDescriptor, "moduleDescriptor");
        }
    }

    public static final class GoldenParityAssertions {
        private final Environment environment;

        private GoldenParityAssertions(Environment environment) {
            this.environment = environment;
        }

        public void requireNoFailedReceipts() {
            List<EchoNativeMutationReceipt> failed = environment.receipts().stream()
                    .filter(receipt -> receipt.status() == EchoNativeLoadStatus.FAILED)
                    .toList();
            if (!failed.isEmpty()) {
                throw new IllegalStateException("Expected no failed native receipts, found " + failed);
            }
        }

        public void requireMutatedServices(String... serviceIds) {
            Set<String> present = environment.mutatedReceipts().stream()
                    .map(EchoNativeMutationReceipt::serviceId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> missing = List.of(serviceIds).stream()
                    .filter(serviceId -> !present.contains(serviceId))
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing mutated services " + missing + "; present " + present);
            }
        }

        public void requireMutatedSurfaces(String... surfaces) {
            Set<String> present = environment.mutatedReceipts().stream()
                    .map(EchoNativeMutationReceipt::surface)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> missing = List.of(surfaces).stream()
                    .filter(surface -> !present.contains(surface))
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalStateException("Missing mutated surfaces " + missing + "; present " + present);
            }
        }

        public void requireOnlyTypedReceipts() {
            List<EchoNativeMutationReceipt> invalid = environment.receipts().stream()
                    .filter(receipt -> receipt.serviceId().isBlank() || receipt.receiptId().isBlank())
                    .toList();
            if (!invalid.isEmpty()) {
                throw new IllegalStateException("Found receipts without typed service identity " + invalid);
            }
        }

        public Map<String, Object> parityReport(String... expectedSurfaces) {
            return typedParityReport(expectedSurfaces).evidence();
        }

        public EchoNativeParityReport typedParityReport(String... expectedSurfaces) {
            Set<String> mutatedSurfaces = environment.mutatedReceipts().stream()
                    .map(EchoNativeMutationReceipt::surface)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> required = List.of(expectedSurfaces);
            List<String> missing = List.of(expectedSurfaces).stream()
                    .filter(surface -> !mutatedSurfaces.contains(surface))
                    .toList();
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("moduleId", environment.moduleId());
            report.put("side", environment.side().name());
            report.put("lane", environment.lane().laneId());
            report.put("receiptCount", environment.receipts().size());
            report.put("mutatedReceiptCount", environment.mutatedReceipts().size());
            report.put("mutatedSurfaces", List.copyOf(mutatedSurfaces));
            report.put("missingSurfaces", missing);
            report.put("passed", missing.isEmpty());
            return new EchoNativeParityReport(
                    environment.moduleId(),
                    required,
                    List.copyOf(mutatedSurfaces),
                    missing,
                    missing.isEmpty(),
                    report
            );
        }
    }

    public static final class FakeRegistryService extends FakeTypedService implements EchoNativeRegistryService {
        private final Map<String, EchoNativeRegistryContentDefinition> definitions = new LinkedHashMap<>();
        private final Set<String> deferred = new LinkedHashSet<>();

        private FakeRegistryService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            EchoNativeMutationReceipt sideGate = rejectWrongSide(checked);
            if (sideGate != null) {
                return sideGate;
            }
            String key = key(checked);
            if (definitions.containsKey(key)) {
                return failed(checked, "duplicate registry content id " + checked.target());
            }
            definitions.put(key, definition(checked));
            return mutated(checked);
        }

        @Override
        public EchoNativeMutationReceipt deferredRegister(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            EchoNativeMutationReceipt sideGate = rejectWrongSide(checked);
            if (sideGate != null) {
                return sideGate;
            }
            String key = key(checked);
            if (!deferred.add(key)) {
                return failed(checked, "duplicate deferred registry content id " + checked.target());
            }
            return mutated(checked);
        }

        @Override
        public EchoNativeMutationReceipt registerDataComponent(EchoNativeServiceMutation mutation) {
            return registerTypedContent(normalize(mutation), "data_components", "duplicate data component ");
        }

        @Override
        public EchoNativeMutationReceipt registerBlockEntity(EchoNativeServiceMutation mutation) {
            return registerTypedContent(normalize(mutation), "block_entities", "duplicate block entity ");
        }

        @Override
        public EchoNativeMutationReceipt registerCreativeTab(EchoNativeServiceMutation mutation) {
            return registerTypedContent(normalize(mutation), "creative_tabs", "duplicate creative tab ");
        }

        @Override
        public EchoNativeMutationReceipt registerLootModifier(EchoNativeServiceMutation mutation) {
            return registerTypedContent(normalize(mutation), "loot_modifiers", "duplicate loot modifier ");
        }

        @Override
        public EchoNativeMutationReceipt registerRecipe(EchoNativeServiceMutation mutation) {
            return registerTypedContent(normalize(mutation), "recipes", "duplicate recipe ");
        }

        @Override
        public EchoNativeMutationReceipt registerTag(EchoNativeServiceMutation mutation) {
            return registerTypedContent(normalize(mutation), "tags", "duplicate tag ");
        }

        @Override
        public EchoNativeRegistryContentSnapshot snapshot(String moduleId) {
            List<EchoNativeRegistryContentDefinition> visible = definitions.values().stream()
                    .filter(definition -> moduleId == null || moduleId.isBlank() || definition.addon().equals(moduleId))
                    .toList();
            return new EchoNativeRegistryContentSnapshot(visible, List.of(), visible.stream()
                    .map(EchoNativeRegistryContentDefinition::id)
                    .sorted()
                    .toList());
        }

        public List<EchoNativeRegistryContentDefinition> definitions() {
            return List.copyOf(definitions.values());
        }

        private EchoNativeRegistryContentDefinition definition(EchoNativeServiceMutation mutation) {
            String registry = evidenceText(mutation, "registry").orElse(mutation.surface());
            String kind = evidenceText(mutation, "kind").orElse("");
            return new EchoNativeRegistryContentDefinition(
                    registry,
                    mutation.target(),
                    mutation.moduleId(),
                    kind,
                    evidenceText(mutation, "blockstate").orElse(""),
                    evidenceText(mutation, "model").orElse(""),
                    evidenceText(mutation, "texture").orElse(""),
                    evidenceText(mutation, "lang").orElse(""),
                    evidenceList(mutation, "inputs"),
                    evidenceList(mutation, "outputs"),
                    evidenceList(mutation, "entries"),
                    evidenceText(mutation, "source").orElse("sdk-testkit"),
                    !Boolean.FALSE.equals(mutation.evidence().get("searchVisible")),
                    evidenceInt(mutation, "mergedSourceCount", 1)
            );
        }

        private EchoNativeMutationReceipt registerTypedContent(
                EchoNativeServiceMutation mutation,
                String registry,
                String duplicatePrefix
        ) {
            EchoNativeMutationReceipt sideGate = rejectWrongSide(mutation);
            if (sideGate != null) {
                return sideGate;
            }
            String key = key(mutation);
            if (definitions.containsKey(key)) {
                return failed(mutation, duplicatePrefix + mutation.target());
            }
            definitions.put(key, definition(withRegistry(mutation, registry)));
            return mutated(mutation);
        }

        private EchoNativeServiceMutation withRegistry(EchoNativeServiceMutation mutation, String registry) {
            Map<String, Object> evidence = new LinkedHashMap<>(mutation.evidence());
            evidence.putIfAbsent("registry", registry);
            return new EchoNativeServiceMutation(
                    mutation.moduleId(),
                    mutation.surface(),
                    mutation.action(),
                    mutation.target(),
                    mutation.side(),
                    evidence
            );
        }
    }

    public static final class FakeLifecycleService extends FakeTypedService implements EchoNativeLifecycleService {
        private final Set<String> gameTests = new LinkedHashSet<>();

        private FakeLifecycleService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt phase(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }

        @Override
        public EchoNativeMutationReceipt registerGameTest(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            if (!gameTests.add(key(checked))) {
                return failed(checked, "duplicate game test " + checked.target());
            }
            return record(checked);
        }

        @Override
        public EchoNativeMutationReceipt runGameTest(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }

        @Override
        public EchoNativeRuntimeLane runtimeLane(EchoNativeServiceMutation mutation) {
            return EchoNativeRuntimeLane.STANDALONE;
        }

        @Override
        public EchoNativeParityReport parityReport(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            List<String> required = evidenceList(checked, "requiredSurfaces");
            Set<String> mutated = receiptSnapshot().stream()
                    .filter(EchoNativeMutationReceipt::mutated)
                    .map(EchoNativeMutationReceipt::surface)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            List<String> missing = required.stream()
                    .filter(surface -> !mutated.contains(surface))
                    .toList();
            return new EchoNativeParityReport(
                    checked.moduleId(),
                    required,
                    List.copyOf(mutated),
                    missing,
                    missing.isEmpty(),
                    Map.of("lane", EchoNativeRuntimeLane.STANDALONE.laneId())
            );
        }

        @Override
        public EchoNativeModuleHealthTelemetry healthTelemetry(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            List<EchoNativeMutationReceipt> receipts = receiptSnapshot();
            long mutated = receipts.stream().filter(EchoNativeMutationReceipt::mutated).count();
            long failed = receipts.stream().filter(receipt -> receipt.status() == EchoNativeLoadStatus.FAILED).count();
            EchoNativeLoadStatus status = failed > 0
                    ? EchoNativeLoadStatus.FAILED
                    : (mutated > 0 ? EchoNativeLoadStatus.MUTATED : EchoNativeLoadStatus.REGISTERED);
            return new EchoNativeModuleHealthTelemetry(
                    checked.moduleId(),
                    status,
                    hostSide(),
                    receipts.size(),
                    mutated,
                    failed,
                    Map.of("lane", EchoNativeRuntimeLane.STANDALONE.laneId())
            );
        }

        @Override
        public EchoNativeDependencyGraphDiagnostics dependencyGraph(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            List<String> resolved = evidenceList(checked, "resolvedOrder");
            if (resolved.isEmpty()) {
                resolved = List.of(checked.moduleId());
            }
            return new EchoNativeDependencyGraphDiagnostics(
                    checked.moduleId(),
                    resolved,
                    evidenceList(checked, "missingDependencies"),
                    evidenceList(checked, "cycles"),
                    !Boolean.FALSE.equals(checked.evidence().get("deterministic")),
                    Map.of("source", "sdk-testkit")
            );
        }

        @Override
        public EchoNativeMutationReceipt shutdown(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }
    }

    public static final class FakeEventService extends FakeTypedService implements EchoNativeEventService {
        private final Set<String> subscriptions = new LinkedHashSet<>();
        private final List<String> publishedEvents = new ArrayList<>();

        private FakeEventService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt publish(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            publishedEvents.add(checked.target());
            return record(checked);
        }

        @Override
        public EchoNativeMutationReceipt subscribe(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            if (!subscriptions.add(key(checked))) {
                return failed(checked, "duplicate event subscription " + checked.target());
            }
            return record(checked);
        }

        public List<String> publishedEvents() {
            return List.copyOf(publishedEvents);
        }
    }

    public static final class FakeCommandService extends DuplicateGuardService implements EchoNativeCommandService {
        private FakeCommandService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate command ");
        }

        @Override
        public EchoNativeMutationReceipt execute(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }
    }

    public static final class FakeConfigService extends DuplicateGuardService implements EchoNativeConfigService {
        private final Map<String, Map<String, Object>> values = new LinkedHashMap<>();

        private FakeConfigService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate config schema ");
        }

        @Override
        public EchoNativeMutationReceipt reload(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }

        @Override
        public EchoNativeMutationReceipt write(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            values.put(checked.target(), checked.evidence());
            return record(checked);
        }

        public Map<String, Map<String, Object>> values() {
            return Map.copyOf(values);
        }
    }

    public static final class FakeNetworkService extends DuplicateGuardService implements EchoNativeNetworkService {
        private FakeNetworkService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt registerPacket(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate network packet ");
        }

        @Override
        public EchoNativeMutationReceipt sendToPlayer(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }

        @Override
        public EchoNativeMutationReceipt broadcast(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }
    }

    public static final class FakeResourceService extends DuplicateGuardService implements EchoNativeResourceService {
        private final List<String> reloads = new ArrayList<>();

        private FakeResourceService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt registerReloadListener(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate resource reload listener ");
        }

        @Override
        public EchoNativeMutationReceipt reload(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            reloads.add(checked.target());
            return record(checked);
        }

        @Override
        public EchoNativeMutationReceipt applyResourcePack(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }

        @Override
        public EchoNativeMutationReceipt runDatagen(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate datagen target ");
        }

        @Override
        public EchoNativeMutationReceipt hotReload(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            reloads.add("hot:" + checked.target());
            return record(checked);
        }

        public List<String> reloads() {
            return List.copyOf(reloads);
        }
    }

    public static final class FakeCapabilityService extends DuplicateGuardService implements EchoNativeCapabilityService {
        private final Map<String, Map<String, Object>> values = new LinkedHashMap<>();

        private FakeCapabilityService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt register(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate capability ");
        }

        @Override
        public EchoNativeMutationReceipt mutate(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            values.put(checked.target(), checked.evidence());
            return record(checked);
        }

        @Override
        public EchoNativeMutationReceipt registerIntegration(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate integration ");
        }

        @Override
        public EchoNativeCapabilityNegotiation negotiate(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            String requestedVersion = evidenceText(checked, "requestedVersion").orElse("");
            String selectedVersion = evidenceText(checked, "selectedVersion").orElse(requestedVersion);
            List<String> alternatives = evidenceList(checked, "alternatives");
            boolean supported = !Boolean.FALSE.equals(checked.evidence().get("supported"));
            return new EchoNativeCapabilityNegotiation(
                    checked.moduleId(),
                    serviceId(),
                    checked.target(),
                    requestedVersion,
                    selectedVersion,
                    supported,
                    alternatives,
                    checked.evidence()
            );
        }

        @Override
        public Map<String, Object> read(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            return values.getOrDefault(checked.target(), Map.of());
        }
    }

    public static final class FakeAttachmentService extends FakeTypedService implements EchoNativeAttachmentService {
        private final Set<String> attachments = new LinkedHashSet<>();

        private FakeAttachmentService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt attach(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            attachments.add(key(checked));
            return record(checked);
        }

        @Override
        public EchoNativeMutationReceipt detach(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            attachments.remove(key(checked));
            return record(checked);
        }
    }

    public static final class FakeWorldgenService extends DuplicateGuardService implements EchoNativeWorldgenService {
        private FakeWorldgenService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt registerFeature(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate worldgen feature ");
        }

        @Override
        public EchoNativeMutationReceipt placeStructure(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }
    }

    public static final class FakeRenderService extends DuplicateGuardService implements EchoNativeRenderService {
        private FakeRenderService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt registerLayer(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate render layer ");
        }

        @Override
        public EchoNativeMutationReceipt registerRenderHook(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate render hook ");
        }

        @Override
        public EchoNativeMutationReceipt registerHudOverlay(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate HUD overlay ");
        }

        @Override
        public EchoNativeMutationReceipt renderTick(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }
    }

    public static final class FakeScreenService extends DuplicateGuardService implements EchoNativeScreenService {
        private FakeScreenService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt registerSurface(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate screen surface ");
        }

        @Override
        public EchoNativeMutationReceipt registerMenu(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate menu ");
        }

        @Override
        public EchoNativeMutationReceipt registerKeybind(EchoNativeServiceMutation mutation) {
            return recordUnique(normalize(mutation), "duplicate keybind ");
        }

        @Override
        public EchoNativeMutationReceipt open(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }

        @Override
        public EchoNativeMutationReceipt close(EchoNativeServiceMutation mutation) {
            return record(normalize(mutation));
        }
    }

    public static final class FakeSaveDataService extends FakeTypedService implements EchoNativeSaveDataService {
        private final Map<String, Map<String, Object>> values = new LinkedHashMap<>();

        private FakeSaveDataService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        @Override
        public EchoNativeMutationReceipt write(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            values.put(checked.target(), checked.evidence());
            return record(checked);
        }

        @Override
        public EchoNativeMutationReceipt delete(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            values.remove(checked.target());
            return record(checked);
        }

        @Override
        public Map<String, Object> read(EchoNativeServiceMutation mutation) {
            EchoNativeServiceMutation checked = normalize(mutation);
            return values.getOrDefault(checked.target(), Map.of());
        }
    }

    private abstract static class DuplicateGuardService extends FakeTypedService {
        private final Set<String> registered = new LinkedHashSet<>();

        private DuplicateGuardService(String moduleId, EchoNativeRuntimeSide side, EchoNativeMutationLedger ledger) {
            super(moduleId, side, ledger);
        }

        protected final EchoNativeMutationReceipt recordUnique(EchoNativeServiceMutation mutation, String duplicatePrefix) {
            EchoNativeMutationReceipt sideGate = rejectWrongSide(mutation);
            if (sideGate != null) {
                return sideGate;
            }
            if (!registered.add(key(mutation))) {
                return failed(mutation, duplicatePrefix + mutation.target());
            }
            return mutated(mutation);
        }
    }

    private abstract static class FakeTypedService implements EchoNativeTypedServiceSupport {
        private final String moduleId;
        private final EchoNativeRuntimeSide hostSide;
        private final EchoNativeMutationLedger ledger;

        private FakeTypedService(String moduleId, EchoNativeRuntimeSide hostSide, EchoNativeMutationLedger ledger) {
            this.moduleId = moduleId;
            this.hostSide = hostSide == null ? EchoNativeRuntimeSide.UNKNOWN : hostSide;
            this.ledger = Objects.requireNonNull(ledger, "ledger");
        }

        protected final EchoNativeRuntimeSide hostSide() {
            return hostSide;
        }

        protected final List<EchoNativeMutationReceipt> receiptSnapshot() {
            return ledger.receipts();
        }

        protected final EchoNativeMutationReceipt record(EchoNativeServiceMutation mutation) {
            EchoNativeMutationReceipt sideGate = rejectWrongSide(mutation);
            return sideGate == null ? mutated(mutation) : sideGate;
        }

        protected final EchoNativeMutationReceipt mutated(EchoNativeServiceMutation mutation) {
            return ledger.append(serviceId(), mutation, EchoNativeLoadStatus.MUTATED);
        }

        protected final EchoNativeMutationReceipt failed(EchoNativeServiceMutation mutation, String reason) {
            return ledger.append(EchoNativeMutationReceipt.failed(serviceId(), mutation, reason));
        }

        protected final EchoNativeMutationReceipt rejectWrongSide(EchoNativeServiceMutation mutation) {
            EchoNativeRuntimeSide requested = mutation.side();
            if (requested == EchoNativeRuntimeSide.UNKNOWN || requested == EchoNativeRuntimeSide.COMMON) {
                return null;
            }
            if (hostSide == EchoNativeRuntimeSide.UNKNOWN || hostSide == EchoNativeRuntimeSide.COMMON || hostSide == requested) {
                return null;
            }
            return failed(mutation, "side gate rejected " + requested.name() + " mutation on " + hostSide.name() + " host");
        }

        protected final EchoNativeServiceMutation normalize(EchoNativeServiceMutation mutation) {
            Objects.requireNonNull(mutation, "mutation");
            EchoNativeRuntimeSide side = mutation.side() == EchoNativeRuntimeSide.UNKNOWN ? hostSide : mutation.side();
            String checkedModuleId = mutation.moduleId().isBlank() ? moduleId : mutation.moduleId();
            return new EchoNativeServiceMutation(
                    checkedModuleId,
                    mutation.surface(),
                    mutation.action(),
                    mutation.target(),
                    side,
                    mutation.evidence()
            );
        }

        protected final String key(EchoNativeServiceMutation mutation) {
            return mutation.moduleId() + "\u0000" + mutation.surface() + "\u0000" + mutation.target();
        }
    }

    private static <T> void register(
            EchoNativeServiceRegistry registry,
            T service,
            Class<T> serviceType,
            String... surfaces
    ) {
        String serviceId = ((EchoNativeTypedServiceSupport) service).serviceId();
        registry.registerTyped(TESTKIT_MODULE_ID, serviceId, service, serviceType, List.of(surfaces));
    }

    private static Optional<String> evidenceText(EchoNativeServiceMutation mutation, String key) {
        Object value = mutation.evidence().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value).trim());
    }

    private static List<String> evidenceList(EchoNativeServiceMutation mutation, String key) {
        Object value = mutation.evidence().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }

    private static int evidenceInt(EchoNativeServiceMutation mutation, String key, int defaultValue) {
        Object value = mutation.evidence().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
