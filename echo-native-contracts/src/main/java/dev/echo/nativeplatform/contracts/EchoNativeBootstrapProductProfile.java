package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public interface EchoNativeBootstrapProductProfile {
    String namespace();

    String nativeLoaderMainLabel();

    String nativeLoaderClientLabel();

    String nativeLoaderSessionMessage();

    String nativeLoaderWindowTitle();

    String nativeLoaderAdapterCoreServiceId();

    String nativeLoaderRuntimeHostClass();

    String nativeMinecraftRuntimeHostClass();

    String nativeMinecraftRuntimeHostId();

    String nativeLoaderBackendClass();

    String nativeLoaderRuntimeLane();

    String nativeUiActionCommand();

    default String nativeUiActionScannerUsed() {
        return "player.scanner_used";
    }

    default String nativeUiActionUseScanner() {
        return "native.ui.use_scanner";
    }

    default String nativeUiActionGrantItem() {
        return "player.inventory.grant";
    }

    default String nativeUiActionTerminalCommand() {
        return "native.ui.terminal_command";
    }

    default String nativeUiActionIndexSearch() {
        return "native.ui.index_search";
    }

    default String nativeUiActionHudRefresh() {
        return "native.ui.hud_refresh";
    }

    default String nativeUiActionMissionLogUpdate() {
        return "native.ui.mission_log_update";
    }

    default String nativeUiActionSurfaceOpen() {
        return "native.ui.surface_open";
    }

    default String nativeUiActionIndexBookmark() {
        return "native.ui.index_bookmark";
    }

    default String nativeUiActionHoloMapState() {
        return "native.ui.holomap_state";
    }

    default String nativeUiActionSignalOsTerminal() {
        return "native.ui.signalos_terminal";
    }

    default String nativeUiEventCommandExecution() {
        return "command_execution";
    }

    default String nativeUiEventTerminalOpened() {
        return "player.terminal_opened";
    }

    default String nativeUiEventClientTick() {
        return "client_tick";
    }

    default String nativeUiEventMissionObjectiveCompleted() {
        return "mission.objective_completed";
    }

    default String nativeGameplayBridgeKey() {
        return "nativeProductGameplayBridge";
    }

    default List<String> nativeLegacyGameplayBridgeKeys() {
        return List.of();
    }

    default String nativeGameplayBridgeId() {
        return "adaptercore.native_product_gameplay_content";
    }

    default String nativeGameplayPackId() {
        return namespace();
    }

    default String nativeGameplayDisplayName() {
        return namespace();
    }

    default String nativeGameplayHandlerClassName() {
        return "NativeProductGameplayHandlers";
    }

    default String nativeGameplayStandaloneRuntimeBackend() {
        return "EchoStandaloneRuntimeAdapterCoreGameplayBridge";
    }

    default String nativePlayableRuntimeKey() {
        return "nativeProductPlayableRuntime";
    }

    default String nativePlayableHudLedgerTarget() {
        return nativeGameplayPackId() + ".playable_beta.hud";
    }

    default String nativeIndexSearchQuery() {
        return nativeGameplayPackId();
    }

    default String nativeLensFallbackTarget() {
        String itemId = nativeInteractionProbeItemId();
        return itemId == null || itemId.isBlank()
                ? namespace() + ":native_lens_target"
                : itemId;
    }

    default String nativeCompatibilityDelegateClass() {
        return "";
    }

    default String nativeCompatibilityDelegateId() {
        return "";
    }

    default NativeBridgePolicy nativeInitialEventBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "descriptor_event_services",
                "Native event bridge remains pending until profile event services are visible to the live runtime.",
                Map.of(
                        "handlerSubscribed", false,
                        "runtimeEventPublished", false
                )
        );
    }

    default NativeBridgePolicy nativeInitialServiceBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "native_service_registry",
                "Native service bridge remains pending until profile service handles are attached to the live runtime.",
                Map.of("serviceRegistryInitialized", false)
        );
    }

    default NativeBridgePolicy nativeCreativeVisibilityBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "native_registry_creative_tab_and_existing_containers",
                "Native content is surfaced through a profile creative tab, with existing Minecraft creative containers patched as secondary visibility paths.",
                Map.of("timeoutSeconds", 1800)
        );
    }

    default NativeBridgePolicy nativeLiveClientProbePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "minecraft_client_thread_reflection_probe",
                "Native bootstrap waits for a live Minecraft client/player before publishing client-visible runtime probes.",
                Map.of(
                        "executed", false,
                        "hudProbeSent", false,
                        "chatProbeSent", false,
                        "windowTitleApplied", false,
                        "hudLabelSent", false,
                        "chatLabelSent", false
                )
        );
    }

    default NativeBridgePolicy nativeClientUiBridgePolicy() {
        return new NativeBridgePolicy(
                true,
                false,
                "profile_ui_surface_routes",
                "Native client UI host routes profile UI surfaces when their module bridges are available.",
                Map.of(
                        "clientUiHostAttached", false,
                        "terminalFallbackReady", false,
                        "indexFallbackReady", false,
                        "lensFallbackReady", false,
                        "hudFallbackReady", false,
                        "customMainMenuReady", false
                )
        );
    }

    default List<String> requiredNativeLifecycleCallbacks() {
        return List.of(
                "onModuleDiscovered",
                "onRegister",
                "onCommonSetup",
                "onClientSetup",
                "onResourcesReady",
                "onWorldReady",
                "onPlayerReady",
                "onFirstTick",
                "onRuntimeShutdown"
        );
    }

    List<String> requiredGameplayHandlerEvents();

    List<String> requiredAgent7WorldLiveHooks();

    List<String> requiredLiveMutationSurfaces();

    List<NativeEntityDefinition> nativeEntities();

    default List<NativeSourceBackedContentMapping> nativeSourceBackedItemMappings() {
        return List.of();
    }

    default List<NativeSourceBackedContentMapping> nativeSourceBackedBlockMappings() {
        return List.of();
    }

    default List<NativeSourceContractFile> nativeRegistrySourceContractFiles() {
        return List.of();
    }

    default List<NativeItemConstructorBinding> nativeItemConstructorBindings() {
        return List.of();
    }

    default List<NativeBlockConstructorBinding> nativeBlockConstructorBindings() {
        return List.of();
    }

    default Map<String, String> nativeModuleClassOverrides() {
        return Map.of();
    }

    default List<String> nativeModuleNamespacePrefixes() {
        return List.of("echo");
    }

    default Map<String, List<String>> nativeModuleServiceClasses() {
        return Map.of();
    }

    default Map<String, String> nativeInfoModuleRuntimeRoutes() {
        return Map.of();
    }

    default Map<String, String> nativeModuleDisplayNames() {
        return Map.of();
    }

    default Map<String, List<NativeInfoModuleStaticInvocation>> nativeInfoModuleStaticInvocations() {
        return Map.of();
    }

    default Map<String, List<NativeInfoModuleStaticValueInvocation>> nativeInfoModuleStaticValueInvocations() {
        return Map.of();
    }

    default Map<String, List<NativeInfoModuleStaticFieldValue>> nativeInfoModuleStaticFieldValues() {
        return Map.of();
    }

    default Map<String, List<NativeInfoModuleStaticFieldArgumentInvocation>> nativeInfoModuleStaticFieldArgumentInvocations() {
        return Map.of();
    }

    default List<NativeIntegrationHook> nativeIntegrationHooks() {
        return List.of();
    }

    default String nativeAdapterCoreMachineRuntimeClass() {
        return "";
    }

    default List<String> nativeRuntimeHostFactoryClasses() {
        return List.of();
    }

    default Map<String, String> nativeAdapterCoreGameplayClasses() {
        return Map.of();
    }

    default List<String> requiredNativeServiceSurfaces() {
        return List.of();
    }

    default Map<String, List<String>> nativeServiceSurfaceModules() {
        return Map.of();
    }

    default Map<String, String> nativeGameplayContentDataPrefixes() {
        return Map.of();
    }

    default String nativeResourcePackDescription() {
        return "ECHO Native runtime resources";
    }

    default String nativeSaveDatapackDescription() {
        return "ECHO Native worldgen";
    }

    default String nativeSaveDatapackFileName() {
        return "echo-native-" + namespace() + "-datapack.zip";
    }

    default String nativeSourceResourceRootMarker() {
        return "";
    }

    default List<String> nativeRequiredResourceEntries() {
        return List.of();
    }

    default List<String> nativeSaveDatapackEntryPrefixes() {
        return List.of();
    }

    default Map<String, String> nativeSaveDatapackRequiredEntriesByValidationKey() {
        return Map.of();
    }

    default List<String> nativeModuleResourceSourcePathMarkers() {
        return List.of();
    }

    default String nativeStructureTemplateSourcePrefix() {
        return "";
    }

    default String nativeStructureTemplateTargetPrefix() {
        return "";
    }

    default String nativeWorldgenStructurePrefix() {
        return "";
    }

    default String nativeWorldgenBiomePrefix() {
        return "";
    }

    default String nativeWorldPresetMirrorSource() {
        return "";
    }

    default String nativeWorldPresetMirrorTarget() {
        return "";
    }

    default String nativeMachineRecipeCatalogPath() {
        return "";
    }

    default String nativeMachineRecipeCatalogSourcePath() {
        return "";
    }

    default String nativeRecipeBehaviorContractSourcePath() {
        return "";
    }

    default List<String> nativeMachineRecipeCatalogTypes() {
        return List.of();
    }

    default List<NativeMachineScenarioRule> nativeMachineScenarioRules() {
        return List.of();
    }

    default List<NativeWorldPaintRecipe> nativeWorldPaintRecipes() {
        return List.of();
    }

    default String nativeItemGroupTranslationKey() {
        return "";
    }

    default String nativeItemGroupTranslationName() {
        return "";
    }

    default Map<String, List<String>> nativeCreativeTabPreferredIcons() {
        return Map.of();
    }

    default List<String> nativeItemShimPathHints() {
        return List.of();
    }

    default List<String> nativeBlockShimPathHints() {
        return List.of();
    }

    default List<NativeUiSurfaceRoute> nativeUiSurfaceRoutes() {
        return List.of();
    }

    default Map<String, String> nativeClientScreenClasses() {
        return Map.of();
    }

    default Map<String, String> nativeClientHudRendererClasses() {
        return Map.of();
    }

    default Map<String, String> nativeClientLoadingRendererClasses() {
        return Map.of();
    }

    default List<NativeUiActionRoute> nativeUiActionRoutes() {
        return List.of();
    }

    default List<String> nativeInfoModuleNamespaces() {
        return List.of();
    }

    default List<String> nativeInfoModulePlacementHints() {
        return List.of();
    }

    default List<String> nativeInfoModuleRewardItemHints() {
        return List.of();
    }

    default List<String> nativeInfoModuleFallbackBlockIds() {
        return List.of();
    }

    default List<String> nativeInfoModuleFallbackItemIds() {
        return List.of();
    }

    default List<String> nativeRecoveryModuleNamespaces() {
        return List.of();
    }

    default List<String> nativeRecoveryPlacementHints() {
        return List.of();
    }

    default List<String> nativeRecoveryBlockPlacementHints() {
        return List.of();
    }

    default List<String> nativeRecoveryRewardItemHints() {
        return List.of();
    }

    default List<String> nativeArcanaModuleNamespaces() {
        return List.of();
    }

    default List<String> nativeArcanaPlacementHints() {
        return List.of();
    }

    default List<String> nativeArcanaRewardItemHints() {
        return List.of();
    }

    default List<String> nativeArcanaFallbackItemIds() {
        return List.of();
    }

    default List<NativeModuleActionRoute> nativeModuleActionRoutes() {
        return List.of();
    }

    default Map<String, List<String>> nativeUiDataSourceRoots() {
        return Map.of();
    }

    default Map<String, String> nativeUiDefaultContentIds() {
        return Map.of();
    }

    default List<String> nativeMainMenuOptions() {
        return List.of("Continue", "New Run", "Settings", "Quit");
    }

    default List<NativePhysicalActionRoute> nativePhysicalActionRoutes() {
        return List.of();
    }

    default List<NativeActionKeyPathHints> nativeActionKeyPathHints() {
        return List.of();
    }

    default List<NativeRewardRule> nativeStarterRewardRules() {
        return List.of();
    }

    default List<NativeRewardRule> nativeBlockRewardRules() {
        return List.of();
    }

    default List<NativeItemActionRule> nativeItemActionRules() {
        return List.of();
    }

    default List<NativeBlockActionRule> nativeBlockActionRules() {
        return List.of();
    }

    default List<NativeFieldActionRoute> nativeItemFieldActionRoutes() {
        return List.of();
    }

    default List<NativeFieldActionRoute> nativeBlockFieldActionRoutes() {
        return List.of();
    }

    default Map<String, String> nativeBlockActionMachineIds() {
        return Map.of();
    }

    default List<NativeBlockFallbackRule> nativeBlockFallbackRules() {
        return List.of();
    }

    default String nativeDefaultBlockFallbackId() {
        return "minecraft:coarse_dirt";
    }

    default NativeMachineOperationRules nativeMachineOperationRules() {
        return NativeMachineOperationRules.empty();
    }

    default NativePathValueRules nativeBatteryCapacityRules() {
        return new NativePathValueRules(0, List.of());
    }

    default NativePathValueRules nativeMachineCapacityRules() {
        return new NativePathValueRules(2_000, List.of());
    }

    default NativePathValueRules nativeFuelEnergyRules() {
        return new NativePathValueRules(100, List.of());
    }

    default NativePathValueRules nativeEnergyItemChargeRules() {
        return new NativePathValueRules(1_000, List.of());
    }

    default NativeOutputRules nativeContaminatedOutputRules() {
        return new NativeOutputRules("minecraft:iron_ingot", List.of());
    }

    default List<String> nativeUiHotkeys() {
        return List.of();
    }

    default Map<String, String> nativeUiHotkeyConflicts() {
        return Map.of();
    }

    default String nativeRecoveryItemId() {
        return "";
    }

    default String nativePlayableProofMarkerBlockId() {
        return "";
    }

    default List<String> nativePlayableStarterToolItemIds() {
        return nativeRecoveryItemId().isBlank() ? List.of() : List.of(nativeRecoveryItemId());
    }

    default String nativePlayableStarterRegionCoreBlockId() {
        return nativePlayableCrashZoneCoreBlockId();
    }

    default String nativePlayableStarterRegionTerrainBlockId() {
        return nativePlayableCrashZoneTerrainBlockId();
    }

    default String nativePlayableStarterRegionSurfaceBlockId() {
        return nativePlayableCrashZoneSurfaceBlockId();
    }

    default List<String> nativePlayableStarterRegionFeatureBlockIds() {
        return nativePlayableCrashZoneFeatureBlockIds();
    }

    default String nativePlayableCrashZoneCoreBlockId() {
        return "";
    }

    default String nativePlayableCrashZoneTerrainBlockId() {
        return "";
    }

    default String nativePlayableCrashZoneSurfaceBlockId() {
        return "";
    }

    default List<String> nativePlayableCrashZoneFeatureBlockIds() {
        return List.of();
    }

    default List<String> nativePlayableStarterCommands() {
        return List.of();
    }

    default String nativeInteractionProbeItemId() {
        List<String> starterItems = nativePlayableStarterToolItemIds();
        return starterItems.isEmpty() ? nativeRecoveryItemId() : starterItems.get(0);
    }

    default String nativeInteractionProbePlacementBlockId() {
        List<String> featureBlocks = nativePlayableStarterRegionFeatureBlockIds();
        return featureBlocks.isEmpty() ? nativePlayableProofMarkerBlockId() : featureBlocks.get(0);
    }

    default String nativeInteractionProbePlacementFallbackBlockId() {
        return "minecraft:barrel";
    }

    default String nativeInteractionProbeBlockUseId() {
        return nativeInteractionProbePlacementBlockId();
    }

    default String nativeInteractionProbeCommand() {
        return "";
    }

    default String nativeInteractionProbeEntityItemId() {
        return nativeInteractionProbeItemId();
    }

    default List<String> nativeMachinePaths() {
        return List.of();
    }

    default String nativeMachineScreenId() {
        return "";
    }

    default String nativeMachineEffectPrefix() {
        return "";
    }

    default String id(String path) {
        return namespace() + ":" + path;
    }

    record NativeEntityDefinition(
            String path,
            String className,
            String fallbackClassName,
            String category,
            float width,
            float height,
            int trackingRange,
            boolean fireImmune
    ) {
        public String id(String namespace) {
            return namespace + ":" + path;
        }
    }

    record NativeSourceBackedContentMapping(
            String id,
            String family,
            String sourcePath,
            String sourceClass,
            String nativeBridgeMethod
    ) {
    }

    record NativeSourceContractFile(
            String kind,
            String path
    ) {
    }

    record NativeItemConstructorBinding(
            String id,
            String className,
            String enumClassName,
            String enumName
    ) {
    }

    record NativeBlockConstructorBinding(
            String id,
            String className,
            String constructorKind,
            int intParamOne,
            int intParamTwo
    ) {
    }

    record NativeIntegrationHook(
            String role,
            String reportKey,
            String className,
            String methodName
    ) {
    }

    record NativeInfoModuleStaticInvocation(
            String reportKey,
            String className,
            String methodName
    ) {
    }

    record NativeInfoModuleStaticValueInvocation(
            String reportKey,
            String className,
            String methodName
    ) {
    }

    record NativeInfoModuleStaticFieldValue(
            String reportKey,
            String className,
            String fieldName
    ) {
    }

    record NativeInfoModuleStaticFieldArgumentInvocation(
            String reportKey,
            String className,
            String methodName,
            String fieldValueKey
    ) {
    }

    record NativeUiActionRoute(
            String surface,
            String actionId,
            String screenId,
            String canonicalId,
            String target,
            String source,
            String bridgeClass,
            String effectPrefix,
            String packetClassName,
            String contextualKey,
            String contextualDeclaredAction,
            String contextualAction,
            Map<String, String> commandsByAction
    ) {
    }

    record NativeUiSurfaceRoute(
            String surface,
            String screenId,
            String canonicalId,
            String target
    ) {
    }

    record NativeModuleActionRoute(
            String namespace,
            String paintStyle,
            List<String> itemRewardIds,
            List<String> blockRewardIds,
            String itemSummary,
            String blockSummary,
            String itemScenarioMethod,
            String blockScenarioMethod,
            boolean blockScenarioFromPath,
            List<String> itemCommands,
            List<String> blockCommands,
            List<NativeModulePathActionRoute> blockPathRoutes
    ) {
        public NativeModuleActionRoute(
                String namespace,
                String paintStyle,
                List<String> itemRewardIds,
                List<String> blockRewardIds,
                String itemSummary,
                String blockSummary
        ) {
            this(
                    namespace,
                    paintStyle,
                    itemRewardIds,
                    blockRewardIds,
                    itemSummary,
                    blockSummary,
                    "",
                    "",
                    false,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        public NativeModuleActionRoute {
            namespace = namespace == null ? "" : namespace;
            paintStyle = paintStyle == null ? "" : paintStyle;
            itemRewardIds = itemRewardIds == null ? List.of() : List.copyOf(itemRewardIds);
            blockRewardIds = blockRewardIds == null ? List.of() : List.copyOf(blockRewardIds);
            itemSummary = itemSummary == null ? "" : itemSummary;
            blockSummary = blockSummary == null ? "" : blockSummary;
            itemScenarioMethod = itemScenarioMethod == null ? "" : itemScenarioMethod;
            blockScenarioMethod = blockScenarioMethod == null ? "" : blockScenarioMethod;
            itemCommands = itemCommands == null ? List.of() : List.copyOf(itemCommands);
            blockCommands = blockCommands == null ? List.of() : List.copyOf(blockCommands);
            blockPathRoutes = blockPathRoutes == null ? List.of() : List.copyOf(blockPathRoutes);
        }
    }

    record NativeModulePathActionRoute(
            List<String> pathHints,
            String paintStyle,
            List<String> rewardIds,
            List<String> blockPlacementIds,
            List<String> commands,
            String summary
    ) {
        public NativeModulePathActionRoute {
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
            paintStyle = paintStyle == null ? "" : paintStyle;
            rewardIds = rewardIds == null ? List.of() : List.copyOf(rewardIds);
            blockPlacementIds = blockPlacementIds == null ? List.of() : List.copyOf(blockPlacementIds);
            commands = commands == null ? List.of() : List.copyOf(commands);
            summary = summary == null ? "" : summary;
        }
    }

    record NativePhysicalActionRoute(
            String key,
            String surface,
            String action,
            boolean contextual
    ) {
    }

    record NativeActionKeyPathHints(
            String actionKey,
            List<String> pathHints
    ) {
    }

    record NativeRewardRule(
            List<String> pathHints,
            List<NativeRewardGrant> grants
    ) {
        public NativeRewardRule {
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
            grants = grants == null ? List.of() : List.copyOf(grants);
        }
    }

    record NativeRewardGrant(
            String itemId,
            int count,
            String fallbackItemId,
            int fallbackCount
    ) {
        public NativeRewardGrant(String itemId, int count) {
            this(itemId, count, "", 0);
        }

        public NativeRewardGrant {
            itemId = itemId == null ? "" : itemId;
            fallbackItemId = fallbackItemId == null ? "" : fallbackItemId;
            count = Math.max(1, count);
            fallbackCount = Math.max(0, fallbackCount);
        }
    }

    record NativeFieldActionRoute(
            String actionKey,
            String paintStyle,
            String scenarioMethod,
            boolean scenarioFromPath,
            List<NativeRewardGrant> grants,
            List<NativeRewardGrant> oneShotGrants,
            String oneShotSuffix,
            boolean oneShotUsesBlockPosition,
            List<NativeWorldPaintPlacement> blockPlacements,
            List<String> commands,
            String actionBarText,
            String actionBarColor,
            boolean grantBlockReward,
            String summary
    ) {
        public NativeFieldActionRoute {
            actionKey = actionKey == null ? "" : actionKey;
            paintStyle = paintStyle == null ? "" : paintStyle;
            scenarioMethod = scenarioMethod == null ? "" : scenarioMethod;
            grants = grants == null ? List.of() : List.copyOf(grants);
            oneShotGrants = oneShotGrants == null ? List.of() : List.copyOf(oneShotGrants);
            oneShotSuffix = oneShotSuffix == null ? "" : oneShotSuffix;
            blockPlacements = blockPlacements == null ? List.of() : List.copyOf(blockPlacements);
            commands = commands == null ? List.of() : List.copyOf(commands);
            actionBarText = actionBarText == null ? "" : actionBarText;
            actionBarColor = actionBarColor == null ? "" : actionBarColor;
            summary = summary == null ? "" : summary;
        }
    }

    record NativeItemActionRule(
            String action,
            List<String> pathHints
    ) {
        public NativeItemActionRule {
            action = action == null ? "" : action;
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
        }
    }

    record NativeBlockActionRule(
            String action,
            List<String> pathHints
    ) {
        public NativeBlockActionRule {
            action = action == null ? "" : action;
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
        }
    }

    record NativeBlockFallbackRule(
            List<String> pathHints,
            String fallbackBlockId
    ) {
        public NativeBlockFallbackRule {
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
            fallbackBlockId = fallbackBlockId == null ? "" : fallbackBlockId;
        }
    }

    record NativeMachineOperationRules(
            List<String> powerNodeFuelItemIds,
            List<String> machineChargeItemIds,
            List<String> waterFilterItemIds,
            String dirtyWaterInputItemId,
            String cleanWaterOutputItemId,
            int waterFilterTicks,
            List<String> radiationCleanserFilterItemIds,
            List<String> contaminatedInputItemIds,
            int radiationCleanserEnergy,
            int radiationCleanserTicks,
            int medicalMachineEnergy,
            int medicalMachineTicks,
            int hazardMachineEnergy,
            int hazardMachineTicks,
            List<String> researchSchematicItemIds,
            List<String> generatorFuelItemIds,
            List<String> gridChargeItemIds,
            int gridExtractEnergy,
            String gridExtractOutputItemId,
            List<String> waterCollectionPathHints,
            List<String> radiationCleanserPathHints,
            List<String> medicalMachinePathHints
    ) {
        public NativeMachineOperationRules {
            powerNodeFuelItemIds = copy(powerNodeFuelItemIds);
            machineChargeItemIds = copy(machineChargeItemIds);
            waterFilterItemIds = copy(waterFilterItemIds);
            dirtyWaterInputItemId = safe(dirtyWaterInputItemId);
            cleanWaterOutputItemId = safe(cleanWaterOutputItemId);
            waterFilterTicks = Math.max(1, waterFilterTicks);
            radiationCleanserFilterItemIds = copy(radiationCleanserFilterItemIds);
            contaminatedInputItemIds = copy(contaminatedInputItemIds);
            radiationCleanserEnergy = Math.max(0, radiationCleanserEnergy);
            radiationCleanserTicks = Math.max(1, radiationCleanserTicks);
            medicalMachineEnergy = Math.max(0, medicalMachineEnergy);
            medicalMachineTicks = Math.max(1, medicalMachineTicks);
            hazardMachineEnergy = Math.max(0, hazardMachineEnergy);
            hazardMachineTicks = Math.max(1, hazardMachineTicks);
            researchSchematicItemIds = copy(researchSchematicItemIds);
            generatorFuelItemIds = copy(generatorFuelItemIds);
            gridChargeItemIds = copy(gridChargeItemIds);
            gridExtractEnergy = Math.max(0, gridExtractEnergy);
            gridExtractOutputItemId = safe(gridExtractOutputItemId);
            waterCollectionPathHints = copy(waterCollectionPathHints);
            radiationCleanserPathHints = copy(radiationCleanserPathHints);
            medicalMachinePathHints = copy(medicalMachinePathHints);
        }

        static NativeMachineOperationRules empty() {
            return new NativeMachineOperationRules(
                    List.of(),
                    List.of(),
                    List.of(),
                    "",
                    "",
                    1,
                    List.of(),
                    List.of(),
                    0,
                    1,
                    0,
                    1,
                    0,
                    1,
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    "",
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        private static List<String> copy(List<String> values) {
            return values == null ? List.of() : List.copyOf(values);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    record NativePathValueRules(
            int defaultValue,
            List<NativePathValueRule> rules
    ) {
        public NativePathValueRules {
            defaultValue = Math.max(0, defaultValue);
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }

    record NativePathValueRule(
            List<String> pathHints,
            int value
    ) {
        public NativePathValueRule {
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
            value = Math.max(0, value);
        }
    }

    record NativeMachineScenarioRule(
            List<String> pathHints,
            String scenarioMethod
    ) {
        public NativeMachineScenarioRule {
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
            scenarioMethod = scenarioMethod == null ? "" : scenarioMethod;
        }
    }

    record NativeWorldPaintRecipe(
            String style,
            List<NativeWorldPaintPlacement> placements
    ) {
        public NativeWorldPaintRecipe {
            style = style == null ? "" : style;
            placements = placements == null ? List.of() : List.copyOf(placements);
        }
    }

    record NativeWorldPaintPlacement(
            int dx,
            int dy,
            int dz,
            String blockId,
            String fallbackBlockId,
            boolean productRelative
    ) {
        public NativeWorldPaintPlacement {
            blockId = blockId == null ? "" : blockId;
            fallbackBlockId = fallbackBlockId == null ? "" : fallbackBlockId;
        }
    }

    record NativeOutputRules(
            String defaultOutputId,
            List<NativeOutputRule> rules
    ) {
        public NativeOutputRules {
            defaultOutputId = defaultOutputId == null ? "" : defaultOutputId;
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }

    record NativeOutputRule(
            List<String> pathHints,
            String outputId
    ) {
        public NativeOutputRule {
            pathHints = pathHints == null ? List.of() : List.copyOf(pathHints);
            outputId = outputId == null ? "" : outputId;
        }
    }

    record NativeBridgePolicy(
            boolean installed,
            boolean applied,
            String strategy,
            String summary,
            Map<String, Object> attributes
    ) {
        public NativeBridgePolicy {
            strategy = strategy == null ? "" : strategy;
            summary = summary == null ? "" : summary;
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }
}
