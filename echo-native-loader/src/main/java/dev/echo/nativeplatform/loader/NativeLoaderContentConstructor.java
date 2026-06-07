package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBlockConstructorBinding;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeItemConstructorBinding;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class NativeLoaderContentConstructor {
    public static final String SERVICE_ID = "echo.native.content_constructor";

    private NativeLoaderContentConstructor() {
    }

    public static Object newItem(
            String itemId,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties,
            Context context
    ) throws ReflectiveOperationException {
        if (isEchoNamespace(namespaceOf(itemId), context) || context.fallbackResolver().requiresItemShim(itemId)) {
            try {
                return NativeLoaderGeneratedContentBridge.newItem(
                        itemId,
                        itemClass,
                        propertiesClass,
                        properties,
                        context.generatedContentBridgeConfig().get()
                );
            } catch (Throwable ignored) {
                // Fall through to real-source construction only if the generated native shim cannot compile.
            }
        }
        Object realItem = newRealItem(itemId, itemClass, propertiesClass, properties, context);
        if (context.fallbackResolver().prefersRealItem(itemId) && realItem != null && neoforgeRuntimeAvailable()) {
            return realItem;
        }
        if (realItem != null && isEchoNamespace(namespaceOf(itemId), context) && !neoforgeRuntimeAvailable()) {
            return itemClass.getConstructor(propertiesClass).newInstance(properties);
        }
        if (realItem != null) {
            return realItem;
        }
        return itemClass.getConstructor(propertiesClass).newInstance(properties);
    }

    public static Object newBlock(
            String blockId,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            Context context
    ) throws ReflectiveOperationException {
        if (isEchoNamespace(namespaceOf(blockId), context) || context.fallbackResolver().requiresBlockShim(blockId)) {
            try {
                return NativeLoaderGeneratedContentBridge.newBlock(
                        blockId,
                        blockClass,
                        blockPropertiesClass,
                        blockProperties,
                        context.generatedContentBridgeConfig().get()
                );
            } catch (Throwable ignored) {
                // Fall through to real-source construction only if the generated native shim cannot compile.
            }
        }
        Object realBlock = newRealBlock(blockId, blockClass, blockPropertiesClass, blockProperties, context);
        if (realBlock != null
                && (isActiveProductNamespace(namespaceOf(blockId), context)
                || context.fallbackResolver().prefersRealBlock(blockId))
                && neoforgeRuntimeAvailable()) {
            return realBlock;
        }
        if (realBlock != null && isEchoNamespace(namespaceOf(blockId), context) && !neoforgeRuntimeAvailable()) {
            return blockClass.getConstructor(blockPropertiesClass).newInstance(blockProperties);
        }
        if (realBlock != null) {
            return realBlock;
        }
        return blockClass.getConstructor(blockPropertiesClass).newInstance(blockProperties);
    }

    private static Object newRealItem(
            String itemId,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties,
            Context context
    ) {
        String namespace = namespaceOf(lowerContentId(itemId));
        String path = pathOf(lowerContentId(itemId));
        try {
            Object genericItem = constructGenericItem(namespace, path, itemClass, propertiesClass, properties, context);
            if (genericItem != null && !lowerContentId(context.profile().namespace()).equals(namespace)) {
                return genericItem;
            }
            Object productItem = constructProductProfileItem(
                    namespace,
                    path,
                    itemClass,
                    propertiesClass,
                    properties,
                    context);
            if (productItem != null) {
                return productItem;
            }
            if (!lowerContentId(context.profile().namespace()).equals(namespace)) {
                return null;
            }
            return constructGenericItem(namespace, path, itemClass, propertiesClass, properties, context);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object newRealBlock(
            String blockId,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            Context context
    ) {
        String namespace = namespaceOf(lowerContentId(blockId));
        String path = pathOf(lowerContentId(blockId));
        try {
            if (path.endsWith("_slab")) {
                return constructBlockWithProperties(
                        context.runtimeClass().apply("world.level.block.SlabBlock"),
                        blockClass,
                        blockPropertiesClass,
                        blockProperties);
            }
            if (path.endsWith("_wall")) {
                return constructBlockWithProperties(
                        context.runtimeClass().apply("world.level.block.WallBlock"),
                        blockClass,
                        blockPropertiesClass,
                        blockProperties);
            }
            if (path.endsWith("_stairs")) {
                return constructStairBlock(blockClass, blockPropertiesClass, blockProperties, context);
            }
            if ("echoterminal".equals(namespace) && "echo_terminal".equals(path)) {
                return constructBlockWithProperties(
                        "com.knoxhack.echoterminal.block.EchoTerminalBlock",
                        blockClass,
                        blockPropertiesClass,
                        blockProperties);
            }
            if ("signalos".equals(namespace)) {
                return switch (path) {
                    case "terminal", "workstation" -> constructBlockWithProperties(
                            "com.knoxhack.signalos.block.SignalOsTerminalBlock",
                            blockClass,
                            blockPropertiesClass,
                            blockProperties);
                    case "server_rack" -> constructBlockWithProperties(
                            "com.knoxhack.signalos.block.SignalOsServerRackBlock",
                            blockClass,
                            blockPropertiesClass,
                            blockProperties);
                    case "network_relay" -> constructBlockWithProperties(
                            "com.knoxhack.signalos.block.SignalOsNetworkRelayBlock",
                            blockClass,
                            blockPropertiesClass,
                            blockProperties);
                    default -> null;
                };
            }
            Object productBlock = constructProductProfileBlock(
                    namespace,
                    path,
                    blockClass,
                    blockPropertiesClass,
                    blockProperties,
                    context);
            if (productBlock != null) {
                return productBlock;
            }
            return constructGenericBlock(namespace, path, blockClass, blockPropertiesClass, blockProperties, context);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object constructProductProfileItem(
            String namespace,
            String path,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties,
            Context context
    ) throws ReflectiveOperationException {
        String id = lowerContentId(namespace) + ":" + lowerContentId(path);
        for (NativeItemConstructorBinding binding : context.profile().nativeItemConstructorBindings()) {
            if (binding == null || !id.equals(lowerContentId(binding.id()))) {
                continue;
            }
            String enumClassName = binding.enumClassName() == null ? "" : binding.enumClassName().trim();
            String enumName = binding.enumName() == null ? "" : binding.enumName().trim();
            if (!enumClassName.isBlank() && !enumName.isBlank()) {
                return constructItemWithEnum(
                        binding.className(),
                        itemClass,
                        propertiesClass,
                        properties,
                        productRuntimeClassName(enumClassName, context),
                        enumName);
            }
            return constructItemWithProperties(binding.className(), itemClass, propertiesClass, properties);
        }
        return null;
    }

    private static Object constructProductProfileBlock(
            String namespace,
            String path,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            Context context
    ) throws ReflectiveOperationException {
        String id = lowerContentId(namespace) + ":" + lowerContentId(path);
        for (NativeBlockConstructorBinding binding : context.profile().nativeBlockConstructorBindings()) {
            if (binding == null || !id.equals(lowerContentId(binding.id()))) {
                continue;
            }
            String className = productRuntimeClassName(binding.className(), context);
            return switch (binding.constructorKind() == null ? "" : binding.constructorKind().trim()) {
                case "properties_int_int" -> constructBlockWithPropertiesAndInts(
                        className,
                        blockClass,
                        blockPropertiesClass,
                        blockProperties,
                        binding.intParamOne(),
                        binding.intParamTwo());
                default -> constructBlockWithProperties(
                        className,
                        blockClass,
                        blockPropertiesClass,
                        blockProperties);
            };
        }
        return null;
    }

    private static Object constructGenericItem(
            String namespace,
            String path,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties,
            Context context
    ) {
        if (!isEchoNamespace(namespace, context)) {
            return null;
        }
        String basePackage = "signalos".equals(namespace) ? "com.knoxhack.signalos" : "com.knoxhack." + namespace;
        return constructFirstItemClass(
                List.of(basePackage + ".item." + pascalCase(path) + "Item"),
                itemClass,
                propertiesClass,
                properties);
    }

    private static Object constructGenericBlock(
            String namespace,
            String path,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            Context context
    ) {
        if (!isEchoNamespace(namespace, context)) {
            return null;
        }
        String basePackage = "signalos".equals(namespace) ? "com.knoxhack.signalos" : "com.knoxhack." + namespace;
        return constructFirstBlockClass(
                List.of(basePackage + ".block." + pascalCase(path) + "Block"),
                blockClass,
                blockPropertiesClass,
                blockProperties);
    }

    private static Object constructFirstItemClass(
            List<String> classNames,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties
    ) {
        for (String className : classNames) {
            try {
                Object item = constructItemWithProperties(className, itemClass, propertiesClass, properties);
                if (item != null) {
                    return item;
                }
            } catch (Throwable ignored) {
                // Try the next discovered module class candidate.
            }
        }
        return null;
    }

    private static Object constructFirstBlockClass(
            List<String> classNames,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties
    ) {
        for (String className : classNames) {
            try {
                Object block = constructBlockWithProperties(className, blockClass, blockPropertiesClass, blockProperties);
                if (block != null) {
                    return block;
                }
            } catch (Throwable ignored) {
                // Try the next discovered module class candidate.
            }
        }
        return null;
    }

    private static Object constructItemWithProperties(
            String className,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties
    ) throws ReflectiveOperationException {
        Class<?> concrete = Class.forName(className, false, NativeLoaderContentConstructor.class.getClassLoader());
        if (!itemClass.isAssignableFrom(concrete)) {
            return null;
        }
        return concrete.getConstructor(propertiesClass).newInstance(properties);
    }

    private static Object constructItemWithEnum(
            String className,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties,
            String enumClassName,
            String enumName
    ) throws ReflectiveOperationException {
        Class<?> concrete = Class.forName(className, false, NativeLoaderContentConstructor.class.getClassLoader());
        if (!itemClass.isAssignableFrom(concrete)) {
            return null;
        }
        Object enumValue = enumConstant(enumClassName, enumName);
        return concrete.getConstructor(propertiesClass, enumValue.getClass()).newInstance(properties, enumValue);
    }

    private static Object constructBlockWithProperties(
            String className,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties
    ) throws ReflectiveOperationException {
        Class<?> concrete = Class.forName(className, false, NativeLoaderContentConstructor.class.getClassLoader());
        if (!blockClass.isAssignableFrom(concrete)) {
            return null;
        }
        return concrete.getConstructor(blockPropertiesClass).newInstance(blockProperties);
    }

    private static Object constructBlockWithPropertiesAndInts(
            String className,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            int first,
            int second
    ) throws ReflectiveOperationException {
        Class<?> concrete = Class.forName(className, false, NativeLoaderContentConstructor.class.getClassLoader());
        if (!blockClass.isAssignableFrom(concrete)) {
            return null;
        }
        return concrete.getConstructor(blockPropertiesClass, int.class, int.class)
                .newInstance(blockProperties, first, second);
    }

    private static Object constructStairBlock(
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            Context context
    ) throws ReflectiveOperationException {
        Class<?> concrete = Class.forName(context.runtimeClass().apply("world.level.block.StairBlock"), false,
                NativeLoaderContentConstructor.class.getClassLoader());
        if (!blockClass.isAssignableFrom(concrete)) {
            return null;
        }
        Class<?> blocksClass = Class.forName(context.runtimeClass().apply("world.level.block.Blocks"));
        Object baseBlock = blocksClass.getField("STONE").get(null);
        Object baseState = blockClass.getMethod("defaultBlockState").invoke(baseBlock);
        Class<?> blockStateClass = Class.forName(context.runtimeClass().apply("world.level.block.state.BlockState"));
        return concrete.getConstructor(blockStateClass, blockPropertiesClass)
                .newInstance(baseState, blockProperties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(String enumClassName, String enumName) throws ReflectiveOperationException {
        Class<? extends Enum> enumClass = Class.forName(enumClassName)
                .asSubclass(Enum.class);
        return Enum.valueOf(enumClass, enumName);
    }

    private static boolean neoforgeRuntimeAvailable() {
        try {
            Class.forName("net.neoforged.neoforge.registries.NeoForgeRegistries");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isActiveProductNamespace(String namespace, Context context) {
        return lowerContentId(context.profile().namespace()).equals(lowerContentId(namespace));
    }

    private static boolean isEchoNamespace(String namespace, Context context) {
        return context.fallbackResolver().isActiveNamespace(namespace);
    }

    private static String productRuntimeClassName(String className, Context context) {
        String safeClassName = className == null ? "" : className.trim();
        if (safeClassName.startsWith("runtime:")) {
            return context.runtimeClass().apply(safeClassName.substring("runtime:".length()));
        }
        return safeClassName;
    }

    private static String pascalCase(String path) {
        StringBuilder builder = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '_' || ch == '-' || ch == ' ') {
                upper = true;
            } else if (upper) {
                builder.append(Character.toUpperCase(ch));
                upper = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String namespaceOf(String contentId) {
        String[] parts = splitContentId(contentId);
        return lowerContentId(parts[0]);
    }

    private static String pathOf(String contentId) {
        String[] parts = splitContentId(contentId);
        return lowerContentId(parts[1]);
    }

    private static String[] splitContentId(String contentId) {
        String value = contentId == null ? "" : contentId.trim();
        int colon = value.indexOf(':');
        if (colon <= 0 || colon >= value.length() - 1) {
            return new String[]{"minecraft", value.isBlank() ? "air" : value};
        }
        return new String[]{value.substring(0, colon), value.substring(colon + 1)};
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record Context(
            EchoNativeBootstrapProductProfile profile,
            NativeLoaderContentFallbackResolver fallbackResolver,
            Function<String, String> runtimeClass,
            Supplier<NativeLoaderGeneratedContentBridge.Config> generatedContentBridgeConfig
    ) {
        public Context {
        }
    }
}
