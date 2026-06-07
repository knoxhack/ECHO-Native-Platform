package dev.echo.nativeplatform.loader;

public final class NativeLoaderContentActionFlow {
    public static final String SERVICE_ID = "echo.native.content_action_flow";

    private final NativeLoaderContentFallbackResolver contentFallback;
    private final NativeLoaderInventoryActionSupport.Context inventoryActionContext;
    private final NativeLoaderInventoryMutationSupport.Context inventoryMutationContext;
    private final NativeLoaderWorldBlockSetter.Context worldBlockSetterContext;
    private final ItemGranter itemGranter;

    public NativeLoaderContentActionFlow(
            NativeLoaderContentFallbackResolver contentFallback,
            NativeLoaderInventoryActionSupport.Context inventoryActionContext,
            NativeLoaderInventoryMutationSupport.Context inventoryMutationContext,
            NativeLoaderWorldBlockSetter.Context worldBlockSetterContext,
            ItemGranter itemGranter
    ) {
        this.contentFallback = contentFallback;
        this.inventoryActionContext = inventoryActionContext;
        this.inventoryMutationContext = inventoryMutationContext;
        this.worldBlockSetterContext = worldBlockSetterContext;
        this.itemGranter = itemGranter;
    }

    public Object heldItemStack(Object player, Object handOrStack) {
        return NativeLoaderInventoryMutationSupport.heldItemStack(inventoryMutationContext, player, handOrStack);
    }

    public boolean healPlayer(Object player, float amount) {
        try {
            player.getClass().getMethod("heal", float.class).invoke(player, amount);
            return true;
        } catch (Throwable ignored) {
            return executeCommand(player, "effect give @s minecraft:instant_health 1 0 true");
        }
    }

    public boolean damageOrShrinkItemStack(Object stack, Object player, Object handOrStack, int amount) {
        return NativeLoaderInventoryMutationSupport.damageOrShrinkItemStack(
                inventoryMutationContext,
                stack,
                player,
                handOrStack,
                amount
        );
    }

    public boolean isCreativePlayer(Object player) {
        return NativeLoaderInventoryMutationSupport.isCreativePlayer(inventoryMutationContext, player);
    }

    public String firstItem(Object player, int count, String... itemIds) {
        return NativeLoaderInventoryMutationSupport.firstItem(inventoryMutationContext, player, count, itemIds);
    }

    public boolean hasItem(Object player, String itemId, int count) {
        return NativeLoaderInventoryMutationSupport.hasItem(inventoryMutationContext, player, itemId, count);
    }

    public boolean giveAnyItem(Object player, String first, String fallback) {
        return NativeLoaderInventoryActionSupport.giveAnyItem(inventoryActionContext, player, first, fallback);
    }

    public boolean giveItem(Object player, String itemId, int count) {
        return itemGranter.grant(player, itemId, count);
    }

    public boolean giveNamespaceItem(Object player, String namespace, String... hints) {
        return NativeLoaderInventoryActionSupport.giveNamespaceItem(inventoryActionContext, player, namespace, hints);
    }

    public boolean executeCommand(Object player, String command) {
        return NativeLoaderInventoryActionSupport.executeCommand(inventoryActionContext, player, command);
    }

    public boolean setBlock(Object level, int x, int y, int z, String blockId) {
        return NativeLoaderWorldBlockSetter.set(worldBlockSetterContext, level, x, y, z, blockId);
    }

    public boolean setBlockNear(Object level, Object pos, int dx, int dy, int dz, String blockId) {
        return NativeLoaderWorldBlockSetter.setNear(worldBlockSetterContext, level, pos, dx, dy, dz, blockId);
    }

    public boolean setAnyBlockNear(Object level, Object pos, int dx, int dy, int dz, String first, String fallback) {
        return NativeLoaderWorldBlockSetter.setAnyNear(
                worldBlockSetterContext,
                level,
                pos,
                dx,
                dy,
                dz,
                first,
                fallback
        );
    }

    public boolean placeNamespaceBlockNear(
            Object level,
            Object pos,
            int dx,
            int dy,
            int dz,
            String namespace,
            String... hints
    ) {
        String safeNamespace = lowerContentId(namespace);
        if (safeNamespace.contains(":")) {
            safeNamespace = namespaceOf(safeNamespace);
        }
        if (safeNamespace.isBlank()) {
            return false;
        }
        if (hints != null) {
            for (String hint : hints) {
                if (tryPlaceNamespaceBlockNear(level, pos, dx, dy, dz, safeNamespace, hint)) {
                    return true;
                }
            }
        }
        return tryPlaceNamespaceBlockNear(level, pos, dx, dy, dz, safeNamespace, "");
    }

    private boolean tryPlaceNamespaceBlockNear(
            Object level,
            Object pos,
            int dx,
            int dy,
            int dz,
            String namespace,
            String hint
    ) {
        String resolved = contentFallback.discoverContentId(contentFallback.cachedBlockIds(), namespace, hint);
        return !resolved.isBlank() && setBlockNear(level, pos, dx, dy, dz, resolved);
    }

    @FunctionalInterface
    public interface ItemGranter {
        boolean grant(Object player, String itemId, int count);
    }

    private static String namespaceOf(String contentId) {
        String[] parts = splitContentId(contentId);
        return lowerContentId(parts[0]);
    }

    private static String[] splitContentId(String contentId) {
        String id = lowerContentId(contentId);
        int separator = id.indexOf(':');
        if (separator < 0) {
            return new String[]{"minecraft", id};
        }
        return new String[]{id.substring(0, separator), id.substring(separator + 1)};
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
