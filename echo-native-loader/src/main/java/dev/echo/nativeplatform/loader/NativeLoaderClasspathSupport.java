package dev.echo.nativeplatform.loader;

import java.io.File;
import java.util.ArrayList;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class NativeLoaderClasspathSupport {
    public static final String SERVICE_ID = "echo.native.classpath_support";

    private NativeLoaderClasspathSupport() {
    }

    public static List<String> nativeContentClasspaths(String nativeModuleClasspathProperty) {
        List<String> entries = new ArrayList<>();
        addClasspathEntries(entries, System.getProperty("java.class.path", ""));
        String moduleClasspath = nativeModuleClasspathProperty == null || nativeModuleClasspathProperty.isBlank()
                ? ""
                : System.getProperty(nativeModuleClasspathProperty, "");
        addClasspathEntries(entries, moduleClasspath);
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
