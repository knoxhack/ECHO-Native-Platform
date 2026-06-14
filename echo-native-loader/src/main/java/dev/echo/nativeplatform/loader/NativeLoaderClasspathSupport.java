package dev.echo.nativeplatform.loader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Locale;

public final class NativeLoaderClasspathSupport {
    public static final String SERVICE_ID = "echo.native.classpath_support";

    private NativeLoaderClasspathSupport() {
    }

    public static List<String> nativeContentClasspaths(String nativeModuleClasspathProperty) {
        List<String> entries = new ArrayList<>();
        addClasspathEntries(entries, System.getProperty("java.class.path", ""));
        entries.addAll(nativeModuleClasspathEntries(nativeModuleClasspathProperty));
        return entries.stream().distinct().toList();
    }

    public static String nativeModuleClasspath(String nativeModuleClasspathProperty) {
        return String.join(File.pathSeparator, nativeModuleClasspathEntries(nativeModuleClasspathProperty));
    }

    public static List<String> nativeModuleClasspathEntries(String nativeModuleClasspathProperty) {
        List<String> entries = new ArrayList<>();
        if (nativeModuleClasspathProperty == null || nativeModuleClasspathProperty.isBlank()) {
            return List.of();
        }
        addClasspathEntries(entries, System.getProperty(nativeModuleClasspathProperty, ""));
        addClasspathFileEntries(entries, System.getProperty(nativeModuleClasspathProperty + "File", ""));
        return entries.stream().distinct().toList();
    }

    public static boolean isNativeContentClasspathCandidate(Path path, Collection<String> nativeModuleNamespacePrefixes) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nativeModuleNamespacePrefixes == null) {
            return false;
        }
        for (String prefix : nativeModuleNamespacePrefixes) {
            String safePrefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
            if (!safePrefix.isBlank() && filename.startsWith(safePrefix)) {
                return true;
            }
        }
        return false;
    }

    private static void addClasspathFileEntries(List<String> entries, String classpathFile) {
        if (classpathFile == null || classpathFile.isBlank()) {
            return;
        }
        Path path = Path.of(classpathFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (text.trim().startsWith("{")) {
                Object parsed = NativeLoaderJsonSupport.parse(text);
                if (parsed instanceof Map<?, ?> object) {
                    Object classpathEntries = object.get("classpathEntries");
                    if (classpathEntries instanceof Iterable<?> iterable) {
                        for (Object item : iterable) {
                            if (item != null && !item.toString().isBlank()) {
                                entries.add(Path.of(item.toString()).toAbsolutePath().normalize().toString());
                            }
                        }
                    }
                    Object moduleClasspath = object.get("moduleClasspath");
                    if (moduleClasspath instanceof String classpath) {
                        addClasspathEntries(entries, classpath);
                    }
                    return;
                }
            }
            for (String item : text.split("\\R")) {
                if (!item.isBlank()) {
                    entries.add(Path.of(item).toAbsolutePath().normalize().toString());
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // The inline classpath property remains the compatibility fallback.
        }
    }

    private static void addClasspathEntries(List<String> entries, String classpath) {
        if (classpath == null || classpath.isBlank()) {
            return;
        }
        for (String item : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!item.isBlank()) {
                entries.add(item);
            }
        }
    }
}
