package dev.echo.nativeplatform.loader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeRegistryFreezeGuardGateMain {
    private EchoNativeRegistryFreezeGuardGateMain() {
    }

    public static void main(String[] args) throws Exception {
        requireBuiltInRegistryFreezePrep();
        requireRegistryOfRegistriesFreezePrep();
        requireAsyncFreezeGuard();
        System.out.println("native registry freeze guard gate PASS");
    }

    private static void requireBuiltInRegistryFreezePrep() {
        FakeBuiltInRegistries.reset(false);

        int prepared = NativeLoaderRegistryRuntimeSupport.prepareAllBuiltInRegistriesForMinecraftFreeze(
                FakeBuiltInRegistries.class);

        require(prepared >= 3, "built-in registry prep should visit the native registries");
        requirePrepared(FakeBuiltInRegistries.BLOCK, "block");
        requirePrepared(FakeBuiltInRegistries.ITEM, "item");
        requirePrepared(FakeBuiltInRegistries.CREATIVE_MODE_TAB, "creative tab");
    }

    private static void requireRegistryOfRegistriesFreezePrep() {
        FakeBuiltInRegistries.reset(false);

        int prepared = NativeLoaderRegistryRuntimeSupport.prepareRegistryAndContentsForMinecraftFreeze(
                FakeBuiltInRegistries.REGISTRY);

        require(prepared >= 3, "registry-of-registries prep should visit nested registries");
        requirePrepared(FakeBuiltInRegistries.BLOCK, "nested block");
        requirePrepared(FakeBuiltInRegistries.ITEM, "nested item");
        requirePrepared(FakeBuiltInRegistries.CREATIVE_MODE_TAB, "nested creative tab");
    }

    private static void requireAsyncFreezeGuard() throws Exception {
        FakeBuiltInRegistries.reset(false);

        boolean started = NativeLoaderRegistryRuntimeSupport.startBuiltInRegistryFreezeGuard(
                FakeBuiltInRegistries.class,
                50L,
                1L);

        require(started, "freeze guard should start for a fresh QA JVM");
        Thread.sleep(90L);
        requirePrepared(FakeBuiltInRegistries.BLOCK, "guard block");
        requirePrepared(FakeBuiltInRegistries.ITEM, "guard item");
        requirePrepared(FakeBuiltInRegistries.CREATIVE_MODE_TAB, "guard creative tab");
    }

    private static void requirePrepared(FakeRegistry registry, String label) {
        require(registry.frozen, label + " registry should be restored to frozen before Minecraft reload");
        require(registry.unregisteredIntrusiveHolders.isEmpty(),
                label + " registry should clear unregistered intrusive holders");
        require(registry.allTags == registry.boundTags,
                label + " registry tag bindings should be preserved instead of stripped");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public static final class FakeBuiltInRegistries {
        public static final FakeRegistry BLOCK = new FakeRegistry("block");
        public static final FakeRegistry ITEM = new FakeRegistry("item");
        public static final FakeRegistry CREATIVE_MODE_TAB = new FakeRegistry("creative_mode_tab");
        public static final FakeRegistrySet REGISTRY = new FakeRegistrySet(BLOCK, ITEM, CREATIVE_MODE_TAB);

        private FakeBuiltInRegistries() {
        }

        static void reset(boolean frozen) {
            BLOCK.reset(frozen, 2);
            ITEM.reset(frozen, 1);
            CREATIVE_MODE_TAB.reset(frozen, 1);
        }
    }

    public static final class FakeRegistrySet implements Iterable<FakeRegistry> {
        private final List<FakeRegistry> registries;

        FakeRegistrySet(FakeRegistry... registries) {
            this.registries = new ArrayList<>(List.of(registries));
        }

        @Override
        public Iterator<FakeRegistry> iterator() {
            return registries.iterator();
        }
    }

    public static final class FakeRegistry {
        private final String key;
        private final Object boundTags = new Object();
        private boolean frozen;
        private Map<Object, Object> unregisteredIntrusiveHolders = new LinkedHashMap<>();
        private Object allTags = boundTags;

        FakeRegistry(String key) {
            this.key = key;
        }

        void reset(boolean frozen, int holders) {
            this.frozen = frozen;
            this.unregisteredIntrusiveHolders = new LinkedHashMap<>();
            for (int i = 0; i < holders; i++) {
                this.unregisteredIntrusiveHolders.put(key + "-holder-" + i, new Object());
            }
            this.allTags = boundTags;
        }

        @Override
        public String toString() {
            return "FakeRegistry[" + key + "]";
        }
    }
}
