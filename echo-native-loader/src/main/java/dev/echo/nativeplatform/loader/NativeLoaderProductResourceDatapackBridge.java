package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NativeLoaderProductResourceDatapackBridge {
    public static final String SERVICE_ID = "echo.native.product_resource_datapack_bridge";
    public static final String INTERNAL_MODULE_RESOURCE_PACK_ID = "echo_native_modules";

    private final EchoNativeBootstrapProductProfile profile;
    private final String nativeModuleClasspathProperty;

    private NativeLoaderProductResourceDatapackBridge(
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty
    ) {
        this.profile = profile;
        this.nativeModuleClasspathProperty = nativeModuleClasspathProperty == null ? "" : nativeModuleClasspathProperty;
    }

    public static Map<String, Object> apply(
            EchoNativeBootstrapProductProfile profile,
            String nativeModuleClasspathProperty,
            String packId,
            List<String> remainingArgs,
            List<String> modules
    ) {
        return new NativeLoaderProductResourceDatapackBridge(profile, nativeModuleClasspathProperty)
                .applyResourceBridge(packId, remainingArgs, modules);
    }

    public static void installInternalModuleResourcePackMount(
            EchoNativeBootstrapProductProfile profile,
            String packId,
            Map<String, Object> resourceBridge,
            List<String> modules,
            Path markerPath,
            ModuleResourcePackMountStarter mountStarter
    ) {
        new NativeLoaderProductResourceDatapackBridge(profile, "")
                .installInternalModuleResourcePackMount(packId, resourceBridge, modules, markerPath, mountStarter);
    }

    private Map<String, Object> applyResourceBridge(String packId, List<String> remainingArgs, List<String> modules) {
        Map<String, Object> data = new LinkedHashMap<>();
        Path gameDir = NativeLoaderLaunchPathSupport.argumentPath(remainingArgs, "--gameDir");
        data.put("bridge", "adaptercore.native_resource");
        data.put("serviceId", SERVICE_ID);
        data.put("packId", packId);
        data.put("nativeProductNamespace", profile.namespace());
        data.put("nativeProductGameplayContentDataPrefixes", new LinkedHashMap<>(profile.nativeGameplayContentDataPrefixes()));
        data.put("contentDataPrefixes", new LinkedHashMap<>(profile.nativeGameplayContentDataPrefixes()));
        data.put("gameDir", gameDir == null ? "" : gameDir.toString());
        data.put("minecraftResourceManagerTouched", false);
        data.put("resourceRuntimeAccessed", false);
        data.put("filesystemMutated", false);
        if (gameDir == null) {
            data.put("applied", false);
            data.put("failureKind", "missing_game_dir");
            data.put("summary", "Native resource bridge could not apply because --gameDir was not present.");
            return data;
        }
        try {
            Path resourcePackDir = gameDir.resolve("resourcepacks").toAbsolutePath().normalize();
            Path resourcePack = resourcePackDir.resolve(
                    "echo-native-" + NativeLoaderLaunchPathSupport.sanitizeResourceIdPart(packId) + "-resources.zip");
            NativeLoaderResourcePackBuilder.BuildResult buildResult = buildResourcePack(resourcePack, modules);
            boolean optionsUpdated = NativeLoaderResourcePackSelection.removeFilePackSelection(
                    gameDir.resolve("options.txt"),
                    resourcePack.getFileName().toString()
            );
            Map<String, Object> datapackResult = installProductDatapacks(gameDir, resourcePack, buildResult);
            Map<String, Object> resourceHostReport = preWorldCreationResourceHostReport(packId, resourcePack, datapackResult);
            Map<String, Object> resourceHostEvidence = writePreWorldCreationResourceHostEvidence(
                    gameDir,
                    packId,
                    resourceHostReport);
            data.put("applied", buildResult.copiedResources() > 0);
            data.put("copiedResourceCount", buildResult.copiedResources());
            data.put("dataEntryCount", buildResult.dataEntries());
            data.put("internalModuleResourcePack", true);
            data.put("internalModuleResourcePackId", INTERNAL_MODULE_RESOURCE_PACK_ID);
            data.put("internalModuleResourcePackMount", "PackRepository required hidden source");
            data.put("resourcePackCacheOnly", true);
            data.put("userFacingResourcePackEnabled", false);
            data.put("nativeProductWorldgenBiomeCount", buildResult.productWorldgenBiomes());
            data.put("nativeProductWorldgenPresetPresent", buildResult.productWorldgenPresetPresent());
            data.put("nativeProductWorldgenRootMarkerPresent", buildResult.productWorldgenRootMarkerPresent());
            data.put("nativeProductOverworldSettingsPresent", buildResult.productOverworldSettingsPresent());
            data.put("productWorldgenBiomeCount", buildResult.productWorldgenBiomes());
            data.put("productWorldgenPresetPresent", buildResult.productWorldgenPresetPresent());
            data.put("productWorldgenRootMarkerPresent", buildResult.productWorldgenRootMarkerPresent());
            data.put("normalizedTechBlockstateCount", buildResult.normalizedTechBlockstates().size());
            data.put("normalizedTechBlockstates", buildResult.normalizedTechBlockstates());
            data.put("techBlockstateNormalizationApplied", !buildResult.normalizedTechBlockstates().isEmpty());
            data.put("safeModeGuardSkippedResourceEntryCount", buildResult.safeModeGuardSkippedEntries().size());
            data.put("safeModeGuardSkippedResourceEntries", buildResult.safeModeGuardSkippedEntries());
            data.put("sourceBackedFallbackResourceEntryCount", buildResult.sourceBackedFallbackEntries().size());
            data.put("sourceBackedFallbackResourceEntries", buildResult.sourceBackedFallbackEntries());
            data.put("recipeDataValidation", buildResult.recipeDataValidation().toMap());
            data.put("productRecipeJsonCount", buildResult.recipeDataValidation().productVanillaRecipeCount());
            data.put("productMachineRecipeCatalogPresent", buildResult.recipeDataValidation().productMachineRecipeCatalogPresent());
            data.put("unsupportedCustomRecipeSerializerGapCount", buildResult.recipeDataValidation().customSerializerGapCount());
            data.put("unsupportedCustomRecipeSerializerGaps", buildResult.recipeDataValidation().customSerializerGaps());
            data.put("datapackBridge", datapackResult);
            data.put("datapackInstalled", Boolean.TRUE.equals(datapackResult.get("installed")));
            data.put("datapackInstalledSaveCount", integer(datapackResult.get("installedSaveCount")));
            data.put("datapackInstallPaths", datapackResult.get("installedPaths"));
            data.put("datapackLaunchStaged", Boolean.TRUE.equals(datapackResult.get("launchStaged")));
            data.put("datapackLaunchStagedCount", integer(datapackResult.get("launchStagedCount")));
            data.put("datapackLaunchStagedPaths", datapackResult.get("launchStagedPaths"));
            data.put("datapackPurgedSaveCount", integer(datapackResult.get("purgedSaveCount")));
            data.put("datapackPurgedPaths", datapackResult.get("purgedPaths"));
            data.put("nativeLoaderResourceHost", resourceHostReport);
            data.put("nativeLoaderResourceHostEvidencePath", resourceHostEvidence.get("path"));
            data.put("nativeLoaderResourceHostEvidenceWritten",
                    Boolean.TRUE.equals(resourceHostEvidence.get("written")));
            data.put("nativeLoaderResourceHostEvidence", resourceHostEvidence);
            int preWorldCreationMountCount = integer(resourceHostReport.get("mountedPreWorldCreationResourceCount"));
            int preWorldCreationDataPackCount = integer(resourceHostReport.get("mountedDataPackResourceCount"));
            int preWorldCreationResourcePackCount = integer(resourceHostReport.get("mountedResourcePackResourceCount"));
            boolean resourceHostEvidenceWritten = Boolean.TRUE.equals(resourceHostEvidence.get("written"));
            data.put("nativeResourceHostPreWorldCreationMountCount", preWorldCreationMountCount);
            data.put("nativeResourceHostPreWorldCreationDataPackMountCount", preWorldCreationDataPackCount);
            data.put("nativeResourceHostPreWorldCreationResourcePackMountCount", preWorldCreationResourcePackCount);
            data.put("nativeResourceHostPreWorldCreationEvidenceRequired", true);
            data.put("nativeResourceHostDataPackMountedBeforeRegistryWorldCreation",
                    resourceHostEvidenceWritten && preWorldCreationDataPackCount > 0);
            data.put("nativeResourceHostResourcePackMountedBeforeRegistryWorldCreation",
                    resourceHostEvidenceWritten && preWorldCreationResourcePackCount > 0);
            data.put("nativeResourceHostMountedBeforeRegistryWorldCreation",
                    resourceHostEvidenceWritten
                            && preWorldCreationMountCount > 0
                            && preWorldCreationDataPackCount > 0
                            && preWorldCreationResourcePackCount > 0);
            data.put("filesystemMutated", buildResult.copiedResources() > 0
                    || optionsUpdated
                    || Boolean.TRUE.equals(datapackResult.get("filesystemMutated")));
            data.put("optionsUpdated", optionsUpdated);
            data.put("resourcePackSelectionRemoved", optionsUpdated);
            data.put("resourcePack", resourcePack.toString());
            data.put("summary", buildResult.copiedResources() > 0
                    ? "Native resource bridge materialized module resources as an internal cache, removed optional pack selection, separated save/launch datapacks, and prepared a hidden required PackRepository source before Minecraft handoff."
                    : "Native resource bridge found no ECHO resource entries to apply.");
        } catch (Throwable exception) {
            data.put("applied", false);
            data.put("failureKind", exception.getClass().getSimpleName());
            data.put("summary", "Native resource bridge failed before Minecraft handoff: " + exception.getMessage());
        }
        return data;
    }

    private Map<String, Object> writePreWorldCreationResourceHostEvidence(
            Path gameDir,
            String packId,
            Map<String, Object> resourceHostReport
    ) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        Path evidencePath = gameDir.toAbsolutePath().normalize()
                .resolve("echo-native")
                .resolve("resource-host-preworld-mounts.json");
        evidence.put("schema", "echo.native.resource_host.preworld_mounts.v1");
        evidence.put("serviceId", NativeLoaderResourceHost.SERVICE_ID);
        evidence.put("packId", packId);
        evidence.put("mountPhase", "before_registry_and_world_creation");
        evidence.put("nativeLoaderOwnedResourcePolicy", true);
        evidence.put("path", evidencePath.toString());
        evidence.put("resourceHostReport", resourceHostReport == null ? Map.of() : Map.copyOf(resourceHostReport));
        evidence.put("mountFilesPresent", mountFilesPresent(resourceHostReport));
        evidence.put("mountedPreWorldCreationResourceCount",
                integer(resourceHostReport == null ? null : resourceHostReport.get("mountedPreWorldCreationResourceCount")));
        evidence.put("mountedDataPackResourceCount",
                integer(resourceHostReport == null ? null : resourceHostReport.get("mountedDataPackResourceCount")));
        evidence.put("mountedResourcePackResourceCount",
                integer(resourceHostReport == null ? null : resourceHostReport.get("mountedResourcePackResourceCount")));
        evidence.put("nativeResourceHostDataPackMountedBeforeRegistryWorldCreation",
                integer(evidence.get("mountedDataPackResourceCount")) > 0);
        evidence.put("nativeResourceHostResourcePackMountedBeforeRegistryWorldCreation",
                integer(evidence.get("mountedResourcePackResourceCount")) > 0);
        evidence.put("nativeResourceHostMountedBeforeRegistryWorldCreation",
                integer(evidence.get("mountedPreWorldCreationResourceCount")) > 0
                        && Boolean.TRUE.equals(evidence.get("nativeResourceHostDataPackMountedBeforeRegistryWorldCreation"))
                        && Boolean.TRUE.equals(evidence.get("nativeResourceHostResourcePackMountedBeforeRegistryWorldCreation"))
                        && Boolean.TRUE.equals(evidence.get("mountFilesPresent")));
        try {
            Files.createDirectories(evidencePath.getParent());
            Files.deleteIfExists(evidencePath);
            evidence.put("written", true);
            Path temp = evidencePath.resolveSibling(evidencePath.getFileName() + ".tmp");
            Files.writeString(temp, EchoNativeJson.write(evidence), StandardCharsets.UTF_8);
            try {
                Files.move(temp, evidencePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, evidencePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            evidence.put("written", false);
            evidence.put("failureKind", exception.getClass().getSimpleName());
            evidence.put("failureMessage", failureMessage(exception));
        }
        return Map.copyOf(evidence);
    }

    private Map<String, Object> preWorldCreationResourceHostReport(
            String packId,
            Path resourcePack,
            Map<String, Object> datapackResult
    ) {
        return NativeLoaderResourceHost.preWorldCreationProductMountReport(
                profile.namespace(),
                packId,
                resourcePack,
                stringList(datapackResult.get("launchStagedPaths")),
                profile.nativeWorldPresetMirrorTarget(),
                NativeLoaderResourceHost.SERVICE_ID
        );
    }

    private NativeLoaderResourcePackBuilder.BuildResult buildResourcePack(
            Path resourcePack,
            List<String> modules
    ) throws IOException {
        return NativeLoaderResourcePackBuilder.buildResourcePack(
                resourcePack,
                profile,
                nativeModuleClasspathProperty
        );
    }

    private String nativeGameplayContentDataPrefix(String key) {
        String prefix = profile.nativeGameplayContentDataPrefixes().get(lowerContentId(key));
        return prefix == null ? "" : prefix;
    }

    private Map<String, Object> installProductDatapacks(
            Path gameDir,
            Path resourcePack,
            NativeLoaderResourcePackBuilder.BuildResult buildResult
    ) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempted", true);
        result.put("installed", false);
        result.put("filesystemMutated", false);
        result.put("installedSaveCount", 0);
        result.put("installedPaths", List.of());
        result.put("launchStaged", false);
        result.put("launchStagedCount", 0);
        result.put("launchStagedPaths", List.of());
        result.put("purgedSaveCount", 0);
        result.put("purgedPaths", List.of());
        result.put("nativeProductDatapackSafeModeGuardActive", false);
        result.put("safeModeGuardActive", false);
        result.put("nativeProductWorldgenBiomeCount", buildResult.productWorldgenBiomes());
        result.put("nativeProductWorldgenPresetPresent", buildResult.productWorldgenPresetPresent());
        result.put("nativeProductWorldgenRootMarkerPresent", buildResult.productWorldgenRootMarkerPresent());
        result.put("nativeProductOverworldSettingsPresent", buildResult.productOverworldSettingsPresent());
        result.put("productWorldgenBiomeCount", buildResult.productWorldgenBiomes());
        result.put("productWorldgenPresetPresent", buildResult.productWorldgenPresetPresent());
        result.put("productWorldgenRootMarkerPresent", buildResult.productWorldgenRootMarkerPresent());
        if (gameDir == null || resourcePack == null || !Files.isRegularFile(resourcePack)) {
            result.put("failureKind", "missing_resource_pack");
            result.put("summary", "Product datapack bridge could not run because the generated resource pack is missing.");
            return result;
        }
        if (buildResult.productWorldgenBiomes() <= 0 || !buildResult.productWorldgenPresetPresent()) {
            result.put("failureKind", "missing_product_worldgen_data");
            result.put("summary", "Product datapack bridge did not install because the real product worldgen data was not copied into the native pack.");
            return result;
        }
        List<String> purgedPaths = new ArrayList<>();
        List<String> installedPaths = new ArrayList<>();
        List<String> launchStagedPaths = new ArrayList<>();
        List<Map<String, Object>> validations = new ArrayList<>();
        Path savesDir = gameDir.resolve("saves").toAbsolutePath().normalize();
        try {
            if (Files.isDirectory(savesDir)) {
                try (var stream = Files.list(savesDir)) {
                    for (Path save : stream
                            .filter(Files::isDirectory)
                            .filter(path -> Files.isRegularFile(path.resolve("level.dat")))
                            .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                            .toList()) {
                        Path datapackDir = save.resolve("datapacks").toAbsolutePath().normalize();
                        Path datapack = datapackDir.resolve(profile.nativeSaveDatapackFileName());
                        if (Files.deleteIfExists(datapack)) {
                            purgedPaths.add(datapack.toString());
                        }
                        Files.createDirectories(datapackDir);
                        int copied = writeProductWorldgenDatapack(resourcePack, datapack);
                        Map<String, Object> validation = validateProductWorldgenDatapack(datapack);
                        validations.add(validation);
                        if (copied > 1 && Boolean.TRUE.equals(validation.get("accepted"))) {
                            installedPaths.add(datapack.toString());
                        } else {
                            Files.deleteIfExists(datapack);
                        }
                    }
                }
            }
            launchStagedPaths.addAll(stageProductWorldgenDatapacks(gameDir, resourcePack, buildResult, validations));
            result.put("installed", !installedPaths.isEmpty());
            result.put("filesystemMutated", !purgedPaths.isEmpty()
                    || !installedPaths.isEmpty()
                    || !launchStagedPaths.isEmpty());
            result.put("installedSaveCount", installedPaths.size());
            result.put("installedPaths", installedPaths);
            result.put("launchStaged", !launchStagedPaths.isEmpty());
            result.put("launchStagedCount", launchStagedPaths.size());
            result.put("launchStagedPaths", launchStagedPaths);
            result.put("purgedSaveCount", purgedPaths.size());
            result.put("purgedPaths", purgedPaths);
            result.put("datapackValidations", validations);
            result.put("nativeProductDatapackSafeModeGuardActive", true);
            result.put("safeModeGuardActive", true);
            result.put("safeModeGuardSkippedEntries", buildResult.safeModeGuardSkippedEntries());
            result.put("summary", installedPaths.isEmpty()
                    ? "Product datapack bridge staged real product worldgen for Native launch/new-world flows but found no existing saves with level.dat to patch."
                    : "Product datapack bridge installed real product worldgen into existing saves and staged it for Native launch/new-world flows, including the configured world preset override.");
        } catch (Throwable exception) {
            result.put("failureKind", exception.getClass().getSimpleName());
            result.put("failureMessage", failureMessage(exception));
            result.put("summary", "Product datapack bridge failed while installing save datapacks: " + failureMessage(exception));
        }
        return result;
    }

    private List<String> stageProductWorldgenDatapacks(
            Path gameDir,
            Path resourcePack,
            NativeLoaderResourcePackBuilder.BuildResult buildResult,
            List<Map<String, Object>> validations
    ) throws IOException {
        List<String> staged = new ArrayList<>();
        if (gameDir == null || resourcePack == null || !Files.isRegularFile(resourcePack)) {
            return staged;
        }
        List<Path> targets = List.of(
                gameDir.resolve("datapacks").resolve(profile.nativeSaveDatapackFileName()),
                gameDir.resolve("echo-native").resolve("worldgen").resolve(profile.nativeSaveDatapackFileName()),
                gameDir.resolve("saves")
                        .resolve(nativeLaunchWorldFolderName())
                        .resolve("datapacks")
                        .resolve(profile.nativeSaveDatapackFileName())
        );
        for (Path target : targets) {
            Path datapack = target.toAbsolutePath().normalize();
            Files.createDirectories(datapack.getParent());
            int copied = writeProductWorldgenDatapack(resourcePack, datapack);
            Map<String, Object> validation = validateProductWorldgenDatapack(datapack);
            Map<String, Object> stagedValidation = new LinkedHashMap<>(validation);
            stagedValidation.put("launchStagingTarget", true);
            validations.add(Map.copyOf(stagedValidation));
            if (copied > 1 && Boolean.TRUE.equals(validation.get("accepted"))) {
                staged.add(datapack.toString());
            } else {
                Files.deleteIfExists(datapack);
            }
        }
        return staged;
    }

    private String nativeLaunchWorldFolderName() {
        String configured = System.getProperty("echo.native.productWorldFolder", "");
        if (configured != null && !configured.isBlank()) {
            return NativeLoaderLaunchPathSupport.sanitizeResourceIdPart(configured.trim());
        }
        String packId = profile.nativeGameplayPackId();
        String source = packId == null || packId.isBlank() ? profile.namespace() : packId;
        String sanitized = NativeLoaderLaunchPathSupport.sanitizeResourceIdPart(source)
                .toLowerCase(java.util.Locale.ROOT);
        return sanitized.isBlank() ? "echo_native_world" : "echo_native_" + sanitized;
    }

    private int writeProductWorldgenDatapack(Path resourcePack, Path datapack) throws IOException {
        return NativeLoaderWorldgenDatapackWriter.writeProductWorldgenDatapack(
                resourcePack,
                datapack,
                profile.nativeSaveDatapackDescription(),
                profile.nativeSaveDatapackEntryPrefixes(),
                profile.nativeStructureTemplateTargetPrefix(),
                profile.nativeStructureTemplateSourcePrefix(),
                profile.namespace(),
                profile.nativeWorldgenStructurePrefix(),
                profile.nativeWorldgenBiomePrefix()
        );
    }

    private Map<String, Object> validateProductWorldgenDatapack(Path datapack) throws IOException {
        return NativeLoaderWorldgenDatapackValidator.validateProductWorldgenDatapack(
                datapack,
                profile.nativeWorldPresetMirrorSource(),
                profile.nativeWorldPresetMirrorTarget(),
                profile.nativeSourceResourceRootMarker(),
                profile.nativeSaveDatapackRequiredEntriesByValidationKey(),
                profile.nativeStructureTemplateSourcePrefix(),
                profile.nativeStructureTemplateTargetPrefix(),
                profile.nativeWorldgenBiomePrefix()
        );
    }

    private void installInternalModuleResourcePackMount(
            String packId,
            Map<String, Object> resourceBridge,
            List<String> modules,
            Path markerPath,
            ModuleResourcePackMountStarter mountStarter
    ) {
        String resourcePack = text(resourceBridge.get("resourcePack"));
        if (resourcePack.isBlank() || mountStarter == null) {
            return;
        }
        Path gameDir = markerPath.toAbsolutePath().normalize().getParent();
        if (gameDir == null) {
            gameDir = Path.of("").toAbsolutePath().normalize();
        } else {
            gameDir = gameDir.getParent() == null ? gameDir : gameDir.getParent();
        }
        mountStarter.start(
                packId,
                Path.of(resourcePack),
                modules,
                profile.nativeModuleResourceSourcePathMarkers(),
                gameDir.resolve("echo-native/module-resource-pack-mount.json")
        );
    }

    private boolean isEchoNamespace(String namespace) {
        String value = lowerContentId(namespace);
        if (value.isBlank()) {
            return false;
        }
        for (String prefix : profile.nativeModuleNamespacePrefixes()) {
            String safePrefix = lowerContentId(prefix);
            if (!safePrefix.isBlank() && value.startsWith(safePrefix)) {
                return true;
            }
        }
        return false;
    }

    @FunctionalInterface
    public interface ModuleResourcePackMountStarter {
        void start(
                String packId,
                Path resourcePackCache,
                List<String> modules,
                List<String> productModuleSourcePathMarkers,
                Path evidencePath
        );
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String failureMessage(Throwable exception) {
        return exception == null || exception.getMessage() == null ? "" : exception.getMessage();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int integer(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : iterable) {
            String text = text(item);
            if (!text.isBlank()) {
                values.add(text);
            }
        }
        return List.copyOf(values);
    }

    private static boolean mountFilesPresent(Map<String, Object> resourceHostReport) {
        Object resources = resourceHostReport == null ? null : resourceHostReport.get("resources");
        if (!(resources instanceof Iterable<?> iterable)) {
            return false;
        }
        boolean sawMount = false;
        for (Object item : iterable) {
            if (!(item instanceof Map<?, ?> resource)) {
                return false;
            }
            Object evidenceValue = resource.get("evidence");
            if (!(evidenceValue instanceof Map<?, ?> evidence)) {
                return false;
            }
            if (!Boolean.TRUE.equals(evidence.get("mountFilePresent"))) {
                return false;
            }
            String mountPath = text(evidence.get("mountPath"));
            if (mountPath.isBlank() || !Files.isRegularFile(Path.of(mountPath))) {
                return false;
            }
            sawMount = true;
        }
        return sawMount;
    }

}
