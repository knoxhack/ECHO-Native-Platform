package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeConfigSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeConfigSourceInventory;
import dev.echo.nativeplatform.contracts.EchoNativeConfigValidationResult;
import dev.echo.nativeplatform.contracts.EchoNativeConfigWritePlan;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeConfigPrototype {
    EchoNativeConfigPrototypeOutcome prototype(
            String packId,
            Path fixture,
            Path serviceBusRegistryPath,
            Path serviceBusSimulationPath,
            Path serviceBusSafetyPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> serviceBusRegistry = readRequiredReport(serviceBusRegistryPath, fixture, packId, "ECHO-NATIVE-SERVICE-BUS-REGISTRY-MISSING", "Service bus registry report missing", diagnostics);
        Map<String, Object> serviceBusSimulation = readRequiredReport(serviceBusSimulationPath, fixture, packId, "ECHO-NATIVE-SERVICE-BUS-SIMULATION-MISSING", "Service bus simulation report missing", diagnostics);
        Map<String, Object> serviceBusSafety = readRequiredReport(serviceBusSafetyPath, fixture, packId, "ECHO-NATIVE-SERVICE-BUS-SAFETY-MISSING", "Service bus safety report missing", diagnostics);

        checkUpstream(serviceBusRegistry, EchoNativeJson.asObject(serviceBusRegistry.get("data")), serviceBusRegistryPath, packId, "ECHO-NATIVE-SERVICE-BUS-REGISTRY-BLOCKED", "Service bus registry is not ready for config prototyping", diagnostics);
        checkUpstream(serviceBusSimulation, EchoNativeJson.asObject(serviceBusSimulation.get("data")), serviceBusSimulationPath, packId, "ECHO-NATIVE-SERVICE-BUS-SIMULATION-BLOCKED", "Service bus simulation is not ready for config prototyping", diagnostics);
        checkUpstream(serviceBusSafety, EchoNativeJson.asObject(serviceBusSafety.get("data")), serviceBusSafetyPath, packId, "ECHO-NATIVE-SERVICE-BUS-SAFETY-BLOCKED", "Service bus safety is not ready for config prototyping", diagnostics);

        ConfigManifest manifest = readConfigManifest(fixture.resolve("configs").resolve("echo.native.configs.json"), fixture, packId, diagnostics);
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();

        List<Map<String, Object>> configSources = ready ? manifest.configSources() : List.of();
        List<Map<String, Object>> validatedConfigs = ready ? validatedConfigs(configSources) : List.of();
        List<Map<String, Object>> plannedWrites = ready ? plannedWrites(configSources) : List.of();

        EchoNativeConfigSourceInventory inventory = new EchoNativeConfigSourceInventory(
                "phase13.m11.config.source.inventory",
                ready,
                true,
                true,
                false,
                false,
                configSources.size(),
                configSources
        );
        EchoNativeConfigValidationResult validation = new EchoNativeConfigValidationResult(
                "phase13.m11.config.validation.result",
                ready,
                true,
                true,
                false,
                false,
                validatedConfigs.size(),
                validatedConfigs
        );
        EchoNativeConfigWritePlan writePlan = new EchoNativeConfigWritePlan(
                "phase13.m11.config.write.plan",
                ready,
                true,
                false,
                false,
                false,
                plannedWrites.size(),
                plannedWrites
        );
        EchoNativeConfigSafetyStatus safetyStatus = new EchoNativeConfigSafetyStatus(
                "phase13.m11.config.safety.status",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ready ? List.of("service_bus_pass", "config_manifest_read", "config_sources_validated", "write_plan_only") : List.of()
        );

        return new EchoNativeConfigPrototypeOutcome(
                packId,
                configSourceInventory(packId, inventory, diagnostics),
                configValidationResult(packId, validation, diagnostics),
                configWritePlan(packId, writePlan, diagnostics),
                configSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static ConfigManifest readConfigManifest(
            Path manifestPath,
            Path fixture,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(manifestPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-MANIFEST-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config manifest missing",
                    "Config prototyping requires a fixture-local configs/echo.native.configs.json manifest.",
                    null,
                    packId,
                    List.of(fixture.resolve("configs/echo.native.configs.json").toString().replace('\\', '/')),
                    "Add a fixture-local config manifest or keep M11 blocked for this fixture."
            ));
            return new ConfigManifest(List.of());
        }
        Map<String, Object> manifest;
        try {
            manifest = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(manifestPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-MANIFEST-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config manifest is invalid JSON",
                    ex.getMessage(),
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Fix the fixture-local config manifest JSON."
            ));
            return new ConfigManifest(List.of());
        }
        if (!"echo.native.config_manifest.v1".equals(manifest.get("schema"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-MANIFEST-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported native config manifest schema",
                    "Config manifest schema was '" + manifest.get("schema") + "'.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use schema echo.native.config_manifest.v1."
            ));
        }
        Map<String, Object> sourcePolicy = EchoNativeJson.asObject(manifest.get("sourcePolicy"));
        if (!Boolean.TRUE.equals(sourcePolicy.get("localOnly"))
                || !Boolean.TRUE.equals(sourcePolicy.get("writePlanOnly"))
                || Boolean.TRUE.equals(sourcePolicy.get("installedConfigMutationAllowed"))
                || Boolean.TRUE.equals(sourcePolicy.get("fixtureConfigMutationAllowed"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-SOURCE-POLICY-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config source policy is unsafe",
                    "M11 requires localOnly=true, writePlanOnly=true, and config mutation disabled.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep config writes as plans only for M11."
            ));
        }
        List<Map<String, Object>> sources = new ArrayList<>();
        Object rawConfigs = manifest.get("configs");
        if (rawConfigs instanceof List<?> configs) {
            for (Object item : configs) {
                Map<String, Object> config = EchoNativeJson.asObject(item);
                Map<String, Object> source = readConfigSource(config, fixture, manifestPath, packId, diagnostics);
                if (!source.isEmpty()) {
                    sources.add(source);
                }
            }
        }
        sources.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        if (sources.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-MANIFEST-EMPTY",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config manifest has no usable config sources",
                    "Config prototyping needs at least one fixture-local config source.",
                    null,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Add config descriptors and fixture-local config files."
            ));
        }
        return new ConfigManifest(List.copyOf(sources));
    }

    private static Map<String, Object> readConfigSource(
            Map<String, Object> config,
            Path fixture,
            Path manifestPath,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        String id = String.valueOf(config.getOrDefault("id", "")).trim();
        String ownerModule = String.valueOf(config.getOrDefault("ownerModule", "")).trim();
        String sourcePath = String.valueOf(config.getOrDefault("sourcePath", "")).trim().replace('\\', '/');
        String plannedTargetPath = String.valueOf(config.getOrDefault("plannedTargetPath", "")).trim().replace('\\', '/');
        String format = String.valueOf(config.getOrDefault("format", "")).trim();
        String scope = String.valueOf(config.getOrDefault("scope", "")).trim();
        if (id.isBlank() || ownerModule.isBlank() || sourcePath.isBlank() || plannedTargetPath.isBlank() || format.isBlank() || scope.isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-DESCRIPTOR-INCOMPLETE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config descriptor is incomplete",
                    "Each config requires id, ownerModule, scope, format, sourcePath, and plannedTargetPath.",
                    id.isBlank() ? null : id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Complete the fixture-local config descriptor."
            ));
            return Map.of();
        }
        if (!"json".equals(format)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-FORMAT-UNSUPPORTED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config format is unsupported",
                    "M11 supports JSON fixture config sources only.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Use format=json for the M11 config prototype."
            ));
            return Map.of();
        }
        if (isUnsafeRelativePath(sourcePath) || !plannedTargetPath.startsWith("planned://config/")) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-PATH-UNSAFE",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config path is unsafe",
                    "Config sources must be fixture-relative, and write targets must use planned://config/ URIs.",
                    id,
                    packId,
                    List.of(relativeReportPath(manifestPath)),
                    "Keep M11 config reads inside the fixture and writes as planned URIs."
            ));
            return Map.of();
        }
        Path configPath = fixture.resolve(sourcePath).normalize();
        Path fixtureRoot = fixture.toAbsolutePath().normalize();
        if (!configPath.toAbsolutePath().normalize().startsWith(fixtureRoot) || !Files.isRegularFile(configPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-SOURCE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config source file missing",
                    "Config source '" + sourcePath + "' was not found under the fixture.",
                    id,
                    packId,
                    List.of(sourcePath),
                    "Add the fixture-local config file or remove the descriptor."
            ));
            return Map.of();
        }
        Map<String, Object> configData;
        try {
            configData = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(configPath)));
        } catch (RuntimeException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-SOURCE-INVALID",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config source is invalid JSON",
                    ex.getMessage(),
                    id,
                    packId,
                    List.of(relativeReportPath(configPath)),
                    "Fix the fixture-local config JSON."
            ));
            return Map.of();
        }
        if (String.valueOf(configData.getOrDefault("schema", "")).isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONFIG-SOURCE-SCHEMA-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native config source schema missing",
                    "Each fixture config JSON must declare a schema.",
                    id,
                    packId,
                    List.of(relativeReportPath(configPath)),
                    "Add a schema field to the fixture config source."
            ));
            return Map.of();
        }
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("format", format);
        source.put("id", id);
        source.put("keyCount", configData.size());
        source.put("localOnly", true);
        source.put("ownerModule", ownerModule);
        source.put("plannedTargetPath", plannedTargetPath);
        source.put("required", Boolean.TRUE.equals(config.get("required")));
        source.put("scope", scope);
        source.put("schema", configData.get("schema"));
        source.put("sourcePath", sourcePath);
        source.put("writePlanOnly", true);
        return source;
    }

    private static void checkUpstream(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            String code,
            String title,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        if (!"PASS".equals(report.get("status")) || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    code,
                    EchoNativeIssueSeverity.ERROR,
                    title,
                    "M11 config prototyping requires PASS M10 service bus reports with no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the M10 service bus prototype reports before config prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> validatedConfigs(List<Map<String, Object>> sources) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> source : sources) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", source.get("id"));
            item.put("ownerModule", source.get("ownerModule"));
            item.put("scope", source.get("scope"));
            item.put("schema", source.get("schema"));
            item.put("valid", true);
            item.put("writePlanOnly", true);
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> plannedWrites(List<Map<String, Object>> sources) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = 0; index < sources.size(); index++) {
            Map<String, Object> source = sources.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("installedConfigMutated", false);
            item.put("order", index);
            item.put("sourceConfigId", source.get("id"));
            item.put("sourcePath", source.get("sourcePath"));
            item.put("targetPath", source.get("plannedTargetPath"));
            item.put("writePlanOnly", true);
            result.add(item);
        }
        return List.copyOf(result);
    }

    private static Map<String, Object> configSourceInventory(String packId, EchoNativeConfigSourceInventory inventory, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m11_config_source_inventory", diagnostics);
        data.put("configSourceCount", inventory.configSourceCount());
        data.put("configSources", inventory.configSources());
        data.put("fixtureConfigMutationAllowed", inventory.fixtureConfigMutationAllowed());
        data.put("installedConfigMutationAllowed", inventory.installedConfigMutationAllowed());
        data.put("inventoryId", inventory.inventoryId());
        data.put("localOnly", inventory.localOnly());
        data.put("packId", packId);
        data.put("read", inventory.read());
        data.put("summary", inventory.read()
                ? "Fixture-local native config sources were inventoried without mutating configs."
                : "Config source inventory is blocked by diagnostics.");
        data.put("writePlanOnly", inventory.writePlanOnly());
        return data;
    }

    private static Map<String, Object> configValidationResult(String packId, EchoNativeConfigValidationResult validation, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m11_config_validation_result", diagnostics);
        data.put("fixtureConfigMutationAllowed", validation.fixtureConfigMutationAllowed());
        data.put("installedConfigMutationAllowed", validation.installedConfigMutationAllowed());
        data.put("localOnly", validation.localOnly());
        data.put("packId", packId);
        data.put("resultId", validation.resultId());
        data.put("summary", validation.valid()
                ? "Fixture-local native config sources validated successfully."
                : "Config validation is blocked by diagnostics.");
        data.put("valid", validation.valid());
        data.put("validatedConfigCount", validation.validatedConfigCount());
        data.put("validatedConfigs", validation.validatedConfigs());
        data.put("writePlanOnly", validation.writePlanOnly());
        return data;
    }

    private static Map<String, Object> configWritePlan(String packId, EchoNativeConfigWritePlan writePlan, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m11_config_write_plan", diagnostics);
        data.put("filesystemMutated", writePlan.filesystemMutated());
        data.put("fixtureConfigMutated", writePlan.fixtureConfigMutated());
        data.put("installedConfigMutated", writePlan.installedConfigMutated());
        data.put("packId", packId);
        data.put("planId", writePlan.planId());
        data.put("plannedWriteCount", writePlan.plannedWriteCount());
        data.put("plannedWrites", writePlan.plannedWrites());
        data.put("ready", writePlan.ready());
        data.put("summary", writePlan.ready()
                ? "Config writes were planned as planned:// targets only; no config files were mutated."
                : "Config write plan is blocked by diagnostics.");
        data.put("writePlanOnly", writePlan.writePlanOnly());
        return data;
    }

    private static Map<String, Object> configSafetyStatus(String packId, EchoNativeConfigSafetyStatus status, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = base("phase13_m11_config_safety_status", diagnostics);
        data.put("addonCodeExecuted", status.addonCodeExecuted());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("fixtureConfigMutated", status.fixtureConfigMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("installedConfigMutated", status.installedConfigMutated());
        data.put("localOnly", status.localOnly());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("safeToContinue", status.safeToContinue());
        data.put("serviceCodeExecuted", status.serviceCodeExecuted());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M11 config prototype stayed local, planned-only, and safe to continue."
                : "M11 config prototype is blocked by diagnostics.");
        data.put("writePlanOnly", status.writePlanOnly());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("addonCodeExecuted", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("minecraftLaunched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("serviceCodeExecuted", false);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String missingCode,
            String missingTitle,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    missingCode,
                    EchoNativeIssueSeverity.ERROR,
                    missingTitle,
                    "Required M11 config prototype input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M10 service bus reports before config prototyping."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static boolean isUnsafeRelativePath(String path) {
        return path.isBlank()
                || path.startsWith("/")
                || path.matches("^[A-Za-z]:.*")
                || path.contains("..")
                || path.startsWith("~");
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }

    private record ConfigManifest(List<Map<String, Object>> configSources) {
    }
}
