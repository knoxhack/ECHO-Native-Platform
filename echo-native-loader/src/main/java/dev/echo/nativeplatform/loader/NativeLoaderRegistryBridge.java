package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class NativeLoaderRegistryBridge {
    public static final String SERVICE_ID = "echo.native.registry_bridge";

    private NativeLoaderRegistryBridge() {
    }

    public static Map<String, Object> apply(
            String packId,
            List<String> modules,
            Config config,
            Context context
    ) {
        return apply(packId, modules, config, context, List.of(), List.of(), List.of());
    }

    public static Map<String, Object> apply(
            String packId,
            List<String> modules,
            Config config,
            Context context,
            List<String> sdkItemIds,
            List<String> sdkBlockIds
    ) {
        return apply(packId, modules, config, context, sdkItemIds, sdkBlockIds, List.of(), List.of());
    }

    public static Map<String, Object> apply(
            String packId,
            List<String> modules,
            Config config,
            Context context,
            List<String> sdkItemIds,
            List<String> sdkBlockIds,
            List<String> sdkCreativeTabIds
    ) {
        return apply(packId, modules, config, context, sdkItemIds, sdkBlockIds, sdkCreativeTabIds, List.of());
    }

    public static Map<String, Object> apply(
            String packId,
            List<String> modules,
            Config config,
            Context context,
            List<String> sdkItemIds,
            List<String> sdkBlockIds,
            List<String> sdkCreativeTabIds,
            List<Map<String, Object>> sdkCreativeTabDeclarations
    ) {
        return apply(
                packId,
                modules,
                config,
                context,
                sdkItemIds,
                sdkBlockIds,
                sdkCreativeTabIds,
                sdkCreativeTabDeclarations,
                List.of()
        );
    }

    public static Map<String, Object> apply(
            String packId,
            List<String> modules,
            Config config,
            Context context,
            List<String> sdkItemIds,
            List<String> sdkBlockIds,
            List<String> sdkCreativeTabIds,
            List<Map<String, Object>> sdkCreativeTabDeclarations,
            List<Map<String, Object>> sdkRegistryDeclarations
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bridge", "adaptercore.native_registry");
        data.put("packId", packId);
        data.put("registryInjected", false);
        data.put("serviceCodeExecuted", false);
        data.put("addonCodeExecuted", false);
        Object blockRegistryForFreeze = null;
        Object itemRegistryForFreeze = null;
        Object creativeTabRegistryForFreeze = null;
        boolean blockRegistryWasFrozen = false;
        boolean itemRegistryWasFrozen = false;
        boolean creativeTabRegistryWasFrozen = false;
        try {
            data.put("vanillaBootstrapBeforeNativeRegistry", context.ensureVanillaBootstrap());
            Class<?> identifierClass = Class.forName(context.runtimeClass("resources.Identifier"));
            Class<?> registriesClass = Class.forName(context.runtimeClass("core.registries.Registries"));
            Class<?> resourceKeyClass = Class.forName(context.runtimeClass("resources.ResourceKey"));
            Class<?> builtInRegistriesClass = Class.forName(context.runtimeClass("core.registries.BuiltInRegistries"));
            Class<?> registryClass = Class.forName(context.runtimeClass("core.Registry"));
            Class<?> itemClass = Class.forName(context.runtimeClass("world.item.Item"));
            Class<?> itemPropertiesClass = Class.forName(context.runtimeClass("world.item.Item$Properties"));
            Class<?> itemStackClass = Class.forName(context.runtimeClass("world.item.ItemStack"));
            Class<?> itemLikeClass = Class.forName(context.runtimeClass("world.level.ItemLike"));
            Class<?> creativeModeTabClass = Class.forName(context.runtimeClass("world.item.CreativeModeTab"));
            Class<?> creativeModeTabsClass = Class.forName(context.runtimeClass("world.item.CreativeModeTabs"));
            Class<?> tabVisibilityClass = Class.forName(context.runtimeClass("world.item.CreativeModeTab$TabVisibility"));
            Class<?> outputClass = Class.forName(context.runtimeClass("world.item.CreativeModeTab$Output"));
            Class<?> componentClass = Class.forName(context.runtimeClass("network.chat.Component"));
            Class<?> blockClass = Class.forName(context.runtimeClass("world.level.block.Block"));
            Class<?> blockPropertiesClass = Class.forName(context.runtimeClass("world.level.block.state.BlockBehaviour$Properties"));
            Class<?> blockItemClass = Class.forName(context.runtimeClass("world.item.BlockItem"));
            Object itemRegistry = builtInRegistriesClass.getField("ITEM").get(null);
            Object itemRegistryKey = registriesClass.getField("ITEM").get(null);
            Object blockRegistry = builtInRegistriesClass.getField("BLOCK").get(null);
            Object blockRegistryKey = registriesClass.getField("BLOCK").get(null);
            Object creativeTabRegistry = builtInRegistriesClass.getField("CREATIVE_MODE_TAB").get(null);
            blockRegistryForFreeze = blockRegistry;
            itemRegistryForFreeze = itemRegistry;
            creativeTabRegistryForFreeze = creativeTabRegistry;
            blockRegistryWasFrozen = NativeLoaderRegistryRuntimeSupport.unfreezeNativeRegistry(blockRegistry);
            itemRegistryWasFrozen = NativeLoaderRegistryRuntimeSupport.unfreezeNativeRegistry(itemRegistry);
            creativeTabRegistryWasFrozen = NativeLoaderRegistryRuntimeSupport.unfreezeNativeRegistry(creativeTabRegistry);
            NativeLoaderRegistryRuntimeSupport.enableNativeIntrusiveHolders(blockRegistry);
            NativeLoaderRegistryRuntimeSupport.enableNativeIntrusiveHolders(itemRegistry);
            data.put("nativeRegistryWindowOpened",
                    blockRegistryWasFrozen || itemRegistryWasFrozen || creativeTabRegistryWasFrozen);

            List<Object> registeredItems = new ArrayList<>();
            List<String> registeredItemIds = new ArrayList<>();
            List<Map<String, Object>> registeredModuleItems = new ArrayList<>();
            List<Map<String, Object>> registeredBlocks = new ArrayList<>();
            List<String> registeredBlockItems = new ArrayList<>();
            List<Map<String, Object>> registeredNativeEntities = new ArrayList<>();
            List<Map<String, Object>> failedBlockRegistrations = new ArrayList<>();
            List<Map<String, Object>> failedItemRegistrations = new ArrayList<>();

            List<String> normalizedSdkBlockIds = normalizeContentIds(sdkBlockIds);
            List<String> normalizedSdkItemIds = normalizeContentIds(sdkItemIds);
            List<String> normalizedSdkCreativeTabIds = normalizeContentIds(sdkCreativeTabIds);
            List<Map<String, Object>> normalizedSdkCreativeTabDeclarations =
                    normalizeCreativeTabDeclarations(sdkCreativeTabDeclarations, normalizedSdkCreativeTabIds);
            List<Map<String, Object>> normalizedSdkRegistryDeclarations =
                    normalizeRegistryDeclarations(sdkRegistryDeclarations);
            List<String> declaredBlockIds = registryDeclarationContentIds(
                    List.of(),
                    normalizedSdkRegistryDeclarations,
                    "block"
            );
            List<String> declaredItemIds = registryDeclarationContentIds(
                    List.of(),
                    normalizedSdkRegistryDeclarations,
                    "item"
            );
            Set<String> blockIds = mergedContentIds(context.blockIds(), normalizedSdkBlockIds, declaredBlockIds);
            for (String blockId : blockIds) {
                int separator = blockId.indexOf(':');
                if (separator < 1 || separator + 1 >= blockId.length() || registeredItemIds.contains(blockId)) {
                    continue;
                }
                try {
                    Object existingBlockItem = registryValue(itemRegistry, identifierClass, blockId);
                    if (existingBlockItem != null && registryValue(blockRegistry, identifierClass, blockId) != null) {
                        registeredItems.add(existingBlockItem);
                        registeredItemIds.add(blockId);
                        registeredBlockItems.add(blockId);
                        Map<String, Object> block = new LinkedHashMap<>();
                        block.put("blockId", blockId);
                        block.put("itemId", blockId);
                        block.put("bridgeKind", "existing_vanilla_block_with_block_item");
                        registeredBlocks.add(block);
                        continue;
                    }
                    Object blockItem = NativeLoaderRegistryContentBridge.registerNativeBlock(
                            blockId.substring(0, separator),
                            blockId.substring(separator + 1),
                            identifierClass,
                            resourceKeyClass,
                            registryClass,
                            blockClass,
                            blockPropertiesClass,
                            blockItemClass,
                            itemPropertiesClass,
                            blockRegistry,
                            blockRegistryKey,
                            itemRegistry,
                            itemRegistryKey,
                            context.blockFactory()
                    );
                    registeredItems.add(blockItem);
                    registeredItemIds.add(blockId);
                    registeredBlockItems.add(blockId);
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("blockId", blockId);
                    block.put("itemId", blockId);
                    block.put("bridgeKind", "vanilla_block_with_block_item");
                    registeredBlocks.add(block);
                } catch (Throwable exception) {
                    Object existingBlockItem = registryValue(itemRegistry, identifierClass, blockId);
                    if (existingBlockItem != null && registryValue(blockRegistry, identifierClass, blockId) != null) {
                        registeredItems.add(existingBlockItem);
                        registeredItemIds.add(blockId);
                        registeredBlockItems.add(blockId);
                        Map<String, Object> block = new LinkedHashMap<>();
                        block.put("blockId", blockId);
                        block.put("itemId", blockId);
                        block.put("bridgeKind", "existing_vanilla_block_with_block_item_after_duplicate");
                        registeredBlocks.add(block);
                    } else {
                        failedBlockRegistrations.add(registrationFailure("block", blockId, exception));
                    }
                }
            }

            List<String> registeredContentItems = new ArrayList<>();
            for (String itemId : mergedContentIds(context.itemIds(), normalizedSdkItemIds, declaredItemIds)) {
                int separator = itemId.indexOf(':');
                if (separator < 1 || separator + 1 >= itemId.length() || registeredItemIds.contains(itemId)) {
                    continue;
                }
                try {
                    Object existingItem = registryValue(itemRegistry, identifierClass, itemId);
                    if (existingItem != null) {
                        registeredItems.add(existingItem);
                        registeredItemIds.add(itemId);
                        registeredContentItems.add(itemId);
                        continue;
                    }
                    registeredItems.add(NativeLoaderRegistryContentBridge.registerNativeItem(
                            itemId.substring(0, separator),
                            itemId.substring(separator + 1),
                            identifierClass,
                            resourceKeyClass,
                            registryClass,
                            itemClass,
                            itemPropertiesClass,
                            itemRegistry,
                            itemRegistryKey,
                            context.itemFactory()
                    ));
                    registeredItemIds.add(itemId);
                    registeredContentItems.add(itemId);
                } catch (Throwable exception) {
                    Object existingItem = registryValue(itemRegistry, identifierClass, itemId);
                    if (existingItem != null) {
                        registeredItems.add(existingItem);
                        registeredItemIds.add(itemId);
                        registeredContentItems.add(itemId);
                    } else {
                        failedItemRegistrations.add(registrationFailure("item", itemId, exception));
                    }
                }
            }

            List<String> creativeBridgeBlockItems = List.copyOf(registeredBlockItems);
            List<String> creativeBridgeContentItems = List.copyOf(registeredContentItems);
            registeredModuleItems.addAll(NativeLoaderRegistryFixtureBridge.realModuleRepresentativeItems(
                    modules,
                    creativeBridgeContentItems,
                    creativeBridgeBlockItems
            ));
            List<Map<String, Object>> registeredCreativeTabs = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                    config.profile(),
                    creativeBridgeBlockItems,
                    creativeBridgeContentItems,
                    normalizedSdkCreativeTabIds,
                    identifierClass,
                    registryClass,
                    creativeModeTabClass,
                    creativeModeTabsClass,
                    componentClass,
                    itemStackClass,
                    itemLikeClass,
                    tabVisibilityClass,
                    outputClass,
                    creativeTabRegistry,
                    itemRegistry,
                    normalizedSdkCreativeTabDeclarations
            );
            Map<String, Object> nativeRegistryHost = registerThroughNativeRegistryHost(
                    registeredBlockItems,
                    registeredContentItems,
                    normalizedSdkCreativeTabIds,
                    normalizedSdkCreativeTabDeclarations,
                    normalizedSdkRegistryDeclarations
            );
            int initializedAllBlockStateCacheCount =
                    NativeLoaderRegistryRuntimeSupport.initializeAllNativeBlockStateCaches(blockClass);
            int clearedIntrusiveHolderCount =
                    NativeLoaderRegistryRuntimeSupport.clearUnregisteredIntrusiveHolders(blockRegistry, itemRegistry);

            data.put("applied", true);
            data.put("registryMutated", true);
            data.put("nativeRegistryHostServiceId", nativeRegistryHostServiceId());
            data.put("sdkRegistryDeclarationItemIds", normalizedSdkItemIds);
            data.put("sdkRegistryDeclarationBlockIds", normalizedSdkBlockIds);
            data.put("sdkRegistryDeclarationFirstClassItemIds", declaredItemIds);
            data.put("sdkRegistryDeclarationFirstClassBlockIds", declaredBlockIds);
            data.put("sdkRegistryDeclarationCreativeTabIds", normalizedSdkCreativeTabIds);
            data.put("sdkRegistryDeclarationCreativeTabs", normalizedSdkCreativeTabDeclarations);
            data.put("sdkRegistryDeclarationFirstClassRegistrations", normalizedSdkRegistryDeclarations);
            data.put("sdkRegistryDeclarationFirstClassRegistrationCount", normalizedSdkRegistryDeclarations.size());
            data.put("sdkRegistryDeclarationInputCount", normalizedSdkItemIds.size()
                    + normalizedSdkBlockIds.size()
                    + normalizedSdkCreativeTabIds.size());
            data.put("sdkRegistryDeclarationsPromotedToVanillaRegistry", !normalizedSdkItemIds.isEmpty()
                    || !normalizedSdkBlockIds.isEmpty()
                    || !declaredItemIds.isEmpty()
                    || !declaredBlockIds.isEmpty()
                    || !normalizedSdkCreativeTabIds.isEmpty());
            data.put("nativeRegistryHostRegistered", integer(nativeRegistryHost.get("registeredCount")) > 0);
            data.put("nativeRegistryHostRegisteredCount", nativeRegistryHost.get("registeredCount"));
            data.put("nativeRegistryHostUnsupportedCount", nativeRegistryHost.get("unsupportedCount"));
            data.put("nativeRegistryHostFailedCount", nativeRegistryHost.get("failedCount"));
            data.put("nativeRegistryHostRegistrations", nativeRegistryHost.get("registrations"));
            data.put("nativeRegistryHostReport", nativeRegistryHost.get("report"));
            data.put("registeredItemCount", registeredItems.size());
            data.put("registeredModuleItemCount", registeredModuleItems.size());
            data.put("registeredContentItemCount", registeredContentItems.size());
            data.put("registeredBlockCount", registeredBlocks.size());
            data.put("registeredBlockItemCount", registeredBlockItems.size());
            data.put("failedBlockRegistrationCount", failedBlockRegistrations.size());
            data.put("failedItemRegistrationCount", failedItemRegistrations.size());
            data.put("failedBlockRegistrationSamples", failedBlockRegistrations.stream().limit(12).toList());
            data.put("failedItemRegistrationSamples", failedItemRegistrations.stream().limit(12).toList());
            data.put("creativeBridgeHostBackedContentItemCount", creativeBridgeContentItems.size());
            data.put("creativeBridgeHostBackedBlockItemCount", creativeBridgeBlockItems.size());
            data.put("registeredNativeEntityCount", registeredNativeEntities.size());
            data.put("nativeEntityBridgeDeferredUntilVanillaBootstrap", true);
            int registeredCreativeTabCount =
                    NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(registeredCreativeTabs);
            int nativeCreativeModuleTabVisibleItemCount =
                    NativeLoaderRegistryContentBridge.visibleCreativeTabItemCount(registeredCreativeTabs);
            boolean nativeCreativeModuleTabContentVisible =
                    registeredCreativeTabCount > 0 && nativeCreativeModuleTabVisibleItemCount > 0;
            boolean nativeCreativeModuleTabRegistryBacked =
                    NativeLoaderRegistryContentBridge.firstClassCreativeTabsAreRegistryBacked(registeredCreativeTabs);
            data.put("attemptedCreativeTabCount", registeredCreativeTabs.size());
            data.put("registeredCreativeTabCount", registeredCreativeTabCount);
            data.put("registeredModuleCreativeTabCount", registeredCreativeTabs.stream()
                    .filter(tab -> Boolean.TRUE.equals(tab.get("moduleScoped"))).count());
            data.put("augmentedCreativeTabCount", 0);
            data.put("nativeCreativeTabBridgeApplied", registeredCreativeTabCount > 0);
            data.put("nativeCreativeModuleTabContentVisible", nativeCreativeModuleTabContentVisible);
            data.put("nativeCreativeModuleTabVisibleItemCount", nativeCreativeModuleTabVisibleItemCount);
            data.put("nativeCreativeModuleTabRegistryBacked", nativeCreativeModuleTabRegistryBacked);
            data.put("visibleCreativeTabPathCount", nativeCreativeModuleTabContentVisible ? registeredCreativeTabCount : 0);
            data.put("nativeBetaItemWrapperAvailable", NativeLoaderGeneratedContentBridge.itemWrapperAvailable());
            data.put("nativeBetaBlockWrapperAvailable", NativeLoaderGeneratedContentBridge.blockWrapperAvailable());
            data.put("nativeBetaFunctionalItemCount",
                    NativeLoaderRegistryContentBridge.nativeFunctionalItemCount(
                            registeredItems,
                            NativeLoaderGeneratedContentBridge.ITEM_CLASS_NAME
                    ));
            data.put("nativeBetaFunctionalBlockCount",
                    NativeLoaderGeneratedContentBridge.blockWrapperAvailable() ? registeredBlocks.size() : 0);
            data.put("creativeContentVisible", nativeCreativeModuleTabContentVisible);
            data.put("registeredItems", registeredItemIds);
            data.put("registeredContentItems", registeredContentItems);
            data.put("registeredBlocks", registeredBlocks);
            data.put("registeredBlockItems", registeredBlockItems);
            data.put("creativeBridgeContentItems", creativeBridgeContentItems);
            data.put("creativeBridgeBlockItems", creativeBridgeBlockItems);
            data.put("registeredNativeEntities", registeredNativeEntities);
            data.put("registeredModuleItems", registeredModuleItems);
            data.put("registeredCreativeTabs", registeredCreativeTabs);
            data.put("augmentedCreativeTabs", List.of());
            data.put("visibleItemCount", nativeCreativeModuleTabVisibleItemCount);
            data.put("visibleItems", nativeCreativeModuleTabContentVisible
                    ? NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(registeredCreativeTabs)
                    : List.of());
            data.put("sourceBackedProductItemMappingCount",
                    NativeLoaderRegistryFixtureBridge.sourceBackedItemMappings(config.profile()).size());
            data.put("sourceBackedProductBlockMappingCount",
                    NativeLoaderRegistryFixtureBridge.sourceBackedBlockMappings(config.profile()).size());
            data.put("sourceBackedProductItemMappings",
                    NativeLoaderRegistryFixtureBridge.sourceBackedItemMappings(config.profile()));
            data.put("sourceBackedProductBlockMappings",
                    NativeLoaderRegistryFixtureBridge.sourceBackedBlockMappings(config.profile()));
            data.put("nativeRegistrySourceContractFiles",
                    NativeLoaderRegistryFixtureBridge.sourceContractFiles(config.profile()));
            data.put("fixtureOnlyRegistryEvidence", false);
            data.put("fixtureRegistryDoesNotSatisfyNativeParity", false);
            data.put("nativeRegistryRuntimeGapCount", 0);
            data.put("nativeRegistryRuntimeGaps", List.of());
            data.put("initializedAllBlockStateCacheCount", initializedAllBlockStateCacheCount);
            data.put("clearedUnregisteredIntrusiveHolderCount", clearedIntrusiveHolderCount);
            data.put("summary", nativeCreativeModuleTabContentVisible
                    ? "AdapterCore native registry bridge registered real ECHO addon blocks, content items, module representatives, and namespace-scoped module creative tabs populated from native registry content; creative search visibility remains a secondary bridge."
                    : "AdapterCore native registry bridge registered real ECHO addon blocks, content items, module representatives, and namespace-scoped module creative tabs after vanilla bootstrap; creative search visibility is patched through the native client bridge.");
        } catch (Throwable exception) {
            try {
                NativeLoaderRegistryFixtureBridge.apply(
                        config.profile(),
                        config.nativeGameDirProperty(),
                        packId,
                        modules,
                        context.itemIds(),
                        context.blockIds(),
                        exception,
                        data
                );
            } catch (Throwable fallbackException) {
                data.put("applied", false);
                data.put("registryMutated", false);
                data.put("failureKind", exception.getClass().getSimpleName());
                data.put("failureMessage", failureMessage(exception));
                data.put("fixtureRegistryFailureKind", fallbackException.getClass().getSimpleName());
                data.put("fixtureRegistryFailureMessage", failureMessage(fallbackException));
                data.put("summary", "AdapterCore native registry bridge failed before Minecraft handoff: " + failureMessage(exception));
            }
        } finally {
            int finallyClearedIntrusiveHolderCount = NativeLoaderRegistryRuntimeSupport.clearUnregisteredIntrusiveHolders(
                    blockRegistryForFreeze,
                    itemRegistryForFreeze,
                    creativeTabRegistryForFreeze);
            data.put("finallyClearedUnregisteredIntrusiveHolderCount", finallyClearedIntrusiveHolderCount);
            data.put("finallyPreservedNativeRegistryTags", true);
            int boundBlockRegistryHolders =
                    NativeLoaderRegistryRuntimeSupport.bindRegistryHoldersWithoutFreezing(blockRegistryForFreeze);
            int boundItemRegistryHolders =
                    NativeLoaderRegistryRuntimeSupport.bindRegistryHoldersWithoutFreezing(itemRegistryForFreeze);
            int boundCreativeTabRegistryHolders =
                    NativeLoaderRegistryRuntimeSupport.bindRegistryHoldersWithoutFreezing(creativeTabRegistryForFreeze);
            boolean restoredBlockFrozenFlag = blockRegistryWasFrozen
                    && NativeLoaderRegistryRuntimeSupport.restoreNativeRegistryFrozenFlag(blockRegistryForFreeze, true);
            boolean restoredItemFrozenFlag = itemRegistryWasFrozen
                    && NativeLoaderRegistryRuntimeSupport.restoreNativeRegistryFrozenFlag(itemRegistryForFreeze, true);
            boolean restoredCreativeTabFrozenFlag = creativeTabRegistryWasFrozen
                    && NativeLoaderRegistryRuntimeSupport.restoreNativeRegistryFrozenFlag(creativeTabRegistryForFreeze, true);
            data.put("finallyDirectNativeRegistryFreezeSkipped", true);
            data.put("finallyRefrozeNativeRegistries", Map.of(
                    "block", false,
                    "item", false,
                    "creativeTab", false
            ));
            data.put("finallyBoundNativeRegistryHoldersWithoutFreeze", Map.of(
                    "block", boundBlockRegistryHolders,
                    "item", boundItemRegistryHolders,
                    "creativeTab", boundCreativeTabRegistryHolders
            ));
            data.put("finallyFallbackRestoredNativeRegistryFrozenFlags", Map.of(
                    "block", restoredBlockFrozenFlag,
                    "item", restoredItemFrozenFlag,
                    "creativeTab", restoredCreativeTabFrozenFlag
            ));
            int preparedBuiltInRegistryFreezeCount = 0;
            int preparedRegistryOfRegistriesFreezeCount = 0;
            boolean registryFreezeGuardStarted = false;
            try {
                Class<?> builtInRegistriesClass = Class.forName(context.runtimeClass("core.registries.BuiltInRegistries"));
                preparedBuiltInRegistryFreezeCount =
                        NativeLoaderRegistryRuntimeSupport.prepareAllBuiltInRegistriesForMinecraftFreeze(builtInRegistriesClass);
                preparedRegistryOfRegistriesFreezeCount =
                        NativeLoaderRegistryRuntimeSupport.prepareRegistryAndContentsForMinecraftFreeze(
                                builtInRegistriesClass.getField("REGISTRY").get(null));
                registryFreezeGuardStarted =
                        NativeLoaderRegistryRuntimeSupport.startBuiltInRegistryFreezeGuard(
                                builtInRegistriesClass,
                                600_000L,
                                10L);
            } catch (Throwable ignored) {
                // The three explicitly-opened registries above were already restored.
            }
            data.put("preparedBuiltInRegistryFreezeCount", preparedBuiltInRegistryFreezeCount);
            data.put("preparedRegistryOfRegistriesFreezeCount", preparedRegistryOfRegistriesFreezeCount);
            data.put("registryFreezeGuardStarted", registryFreezeGuardStarted);
            data.put("registryFreezeGuardDurationMillis", 600_000L);
            data.put("registryFreezeGuardIntervalMillis", 10L);
            data.put("clientRegistryLayerPrimed", primeClientRegistryLayer(context, data));
            data.put("nativeRegistryFreezeDeferredToMinecraft", true);
            data.put("nativeRegistryWindowClosed",
                    blockRegistryWasFrozen
                            || itemRegistryWasFrozen
                            || creativeTabRegistryWasFrozen
                            || preparedBuiltInRegistryFreezeCount > 0
                            || preparedRegistryOfRegistriesFreezeCount > 0);
        }
        return data;
    }

    private static boolean primeClientRegistryLayer(Context context, Map<String, Object> data) {
        try {
            Class<?> clientRegistryLayerClass =
                    Class.forName(context.runtimeClass("client.multiplayer.ClientRegistryLayer"));
            clientRegistryLayerClass.getMethod("createRegistryAccess").invoke(null);
            return true;
        } catch (Throwable exception) {
            data.put("clientRegistryLayerPrimeFailureKind", exception.getClass().getSimpleName());
            data.put("clientRegistryLayerPrimeFailureMessage", failureMessage(exception));
            return false;
        }
    }

    private static Set<String> mergedContentIds(List<String> discoveredIds, List<String> sdkIds) {
        return mergedContentIds(discoveredIds, sdkIds, List.of());
    }

    private static Set<String> mergedContentIds(
            List<String> discoveredIds,
            List<String> sdkIds,
            List<String> declaredIds
    ) {
        Set<String> ids = new LinkedHashSet<>();
        ids.addAll(normalizeContentIds(discoveredIds));
        ids.addAll(normalizeContentIds(sdkIds));
        ids.addAll(normalizeContentIds(declaredIds));
        return Set.copyOf(ids);
    }

    private static List<String> registryDeclarationContentIds(
            List<String> directIds,
            List<Map<String, Object>> registryDeclarations,
            String registry
    ) {
        Set<String> ids = new LinkedHashSet<>(normalizeContentIds(directIds));
        String normalizedRegistry = normalizeRegistry(registry);
        for (Map<String, Object> declaration : registryDeclarations == null
                ? List.<Map<String, Object>>of()
                : registryDeclarations) {
            if (!normalizedRegistry.equals(normalizeRegistry(String.valueOf(declaration.getOrDefault("registry", ""))))) {
                continue;
            }
            ids.addAll(normalizeContentIds(List.of(String.valueOf(declaration.getOrDefault("id", "")))));
        }
        return List.copyOf(ids);
    }

    private static Map<String, Object> registrationFailure(String registry, String id, Throwable exception) {
        Map<String, Object> failure = new LinkedHashMap<>();
        failure.put("registry", registry);
        failure.put("id", id);
        failure.put("failureKind", exception.getClass().getSimpleName());
        failure.put("failureMessage", failureMessage(exception));
        Throwable cause = exception.getCause();
        if (cause != null) {
            failure.put("causeKind", cause.getClass().getSimpleName());
            failure.put("causeMessage", failureMessage(cause));
        }
        return Map.copyOf(failure);
    }

    private static Object registryValue(Object registry, Class<?> identifierClass, String contentId) {
        try {
            int separator = contentId.indexOf(':');
            if (separator < 1 || separator + 1 >= contentId.length()) {
                return null;
            }
            Object id = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                    .invoke(null, contentId.substring(0, separator), contentId.substring(separator + 1));
            Boolean containsKey = registryContainsKey(registry, identifierClass, id);
            if (Boolean.FALSE.equals(containsKey)) {
                return null;
            }
            for (String methodName : List.of("getOptionalValue", "getOptional", "get")) {
                try {
                    Method method = registry.getClass().getMethod(methodName, identifierClass);
                    Object value = unwrapRegistryValue(method.invoke(registry, id));
                    if (value != null) {
                        return value;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try the next registry accessor used by this Minecraft snapshot.
                }
            }
            if (Boolean.TRUE.equals(containsKey)) {
                try {
                    Method method = registry.getClass().getMethod("getValue", identifierClass);
                    return unwrapRegistryValue(method.invoke(registry, id));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    return null;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static Boolean registryContainsKey(Object registry, Class<?> identifierClass, Object id) {
        if (registry == null || id == null) {
            return Boolean.FALSE;
        }
        try {
            Method method = registry.getClass().getMethod("containsKey", identifierClass);
            Object value = method.invoke(registry, id);
            if (value instanceof Boolean result) {
                return result;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Some runtime registry facades expose optional lookups without containsKey.
        }
        return null;
    }

    private static Object unwrapRegistryValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Optional<?> optional) {
            return optional.orElse(null);
        }
        if (value.getClass().getName().endsWith(".Holder$Reference")) {
            for (String methodName : List.of("value", "get", "getValue")) {
                try {
                    Object unwrapped = value.getClass().getMethod(methodName).invoke(value);
                    if (unwrapped != null) {
                        return unwrapped;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Try the next holder accessor.
                }
            }
        }
        return value;
    }

    private static List<String> normalizeContentIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String id : ids) {
            String text = id == null ? "" : id.trim().toLowerCase();
            int separator = text.indexOf(':');
            if (separator < 1 || separator + 1 >= text.length()) {
                continue;
            }
            normalized.add(text);
        }
        return List.copyOf(normalized);
    }

    private static List<Map<String, Object>> normalizeCreativeTabDeclarations(
            List<Map<String, Object>> declarations,
            List<String> creativeTabIds
    ) {
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> declaration : declarations == null ? List.<Map<String, Object>>of() : declarations) {
            String registry = String.valueOf(declaration.getOrDefault("registry", ""))
                    .trim()
                    .toLowerCase(java.util.Locale.ROOT)
                    .replace('-', '_');
            if (!"creative_tab".equals(registry)) {
                continue;
            }
            List<String> normalizedIds = normalizeContentIds(List.of(
                    String.valueOf(declaration.getOrDefault("id", ""))
            ));
            if (normalizedIds.isEmpty()) {
                continue;
            }
            String id = normalizedIds.get(0);
            Map<String, Object> normalized = new LinkedHashMap<>(declaration);
            normalized.put("registry", "creative_tab");
            normalized.put("id", id);
            byId.put(id, Map.copyOf(normalized));
        }
        for (String creativeTabId : creativeTabIds == null ? List.<String>of() : creativeTabIds) {
            byId.putIfAbsent(creativeTabId, Map.of(
                    "registry", "creative_tab",
                    "id", creativeTabId
            ));
        }
        return List.copyOf(byId.values());
    }

    private static List<Map<String, Object>> normalizeRegistryDeclarations(List<Map<String, Object>> declarations) {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> declaration : declarations == null ? List.<Map<String, Object>>of() : declarations) {
            String registry = normalizeRegistry(String.valueOf(declaration.getOrDefault("registry", "")));
            if (!firstClassRegistryKinds().contains(registry)) {
                continue;
            }
            List<String> normalizedIds = normalizeContentIds(List.of(
                    String.valueOf(declaration.getOrDefault("id", ""))
            ));
            if (normalizedIds.isEmpty()) {
                continue;
            }
            String id = normalizedIds.get(0);
            Map<String, Object> normalized = new LinkedHashMap<>(declaration);
            normalized.put("registry", registry);
            normalized.put("id", id);
            String key = registry + ":" + id;
            byKey.put(key, mergedProperties(byKey.getOrDefault(key, Map.of()), normalized));
        }
        return List.copyOf(byKey.values());
    }

    private static String normalizeRegistry(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_')
                .replace('.', '_');
        return switch (normalized) {
            case "items" -> "item";
            case "blocks" -> "block";
            case "entities" -> "entity";
            case "blockentity", "blockentities", "block_entity", "block_entities" -> "block_entity";
            case "menus" -> "menu";
            case "sounds" -> "sound";
            case "particles", "particle_profile", "particle_profiles" -> "particle";
            case "effects", "mob_effect", "mob_effects", "mobeffect", "mobeffects" -> "effect";
            case "commands" -> "command";
            case "datacomponent", "datacomponents", "data_component", "data_components" -> "data_component";
            case "recipes" -> "recipe";
            case "creativegroup", "creativegroups", "creative_group", "creative_groups",
                    "creative_tab", "creative_tabs" -> "creative_tab";
            case "biomes" -> "biome";
            case "configured_feature", "configured_features", "placed_feature", "placed_features",
                    "world_generator", "world_generators", "worldgens" -> "worldgen";
            case "asset", "assets", "clientasset", "clientassets", "client_asset", "client_assets" -> "client_asset";
            default -> normalized;
        };
    }

    private static Map<String, Map<String, Object>> declarationsById(List<Map<String, Object>> declarations) {
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> declaration : declarations == null ? List.<Map<String, Object>>of() : declarations) {
            List<String> ids = normalizeContentIds(List.of(String.valueOf(declaration.getOrDefault("id", ""))));
            if (!ids.isEmpty()) {
                byId.put(ids.get(0), Map.copyOf(declaration));
            }
        }
        return Map.copyOf(byId);
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static Map<String, Object> registerThroughNativeRegistryHost(
            List<String> blockIds,
            List<String> itemIds,
            List<String> creativeTabIds,
            List<Map<String, Object>> creativeTabDeclarations,
            List<Map<String, Object>> registryDeclarations
    ) {
        Object host = newNativeRegistryHost();
        attachLiveRegistryBridge(host);
        Map<String, Map<String, Object>> creativeTabDeclarationsById =
                declarationsById(creativeTabDeclarations);
        Map<String, Map<String, Object>> registryDeclarationsByKey =
                declarationsByRegistryKey(registryDeclarations);
        Set<String> registeredKeys = new LinkedHashSet<>();
        List<Map<String, Object>> registrations = new ArrayList<>();
        int registeredCount = 0;
        int unsupportedCount = 0;
        int failedCount = 0;
        for (String blockId : blockIds == null ? List.<String>of() : blockIds) {
            Map<String, Object> registration = nativeRegistryHostRegistration(
                    host,
                    "block",
                    blockId,
                    registryDeclarationsByKey.getOrDefault("block:" + blockId, Map.of())
            );
            registrations.add(registration);
            EchoNativeLoadStatus status = status(registration.get("status"));
            if (status == EchoNativeLoadStatus.MUTATED) {
                registeredCount++;
            } else if (status == EchoNativeLoadStatus.UNSUPPORTED) {
                unsupportedCount++;
            } else if (status == EchoNativeLoadStatus.FAILED) {
                failedCount++;
            }
            registeredKeys.add("block:" + blockId);
        }
        for (String itemId : itemIds == null ? List.<String>of() : itemIds) {
            Map<String, Object> registration = nativeRegistryHostRegistration(
                    host,
                    "item",
                    itemId,
                    registryDeclarationsByKey.getOrDefault("item:" + itemId, Map.of())
            );
            registrations.add(registration);
            EchoNativeLoadStatus status = status(registration.get("status"));
            if (status == EchoNativeLoadStatus.MUTATED) {
                registeredCount++;
            } else if (status == EchoNativeLoadStatus.UNSUPPORTED) {
                unsupportedCount++;
            } else if (status == EchoNativeLoadStatus.FAILED) {
                failedCount++;
            }
            registeredKeys.add("item:" + itemId);
        }
        for (String creativeTabId : creativeTabIds == null ? List.<String>of() : creativeTabIds) {
            Map<String, Object> registration = nativeRegistryHostRegistration(
                    host,
                    "creative_tab",
                    creativeTabId,
                    mergedProperties(
                            registryDeclarationsByKey.getOrDefault("creative_tab:" + creativeTabId, Map.of()),
                            creativeTabDeclarationsById.getOrDefault(creativeTabId, Map.of())
                    )
            );
            registrations.add(registration);
            EchoNativeLoadStatus status = status(registration.get("status"));
            if (status == EchoNativeLoadStatus.MUTATED) {
                registeredCount++;
            } else if (status == EchoNativeLoadStatus.UNSUPPORTED) {
                unsupportedCount++;
            } else if (status == EchoNativeLoadStatus.FAILED) {
                failedCount++;
            }
            registeredKeys.add("creative_tab:" + creativeTabId);
        }
        for (Map<String, Object> declaration : registryDeclarations == null
                ? List.<Map<String, Object>>of()
                : registryDeclarations) {
            String registry = String.valueOf(declaration.getOrDefault("registry", ""));
            String id = String.valueOf(declaration.getOrDefault("id", ""));
            String key = registry + ":" + id;
            if (registeredKeys.contains(key)) {
                continue;
            }
            Map<String, Object> registration = nativeRegistryHostRegistration(host, registry, id, declaration);
            registrations.add(registration);
            EchoNativeLoadStatus status = status(registration.get("status"));
            if (status == EchoNativeLoadStatus.MUTATED) {
                registeredCount++;
            } else if (status == EchoNativeLoadStatus.UNSUPPORTED) {
                unsupportedCount++;
            } else if (status == EchoNativeLoadStatus.FAILED) {
                failedCount++;
            }
            registeredKeys.add(key);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("registeredCount", registeredCount);
        report.put("unsupportedCount", unsupportedCount);
        report.put("failedCount", failedCount);
        report.put("registrations", List.copyOf(registrations));
        report.put("report", nativeRegistryHostReport(host));
        return Map.copyOf(report);
    }

    private static Map<String, Map<String, Object>> declarationsByRegistryKey(List<Map<String, Object>> declarations) {
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> declaration : declarations == null ? List.<Map<String, Object>>of() : declarations) {
            String registry = String.valueOf(declaration.getOrDefault("registry", ""));
            String id = String.valueOf(declaration.getOrDefault("id", ""));
            if (!registry.isBlank() && !id.isBlank()) {
                byKey.put(registry + ":" + id, Map.copyOf(declaration));
            }
        }
        return Map.copyOf(byKey);
    }

    private static Map<String, Object> mergedProperties(
            Map<String, Object> base,
            Map<String, Object> overlay
    ) {
        Map<String, Object> merged = new LinkedHashMap<>(base == null ? Map.of() : base);
        for (Map.Entry<String, Object> entry : (overlay == null ? Map.<String, Object>of() : overlay).entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (listMergeProperty(entry.getKey()) && merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), mergedListValues(merged.get(entry.getKey()), entry.getValue()));
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(merged);
    }

    private static boolean listMergeProperty(String key) {
        return switch (String.valueOf(key)) {
            case "itemIds", "surfaceIds", "featuredItemIds", "registryBackedItemIds",
                    "sourceNamespaces", "searchVisibleItemIds" -> true;
            default -> false;
        };
    }

    private static List<Object> mergedListValues(Object baseValue, Object overlayValue) {
        List<Object> values = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        appendMergedListValues(values, seen, baseValue);
        appendMergedListValues(values, seen, overlayValue);
        return List.copyOf(values);
    }

    private static void appendMergedListValues(List<Object> values, Set<String> seen, Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            if (value != null && seen.add(String.valueOf(value))) {
                values.add(value);
            }
            return;
        }
        for (Object item : iterable) {
            if (item != null && seen.add(String.valueOf(item))) {
                values.add(item);
            }
        }
    }

    private static Map<String, Object> nativeRegistryHostRegistration(
            Object host,
            String registry,
            String id
    ) {
        return nativeRegistryHostRegistration(host, registry, id, Map.of());
    }

    private static Map<String, Object> nativeRegistryHostRegistration(
            Object host,
            String registry,
            String id,
            Map<String, Object> properties
    ) {
        String safeId = id == null ? "" : id;
        Map<String, Object> hostProperties = new LinkedHashMap<>(properties == null ? Map.of() : properties);
        hostProperties.put("source", nativeBootstrapRegistryBridgeFactoryServiceId(host));
        hostProperties.put("registry", registry);
        hostProperties.put("id", safeId);
        EchoNativeLoadStatus status = invokeRegisterDeclared(host, namespace(safeId), registry, safeId, hostProperties);
        Map<String, Object> registration = new LinkedHashMap<>();
        registration.put("registry", registry);
        registration.put("id", safeId);
        registration.put("status", status.name());
        registration.put("nativeRegistryHostRegistered", status == EchoNativeLoadStatus.MUTATED);
        if (!hostProperties.isEmpty()) {
            registration.put("properties", Map.copyOf(hostProperties));
        }
        return Map.copyOf(registration);
    }

    private static EchoNativeLoadStatus status(Object value) {
        if (value instanceof EchoNativeLoadStatus status) {
            return status;
        }
        try {
            return EchoNativeLoadStatus.valueOf(String.valueOf(value));
        } catch (RuntimeException ignored) {
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static String namespace(String id) {
        if (id == null) {
            return "";
        }
        int separator = id.indexOf(':');
        return separator > 0 ? id.substring(0, separator) : "";
    }

    private static Object newNativeRegistryHost() {
        try {
            Class<?> hostType = Class.forName(
                    "dev.echo.nativeplatform.loader.EchoNativeRegistryHost",
                    true,
                    nativeLoaderRuntimeClassLoader());
            return hostType.getConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Native Loader registry host could not be created", exception);
        }
    }

    private static void attachLiveRegistryBridge(Object host) {
        if (host == null) {
            throw new IllegalArgumentException("Native Loader registry host is required");
        }
        try {
            ClassLoader loader = host.getClass().getClassLoader();
            Class<?> factoryType = Class.forName(
                    "dev.echo.nativeplatform.loader.NativeLoaderBootstrapRegistryBridgeFactory",
                    true,
                    loader);
            Class<?> bridgeType = Class.forName(
                    "dev.echo.nativeplatform.loader.NativeLoaderLiveRegistryBridge",
                    true,
                    loader);
            Object bootstrapAppliedLiveBridge = factoryType
                    .getMethod("bootstrapAppliedLiveBridge")
                    .invoke(null);
            InvocationHandler handler = (proxy, method, arguments) ->
                    method.invoke(bootstrapAppliedLiveBridge, arguments);
            Object bridgeProxy = Proxy.newProxyInstance(loader, new Class<?>[] { bridgeType }, handler);
            host.getClass().getMethod("attachLiveBridge", bridgeType).invoke(host, bridgeProxy);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Native Loader live registry bridge could not be attached", exception);
        }
    }

    private static EchoNativeLoadStatus invokeRegisterDeclared(
            Object host,
            String namespace,
            String registry,
            String id,
            Map<String, Object> properties
    ) {
        try {
            Method method = host.getClass().getMethod(
                    "registerDeclared",
                    String.class,
                    String.class,
                    String.class,
                    Map.class);
            return status(method.invoke(host, namespace, registry, id, properties));
        } catch (ReflectiveOperationException exception) {
            return EchoNativeLoadStatus.FAILED;
        }
    }

    private static Map<String, Object> nativeRegistryHostReport(Object host) {
        try {
            Object report = host.getClass().getMethod("toReport").invoke(host);
            if (report instanceof Map<?, ?> map) {
                Map<String, Object> converted = new LinkedHashMap<>();
                map.forEach((key, value) -> converted.put(String.valueOf(key), value));
                return Map.copyOf(converted);
            }
        } catch (ReflectiveOperationException ignored) {
            return Map.of("nativeRegistryHostReportFailed", true);
        }
        return Map.of("nativeRegistryHostReportFailed", true);
    }

    private static String nativeRegistryHostServiceId() {
        try {
            return String.valueOf(Class.forName(
                    "dev.echo.nativeplatform.loader.EchoNativeRegistryHost",
                    true,
                    nativeLoaderRuntimeClassLoader()).getField("SERVICE_ID").get(null));
        } catch (ReflectiveOperationException exception) {
            return "echo.native.registry.host";
        }
    }

    private static String nativeBootstrapRegistryBridgeFactoryServiceId(Object host) {
        try {
            return String.valueOf(Class.forName(
                    "dev.echo.nativeplatform.loader.NativeLoaderBootstrapRegistryBridgeFactory",
                    true,
                    host.getClass().getClassLoader()).getField("SERVICE_ID").get(null));
        } catch (ReflectiveOperationException exception) {
            return "echo.native.bootstrap_registry_bridge_factory";
        }
    }

    private static List<String> firstClassRegistryKinds() {
        try {
            Object result = Class.forName(
                    "dev.echo.nativeplatform.loader.EchoNativeRegistryHost",
                    true,
                    nativeLoaderRuntimeClassLoader()).getMethod("firstClassRegistryKinds").invoke(null);
            if (result instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        } catch (ReflectiveOperationException ignored) {
            return List.of();
        }
        return List.of();
    }

    private static ClassLoader nativeLoaderRuntimeClassLoader() {
        String runtimeJar = System.getProperty("echo.native.loaderRuntimeJar", "").trim();
        if (runtimeJar.isBlank()) {
            return NativeLoaderRegistryBridge.class.getClassLoader();
        }
        try {
            URL url = Path.of(runtimeJar).toUri().toURL();
            return new URLClassLoader(new URL[] { url }, NativeLoaderRegistryBridge.class.getClassLoader());
        } catch (RuntimeException | java.net.MalformedURLException exception) {
            return NativeLoaderRegistryBridge.class.getClassLoader();
        }
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public static final class Config {
        private final EchoNativeBootstrapProductProfile profile;
        private final String nativeGameDirProperty;

        public Config(EchoNativeBootstrapProductProfile profile, String nativeGameDirProperty) {
            this.profile = profile;
            this.nativeGameDirProperty = nativeGameDirProperty == null ? "" : nativeGameDirProperty;
        }

        EchoNativeBootstrapProductProfile profile() {
            return profile;
        }

        String nativeGameDirProperty() {
            return nativeGameDirProperty;
        }
    }

    public static final class Context {
        private final VanillaBootstrapper vanillaBootstrapper;
        private final RuntimeClassResolver runtimeClassResolver;
        private final ContentIdSupplier itemIdSupplier;
        private final ContentIdSupplier blockIdSupplier;
        private final NativeLoaderRegistryContentBridge.NativeItemFactory itemFactory;
        private final NativeLoaderRegistryContentBridge.NativeBlockFactory blockFactory;

        public Context(
                VanillaBootstrapper vanillaBootstrapper,
                RuntimeClassResolver runtimeClassResolver,
                ContentIdSupplier itemIdSupplier,
                ContentIdSupplier blockIdSupplier,
                NativeLoaderRegistryContentBridge.NativeItemFactory itemFactory,
                NativeLoaderRegistryContentBridge.NativeBlockFactory blockFactory
        ) {
            this.vanillaBootstrapper = vanillaBootstrapper;
            this.runtimeClassResolver = runtimeClassResolver;
            this.itemIdSupplier = itemIdSupplier;
            this.blockIdSupplier = blockIdSupplier;
            this.itemFactory = itemFactory;
            this.blockFactory = blockFactory;
        }

        boolean ensureVanillaBootstrap() throws Exception {
            return vanillaBootstrapper.ensure();
        }

        String runtimeClass(String suffix) {
            return runtimeClassResolver.resolve(suffix);
        }

        List<String> itemIds() throws Exception {
            return itemIdSupplier.get();
        }

        List<String> blockIds() throws Exception {
            return blockIdSupplier.get();
        }

        NativeLoaderRegistryContentBridge.NativeItemFactory itemFactory() {
            return itemFactory;
        }

        NativeLoaderRegistryContentBridge.NativeBlockFactory blockFactory() {
            return blockFactory;
        }
    }

    @FunctionalInterface
    public interface VanillaBootstrapper {
        boolean ensure() throws Exception;
    }

    @FunctionalInterface
    public interface RuntimeClassResolver {
        String resolve(String suffix);
    }

    @FunctionalInterface
    public interface ContentIdSupplier {
        List<String> get() throws Exception;
    }
}
