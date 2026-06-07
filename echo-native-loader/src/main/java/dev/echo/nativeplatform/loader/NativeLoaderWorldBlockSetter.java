package dev.echo.nativeplatform.loader;

public final class NativeLoaderWorldBlockSetter {
    public static final String SERVICE_ID = "echo.native.world_block_setter";

    private NativeLoaderWorldBlockSetter() {
    }

    public static boolean setNear(
            Context context,
            Object level,
            Object pos,
            int dx,
            int dy,
            int dz,
            String blockId
    ) {
        if (level == null || pos == null) {
            return false;
        }
        try {
            return set(
                    context,
                    level,
                    context.intMethod(pos, "getX") + dx,
                    context.intMethod(pos, "getY") + dy,
                    context.intMethod(pos, "getZ") + dz,
                    blockId
            );
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean setAnyNear(
            Context context,
            Object level,
            Object pos,
            int dx,
            int dy,
            int dz,
            String first,
            String fallback
    ) {
        String resolved = context.resolveBlockId(first);
        if (!resolved.isBlank() && setNear(context, level, pos, dx, dy, dz, resolved)) {
            return true;
        }
        String fallbackResolved = context.resolveBlockId(fallback);
        return !fallbackResolved.isBlank() && setNear(context, level, pos, dx, dy, dz, fallbackResolved);
    }

    public static boolean set(Context context, Object level, int x, int y, int z, String blockId) {
        try {
            String resolvedBlockId = context.resolveBlockId(blockId);
            if (resolvedBlockId.isBlank()) {
                resolvedBlockId = context.blockFallback(blockId);
            }
            Object block = context.registryValue("BLOCK", resolvedBlockId);
            if (block == null) {
                return false;
            }
            Object state = block.getClass().getMethod("defaultBlockState").invoke(block);
            Class<?> blockPosClass = Class.forName(context.runtimeClass("core.BlockPos"));
            Object pos = blockPosClass.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
            return invokeSetBlock(level, pos, state);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String blockPosText(Context context, Object pos) {
        if (pos == null) {
            return "";
        }
        try {
            return "@ " + context.intMethod(pos, "getX")
                    + " " + context.intMethod(pos, "getY")
                    + " " + context.intMethod(pos, "getZ");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean invokeSetBlock(Object level, Object pos, Object state) {
        if (level == null || pos == null || state == null) {
            return false;
        }
        for (java.lang.reflect.Method method : level.getClass().getMethods()) {
            if (!"setBlock".equals(method.getName())) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length < 3 || parameters.length > 4) {
                continue;
            }
            if (!parameters[0].isInstance(pos) || !parameters[1].isInstance(state)) {
                continue;
            }
            try {
                Object result = parameters.length == 3
                        ? method.invoke(level, pos, state, 3)
                        : method.invoke(level, pos, state, 3, 512);
                return !Boolean.FALSE.equals(result);
            } catch (Throwable ignored) {
                // Try the next overload before giving up.
            }
        }
        try {
            java.lang.reflect.Method setBlockAndUpdate = level.getClass().getMethod(
                    "setBlockAndUpdate",
                    pos.getClass(),
                    state.getClass()
            );
            Object result = setBlockAndUpdate.invoke(level, pos, state);
            return !Boolean.FALSE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static final class Context {
        private final BlockIdResolver blockIdResolver;
        private final BlockFallbackResolver blockFallbackResolver;
        private final RegistryValueLookup registryValueLookup;
        private final RuntimeClassResolver runtimeClassResolver;
        private final IntMethodReader intMethodReader;

        public Context(
                BlockIdResolver blockIdResolver,
                BlockFallbackResolver blockFallbackResolver,
                RegistryValueLookup registryValueLookup,
                RuntimeClassResolver runtimeClassResolver,
                IntMethodReader intMethodReader
        ) {
            this.blockIdResolver = blockIdResolver;
            this.blockFallbackResolver = blockFallbackResolver;
            this.registryValueLookup = registryValueLookup;
            this.runtimeClassResolver = runtimeClassResolver;
            this.intMethodReader = intMethodReader;
        }

        private String resolveBlockId(String blockId) {
            return blockIdResolver.resolve(blockId);
        }

        private String blockFallback(String blockId) {
            return blockFallbackResolver.resolve(blockId);
        }

        private Object registryValue(String registryField, String contentId) throws ReflectiveOperationException {
            return registryValueLookup.get(registryField, contentId);
        }

        private String runtimeClass(String suffix) {
            return runtimeClassResolver.resolve(suffix);
        }

        private int intMethod(Object target, String methodName) throws ReflectiveOperationException {
            return intMethodReader.get(target, methodName);
        }
    }

    @FunctionalInterface
    public interface BlockIdResolver {
        String resolve(String blockId);
    }

    @FunctionalInterface
    public interface BlockFallbackResolver {
        String resolve(String blockId);
    }

    @FunctionalInterface
    public interface RegistryValueLookup {
        Object get(String registryField, String contentId) throws ReflectiveOperationException;
    }

    @FunctionalInterface
    public interface RuntimeClassResolver {
        String resolve(String suffix);
    }

    @FunctionalInterface
    public interface IntMethodReader {
        int get(Object target, String methodName) throws ReflectiveOperationException;
    }
}
