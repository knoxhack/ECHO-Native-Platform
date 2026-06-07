package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;
import dev.echo.nativeplatform.loader.NativeLoaderActivationReports;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeBootstrapActivationEnvironment {
    private EchoNativeBootstrapActivationEnvironment() {
    }

    static List<String> runtimeClasspath(String classpathProperty) {
        String classpath = classpathProperty == null ? "" : classpathProperty;
        if (classpath.isBlank()) {
            return List.of();
        }
        List<String> entries = new ArrayList<>();
        for (String item : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!item.isBlank()) {
                entries.add(Path.of(item).toAbsolutePath().normalize().toString());
            }
        }
        return List.copyOf(entries);
    }

    static Path fixtureRoot(Path markerPath, String gameDirProperty) {
        String gameDir = gameDirProperty == null ? "" : gameDirProperty.trim();
        Path fromGameDir = gameDir.isBlank() ? null : fixtureRootFrom(Path.of(gameDir));
        if (fromGameDir != null) {
            return fromGameDir;
        }
        return markerPath == null ? null : fixtureRootFrom(markerPath);
    }

    static ClassLoader moduleClassLoader(String classpathProperty, ClassLoader parent) {
        String classpath = classpathProperty == null ? "" : classpathProperty;
        ClassLoader fallback = parent == null
                ? EchoNativeBootstrapActivationEnvironment.class.getClassLoader()
                : parent;
        List<URL> urls = new ArrayList<>();
        for (String item : runtimeClasspathWithLoader(classpath)) {
            if (item.isBlank()) {
                continue;
            }
            try {
                urls.add(Path.of(item).toAbsolutePath().normalize().toUri().toURL());
            } catch (MalformedURLException ignored) {
                // Invalid classpath entries are ignored; activation will report the missing class.
            }
        }
        return new URLClassLoader(urls.toArray(URL[]::new), fallback);
    }

    private static List<String> runtimeClasspathWithLoader(String classpath) {
        List<String> entries = new ArrayList<>();
        if (classpath != null && !classpath.isBlank()) {
            entries.addAll(runtimeClasspath(classpath));
        }
        String loaderRuntimeJar = System.getProperty("echo.native.loaderRuntimeJar", "").trim();
        if (!loaderRuntimeJar.isBlank()) {
            String normalized = Path.of(loaderRuntimeJar).toAbsolutePath().normalize().toString();
            if (!entries.contains(normalized)) {
                entries.add(normalized);
            }
        }
        return entries;
    }

    static void writeServiceRegistry(
            Path markerPath,
            EchoNativeServiceRegistry serviceRegistry,
            String registryPathProperty,
            JsonWriter writer
    ) {
        if (markerPath == null || serviceRegistry == null || writer == null) {
            return;
        }
        Path registryPath = markerPath.toAbsolutePath().normalize().getParent().resolve("native-service-registry.json");
        if (registryPathProperty != null && !registryPathProperty.isBlank()) {
            System.setProperty(registryPathProperty, registryPath.toString());
        }
        try {
            writer.write(registryPath, serviceRegistryDocument(serviceRegistry));
        } catch (IOException ignored) {
            // The activation marker still carries service state; the file handoff is best-effort for live runtime import.
        }
    }

    private static Path fixtureRootFrom(Path path) {
        Path current = path.toAbsolutePath().normalize();
        if (Files.isRegularFile(current)) {
            current = current.getParent();
        }
        while (current != null) {
            if (Files.isRegularFile(current.resolve("echo.pack.json"))
                    && Files.isDirectory(current.resolve("modules"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static Map<String, Object> serviceRegistryDocument(EchoNativeServiceRegistry serviceRegistry) {
        List<Map<String, Object>> services = serviceRegistry.registeredServices().stream()
                .map(service -> {
                    Object instance = serviceRegistry.service(service.moduleId(), service.serviceId()).orElse(null);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("moduleId", service.moduleId());
                    item.put("serviceId", service.serviceId());
                    item.put("moduleServiceKey", NativeLoaderActivationReports.moduleServiceKey(
                            service.moduleId(),
                            service.serviceId()
                    ));
                    item.put("implementationClass", service.implementationClass());
                    item.put("serviceInstanceAttached", instance != null);
                    item.put("serviceInstanceClass", instance == null ? "" : instance.getClass().getName());
                    item.put("surfaces", service.surfaces());
                    return Map.copyOf(item);
                })
                .toList();
        Map<String, Object> registry = new LinkedHashMap<>();
        registry.put("schema", "echo.native.service_registry.v1");
        registry.put("serviceCount", services.size());
        registry.put("services", services);
        return Map.copyOf(registry);
    }

    @FunctionalInterface
    interface JsonWriter {
        void write(Path path, Object value) throws IOException;
    }
}
