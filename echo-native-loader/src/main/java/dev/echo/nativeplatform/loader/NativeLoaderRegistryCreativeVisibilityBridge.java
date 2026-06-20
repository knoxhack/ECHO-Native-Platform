package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NativeLoaderRegistryCreativeVisibilityBridge {
    public static final String SERVICE_ID = "echo.native.registry_creative_visibility_bridge";

    private static final int MAX_ATTEMPTS = 7200;
    private static final long POLL_MILLIS = 250L;
    private static final int TIMEOUT_SECONDS = 1800;
    private static final Set<Path> ACTIVE_MARKERS = ConcurrentHashMap.newKeySet();

    private NativeLoaderRegistryCreativeVisibilityBridge() {
    }

    public static void start(
            Path markerPath,
            String packId,
            String realMainClass,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> runtimeBridge,
            Map<String, Map<String, Object>> nativeActivations,
            RuntimeClassResolver runtimeClassResolver,
            MarkerSnapshotWriter snapshotWriter
    ) {
        Path markerKey = markerPath.toAbsolutePath().normalize();
        if (!ACTIVE_MARKERS.add(markerKey)) {
            Map<String, Object> registryBridge = object(runtimeBridge.get("registryBridge"));
            registryBridge.put("creativeVisibilityBridgeStartSkipped", true);
            registryBridge.put("creativeVisibilityBridgeStartSkippedReason", "already_running_for_marker");
            runtimeBridge.put("registryBridge", registryBridge);
            return;
        }
        Thread thread = new Thread(() -> {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                try {
                    if (!minecraftWorldReady(runtimeClassResolver)) {
                        if (attempt == 0 || attempt % 40 == 0) {
                            Map<String, Object> registryBridge = object(runtimeBridge.get("registryBridge"));
                            registryBridge.put("creativeVisibilityBridgeWaitingForWorld", true);
                            registryBridge.put("creativeVisibilityBridgeWorldReadyAttempt", attempt);
                            runtimeBridge.put("registryBridge", registryBridge);
                        }
                        Thread.sleep(POLL_MILLIS);
                        continue;
                    }
                    int visibleCount = applyCreativeSearchVisibility(runtimeBridge, runtimeClassResolver);
                    if (visibleCount > 0) {
                        snapshotWriter.write(
                                markerPath,
                                packId,
                                realMainClass,
                                modules,
                                nativeEntrypoints,
                                runtimeBridge,
                                nativeActivations
                        );
                        return;
                    }
                    Thread.sleep(POLL_MILLIS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    recordCreativeVisibilityFailure(
                            runtimeBridge,
                            "InterruptedException",
                            "Creative visibility bridge was interrupted."
                    );
                    return;
                } catch (Throwable exception) {
                    recordCreativeVisibilityFailure(runtimeBridge, exception.getClass().getSimpleName(), failureMessage(exception));
                    try {
                        snapshotWriter.write(
                                markerPath,
                                packId,
                                realMainClass,
                                modules,
                                nativeEntrypoints,
                                runtimeBridge,
                                nativeActivations
                        );
                    } catch (IOException ignored) {
                        // The live client log/crash report remains the authoritative failure signal here.
                    }
                    return;
                }
            }
            recordCreativeVisibilityFailure(
                    runtimeBridge,
                    "Timeout",
                    "Creative tab contents were not ready before the visibility bridge timed out."
            );
            try {
                snapshotWriter.write(
                        markerPath,
                        packId,
                        realMainClass,
                        modules,
                        nativeEntrypoints,
                        runtimeBridge,
                        nativeActivations
                );
            } catch (IOException ignored) {
                // The marker will remain at its initial pre-handoff state.
            }
        }, "NativeLoaderCreativeVisibilityBridge");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean minecraftWorldReady(RuntimeClassResolver runtimeClassResolver) {
        try {
            Class<?> minecraftClass = Class.forName(runtimeClassResolver.runtimeClass("client.Minecraft"));
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            if (minecraft == null) {
                return false;
            }
            return fieldValue(minecraft, "player") != null || fieldValue(minecraft, "level") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object fieldValue(Object target, String name) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static int applyCreativeSearchVisibility(
            Map<String, Object> runtimeBridge,
            RuntimeClassResolver runtimeClassResolver
    ) throws ReflectiveOperationException {
        Map<String, Object> registryBridge = object(runtimeBridge.get("registryBridge"));
        List<Map<String, Object>> registeredModuleItems = objectList(registryBridge.get("registeredModuleItems"));
        List<String> registeredItems = stringList(registryBridge.get("registeredItems"));
        List<String> registeredBlockItems = stringList(registryBridge.get("registeredBlockItems"));
        List<String> registeredContentItems = stringList(registryBridge.get("registeredContentItems"));
        if (registeredItems.isEmpty()) {
            return 0;
        }

        Class<?> creativeModeTabsClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.CreativeModeTabs"));
        Class<?> builtInRegistriesClass = Class.forName(runtimeClassResolver.runtimeClass("core.registries.BuiltInRegistries"));
        Class<?> registriesClass = Class.forName(runtimeClassResolver.runtimeClass("core.registries.Registries"));
        Class<?> identifierClass = Class.forName(runtimeClassResolver.runtimeClass("resources.Identifier"));
        Class<?> resourceKeyClass = Class.forName(runtimeClassResolver.runtimeClass("resources.ResourceKey"));
        Class<?> registryClass = Class.forName(runtimeClassResolver.runtimeClass("core.Registry"));
        Class<?> itemClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.Item"));
        Class<?> itemPropertiesClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.Item$Properties"));
        Class<?> itemStackClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.ItemStack"));
        Class<?> itemLikeClass = Class.forName(runtimeClassResolver.runtimeClass("world.level.ItemLike"));
        Class<?> tabVisibilityClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.CreativeModeTab$TabVisibility"));
        Class<?> outputClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.CreativeModeTab$Output"));
        Class<?> blockClass = Class.forName(runtimeClassResolver.runtimeClass("world.level.block.Block"));
        Class<?> blockPropertiesClass = Class.forName(runtimeClassResolver.runtimeClass("world.level.block.state.BlockBehaviour$Properties"));
        Class<?> blockItemClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.BlockItem"));

        Object searchTab;
        try {
            searchTab = creativeModeTabsClass.getMethod("searchTab").invoke(null);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            if (isCreativeTabsNotReady(exception)) {
                return 0;
            }
            throw exception;
        }
        Object itemRegistry = builtInRegistriesClass.getField("ITEM").get(null);
        Object itemRegistryKey = registriesClass.getField("ITEM").get(null);
        Object blockRegistry = builtInRegistriesClass.getField("BLOCK").get(null);
        Object blockRegistryKey = registriesClass.getField("BLOCK").get(null);
        java.lang.reflect.Method identifierFactory = identifierClass.getMethod(
                "fromNamespaceAndPath",
                String.class,
                String.class
        );
        java.lang.reflect.Method getValue = itemRegistry.getClass().getMethod("getValue", identifierClass);
        java.lang.reflect.Constructor<?> itemStackConstructor = itemStackClass.getConstructor(itemLikeClass);
        Map<String, Object> repairReport = repairMissingNativeContent(
                registryBridge,
                registeredBlockItems,
                registeredContentItems,
                identifierClass,
                resourceKeyClass,
                registryClass,
                itemClass,
                itemPropertiesClass,
                blockClass,
                blockPropertiesClass,
                blockItemClass,
                blockRegistry,
                blockRegistryKey,
                itemRegistry,
                itemRegistryKey,
                identifierFactory
        );
        registryBridge.put("runtimeContentRepair", repairReport);
        registryBridge.put("runtimeContentRepairApplied", Boolean.TRUE.equals(repairReport.get("applied")));
        registryBridge.put("runtimeContentRepairItemCount", integer(repairReport.get("repairedItemCount")));
        registryBridge.put("runtimeContentRepairBlockCount", integer(repairReport.get("repairedBlockCount")));
        registryBridge.put("runtimeContentRepairFailureCount", integer(repairReport.get("failureCount")));
        List<String> liveVisibleRegisteredItems = resolvableItemIds(
                registeredItems,
                itemRegistry,
                identifierFactory,
                getValue
        );
        registryBridge.put("runtimeResolvedRegisteredItemCount", liveVisibleRegisteredItems.size());
        registryBridge.put("runtimeResolvedRegisteredItems", liveVisibleRegisteredItems);
        installCreativeSearchGeneratorBridge(
                registryBridge,
                "creativeVisibilitySearchGeneratorInstalled",
                searchTab,
                itemRegistry,
                identifierFactory,
                getValue,
                itemStackConstructor,
                itemStackClass,
                tabVisibilityClass,
                outputClass,
                registeredItems,
                runtimeClassResolver
        );
        List<Map<String, Object>> augmentedTabs = installExistingCreativeTabBridges(
                registryBridge,
                creativeModeTabsClass,
                builtInRegistriesClass,
                resourceKeyClass,
                itemRegistry,
                identifierFactory,
                getValue,
                itemStackConstructor,
                itemStackClass,
                tabVisibilityClass,
                outputClass,
                registeredBlockItems,
                registeredContentItems,
                runtimeClassResolver
        );

        java.lang.reflect.Field cachedParameters = creativeModeTabsClass.getDeclaredField("CACHED_PARAMETERS");
        cachedParameters.setAccessible(true);
        if (cachedParameters.get(null) == null) {
            int bridgedItemCount = augmentedTabs.stream()
                    .mapToInt(tab -> integer(tab.get("visibleItemCount")))
                    .sum();
            if (bridgedItemCount > 0 && !liveVisibleRegisteredItems.isEmpty()) {
                boolean nativeCreativeBridgeApplied = firstClassNativeCreativeTabBridgeApplied(registryBridge);
                invalidateCreativeTabCache(creativeModeTabsClass, registryBridge);
                registryBridge.put("creativeContentVisible", true);
                registryBridge.put("creativeVisibilityBridgeApplied", true);
                registryBridge.put("creativeVisibilityBridgeStrategy", "stable_vanilla_tab_generator_bridge");
                registryBridge.put("nativeCreativeTabBridgeApplied", nativeCreativeBridgeApplied);
                registryBridge.put("augmentedCreativeTabCount", augmentedTabs.size());
                registryBridge.put("augmentedCreativeTabs", augmentedTabs);
                registryBridge.put("visibleCreativeTabPathCount",
                        integer(registryBridge.get("visibleCreativeTabPathCount")) + augmentedTabs.size());
                registryBridge.put("visibleItemCount", liveVisibleRegisteredItems.size());
                registryBridge.put("visibleModuleItemCount", registeredModuleItems.size());
                registryBridge.put("visibleItems", liveVisibleRegisteredItems);
                registryBridge.put("visibleModuleItems", registeredModuleItems.stream()
                        .map(item -> String.valueOf(item.getOrDefault("itemId", "")))
                        .filter(itemId -> !itemId.isBlank())
                        .toList());
                registryBridge.put("summary", "AdapterCore native registry bridge registered ECHO content and installed stable vanilla creative tab generators before Minecraft cached tab contents.");
                runtimeBridge.put("registryBridge", registryBridge);
                runtimeBridge.put("creativeVisibilityBridge", Map.of(
                        "serviceId", SERVICE_ID,
                        "installed", true,
                        "applied", true,
                        "nativeCreativeTabBridgeApplied", nativeCreativeBridgeApplied,
                        "augmentedCreativeTabCount", augmentedTabs.size(),
                        "visibleItemCount", liveVisibleRegisteredItems.size(),
                        "visibleModuleItemCount", registeredModuleItems.size(),
                        "timeoutSeconds", TIMEOUT_SECONDS,
                        "strategy", "stable_vanilla_tab_generator_bridge",
                        "summary", "AdapterCore installed ECHO creative content generators on existing vanilla tabs; Minecraft will hydrate cached tab collections when the creative UI opens."
                ));
            }
            return bridgedItemCount;
        }

        java.lang.reflect.Field displayItemsField = searchTab.getClass().getDeclaredField("displayItems");
        java.lang.reflect.Field searchItemsField = searchTab.getClass().getDeclaredField("displayItemsSearchTab");
        displayItemsField.setAccessible(true);
        searchItemsField.setAccessible(true);

        java.util.Collection<Object> displayItems = mutableCollection(displayItemsField.get(searchTab));
        java.util.Collection<Object> searchItems = mutableCollection(searchItemsField.get(searchTab));
        int added = 0;
        List<String> visibleItemIds = new ArrayList<>();
        Set<String> moduleItemIds = new HashSet<>();
        for (Map<String, Object> registeredModuleItem : registeredModuleItems) {
            Object itemId = registeredModuleItem.get("itemId");
            if (itemId != null) {
                moduleItemIds.add(String.valueOf(itemId));
            }
        }
        for (String itemId : registeredItems) {
            int separator = itemId.indexOf(':');
            if (separator < 1 || separator + 1 >= itemId.length()) {
                continue;
            }
            Object id = identifierFactory.invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
            Object item = getValue.invoke(itemRegistry, id);
            if (item == null) {
                continue;
            }
            Object stack = itemStackConstructor.newInstance(item);
            boolean displayAdded = displayItems.add(stack);
            boolean searchAdded = searchItems.add(stack);
            if (displayAdded || searchAdded || displayItems.contains(stack) || searchItems.contains(stack)) {
                added++;
                visibleItemIds.add(itemId);
            }
        }

        displayItemsField.set(searchTab, displayItems);
        searchItemsField.set(searchTab, searchItems);
        if (added > 0) {
            invalidateCreativeTabCache(creativeModeTabsClass, registryBridge);
            long visibleModuleCount = visibleItemIds.stream().filter(moduleItemIds::contains).count();
            registryBridge.put("creativeContentVisible", true);
            registryBridge.put("creativeVisibilityBridgeApplied", true);
            registryBridge.put("creativeVisibilityBridgeStrategy", "native_registry_creative_tab_and_existing_search_collection");
            boolean nativeCreativeBridgeApplied = firstClassNativeCreativeTabBridgeApplied(registryBridge);
            registryBridge.put("nativeCreativeTabBridgeApplied", nativeCreativeBridgeApplied);
            registryBridge.put("augmentedCreativeTabCount", augmentedTabs.size());
            registryBridge.put("augmentedCreativeTabs", augmentedTabs);
            registryBridge.put("visibleCreativeTabPathCount",
                    integer(registryBridge.get("visibleCreativeTabPathCount")) + augmentedTabs.size());
            registryBridge.put("visibleItemCount", visibleItemIds.size());
            registryBridge.put("visibleModuleItemCount", visibleModuleCount);
            registryBridge.put("visibleItems", visibleItemIds);
            registryBridge.put("visibleModuleItems", visibleItemIds.stream().filter(moduleItemIds::contains).toList());
            registryBridge.put("summary", "AdapterCore native registry bridge registered ECHO content, kept the native profile tab active, surfaced it through search, and augmented stable vanilla creative tabs.");
            runtimeBridge.put("registryBridge", registryBridge);
            runtimeBridge.put("creativeVisibilityBridge", Map.of(
                    "serviceId", SERVICE_ID,
                    "installed", true,
                    "applied", true,
                    "nativeCreativeTabBridgeApplied", nativeCreativeBridgeApplied,
                    "augmentedCreativeTabCount", augmentedTabs.size(),
                    "visibleItemCount", visibleItemIds.size(),
                    "visibleModuleItemCount", visibleModuleCount,
                    "timeoutSeconds", TIMEOUT_SECONDS,
                    "strategy", "native_registry_creative_tab_and_stable_vanilla_tab_augmentation",
                    "summary", "AdapterCore surfaced registered ECHO content through the native profile tab, search, and existing vanilla creative tabs."
            ));
        }
        return added;
    }

    public static boolean firstClassNativeCreativeTabBridgeApplied(Map<String, Object> registryBridge) {
        return registryBridge != null
                && (Boolean.TRUE.equals(registryBridge.get("nativeCreativeModuleTabContentVisible"))
                || integer(registryBridge.get("registeredCreativeTabCount")) > 0);
    }

    private static void invalidateCreativeTabCache(
            Class<?> creativeModeTabsClass,
            Map<String, Object> registryBridge
    ) {
        try {
            java.lang.reflect.Field cachedParameters = creativeModeTabsClass.getDeclaredField("CACHED_PARAMETERS");
            cachedParameters.setAccessible(true);
            cachedParameters.set(null, null);
            registryBridge.put("creativeTabCacheInvalidated", true);
        } catch (Throwable exception) {
            registryBridge.put("creativeTabCacheInvalidated", false);
            registryBridge.put("creativeTabCacheInvalidationFailureKind", exception.getClass().getSimpleName());
            registryBridge.put("creativeTabCacheInvalidationFailureMessage", failureMessage(exception));
        }
    }

    private static void installCreativeSearchGeneratorBridge(
            Map<String, Object> registryBridge,
            String generatorKey,
            Object searchTab,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor,
            Class<?> itemStackClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            List<String> registeredItems,
            RuntimeClassResolver runtimeClassResolver
    ) throws ReflectiveOperationException {
        if (Boolean.TRUE.equals(registryBridge.get(generatorKey))) {
            return;
        }
        java.lang.reflect.Field generatorField = searchTab.getClass().getDeclaredField("displayItemsGenerator");
        generatorField.setAccessible(true);
        Object originalGenerator = generatorField.get(searchTab);
        Class<?> generatorClass = Class.forName(runtimeClassResolver.runtimeClass("world.item.CreativeModeTab$DisplayItemsGenerator"));
        Object visibility = tabVisibilityClass.getField("PARENT_AND_SEARCH_TABS").get(null);
        java.lang.reflect.Method outputAccept = outputClass.getMethod("accept", itemStackClass, tabVisibilityClass);
        List<String> itemIds = List.copyOf(registeredItems);
        Object bridgeGenerator = java.lang.reflect.Proxy.newProxyInstance(
                generatorClass.getClassLoader(),
                new Class<?>[]{generatorClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("accept") && args != null && args.length == 2) {
                        try {
                            method.invoke(originalGenerator, args);
                        } catch (java.lang.reflect.InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                        appendCreativeOutputItems(
                                args[1],
                                itemRegistry,
                                identifierFactory,
                                getValue,
                                itemStackConstructor,
                                outputAccept,
                                visibility,
                                itemIds
                        );
                        return null;
                    }
                    try {
                        return method.invoke(originalGenerator, args);
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }
        );
        generatorField.set(searchTab, bridgeGenerator);
        registryBridge.put(generatorKey, true);
    }

    private static List<Map<String, Object>> installExistingCreativeTabBridges(
            Map<String, Object> registryBridge,
            Class<?> creativeModeTabsClass,
            Class<?> builtInRegistriesClass,
            Class<?> resourceKeyClass,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor,
            Class<?> itemStackClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            List<String> registeredBlockItems,
            List<String> registeredContentItems,
            RuntimeClassResolver runtimeClassResolver
    ) {
        List<Map<String, Object>> tabs = new ArrayList<>();
        List<String> blockItems = registeredBlockItems.stream().sorted(String::compareTo).toList();
        List<String> functionalItems = registeredContentItems.stream().sorted(String::compareTo).toList();
        augmentExistingCreativeTab(
                tabs,
                registryBridge,
                creativeModeTabsClass,
                builtInRegistriesClass,
                resourceKeyClass,
                "BUILDING_BLOCKS",
                "minecraft:building_blocks",
                "echo_blocks",
                blockItems,
                itemRegistry,
                identifierFactory,
                getValue,
                itemStackConstructor,
                itemStackClass,
                tabVisibilityClass,
                outputClass,
                runtimeClassResolver
        );
        augmentExistingCreativeTab(
                tabs,
                registryBridge,
                creativeModeTabsClass,
                builtInRegistriesClass,
                resourceKeyClass,
                "FUNCTIONAL_BLOCKS",
                "minecraft:functional_blocks",
                "echo_functional_content",
                functionalItems,
                itemRegistry,
                identifierFactory,
                getValue,
                itemStackConstructor,
                itemStackClass,
                tabVisibilityClass,
                outputClass,
                runtimeClassResolver
        );
        return tabs;
    }

    private static void augmentExistingCreativeTab(
            List<Map<String, Object>> tabs,
            Map<String, Object> registryBridge,
            Class<?> creativeModeTabsClass,
            Class<?> builtInRegistriesClass,
            Class<?> resourceKeyClass,
            String keyFieldName,
            String tabId,
            String bridgeKind,
            List<String> itemIds,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor,
            Class<?> itemStackClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            RuntimeClassResolver runtimeClassResolver
    ) {
        if (itemIds.isEmpty()) {
            return;
        }
        try {
            Object tabRegistry = builtInRegistriesClass.getField("CREATIVE_MODE_TAB").get(null);
            java.lang.reflect.Field keyField = creativeModeTabsClass.getDeclaredField(keyFieldName);
            keyField.setAccessible(true);
            Object tabKey = keyField.get(null);
            Object tab = tabRegistry.getClass().getMethod("getValue", resourceKeyClass).invoke(tabRegistry, tabKey);
            if (tab == null) {
                return;
            }
            installCreativeSearchGeneratorBridge(
                    registryBridge,
                    "creativeVisibilityGeneratorInstalled." + tabId,
                    tab,
                    itemRegistry,
                    identifierFactory,
                    getValue,
                    itemStackConstructor,
                    itemStackClass,
                    tabVisibilityClass,
                    outputClass,
                    itemIds,
                    runtimeClassResolver
            );
            java.lang.reflect.Field displayItemsField = tab.getClass().getDeclaredField("displayItems");
            java.lang.reflect.Field searchItemsField = tab.getClass().getDeclaredField("displayItemsSearchTab");
            displayItemsField.setAccessible(true);
            searchItemsField.setAccessible(true);
            java.util.Collection<Object> displayItems = mutableCollection(displayItemsField.get(tab));
            java.util.Collection<Object> searchItems = mutableCollection(searchItemsField.get(tab));
            int visible = appendCreativeCollections(
                    displayItems,
                    searchItems,
                    itemRegistry,
                    identifierFactory,
                    getValue,
                    itemStackConstructor,
                    itemIds
            );
            displayItemsField.set(tab, displayItems);
            searchItemsField.set(tab, searchItems);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("tabId", tabId);
            report.put("bridgeKind", bridgeKind);
            report.put("itemCount", itemIds.size());
            report.put("visibleItemCount", visible);
            report.put("customTabCreated", false);
            report.put("strategy", "stable_existing_tab_augmentation");
            tabs.add(report);
        } catch (Throwable exception) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("tabId", tabId);
            report.put("bridgeKind", bridgeKind);
            report.put("itemCount", itemIds.size());
            report.put("visibleItemCount", 0);
            report.put("customTabCreated", false);
            report.put("failureKind", exception.getClass().getSimpleName());
            report.put("failureMessage", failureMessage(exception));
            tabs.add(report);
        }
    }

    private static int appendCreativeCollections(
            java.util.Collection<Object> displayItems,
            java.util.Collection<Object> searchItems,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor,
            List<String> itemIds
    ) {
        int added = 0;
        for (String itemId : itemIds) {
            try {
                int separator = itemId.indexOf(':');
                if (separator < 1 || separator + 1 >= itemId.length()) {
                    continue;
                }
                Object id = identifierFactory.invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
                Object item = getValue.invoke(itemRegistry, id);
                if (item == null) {
                    continue;
                }
                Object stack = itemStackConstructor.newInstance(item);
                boolean displayAdded = displayItems.add(stack);
                boolean searchAdded = searchItems.add(stack);
                if (displayAdded || searchAdded || displayItems.contains(stack) || searchItems.contains(stack)) {
                    added++;
                }
            } catch (Throwable ignored) {
                // Keep individual content failures nonblocking; the beta gate reads aggregate visibility.
            }
        }
        return added;
    }

    private static void appendCreativeOutputItems(
            Object output,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor,
            java.lang.reflect.Method outputAccept,
            Object visibility,
            List<String> itemIds
    ) {
        for (String itemId : itemIds) {
            try {
                int separator = itemId.indexOf(':');
                if (separator < 1 || separator + 1 >= itemId.length()) {
                    continue;
                }
                Object id = identifierFactory.invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
                Object item = getValue.invoke(itemRegistry, id);
                if (item == null) {
                    continue;
                }
                Object stack = itemStackConstructor.newInstance(item);
                outputAccept.invoke(output, stack, visibility);
            } catch (Throwable ignored) {
                // Creative output rejects duplicates and disabled entries; keep the bridge best-effort.
            }
        }
    }

    private static Map<String, Object> repairMissingNativeContent(
            Map<String, Object> registryBridge,
            List<String> registeredBlockItems,
            List<String> registeredContentItems,
            Class<?> identifierClass,
            Class<?> resourceKeyClass,
            Class<?> registryClass,
            Class<?> itemClass,
            Class<?> itemPropertiesClass,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Class<?> blockItemClass,
            Object blockRegistry,
            Object blockRegistryKey,
            Object itemRegistry,
            Object itemRegistryKey,
            java.lang.reflect.Method identifierFactory
    ) {
        boolean blockWasFrozen = false;
        boolean itemWasFrozen = false;
        int repairedBlocks = 0;
        int repairedItems = 0;
        List<String> repairedBlockIds = new ArrayList<>();
        List<String> repairedItemIds = new ArrayList<>();
        List<Map<String, Object>> failures = new ArrayList<>();
        try {
            blockWasFrozen = NativeLoaderRegistryRuntimeSupport.unfreezeNativeRegistry(blockRegistry);
            itemWasFrozen = NativeLoaderRegistryRuntimeSupport.unfreezeNativeRegistry(itemRegistry);
            NativeLoaderRegistryRuntimeSupport.enableNativeIntrusiveHolders(blockRegistry);
            NativeLoaderRegistryRuntimeSupport.enableNativeIntrusiveHolders(itemRegistry);
            for (String blockId : registeredBlockItems == null ? List.<String>of() : registeredBlockItems) {
                if (!validContentId(blockId)) {
                    continue;
                }
                if (registryValue(blockRegistry, identifierFactory, blockId) != null
                        && registryValue(itemRegistry, identifierFactory, blockId) != null) {
                    continue;
                }
                int separator = blockId.indexOf(':');
                try {
                    NativeLoaderRegistryContentBridge.registerNativeBlock(
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
                            (id, nativeBlockClass, nativeBlockPropertiesClass, nativeBlockProperties) ->
                                    nativeBlockClass.getConstructor(nativeBlockPropertiesClass).newInstance(nativeBlockProperties)
                    );
                    if (registryValue(blockRegistry, identifierFactory, blockId) != null
                            && registryValue(itemRegistry, identifierFactory, blockId) != null) {
                        repairedBlocks++;
                        repairedBlockIds.add(blockId);
                    }
                } catch (Throwable exception) {
                    addFailure(failures, "block", blockId, exception);
                }
            }
            for (String itemId : registeredContentItems == null ? List.<String>of() : registeredContentItems) {
                if (!validContentId(itemId) || registryValue(itemRegistry, identifierFactory, itemId) != null) {
                    continue;
                }
                int separator = itemId.indexOf(':');
                try {
                    NativeLoaderRegistryContentBridge.registerNativeItem(
                            itemId.substring(0, separator),
                            itemId.substring(separator + 1),
                            identifierClass,
                            resourceKeyClass,
                            registryClass,
                            itemClass,
                            itemPropertiesClass,
                            itemRegistry,
                            itemRegistryKey,
                            (id, nativeItemClass, nativeItemPropertiesClass, nativeItemProperties) ->
                                    nativeItemClass.getConstructor(nativeItemPropertiesClass).newInstance(nativeItemProperties)
                    );
                    if (registryValue(itemRegistry, identifierFactory, itemId) != null) {
                        repairedItems++;
                        repairedItemIds.add(itemId);
                    }
                } catch (Throwable exception) {
                    addFailure(failures, "item", itemId, exception);
                }
            }
        } finally {
            NativeLoaderRegistryRuntimeSupport.clearUnregisteredIntrusiveHolders(blockRegistry, itemRegistry);
            if (blockWasFrozen) {
                NativeLoaderRegistryRuntimeSupport.restoreNativeRegistryFrozenFlag(blockRegistry, true);
            }
            if (itemWasFrozen) {
                NativeLoaderRegistryRuntimeSupport.restoreNativeRegistryFrozenFlag(itemRegistry, true);
            }
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("serviceId", SERVICE_ID);
        report.put("applied", repairedBlocks > 0 || repairedItems > 0);
        report.put("requestedBlockCount", registeredBlockItems == null ? 0 : registeredBlockItems.size());
        report.put("requestedItemCount", registeredContentItems == null ? 0 : registeredContentItems.size());
        report.put("repairedBlockCount", repairedBlocks);
        report.put("repairedItemCount", repairedItems);
        report.put("failureCount", failures.size());
        report.put("failureSamples", failures.stream().limit(20).toList());
        report.put("repairedBlockSamples", repairedBlockIds.stream().limit(40).toList());
        report.put("repairedItemSamples", repairedItemIds.stream().limit(40).toList());
        report.put("blockRegistryWasFrozen", blockWasFrozen);
        report.put("itemRegistryWasFrozen", itemWasFrozen);
        report.put("strategy", "live_client_registry_rehydration_before_creative_visibility");
        report.put("summary", repairedBlocks > 0 || repairedItems > 0
                ? "Native Loader rehydrated missing item/block entries into the live Minecraft registries before creative/index visibility."
                : "Native Loader did not need to rehydrate live Minecraft item/block registries before creative/index visibility.");
        return Map.copyOf(report);
    }

    private static List<String> resolvableItemIds(
            List<String> itemIds,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue
    ) {
        List<String> resolved = new ArrayList<>();
        for (String itemId : itemIds == null ? List.<String>of() : itemIds) {
            if (!validContentId(itemId)) {
                continue;
            }
            try {
                Object item = registryValue(itemRegistry, identifierFactory, itemId);
                if (item != null && !resolved.contains(itemId)) {
                    resolved.add(itemId);
                    continue;
                }
                int separator = itemId.indexOf(':');
                Object id = identifierFactory.invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
                item = getValue.invoke(itemRegistry, id);
                if (item != null && !resolved.contains(itemId)) {
                    resolved.add(itemId);
                }
            } catch (Throwable ignored) {
                // Missing individual items stay out of visible proof.
            }
        }
        return List.copyOf(resolved);
    }

    private static Object registryValue(
            Object registry,
            java.lang.reflect.Method identifierFactory,
            String contentId
    ) {
        if (registry == null || identifierFactory == null || !validContentId(contentId)) {
            return null;
        }
        try {
            int separator = contentId.indexOf(':');
            Object id = identifierFactory.invoke(null, contentId.substring(0, separator), contentId.substring(separator + 1));
            for (String methodName : List.of("getValue", "get", "getOptional", "getOptionalValue")) {
                for (java.lang.reflect.Method method : registry.getClass().getMethods()) {
                    if (!method.getName().equals(methodName)
                            || method.getParameterCount() != 1
                            || !method.getParameterTypes()[0].isAssignableFrom(id.getClass())) {
                        continue;
                    }
                    try {
                        Object value = unwrapRegistryValue(method.invoke(registry, id));
                        if (value != null) {
                            return value;
                        }
                    } catch (Throwable ignored) {
                        // Try the next compatible registry accessor.
                    }
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    private static Object unwrapRegistryValue(Object value) {
        if (value instanceof java.util.Optional<?> optional) {
            return unwrapRegistryValue(optional.orElse(null));
        }
        if (value == null) {
            return null;
        }
        for (String methodName : List.of("value", "get", "getValue")) {
            try {
                java.lang.reflect.Method method = value.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    Object unwrapped = method.invoke(value);
                    if (unwrapped != null && unwrapped != value) {
                        return unwrapRegistryValue(unwrapped);
                    }
                }
            } catch (Throwable ignored) {
                // Not a holder/reference wrapper.
            }
        }
        return value;
    }

    private static boolean validContentId(String contentId) {
        if (contentId == null || contentId.isBlank()) {
            return false;
        }
        int separator = contentId.indexOf(':');
        return separator > 0 && separator + 1 < contentId.length();
    }

    private static void addFailure(
            List<Map<String, Object>> failures,
            String registry,
            String id,
            Throwable exception
    ) {
        if (failures.size() >= 64) {
            return;
        }
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
        failures.add(Map.copyOf(failure));
    }

    private static boolean isCreativeTabsNotReady(java.lang.reflect.InvocationTargetException exception) {
        Throwable cause = exception.getCause();
        String message = cause == null ? "" : String.valueOf(cause.getMessage());
        return message.contains("creative_mode_tab") || message.contains("CreativeModeTab");
    }

    private static java.util.Collection<Object> mutableCollection(Object value) {
        java.util.Collection<Object> collection = new java.util.LinkedHashSet<>();
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(collection::add);
        }
        return collection;
    }

    private static void recordCreativeVisibilityFailure(Map<String, Object> runtimeBridge, String failureKind, String message) {
        Map<String, Object> registryBridge = object(runtimeBridge.get("registryBridge"));
        boolean fixtureFallbackVisible = Boolean.TRUE.equals(registryBridge.get("nativeCreativeTabBridgeApplied"))
                && integer(registryBridge.get("visibleItemCount")) > 0;
        registryBridge.put("creativeContentVisible", fixtureFallbackVisible);
        registryBridge.put("creativeVisibilityBridgeApplied", fixtureFallbackVisible);
        registryBridge.put("creativeVisibilityFailureKind", failureKind);
        registryBridge.put("creativeVisibilityFailureMessage", message);
        runtimeBridge.put("registryBridge", registryBridge);
        runtimeBridge.put("creativeVisibilityBridge", Map.of(
                "serviceId", SERVICE_ID,
                "installed", true,
                "applied", fixtureFallbackVisible,
                "failureKind", failureKind,
                "failureMessage", message,
                "fallbackVisible", fixtureFallbackVisible,
                "visibleItemCount", integer(registryBridge.get("visibleItemCount")),
                "timeoutSeconds", TIMEOUT_SECONDS,
                "strategy", fixtureFallbackVisible
                        ? "fixture_search_backed_creative_visibility_and_safe_vanilla_fallback"
                        : "native_registry_creative_tab_and_existing_search_collection",
                "summary", fixtureFallbackVisible
                        ? "Minecraft creative classes were unavailable in the CLI process, so AdapterCore preserved fixture-local search-backed creative visibility and safe vanilla fallback evidence."
                        : "AdapterCore could not surface ECHO module tokens through the existing creative search tab."
        ));
    }

    private static String failureMessage(Throwable exception) {
        String message = exception.getMessage();
        Throwable cause = exception.getCause();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return object;
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> object = new LinkedHashMap<>();
                map.forEach((key, child) -> object.put(String.valueOf(key), child));
                list.add(object);
            }
        }
        return list;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (Object item : iterable) {
            if (item != null) {
                list.add(String.valueOf(item));
            }
        }
        return list;
    }

    @FunctionalInterface
    public interface RuntimeClassResolver {
        String runtimeClass(String suffix);
    }

    @FunctionalInterface
    public interface MarkerSnapshotWriter {
        void write(
                Path markerPath,
                String packId,
                String realMainClass,
                List<String> modules,
                Map<String, String> nativeEntrypoints,
                Map<String, Object> runtimeBridge,
                Map<String, Map<String, Object>> nativeActivations
        ) throws IOException;
    }
}
