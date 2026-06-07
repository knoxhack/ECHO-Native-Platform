package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class EchoNativeLockfileGenerator {
    public EchoNativeLockfilePlan generate(
            EchoNativePackProfile profile,
            List<EchoNativeAddonDescriptor> descriptors,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (profile == null) {
            return new EchoNativeLockfilePlan("", Map.of(
                    "dryRunOnly", true,
                    "lockedModules", List.of(),
                    "phase", "phase12_packos_dry_run"
            ), List.copyOf(diagnostics));
        }

        List<EchoNativeAddonDescriptor> sortedDescriptors = descriptors.stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id)
                        .thenComparing(descriptor -> safePath(descriptor.descriptorPath())))
                .toList();

        Map<String, Set<String>> featureProviders = new TreeMap<>();
        for (EchoNativeAddonDescriptor descriptor : sortedDescriptors) {
            for (String feature : descriptor.provides()) {
                featureProviders.computeIfAbsent(feature, ignored -> new TreeSet<>()).add(descriptor.id());
            }
        }

        Map<String, Object> lockfile = new LinkedHashMap<>();
        lockfile.put("schema", "echo.native.lockfile.v1");
        lockfile.put("packId", profile.id());
        lockfile.put("packName", profile.name());
        lockfile.put("packStatus", profile.status());
        lockfile.put("minecraftVersion", profile.minecraftVersion());
        lockfile.put("loaderKind", profile.loaderKind());
        lockfile.put("loaderVersion", profile.loaderVersion());
        lockfile.put("rootModule", profile.rootModule());
        lockfile.put("dryRunOnly", true);
        lockfile.put("phase", "phase12_packos_dry_run");
        lockfile.put("moduleLoadOrder", loadOrder(profile, sortedDescriptors));
        lockfile.put("lockedModules", lockedModules(profile, sortedDescriptors));
        lockfile.put("lockedFeatures", lockedFeatures(profile, featureProviders));
        lockfile.put("checksums", Map.of(
                "mode", "descriptor_metadata",
                "mutationAllowed", false
        ));
        lockfile.put("safety", Map.of(
                "downloadAllowed", false,
                "installMutationAllowed", false,
                "repairExecutionAllowed", false
        ));
        return new EchoNativeLockfilePlan(profile.id(), lockfile, List.copyOf(diagnostics));
    }

    private static List<String> loadOrder(EchoNativePackProfile profile, List<EchoNativeAddonDescriptor> descriptors) {
        Set<String> ordered = new LinkedHashSet<>(profile.requiredModules());
        descriptors.stream().map(EchoNativeAddonDescriptor::id).sorted().forEach(ordered::add);
        return List.copyOf(ordered);
    }

    private static List<Map<String, Object>> lockedModules(EchoNativePackProfile profile, List<EchoNativeAddonDescriptor> descriptors) {
        Set<String> required = new TreeSet<>(profile.requiredModules());
        List<Map<String, Object>> modules = new ArrayList<>();
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            Map<String, Object> module = new LinkedHashMap<>();
            module.put("apiStability", descriptor.apiStability().name());
            module.put("checksum", checksum(descriptor));
            module.put("checksumMode", "descriptor_metadata");
            module.put("consumes", descriptor.consumes());
            module.put("id", descriptor.id());
            module.put("kind", descriptor.kind());
            module.put("name", descriptor.name());
            module.put("official", descriptor.official());
            module.put("optional", descriptor.optional());
            module.put("path", safePath(descriptor.descriptorPath()));
            module.put("provides", descriptor.provides());
            module.put("required", required.contains(descriptor.id()));
            module.put("requires", descriptor.requires());
            module.put("role", descriptor.role());
            module.put("side", descriptor.side().name());
            module.put("source", "fixture_descriptor");
            module.put("standalone", descriptor.standalone());
            module.put("trustLevel", descriptor.trustLevel().name());
            module.put("version", descriptor.version());
            modules.add(module);
        }
        return List.copyOf(modules);
    }

    private static List<Map<String, Object>> lockedFeatures(EchoNativePackProfile profile, Map<String, Set<String>> featureProviders) {
        Set<String> allFeatures = new TreeSet<>();
        allFeatures.addAll(featureProviders.keySet());
        allFeatures.addAll(profile.requiredFeatures());
        allFeatures.addAll(profile.optionalFeatures());

        List<Map<String, Object>> features = new ArrayList<>();
        for (String feature : allFeatures) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("featureId", feature);
            item.put("optionalForPack", profile.optionalFeatures().contains(feature));
            item.put("providers", List.copyOf(featureProviders.getOrDefault(feature, Set.of())));
            item.put("requiredByPack", profile.requiredFeatures().contains(feature));
            item.put("status", featureStatus(profile, feature, featureProviders));
            features.add(item);
        }
        return List.copyOf(features);
    }

    private static String featureStatus(EchoNativePackProfile profile, String feature, Map<String, Set<String>> featureProviders) {
        if (featureProviders.containsKey(feature)) {
            return "locked";
        }
        if (profile.requiredFeatures().contains(feature)) {
            return "missing_required";
        }
        if (profile.optionalFeatures().contains(feature)) {
            return "planned_missing_optional";
        }
        return "unprovided";
    }

    private static String checksum(EchoNativeAddonDescriptor descriptor) {
        String value = String.join("|",
                descriptor.id(),
                descriptor.name(),
                descriptor.version(),
                descriptor.kind(),
                descriptor.role(),
                descriptor.side().name(),
                descriptor.trustLevel().name(),
                descriptor.apiStability().name(),
                String.join(",", descriptor.requires()),
                String.join(",", descriptor.optional()),
                String.join(",", descriptor.provides()),
                String.join(",", descriptor.consumes()),
                String.join(",", descriptor.transforms()),
                safePath(descriptor.descriptorPath())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                hex.append(String.format("%02x", item));
            }
            return "sha256:" + hex;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    static String safePath(Path path) {
        Path normalized = path.normalize();
        if (!normalized.isAbsolute()) {
            return normalized.toString().replace('\\', '/');
        }
        Path workspace = Path.of("").toAbsolutePath().normalize();
        try {
            return workspace.relativize(normalized).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return normalized.getFileName() == null ? normalized.toString().replace('\\', '/') : normalized.getFileName().toString();
        }
    }
}
