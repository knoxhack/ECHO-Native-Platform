package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.loader.NativeLoaderClientReflectionSupport;

final class EchoNativeBootstrapMinecraftRuntimeFlow {
    private final String moduleClasspathProperty;
    private final Class<?> bootstrapClass;

    EchoNativeBootstrapMinecraftRuntimeFlow(String moduleClasspathProperty, Class<?> bootstrapClass) {
        this.moduleClasspathProperty = moduleClasspathProperty == null ? "" : moduleClasspathProperty;
        this.bootstrapClass = bootstrapClass;
    }

    String runtimeClass(String suffix) {
        return "net." + "minecraft." + suffix;
    }

    boolean ensureVanillaBootstrap() {
        try {
            Class.forName(runtimeClass("SharedConstants")).getMethod("tryDetectVersion").invoke(null);
            Class.forName(runtimeClass("server.Bootstrap")).getMethod("bootStrap").invoke(null);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    Object interactionResult(String name) {
        try {
            return Class.forName(runtimeClass("world.InteractionResult")).getField(name).get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    boolean isClientSideLevel(Object level) {
        if (level == null) {
            return false;
        }
        Object methodValue = NativeLoaderClientReflectionSupport.optionalMethodValue(level, "isClientSide");
        if (methodValue instanceof Boolean value) {
            return value;
        }
        try {
            Object fieldValue = NativeLoaderClientReflectionSupport.optionalFieldValue(level, "isClientSide");
            return Boolean.TRUE.equals(fieldValue);
        } catch (Throwable ignored) {
            return false;
        }
    }

    Object registryValue(String registryField, String contentId) throws ReflectiveOperationException {
        Class<?> builtInRegistriesClass = Class.forName(runtimeClass("core.registries.BuiltInRegistries"));
        Object registry = builtInRegistriesClass.getField(registryField).get(null);
        Object identifier = nativeIdentifier(contentId);
        return registry.getClass().getMethod("getValue", identifier.getClass()).invoke(registry, identifier);
    }

    int intMethod(Object target, String methodName) throws ReflectiveOperationException {
        Object value = target.getClass().getMethod(methodName).invoke(target);
        return value instanceof Number number ? number.intValue() : 0;
    }

    ClassLoader nativeModuleClassLoader() {
        return EchoNativeBootstrapActivationEnvironment.moduleClassLoader(
                System.getProperty(moduleClasspathProperty, ""),
                bootstrapClass.getClassLoader()
        );
    }

    private Object nativeIdentifier(String contentId) throws ReflectiveOperationException {
        Class<?> identifierClass = Class.forName(runtimeClass("resources.Identifier"));
        String[] parts = splitContentId(contentId);
        return identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                .invoke(null, parts[0], parts[1]);
    }

    private static String[] splitContentId(String contentId) {
        String value = contentId == null ? "" : contentId.trim();
        int colon = value.indexOf(':');
        if (colon <= 0 || colon >= value.length() - 1) {
            return new String[]{"minecraft", value.isBlank() ? "air" : value};
        }
        return new String[]{value.substring(0, colon), value.substring(colon + 1)};
    }
}
