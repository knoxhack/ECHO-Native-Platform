package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeOutputRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeOutputRules;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePathValueRule;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativePathValueRules;
import dev.echo.nativeplatform.loader.NativeLoaderClientReflectionSupport;
import dev.echo.nativeplatform.loader.NativeLoaderModuleSurfaceFlow;
import dev.echo.nativeplatform.loader.NativeLoaderProductBlockActionExecutor;
import dev.echo.nativeplatform.loader.NativeLoaderProductItemActionExecutor;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

final class EchoNativeBootstrapProductActionFlow {
    private final EchoNativeBootstrapProductProfile bootstrapProfile;
    private final EchoNativeProductProfileCore productProfile;
    private final Context context;

    EchoNativeBootstrapProductActionFlow(
            EchoNativeBootstrapProductProfile bootstrapProfile,
            EchoNativeProductProfileCore productProfile,
            Context context
    ) {
        this.bootstrapProfile = bootstrapProfile;
        this.productProfile = productProfile;
        this.context = context;
    }

    Object onItemUse(String itemId, Object level, Object player, Object hand) {
        return context.interactionResult().apply(nativeBetaItemAction(itemId, level, player, null, hand) ? "SUCCESS" : "PASS");
    }

    Object onItemUseOn(String itemId, Object useContext) {
        Object player = NativeLoaderClientReflectionSupport.optionalMethodValue(useContext, "getPlayer");
        Object level = NativeLoaderClientReflectionSupport.optionalMethodValue(useContext, "getLevel");
        Object pos = NativeLoaderClientReflectionSupport.optionalMethodValue(useContext, "getClickedPos");
        Object stack = NativeLoaderClientReflectionSupport.optionalMethodValue(useContext, "getItemInHand");
        return context.interactionResult().apply(nativeBetaItemAction(itemId, level, player, pos, stack) ? "SUCCESS" : "PASS");
    }

    boolean itemBarVisible(String itemId, Object stack) {
        return batteryCapacity(itemId) > 0 && batteryStoredEnergy(stack, batteryCapacity(itemId)) > 0;
    }

    int itemBarWidth(String itemId, Object stack) {
        int capacity = batteryCapacity(itemId);
        if (capacity <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(13, Math.round(13.0F * batteryStoredEnergy(stack, capacity) / capacity)));
    }

    int itemBarColor(String itemId, Object stack) {
        int capacity = batteryCapacity(itemId);
        if (capacity <= 0) {
            return 0x55FFFF;
        }
        float charge = (float) batteryStoredEnergy(stack, capacity) / (float) capacity;
        try {
            Class<?> mthClass = Class.forName(context.runtimeClass().apply("util.Mth"));
            Object value = mthClass.getMethod("hsvToRgb", float.class, float.class, float.class)
                    .invoke(null, 0.50F + charge * 0.10F, 0.85F, 1.0F);
            return value instanceof Number number ? number.intValue() : 0x55FFFF;
        } catch (Throwable ignored) {
            return charge > 0.66F ? 0x55FFFF : charge > 0.33F ? 0xFFFF55 : 0xFF5555;
        }
    }

    Object onBlockUse(String blockId, Object level, Object pos, Object player) {
        return context.interactionResult().apply(nativeBetaBlockAction(blockId, level, pos, player) ? "SUCCESS" : "PASS");
    }

    void onBlockPlaced(String blockId, Object level, Object pos) {
        // Native block placement is visible through block/entity behavior rather than generated chat overlays.
    }

    boolean nativeBetaItemAction(String itemId, Object level, Object player, Object pos, Object handOrStack) {
        String id = lowerContentId(itemId);
        String namespace = namespaceOf(id);
        String path = pathOf(id);
        if (context.isClientSideLevel().test(level)) {
            return openModuleSurfaceFor(namespace, path);
        }
        return serverItemAction(namespace, path, level, player, pos, handOrStack);
    }

    boolean nativeBetaBlockAction(String blockId, Object level, Object pos, Object player) {
        String id = lowerContentId(blockId);
        String namespace = namespaceOf(id);
        String path = pathOf(id);
        if (context.isClientSideLevel().test(level)) {
            return openModuleSurfaceFor(namespace, path, id, level, pos, player);
        }
        return serverBlockAction(namespace, path, level, pos, player);
    }

    boolean openModuleSurfaceFor(String namespace, String path) {
        return openModuleSurfaceFor(namespace, path, "", null, null, null);
    }

    boolean openModuleSurfaceFor(
            String namespace,
            String path,
            String contentId,
            Object level,
            Object pos,
            Object player
    ) {
        return NativeLoaderModuleSurfaceFlow.open(
                namespace,
                path,
                contentId,
                level,
                pos,
                player,
                context.moduleSurfaceContext()
        );
    }

    Map<String, Object> gameplaySurfaceContextForMode(String mode) {
        return NativeLoaderModuleSurfaceFlow.contextForMode(mode);
    }

    String moduleSurface(String namespace, String path) {
        return NativeLoaderModuleSurfaceFlow.surface(namespace, path, context.moduleSurfaceContext());
    }

    boolean productMachinePath(String path) {
        return productProfile.isMachinePath(path);
    }

    String productId(String path) {
        return productProfile.id(path);
    }

    String productPath(String idOrPath) {
        return productProfile.path(idOrPath);
    }

    String productConfiguredId(String idOrPath) {
        return productProfile.configuredId(idOrPath);
    }

    List<String> productConfiguredIds(List<String> idsOrPaths) {
        return productProfile.configuredIds(idsOrPaths);
    }

    String productBlockActionMachineId(String action, String fallbackPath) {
        return productProfile.blockActionMachineId(action, fallbackPath);
    }

    String productPackKey() {
        return productProfile.packKey();
    }

    String productTerminalRoute(String path) {
        return productProfile.terminalRoute(path);
    }

    String productHoloMapRoute(String path) {
        return productProfile.holoMapRoute(path);
    }

    String productHudActionKey(String path) {
        return productProfile.hudActionKey(path);
    }

    String[] productIds(String... paths) {
        return productProfile.ids(paths);
    }

    boolean moduleRuntimeMutationAccepted(Map<String, Object> report) {
        Map<String, Object> result = report == null ? Map.of() : report;
        return Boolean.TRUE.equals(result.get("adapterCoreActionExecuted"))
                && Boolean.TRUE.equals(result.get("runtimeHostMutated"))
                && Boolean.TRUE.equals(result.get("stateMutation"))
                && Boolean.TRUE.equals(result.get("saveTouched"))
                && Boolean.TRUE.equals(result.get("missionUpdated"))
                && Boolean.TRUE.equals(result.get("feedbackEmitted"));
    }

    private boolean serverItemAction(
            String namespace,
            String path,
            Object level,
            Object player,
            Object pos,
            Object handOrStack
    ) {
        if (player == null) {
            return false;
        }
        if (bootstrapProfile.namespace().equals(namespace)) {
            String itemId = namespace + ":" + path;
            String action = productProfile.itemAction(path);
            if (NativeLoaderProductItemActionExecutor.handles(action)) {
                return NativeLoaderProductItemActionExecutor.execute(
                        action,
                        path,
                        itemId,
                        level,
                        pos,
                        player,
                        handOrStack,
                        itemActionOperations()
                );
            }
        }
        if ("echoterminal".equals(namespace)
                || "echowiki".equals(namespace)
                || "echoindex".equals(namespace)
                || "echolens".equals(namespace)
                || "echoholomap".equals(namespace)
                || "signalos".equals(namespace)) {
            return serverModuleAction(namespace, path, level, null, player, false);
        }
        if (context.isEchoNamespace().test(namespace)) {
            return serverModuleAction(namespace, path, level, null, player, false);
        }
        return false;
    }

    private boolean serverBlockAction(String namespace, String path, Object level, Object pos, Object player) {
        if (bootstrapProfile.namespace().equals(namespace) && player != null) {
            return productServerBlockAction(path, level, pos, player);
        }
        if ("echoterminal".equals(namespace)
                || "signalos".equals(namespace)
                || "echoindex".equals(namespace)
                || "echolens".equals(namespace)
                || "echoholomap".equals(namespace)
                || "echowiki".equals(namespace)
                || moduleSurface(namespace, path).length() > 0) {
            return serverModuleAction(namespace, path, level, pos, player, true);
        }
        if (context.isEchoNamespace().test(namespace)) {
            return serverModuleAction(namespace, path, level, pos, player, true);
        }
        return false;
    }

    private boolean serverModuleAction(
            String namespace,
            String path,
            Object level,
            Object pos,
            Object player,
            boolean blockRoute
    ) {
        Map<String, Object> report = context.moduleRuntime().invoke(namespace, path, level, pos, player, blockRoute);
        String surface = moduleSurface(namespace, path);
        if (!surface.isBlank()) {
            return openModuleSurfaceFor(namespace, path, namespace + ":" + path, level, pos, player);
        }
        return moduleRuntimeMutationAccepted(report);
    }

    private NativeLoaderProductItemActionExecutor.Operations itemActionOperations() {
        return new NativeLoaderProductItemActionExecutor.Operations(
                context.heldItemStack()::value,
                context.waterBottleUsed()::run,
                context.removeConsumableItem()::run,
                context.isCreativePlayer()::test,
                context.giveItem()::run,
                context.radAwayUsed()::run,
                context.executeCommand()::run,
                context.invokeRealItemUse()::run,
                context.filterCartridgeUsed()::run,
                context.crudeFilterUsed()::run,
                context.itemConsumed()::run,
                context.healPlayer()::run,
                context.handWarmerUsed()::run,
                context.damageOrShrinkItemStack()::run,
                player -> Boolean.TRUE.equals(NativeLoaderClientReflectionSupport.optionalMethodValue(player, "isShiftKeyDown")),
                context.recoveryItemId()::get,
                this::productPath,
                context.scannerUse()::run,
                context.registryContains()::test,
                this::productId,
                this::productDeployableEntityId,
                context.deployEntityRoute()::run,
                context.dataLogRecovered()::run,
                this::openModuleSurfaceFor
        );
    }

    private boolean productServerBlockAction(String path, Object level, Object pos, Object player) {
        return NativeLoaderProductBlockActionExecutor.execute(
                productProfile.blockAction(path),
                path,
                level,
                pos,
                player,
                bootstrapProfile.nativeMachineOperationRules(),
                blockActionOperations()
        );
    }

    private String productDeployableEntityId(String itemId) {
        String path = productPath(itemId);
        if (path.endsWith("_item")) {
            path = path.substring(0, path.length() - "_item".length());
        }
        return productId(path);
    }

    private NativeLoaderProductBlockActionExecutor.Operations blockActionOperations() {
        return new NativeLoaderProductBlockActionExecutor.Operations(
                this::productId,
                this::productBlockActionMachineId,
                this::productConfiguredId,
                this::productConfiguredIds,
                this::firstConfiguredItem,
                this::energyItemCharge,
                this::cleanOutputForContaminated,
                context.hasItem()::test,
                context.giveItem()::run,
                context.removeItem()::run,
                context.machineUseBlock()::run,
                context.machineReceiveEnergy()::run,
                context.machineExtractEnergy()::run,
                context.machineInsertItem()::run,
                context.machineExtractItem()::run,
                context.machineTick()::run,
                context.powerNodeState()::run,
                context.researchLabAnalyze()::run,
                context.scannerUse()::run,
                context.terminalOpened()::run,
                this::productHoloMapRoute,
                this::openModuleSurfaceFor,
                context.waterFiltered()::run
        );
    }

    private String firstConfiguredItem(Object player, int count, List<String> idsOrPaths) {
        return context.firstItem().value(player, count, productConfiguredIds(idsOrPaths).toArray(String[]::new));
    }

    private int batteryCapacity(String itemId) {
        return pathValue(pathOf(lowerContentId(itemId)), bootstrapProfile.nativeBatteryCapacityRules());
    }

    private int batteryStoredEnergy(Object stack, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        try {
            int maxDamage = ((Number) stack.getClass().getMethod("getMaxDamage").invoke(stack)).intValue();
            int damage = ((Number) stack.getClass().getMethod("getDamageValue").invoke(stack)).intValue();
            if (maxDamage > 0) {
                float remaining = 1.0F - Math.max(0, Math.min(maxDamage, damage)) / (float) maxDamage;
                return Math.max(0, Math.min(capacity, Math.round(capacity * remaining)));
            }
        } catch (Throwable ignored) {
            // Native batteries default to a full portable cell until a machine drains durability.
        }
        return capacity;
    }

    private int energyItemCharge(String itemId) {
        return pathValue(itemId, bootstrapProfile.nativeEnergyItemChargeRules());
    }

    private String cleanOutputForContaminated(String input) {
        String id = lowerContentId(input);
        NativeOutputRules outputRules = bootstrapProfile.nativeContaminatedOutputRules();
        if (outputRules != null) {
            for (NativeOutputRule rule : outputRules.rules()) {
                if (rule != null && hasAny(id, rule.pathHints()) && !lowerContentId(rule.outputId()).isBlank()) {
                    return lowerContentId(rule.outputId());
                }
            }
            if (!lowerContentId(outputRules.defaultOutputId()).isBlank()) {
                return lowerContentId(outputRules.defaultOutputId());
            }
        }
        return "minecraft:iron_ingot";
    }

    private int pathValue(String contentId, NativePathValueRules valueRules) {
        if (valueRules == null) {
            return 0;
        }
        String id = lowerContentId(contentId);
        for (NativePathValueRule rule : valueRules.rules()) {
            if (rule != null && hasAny(id, rule.pathHints())) {
                return rule.value();
            }
        }
        return valueRules.defaultValue();
    }

    private static String lowerContentId(String contentId) {
        return contentId == null ? "" : contentId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String namespaceOf(String contentId) {
        int separator = contentId.indexOf(':');
        return separator < 0 ? "" : contentId.substring(0, separator);
    }

    private static String pathOf(String contentId) {
        int separator = contentId.indexOf(':');
        return separator < 0 ? contentId : contentId.substring(separator + 1);
    }

    private static boolean hasAny(String value, List<String> needles) {
        String safeValue = lowerContentId(value);
        if (safeValue.isBlank() || needles == null || needles.isEmpty()) {
            return false;
        }
        for (String needle : needles) {
            if (!lowerContentId(needle).isBlank() && safeValue.contains(lowerContentId(needle))) {
                return true;
            }
        }
        return false;
    }

    record Context(
            NativeLoaderModuleSurfaceFlow.Context moduleSurfaceContext,
            Function<String, String> runtimeClass,
            Function<String, Object> interactionResult,
            Predicate<Object> isClientSideLevel,
            ModuleRuntime moduleRuntime,
            Predicate<String> isEchoNamespace,
            HeldItemStack heldItemStack,
            WaterBottleUsed waterBottleUsed,
            LevelPlayerItemCountAction removeConsumableItem,
            Predicate<Object> isCreativePlayer,
            PlayerItemCountAction giveItem,
            SourceAction radAwayUsed,
            CommandExecutor executeCommand,
            RealItemUse invokeRealItemUse,
            FilterCartridgeUsed filterCartridgeUsed,
            ItemUse crudeFilterUsed,
            ItemUse itemConsumed,
            PlayerHealer healPlayer,
            HandWarmerUsed handWarmerUsed,
            DamageOrShrinkItemStack damageOrShrinkItemStack,
            ScannerUse scannerUse,
            BiPredicate<String, String> registryContains,
            SourcePositionAction deployEntityRoute,
            DataLogRecovered dataLogRecovered,
            FirstItem firstItem,
            PlayerItemCountPredicate hasItem,
            LevelPlayerItemCountAction removeItem,
            MachineUseBlock machineUseBlock,
            MachineEnergyAction machineReceiveEnergy,
            MachineEnergyAction machineExtractEnergy,
            MachineItemAction machineInsertItem,
            MachineItemAction machineExtractItem,
            MachineTick machineTick,
            PowerNodeState powerNodeState,
            ResearchLabAnalyze researchLabAnalyze,
            TerminalOpened terminalOpened,
            WaterFiltered waterFiltered,
            java.util.function.Supplier<String> recoveryItemId
    ) {
    }

    @FunctionalInterface
    interface ModuleRuntime {
        Map<String, Object> invoke(
                String namespace,
                String path,
                Object level,
                Object pos,
                Object player,
                boolean blockRoute
        );
    }

    @FunctionalInterface
    interface HeldItemStack {
        Object value(Object player, Object handOrStack);
    }

    @FunctionalInterface
    interface WaterBottleUsed {
        boolean run(Object level, Object player, Object handOrStack, String itemId, String path);
    }

    @FunctionalInterface
    interface LevelPlayerItemCountAction {
        boolean run(Object level, Object player, String itemId, int count);
    }

    @FunctionalInterface
    interface PlayerItemCountAction {
        boolean run(Object player, String itemId, int count);
    }

    @FunctionalInterface
    interface PlayerItemCountPredicate {
        boolean test(Object player, String itemId, int count);
    }

    @FunctionalInterface
    interface SourceAction {
        boolean run(Object level, Object player, String source);
    }

    @FunctionalInterface
    interface CommandExecutor {
        boolean run(Object player, String command);
    }

    @FunctionalInterface
    interface RealItemUse {
        boolean run(Object level, Object player, Object handOrStack, String itemId);
    }

    @FunctionalInterface
    interface FilterCartridgeUsed {
        boolean run(Object level, Object player, String itemId, String tierName, int tier, int refillAmount);
    }

    @FunctionalInterface
    interface ItemUse {
        boolean run(Object level, Object player, Object handOrStack);
    }

    @FunctionalInterface
    interface PlayerHealer {
        boolean run(Object player, float amount);
    }

    @FunctionalInterface
    interface HandWarmerUsed {
        boolean run(Object level, Object player, Object handOrStack, int warmthDelta);
    }

    @FunctionalInterface
    interface DamageOrShrinkItemStack {
        boolean run(Object stack, Object player, Object handOrStack, int amount);
    }

    @FunctionalInterface
    interface ScannerUse {
        boolean run(Object level, Object player, String source, boolean deepScan);
    }

    @FunctionalInterface
    interface SourcePositionAction {
        boolean run(Object level, Object player, Object pos, String source);
    }

    @FunctionalInterface
    interface DataLogRecovered {
        boolean run(Object level, Object player, String logType, String title);
    }

    @FunctionalInterface
    interface FirstItem {
        String value(Object player, int count, String... itemIds);
    }

    @FunctionalInterface
    interface MachineUseBlock {
        boolean run(Object level, Object player, Object pos, String machineId);
    }

    @FunctionalInterface
    interface MachineEnergyAction {
        boolean run(Object level, Object player, Object pos, String machineId, int amount);
    }

    @FunctionalInterface
    interface MachineItemAction {
        boolean run(Object level, Object player, Object pos, String machineId, String itemId, int count);
    }

    @FunctionalInterface
    interface MachineTick {
        boolean run(Object level, Object player, Object pos, String machineId);
    }

    @FunctionalInterface
    interface PowerNodeState {
        boolean run(Object level, Object player, Object pos, boolean active, int activeNodeCount, String source);
    }

    @FunctionalInterface
    interface ResearchLabAnalyze {
        boolean run(Object level, Object player, String source);
    }

    @FunctionalInterface
    interface TerminalOpened {
        boolean run(Object level, Object player, String route);
    }

    @FunctionalInterface
    interface WaterFiltered {
        boolean run(Object level, Object player, String source);
    }
}
