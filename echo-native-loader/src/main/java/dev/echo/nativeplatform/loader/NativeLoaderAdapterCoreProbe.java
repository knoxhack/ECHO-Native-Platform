package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class NativeLoaderAdapterCoreProbe {
    public static final String SERVICE_ID = "echo.native.adaptercore_probe";

    private NativeLoaderAdapterCoreProbe() {
    }

    public static Map<String, Object> probe(Supplier<ClassLoader> moduleClassLoader) {
        Map<String, Object> probe = new LinkedHashMap<>();
        probe.put("nativeAdapterCoreProbeServiceId", SERVICE_ID);
        probe.put("moduleId", "echoadaptercore");
        probe.put("descriptorClass", "com.knoxhack.echo.adaptercore.EchoNativeAdapterDescriptor");
        probe.put("probeAttempted", true);
        probe.put("source", "adaptercore_reflection");
        probe.put("addonCodeExecuted", false);
        try {
            Class<?> descriptor = descriptorClass(moduleClassLoader);
            Object adapter = descriptor.getMethod("adapter").invoke(null);
            Object capabilities = invoke(adapter, "capabilities");
            probe.put("available", true);
            probe.put("classLoader", descriptor.getClassLoader() == NativeLoaderAdapterCoreProbe.class.getClassLoader()
                    ? "loader_classpath"
                    : "native_module_classpath");
            probe.put("id", text(invoke(adapter, "id")));
            probe.put("kind", serialized(invoke(adapter, "kind")));
            probe.put("runtime", serialized(invoke(adapter, "runtime")));
            probe.put("status", serialized(invoke(adapter, "status")));
            probe.put("displayName", text(invoke(adapter, "displayName")));
            probe.put("nativeLoaderSupported", bool(invoke(adapter, "nativeLoaderSupported")));
            probe.put("nativeClasspath", bool(invoke(capabilities, "nativeClasspath")));
            probe.put("nativePackOsBootstrap", bool(invoke(capabilities, "nativePackOsBootstrap")));
            probe.put("nativeTransformPipeline", bool(invoke(capabilities, "nativeTransformPipeline")));
            probe.put("summary", "AdapterCore native adapter descriptor was discovered in the live Native Loader process.");
        } catch (Throwable exception) {
            probe.put("available", false);
            probe.put("failureKind", exception.getClass().getSimpleName());
            probe.put("summary", "AdapterCore native adapter descriptor was not available to the live Native Loader process.");
        }
        return Map.copyOf(probe);
    }

    private static Class<?> descriptorClass(Supplier<ClassLoader> moduleClassLoader) throws ClassNotFoundException {
        try {
            return Class.forName("com.knoxhack.echo.adaptercore.EchoNativeAdapterDescriptor");
        } catch (ClassNotFoundException exception) {
            return Class.forName(
                    "com.knoxhack.echo.adaptercore.EchoNativeAdapterDescriptor",
                    true,
                    moduleClassLoader.get());
        }
    }

    private static Object invoke(Object target, String method) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        return target.getClass().getMethod(method).invoke(target);
    }

    private static String serialized(Object value) {
        if (value == null) {
            return "";
        }
        try {
            Object serialized = value.getClass().getMethod("serializedName").invoke(value);
            return text(serialized);
        } catch (ReflectiveOperationException exception) {
            return text(value);
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }
}
