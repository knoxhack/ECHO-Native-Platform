package dev.echo.nativeplatform.product;

import java.util.List;
import java.util.Map;

public record EchoNativeProductHookPlan(
        List<RegistryHook> registryHooks,
        List<LifecycleHook> lifecycleHooks,
        List<EventSubscriptionHook> eventSubscriptions,
        List<EventPublishHook> eventsToPublish,
        List<CommandHook> commandHooks,
        List<NetworkHook> networkHooks,
        List<ResourceHook> resourceHooks,
        List<ConfigHook> configHooks,
        List<RuntimeHook> runtimeHooks,
        List<ClientSurfaceHook> clientSurfaceHooks,
        List<ProductWorldHook> productWorldHooks,
        List<ProductOnboardingHook> productOnboardingHooks,
        List<SaveDataHook> saveDataHooks
) {
    private static final EchoNativeProductHookPlan EMPTY = new EchoNativeProductHookPlan(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
    );

    public EchoNativeProductHookPlan {
        registryHooks = registryHooks == null ? List.of() : List.copyOf(registryHooks);
        lifecycleHooks = lifecycleHooks == null ? List.of() : List.copyOf(lifecycleHooks);
        eventSubscriptions = eventSubscriptions == null ? List.of() : List.copyOf(eventSubscriptions);
        eventsToPublish = eventsToPublish == null ? List.of() : List.copyOf(eventsToPublish);
        commandHooks = commandHooks == null ? List.of() : List.copyOf(commandHooks);
        networkHooks = networkHooks == null ? List.of() : List.copyOf(networkHooks);
        resourceHooks = resourceHooks == null ? List.of() : List.copyOf(resourceHooks);
        configHooks = configHooks == null ? List.of() : List.copyOf(configHooks);
        runtimeHooks = runtimeHooks == null ? List.of() : List.copyOf(runtimeHooks);
        clientSurfaceHooks = clientSurfaceHooks == null ? List.of() : List.copyOf(clientSurfaceHooks);
        productWorldHooks = productWorldHooks == null ? List.of() : List.copyOf(productWorldHooks);
        productOnboardingHooks = productOnboardingHooks == null ? List.of() : List.copyOf(productOnboardingHooks);
        saveDataHooks = saveDataHooks == null ? List.of() : List.copyOf(saveDataHooks);
    }

    public EchoNativeProductHookPlan(
            List<RegistryHook> registryHooks,
            List<LifecycleHook> lifecycleHooks,
            List<EventSubscriptionHook> eventSubscriptions,
            List<EventPublishHook> eventsToPublish,
            List<CommandHook> commandHooks,
            List<NetworkHook> networkHooks,
            List<ResourceHook> resourceHooks,
            List<ConfigHook> configHooks,
            List<ClientSurfaceHook> clientSurfaceHooks,
            List<ProductWorldHook> productWorldHooks,
            List<ProductOnboardingHook> productOnboardingHooks,
            List<SaveDataHook> saveDataHooks
    ) {
        this(
                registryHooks,
                lifecycleHooks,
                eventSubscriptions,
                eventsToPublish,
                commandHooks,
                networkHooks,
                resourceHooks,
                configHooks,
                List.of(),
                clientSurfaceHooks,
                productWorldHooks,
                productOnboardingHooks,
                saveDataHooks
        );
    }

    public static EchoNativeProductHookPlan empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return registryHooks.isEmpty()
                && lifecycleHooks.isEmpty()
                && eventSubscriptions.isEmpty()
                && eventsToPublish.isEmpty()
                && commandHooks.isEmpty()
                && networkHooks.isEmpty()
                && resourceHooks.isEmpty()
                && configHooks.isEmpty()
                && runtimeHooks.isEmpty()
                && clientSurfaceHooks.isEmpty()
                && productWorldHooks.isEmpty()
                && productOnboardingHooks.isEmpty()
                && saveDataHooks.isEmpty();
    }

    public record RegistryHook(
            String moduleId,
            String registry,
            String id,
            Map<String, Object> properties
    ) {
        public RegistryHook {
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }
    }

    public record LifecycleHook(
            String moduleId,
            String phaseId,
            Map<String, Object> evidence
    ) {
        public LifecycleHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record EventSubscriptionHook(
            String moduleId,
            String eventId,
            String handlerId,
            Map<String, Object> evidence
    ) {
        public EventSubscriptionHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record EventPublishHook(
            String sourceModule,
            String eventId,
            Map<String, Object> payload
    ) {
        public EventPublishHook {
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    public record CommandHook(
            String moduleId,
            String commandId,
            String targetSurface,
            String targetBridge,
            Map<String, Object> evidence
    ) {
        public CommandHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record NetworkHook(
            String moduleId,
            String packetId,
            String surface,
            String sourceRuntimeTarget,
            List<String> consumers,
            Map<String, Object> evidence
    ) {
        public NetworkHook {
            consumers = consumers == null ? List.of() : List.copyOf(consumers);
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record ResourceHook(
            String moduleId,
            String resourceId,
            String resourceType,
            Map<String, Object> evidence
    ) {
        public ResourceHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record ConfigHook(
            String moduleId,
            String configId,
            String scope,
            Map<String, Object> evidence
    ) {
        public ConfigHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record RuntimeHook(
            String moduleId,
            String surface,
            String targetId,
            String action,
            Map<String, Object> payload
    ) {
        public RuntimeHook {
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }

    public record ClientSurfaceHook(
            String moduleId,
            String surfaceId,
            String surfaceType,
            Map<String, Object> config
    ) {
        public ClientSurfaceHook {
            config = config == null ? Map.of() : Map.copyOf(config);
        }
    }

    public record ProductWorldHook(
            String moduleId,
            String worldId,
            String defaultWorldMode,
            String productWorldPreset,
            String productDatapack,
            String productResourcePack,
            String vanillaSavePolicy,
            Map<String, Object> evidence
    ) {
        public ProductWorldHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record ProductOnboardingHook(
            String moduleId,
            String playerId,
            String spawnProfile,
            String spawnDimension,
            String spawnStructureId,
            String starterItemId,
            String missionId,
            String missionPhase,
            String objectiveKey,
            String hudChannel,
            String briefing,
            Map<String, Object> evidence
    ) {
        public ProductOnboardingHook {
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record SaveDataHook(
            String key,
            String value,
            boolean delete
    ) {
    }
}
