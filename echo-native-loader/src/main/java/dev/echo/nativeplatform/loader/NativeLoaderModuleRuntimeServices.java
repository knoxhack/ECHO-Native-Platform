package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class NativeLoaderModuleRuntimeServices {
    public static final String SERVICE_ID = "echo.native.module_runtime_services";

    private NativeLoaderModuleRuntimeServices() {
    }

    public static void invoke(Context context, String namespace, Map<String, Object> report, Object level, Object player) {
        String ns = lowerContentId(namespace);
        report.put("nativeModuleRuntimeServicesId", SERVICE_ID);
        for (String className : moduleServiceClasses(context, ns)) {
            NativeLoaderRuntimeReflectionSupport.invokeStaticNoArg(report, serviceKey(className, "register"), className, "register");
            NativeLoaderRuntimeReflectionSupport.invokeStaticNoArg(report, serviceKey(className, "registerWhenReady"), className, "registerWhenReady");
            NativeLoaderRuntimeReflectionSupport.invokeStaticNoArg(report, serviceKey(className, "ensureDefaults"), className, "ensureDefaults");
            NativeLoaderRuntimeReflectionSupport.invokeStaticNoArg(report, serviceKey(className, "registerBuiltins"), className, "registerBuiltins");
        }
        switch (ns) {
            case "echoindex" -> invokeProductHooks(context, report, "index_provider");
            case "echolens" -> invokeProductHooks(context, report, "lens_integration");
            default -> {
                // Core service classes above and module-specific runtime dispatch below handle the rest.
            }
        }
        long gameTime = gameTime(level);
        switch (ns) {
            case "echoweathercore" -> {
                Object weatherState = NativeLoaderRuntimeReflectionSupport.invokeStaticNoArgValue(
                        report,
                        "weatherStateManager",
                        "com.knoxhack.echoweathercore.server.WeatherStateManager",
                        "getInstance"
                );
                if (level != null) {
                    NativeLoaderRuntimeReflectionSupport.invokeMethodValue(
                            report,
                            "weatherStateTickLevel",
                            weatherState,
                            "tickLevel",
                            new Class<?>[]{tryClass(context.runtimeClass().apply("world.level.Level"))},
                            level
                    );
                }
                NativeLoaderRuntimeReflectionSupport.invokeStaticNoArgValue(
                        report,
                        "weatherForecastManager",
                        "com.knoxhack.echoweathercore.server.WeatherForecastManager",
                        "getInstance"
                );
                NativeLoaderRuntimeReflectionSupport.invokeStaticNoArgValue(
                        report,
                        "weatherWarningManager",
                        "com.knoxhack.echoweathercore.server.WeatherWarningManager",
                        "getInstance"
                );
            }
            case "echoworldcore" -> {
                Object service = NativeLoaderRuntimeReflectionSupport.staticFieldValue(
                        "com.knoxhack.echoworldcore.service.WorldRegionService",
                        "INSTANCE"
                );
                NativeLoaderRuntimeReflectionSupport.invokeMethodValue(
                        report,
                        "worldRegionServiceProviderCount",
                        service,
                        "providerCount",
                        new Class<?>[0]
                );
                NativeLoaderRuntimeReflectionSupport.invokeMethodValue(
                        report,
                        "worldRegionTickPlayer",
                        service,
                        "tickPlayer",
                        new Class<?>[]{tryClass(context.runtimeClass().apply("server.level.ServerPlayer"))},
                        player
                );
            }
            case "echoatmospherecore" -> invokeStaticMethodValue(report, "atmosphereLevelTick",
                    "com.knoxhack.echo.atmospherecore.EchoAtmosphereRuntimeState", "materializeLevelTick",
                    new Class<?>[]{long.class, String.class}, gameTime, "echo_native_gameplay");
            case "echobiomecore" -> invokeStaticMethodValue(report, "biomeLevelTick",
                    "com.knoxhack.echo.biomecore.EchoBiomeRuntimeState", "materializeLevelTick",
                    new Class<?>[]{long.class, String.class}, gameTime, "echo_native_gameplay");
            case "echostructurecore" -> invokeStaticMethodValue(report, "structureLevelTick",
                    "com.knoxhack.echo.structurecore.EchoStructureRuntimeState", "materializeLevelTick",
                    new Class<?>[]{long.class, String.class}, gameTime, "echo_native_gameplay");
            case "echostatuscore" -> invokeStaticMethodValue(report, "statusRegistry",
                    "com.knoxhack.echo.statuscore.EchoStatusRuntimeState", "materializeServerRegistry",
                    new Class<?>[]{String.class}, "echo_native_gameplay");
            case "echodifficultycore" -> invokeStaticMethodValue(report, "difficultyPolicy",
                    "com.knoxhack.echo.difficultycore.EchoDifficultyRuntimeState", "materializeServerPolicy",
                    new Class<?>[]{String.class}, "echo_native_gameplay");
            default -> {
                // Native module activation and info-module routes cover other namespaces.
            }
        }
    }

    private static int invokeProductHooks(Context context, Map<String, Object> report, String... roles) {
        int invoked = 0;
        for (String role : roles) {
            if (context.productHookInvoker().invoke(report, role)) {
                invoked++;
            }
        }
        return invoked;
    }

    private static Object invokeStaticMethodValue(
            Map<String, Object> report,
            String key,
            String className,
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) {
        return NativeLoaderRuntimeReflectionSupport.invokeStaticMethodValue(
                report,
                key,
                className,
                methodName,
                parameterTypes,
                args
        );
    }

    private static List<String> moduleServiceClasses(Context context, String namespace) {
        return context.profile().nativeModuleServiceClasses().getOrDefault(lowerContentId(namespace), List.of());
    }

    private static Class<?> tryClass(String className) {
        return NativeLoaderRuntimeReflectionSupport.tryClass(className);
    }

    private static String serviceKey(String className, String methodName) {
        String safe = (className + "." + methodName).replaceAll("[^A-Za-z0-9_]+", "_");
        return safe.length() <= 80 ? safe : safe.substring(Math.max(0, safe.length() - 80));
    }

    private static long gameTime(Object level) {
        Object value = NativeLoaderClientReflectionSupport.optionalMethodValue(level, "getGameTime");
        return value instanceof Number number ? number.longValue() : System.currentTimeMillis() / 50L;
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record Context(
            EchoNativeBootstrapProductProfile profile,
            Function<String, String> runtimeClass,
            NativeLoaderInfoModuleRuntimeInvoker.ProductHookInvoker productHookInvoker
    ) {
    }
}
