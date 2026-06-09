package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.packos.EchoNativePackProfileLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class EchoNativeDescriptorScanner {
    private final EchoNativePackProfileLoader profileLoader = new EchoNativePackProfileLoader();

    public EchoNativeScanResult scan(Path fixtureRoot) {
        LegacyFixtureAlias alias = legacyFixtureAlias(fixtureRoot);
        if (alias != null) {
            return requiredClosure(scan(alias.sourceRoot(), true, alias.packProfile()));
        }
        return scan(fixtureRoot, true);
    }

    public EchoNativeScanResult scanProduct(Path productRoot) {
        return scan(productRoot, false);
    }

    public EchoNativeScanResult scanProduct(Path productRoot, EchoNativePackProfile packProfile) {
        return scan(productRoot, false, packProfile);
    }

    private EchoNativeScanResult scan(Path fixtureRoot, boolean includeSamples) {
        return scan(fixtureRoot, includeSamples, null);
    }

    private EchoNativeScanResult scan(Path fixtureRoot, boolean includeSamples, EchoNativePackProfile profileOverride) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Path scanRoot = fixtureRoot.toAbsolutePath().normalize();
        try {
            List<Path> descriptorPaths = descriptorPaths(scanRoot, includeSamples);
            EchoNativePackProfile profile = profileOverride != null
                    ? profileOverride
                    : Files.isRegularFile(scanRoot.resolve("echo.pack.json"))
                    ? profileLoader.load(scanRoot)
                    : syntheticProfile(scanRoot);
            List<EchoNativeAddonDescriptor> descriptors = readDescriptors(scanRoot, profile, descriptorPaths, diagnostics);
            if (descriptors.isEmpty()) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-DESCRIPTOR-NONE",
                        EchoNativeIssueSeverity.ERROR,
                        "No native descriptors were discovered",
                        "The native product scanner did not find fixture modules or addon/source descriptors under the requested root.",
                        null,
                        profile.id(),
                        List.of(relative(scanRoot, scanRoot)),
                        "Point echoNativeFixture at a fixture root, the ECHO workspace root, or an addon project containing src/main/resources/META-INF/echo.mod.json."
                ));
            }
            descriptors.sort(Comparator.comparing(EchoNativeAddonDescriptor::id));
            return new EchoNativeScanResult(profile, List.copyOf(descriptors), List.copyOf(diagnostics));
        } catch (RuntimeException | IOException ex) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PACK-PROFILE-INVALID",
                    EchoNativeIssueSeverity.FATAL,
                    "Invalid pack profile",
                    ex.getMessage(),
                    null,
                    "",
                    List.of(relative(scanRoot, scanRoot.resolve("echo.pack.json"))),
                    "Add a valid echo.pack.json fixture profile or point at a source workspace/addon with echo.mod.json descriptors."
            ));
            return new EchoNativeScanResult(null, List.of(), List.copyOf(diagnostics));
        }
    }

    private static List<EchoNativeAddonDescriptor> readDescriptors(
            Path scanRoot,
            EchoNativePackProfile profile,
            List<Path> descriptorPaths,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        List<EchoNativeAddonDescriptor> descriptors = new ArrayList<>();
        for (Path descriptorPath : descriptorPaths) {
            try {
                descriptors.add(readDescriptor(descriptorPath));
            } catch (RuntimeException | IOException ex) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-DESCRIPTOR-INVALID",
                        EchoNativeIssueSeverity.ERROR,
                        "Invalid native descriptor",
                        ex.getMessage(),
                        moduleFromPath(scanRoot, descriptorPath),
                        profile.id(),
                        List.of(relative(scanRoot, descriptorPath)),
                        "Fix the source descriptor JSON before native product launch."
                ));
            }
        }
        return descriptors;
    }

    private static List<Path> descriptorPaths(Path scanRoot, boolean includeSamples) throws IOException {
        Path modulesRoot = scanRoot.resolve("modules");
        if (Files.isDirectory(modulesRoot)) {
            return findDescriptors(modulesRoot);
        }
        Path addonDescriptor = scanRoot.resolve("src/main/resources/META-INF/echo.mod.json");
        if (Files.isRegularFile(addonDescriptor)) {
            return List.of(addonDescriptor);
        }
        List<Path> workspaceDescriptors = new ArrayList<>();
        List<String> sourceRoots = includeSamples ? List.of("core", "addons", "samples") : List.of("core", "addons");
        for (String sourceRoot : sourceRoots) {
            Path root = scanRoot.resolve(sourceRoot);
            if (Files.isDirectory(root)) {
                workspaceDescriptors.addAll(findDescriptors(root));
            }
        }
        workspaceDescriptors.sort(Comparator.naturalOrder());
        return List.copyOf(workspaceDescriptors);
    }

    private static List<Path> findDescriptors(Path modulesRoot) throws IOException {
        try (Stream<Path> stream = Files.walk(modulesRoot)) {
            return stream
                    .filter(path -> path.getFileName().toString().equals("echo.mod.json"))
                    .sorted()
                    .toList();
        }
    }

    private static EchoNativeAddonDescriptor readDescriptor(Path descriptorPath) throws IOException {
        Map<String, Object> json = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(descriptorPath)));
        return new EchoNativeAddonDescriptor(
                string(json.get("schema")),
                string(json.get("id")),
                string(json.get("name")),
                string(json.get("version")),
                string(json.get("kind")),
                string(json.get("role")),
                string(json.get("entrypoint")),
                EchoNativeRuntimeSide.from(string(json.get("side"))),
                EchoNativeTrustLevel.from(string(json.get("trustLevel"))),
                EchoNativeApiStability.from(string(json.get("apiStability"))),
                bool(json.get("official")),
                bool(json.get("standalone")),
                EchoNativeJson.stringList(json.get("requires")),
                EchoNativeJson.stringList(json.get("optional")),
                EchoNativeJson.stringList(json.get("provides")),
                EchoNativeJson.stringList(json.get("consumes")),
                EchoNativeJson.stringList(json.get("transforms")),
                EchoNativeJson.asObject(json.get("access")),
                descriptorPath.normalize()
        );
    }

    private static String moduleFromPath(Path modulesRoot, Path descriptorPath) {
        Path relative = modulesRoot.toAbsolutePath().normalize().relativize(descriptorPath.toAbsolutePath().normalize());
        return relative.getNameCount() == 0 ? "" : relative.getName(0).toString();
    }

    private static EchoNativePackProfile syntheticProfile(Path scanRoot) {
        String fallbackId = scanRoot.getFileName() == null
                ? "echo_native_product"
                : scanRoot.getFileName().toString().replace('-', '_');
        return new EchoNativePackProfile(
                "echo.pack.synthetic.v1",
                fallbackId,
                fallbackId,
                "source_profile",
                fallbackId,
                "",
                "echo_native",
                "",
                List.of(),
                List.of(),
                List.of(),
                scanRoot.resolve("echo.pack.json").normalize()
        );
    }

    private static EchoNativeScanResult requiredClosure(EchoNativeScanResult result) {
        if (result.packProfile() == null || result.descriptors().isEmpty()) {
            return result;
        }
        Map<String, EchoNativeAddonDescriptor> byId = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : result.descriptors()) {
            byId.put(descriptor.id(), descriptor);
        }
        Set<String> included = new LinkedHashSet<>();
        includeRequired(result.packProfile().rootModule(), byId, included);
        for (String requiredModule : result.packProfile().requiredModules()) {
            includeRequired(requiredModule, byId, included);
        }
        List<EchoNativeAddonDescriptor> descriptors = result.descriptors().stream()
                .filter(descriptor -> included.contains(descriptor.id()))
                .toList();
        return new EchoNativeScanResult(result.packProfile(), descriptors, result.diagnostics());
    }

    private static void includeRequired(
            String moduleId,
            Map<String, EchoNativeAddonDescriptor> byId,
            Set<String> included
    ) {
        if (moduleId == null || moduleId.isBlank() || !included.add(moduleId)) {
            return;
        }
        EchoNativeAddonDescriptor descriptor = byId.get(moduleId);
        if (descriptor == null) {
            return;
        }
        for (String requiredModule : descriptor.requires()) {
            includeRequired(requiredModule, byId, included);
        }
    }

    private static LegacyFixtureAlias legacyFixtureAlias(Path fixtureRoot) {
        Path requested = fixtureRoot.toAbsolutePath().normalize();
        if (Files.isDirectory(requested.resolve("modules"))
                || Files.isRegularFile(requested.resolve("src/main/resources/META-INF/echo.mod.json"))) {
            return null;
        }
        String normalized = fixtureRoot.normalize().toString().replace('\\', '/');
        if (!"fixtures/ashfall".equals(normalized)) {
            return null;
        }
        Path modulesRoot = echoModulesRoot();
        Path sourceRoot = modulesRoot.getFileName() != null
                && "addons".equals(modulesRoot.getFileName().toString())
                && modulesRoot.getParent() != null
                ? modulesRoot.getParent()
                : modulesRoot;
        if (!Files.isRegularFile(sourceRoot.resolve("addons/echoashfallprotocol/src/main/resources/META-INF/echo.mod.json"))) {
            return null;
        }
        EchoNativePackProfile profile = new EchoNativePackProfile(
                "echo.pack.synthetic.v1",
                "ashfall",
                "ashfall",
                "source_profile",
                "echoashfallprotocol",
                "1.21.1",
                "echo_native",
                "",
                List.of("echoashfallprotocol"),
                List.of(),
                List.of(),
                requested.resolve("echo.pack.json").normalize()
        );
        return new LegacyFixtureAlias(sourceRoot, profile);
    }

    private static Path echoModulesRoot() {
        String property = System.getProperty("echo.modules.root");
        if (property != null && !property.isBlank()) {
            return Path.of(property).toAbsolutePath().normalize();
        }
        String env = System.getenv("ECHO_MODULES_ROOT");
        if (env != null && !env.isBlank()) {
            return Path.of(env).toAbsolutePath().normalize();
        }
        Path workspace = Path.of("").toAbsolutePath().normalize();
        Path splitCheckout = workspace.resolveSibling("ECHO-Modules").resolve("addons");
        if (Files.isDirectory(splitCheckout)) {
            return splitCheckout.normalize();
        }
        return workspace.resolveSibling("addons").normalize();
    }

    private record LegacyFixtureAlias(Path sourceRoot, EchoNativePackProfile packProfile) {
    }

    private static String relative(Path root, Path path) {
        try {
            return root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        } catch (IllegalArgumentException ex) {
            return path.toString().replace('\\', '/');
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean booleanValue && booleanValue;
    }
}
