package dev.echo.nativeplatform.loader;

public final class NativeLoaderInventoryMutationSupport {
    public static final String SERVICE_ID = "echo.native.inventory_mutation_support";

    private NativeLoaderInventoryMutationSupport() {
    }

    public static Object heldItemStack(Context context, Object player, Object handOrStack) {
        if (handOrStack != null && handOrStack.getClass().getName().equals(context.runtimeClass("world.item.ItemStack"))) {
            return handOrStack;
        }
        if (player != null && handOrStack != null) {
            try {
                return player.getClass().getMethod("getItemInHand", handOrStack.getClass()).invoke(player, handOrStack);
            } catch (Throwable ignored) {
                // Fall back to the main hand below.
            }
        }
        Object stack = context.methodValue(player, "getMainHandItem");
        return stack == null ? context.methodValue(player, "getOffhandItem") : stack;
    }

    public static boolean shrinkItemStack(Context context, Object stack, Object player, int count) {
        if (stack == null || isCreativePlayer(context, player)) {
            return false;
        }
        try {
            stack.getClass().getMethod("shrink", int.class).invoke(stack, Math.max(1, count));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean damageOrShrinkItemStack(Context context, Object stack, Object player, Object handOrStack, int amount) {
        if (stack == null || isCreativePlayer(context, player)) {
            return false;
        }
        try {
            int maxDamage = ((Number) stack.getClass().getMethod("getMaxDamage").invoke(stack)).intValue();
            int damage = ((Number) stack.getClass().getMethod("getDamageValue").invoke(stack)).intValue();
            if (maxDamage > 0 && damage + amount < maxDamage) {
                stack.getClass().getMethod("setDamageValue", int.class).invoke(stack, damage + amount);
                return true;
            }
        } catch (Throwable ignored) {
            // Fall through to consume when durability APIs are unavailable.
        }
        return shrinkItemStack(context, stack, player, 1);
    }

    public static boolean isCreativePlayer(Context context, Object player) {
        Object creative = context.methodValue(player, "isCreative");
        if (creative instanceof Boolean value) {
            return value;
        }
        Object abilities = context.methodValue(player, "getAbilities");
        if (abilities != null) {
            try {
                return Boolean.TRUE.equals(context.fieldValue(abilities, "instabuild"));
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean consumeAnyItem(Context context, Object player, int count, String... itemIds) {
        return !consumeFirstItem(context, player, count, itemIds).isBlank();
    }

    public static String firstItem(Context context, Object player, int count, String... itemIds) {
        if (itemIds == null) {
            return "";
        }
        for (String itemId : itemIds) {
            if (hasItem(context, player, itemId, count)) {
                return lowerContentId(itemId);
            }
        }
        return "";
    }

    public static String consumeFirstItem(Context context, Object player, int count, String... itemIds) {
        if (itemIds == null) {
            return "";
        }
        for (String itemId : itemIds) {
            if (consumeItem(context, player, itemId, count)) {
                return lowerContentId(itemId);
            }
        }
        return "";
    }

    public static boolean hasAnyItem(Context context, Object player, String... itemIds) {
        if (itemIds == null) {
            return false;
        }
        for (String itemId : itemIds) {
            if (hasItem(context, player, itemId, 1)) {
                return true;
            }
        }
        return false;
    }

    public static boolean consumeItem(Context context, Object player, String itemId, int count) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        if (isCreativePlayer(context, player) && hasItem(context, player, itemId, 1)) {
            return true;
        }
        if (!hasItem(context, player, itemId, count)) {
            return false;
        }
        try {
            Object item = context.registryValue("ITEM", itemId);
            if (item == null) {
                return false;
            }
            Object inventory = context.methodValue(player, "getInventory");
            if (inventory == null) {
                return false;
            }
            int remaining = Math.max(1, count);
            int size = context.intMethod(inventory, "getContainerSize");
            for (int index = 0; index < size && remaining > 0; index++) {
                Object stack = inventory.getClass().getMethod("getItem", int.class).invoke(inventory, index);
                if (!stackMatchesItem(context, stack, item)) {
                    continue;
                }
                int stackCount = context.intMethod(stack, "getCount");
                int removed = Math.min(stackCount, remaining);
                if (removed > 0) {
                    stack.getClass().getMethod("shrink", int.class).invoke(stack, removed);
                    remaining -= removed;
                }
            }
            return remaining == 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean hasItem(Context context, Object player, String itemId, int count) {
        try {
            Object item = context.registryValue("ITEM", itemId);
            Object inventory = context.methodValue(player, "getInventory");
            if (item == null || inventory == null) {
                return false;
            }
            int found = 0;
            int size = context.intMethod(inventory, "getContainerSize");
            for (int index = 0; index < size; index++) {
                Object stack = inventory.getClass().getMethod("getItem", int.class).invoke(inventory, index);
                if (stackMatchesItem(context, stack, item)) {
                    found += context.intMethod(stack, "getCount");
                    if (found >= Math.max(1, count)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    private static boolean stackMatchesItem(Context context, Object stack, Object item) {
        if (stack == null || item == null) {
            return false;
        }
        Object stackItem = context.methodValue(stack, "getItem");
        return stackItem == item || (stackItem != null && stackItem.equals(item));
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static final class Context {
        private final RuntimeClassResolver runtimeClassResolver;
        private final MethodReader methodReader;
        private final FieldReader fieldReader;
        private final RegistryValueLookup registryValueLookup;
        private final IntMethodReader intMethodReader;

        public Context(
                RuntimeClassResolver runtimeClassResolver,
                MethodReader methodReader,
                FieldReader fieldReader,
                RegistryValueLookup registryValueLookup,
                IntMethodReader intMethodReader
        ) {
            this.runtimeClassResolver = runtimeClassResolver;
            this.methodReader = methodReader;
            this.fieldReader = fieldReader;
            this.registryValueLookup = registryValueLookup;
            this.intMethodReader = intMethodReader;
        }

        private String runtimeClass(String suffix) {
            return runtimeClassResolver.resolve(suffix);
        }

        private Object methodValue(Object target, String methodName) {
            return methodReader.get(target, methodName);
        }

        private Object fieldValue(Object target, String fieldName) throws IllegalAccessException {
            return fieldReader.get(target, fieldName);
        }

        private Object registryValue(String registryField, String contentId) throws ReflectiveOperationException {
            return registryValueLookup.get(registryField, contentId);
        }

        private int intMethod(Object target, String methodName) throws ReflectiveOperationException {
            return intMethodReader.get(target, methodName);
        }
    }

    @FunctionalInterface
    public interface RuntimeClassResolver {
        String resolve(String suffix);
    }

    @FunctionalInterface
    public interface MethodReader {
        Object get(Object target, String methodName);
    }

    @FunctionalInterface
    public interface FieldReader {
        Object get(Object target, String fieldName) throws IllegalAccessException;
    }

    @FunctionalInterface
    public interface RegistryValueLookup {
        Object get(String registryField, String contentId) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface IntMethodReader {
        int get(Object target, String methodName) throws ReflectiveOperationException;
    }
}
