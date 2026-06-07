package dev.echo.nativeplatform.loader;

import java.util.List;
import java.util.Map;

public final class NativeLoaderRegistryContentBridge {
    public static final String SERVICE_ID = "echo.native.registry_content_bridge";

    private NativeLoaderRegistryContentBridge() {
    }

    public static Object registerNativeItem(
            String namespace,
            String path,
            Class<?> identifierClass,
            Class<?> resourceKeyClass,
            Class<?> registryClass,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object itemRegistry,
            Object itemRegistryKey,
            NativeItemFactory itemFactory
    ) throws ReflectiveOperationException {
        Object id = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                .invoke(null, namespace, path);
        Object itemKey = resourceKeyClass.getMethod("create", resourceKeyClass, identifierClass)
                .invoke(null, itemRegistryKey, id);
        Object properties = propertiesClass.getConstructor().newInstance();
        propertiesClass.getMethod("setId", resourceKeyClass).invoke(properties, itemKey);
        Object item = itemFactory.create(namespace + ":" + path, itemClass, propertiesClass, properties);
        registryClass.getMethod("register", registryClass, identifierClass, Object.class)
                .invoke(null, itemRegistry, id, item);
        return item;
    }

    public static Object registerNativeBlock(
            String namespace,
            String path,
            Class<?> identifierClass,
            Class<?> resourceKeyClass,
            Class<?> registryClass,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Class<?> blockItemClass,
            Class<?> itemPropertiesClass,
            Object blockRegistry,
            Object blockRegistryKey,
            Object itemRegistry,
            Object itemRegistryKey,
            NativeBlockFactory blockFactory
    ) throws ReflectiveOperationException {
        Object id = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                .invoke(null, namespace, path);
        Object blockKey = resourceKeyClass.getMethod("create", resourceKeyClass, identifierClass)
                .invoke(null, blockRegistryKey, id);
        Object blockProperties = blockPropertiesClass.getMethod("of").invoke(null);
        blockPropertiesClass.getMethod("setId", resourceKeyClass).invoke(blockProperties, blockKey);
        blockPropertiesClass.getMethod("strength", float.class).invoke(blockProperties, 1.0F);
        Object block = blockFactory.create(namespace + ":" + path, blockClass, blockPropertiesClass, blockProperties);
        registryClass.getMethod("register", registryClass, identifierClass, Object.class)
                .invoke(null, blockRegistry, id, block);
        NativeLoaderRegistryRuntimeSupport.initializeNativeBlockStateIds(blockClass, block);

        Object itemKey = resourceKeyClass.getMethod("create", resourceKeyClass, identifierClass)
                .invoke(null, itemRegistryKey, id);
        Object itemProperties = itemPropertiesClass.getConstructor().newInstance();
        itemPropertiesClass.getMethod("setId", resourceKeyClass).invoke(itemProperties, itemKey);
        itemPropertiesClass.getMethod("useBlockDescriptionPrefix").invoke(itemProperties);
        Object blockItem = blockItemClass.getConstructor(blockClass, itemPropertiesClass).newInstance(block, itemProperties);
        registryClass.getMethod("register", registryClass, identifierClass, Object.class)
                .invoke(null, itemRegistry, id, blockItem);
        return blockItem;
    }

    public static int firstClassCreativeTabPresenceCount(List<Map<String, Object>> registeredCreativeTabs) {
        int count = 0;
        for (Map<String, Object> tab : registeredCreativeTabs == null ? List.<Map<String, Object>>of() : registeredCreativeTabs) {
            if (releaseVisibleNativeCreativeTab(tab)) {
                count++;
            }
        }
        return count;
    }

    public static int visibleCreativeTabItemCount(List<Map<String, Object>> registeredCreativeTabs) {
        return NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(registeredCreativeTabs).size();
    }

    public static boolean firstClassCreativeTabsAreRegistryBacked(List<Map<String, Object>> registeredCreativeTabs) {
        boolean present = false;
        for (Map<String, Object> tab : registeredCreativeTabs == null ? List.<Map<String, Object>>of() : registeredCreativeTabs) {
            if (!releaseVisibleNativeCreativeTab(tab)) {
                continue;
            }
            present = true;
            if (!Boolean.TRUE.equals(tab.get("declaredCreativeTabItemsBackedByNativeRegistry"))) {
                return false;
            }
        }
        return present;
    }

    private static boolean releaseVisibleNativeCreativeTab(Map<String, Object> tab) {
        List<String> nativeRegistryItems = normalizedContentIds(stringList(
                tab == null ? null : tab.get("creativeTabItemsFromNativeRegistry")));
        List<String> outputProofItems = normalizedContentIds(stringList(
                tab == null ? null : tab.get("creativeTabOutputProofItemIds")));
        List<String> searchOutputProofItems = normalizedContentIds(stringList(
                tab == null ? null : tab.get("creativeTabSearchOutputProofItemIds")));
        boolean searchVisible = tab == null || !Boolean.FALSE.equals(tab.get("searchVisible"));
        return tab != null
                && Boolean.TRUE.equals(tab.get("firstClassNativeCreativeTabPresent"))
                && Boolean.TRUE.equals(tab.get("registered"))
                && Boolean.TRUE.equals(tab.get("nativeRegistryContentBacked"))
                && Boolean.TRUE.equals(tab.get("releaseCreativeTabTrusted"))
                && Boolean.TRUE.equals(tab.get("creativeTabOutputBacked"))
                && Boolean.TRUE.equals(tab.get("creativeTabSearchOutputBacked"))
                && !Boolean.FALSE.equals(tab.get("declaredCreativeTabItemsBackedByNativeRegistry"))
                && !Boolean.FALSE.equals(tab.get("declaredIconItemBackedByNativeRegistry"))
                && !Boolean.FALSE.equals(tab.get("resolvedIconItemBackedByNativeRegistry"))
                && !Boolean.TRUE.equals(tab.get("fallbackOnlyCreativeVisibility"))
                && !nativeRegistryItems.isEmpty()
                && !outputProofItems.isEmpty()
                && (!searchVisible || !searchOutputProofItems.isEmpty())
                && outputProofItems.containsAll(nativeRegistryItems)
                && (!searchVisible || searchOutputProofItems.containsAll(nativeRegistryItems));
    }

    private static List<String> normalizedContentIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<String> normalized = new java.util.ArrayList<>();
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

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (Object item : iterable) {
            values.add(String.valueOf(item));
        }
        return List.copyOf(values);
    }

    public static int nativeFunctionalItemCount(List<Object> registeredItems, String wrapperClassName) {
        int count = 0;
        for (Object item : registeredItems) {
            if (item != null && wrapperClassName.equals(item.getClass().getName())) {
                count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    public interface NativeItemFactory {
        Object create(
                String itemId,
                Class<?> itemClass,
                Class<?> propertiesClass,
                Object properties
        ) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface NativeBlockFactory {
        Object create(
                String blockId,
                Class<?> blockClass,
                Class<?> blockPropertiesClass,
                Object blockProperties
        ) throws ReflectiveOperationException;
    }
}
