package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeModuleDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeTransformCompatibilityPolicy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeValidator {
    public enum ValidationMode {
        COMPATIBILITY,
        RELEASE
    }

    public List<EchoNativeDiagnostic> validate(EchoNativeScanResult scanResult) {
        return validate(scanResult, ValidationMode.COMPATIBILITY);
    }

    public List<EchoNativeDiagnostic> validate(EchoNativeScanResult scanResult, ValidationMode validationMode) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(scanResult.diagnostics());
        if (scanResult.packProfile() == null) {
            return List.copyOf(diagnostics);
        }
        ValidationMode mode = validationMode == null ? ValidationMode.COMPATIBILITY : validationMode;
        String packId = scanResult.packProfile().id();
        Map<String, List<EchoNativeAddonDescriptor>> byId = new HashMap<>();
        Set<String> providedFeatures = new HashSet<>();
        for (EchoNativeAddonDescriptor descriptor : scanResult.descriptors()) {
            byId.computeIfAbsent(descriptor.id(), ignored -> new ArrayList<>()).add(descriptor);
            providedFeatures.addAll(descriptor.provides());
            validateDescriptor(packId, descriptor, mode, diagnostics);
        }
        for (Map.Entry<String, List<EchoNativeAddonDescriptor>> entry : byId.entrySet()) {
            if (entry.getValue().size() > 1) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-MODULE-DUPLICATE",
                        EchoNativeIssueSeverity.ERROR,
                        "Duplicate module id",
                        "Module id '" + entry.getKey() + "' appears " + entry.getValue().size() + " times in the fixture.",
                        entry.getKey(),
                        packId,
                        entry.getValue().stream().map(descriptor -> descriptor.descriptorPath().toString().replace('\\', '/')).toList(),
                        "Keep one descriptor per module id."
                ));
            }
        }
        if (scanResult.packProfile().rootModule().isBlank() || !byId.containsKey(scanResult.packProfile().rootModule())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ROOT-MODULE-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Root module missing",
                    "Root module '" + scanResult.packProfile().rootModule() + "' was not discovered.",
                    scanResult.packProfile().rootModule(),
                    packId,
                    List.of(scanResult.packProfile().profilePath().toString().replace('\\', '/')),
                    "Add the root module descriptor to the fixture or correct rootModule."
            ));
        }
        for (String requiredModule : scanResult.packProfile().requiredModules()) {
            if (!byId.containsKey(requiredModule)) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-REQUIRED-MODULE-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Required module missing",
                        "Required module '" + requiredModule + "' was not discovered.",
                        requiredModule,
                        packId,
                        List.of(scanResult.packProfile().profilePath().toString().replace('\\', '/')),
                        "Add a descriptor fixture for the required module."
                ));
            }
        }
        for (String requiredFeature : scanResult.packProfile().requiredFeatures()) {
            if (!providedFeatures.contains(requiredFeature)) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-REQUIRED-FEATURE-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Required feature missing",
                        "Required feature '" + requiredFeature + "' has no discovered provider.",
                        null,
                        packId,
                        List.of(scanResult.packProfile().profilePath().toString().replace('\\', '/')),
                        "Add a provider descriptor or adjust the pack profile."
                ));
            }
        }
        return List.copyOf(diagnostics);
    }

    private static void validateDescriptor(
            String packId,
            EchoNativeAddonDescriptor descriptor,
            ValidationMode validationMode,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (!"echo.mod.v1".equals(descriptor.schema())) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DESCRIPTOR-SCHEMA",
                    EchoNativeIssueSeverity.ERROR,
                    "Unsupported descriptor schema",
                    "Descriptor '" + descriptor.id() + "' uses schema '" + descriptor.schema() + "'.",
                    descriptor.id(),
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Use schema echo.mod.v1 for native loader descriptors."
            ));
        }
        if (descriptor.id().isBlank()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DESCRIPTOR-ID-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Descriptor id missing",
                    "A descriptor is missing id.",
                    null,
                    packId,
                    List.of(descriptor.descriptorPath().toString().replace('\\', '/')),
                    "Add a stable module id."
            ));
        }
        validateNativeEntrypointAccess(packId, descriptor, validationMode, diagnostics);
        diagnostics.addAll(EchoNativeTransformCompatibilityPolicy.evaluate(packId, descriptor).diagnostics());
    }

    private static void validateNativeEntrypointAccess(
            String packId,
            EchoNativeAddonDescriptor descriptor,
            ValidationMode validationMode,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
        String nativeEntrypoint = string(access.get("nativeEntrypoint"));
        if (!nativeEntrypoint.isBlank()) {
            validateResolvedNativeClasspath(packId, descriptor, nativeEntrypoint, validationMode, diagnostics);
        }
    }

    private static void validateResolvedNativeClasspath(
            String packId,
            EchoNativeAddonDescriptor descriptor,
            String nativeEntrypoint,
            ValidationMode validationMode,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        EchoNativeModuleDescriptor moduleDescriptor = EchoNativeModuleDescriptor.fromAddon(descriptor);
        if (validationMode == ValidationMode.RELEASE) {
            validateReleaseNativeClasspath(packId, descriptor, nativeEntrypoint, moduleDescriptor, diagnostics);
        }
        List<Path> classpath = moduleDescriptor.classpath();
        List<String> missingEntries = classpath.stream()
                .filter(path -> !Files.exists(path))
                .map(EchoNativeValidator::path)
                .toList();
        boolean hasExistingEntry = classpath.stream().anyMatch(Files::exists);
        if (!hasExistingEntry) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CLASSPATH-UNRESOLVED",
                    EchoNativeIssueSeverity.ERROR,
                    "Native entrypoint classpath resolved to no existing runtime entries",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '" + nativeEntrypoint
                            + "', but its nativeClasspath did not resolve to any existing classpath entries.",
                    descriptor.id(),
                    packId,
                    diagnosticSources(descriptor, classpath),
                    "Build the module outputs or replace the inferred token with explicit existing artifact paths."
            ));
            return;
        }
        if (!missingEntries.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CLASSPATH-ENTRY-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Native entrypoint classpath contains missing runtime entries",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '" + nativeEntrypoint
                            + "', but some nativeClasspath entries do not exist: " + String.join(", ", missingEntries),
                    descriptor.id(),
                    packId,
                    diagnosticSources(descriptor, classpath),
                    "Remove stale nativeClasspath entries or point them at existing class directories or jars."
            ));
        }
    }

    private static void validateReleaseNativeClasspath(
            String packId,
            EchoNativeAddonDescriptor descriptor,
            String nativeEntrypoint,
            EchoNativeModuleDescriptor moduleDescriptor,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<Path> releaseClasspath = releaseClasspath(moduleDescriptor);
        if (releaseClasspath.isEmpty()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLASSPATH-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Release native entrypoint is missing resolved classpath",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '" + nativeEntrypoint
                            + "', but no release native classpath entries could be resolved.",
                    descriptor.id(),
                    packId,
                    diagnosticSources(descriptor, moduleDescriptor.generatedClasspath()),
                    "Declare packaged artifacts, use "
                            + EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN
                            + ", or package the module so the scanner can resolve module output/artifact paths."
            ));
        }
        if (moduleDescriptor.compatibilityClasspathFallback()) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-RELEASE-CLASSPATH-FALLBACK",
                    EchoNativeIssueSeverity.ERROR,
                    "Release native entrypoint depends on compatibility classpath fallback",
                    "Module '" + descriptor.id() + "' declares nativeEntrypoint '" + nativeEntrypoint
                            + "', but its classpath resolution used undeclared or unresolved fallback entries.",
                    descriptor.id(),
                    packId,
                    diagnosticSources(descriptor, moduleDescriptor.classpath()),
                    "Declare explicit packaged artifact paths or "
                            + EchoNativeModuleDescriptor.INFERRED_CLASSPATH_TOKEN
                            + " resolving to existing module output/artifact paths, then rerun release validation."
            ));
        }
    }

    private static List<Path> releaseClasspath(EchoNativeModuleDescriptor moduleDescriptor) {
        return moduleDescriptor.declaredClasspath().isEmpty()
                ? moduleDescriptor.generatedClasspath()
                : moduleDescriptor.declaredClasspath();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String descriptorPath(EchoNativeAddonDescriptor descriptor) {
        return descriptor.descriptorPath() == null ? "" : descriptor.descriptorPath().toString().replace('\\', '/');
    }

    private static List<String> diagnosticSources(EchoNativeAddonDescriptor descriptor, List<Path> classpath) {
        List<String> sources = new ArrayList<>();
        sources.add(descriptorPath(descriptor));
        classpath.stream()
                .map(EchoNativeValidator::path)
                .forEach(sources::add);
        return List.copyOf(sources);
    }

    private static String path(Path path) {
        return path == null ? "" : path.toString().replace('\\', '/');
    }
}
