package dev.echo.nativeplatform.samples;

import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleEntrypoint;
import dev.echo.nativeplatform.contracts.EchoNativeModuleLoadContext;

import java.util.Map;

public final class AshfallNativeSampleModule implements EchoNativeModuleEntrypoint {
    @Override
    public void registerServices(EchoNativeModuleLoadContext context) {
        context.registerService(
                "ashfall.first_spawn",
                new SampleService("ashfall.first_spawn"),
                "events",
                "inventory",
                "save_data",
                "hud"
        );
        context.registerService(
                "ashfall.drop_pod",
                new SampleService("ashfall.drop_pod"),
                "world_blocks",
                "structures",
                "events"
        );
    }

    @Override
    public void registerContent(EchoNativeModuleLoadContext context) {
        context.registerService(
                "content.item.echoashfallprotocol.drop_pod_beacon",
                Map.of("type", "item", "id", "echoashfallprotocol:drop_pod_beacon"),
                "inventory"
        );
        context.registerService(
                "content.block.echoashfallprotocol.drop_pod_marker",
                Map.of("type", "block", "id", "echoashfallprotocol:drop_pod_marker"),
                "world_blocks"
        );
    }

    @Override
    public void ready(EchoNativeModuleLoadContext context) {
        context.recordMutation(
                "metadata",
                "sample_module_ready",
                context.descriptor().id(),
                EchoNativeLoadStatus.MUTATED
        );
    }

    @Override
    public void shutdown(EchoNativeModuleLoadContext context) {
        context.recordMutation(
                "lifecycle",
                "shutdown",
                context.descriptor().id(),
                EchoNativeLoadStatus.MUTATED
        );
    }

    public record SampleService(String id) {
    }
}
