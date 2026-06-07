package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeBridgePolicy;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeUiSurfaceRoute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderRuntimeBridgeSeed {
    private static final List<String> REQUIRED_PRODUCT_CLIENT_SURFACE_TYPES = List.of(
            "main_menu",
            "loading_screen",
            "hud",
            "terminal",
            "index",
            "lens",
            "holomap"
    );

    private final EchoNativeBootstrapProductProfile profile;
    private final String productGameplayBridgeKey;

    public NativeLoaderRuntimeBridgeSeed(
            EchoNativeBootstrapProductProfile profile,
            String productGameplayBridgeKey
    ) {
        this.profile = profile;
        this.productGameplayBridgeKey = productGameplayBridgeKey;
    }

    public Map<String, Object> create(
            String packId,
            List<String> modules,
            Map<String, String> nativeEntrypoints,
            Map<String, Object> resourceBridge,
            Map<String, Object> worldStartupBridge,
            Map<String, Object> registryBridge,
            Map<String, Object> productGameplayBridge,
            boolean nativeLoaderActive,
            String mainLabel,
            String clientLabel,
            LiveProofFactory liveProofFactory
    ) {
        Map<String, Object> bridge = new LinkedHashMap<>();
        bridge.putAll(NativeLoaderAdapterCoreMarkerFields.seedFields(
                resourceBridge,
                registryBridge,
                productGameplayBridge
        ));
        bridge.put("resourceBridge", resourceBridge);
        bridge.put("worldStartupBridge", worldStartupBridge);
        bridge.put("registryBridge", registryBridge);
        bridge.put(productGameplayBridgeKey, productGameplayBridge);
        bridge.put("eventBridge", policyMap(profile.nativeInitialEventBridgePolicy()));
        bridge.put("serviceBridge", policyMap(profile.nativeInitialServiceBridgePolicy()));
        bridge.put("creativeVisibilityBridge", policyMap(profile.nativeCreativeVisibilityBridgePolicy()));

        Map<String, Object> liveClientProbe = policyMap(profile.nativeLiveClientProbePolicy());
        liveClientProbe.put("nativeLoaderTextLabelApplied", nativeLoaderActive);
        liveClientProbe.put("mainLabelText", mainLabel);
        liveClientProbe.put("labelText", clientLabel);
        bridge.put("liveClientProbe", liveClientProbe);

        Map<String, Object> nativeClientUiBridge = policyMap(profile.nativeClientUiBridgePolicy());
        nativeClientUiBridge.putAll(productClientSurfaceContract());
        bridge.put("nativeClientUiBridge", nativeClientUiBridge);
        bridge.put("nativeLoaderLiveProof", liveProofFactory.create(
                "",
                liveClientProbe,
                nativeClientUiBridge,
                productGameplayBridge,
                object(bridge.get("serviceBridge")),
                Map.of()
        ));
        bridge.put("moduleDescriptorCount", modules.size());
        bridge.put("nativeEntrypointCount", nativeEntrypoints.size());
        bridge.put("packId", packId);
        return bridge;
    }

    private Map<String, Object> productClientSurfaceContract() {
        List<Map<String, Object>> declaredSurfaces = new ArrayList<>();
        List<String> expectedSurfaceTypes = new ArrayList<>();
        List<String> expectedSurfaceIds = new ArrayList<>();
        for (NativeUiSurfaceRoute route : profile.nativeUiSurfaceRoutes()) {
            String surfaceType = requiredProductSurfaceType(route == null ? "" : route.surface());
            if (surfaceType.isBlank()) {
                continue;
            }
            addUnique(expectedSurfaceTypes, surfaceType);
            String surfaceId = firstNonBlank(
                    route.canonicalId(),
                    route.screenId(),
                    route.target(),
                    surfaceType
            );
            addUnique(expectedSurfaceIds, surfaceId);
            Map<String, Object> surface = new LinkedHashMap<>();
            surface.put("surface", safe(route.surface()));
            surface.put("surfaceType", surfaceType);
            surface.put("screenId", safe(route.screenId()));
            surface.put("canonicalId", safe(route.canonicalId()));
            surface.put("target", safe(route.target()));
            declaredSurfaces.add(Map.copyOf(surface));
        }
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("profileClientSurfaceContractRequired", !expectedSurfaceTypes.isEmpty());
        contract.put("profileExpectedClientSurfaceCount", expectedSurfaceTypes.size());
        contract.put("profileExpectedClientSurfaceTypes", List.copyOf(expectedSurfaceTypes));
        contract.put("profileExpectedClientSurfaceIds", List.copyOf(expectedSurfaceIds));
        contract.put("profileDeclaredClientSurfaces", List.copyOf(declaredSurfaces));
        contract.put("profileClientSurfaceContractSatisfied", expectedSurfaceTypes.isEmpty());
        contract.put("profileMissingClientSurfaceTypes", List.copyOf(expectedSurfaceTypes));
        return Map.copyOf(contract);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String requiredProductSurfaceType(String surface) {
        String normalized = normalize(surface);
        String type = switch (normalized) {
            case "main_menu", "mainmenu" -> "main_menu";
            case "loading", "loading_screen" -> "loading_screen";
            case "hud" -> "hud";
            case "terminal" -> "terminal";
            case "index" -> "index";
            case "lens" -> "lens";
            case "holomap", "holo_map", "minimap" -> "holomap";
            default -> "";
        };
        return REQUIRED_PRODUCT_CLIENT_SURFACE_TYPES.contains(type) ? type : "";
    }

    private static String normalize(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean previousSeparator = false;
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                result.append(ch);
                previousSeparator = false;
            } else if (!previousSeparator) {
                result.append('_');
                previousSeparator = true;
            }
        }
        while (result.length() > 0 && result.charAt(result.length() - 1) == '_') {
            result.deleteCharAt(result.length() - 1);
        }
        return result.toString();
    }

    private static void addUnique(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static Map<String, Object> policyMap(NativeBridgePolicy policy) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (policy == null) {
            return data;
        }
        data.put("installed", policy.installed());
        data.put("applied", policy.applied());
        if (!policy.strategy().isBlank()) {
            data.put("strategy", policy.strategy());
        }
        data.put("summary", policy.summary());
        data.putAll(policy.attributes());
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    public interface LiveProofFactory {
        Map<String, Object> create(
                String realMainClass,
                Map<String, Object> liveClientProbe,
                Map<String, Object> nativeClientUiBridge,
                Map<String, Object> productGameplayBridge,
                Map<String, Object> serviceBridge,
                Map<String, Map<String, Object>> nativeActivations
        );
    }
}
