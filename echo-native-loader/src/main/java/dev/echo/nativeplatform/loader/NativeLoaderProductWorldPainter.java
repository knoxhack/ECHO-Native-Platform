package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeWorldPaintPlacement;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeWorldPaintRecipe;

import java.util.List;
import java.util.Locale;

public final class NativeLoaderProductWorldPainter {
    public static final String SERVICE_ID = "echo.native.product_world_painter";

    private NativeLoaderProductWorldPainter() {
    }

    public static void paintModuleActionRoute(String paintStyle, Object level, Object pos, Context context) {
        paintStyle(paintStyle, level, pos, context);
    }

    public static void signalTrace(Object level, Object pos, Context context) {
        paintStyle("signal_trace", level, pos, context);
    }

    public static void outpostPad(Object level, Object pos, Context context) {
        paintStyle("outpost_pad", level, pos, context);
    }

    public static void wastelandPatch(Object level, Object pos, Context context) {
        paintStyle("wasteland_patch", level, pos, context);
    }

    public static void machineRig(Object level, Object pos, Context context) {
        paintStyle("machine_rig", level, pos, context);
    }

    public static void powerCircuit(Object level, Object pos, Context context) {
        paintStyle("power_circuit", level, pos, context);
    }

    public static void convoyPad(Object level, Object pos, Context context) {
        paintStyle("convoy_pad", level, pos, context);
    }

    public static void orbitalPad(Object level, Object pos, Context context) {
        paintStyle("orbital_pad", level, pos, context);
    }

    public static void greenhouse(Object level, Object pos, Context context) {
        paintStyle("greenhouse", level, pos, context);
    }

    public static void multiblockFrame(Object level, Object pos, Context context) {
        paintStyle("multiblock_frame", level, pos, context);
    }

    public static void containment(Object level, Object pos, Context context) {
        paintStyle("containment", level, pos, context);
    }

    public static void shelter(Object level, Object pos, Context context) {
        paintStyle("shelter", level, pos, context);
    }

    public static void armoryStation(Object level, Object pos, Context context) {
        paintStyle("armory_station", level, pos, context);
    }

    public static void relicSeal(Object level, Object pos, Context context) {
        paintStyle("relic_seal", level, pos, context);
    }

    public static void stationNode(Object level, Object pos, Context context) {
        paintStyle("station_node", level, pos, context);
    }

    public static void arcanaNode(Object level, Object pos, Context context) {
        paintStyle("arcana_node", level, pos, context);
    }

    public static void logisticsDepot(Object level, Object pos, Context context) {
        paintStyle("logistics_depot", level, pos, context);
    }

    private static void paintStyle(String style, Object level, Object pos, Context context) {
        if (missing(level, pos, context)) {
            return;
        }
        context.paintRecipe(style, level, pos);
    }

    private static boolean missing(Object level, Object pos, Context context) {
        return level == null || pos == null || context == null;
    }

    private static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Context {
        private final BlockSetter blockSetter;
        private final FallbackBlockSetter fallbackBlockSetter;
        private final ProductIdResolver productIdResolver;
        private final List<NativeWorldPaintRecipe> paintRecipes;

        public Context(
                BlockSetter blockSetter,
                FallbackBlockSetter fallbackBlockSetter,
                ProductIdResolver productIdResolver,
                List<NativeWorldPaintRecipe> paintRecipes
        ) {
            this.blockSetter = blockSetter;
            this.fallbackBlockSetter = fallbackBlockSetter;
            this.productIdResolver = productIdResolver;
            this.paintRecipes = paintRecipes == null ? List.of() : List.copyOf(paintRecipes);
        }

        public boolean paintRecipe(String style, Object level, Object pos) {
            String requestedStyle = key(style);
            if (requestedStyle.isBlank()) {
                return false;
            }
            boolean matched = false;
            for (NativeWorldPaintRecipe recipe : paintRecipes) {
                if (!requestedStyle.equals(key(recipe.style()))) {
                    continue;
                }
                matched = true;
                for (NativeWorldPaintPlacement placement : recipe.placements()) {
                    place(level, pos, placement);
                }
            }
            return matched;
        }

        private boolean place(Object level, Object pos, NativeWorldPaintPlacement placement) {
            if (placement == null) {
                return false;
            }
            String blockId = placement.productRelative()
                    ? productId(placement.blockId())
                    : placement.blockId();
            if (blockId == null || blockId.isBlank()) {
                return false;
            }
            String fallback = placement.fallbackBlockId();
            if (fallback == null || fallback.isBlank()) {
                return set(level, pos, placement.dx(), placement.dy(), placement.dz(), blockId);
            }
            return setAny(level, pos, placement.dx(), placement.dy(), placement.dz(), blockId, fallback);
        }

        public boolean setPlacement(Object level, Object pos, NativeWorldPaintPlacement placement) {
            return place(level, pos, placement);
        }

        public boolean set(Object level, Object pos, int dx, int dy, int dz, String blockId) {
            return blockSetter.setNear(level, pos, dx, dy, dz, blockId);
        }

        public boolean setAny(Object level, Object pos, int dx, int dy, int dz, String first, String fallback) {
            return fallbackBlockSetter.setNear(level, pos, dx, dy, dz, first, fallback);
        }

        public String productId(String path) {
            return productIdResolver.id(path);
        }
    }

    @FunctionalInterface
    public interface BlockSetter {
        boolean setNear(Object level, Object pos, int dx, int dy, int dz, String blockId);
    }

    @FunctionalInterface
    public interface FallbackBlockSetter {
        boolean setNear(Object level, Object pos, int dx, int dy, int dz, String first, String fallback);
    }

    @FunctionalInterface
    public interface ProductIdResolver {
        String id(String path);
    }
}
