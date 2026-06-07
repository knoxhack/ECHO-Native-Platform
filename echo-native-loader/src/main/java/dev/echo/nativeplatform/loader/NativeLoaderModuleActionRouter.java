package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeModuleActionRoute;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile.NativeModulePathActionRoute;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NativeLoaderModuleActionRouter {
    public static final String SERVICE_ID = "echo.native.module_action_router";

    private NativeLoaderModuleActionRouter() {
    }

    public static String runItemModule(
            String itemId,
            String namespace,
            String path,
            Object level,
            Object anchor,
            Object player,
            String routeKey,
            Config config,
            Context context
    ) {
        context.activateModule(namespace);
        if (context.isActiveProductNamespace(namespace)) {
            return NativeLoaderProductActionRouter.runItem(
                    path,
                    level,
                    anchor,
                    player,
                    routeKey,
                    context.productActionRouter()
            );
        }
        if (context.isInfoNamespace(namespace)) {
            Map<String, Object> itemInfoRuntime = context.invokeInfoRuntime(namespace, player);
            if (!context.placeNamespaceBlock(level, anchor, 0, 0, 0, namespace,
                    pathHints(path, config.infoPlacementHints()))) {
                context.setAnyBlock(level, anchor, 0, 0, 0,
                        listValue(config.infoFallbackBlockIds(), 0),
                        listValue(config.infoFallbackBlockIds(), 1));
            }
            if (!context.grantNamespaceItem(player, namespace, pathHints(path, config.infoRewardItemHints()))) {
                context.grantAnyItem(player,
                        listValue(config.infoFallbackItemIds(), 0),
                        listValue(config.infoFallbackItemIds(), 1));
            }
            return context.infoSummary(namespace, itemInfoRuntime);
        }
        if (context.isRecoveryNamespace(namespace)) {
            context.placeNamespaceBlock(level, anchor, 0, 0, 0, namespace,
                    pathHints(path, config.recoveryPlacementHints()));
            context.grantNamespaceItem(player, namespace, pathHints(path, config.recoveryRewardItemHints()));
            return "Recovery restored supplies and marked the return route.";
        }
        if (context.isArcanaNamespace(namespace)) {
            NativeLoaderProductWorldPainter.arcanaNode(level, anchor, context.worldPainter());
            context.placeNamespaceBlock(level, anchor, 0, 0, 1, namespace,
                    pathActionHints(path, config.arcanaPlacementHints(), context));
            if (!context.grantNamespaceItem(player, namespace,
                    pathActionHints(path, config.arcanaRewardItemHints(), context))) {
                context.grantAnyItem(player,
                        listValue(config.arcanaFallbackItemIds(), 0),
                        listValue(config.arcanaFallbackItemIds(), 1));
            }
            return "Arcana module resolved a field cast and placed a focus node.";
        }
        NativeModuleActionRoute itemActionRoute = moduleActionRoute(namespace, config);
        if (itemActionRoute != null) {
            runConfiguredScenario(itemActionRoute.itemScenarioMethod(), path, context, false);
            NativeLoaderProductWorldPainter.paintModuleActionRoute(
                    itemActionRoute.paintStyle(),
                    level,
                    anchor,
                    context.worldPainter()
            );
            grantActionRewards(player, itemActionRoute.itemRewardIds(), context);
            executeCommands(player, itemActionRoute.itemCommands(), context);
            return itemActionRoute.itemSummary();
        }
        context.grantStarterReward(player, itemId);
        if (!context.grantNamespaceItem(player, namespace, path, context.actionKey(path), "")) {
            context.grantAnyItem(player, context.productId("scrap_metal"), context.productId("ash"));
        }
        return context.genericSummary(namespace, path, false);
    }

    public static String runBlockModule(
            String blockId,
            String namespace,
            String path,
            Object level,
            Object pos,
            Object player,
            String routeKey,
            Config config,
            Context context
    ) {
        context.activateModule(namespace);
        if (context.isActiveProductNamespace(namespace)) {
            return NativeLoaderProductActionRouter.runBlock(
                    path,
                    level,
                    pos,
                    player,
                    routeKey,
                    context.productActionRouter()
            );
        }
        if (context.isInfoNamespace(namespace)) {
            Map<String, Object> blockInfoRuntime = context.invokeInfoRuntime(namespace, player);
            if (!context.placeNamespaceBlock(level, pos, 0, 1, 0, namespace,
                    pathHints(path, config.infoPlacementHints()))) {
                context.setAnyBlock(level, pos, 0, 1, 0,
                        listValue(config.infoFallbackBlockIds(), 0),
                        listValue(config.infoFallbackBlockIds(), 1));
            }
            if (!context.grantNamespaceItem(player, namespace, pathHints(path, config.infoRewardItemHints()))) {
                context.grantAnyItem(player,
                        listValue(config.infoFallbackItemIds(), 0),
                        listValue(config.infoFallbackItemIds(), 1));
            }
            return context.infoSummary(namespace, blockInfoRuntime);
        }
        if (context.isRecoveryNamespace(namespace)) {
            context.placeNamespaceBlock(level, pos, 0, 1, 0, namespace,
                    pathHints(path, config.recoveryBlockPlacementHints()));
            context.grantNamespaceItem(player, namespace, pathHints(path, config.recoveryRewardItemHints()));
            return "Recovery block restored supplies and marked a return anchor.";
        }
        if (context.isArcanaNamespace(namespace)) {
            NativeLoaderProductWorldPainter.arcanaNode(level, pos, context.worldPainter());
            context.placeNamespaceBlock(level, pos, 0, 0, 1, namespace,
                    pathActionHints(path, config.arcanaPlacementHints(), context));
            if (!context.grantNamespaceItem(player, namespace,
                    pathActionHints(path, config.arcanaRewardItemHints(), context))) {
                context.grantAnyItem(player,
                        listValue(config.arcanaFallbackItemIds(), 0),
                        listValue(config.arcanaFallbackItemIds(), 1));
            }
            return "Arcana block resolved a field ritual effect.";
        }
        NativeModuleActionRoute blockActionRoute = moduleActionRoute(namespace, config);
        if (blockActionRoute != null) {
            NativeModulePathActionRoute pathActionRoute = modulePathActionRoute(path, blockActionRoute);
            if (pathActionRoute != null) {
                NativeLoaderProductWorldPainter.paintModuleActionRoute(
                        pathActionRoute.paintStyle(),
                        level,
                        pos,
                        context.worldPainter()
                );
                grantActionRewards(player, pathActionRoute.rewardIds(), context);
                setPathActionBlock(level, pos, pathActionRoute.blockPlacementIds(), context);
                executeCommands(player, pathActionRoute.commands(), context);
                return pathActionRoute.summary();
            }
            NativeLoaderProductWorldPainter.paintModuleActionRoute(
                    blockActionRoute.paintStyle(),
                    level,
                    pos,
                    context.worldPainter()
            );
            grantActionRewards(player, blockActionRoute.blockRewardIds(), context);
            executeCommands(player, blockActionRoute.blockCommands(), context);
            Map<String, Object> scenario = runConfiguredScenario(
                    blockActionRoute.blockScenarioMethod(),
                    path,
                    context,
                    blockActionRoute.blockScenarioFromPath()
            );
            return scenario.isEmpty()
                    ? blockActionRoute.blockSummary()
                    : context.scenarioSummary(scenario, blockActionRoute.blockSummary());
        }
        context.grantBlockReward(player, blockId);
        if (!context.placeNamespaceBlock(level, pos, 0, 1, 0,
                namespace, path, context.actionKey(path), "")) {
            context.setAnyBlock(level, pos, 0, 1, 0,
                    context.productId("relay_station"),
                    context.productId("echo_cache"));
        }
        return context.genericSummary(namespace, path, true);
    }

    private static NativeModuleActionRoute moduleActionRoute(String namespace, Config config) {
        String ns = lower(namespace);
        for (NativeModuleActionRoute route : config.moduleActionRoutes()) {
            if (route != null && ns.equals(lower(route.namespace()))) {
                return route;
            }
        }
        return null;
    }

    private static NativeModulePathActionRoute modulePathActionRoute(String path, NativeModuleActionRoute actionRoute) {
        if (actionRoute == null) {
            return null;
        }
        for (NativeModulePathActionRoute pathRoute : actionRoute.blockPathRoutes()) {
            if (pathRoute != null && hasAny(path, pathRoute.pathHints().toArray(String[]::new))) {
                return pathRoute;
            }
        }
        return null;
    }

    private static String[] pathHints(String path, List<String> hints) {
        List<String> merged = new ArrayList<>();
        if (path != null && !path.isBlank()) {
            merged.add(path);
        }
        for (String hint : hints == null ? List.<String>of() : hints) {
            if (hint != null && !hint.isBlank()) {
                merged.add(hint);
            }
        }
        return merged.toArray(String[]::new);
    }

    private static String[] pathActionHints(String path, List<String> hints, Context context) {
        List<String> merged = new ArrayList<>();
        if (path != null && !path.isBlank()) {
            merged.add(path);
        }
        String actionKey = context.actionKey(path);
        if (!actionKey.isBlank()) {
            merged.add(actionKey);
        }
        for (String hint : hints == null ? List.<String>of() : hints) {
            if (hint != null && !hint.isBlank()) {
                merged.add(hint);
            }
        }
        return merged.toArray(String[]::new);
    }

    private static String listValue(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value;
    }

    private static void grantActionRewards(Object player, List<String> rewardIds, Context context) {
        if (rewardIds == null) {
            return;
        }
        for (int index = 0; index < rewardIds.size(); index += 2) {
            context.grantAnyItem(player, listValue(rewardIds, index), listValue(rewardIds, index + 1));
        }
    }

    private static void setPathActionBlock(
            Object level,
            Object pos,
            List<String> blockPlacementIds,
            Context context
    ) {
        if (blockPlacementIds == null || blockPlacementIds.isEmpty()) {
            return;
        }
        context.setAnyBlock(level, pos, 0, 1, 0, listValue(blockPlacementIds, 0), listValue(blockPlacementIds, 1));
    }

    private static void executeCommands(Object player, List<String> commands, Context context) {
        if (commands == null) {
            return;
        }
        for (String command : commands) {
            if (command != null && !command.isBlank()) {
                context.executeCommand(player, command);
            }
        }
    }

    private static Map<String, Object> runConfiguredScenario(
            String scenarioMethod,
            String path,
            Context context,
            boolean scenarioFromPath
    ) {
        String method = scenarioFromPath ? context.machineScenarioFor(path) : scenarioMethod;
        if (method == null || method.isBlank()) {
            return Map.of();
        }
        return context.runScenario(context.machineRuntimeClass(), method);
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = lower(value);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && haystack.contains(lower(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static final class Config {
        private final List<String> infoPlacementHints;
        private final List<String> infoRewardItemHints;
        private final List<String> infoFallbackBlockIds;
        private final List<String> infoFallbackItemIds;
        private final List<String> recoveryPlacementHints;
        private final List<String> recoveryRewardItemHints;
        private final List<String> recoveryBlockPlacementHints;
        private final List<String> arcanaPlacementHints;
        private final List<String> arcanaRewardItemHints;
        private final List<String> arcanaFallbackItemIds;
        private final List<NativeModuleActionRoute> moduleActionRoutes;

        public Config(
                List<String> infoPlacementHints,
                List<String> infoRewardItemHints,
                List<String> infoFallbackBlockIds,
                List<String> infoFallbackItemIds,
                List<String> recoveryPlacementHints,
                List<String> recoveryRewardItemHints,
                List<String> recoveryBlockPlacementHints,
                List<String> arcanaPlacementHints,
                List<String> arcanaRewardItemHints,
                List<String> arcanaFallbackItemIds,
                List<NativeModuleActionRoute> moduleActionRoutes
        ) {
            this.infoPlacementHints = safeList(infoPlacementHints);
            this.infoRewardItemHints = safeList(infoRewardItemHints);
            this.infoFallbackBlockIds = safeList(infoFallbackBlockIds);
            this.infoFallbackItemIds = safeList(infoFallbackItemIds);
            this.recoveryPlacementHints = safeList(recoveryPlacementHints);
            this.recoveryRewardItemHints = safeList(recoveryRewardItemHints);
            this.recoveryBlockPlacementHints = safeList(recoveryBlockPlacementHints);
            this.arcanaPlacementHints = safeList(arcanaPlacementHints);
            this.arcanaRewardItemHints = safeList(arcanaRewardItemHints);
            this.arcanaFallbackItemIds = safeList(arcanaFallbackItemIds);
            this.moduleActionRoutes = moduleActionRoutes == null ? List.of() : List.copyOf(moduleActionRoutes);
        }

        List<String> infoPlacementHints() {
            return infoPlacementHints;
        }

        List<String> infoRewardItemHints() {
            return infoRewardItemHints;
        }

        List<String> infoFallbackBlockIds() {
            return infoFallbackBlockIds;
        }

        List<String> infoFallbackItemIds() {
            return infoFallbackItemIds;
        }

        List<String> recoveryPlacementHints() {
            return recoveryPlacementHints;
        }

        List<String> recoveryRewardItemHints() {
            return recoveryRewardItemHints;
        }

        List<String> recoveryBlockPlacementHints() {
            return recoveryBlockPlacementHints;
        }

        List<String> arcanaPlacementHints() {
            return arcanaPlacementHints;
        }

        List<String> arcanaRewardItemHints() {
            return arcanaRewardItemHints;
        }

        List<String> arcanaFallbackItemIds() {
            return arcanaFallbackItemIds;
        }

        List<NativeModuleActionRoute> moduleActionRoutes() {
            return moduleActionRoutes;
        }

        private static List<String> safeList(List<String> values) {
            return values == null ? List.of() : List.copyOf(values);
        }
    }

    public static final class Context {
        private final NativeLoaderProductActionRouter.Context productActionRouter;
        private final NativeLoaderProductWorldPainter.Context worldPainter;
        private final ModuleActivator moduleActivator;
        private final NamespacePredicate activeProductNamespace;
        private final NamespacePredicate infoNamespace;
        private final NamespacePredicate recoveryNamespace;
        private final NamespacePredicate arcanaNamespace;
        private final InfoRuntimeInvoker infoRuntimeInvoker;
        private final NamespaceBlockPlacer namespaceBlockPlacer;
        private final NamespaceItemGranter namespaceItemGranter;
        private final AnyBlockSetter anyBlockSetter;
        private final AnyItemGranter anyItemGranter;
        private final InfoSummary infoSummary;
        private final ScenarioRunner scenarioRunner;
        private final ScenarioSummarizer scenarioSummarizer;
        private final CommandExecutor commandExecutor;
        private final ActionKey actionKey;
        private final ProductIdResolver productIdResolver;
        private final StarterRewardGranter starterRewardGranter;
        private final BlockRewardGranter blockRewardGranter;
        private final GenericSummary genericSummary;
        private final String machineRuntimeClass;

        Context(
                NativeLoaderProductActionRouter.Context productActionRouter,
                NativeLoaderProductWorldPainter.Context worldPainter,
                ModuleActivator moduleActivator,
                NamespacePredicate activeProductNamespace,
                NamespacePredicate infoNamespace,
                NamespacePredicate recoveryNamespace,
                NamespacePredicate arcanaNamespace,
                InfoRuntimeInvoker infoRuntimeInvoker,
                NamespaceBlockPlacer namespaceBlockPlacer,
                NamespaceItemGranter namespaceItemGranter,
                AnyBlockSetter anyBlockSetter,
                AnyItemGranter anyItemGranter,
                InfoSummary infoSummary,
                ScenarioRunner scenarioRunner,
                ScenarioSummarizer scenarioSummarizer,
                CommandExecutor commandExecutor,
                ActionKey actionKey,
                ProductIdResolver productIdResolver,
                StarterRewardGranter starterRewardGranter,
                BlockRewardGranter blockRewardGranter,
                GenericSummary genericSummary,
                String machineRuntimeClass
        ) {
            this.productActionRouter = productActionRouter;
            this.worldPainter = worldPainter;
            this.moduleActivator = moduleActivator;
            this.activeProductNamespace = activeProductNamespace;
            this.infoNamespace = infoNamespace;
            this.recoveryNamespace = recoveryNamespace;
            this.arcanaNamespace = arcanaNamespace;
            this.infoRuntimeInvoker = infoRuntimeInvoker;
            this.namespaceBlockPlacer = namespaceBlockPlacer;
            this.namespaceItemGranter = namespaceItemGranter;
            this.anyBlockSetter = anyBlockSetter;
            this.anyItemGranter = anyItemGranter;
            this.infoSummary = infoSummary;
            this.scenarioRunner = scenarioRunner;
            this.scenarioSummarizer = scenarioSummarizer;
            this.commandExecutor = commandExecutor;
            this.actionKey = actionKey;
            this.productIdResolver = productIdResolver;
            this.starterRewardGranter = starterRewardGranter;
            this.blockRewardGranter = blockRewardGranter;
            this.genericSummary = genericSummary;
            this.machineRuntimeClass = machineRuntimeClass == null ? "" : machineRuntimeClass;
        }

        NativeLoaderProductActionRouter.Context productActionRouter() {
            return productActionRouter;
        }

        NativeLoaderProductWorldPainter.Context worldPainter() {
            return worldPainter;
        }

        void activateModule(String namespace) {
            moduleActivator.activate(namespace);
        }

        boolean isActiveProductNamespace(String namespace) {
            return activeProductNamespace.matches(namespace);
        }

        boolean isInfoNamespace(String namespace) {
            return infoNamespace.matches(namespace);
        }

        boolean isRecoveryNamespace(String namespace) {
            return recoveryNamespace.matches(namespace);
        }

        boolean isArcanaNamespace(String namespace) {
            return arcanaNamespace.matches(namespace);
        }

        Map<String, Object> invokeInfoRuntime(String namespace, Object player) {
            return infoRuntimeInvoker.invoke(namespace, player);
        }

        boolean placeNamespaceBlock(Object level, Object pos, int dx, int dy, int dz, String namespace, String... hints) {
            return namespaceBlockPlacer.place(level, pos, dx, dy, dz, namespace, hints);
        }

        boolean grantNamespaceItem(Object player, String namespace, String... hints) {
            return namespaceItemGranter.grant(player, namespace, hints);
        }

        boolean setAnyBlock(Object level, Object pos, int dx, int dy, int dz, String first, String fallback) {
            return anyBlockSetter.setNear(level, pos, dx, dy, dz, first, fallback);
        }

        boolean grantAnyItem(Object player, String first, String fallback) {
            return anyItemGranter.grant(player, first, fallback);
        }

        String infoSummary(String namespace, Map<String, Object> report) {
            return infoSummary.summary(namespace, report);
        }

        Map<String, Object> runScenario(String className, String methodName) {
            return scenarioRunner.run(className, methodName);
        }

        String scenarioSummary(Map<String, Object> scenario, String fallback) {
            return scenarioSummarizer.summary(scenario, fallback);
        }

        boolean executeCommand(Object player, String command) {
            return commandExecutor.execute(player, command);
        }

        String actionKey(String path) {
            return actionKey.key(path);
        }

        String productId(String path) {
            return productIdResolver.id(path);
        }

        void grantStarterReward(Object player, String itemId) {
            starterRewardGranter.grant(player, itemId);
        }

        void grantBlockReward(Object player, String blockId) {
            blockRewardGranter.grant(player, blockId);
        }

        String genericSummary(String namespace, String path, boolean blockRoute) {
            return genericSummary.summary(namespace, path, blockRoute);
        }

        String machineRuntimeClass() {
            return machineRuntimeClass;
        }

        String machineScenarioFor(String path) {
            return productActionRouter.machineScenarioFor(path);
        }
    }

    @FunctionalInterface
    public interface ModuleActivator {
        void activate(String namespace);
    }

    @FunctionalInterface
    public interface NamespacePredicate {
        boolean matches(String namespace);
    }

    @FunctionalInterface
    public interface InfoRuntimeInvoker {
        Map<String, Object> invoke(String namespace, Object player);
    }

    @FunctionalInterface
    public interface NamespaceBlockPlacer {
        boolean place(Object level, Object pos, int dx, int dy, int dz, String namespace, String... hints);
    }

    @FunctionalInterface
    public interface NamespaceItemGranter {
        boolean grant(Object player, String namespace, String... hints);
    }

    @FunctionalInterface
    public interface AnyBlockSetter {
        boolean setNear(Object level, Object pos, int dx, int dy, int dz, String first, String fallback);
    }

    @FunctionalInterface
    public interface AnyItemGranter {
        boolean grant(Object player, String first, String fallback);
    }

    @FunctionalInterface
    public interface InfoSummary {
        String summary(String namespace, Map<String, Object> report);
    }

    @FunctionalInterface
    public interface ScenarioRunner {
        Map<String, Object> run(String className, String methodName);
    }

    @FunctionalInterface
    public interface ScenarioSummarizer {
        String summary(Map<String, Object> scenario, String fallback);
    }

    @FunctionalInterface
    public interface CommandExecutor {
        boolean execute(Object player, String command);
    }

    @FunctionalInterface
    public interface ActionKey {
        String key(String path);
    }

    @FunctionalInterface
    public interface ProductIdResolver {
        String id(String path);
    }

    @FunctionalInterface
    public interface StarterRewardGranter {
        void grant(Object player, String itemId);
    }

    @FunctionalInterface
    public interface BlockRewardGranter {
        void grant(Object player, String blockId);
    }

    @FunctionalInterface
    public interface GenericSummary {
        String summary(String namespace, String path, boolean blockRoute);
    }
}
