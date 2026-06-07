package dev.echo.nativeplatform.contracts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EchoNativeServiceRegistry {
    private final Map<String, Object> servicesById = new LinkedHashMap<>();
    private final Map<String, Object> servicesByModuleAndId = new LinkedHashMap<>();
    private final Map<String, EchoNativeRegisteredService> descriptorsByModuleAndId = new LinkedHashMap<>();

    public <T> void registerTyped(String moduleId, String serviceId, T service, Class<T> serviceType, List<String> surfaces) {
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType is required");
        }
        if (service == null || !serviceType.isInstance(service)) {
            throw new IllegalArgumentException("service must implement " + serviceType.getName());
        }
        register(moduleId, serviceId, service, surfaces, service.getClass().getName());
    }

    public void register(String moduleId, String serviceId, Object service, List<String> surfaces) {
        register(moduleId, serviceId, service, surfaces, null);
    }

    public void register(
            String moduleId,
            String serviceId,
            Object service,
            List<String> surfaces,
            String implementationClass
    ) {
        if (moduleId == null || moduleId.isBlank()) {
            throw new IllegalArgumentException("moduleId is required");
        }
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId is required");
        }
        String key = serviceKey(moduleId, serviceId);
        if (servicesByModuleAndId.containsKey(key)) {
            throw new IllegalStateException("Duplicate native service registration for " + moduleId + ":" + serviceId);
        }
        if (servicesById.containsKey(serviceId)) {
            throw new IllegalStateException("Duplicate native service id collision for " + serviceId
                    + " between modules " + ownerForServiceId(serviceId) + " and " + moduleId);
        }
        Object value = service == null ? serviceId : service;
        String implementation = implementationClass == null || implementationClass.isBlank()
                ? value.getClass().getName()
                : implementationClass;
        servicesById.put(serviceId, value);
        servicesByModuleAndId.put(key, value);
        descriptorsByModuleAndId.put(key, new EchoNativeRegisteredService(
                moduleId,
                serviceId,
                implementation,
                surfaces == null ? List.of() : List.copyOf(surfaces)
        ));
    }

    public boolean hasService(String serviceId) {
        return servicesById.containsKey(serviceId);
    }

    public boolean hasService(String moduleId, String serviceId) {
        return servicesByModuleAndId.containsKey(serviceKey(moduleId, serviceId));
    }

    public Optional<Object> service(String serviceId) {
        return Optional.ofNullable(servicesById.get(serviceId));
    }

    public Optional<Object> service(String moduleId, String serviceId) {
        return Optional.ofNullable(servicesByModuleAndId.get(serviceKey(moduleId, serviceId)));
    }

    public <T> Optional<T> service(String serviceId, Class<T> serviceType) {
        return typed(servicesById.get(serviceId), serviceType);
    }

    public <T> Optional<T> service(String moduleId, String serviceId, Class<T> serviceType) {
        return typed(servicesByModuleAndId.get(serviceKey(moduleId, serviceId)), serviceType);
    }

    public List<EchoNativeRegisteredService> registeredServices() {
        return descriptorsByModuleAndId.values().stream()
                .sorted(Comparator.comparing(EchoNativeRegisteredService::moduleId)
                        .thenComparing(EchoNativeRegisteredService::serviceId))
                .toList();
    }

    public List<EchoNativeRegisteredService> servicesForModule(String moduleId) {
        List<EchoNativeRegisteredService> result = new ArrayList<>();
        for (EchoNativeRegisteredService service : registeredServices()) {
            if (service.moduleId().equals(moduleId)) {
                result.add(service);
            }
        }
        return List.copyOf(result);
    }

    private static String serviceKey(String moduleId, String serviceId) {
        return String.valueOf(moduleId) + "\u0000" + String.valueOf(serviceId);
    }

    private String ownerForServiceId(String serviceId) {
        for (EchoNativeRegisteredService descriptor : descriptorsByModuleAndId.values()) {
            if (descriptor.serviceId().equals(serviceId)) {
                return descriptor.moduleId();
            }
        }
        return "unknown";
    }

    private static <T> Optional<T> typed(Object service, Class<T> serviceType) {
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType is required");
        }
        if (service == null) {
            return Optional.empty();
        }
        if (!serviceType.isInstance(service)) {
            throw new ClassCastException("Native service " + service.getClass().getName()
                    + " is not assignable to " + serviceType.getName());
        }
        return Optional.of(serviceType.cast(service));
    }
}
