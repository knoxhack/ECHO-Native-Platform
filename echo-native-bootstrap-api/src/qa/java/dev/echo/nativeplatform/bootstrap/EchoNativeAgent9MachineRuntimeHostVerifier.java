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
        Path ashfallSource = repoRoot.resolve("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol");
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

        String nativeBootstrap = read(repoRoot.resolve(
                "echo-native-platform/echo-native-bootstrap-api/src/main/java/dev/echo/nativeplatform/bootstrap/EchoNativeBootstrapMain.java"));
        requireContains(nativeBootstrap, "nativeAdapterCoreMachineUseBlock",
                "native processor UI path must start through AdapterCore machine.use_block");
        requireContains(nativeBootstrap, "nativeAdapterCoreMachineTick",
                "native processor UI path must call AdapterCore block_entities.tick");
        requireContains(nativeBootstrap, "nativeAdapterCoreMachineStateChanged",
                "native processor UI path must route visible state through AdapterCore machine.state_changed");
        requireContains(nativeBootstrap, "nativeAdapterCoreMachineInsertItem",
                "native generator UI path must call AdapterCore capabilities.insert_item");
        String cacheAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallCacheAction",
                "private static boolean nativeAshfallRelayAction");
        requireContains(cacheAction, "nativeAdapterCoreMachineUseBlock",
                "native cache UI path must delegate to AdapterCore machine.use_block");
        requireNotContains(cacheAction, "nativeAdapterCoreCacheOpened",
                "native cache UI path must not publish cache-open outside StructureCacheBlock.useStructureCache");
        requireNotContains(cacheAction, "blockRuntime.",
                "native cache UI path must not mutate shell opened state");
        requireNotContains(cacheAction, "giveNativeBetaItem",
                "native cache UI path must not grant fixed fake loot outside the live cache inventory/menu");
        String generatorAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallGeneratorAction",
                "private static boolean nativeAshfallGridAction");
        requireContains(generatorAction, "nativeAdapterCoreMachineUseBlock",
                "native generator UI path must start through AdapterCore machine.use_block");
        requireContains(generatorAction, "nativeAdapterCoreMachineInsertItem",
                "native generator UI path must insert fuel into the active machine host");
        requireContains(generatorAction, "nativeAdapterCoreMachineTick",
                "native generator UI path must tick the real generator block entity");
        requireNotContains(generatorAction, "nativeAdapterCoreMachineStateChanged",
                "native generator path must not publish synthetic state_changed as its implementation");
        requireNotContains(generatorAction, "blockRuntime.energy",
                "native generator path must not mutate shell blockRuntime energy");
        requireNotContains(generatorAction, "gridEnergyGenerated",
                "native generator path must not mutate shell player energy counters");
        requireNotContains(generatorAction, "nativeFuelEnergy",
                "native generator path must not use fake fuel energy math");
        requireNotContains(generatorAction, "\"fuel_added\"",
                "native generator path must not count activation reports as implementation");
        String powerNodeAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallPowerNodeAction",
                "private static boolean nativeAshfallBunkAction");
        requireContains(powerNodeAction, "nativeAdapterCoreMachineUseBlock",
                "native power-node UI path must start through AdapterCore machine.use_block");
        requireContains(powerNodeAction, "nativeAdapterCoreMachineReceiveEnergy",
                "native power-node UI path must charge the real active runtime host");
        requireContains(powerNodeAction, "nativeAdapterCoreMachineTick",
                "native power-node UI path must tick the real power-node block entity");
        requireNotContains(powerNodeAction, "blockRuntime.active",
                "native power-node path must not mutate shell active state");
        requireNotContains(powerNodeAction, "blockRuntime.energy",
                "native power-node path must not mutate shell energy state");
        requireNotContains(powerNodeAction, "playerRuntime.powerNodes",
                "native power-node path must not mutate shell power-node counters");
        String relayAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallRelayAction",
                "private static boolean nativeAshfallPowerNodeAction");
        requireContains(relayAction, "nativeAdapterCoreMachineUseBlock",
                "native relay UI path must delegate to AdapterCore machine.use_block");
        requireNotContains(relayAction, "nativeAdapterCoreRelayActivated",
                "native relay UI path must not publish relay activation outside RelayStationBlock.useRelayStation");
        requireNotContains(relayAction, "blockRuntime.",
                "native relay UI path must not mutate shell repair/active state");
        requireNotContains(relayAction, "playerRuntime.",
                "native relay UI path must not mutate shell relay lists");
        requireNotContains(relayAction, "nativeAdapterCoreRemoveItem",
                "native relay UI path must not consume items outside the live RelayStationBlock path");
        requireNotContains(relayAction, "teleportPlayerTo",
                "native relay UI path must not teleport outside RadioNetwork.fastTravelTo");
        String bunkAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallBunkAction",
                "private static boolean nativeAshfallWaterMachineAction");
        requireContains(bunkAction, "nativeAdapterCoreMachineUseBlock",
                "native bunk UI path must delegate to AdapterCore machine.use_block");
        requireNotContains(bunkAction, "nativeAdapterCoreShelterSlept",
                "native bunk UI path must not publish a fake shelter event outside EmergencyBunkBlock.useEmergencyBunk");
        requireNotContains(bunkAction, "blockRuntime.",
                "native bunk UI path must not mutate shell occupied/active state");
        requireNotContains(bunkAction, "executeNativeBetaCommand",
                "native bunk UI path must not issue direct spawnpoint/effect command strings");
        String serverBlockAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallServerBlockAction",
                "private static boolean nativeAshfallCacheAction");
        String nexusBranch = branchSlice(serverBlockAction,
                "if (hasAny(path, \"nexus_core\"))",
                "return false;");
        requireContains(nexusBranch, "nativeAdapterCoreMachineUseBlock",
                "native nexus UI path must delegate to AdapterCore machine.use_block");
        requireContains(nexusBranch, "\"echoashfallprotocol:nexus_core\"",
                "native nexus UI path must use the canonical Ashfall nexus_core id");
        requireNotContains(nexusBranch, "nativeAdapterCoreNexusState",
                "native nexus UI path must not publish fake nexus state outside NexusCoreBlock.useNexusCore");
        requireNotContains(nexusBranch, "blockRuntime.active",
                "native nexus UI path must not mutate shell active state");
        requireNotContains(nexusBranch, "openNativeModuleSurfaceFor(\"echoterminal\", \"terminal\")",
                "native nexus UI path must not open the terminal outside the live NexusCoreBlock access result");
        String gridAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallGridAction",
                "private static boolean nativeAshfallProcessorAction");
        requireContains(gridAction, "nativeAdapterCoreMachineUseBlock",
                "native grid UI path must start through AdapterCore machine.use_block");
        requireContains(gridAction, "nativeAdapterCoreMachineReceiveEnergy",
                "native grid UI path must charge the active runtime host");
        requireContains(gridAction, "nativeAdapterCoreMachineExtractEnergy",
                "native grid UI path must extract from the active runtime host");
        requireContains(gridAction, "nativeAdapterCoreMachineTick",
                "native grid UI path must tick the real grid block entity");
        requireNotContains(gridAction, "blockRuntime.energy",
                "native grid path must not mutate shell energy state");
        requireNotContains(gridAction, "blockRuntime.active",
                "native grid path must not mutate shell active state");
        requireNotContains(gridAction, "playerRuntime.powerNodes",
                "native grid path must not use shell power-node counters");
        requireNotContains(gridAction, "nativeMachineCapacity",
                "native grid path must not use fake native capacity math");
        String waterAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallWaterMachineAction",
                "private static boolean nativeAshfallHazardMachineAction");
        requireContains(waterAction, "nativeAdapterCoreMachineUseBlock",
                "native water UI path must start through AdapterCore machine.use_block");
        requireContains(waterAction, "nativeAdapterCoreMachineReceiveEnergy",
                "native purifier UI path must charge the active runtime host");
        requireContains(waterAction, "nativeAdapterCoreMachineInsertItem",
                "native purifier UI path must insert water/filter inputs into the active runtime host");
        requireContains(waterAction, "nativeAdapterCoreMachineTick",
                "native water UI path must tick the real water block entity");
        requireContains(waterAction, "nativeAdapterCoreMachineExtractItem",
                "native purifier UI path must extract clean water from the active runtime host");
        requireContains(waterAction, "nativeAdapterCoreWaterFiltered",
                "native purifier UI path must publish water filtered only after real output extraction");
        requireNotContains(waterAction, "ensureNativeMachineEnergy",
                "native water path must not charge fake shell energy");
        requireNotContains(waterAction, "blockRuntime.",
                "native water path must not mutate shell block runtime state");
        requireNotContains(waterAction, "playerRuntime.filteredWaterCrafted",
                "native water path must not mutate shell water counters");
        requireNotContains(waterAction, "nativeAdapterCoreDirtyWaterCollected",
                "native rain collector path must not publish a fake dirty-water event outside RainCollectorBlockEntity.fillBottle");
        requireNotContains(waterAction, "giveNativeBetaItem(player, \"echoashfallprotocol:dirty_water_bottle\"",
                "native rain collector path must not grant dirty water outside the live runtime host");
        String hazardAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallHazardMachineAction",
                "private static boolean nativeAshfallResearchLabAction");
        requireContains(hazardAction, "nativeAdapterCoreMachineUseBlock",
                "native hazard machine UI path must start through AdapterCore machine.use_block");
        requireContains(hazardAction, "nativeAdapterCoreMachineReceiveEnergy",
                "native hazard machine UI path must charge the active runtime host");
        requireContains(hazardAction, "nativeAdapterCoreMachineTick",
                "native hazard machine UI path must tick the real hazard block entity");
        requireContains(hazardAction, "nativeAdapterCoreMachineInsertItem",
                "native radiation cleanser path must insert inputs into the active runtime host");
        requireContains(hazardAction, "nativeAdapterCoreMachineExtractItem",
                "native radiation cleanser path must extract output from the active runtime host");
        requireNotContains(hazardAction, "ensureNativeMachineEnergy",
                "native hazard path must not charge fake shell energy");
        requireNotContains(hazardAction, "blockRuntime.",
                "native hazard path must not mutate shell block runtime state");
        requireNotContains(hazardAction, "playerRuntime.",
                "native hazard path must not mutate shell player hazard state");
        requireNotContains(hazardAction, "nativeAdapterCoreRadiationCleanserUsed",
                "native radiation cleanser path must not publish cleanser use outside RadiationCleanserBlockEntity.serverTick");
        requireNotContains(hazardAction, "nativeAdapterCoreMedBayUsed",
                "native med bay path must not publish med bay use outside FieldMedBayBlockEntity.serverTick");
        requireNotContains(hazardAction, "nativeAdapterCoreAtmosphericScrubberUsed",
                "native scrubber path must not publish scrubber use outside AtmosphericScrubberBlockEntity.serverTick");
        requireNotContains(hazardAction, "healPlayer",
                "native med bay path must not apply direct healing outside FieldMedBayBlockEntity.serverTick");
        requireNotContains(hazardAction, "executeNativeBetaCommand",
                "native hazard path must not apply direct command effects outside live block entities");
        String researchAction = methodSlice(nativeBootstrap,
                "private static boolean nativeAshfallResearchLabAction",
                "private static boolean nativeAshfallGeneratorAction");
        requireContains(researchAction, "nativeAdapterCoreMachineUseBlock",
                "native research lab UI path must start through AdapterCore machine.use_block");
        requireContains(researchAction, "nativeAdapterCoreResearchLabAnalyze",
                "native research lab schematic analysis must delegate to AdapterCore exploration runtime");
        requireNotContains(researchAction, "nativeAdapterCoreLabObjective",
                "native research lab tick fallback must not use a lab objective as a fake schematic implementation");
        requireNotContains(researchAction, "nativeAdapterCoreRemoveItem",
                "native research lab path must not remove schematic items outside the shared runtime");
        requireNotContains(researchAction, "playerRuntime.researchPoints",
                "native research lab path must not mutate shell research point counters");
        requireNotContains(researchAction, "playerRuntime.schematicsUnlocked",
                "native research lab path must not mutate shell schematic counters");
        requireNotContains(researchAction, "blockRuntime.progress",
                "native research lab path must not mutate shell progress state");
        requireNotContains(researchAction, "giveNativeBetaItem",
                "native research lab path must not grant fake upgrade loot outside the shared runtime");
        requireNotContains(researchAction, "openNativeModuleSurfaceFor(\"echoindex\", \"index\")",
                "native research lab path must not open a fake index surface instead of using the live block result");
        requireNotContains(nativeBootstrap, "nativeAshfallProcessorShellAction",
                "native processor path must not keep a dead fake recipe shell");
        requireNotContains(nativeBootstrap, "nativeAdapterCoreMachineOutputCreated",
                "native processor path must not synthesize output events outside real block entity ticks");
        requireNotContains(nativeBootstrap, "scrap_press_cycle",
                "native processor path must not duplicate scrap press recipe cycles");
        requireNotContains(nativeBootstrap, "ore_grinder_cycle",
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
