package dev.echo.nativeplatform.loader;

import java.util.List;

public final class NativeLoaderInventoryActionSupport {
    public static final String SERVICE_ID = "echo.native.inventory_action_support";

    private NativeLoaderInventoryActionSupport() {
    }

    public static boolean giveAnyItem(Context context, Object player, String first, String fallback) {
        String resolved = context.resolveItemId(first);
        if (!resolved.isBlank() && context.grantItem(player, resolved, 1)) {
            return true;
        }
        String fallbackResolved = context.resolveItemId(fallback);
        return !fallbackResolved.isBlank() && context.grantItem(player, fallbackResolved, 1);
    }

    public static boolean giveNamespaceItem(Context context, Object player, String namespace, String... hints) {
        String safeNamespace = lowerContentId(namespace);
        if (safeNamespace.contains(":")) {
            safeNamespace = namespaceOf(safeNamespace);
        }
        if (safeNamespace.isBlank()) {
            return false;
        }
        if (hints != null) {
            for (String hint : hints) {
                if (tryGiveNamespaceItem(context, player, safeNamespace, hint)) {
                    return true;
                }
            }
        }
        return tryGiveNamespaceItem(context, player, safeNamespace, "");
    }

    public static boolean executeCommand(Context context, Object player, String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        Object connection = null;
        try {
            connection = context.fieldValue(player, "connection");
        } catch (Throwable ignored) {
            connection = null;
        }
        if (connection == null) {
            try {
                Class<?> minecraftClass = Class.forName(context.runtimeClass("client.Minecraft"));
                Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                connection = context.methodValue(minecraft, "getConnection");
            } catch (Throwable ignored) {
                connection = null;
            }
        }
        if (connection == null) {
            return false;
        }
        try {
            connection.getClass().getMethod("sendCommand", String.class).invoke(connection, command);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryGiveNamespaceItem(Context context, Object player, String namespace, String hint) {
        String resolved = context.discoverContentId(context.itemIds(), namespace, hint);
        return !resolved.isBlank() && context.grantItem(player, resolved, 1);
    }

    private static String namespaceOf(String contentId) {
        String value = contentId == null ? "" : contentId.trim();
        int colon = value.indexOf(':');
        if (colon <= 0 || colon >= value.length() - 1) {
            return "minecraft";
        }
        return lowerContentId(value.substring(0, colon));
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static final class Context {
        private final ItemIdResolver itemIdResolver;
        private final ContentIdDiscoverer contentIdDiscoverer;
        private final ItemIdsSupplier itemIdsSupplier;
        private final ItemGranter itemGranter;
        private final FieldReader fieldReader;
        private final MethodReader methodReader;
        private final RuntimeClassResolver runtimeClassResolver;

        public Context(
                ItemIdResolver itemIdResolver,
                ContentIdDiscoverer contentIdDiscoverer,
                ItemIdsSupplier itemIdsSupplier,
                ItemGranter itemGranter,
                FieldReader fieldReader,
                MethodReader methodReader,
                RuntimeClassResolver runtimeClassResolver
        ) {
            this.itemIdResolver = itemIdResolver;
            this.contentIdDiscoverer = contentIdDiscoverer;
            this.itemIdsSupplier = itemIdsSupplier;
            this.itemGranter = itemGranter;
            this.fieldReader = fieldReader;
            this.methodReader = methodReader;
            this.runtimeClassResolver = runtimeClassResolver;
        }

        private String resolveItemId(String itemId) {
            return itemIdResolver.resolve(itemId);
        }

        private String discoverContentId(List<String> ids, String namespace, String hint) {
            return contentIdDiscoverer.discover(ids, namespace, hint);
        }

        private List<String> itemIds() {
            return itemIdsSupplier.get();
        }

        private boolean grantItem(Object player, String itemId, int count) {
            return itemGranter.grant(player, itemId, count);
        }

        private Object fieldValue(Object target, String fieldName) throws IllegalAccessException {
            return fieldReader.get(target, fieldName);
        }

        private Object methodValue(Object target, String methodName) {
            return methodReader.get(target, methodName);
        }

        private String runtimeClass(String suffix) {
            return runtimeClassResolver.resolve(suffix);
        }
    }

    @FunctionalInterface
    public interface ItemIdResolver {
        String resolve(String itemId);
    }

    @FunctionalInterface
    public interface ContentIdDiscoverer {
        String discover(List<String> ids, String namespace, String hint);
    }

    @FunctionalInterface
    public interface ItemIdsSupplier {
        List<String> get();
    }

    @FunctionalInterface
    public interface ItemGranter {
        boolean grant(Object player, String itemId, int count);
    }

    @FunctionalInterface
    public interface FieldReader {
        Object get(Object target, String fieldName) throws IllegalAccessException;
    }

    @FunctionalInterface
    public interface MethodReader {
        Object get(Object target, String methodName);
    }

    @FunctionalInterface
    public interface RuntimeClassResolver {
        String resolve(String suffix);
    }
}
