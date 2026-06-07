package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NativeLoaderProductGameplayContentDiscovery {
    public static final String SERVICE_ID = "echo.native.product_gameplay_content_discovery";

    private final EchoNativeBootstrapProductProfile profile;
    private final String nativeModuleClasspathProperty;

    public NativeLoaderProductGameplayContentDiscovery(
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty
    ) {
        this.profile = profile;
        this.nativeModuleClasspathProperty = nativeModuleClasspathProperty == null ? "" : nativeModuleClasspathProperty;
    }

    public Map<String, List<String>> discover() throws IOException {
        Set<String> missions = new TreeSet<>();
        Set<String> worldRegions = new TreeSet<>();
        Set<String> progressionAdvancements = new TreeSet<>();
        Set<String> hazardBiomeTags = new TreeSet<>();
        discoverClasspath(System.getProperty("java.class.path", ""), missions, worldRegions, progressionAdvancements, hazardBiomeTags);
        discoverClasspath(System.getProperty(nativeModuleClasspathProperty, ""), missions, worldRegions, progressionAdvancements, hazardBiomeTags);

        Map<String, List<String>> content = new LinkedHashMap<>();
        content.put("missions", List.copyOf(missions));
        content.put("worldRegions", List.copyOf(worldRegions));
        content.put("progressionAdvancements", List.copyOf(progressionAdvancements));
        content.put("hazardBiomeTags", List.copyOf(hazardBiomeTags));
        return content;
    }

    private void discoverClasspath(
            String classpath,
            Set<String> missions,
            Set<String> worldRegions,
            Set<String> progressionAdvancements,
            Set<String> hazardBiomeTags
    ) {
        for (String item : classpath.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (item.isBlank()) {
                continue;
            }
            Path path = Path.of(item);
            if (!isNativeContentClasspathCandidate(path) || !Files.isRegularFile(path)) {
                continue;
            }
            discoverIfReadable(path, missions, worldRegions, progressionAdvancements, hazardBiomeTags);
        }
    }

    private void discover(
            Path jar,
            Set<String> missions,
            Set<String> worldRegions,
            Set<String> progressionAdvancements,
            Set<String> hazardBiomeTags
    ) throws IOException {
        try (ZipFile input = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.endsWith(".json") || !name.startsWith(productDataPath(""))) {
                    continue;
                }
                String missionsPrefix = contentDataPrefix("missions");
                String worldRegionsPrefix = contentDataPrefix("world_regions");
                String progressionPrefix = contentDataPrefix("progression_advancements");
                String hazardBiomePrefix = contentDataPrefix("hazard_biome_tags");
                if (!missionsPrefix.isBlank() && name.startsWith(missionsPrefix)) {
                    addDataId(missions, name, missionsPrefix);
                } else if (!worldRegionsPrefix.isBlank() && name.startsWith(worldRegionsPrefix)) {
                    addDataId(worldRegions, name, worldRegionsPrefix);
                } else if (!progressionPrefix.isBlank() && name.startsWith(progressionPrefix)) {
                    addDataId(progressionAdvancements, name, progressionPrefix);
                } else if (!hazardBiomePrefix.isBlank()
                        && name.startsWith(hazardBiomePrefix)
                        && name.contains("hazard")) {
                    addDataId(hazardBiomeTags, name, hazardBiomePrefix);
                }
            }
        }
    }

    private void discoverIfReadable(
            Path jar,
            Set<String> missions,
            Set<String> worldRegions,
            Set<String> progressionAdvancements,
            Set<String> hazardBiomeTags
    ) {
        try {
            discover(jar, missions, worldRegions, progressionAdvancements, hazardBiomeTags);
        } catch (IOException ignored) {
            // One malformed or stale addon jar must not disable the full AdapterCore gameplay bridge.
        }
    }

    private void addDataId(Set<String> ids, String name, String prefix) {
        String path = name.substring(prefix.length(), name.length() - ".json".length());
        if (!path.isBlank() && isValidContentPath(path)) {
            ids.add(profile.id(path));
        }
    }

    private boolean isNativeContentClasspathCandidate(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String prefix : profile.nativeModuleNamespacePrefixes()) {
            String safePrefix = lowerContentId(prefix);
            if (!safePrefix.isBlank() && filename.startsWith(safePrefix)) {
                return true;
            }
        }
        return false;
    }

    private String productDataPath(String path) {
        if (path == null || path.isBlank()) {
            return "data/" + profile.namespace() + "/";
        }
        return "data/" + profile.namespace() + "/" + path;
    }

    private String contentDataPrefix(String key) {
        String prefix = profile.nativeGameplayContentDataPrefixes().get(lowerContentId(key));
        return prefix == null ? "" : prefix;
    }

    private static boolean isValidContentPath(String value) {
        return value.matches("[a-z0-9_./-]+") && !value.contains("//") && !value.startsWith("/") && !value.endsWith("/");
    }

    private static String lowerContentId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
