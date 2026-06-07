package dev.echo.nativeplatform.loader;

public final class NativeLoaderGeneratedContentBridge {
    public static final String ITEM_CLASS_NAME = "dev.echo.nativeplatform.generated.EchoNativeBetaItem";
    public static final String BLOCK_CLASS_NAME = "dev.echo.nativeplatform.generated.EchoNativeBetaBlock";

    private NativeLoaderGeneratedContentBridge() {
    }

    public static boolean itemWrapperAvailable() {
        return classAvailable(ITEM_CLASS_NAME);
    }

    public static boolean blockWrapperAvailable() {
        return classAvailable(BLOCK_CLASS_NAME);
    }

    public static Object newItem(
            String itemId,
            Class<?> itemClass,
            Class<?> propertiesClass,
            Object properties,
            Config config
    ) throws ReflectiveOperationException {
        if (itemWrapperAvailable()) {
            Class<?> wrapper = Class.forName(ITEM_CLASS_NAME, true, classLoader(config));
            try {
                return wrapper.getConstructor(String.class, itemClass, propertiesClass)
                        .newInstance(itemId, itemClass, properties);
            } catch (NoSuchMethodException ignored) {
                // Fall through to the vanilla constructor when no generated shim constructor matches this snapshot.
            }
        }
        return itemClass.getConstructor(propertiesClass).newInstance(properties);
    }

    public static Object newBlock(
            String blockId,
            Class<?> blockClass,
            Class<?> blockPropertiesClass,
            Object blockProperties,
            Config config
    ) throws ReflectiveOperationException {
        if (blockWrapperAvailable()) {
            Class<?> wrapper = Class.forName(BLOCK_CLASS_NAME, true, classLoader(config));
            try {
                return wrapper.getConstructor(String.class, blockClass, blockPropertiesClass)
                        .newInstance(blockId, blockClass, blockProperties);
            } catch (NoSuchMethodException ignored) {
                // Fall through to the vanilla constructor when no generated shim constructor matches this snapshot.
            }
        }
        return blockClass.getConstructor(blockPropertiesClass).newInstance(blockProperties);
    }

    private static boolean classAvailable(String className) {
        try {
            Class.forName(className, false, NativeLoaderGeneratedContentBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static ClassLoader classLoader(Config config) {
        Class<?> anchor = config == null ? null : config.bootstrapClass();
        return anchor == null ? NativeLoaderGeneratedContentBridge.class.getClassLoader() : anchor.getClassLoader();
    }

    public record Config(String nativeModuleClasspathProperty, Class<?> bootstrapClass) {
        public Config {
            nativeModuleClasspathProperty = nativeModuleClasspathProperty == null ? "" : nativeModuleClasspathProperty;
            bootstrapClass = bootstrapClass == null ? NativeLoaderGeneratedContentBridge.class : bootstrapClass;
        }
    }
}
