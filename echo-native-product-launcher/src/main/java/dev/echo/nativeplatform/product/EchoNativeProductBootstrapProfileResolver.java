package dev.echo.nativeplatform.product;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeProductBootstrapProfileResolver {
    private EchoNativeProductBootstrapProfileResolver() {
    }

    static EchoNativeProductLauncher.EchoNativeProductBootstrapProfileReport resolve(
            EchoNativePackProfile packProfile,
            List<EchoNativeAddonDescriptor> descriptors
    ) {
        if (packProfile == null) {
            return EchoNativeProductLauncher.EchoNativeProductBootstrapProfileReport.unavailable("");
        }
        List<EchoNativeAddonDescriptor> safeDescriptors = descriptors == null ? List.of() : List.copyOf(descriptors);
        Map<String, EchoNativeAddonDescriptor> byId = new LinkedHashMap<>();
        for (EchoNativeAddonDescriptor descriptor : safeDescriptors) {
            byId.put(descriptor.id(), descriptor);
        }

        String rootModuleId = clean(packProfile.rootModule());
        EchoNativeAddonDescriptor descriptor = rootModuleId.isBlank() ? null : byId.get(rootModuleId);
        if (descriptor == null) {
            descriptor = firstPackRoot(safeDescriptors);
            if (rootModuleId.isBlank() && descriptor != null) {
                rootModuleId = clean(descriptor.id());
            }
        }
        if (descriptor == null) {
            descriptor = firstWithBootstrapProfile(safeDescriptors);
            if (rootModuleId.isBlank() && descriptor != null) {
                rootModuleId = clean(descriptor.id());
            }
        }

        String bootstrapProfileClass = descriptor == null ? "" : nativeBootstrapProfile(descriptor);
        String descriptorId = descriptor == null ? "" : clean(descriptor.id());
        String descriptorPath = descriptor == null || descriptor.descriptorPath() == null
                ? ""
                : descriptor.descriptorPath().toString().replace('\\', '/');
        return new EchoNativeProductLauncher.EchoNativeProductBootstrapProfileReport(
                clean(packProfile.id()),
                rootModuleId,
                bootstrapProfileClass,
                descriptorId,
                descriptorPath,
                !rootModuleId.isBlank()
                        && !descriptorId.isBlank()
                        && rootModuleId.equals(descriptorId)
                        && !bootstrapProfileClass.isBlank()
        );
    }

    private static EchoNativeAddonDescriptor firstPackRoot(List<EchoNativeAddonDescriptor> descriptors) {
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            if ("pack_root".equals(clean(descriptor.kind())) || "official_pack".equals(clean(descriptor.role()))) {
                return descriptor;
            }
        }
        return null;
    }

    private static EchoNativeAddonDescriptor firstWithBootstrapProfile(List<EchoNativeAddonDescriptor> descriptors) {
        for (EchoNativeAddonDescriptor descriptor : descriptors) {
            if (!nativeBootstrapProfile(descriptor).isBlank()) {
                return descriptor;
            }
        }
        return null;
    }

    private static String nativeBootstrapProfile(EchoNativeAddonDescriptor descriptor) {
        Map<String, Object> access = descriptor.access() == null ? Map.of() : descriptor.access();
        Object value = access.get("nativeBootstrapProfile");
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
