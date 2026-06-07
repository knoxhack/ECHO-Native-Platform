package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class NativeLoaderRegistryCreativeBridge {
    public static final String SERVICE_ID = "echo.native.registry_creative_bridge";

    private NativeLoaderRegistryCreativeBridge() {
    }

    public static List<Map<String, Object>> registerNativeCreativeTabs(
            EchoNativeBootstrapProductProfile profile,
            List<String> registeredBlockItems,
            List<String> registeredContentItems,
            List<String> requestedCreativeTabIds,
            Class<?> identifierClass,
            Class<?> registryClass,
            Class<?> creativeModeTabClass,
            Class<?> creativeModeTabsClass,
            Class<?> componentClass,
            Class<?> itemStackClass,
            Class<?> itemLikeClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            Object creativeTabRegistry,
            Object itemRegistry,
            List<Map<String, Object>> requestedCreativeTabDeclarations
    ) {
        List<Map<String, Object>> bridges = new ArrayList<>();
        for (Map<String, Object> plan : plannedNativeCreativeTabs(
                profile,
                registeredBlockItems,
                registeredContentItems,
                requestedCreativeTabIds,
                requestedCreativeTabDeclarations
        )) {
            bridges.add(registerCreativeTab(
                    profile,
                    string(plan.get("namespace")),
                    string(plan.get("tabPath")),
                    string(plan.get("requestedCreativeTabId")),
                    stringList(plan.get("items")),
                    stringList(plan.get("registeredContentItems")),
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
                    object(plan.get("declaration")),
                    plan
            ));
        }
        return List.copyOf(bridges);
    }

    public static List<Map<String, Object>> plannedNativeCreativeTabs(
            EchoNativeBootstrapProductProfile profile,
            List<String> registeredBlockItems,
            List<String> registeredContentItems,
            List<String> requestedCreativeTabIds,
            List<Map<String, Object>> requestedCreativeTabDeclarations
    ) {
        List<String> allNativeItems = mergeCreativeItems(registeredContentItems, registeredBlockItems);
        if (allNativeItems.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> plans = new ArrayList<>();
        Map<String, List<Map<String, Object>>> declarationsByNamespace =
                creativeTabDeclarationsByNamespace(requestedCreativeTabDeclarations);
        int orderIndex = 0;
        for (Map.Entry<String, List<String>> entry : itemsByNamespace(allNativeItems).entrySet()) {
            String namespace = safeNamespace(entry.getKey());
            List<String> namespaceItems = entry.getValue();
            List<String> namespaceContentItems = registeredContentItems == null
                    ? List.of()
                    : registeredContentItems.stream()
                    .filter(item -> item.startsWith(namespace + ":"))
                    .toList();
            List<Map<String, Object>> namespaceDeclarations =
                    declarationsByNamespace.getOrDefault(namespace, List.of());
            if (!namespaceDeclarations.isEmpty()) {
                for (Map<String, Object> declaration : namespaceDeclarations) {
                    String requestedCreativeTabId = normalizedContentId(
                            String.valueOf(declaration.getOrDefault("id", ""))
                    );
                    List<String> declarationItems = creativeItemsForDeclaration(declaration, namespaceItems);
                    plans.add(creativeTabPlan(
                            profile,
                            namespace,
                            requestedCreativeTabPath(namespace, requestedCreativeTabId, "native_modules"),
                            requestedCreativeTabId,
                            declarationItems,
                            namespaceContentItems,
                            declaration,
                            orderIndex++
                    ));
                }
                continue;
            }
            String requestedCreativeTabId = requestedCreativeTabId(namespace, requestedCreativeTabIds);
            plans.add(creativeTabPlan(
                    profile,
                    namespace,
                    requestedCreativeTabPath(namespace, requestedCreativeTabId,
                            namespace.equals(safeNamespace(profile.namespace())) ? "ashes_tab" : "native_modules"),
                    requestedCreativeTabId,
                    namespaceItems,
                    namespaceContentItems,
                    Map.of(),
                    orderIndex++
            ));
        }
        return List.copyOf(plans);
    }

    public static List<String> visibleNativeCreativeTabItems(List<Map<String, Object>> registeredCreativeTabs) {
        List<String> items = new ArrayList<>();
        for (Map<String, Object> tab : registeredCreativeTabs == null ? List.<Map<String, Object>>of() : registeredCreativeTabs) {
            if (!Boolean.TRUE.equals(tab.get("firstClassNativeCreativeTabPresent"))
                    || !Boolean.TRUE.equals(tab.get("registered"))
                    || !Boolean.TRUE.equals(tab.get("nativeRegistryContentBacked"))
                    || !Boolean.TRUE.equals(tab.get("releaseCreativeTabTrusted"))
                    || !Boolean.TRUE.equals(tab.get("creativeTabOutputBacked"))
                    || !Boolean.TRUE.equals(tab.get("creativeTabSearchOutputBacked"))
                    || Boolean.FALSE.equals(tab.get("declaredCreativeTabItemsBackedByNativeRegistry"))
                    || Boolean.FALSE.equals(tab.get("declaredIconItemBackedByNativeRegistry"))
                    || Boolean.FALSE.equals(tab.get("resolvedIconItemBackedByNativeRegistry"))
                    || Boolean.TRUE.equals(tab.get("fallbackOnlyCreativeVisibility"))) {
                continue;
            }
            boolean searchVisible = !Boolean.FALSE.equals(tab.get("searchVisible"));
            List<String> nativeRegistryItems = normalizedContentIds(stringList(tab.get("creativeTabItemsFromNativeRegistry")));
            List<String> provenOutputItems = normalizedContentIds(stringList(tab.get("creativeTabOutputProofItemIds")));
            List<String> provenSearchOutputItems = normalizedContentIds(stringList(tab.get("creativeTabSearchOutputProofItemIds")));
            if (nativeRegistryItems.isEmpty()
                    || provenOutputItems.isEmpty()
                    || (searchVisible && provenSearchOutputItems.isEmpty())
                    || !provenOutputItems.containsAll(nativeRegistryItems)
                    || (searchVisible && !provenSearchOutputItems.containsAll(nativeRegistryItems))) {
                continue;
            }
            for (String outputItem : provenOutputItems) {
                if (nativeRegistryItems.contains(outputItem) && !items.contains(outputItem)) {
                    items.add(outputItem);
                }
            }
        }
        return List.copyOf(items);
    }

    private static Map<String, Object> creativeTabPlan(
            EchoNativeBootstrapProductProfile profile,
            String namespace,
            String tabPath,
            String requestedCreativeTabId,
            List<String> allNativeItems,
            List<String> registeredContentItems,
            Map<String, Object> requestedCreativeTabDeclaration,
            int orderIndex
    ) {
        Map<String, Object> declaration = requestedCreativeTabDeclaration == null
                ? Map.of()
                : requestedCreativeTabDeclaration;
        String safeNamespace = safeNamespace(namespace);
        String safeTabPath = string(tabPath).isBlank() ? "native_modules" : string(tabPath);
        String tabId = safeNamespace + ":" + safeTabPath;
        String declaredTitleKey = string(declaration.get("titleKey"));
        String title = safeNamespace.equals(safeNamespace(profile.namespace()))
                ? profile.nativeGameplayDisplayName() + " Native Modules"
                : titleFromNamespace(safeNamespace) + " Native Modules";
        if (!declaredTitleKey.isBlank()) {
            title = declaredTitleKey;
        }
        String iconItem = string(declaration.get("iconItem"));
        String iconSource = "declaration";
        if (iconItem.isBlank()) {
            iconItem = iconItemId(profile, safeNamespace, registeredContentItems, allNativeItems);
            iconSource = preferredIconCandidates(profile, safeNamespace).contains(iconItem)
                    ? "profile_preferred"
                    : "registry_fallback";
        }
        List<String> declaredItems = normalizedContentIds(stringList(declaration.get("itemIds")));
        List<String> unbackedItems = unbackedDeclaredCreativeTabItems(declaration, allNativeItems);
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("tabId", tabId);
        plan.put("tabPath", safeTabPath);
        plan.put("namespace", safeNamespace);
        plan.put("moduleScoped", true);
        plan.put("creativeGroupId", tabId);
        plan.put("nativeCreativeGroupId", tabId);
        plan.put("orderIndex", orderIndex);
        plan.put("sortKey", "%05d:%s".formatted(orderIndex, tabId));
        plan.put("orderAnchor", string(declaration.get("orderAnchor")).isBlank()
                ? "minecraft:combat"
                : string(declaration.get("orderAnchor")));
        plan.put("orderStrategy", "with_tabs_before_anchor");
        plan.put("requestedCreativeTabId", requestedCreativeTabId);
        plan.put("sdkCreativeTabDeclared", !string(requestedCreativeTabId).isBlank());
        plan.put("title", title);
        plan.put("titleKey", declaredTitleKey);
        plan.put("iconItem", iconItem);
        plan.put("iconSource", iconSource);
        plan.put("preferredIconCandidates", preferredIconCandidates(profile, safeNamespace));
        plan.put("declaredSurfaceIds", stringList(declaration.get("surfaceIds")));
        plan.put("items", List.copyOf(allNativeItems));
        plan.put("registeredContentItems", registeredContentItems == null ? List.of() : List.copyOf(registeredContentItems));
        plan.put("itemCount", allNativeItems.size());
        plan.put("plannedVisibleItemCount", allNativeItems.size());
        plan.put("nativeRegistryContentBacked", true);
        plan.put("nativeCreativeTabPopulationMode", "native_registry_content_snapshot");
        plan.put("creativeTabItemsFromNativeRegistry", List.copyOf(allNativeItems));
        plan.put("creativeTabItemsFromDeclarations", declaredItems);
        plan.put("unbackedDeclaredCreativeTabItems", unbackedItems);
        plan.put("declaredCreativeTabItemsBackedByNativeRegistry", unbackedItems.isEmpty());
        String searchVisibility = creativeTabSearchVisibility(declaration);
        boolean searchVisible = creativeTabSearchVisible(declaration, searchVisibility);
        plan.put("searchVisibility", searchVisibility);
        plan.put("searchVisible", searchVisible);
        plan.put("searchVisibleItemIds", searchVisible ? List.copyOf(allNativeItems) : List.of());
        plan.put("declaration", Map.copyOf(declaration));
        return Map.copyOf(plan);
    }

    private static Map<String, Object> registerCreativeTab(
            EchoNativeBootstrapProductProfile profile,
            String namespace,
            String tabPath,
            String requestedCreativeTabId,
            List<String> allNativeItems,
            List<String> registeredContentItems,
            Class<?> identifierClass,
            Class<?> registryClass,
            Class<?> creativeModeTabClass,
            Class<?> creativeModeTabsClass,
            Class<?> componentClass,
            Class<?> itemStackClass,
            Class<?> itemLikeClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            Object creativeTabRegistry,
            Object itemRegistry,
            Map<String, Object> requestedCreativeTabDeclaration
    ) {
        return registerCreativeTab(
                profile,
                namespace,
                tabPath,
                requestedCreativeTabId,
                allNativeItems,
                registeredContentItems,
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
                requestedCreativeTabDeclaration,
                creativeTabPlan(
                        profile,
                        namespace,
                        tabPath,
                        requestedCreativeTabId,
                        allNativeItems,
                        registeredContentItems,
                        requestedCreativeTabDeclaration,
                        0
                )
        );
    }

    private static Map<String, Object> registerCreativeTab(
            EchoNativeBootstrapProductProfile profile,
            String namespace,
            String tabPath,
            String requestedCreativeTabId,
            List<String> allNativeItems,
            List<String> registeredContentItems,
            Class<?> identifierClass,
            Class<?> registryClass,
            Class<?> creativeModeTabClass,
            Class<?> creativeModeTabsClass,
            Class<?> componentClass,
            Class<?> itemStackClass,
            Class<?> itemLikeClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            Object creativeTabRegistry,
            Object itemRegistry,
            Map<String, Object> requestedCreativeTabDeclaration,
            Map<String, Object> plan
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        String tabId = string(plan.get("tabId"));
        String declaredTitleKey = string(plan.get("titleKey"));
        String title = string(plan.get("title"));
        bridge.putAll(plan);
        bridge.put("tabId", tabId);
        bridge.put("namespace", namespace);
        bridge.put("moduleScoped", true);
        bridge.put("customTabCreated", false);
        bridge.put("existingNativeCreativeTab", false);
        bridge.put("firstClassNativeCreativeTabCreated", false);
        bridge.put("firstClassNativeCreativeTabPresent", false);
        bridge.put("registered", false);
        bridge.put("existingCreativeContainer", false);
        bridge.put("sdkCreativeTabDeclared", !requestedCreativeTabId.isBlank());
        bridge.put("requestedCreativeTabId", requestedCreativeTabId);
        bridge.put("titleKey", declaredTitleKey);
        bridge.put("declaredSurfaceIds", stringList(requestedCreativeTabDeclaration.get("surfaceIds")));
        bridge.put("strategy", "native_registry_creative_tab");
        bridge.put("itemCount", allNativeItems.size());
        bridge.put("visibleItemCount", 0);
        bridge.put("items", allNativeItems);
        bridge.put("nativeRegistryContentBacked", true);
        bridge.put("nativeCreativeTabPopulationMode", "native_registry_content_snapshot");
        bridge.put("nativeRegistryContentItemCount", registeredContentItems.size());
        bridge.put("nativeRegistryBlockItemCount", allNativeItems.stream()
                .filter(itemId -> !registeredContentItems.contains(itemId))
                .count());
        bridge.put("creativeTabItemsFromNativeRegistry", allNativeItems);
        bridge.put("creativeTabItemsFromDeclarations", normalizedContentIds(stringList(requestedCreativeTabDeclaration.get("itemIds"))));
        bridge.put("unbackedDeclaredCreativeTabItems",
                unbackedDeclaredCreativeTabItems(requestedCreativeTabDeclaration, allNativeItems));
        bridge.put("declaredCreativeTabItemsBackedByNativeRegistry",
                unbackedDeclaredCreativeTabItems(requestedCreativeTabDeclaration, allNativeItems).isEmpty());
        String iconItem = string(plan.get("iconItem"));
        try {
            Object id = identifier(namespace, tabPath, identifierClass);
            Object existing = getRegistryValue(creativeTabRegistry, identifierClass, id);
            boolean created = existing == null;
            Object tab = existing;
            String declaredIconItem = normalizedContentId(string(requestedCreativeTabDeclaration.get("iconItem")));
            String plannedIconItem = normalizedContentId(iconItem);
            String resolvedIconItem = resolvedItemId(
                    iconCandidates(plannedIconItem, allNativeItems),
                    itemRegistry,
                    identifierClass
            );
            boolean declaredIconBacked = declaredIconItem.isBlank() || declaredIconItem.equals(resolvedIconItem);
            String liveIconItem = resolvedIconItem.isBlank() ? plannedIconItem : resolvedIconItem;
            if (existing == null) {
                tab = buildCreativeTab(
                        title,
                        tabId,
                        allNativeItems,
                        registeredContentItems,
                        creativeModeTabClass,
                        creativeModeTabsClass,
                        componentClass,
                        itemStackClass,
                        itemLikeClass,
                        tabVisibilityClass,
                        outputClass,
                        itemRegistry,
                        identifierClass,
                        liveIconItem,
                        declaredTitleKey,
                        string(plan.get("orderAnchor")),
                        string(plan.get("searchVisibility"))
                );
                registryClass.getMethod("register", registryClass, identifierClass, Object.class)
                        .invoke(null, creativeTabRegistry, id, tab);
            }
            CreativeTabOutputProof outputProof = existingCreativeTabOutputProof(tab, outputClass);
            List<String> provenOutputItems = outputProof.itemIds();
            List<String> provenSearchVisibleItems = outputProof.searchVisibleItemIds();
            boolean outputBacked = provenOutputItems.containsAll(allNativeItems);
            boolean searchVisible = !Boolean.FALSE.equals(bridge.get("searchVisible"));
            boolean searchOutputBacked = !searchVisible
                    || !outputProof.visibilityInspectable()
                    || provenSearchVisibleItems.containsAll(allNativeItems);
            boolean resolvedIconBacked = !liveIconItem.isBlank() && allNativeItems.contains(liveIconItem);
            boolean declaredItemsBacked = Boolean.TRUE.equals(bridge.get("declaredCreativeTabItemsBackedByNativeRegistry"));
            boolean releaseTrusted = outputBacked && searchOutputBacked
                    && declaredItemsBacked && declaredIconBacked && resolvedIconBacked;
            bridge.put("customTabCreated", created);
            bridge.put("existingNativeCreativeTab", !created);
            bridge.put("firstClassNativeCreativeTabCreated", created);
            bridge.put("firstClassNativeCreativeTabPresent", true);
            bridge.put("registered", true);
            bridge.put("releaseCreativeTabTrusted", releaseTrusted);
            bridge.put("fallbackOnlyCreativeVisibility", false);
            bridge.put("creativeTabRegistrationMode", created
                    ? "created_native_registry_tab"
                    : "existing_native_registry_tab");
            bridge.put("creativeTabOutputProofItemIds", provenOutputItems);
            bridge.put("creativeTabOutputBacked", outputBacked);
            bridge.put("creativeTabSearchOutputProofItemIds", provenSearchVisibleItems);
            bridge.put("creativeTabSearchOutputBacked", searchOutputBacked);
            bridge.put("creativeTabOutputVisibilityInspectable", outputProof.visibilityInspectable());
            bridge.put("existingNativeCreativeTabOutputProofItemIds", provenOutputItems);
            bridge.put("existingNativeCreativeTabOutputBacked", outputBacked);
            bridge.put("existingNativeCreativeTabSearchOutputProofItemIds", provenSearchVisibleItems);
            bridge.put("existingNativeCreativeTabSearchOutputBacked", searchOutputBacked);
            bridge.put("declaredIconItem", declaredIconItem);
            bridge.put("plannedIconItem", plannedIconItem);
            bridge.put("resolvedIconItem", liveIconItem);
            bridge.put("declaredIconItemBackedByNativeRegistry", declaredIconBacked);
            bridge.put("resolvedIconItemBackedByNativeRegistry", resolvedIconBacked);
            bridge.put("declaredIconItemFallbackUsed", !declaredIconItem.isBlank() && !declaredIconBacked);
            bridge.put("visibleItemCount", releaseTrusted ? allNativeItems.size() : 0);
            bridge.put("iconItem", liveIconItem);
            bridge.put("preferredIconCandidates", preferredIconCandidates(profile, namespace));
            bridge.put("title", title);
            bridge.put("summary", created
                    ? outputBacked
                    ? "Native Loader registered a namespace creative tab for native module content and proved its live output contains the registry-backed native item population."
                    : "Native Loader registered a namespace creative tab for native module content, but could not prove its live output contains the registry-backed native item population."
                    : outputBacked
                    ? "Native Loader found the exact native namespace creative tab already registered and proved its output contains the registry-backed native item population."
                    : "Native Loader found the exact native namespace creative tab already registered, but could not prove its output contains the registry-backed native item population.");
        } catch (Throwable exception) {
            bridge.put("existingCreativeContainer", true);
            bridge.put("strategy", "existing_vanilla_creative_containers_fallback");
            bridge.put("nativeRegistryContentBacked", false);
            bridge.put("nativeCreativeTabPopulationMode", "unproven_fallback_existing_containers");
            bridge.put("existingNativeCreativeTab", false);
            bridge.put("firstClassNativeCreativeTabCreated", false);
            bridge.put("firstClassNativeCreativeTabPresent", false);
            bridge.put("releaseCreativeTabTrusted", false);
            bridge.put("visibleItemCount", 0);
            bridge.put("fallbackOnlyCreativeVisibility", true);
            bridge.put("failureKind", exception.getClass().getSimpleName());
            bridge.put("failureMessage", failureMessage(exception));
            bridge.put("summary", "Native Loader could not create the namespace creative tab, so content will be surfaced through existing Minecraft creative containers/search by the live creative visibility bridge.");
        }
        return Map.copyOf(bridge);
    }

    private static List<String> iconCandidates(String plannedIconItem, List<String> allNativeItems) {
        List<String> candidates = new ArrayList<>();
        String planned = normalizedContentId(plannedIconItem);
        if (!planned.isBlank()) {
            candidates.add(planned);
        }
        for (String itemId : allNativeItems == null ? List.<String>of() : allNativeItems) {
            String normalized = normalizedContentId(itemId);
            if (!normalized.isBlank() && !candidates.contains(normalized)) {
                candidates.add(normalized);
            }
        }
        for (String fallback : List.of("minecraft:compass", "minecraft:stick")) {
            if (!candidates.contains(fallback)) {
                candidates.add(fallback);
            }
        }
        return List.copyOf(candidates);
    }

    private static String resolvedItemId(
            List<String> itemIds,
            Object itemRegistry,
            Class<?> identifierClass
    ) {
        for (String itemId : itemIds == null ? List.<String>of() : itemIds) {
            try {
                int separator = itemId.indexOf(':');
                if (separator < 1 || separator + 1 >= itemId.length()) {
                    continue;
                }
                Object id = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                        .invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
                Object item = registryItem(itemRegistry, identifierClass, id);
                if (item != null) {
                    return itemId;
                }
            } catch (Throwable ignored) {
                // Icon proof must come from a resolvable live item registry entry.
            }
        }
        return "";
    }

    private record CreativeTabOutputProof(
            List<String> itemIds,
            List<String> searchVisibleItemIds,
            boolean visibilityInspectable
    ) {
    }

    private record ReturnedCreativeTabItems(boolean methodPresent, List<String> itemIds) {
    }

    private static List<String> existingCreativeTabOutputItems(Object existingTab, Class<?> outputClass) {
        return existingCreativeTabOutputProof(existingTab, outputClass).itemIds();
    }

    private static CreativeTabOutputProof existingCreativeTabOutputProof(Object existingTab, Class<?> outputClass) {
        if (existingTab == null) {
            return new CreativeTabOutputProof(List.of(), List.of(), false);
        }
        CreativeTabOutputProof returnedProof = existingCreativeTabReturnedOutputProof(existingTab);
        if (!returnedProof.itemIds().isEmpty()) {
            return returnedProof;
        }
        return existingCreativeTabEmittedOutputItems(existingTab, outputClass);
    }

    private static List<String> existingCreativeTabReturnedOutputItems(Object existingTab) {
        return existingCreativeTabReturnedOutputProof(existingTab).itemIds();
    }

    private static CreativeTabOutputProof existingCreativeTabReturnedOutputProof(Object existingTab) {
        ReturnedCreativeTabItems displayItems = returnedCreativeTabItems(existingTab, "getDisplayItems");
        ReturnedCreativeTabItems searchItems = returnedCreativeTabItems(existingTab, "getSearchTabDisplayItems");
        if (!displayItems.itemIds().isEmpty()) {
            return new CreativeTabOutputProof(
                    displayItems.itemIds(),
                    searchItems.methodPresent() ? searchItems.itemIds() : displayItems.itemIds(),
                    searchItems.methodPresent()
            );
        }
        if (searchItems.methodPresent() && !searchItems.itemIds().isEmpty()) {
            return new CreativeTabOutputProof(searchItems.itemIds(), searchItems.itemIds(), true);
        }
        return new CreativeTabOutputProof(List.of(), List.of(), false);
    }

    private static ReturnedCreativeTabItems returnedCreativeTabItems(Object existingTab, String methodName) {
        try {
            Object value = existingTab.getClass().getMethod(methodName).invoke(existingTab);
            return new ReturnedCreativeTabItems(true, stackLikeItemIds(value));
        } catch (Throwable ignored) {
            // Older mappings or unbuilt tabs may not expose display item collections.
            return new ReturnedCreativeTabItems(false, List.of());
        }
    }

    private static CreativeTabOutputProof existingCreativeTabEmittedOutputItems(Object existingTab, Class<?> outputClass) {
        if (outputClass == null || outputClass.isInterface()
                || java.lang.reflect.Modifier.isAbstract(outputClass.getModifiers())) {
            return new CreativeTabOutputProof(List.of(), List.of(), false);
        }
        for (java.lang.reflect.Method method : existingTab.getClass().getMethods()) {
            if (!List.of("emitItems", "displayItems").contains(method.getName())
                    || method.getParameterCount() != 1
                    || !method.getParameterTypes()[0].isAssignableFrom(outputClass)) {
                continue;
            }
            Object output = instantiateOutput(outputClass);
            if (output == null) {
                continue;
            }
            try {
                method.invoke(existingTab, output);
                List<String> itemIds = outputItemIds(output);
                if (!itemIds.isEmpty()) {
                    return new CreativeTabOutputProof(
                            itemIds,
                            outputSearchVisibleItemIds(output),
                            outputVisibilityInspectable(output)
                    );
                }
            } catch (Throwable ignored) {
                // Preexisting creative tabs are trusted only when output proof is inspectable.
            }
        }
        return new CreativeTabOutputProof(List.of(), List.of(), false);
    }

    private static Object instantiateOutput(Class<?> outputClass) {
        try {
            java.lang.reflect.Constructor<?> constructor = outputClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<String> outputItemIds(Object output) {
        try {
            Object value = output.getClass().getMethod("itemIds").invoke(output);
            if (value instanceof Iterable<?> iterable) {
                List<String> ids = new ArrayList<>();
                for (Object item : iterable) {
                    String id = normalizedContentId(String.valueOf(item));
                    if (!id.isBlank() && !ids.contains(id)) {
                        ids.add(id);
                    }
                }
                return List.copyOf(ids);
            }
        } catch (Throwable ignored) {
            // Output implementations without an itemIds accessor cannot prove release-visible content.
        }
        return List.of();
    }

    private static boolean outputVisibilityInspectable(Object output) {
        try {
            Object value = output.getClass().getMethod("visibilities").invoke(output);
            return value instanceof Iterable<?>;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<String> outputSearchVisibleItemIds(Object output) {
        try {
            Object itemValue = output.getClass().getMethod("itemIds").invoke(output);
            Object visibilityValue = output.getClass().getMethod("visibilities").invoke(output);
            if (!(itemValue instanceof Iterable<?> items) || !(visibilityValue instanceof Iterable<?> visibilities)) {
                return List.of();
            }
            List<Object> itemList = new ArrayList<>();
            items.forEach(itemList::add);
            List<Object> visibilityList = new ArrayList<>();
            visibilities.forEach(visibilityList::add);
            List<String> ids = new ArrayList<>();
            for (int index = 0; index < itemList.size() && index < visibilityList.size(); index++) {
                if (!creativeTabVisibilitySearchVisible(visibilityList.get(index))) {
                    continue;
                }
                String id = normalizedContentId(String.valueOf(itemList.get(index)));
                if (!id.isBlank() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
            return List.copyOf(ids);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    private static boolean creativeTabVisibilitySearchVisible(Object visibility) {
        String text = String.valueOf(visibility).trim().toLowerCase(java.util.Locale.ROOT);
        return text.contains("parent_and_search") || text.contains("search");
    }

    private static List<String> stackLikeItemIds(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (Object stack : iterable) {
            String id = stackLikeItemId(stack);
            if (!id.isBlank() && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    private static String stackLikeItemId(Object stack) {
        if (stack == null) {
            return "";
        }
        for (String methodName : List.of("item", "getItem")) {
            try {
                Object item = stack.getClass().getMethod(methodName).invoke(stack);
                String id = itemLikeId(item);
                if (!id.isBlank()) {
                    return id;
                }
            } catch (Throwable ignored) {
                // Try the next common accessor.
            }
        }
        return "";
    }

    private static String itemLikeId(Object item) {
        if (item == null) {
            return "";
        }
        for (String methodName : List.of("id", "getId")) {
            try {
                String id = normalizedContentId(String.valueOf(item.getClass().getMethod(methodName).invoke(item)));
                if (!id.isBlank()) {
                    return id;
                }
            } catch (Throwable ignored) {
                // Native runtime item classes are mapping-dependent; fail closed when no id is exposed.
            }
        }
        return "";
    }

    private static Object buildCreativeTab(
            String titleText,
            String tabId,
            List<String> allNativeItems,
            List<String> registeredContentItems,
            Class<?> creativeModeTabClass,
            Class<?> creativeModeTabsClass,
            Class<?> componentClass,
            Class<?> itemStackClass,
            Class<?> itemLikeClass,
            Class<?> tabVisibilityClass,
            Class<?> outputClass,
            Object itemRegistry,
            Class<?> identifierClass,
            String iconItem,
            String titleKey,
            String orderAnchor,
            String searchVisibility
    ) throws ReflectiveOperationException {
        Object builder = creativeTabBuilder(creativeModeTabClass);
        Object title = titleComponent(componentClass, titleText, titleKey);
        builder.getClass().getMethod("title", componentClass).invoke(builder, title);
        putBeforeAnchorIfSupported(builder, creativeModeTabsClass, orderAnchor);

        java.lang.reflect.Method identifierFactory = identifierClass.getMethod(
                "fromNamespaceAndPath",
                String.class,
                String.class
        );
        java.lang.reflect.Method getValue = itemRegistry.getClass().getMethod("getValue", identifierClass);
        java.lang.reflect.Constructor<?> itemStackConstructor = itemStackClass.getConstructor(itemLikeClass);
            Object iconStack = firstStack(
                List.of(iconItem, "minecraft:compass", "minecraft:stick"),
                itemRegistry,
                identifierFactory,
                getValue,
                itemStackConstructor
        );
        if (iconStack == null) {
            throw new IllegalStateException("No item stack could be resolved for " + tabId
                    + "; candidates="
                    + stackResolutionDiagnostics(
                    List.of(iconItem, "minecraft:compass", "minecraft:stick"),
                    itemRegistry,
                    identifierFactory,
                    getValue,
                    itemStackConstructor
            ));
        }
        Supplier<Object> iconSupplier = () -> iconStack;
        builder.getClass().getMethod("icon", Supplier.class).invoke(builder, iconSupplier);

        Class<?> generatorClass = Class.forName(creativeModeTabClass.getName() + "$DisplayItemsGenerator");
        Object visibility = creativeTabVisibility(tabVisibilityClass, searchVisibility);
        java.lang.reflect.Method outputAccept = outputClass.getMethod("accept", itemStackClass, tabVisibilityClass);
        Object generator = java.lang.reflect.Proxy.newProxyInstance(
                generatorClass.getClassLoader(),
                new Class<?>[]{generatorClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("accept") && args != null && args.length == 2) {
                        appendCreativeOutputItems(
                                args[1],
                                allNativeItems,
                                itemRegistry,
                                identifierFactory,
                                getValue,
                                itemStackConstructor,
                                outputAccept,
                                visibility
                        );
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
        builder.getClass().getMethod("displayItems", generatorClass).invoke(builder, generator);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private static Object creativeTabBuilder(Class<?> creativeModeTabClass) throws ReflectiveOperationException {
        try {
            return creativeModeTabClass.getMethod("builder").invoke(null);
        } catch (NoSuchMethodException ignored) {
            // Newer Minecraft versions require a tab row and column for custom creative tab builders.
        }
        for (java.lang.reflect.Method method : creativeModeTabClass.getMethods()) {
            if (!method.getName().equals("builder")
                    || method.getParameterCount() == 0
                    || !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            Object[] arguments = creativeTabBuilderArguments(method.getParameterTypes());
            if (arguments == null) {
                continue;
            }
            return method.invoke(null, arguments);
        }
        throw new NoSuchMethodException(creativeModeTabClass.getName() + ".builder()");
    }

    private static Object[] creativeTabBuilderArguments(Class<?>[] parameterTypes) {
        Object[] arguments = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            Class<?> parameterType = parameterTypes[index];
            if (parameterType.isEnum()) {
                Object[] constants = parameterType.getEnumConstants();
                if (constants == null || constants.length == 0) {
                    return null;
                }
                arguments[index] = constants[0];
            } else if (parameterType == int.class || parameterType == Integer.class) {
                arguments[index] = 0;
            } else {
                return null;
            }
        }
        return arguments;
    }

    private static String creativeTabSearchVisibility(Map<String, Object> declaration) {
        String declared = string(declaration == null ? null : declaration.get("searchVisibility"))
                .trim()
                .toLowerCase()
                .replace('-', '_')
                .replace('.', '_');
        if (declared.equals("parent_tabs") || declared.equals("parent_tab_only") || declared.equals("parent_only")) {
            return "parent_tabs";
        }
        if (Boolean.FALSE.equals(declaration == null ? null : declaration.get("searchVisible"))) {
            return "parent_tabs";
        }
        return "parent_and_search_tabs";
    }

    private static boolean creativeTabSearchVisible(Map<String, Object> declaration, String searchVisibility) {
        if (Boolean.FALSE.equals(declaration == null ? null : declaration.get("searchVisible"))) {
            return false;
        }
        return !"parent_tabs".equals(searchVisibility);
    }

    private static Object creativeTabVisibility(Class<?> tabVisibilityClass, String searchVisibility)
            throws ReflectiveOperationException {
        if ("parent_tabs".equals(searchVisibility)) {
            for (String fieldName : List.of("PARENT_TABS", "PARENT_TAB_ONLY", "PARENT_ONLY")) {
                try {
                    return tabVisibilityClass.getField(fieldName).get(null);
                } catch (NoSuchFieldException ignored) {
                    // Runtime mappings differ; try the next known parent-only constant.
                }
            }
        }
        return tabVisibilityClass.getField("PARENT_AND_SEARCH_TABS").get(null);
    }

    private static Object titleComponent(Class<?> componentClass, String titleText, String titleKey)
            throws ReflectiveOperationException {
        if (titleKey != null && !titleKey.isBlank()) {
            try {
                return componentClass.getMethod("translatable", String.class).invoke(null, titleKey);
            } catch (NoSuchMethodException ignored) {
                // Older runtime mappings still have literal; the visible fallback stays usable.
            }
        }
        return componentClass.getMethod("literal", String.class).invoke(null, titleText);
    }

    private static void putBeforeAnchorIfSupported(Object builder, Class<?> creativeModeTabsClass, String orderAnchor) {
        try {
            Object anchorKey = creativeModeTabsClass.getField(creativeModeTabFieldName(orderAnchor)).get(null);
            for (java.lang.reflect.Method method : builder.getClass().getMethods()) {
                if (!method.getName().equals("withTabsBefore")
                        || method.getParameterCount() != 1
                        || !method.getParameterTypes()[0].isArray()) {
                    continue;
                }
                Object array = Array.newInstance(method.getParameterTypes()[0].getComponentType(), 1);
                Array.set(array, 0, anchorKey);
                method.invoke(builder, array);
                return;
            }
        } catch (Throwable ignored) {
            // Tab order is cosmetic; registration and content visibility are the release-critical path.
        }
    }

    private static String creativeModeTabFieldName(String orderAnchor) {
        String anchor = string(orderAnchor);
        if (anchor.startsWith("minecraft:")) {
            anchor = anchor.substring("minecraft:".length());
        }
        return switch (anchor) {
            case "building_blocks" -> "BUILDING_BLOCKS";
            case "colored_blocks" -> "COLORED_BLOCKS";
            case "natural_blocks" -> "NATURAL_BLOCKS";
            case "functional_blocks" -> "FUNCTIONAL_BLOCKS";
            case "redstone_blocks" -> "REDSTONE_BLOCKS";
            case "tools_and_utilities" -> "TOOLS_AND_UTILITIES";
            case "combat" -> "COMBAT";
            case "food_and_drinks" -> "FOOD_AND_DRINKS";
            case "ingredients" -> "INGREDIENTS";
            case "spawn_eggs" -> "SPAWN_EGGS";
            default -> "COMBAT";
        };
    }

    private static void appendCreativeOutputItems(
            Object output,
            List<String> itemIds,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor,
            java.lang.reflect.Method outputAccept,
            Object visibility
    ) {
        for (String itemId : itemIds) {
            try {
                Object stack = stackForItemId(itemId, itemRegistry, identifierFactory, getValue, itemStackConstructor);
                if (stack != null) {
                    outputAccept.invoke(output, stack, visibility);
                }
            } catch (Throwable ignored) {
                // One bad native content id must not drop the whole tab.
            }
        }
    }

    private static Object firstStack(
            List<String> itemIds,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor
    ) {
        for (String itemId : itemIds) {
            Object stack = stackForItemId(itemId, itemRegistry, identifierFactory, getValue, itemStackConstructor);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    private static List<Map<String, Object>> stackResolutionDiagnostics(
            List<String> itemIds,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor
    ) {
        List<Map<String, Object>> diagnostics = new ArrayList<>();
        for (String itemId : itemIds == null ? List.<String>of() : itemIds) {
            Map<String, Object> diagnostic = new LinkedHashMap<>();
            diagnostic.put("itemId", itemId);
            try {
                int separator = itemId.indexOf(':');
                if (separator < 1 || separator + 1 >= itemId.length()) {
                    diagnostic.put("resolved", false);
                    diagnostic.put("failureKind", "InvalidIdentifier");
                    diagnostics.add(Map.copyOf(diagnostic));
                    continue;
                }
                Object id = identifierFactory.invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
                Object item = unwrapRegistryValue(getValue.invoke(itemRegistry, id));
                if (item == null) {
                    item = registryItem(itemRegistry, id.getClass(), id);
                }
                diagnostic.put("itemResolved", item != null);
                diagnostic.put("itemClass", item == null ? "" : item.getClass().getName());
                Object stack = item == null ? null : itemStackConstructor.newInstance(item);
                diagnostic.put("stackResolved", stack != null);
                diagnostic.put("stackClass", stack == null ? "" : stack.getClass().getName());
            } catch (Throwable exception) {
                diagnostic.put("itemResolved", false);
                diagnostic.put("stackResolved", false);
                diagnostic.put("failureKind", exception.getClass().getSimpleName());
                diagnostic.put("failureMessage", failureMessage(exception));
            }
            diagnostics.add(Map.copyOf(diagnostic));
        }
        return List.copyOf(diagnostics);
    }

    private static Object stackForItemId(
            String itemId,
            Object itemRegistry,
            java.lang.reflect.Method identifierFactory,
            java.lang.reflect.Method getValue,
            java.lang.reflect.Constructor<?> itemStackConstructor
    ) {
        try {
            int separator = itemId.indexOf(':');
            if (separator < 1 || separator + 1 >= itemId.length()) {
                return null;
            }
            Object id = identifierFactory.invoke(null, itemId.substring(0, separator), itemId.substring(separator + 1));
            Object item = unwrapRegistryValue(getValue.invoke(itemRegistry, id));
            if (item == null) {
                item = registryItem(itemRegistry, id.getClass(), id);
            }
            return item == null ? null : itemStackConstructor.newInstance(item);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object registryItem(Object itemRegistry, Class<?> identifierClass, Object id) {
        if (itemRegistry == null || identifierClass == null || id == null) {
            return null;
        }
        for (String methodName : List.of("getValue", "get", "getOptional", "getOptionalValue")) {
            for (java.lang.reflect.Method method : itemRegistry.getClass().getMethods()) {
                if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameterType = method.getParameterTypes()[0];
                if (!parameterType.isAssignableFrom(id.getClass())) {
                    continue;
                }
                try {
                    Object unwrapped = unwrapRegistryValue(method.invoke(itemRegistry, id));
                    if (unwrapped != null) {
                        return unwrapped;
                    }
                } catch (Throwable ignored) {
                    // Runtime mappings vary; try the next compatible registry accessor.
                }
            }
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

    private static Object getRegistryValue(Object registry, Class<?> identifierClass, Object id) {
        try {
            return registry.getClass().getMethod("getValue", identifierClass).invoke(registry, id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object identifier(String namespace, String path, Class<?> identifierClass)
            throws ReflectiveOperationException {
        return identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                .invoke(null, namespace, path);
    }

    private static String iconItemId(
            EchoNativeBootstrapProductProfile profile,
            String namespace,
            List<String> registeredContentItems,
            List<String> allNativeItems
    ) {
        for (String preferred : preferredIconCandidates(profile, namespace)) {
            if (allNativeItems.contains(preferred)) {
                return preferred;
            }
        }
        List<String> groupContentItems = registeredContentItems == null
                ? List.of()
                : registeredContentItems.stream()
                .filter(allNativeItems::contains)
                .toList();
        return groupContentItems.stream()
                .filter(itemId -> itemId.endsWith(":scrap_knife"))
                .findFirst()
                .or(() -> allNativeItems.stream().filter(itemId -> itemId.endsWith(":scrap_knife")).findFirst())
                .orElseGet(() -> groupContentItems.isEmpty() ? allNativeItems.get(0) : groupContentItems.get(0));
    }

    private static List<String> preferredIconCandidates(EchoNativeBootstrapProductProfile profile, String namespace) {
        if (profile == null) {
            return List.of();
        }
        List<String> preferred = profile.nativeCreativeTabPreferredIcons().get(safeNamespace(namespace));
        if (preferred == null || preferred.isEmpty()) {
            return List.of();
        }
        return preferred.stream()
                .filter(itemId -> itemId != null && !itemId.isBlank())
                .map(String::trim)
                .toList();
    }

    private static String requestedCreativeTabId(String namespace, List<String> requestedCreativeTabIds) {
        String safeNamespace = safeNamespace(namespace);
        if (requestedCreativeTabIds == null) {
            return "";
        }
        for (String tabId : requestedCreativeTabIds) {
            String normalized = tabId == null ? "" : tabId.trim().toLowerCase(java.util.Locale.ROOT);
            int separator = normalized.indexOf(':');
            if (separator > 0 && safeNamespace.equals(normalized.substring(0, separator))
                    && separator + 1 < normalized.length()) {
                return normalized;
            }
        }
        return "";
    }

    private static Map<String, List<Map<String, Object>>> creativeTabDeclarationsByNamespace(
            List<Map<String, Object>> declarations
    ) {
        Map<String, List<Map<String, Object>>> byNamespace = new LinkedHashMap<>();
        for (Map<String, Object> declaration : declarations == null ? List.<Map<String, Object>>of() : declarations) {
            String tabId = normalizedContentId(String.valueOf(declaration.getOrDefault("id", "")));
            int separator = tabId.indexOf(':');
            if (separator < 1 || separator + 1 >= tabId.length()) {
                continue;
            }
            Map<String, Object> normalized = new LinkedHashMap<>(declaration);
            normalized.put("registry", "creative_tab");
            normalized.put("id", tabId);
            byNamespace.computeIfAbsent(safeNamespace(tabId.substring(0, separator)), ignored -> new ArrayList<>())
                    .add(Map.copyOf(normalized));
        }
        Map<String, List<Map<String, Object>>> copy = new LinkedHashMap<>();
        byNamespace.forEach((namespace, namespaceDeclarations) ->
                copy.put(namespace, List.copyOf(namespaceDeclarations)));
        return Map.copyOf(copy);
    }

    private static List<String> creativeItemsForDeclaration(
            Map<String, Object> declaration,
            List<String> namespaceItems
    ) {
        List<String> declaredItems = normalizedContentIds(stringList(declaration.get("itemIds")));
        if (declaredItems.isEmpty()) {
            return List.copyOf(namespaceItems);
        }
        List<String> registryBackedDeclaredItems = declaredItems.stream()
                .filter(namespaceItems::contains)
                .toList();
        return List.copyOf(registryBackedDeclaredItems);
    }

    private static List<String> unbackedDeclaredCreativeTabItems(
            Map<String, Object> declaration,
            List<String> nativeRegistryItems
    ) {
        List<String> declaredItems = normalizedContentIds(stringList(declaration.get("itemIds")));
        if (declaredItems.isEmpty()) {
            return List.of();
        }
        return declaredItems.stream()
                .filter(itemId -> !nativeRegistryItems.contains(itemId))
                .toList();
    }

    private static List<String> normalizedContentIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String id : ids) {
            String itemId = normalizedContentId(id);
            if (!itemId.isBlank() && !normalized.contains(itemId)) {
                normalized.add(itemId);
            }
        }
        return List.copyOf(normalized);
    }

    private static String normalizedContentId(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator < 1 || separator + 1 >= normalized.length()) {
            return "";
        }
        return normalized;
    }

    private static String requestedCreativeTabPath(String namespace, String requestedCreativeTabId, String fallback) {
        String normalized = requestedCreativeTabId == null ? "" : requestedCreativeTabId.trim().toLowerCase(java.util.Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator > 0 && safeNamespace(namespace).equals(normalized.substring(0, separator))
                && separator + 1 < normalized.length()) {
            return normalized.substring(separator + 1);
        }
        return fallback;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0F;
        }
        if (type == double.class) {
            return 0.0D;
        }
        return null;
    }

    private static String safeNamespace(String namespace) {
        String value = namespace == null ? "" : namespace.toLowerCase(java.util.Locale.ROOT).trim();
        return value.isBlank() ? "echo_native" : value;
    }

    private static String titleFromNamespace(String namespace) {
        String safe = safeNamespace(namespace).replace('-', '_');
        StringBuilder title = new StringBuilder();
        for (String part : safe.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                title.append(part.substring(1));
            }
        }
        return title.isEmpty() ? "ECHO" : title.toString();
    }

    private static String failureMessage(Throwable exception) {
        Throwable cause = exception.getCause();
        String message = exception.getMessage();
        if ((message == null || message.isBlank()) && cause != null) {
            message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static List<String> mergeCreativeItems(List<String> contentItems, List<String> blockItems) {
        List<String> merged = new ArrayList<>();
        merged.addAll(contentItems);
        merged.addAll(blockItems);
        return merged.stream().filter(itemId -> !itemId.isBlank()).distinct().sorted(String::compareTo).toList();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (Object item : iterable) {
            String text = string(item);
            if (!text.isBlank()) {
                list.add(text);
            }
        }
        return List.copyOf(list);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> object = new LinkedHashMap<>();
        map.forEach((key, item) -> object.put(String.valueOf(key), item));
        return Map.copyOf(object);
    }

    private static Map<String, List<String>> itemsByNamespace(List<String> itemIds) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String itemId : itemIds) {
            int separator = itemId.indexOf(':');
            if (separator < 1 || separator + 1 >= itemId.length()) {
                continue;
            }
            String namespace = safeNamespace(itemId.substring(0, separator));
            grouped.computeIfAbsent(namespace, ignored -> new ArrayList<>()).add(itemId);
        }
        Map<String, List<String>> sorted = new LinkedHashMap<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), List.copyOf(entry.getValue())));
        return Collections.unmodifiableMap(sorted);
    }
}
