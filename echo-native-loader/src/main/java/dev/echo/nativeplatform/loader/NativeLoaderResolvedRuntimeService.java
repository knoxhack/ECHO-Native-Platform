package dev.echo.nativeplatform.loader;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NativeLoaderResolvedRuntimeService(
        String moduleId,
        String serviceId,
        String implementationClass,
        String serviceInstanceClass,
        boolean serviceInstanceAttached,
        List<String> surfaces
) {
    public String moduleServiceKey() {
        return String.valueOf(moduleId) + "::" + String.valueOf(serviceId);
    }

    public Map<String, Object> toReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("serviceId", serviceId);
        report.put("moduleServiceKey", moduleServiceKey());
        report.put("implementationClass", implementationClass);
        report.put("serviceInstanceClass", serviceInstanceClass);
        report.put("serviceInstanceAttached", serviceInstanceAttached);
        report.put("surfaces", surfaces);
        return Map.copyOf(report);
    }
}
