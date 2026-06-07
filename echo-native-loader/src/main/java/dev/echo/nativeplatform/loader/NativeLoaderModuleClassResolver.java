package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NativeLoaderModuleClassResolver {
    public static final String SERVICE_ID = "echo.native.module_class_resolver";

    private NativeLoaderModuleClassResolver() {
    }

    public static String resolve(EchoNativeBootstrapProductProfile profile, String namespace) {
        String ns = lowerContentId(namespace);
        if (ns.isBlank()) {
            return "";
        }
        String override = overrideFor(profile, ns);
        if (!override.isBlank()) {
            return override;
        }

        String simpleName = simpleName(ns);
        if (simpleName.isBlank()) {
            return "";
        }
        for (String candidate : candidates(ns, simpleName)) {
            if (classAvailable(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private static String overrideFor(EchoNativeBootstrapProductProfile profile, String namespace) {
        if (profile == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : profile.nativeModuleClassOverrides().entrySet()) {
            if (lowerContentId(entry.getKey()).equals(namespace)) {
                return entry.getValue() == null ? "" : entry.getValue().trim();
            }
        }
        return "";
    }

    private static List<String> candidates(String namespace, String simpleName) {
        List<String> candidates = new ArrayList<>();
        candidates.add("com.knoxhack." + namespace + "." + simpleName);
        if (namespace.startsWith("echo") && namespace.endsWith("core") && namespace.length() > 8) {
            candidates.add("com.knoxhack.echo." + namespace.substring(4) + "." + simpleName);
        }
        return candidates;
    }

    private static boolean classAvailable(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable first) {
            try {
                Class.forName(className);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    private static String simpleName(String namespace) {
        if (namespace.isBlank() || !namespace.startsWith("echo")) {
            return "";
        }
        String stem = namespace.substring(4);
        if (stem.isBlank()) {
            return "";
        }
        if (stem.endsWith("core") && stem.length() > 4) {
            return "Echo" + pascalToken(stem.substring(0, stem.length() - 4)) + "CoreNativeModule";
        }
        if (stem.endsWith("protocol") && stem.length() > 8) {
            return "Echo" + pascalToken(stem.substring(0, stem.length() - 8)) + "ProtocolNativeModule";
        }
        return "Echo" + pascalToken(stem) + "NativeModule";
    }

    private static String pascalToken(String value) {
        String safe = value.replaceAll("[^a-zA-Z0-9]+", " ").trim();
        if (safe.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : safe.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
