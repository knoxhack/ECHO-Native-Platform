package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeCommandService;
import dev.echo.nativeplatform.contracts.EchoNativeServiceRegistry;

import java.util.List;
import java.util.Map;

public final class EchoNativeServiceCollisionGateMain {
    private EchoNativeServiceCollisionGateMain() {
    }

    public static void main(String[] args) {
        requireSameModuleDuplicateRejected();
        requireCrossModuleServiceIdCollisionRejected();
        requireTypedCrossModuleServiceIdCollisionRejected();
        requireEventSubscriptionCollisionRejected();
        System.out.println("native service collision gate PASS");
    }

    private static void requireSameModuleDuplicateRejected() {
        EchoNativeServiceRegistry registry = new EchoNativeServiceRegistry();
        registry.register("module_a", "shared.service", new Object(), List.of("commands"));
        requireThrows(
                () -> registry.register("module_a", "shared.service", new Object(), List.of("events")),
                "Duplicate native service registration for module_a:shared.service"
        );
    }

    private static void requireCrossModuleServiceIdCollisionRejected() {
        EchoNativeServiceRegistry registry = new EchoNativeServiceRegistry();
        registry.register("module_a", "shared.service", new Object(), List.of("commands"));
        requireThrows(
                () -> registry.register("module_b", "shared.service", new Object(), List.of("events")),
                "Duplicate native service id collision for shared.service between modules module_a and module_b"
        );
    }

    private static void requireTypedCrossModuleServiceIdCollisionRejected() {
        EchoNativeServiceRegistry registry = new EchoNativeServiceRegistry();
        registry.registerTyped(
                "module_a",
                "commands.shared",
                new NoopCommandService(),
                EchoNativeCommandService.class,
                List.of("commands")
        );
        requireThrows(
                () -> registry.registerTyped(
                        "module_b",
                        "commands.shared",
                        new NoopCommandService(),
                        EchoNativeCommandService.class,
                        List.of("commands")
                ),
                "Duplicate native service id collision for commands.shared between modules module_a and module_b"
        );
    }

    private static void requireEventSubscriptionCollisionRejected() {
        NativeLoaderLifecycleEventHost eventHost = new NativeLoaderLifecycleEventHost();
        eventHost.subscribeDeclaredHook("module_a", "echo.event.ready", "ready_handler", Map.of());
        requireThrows(
                () -> eventHost.subscribeDeclaredHook("module_a", "echo.event.ready", "ready_handler", Map.of()),
                "Duplicate native event subscription collision for module_a:echo.event.ready:ready_handler"
        );
    }

    private static void requireThrows(Runnable action, String expectedMessage) {
        try {
            action.run();
        } catch (IllegalStateException exception) {
            require(expectedMessage.equals(exception.getMessage()),
                    "Unexpected collision diagnostic: " + exception.getMessage());
            return;
        }
        throw new IllegalStateException("Expected service collision rejection: " + expectedMessage);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class NoopCommandService implements EchoNativeCommandService {
    }
}
