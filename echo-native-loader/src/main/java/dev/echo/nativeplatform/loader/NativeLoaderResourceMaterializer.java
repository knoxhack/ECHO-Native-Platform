package dev.echo.nativeplatform.loader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NativeLoaderResourceMaterializer {
    public static final String SERVICE_ID = "echo.native.resource_materializer";

    private NativeLoaderResourceMaterializer() {
    }

    public static void copyRequiredProductWorldgenSourceEntries(
            Map<String, byte[]> entries,
            List<String> sourceBackedFallbackEntries,
            List<String> nativeRequiredResourceEntries,
            String nativeSourceResourceRootMarker,
            String nativeStructureTemplateSourcePrefix,
            String nativeStructureTemplateTargetPrefix
    ) throws IOException {
        Path sourceRoot = findSourceResourceRoot(nativeSourceResourceRootMarker);
        if (sourceRoot == null || entries == null) {
            return;
        }
        for (String name : nativeRequiredResourceEntries == null ? List.<String>of() : nativeRequiredResourceEntries) {
            if (entries.containsKey(name) || isUnsafeNativeResourcePackEntry(
                    name,
                    nativeStructureTemplateSourcePrefix,
                    nativeStructureTemplateTargetPrefix
            )) {
                continue;
            }
            Path source = sourceRoot.resolve(name).toAbsolutePath().normalize();
            if (Files.isRegularFile(source) && source.startsWith(sourceRoot.toAbsolutePath().normalize())) {
                entries.put(name, Files.readAllBytes(source));
                if (sourceBackedFallbackEntries != null) {
                    sourceBackedFallbackEntries.add(name);
                }
            }
        }
    }

    public static void copyResourceEntries(
            Path jar,
            Map<String, byte[]> entries,
            List<String> safeModeGuardSkippedEntries,
            String nativeStructureTemplateSourcePrefix,
            String nativeStructureTemplateTargetPrefix
    ) throws IOException {
        Set<String> seenInJar = new HashSet<>();
        if (Files.isDirectory(jar)) {
            try (java.util.stream.Stream<Path> stream = Files.walk(jar)) {
                for (Path file : stream.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).toList()) {
                    String name = jar.relativize(file).toString().replace('\\', '/');
                    if ((!name.startsWith("assets/") && !name.startsWith("data/"))
                            || !seenInJar.add(name)
                            || entries.containsKey(name)) {
                        continue;
                    }
                    if (isUnsafeNativeResourcePackEntry(
                            name,
                            nativeStructureTemplateSourcePrefix,
                            nativeStructureTemplateTargetPrefix
                    )) {
                        if (safeModeGuardSkippedEntries != null) {
                            safeModeGuardSkippedEntries.add(name);
                        }
                        continue;
                    }
                    entries.put(name, Files.readAllBytes(file));
                }
            }
            return;
        }
        try (ZipFile input = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> zipEntries = input.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry entry = zipEntries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()
                        || (!name.startsWith("assets/") && !name.startsWith("data/"))
                        || !seenInJar.add(name)
                        || entries.containsKey(name)) {
                    continue;
                }
                if (isUnsafeNativeResourcePackEntry(
                        name,
                        nativeStructureTemplateSourcePrefix,
                        nativeStructureTemplateTargetPrefix
                )) {
                    if (safeModeGuardSkippedEntries != null) {
                        safeModeGuardSkippedEntries.add(name);
                    }
                    continue;
                }
                try (InputStream entryInput = input.getInputStream(entry)) {
                    entries.put(name, entryInput.readAllBytes());
                }
            }
        }
    }

    public static boolean isUnsafeNativeResourcePackEntry(
            String name,
            String nativeStructureTemplateSourcePrefix,
            String nativeStructureTemplateTargetPrefix
    ) {
        if (NativeLoaderWorldgenDatapackPolicy.isUnsafeNativeSaveDatapackEntry(
                name,
                nativeStructureTemplateTargetPrefix
        )) {
            return true;
        }
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String sourcePrefix = nativeStructureTemplateSourcePrefix == null ? "" : nativeStructureTemplateSourcePrefix;
        String targetPrefix = nativeStructureTemplateTargetPrefix == null ? "" : nativeStructureTemplateTargetPrefix;
        return ((!sourcePrefix.isBlank() && name.startsWith(sourcePrefix))
                || (!targetPrefix.isBlank() && name.startsWith(targetPrefix)))
                && (lower.endsWith(".md") || lower.endsWith(".txt") || lower.contains("/readme"));
    }

    private static Path findSourceResourceRoot(String nativeSourceResourceRootMarker) {
        String marker = nativeSourceResourceRootMarker == null ? "" : nativeSourceResourceRootMarker;
        if (marker.isBlank()) {
            return null;
        }
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve("src/main/resources").toAbsolutePath().normalize();
            if (Files.isRegularFile(candidate.resolve(marker))) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}
