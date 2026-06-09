package dev.echo.nativeplatform.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EchoNativeAgent9MachineRuntimeHostVerifier {
    private static final List<String> PROCESSOR_ENTITIES = List.of(
            "ScrapPressBlockEntity",
            "OreGrinderBlockEntity",
            "FilterWorkbenchBlockEntity",
            "IsotopeRefinerBlockEntity",
            "CrystallineSynthesizerBlockEntity",
            "DeepCoreMinerBlockEntity");
    private static final List<String> GENERATOR_ENTITIES = List.of(
            "MicroGeneratorBlockEntity",
            "ThermalBurnerBlockEntity",
            "ThermalArrayBlockEntity",
            "ScrapDynamoBlockEntity");
    private static final List<String> GRID_ENTITIES = List.of(
            "PowerNodeBlockEntity",
            "PowerCableBlockEntity",
            "BatteryBankBlockEntity",
            "NexusCapacitorBlockEntity",
            "LoadDistributorBlockEntity",
            "FactoryControllerBlockEntity");
    private static final List<String> WATER_ENTITIES = List.of(
            "WaterPurifierBlockEntity",
            "RainCollectorBlockEntity");
    private static final List<String> HAZARD_ENTITIES = List.of(
            "FieldMedBayBlockEntity",
            "AtmosphericScrubberBlockEntity",
            "RadiationCleanserBlockEntity");

    private EchoNativeAgent9MachineRuntimeHostVerifier() {
    }

    public static void main(String[] args) throws IOException {
        Path repoRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of(System.getProperty("user.dir")).getParent().toAbsolutePath().normalize();
        Path ashfallSource = echoModulesRoot().resolve("echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol");
        Path hostPath = ashfallSource.resolve("nativebridge/AshfallAdapterCoreMachineRuntimeHost.java");
        String hostSource = read(hostPath);

        requireContains(hostSource, "EVENT_OUTPUT_CREATED = \"machine.output_created\"",
                "machine host must keep the canonical machine.output_created event id");
        requireContains(hostSource, "ACTION_TICK = \"block_entities.tick\"",
                "machine host must keep the canonical block_entities.tick action id");
        requireContains(hostSource, "ACTION_INSERT_ITEM = \"capabilities.insert_item\"",
                "machine host must keep the canonical capabilities.insert_item action id");
        requireContains(hostSource, "ACTION_EXTRACT_ITEM = \"capabilities.extract_item\"",
                "machine host must keep the canonical capabilities.extract_item action id");
        requireContains(hostSource, "ACTION_RECEIVE_ENERGY = \"capabilities.receive_energy\"",
                "machine host must keep the canonical capabilities.receive_energy action id");
        requireContains(hostSource, "ACTION_EXTRACT_ENERGY = \"capabilities.extract_energy\"",
                "machine host must keep the canonical capabilities.extract_energy action id");
        requireContains(hostSource, "dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_TICK",
                "machine host must register block_entities.tick as a consumed AdapterCore action");
        requireContains(hostSource, "dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_INSERT_ITEM",
                "machine host must register capabilities.insert_item as a consumed AdapterCore action");
        requireContains(hostSource, "dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_EXTRACT_ITEM",
                "machine host must register capabilities.extract_item as a consumed AdapterCore action");
        requireContains(hostSource, "dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_RECEIVE_ENERGY",
                "machine host must register capabilities.receive_energy as a consumed AdapterCore action");
        requireContains(hostSource, "dispatcher.registerAction(RUNTIME_HOST_ID, ACTION_EXTRACT_ENERGY",
                "machine host must register capabilities.extract_energy as a consumed AdapterCore action");
        requireContains(hostSource, "dispatchNativeMachineTick",
                "machine host must expose native client calls through block_entities.tick");
        requireContains(hostSource, "dispatchNativeMachineInsertItem",
                "machine host must expose native client item insertion through capabilities.insert_item");
        requireContains(hostSource, "dispatchNativeMachineExtractItem",
                "machine host must expose native client item extraction through capabilities.extract_item");
        requireContains(hostSource, "dispatchNativeMachineReceiveEnergy",
                "machine host must expose native client charging through capabilities.receive_energy");
        requireContains(hostSource, "dispatchNativeMachineExtractEnergy",
                "machine host must expose native client energy removal through capabilities.extract_energy");
        requireContains(hostSource, "dispatcher.registerAction(RUNTIME_HOST_ID, EVENT_OUTPUT_CREATED",
                "machine host must register machine.output_created as a consumed AdapterCore event");

        for (String entityName : PROCESSOR_ENTITIES) {
            requireContains(hostSource, "instanceof " + entityName,
                    "machine host tick/snapshot path must recognize " + entityName);
            requireContains(hostSource, entityName + ".serverTick",
                    "machine host must tick real " + entityName + " server logic");
            Path entityPath = ashfallSource.resolve("block/entity/" + entityName + ".java");
            String entitySource = read(entityPath);
            requireContains(entitySource, "AshfallAdapterCoreMachineRuntimeHost.outputCreated(",
                    entityName + " must publish real machine.output_created from its server mutation path");
            requireContains(entitySource, "serverTick(",
                    entityName + " must expose real server tick logic");
            requireContains(entitySource, "saveAdditional(",
                    entityName + " must persist mutated machine state");
            requireContains(entitySource, "loadAdditional(",
                    entityName + " must reload persisted machine state");
        }
        for (String entityName : GENERATOR_ENTITIES) {
            requireContains(hostSource, "instanceof " + entityName,
                    "machine host tick/snapshot path must recognize " + entityName);
            requireContains(hostSource, entityName + ".serverTick",
                    "machine host must tick real " + entityName + " server logic");
            Path entityPath = ashfallSource.resolve("block/entity/" + entityName + ".java");
            String entitySource = read(entityPath);
            requireContains(entitySource, "serverTick(",
                    entityName + " must expose real server tick logic");
            requireContains(entitySource, "saveAdditional(",
                    entityName + " must persist mutated generator state");
            requireContains(entitySource, "loadAdditional(",
                    entityName + " must reload persisted generator state");
        }
        String burnerSource = read(ashfallSource.resolve("block/entity/ThermalBurnerBlockEntity.java"));
        requireContains(burnerSource, "AshfallAdapterCoreMachineRuntimeHost.outputCreated(",
                "thermal burner ash output must publish real machine.output_created from its server tick");
        for (String entityName : GRID_ENTITIES) {
            requireContains(hostSource, "instanceof " + entityName,
                    "machine host tick/snapshot path must recognize " + entityName);
            requireContains(hostSource, entityName + ".serverTick",
                    "machine host must tick real " + entityName + " server logic");
            Path entityPath = ashfallSource.resolve("block/entity/" + entityName + ".java");
            String entitySource = read(entityPath);
            requireContains(entitySource, "serverTick(",
                    entityName + " must expose real server tick logic");
            requireContains(entitySource, "saveAdditional(",
                    entityName + " must persist mutated grid state");
            requireContains(entitySource, "loadAdditional(",
                    entityName + " must reload persisted grid state");
        }
        for (String entityName : WATER_ENTITIES) {
            requireContains(hostSource, "instanceof " + entityName,
                    "machine host tick/snapshot path must recognize " + entityName);
            requireContains(hostSource, entityName + ".serverTick",
                    "machine host must tick real " + entityName + " server logic");
            Path entityPath = ashfallSource.resolve("block/entity/" + entityName + ".java");
            String entitySource = read(entityPath);
            requireContains(entitySource, "serverTick(",
                    entityName + " must expose real server tick logic");
            requireContains(entitySource, "saveAdditional(",
                    entityName + " must persist mutated water state");
            requireContains(entitySource, "loadAdditional(",
                    entityName + " must reload persisted water state");
        }
        for (String entityName : HAZARD_ENTITIES) {
            requireContains(hostSource, "instanceof " + entityName,
                    "machine host tick/snapshot path must recognize " + entityName);
            requireContains(hostSource, entityName + ".serverTick",
                    "machine host must tick real " + entityName + " server logic");
            Path entityPath = ashfallSource.resolve("block/entity/" + entityName + ".java");
            String entitySource = read(entityPath);
            requireContains(entitySource, "serverTick(",
                    entityName + " must expose real server tick logic");
            requireContains(entitySource, "saveAdditional(",
                    entityName + " must persist mutated hazard state");
            requireContains(entitySource, "loadAdditional(",
                    entityName + " must reload persisted hazard state");
        }
        requireContains(hostSource, "RainCollectorBlockEntity.fillBottle",
                "machine.use_block must call the same RainCollectorBlockEntity.fillBottle path as NeoForge bottle use");
        requireContains(hostSource, "EmergencyBunkBlock.useEmergencyBunk",
                "machine.use_block must call the same EmergencyBunkBlock use path as NeoForge bunk use");
        requireContains(hostSource, "RelayStationBlock.useRelayStation",
                "machine.use_block must call the same RelayStationBlock use path as NeoForge relay use");
        requireContains(hostSource, "StructureCacheBlock.useStructureCache",
                "machine.use_block must call the same StructureCacheBlock use path as NeoForge cache use");
        requireContains(hostSource, "NexusCoreBlock.useNexusCore",
                "machine.use_block must call the same NexusCoreBlock use path as NeoForge nexus use");
        requireContains(hostSource, "ResearchLabBlock.useResearchLab",
                "machine.use_block must call the same ResearchLabBlock use path as NeoForge research lab use");
        requireContains(hostSource, "EchoAshfallProtocol.MODID + \":emergency_bunk\"",
                "machine host must expose emergency_bunk as a supported live use target");
        requireContains(hostSource, "EchoAshfallProtocol.MODID + \":relay_station\"",
                "machine host must expose relay_station as a supported live use target");
        requireContains(hostSource, "EchoAshfallProtocol.MODID + \":nexus_core\"",
                "machine host must expose nexus_core as a supported live use target");
        requireContains(hostSource, "EchoAshfallProtocol.MODID + \":research_lab\"",
                "machine host must expose research_lab as a supported live use target");
        requireContains(hostSource, "applyLiveUse",
                "machine host must support record-only machine.use_block dispatch for NeoForge callers that already applied live use");
        String bunkBlockSource = read(ashfallSource.resolve("block/EmergencyBunkBlock.java"));
        requireContains(bunkBlockSource, "useEmergencyBunk(",
                "EmergencyBunkBlock must expose one canonical use implementation for NeoForge and native callers");
        String relayBlockSource = read(ashfallSource.resolve("block/RelayStationBlock.java"));
        requireContains(relayBlockSource, "useRelayStation(",
                "RelayStationBlock must expose one canonical use implementation for NeoForge and native callers");
        String cacheBlockSource = read(ashfallSource.resolve("block/StructureCacheBlock.java"));
        requireContains(cacheBlockSource, "useStructureCache(",
                "StructureCacheBlock must expose one canonical use implementation for NeoForge and native callers");
        requireContains(cacheBlockSource, "dispatchUseBlock(",
                "StructureCacheBlock must still record cache use through AdapterCore machine.use_block");
        String nexusBlockSource = read(ashfallSource.resolve("block/NexusCoreBlock.java"));
        requireContains(nexusBlockSource, "useNexusCore(",
                "NexusCoreBlock must expose one canonical use implementation for NeoForge and native callers");
        String researchBlockSource = read(ashfallSource.resolve("block/ResearchLabBlock.java"));
        requireContains(researchBlockSource, "useResearchLab(",
                "ResearchLabBlock must expose one canonical use implementation for NeoForge and native callers");
        String explorationRuntimeSource = read(ashfallSource.resolve("event/AshfallAdapterCoreExplorationRuntime.java"));
        requireContains(explorationRuntimeSource, "analyzeFirstSchematicAtResearchLab(",
                "Research Lab schematic analysis must expose a shared runtime entrypoint for native callers");
        requireContains(explorationRuntimeSource, "schematicFragmentAnalyzed(",
                "Research Lab fragment analysis must flow through AdapterCore exploration runtime");
        String networkSource = read(ashfallSource.resolve("network/ModNetwork.java"));
        requireContains(networkSource, "AshfallAdapterCoreExplorationRuntime.schematicFragmentAnalyzed(",
                "NeoForge Research Lab packet analysis must use the same AdapterCore runtime as native analysis");

        Path nativePlatform = nativePlatformRoot(repoRoot);
        String bootstrapSource = read(nativePlatform.resolve(
                "echo-native-bootstrap-api/src/main/java/dev/echo/nativeplatform/bootstrap/EchoNativeBootstrapOrchestrator.java"));
        String adapterCoreFlowSource = read(nativePlatform.resolve(
                "echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderAdapterCoreFlow.java"));
        String blockExecutorSource = read(nativePlatform.resolve(
                "echo-native-loader/src/main/java/dev/echo/nativeplatform/loader/NativeLoaderProductBlockActionExecutor.java"));
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::useBlock",
                "native block UI path must start through AdapterCore machine.use_block");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::machineTick",
                "native block UI path must call AdapterCore block_entities.tick");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::powerNodeState",
                "native power-node UI path must route visible state through AdapterCore machine.state_changed");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::insertItem",
                "native generator UI path must call AdapterCore capabilities.insert_item");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::receiveEnergy",
                "native machine UI path must charge the active runtime host");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::extractEnergy",
                "native grid UI path must extract from the active runtime host");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::extractItem",
                "native machine UI path must extract output from the active runtime host");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::researchLabAnalyze",
                "native research lab schematic analysis must delegate to AdapterCore exploration runtime");
        requireContains(bootstrapSource, "ADAPTER_CORE_FLOW::waterFiltered",
                "native purifier UI path must publish water filtered only after real output extraction");
        requireContains(adapterCoreFlowSource, "NativeLoaderAdapterCoreMachineRuntimeActions.useBlock",
                "AdapterCore flow must delegate machine.use_block to the machine runtime action service");
        requireContains(adapterCoreFlowSource, "NativeLoaderAdapterCoreMachineRuntimeActions.tick",
                "AdapterCore flow must delegate block_entities.tick to the machine runtime action service");
        requireContains(adapterCoreFlowSource, "NativeLoaderAdapterCoreMachineRuntimeActions.insertItem",
                "AdapterCore flow must delegate capabilities.insert_item to the machine runtime action service");
        requireContains(adapterCoreFlowSource, "NativeLoaderAdapterCoreMachineRuntimeActions.extractItem",
                "AdapterCore flow must delegate capabilities.extract_item to the machine runtime action service");
        requireContains(adapterCoreFlowSource, "NativeLoaderAdapterCoreMachineRuntimeActions.receiveEnergy",
                "AdapterCore flow must delegate capabilities.receive_energy to the machine runtime action service");
        requireContains(adapterCoreFlowSource, "NativeLoaderAdapterCoreMachineRuntimeActions.extractEnergy",
                "AdapterCore flow must delegate capabilities.extract_energy to the machine runtime action service");
        requireContains(blockExecutorSource, "ops.machineUseBlock",
                "native product block executor must delegate use actions to AdapterCore machine.use_block");
        requireContains(blockExecutorSource, "ops.machineInsertItem",
                "native product block executor must insert inputs into the active machine host");
        requireContains(blockExecutorSource, "ops.machineTick",
                "native product block executor must tick real block entities");
        requireContains(blockExecutorSource, "ops.machineReceiveEnergy",
                "native product block executor must charge active machine hosts");
        requireContains(blockExecutorSource, "ops.machineExtractEnergy",
                "native product block executor must extract energy from active machine hosts");
        requireContains(blockExecutorSource, "ops.machineExtractItem",
                "native product block executor must extract outputs from active machine hosts");
        requireContains(blockExecutorSource, "ops.waterFiltered",
                "native product block executor must publish water filtered only after real output extraction");
        requireContains(blockExecutorSource, "ops.researchLabAnalyze",
                "native research lab path must delegate to AdapterCore exploration runtime");
        requireContains(blockExecutorSource, "ops.blockActionMachineId(\"cache\", \"recovery_cache\")",
                "native cache UI path must use the canonical recovery cache machine id");
        requireContains(blockExecutorSource, "ops.blockActionMachineId(\"relay\", \"relay_station\")",
                "native relay UI path must use the canonical relay station machine id");
        requireContains(blockExecutorSource, "ops.blockActionMachineId(\"power_node\", \"power_node\")",
                "native power-node UI path must use the canonical power-node machine id");
        requireContains(blockExecutorSource, "ops.blockActionMachineId(\"research_lab\", \"research_lab\")",
                "native research lab UI path must use the canonical research lab machine id");
        requireContains(blockExecutorSource, "ops.productId(path)",
                "native processor/generator/grid/water/hazard paths must derive canonical product machine ids");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreCacheOpened",
                "native cache UI path must not publish cache-open outside StructureCacheBlock.useStructureCache");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreRelayActivated",
                "native relay UI path must not publish relay activation outside RelayStationBlock.useRelayStation");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreShelterSlept",
                "native bunk UI path must not publish a fake shelter event outside EmergencyBunkBlock.useEmergencyBunk");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreNexusState",
                "native nexus UI path must not publish fake nexus state outside NexusCoreBlock.useNexusCore");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreRadiationCleanserUsed",
                "native radiation cleanser path must not publish cleanser use outside RadiationCleanserBlockEntity.serverTick");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreMedBayUsed",
                "native med bay path must not publish med bay use outside FieldMedBayBlockEntity.serverTick");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreAtmosphericScrubberUsed",
                "native scrubber path must not publish scrubber use outside AtmosphericScrubberBlockEntity.serverTick");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreLabObjective",
                "native research lab tick fallback must not use a lab objective as a fake schematic implementation");
        requireNotContains(blockExecutorSource, "blockRuntime.",
                "native machine paths must not mutate shell block runtime state");
        requireNotContains(blockExecutorSource, "playerRuntime.",
                "native machine paths must not mutate shell player counters");
        requireNotContains(blockExecutorSource, "ensureNativeMachineEnergy",
                "native machine paths must not charge fake shell energy");
        requireNotContains(blockExecutorSource, "nativeMachineCapacity",
                "native grid path must not use fake native capacity math");
        requireNotContains(blockExecutorSource, "nativeFuelEnergy",
                "native generator path must not use fake fuel energy math");
        requireNotContains(blockExecutorSource, "\"fuel_added\"",
                "native generator path must not count activation reports as implementation");
        requireNotContains(blockExecutorSource, "teleportPlayerTo",
                "native relay UI path must not teleport outside RadioNetwork.fastTravelTo");
        requireNotContains(blockExecutorSource, "executeNativeBetaCommand",
                "native machine paths must not apply direct command effects outside live block entities");
        requireNotContains(blockExecutorSource, "healPlayer",
                "native med bay path must not apply direct healing outside FieldMedBayBlockEntity.serverTick");
        requireNotContains(blockExecutorSource, "giveNativeBetaItem",
                "native machine paths must not grant fixed fake loot outside live runtime hosts");
        requireNotContains(blockExecutorSource, "openNativeModuleSurfaceFor(\"echoterminal\", \"terminal\")",
                "native nexus UI path must not open the terminal outside the live NexusCoreBlock access result");
        requireNotContains(blockExecutorSource, "openNativeModuleSurfaceFor(\"echoindex\", \"index\")",
                "native research lab path must not open a fake index surface instead of using the live block result");
        requireNotContains(blockExecutorSource, "nativeAshfallProcessorShellAction",
                "native processor path must not keep a dead fake recipe shell");
        requireNotContains(blockExecutorSource, "nativeAdapterCoreMachineOutputCreated",
                "native processor path must not synthesize output events outside real block entity ticks");
        requireNotContains(blockExecutorSource, "scrap_press_cycle",
                "native processor path must not duplicate scrap press recipe cycles");
        requireNotContains(blockExecutorSource, "ore_grinder_cycle",
                "native processor path must not duplicate ore grinder recipe cycles");

        System.out.println("agent9 machine runtime host verifier PASS processors=" + PROCESSOR_ENTITIES.size()
                + " generators=" + GENERATOR_ENTITIES.size()
                + " grid=" + GRID_ENTITIES.size()
                + " water=" + WATER_ENTITIES.size()
                + " hazard=" + HAZARD_ENTITIES.size()
                + " canonicalTick=block_entities.tick canonicalInsert=capabilities.insert_item"
                + " canonicalExtract=capabilities.extract_item"
                + " canonicalEnergy=capabilities.receive_energy/capabilities.extract_energy"
                + " canonicalOutput=machine.output_created");
    }

    private static String read(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing required source file: " + path);
        }
        return Files.readString(path);
    }

    private static Path echoModulesRoot() {
        String configured = System.getProperty("echo.modules.root");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("ECHO_MODULES_ROOT");
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path workspaceRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
        Path workspaceModules = workspaceRoot == null
                ? Path.of("..", "ECHO-Modules", "addons")
                : workspaceRoot.resolve("ECHO-Modules").resolve("addons");
        if (Files.isDirectory(workspaceModules)) {
            return workspaceModules.toAbsolutePath().normalize();
        }
        Path legacyAddons = workspaceRoot == null
                ? Path.of("..", "addons")
                : workspaceRoot.resolve("addons");
        return legacyAddons.toAbsolutePath().normalize();
    }

    private static Path nativePlatformRoot(Path repoRoot) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isDirectory(current.resolve("echo-native-bootstrap-api"))) {
            return current;
        }
        for (String candidate : List.of("ECHO-Native-Platform", "echo-native-platform")) {
            Path path = repoRoot.resolve(candidate).toAbsolutePath().normalize();
            if (Files.isDirectory(path.resolve("echo-native-bootstrap-api"))) {
                return path;
            }
        }
        return repoRoot.resolve("ECHO-Native-Platform").toAbsolutePath().normalize();
    }

    private static void requireContains(String source, String needle, String message) {
        if (!source.contains(needle)) {
            throw new IllegalStateException(message + " (missing `" + needle + "`).");
        }
    }

    private static void requireNotContains(String source, String needle, String message) {
        if (source.contains(needle)) {
            throw new IllegalStateException(message + " (found `" + needle + "`).");
        }
    }

    private static String methodSlice(String source, String startNeedle, String endNeedle) {
        int start = source.indexOf(startNeedle);
        if (start < 0) {
            throw new IllegalStateException("Missing method slice start `" + startNeedle + "`.");
        }
        int end = source.indexOf(endNeedle, start + startNeedle.length());
        if (end < 0) {
            throw new IllegalStateException("Missing method slice end `" + endNeedle + "`.");
        }
        return source.substring(start, end);
    }

    private static String branchSlice(String source, String startNeedle, String endNeedle) {
        int start = source.indexOf(startNeedle);
        if (start < 0) {
            throw new IllegalStateException("Missing branch slice start `" + startNeedle + "`.");
        }
        int end = source.indexOf(endNeedle, start + startNeedle.length());
        if (end < 0) {
            throw new IllegalStateException("Missing branch slice end `" + endNeedle + "`.");
        }
        return source.substring(start, end);
    }
}
