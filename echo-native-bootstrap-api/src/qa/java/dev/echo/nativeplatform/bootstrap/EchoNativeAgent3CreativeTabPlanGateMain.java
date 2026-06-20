package dev.echo.nativeplatform.bootstrap;

import dev.echo.nativeplatform.contracts.EchoNativeBootstrapProductProfile;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.loader.EchoNativeRegistryHost;
import dev.echo.nativeplatform.loader.NativeLoaderBootstrapRegistryBridgeFactory;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryBridge;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryContentBridge;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryCreativeBridge;
import dev.echo.nativeplatform.loader.NativeLoaderRegistryCreativeVisibilityBridge;
import dev.echo.nativeplatform.loader.NativeLoaderRuntimeBridgeAggregator;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class EchoNativeAgent3CreativeTabPlanGateMain {
    private EchoNativeAgent3CreativeTabPlanGateMain() {
    }

    public static void main(String[] args) {
        assertCreativeTabPlan();
        assertMultipleDeclaredCreativeGroupsInOneModule();
        assertLiveCreativeTabRegistration();
        assertDeclaredCreativeTabSearchVisibilityDrivesLiveOutput();
        assertDeclaredCreativeTabItemsMustBeBackedOnLivePath();
        assertDeclaredCreativeTabIconMustResolveToNativeItem();
        assertCreatedCreativeTabMustProveLiveOutputItems();
        assertLiveMultipleDeclaredCreativeGroupsInOneModule();
        assertExistingCreativeTabIsPresentButNotCreated();
        assertMissingCreativeTabKeyIsNotTrustedFromGetValueFallback();
        assertProjectedCreativeTabItemsAreNotVisibleReleaseEvidence();
        assertCreativeTabFallbackIsNotTrustedAsRegistryBacked();
        assertCreativeVisibilityBridgeDoesNotPromoteVanillaAugmentationAsNativeTab();
        assertBootstrapRegistryHostPromotesAllFirstClassDeclarations();
        assertBootstrapAppliedRegistryBridgeCanonicalizesDirectIds();
        System.out.println("agent3 native creative tab plan gate PASS plans=2 liveTabs=1 existingTabs=1");
    }

    private static void assertCreativeTabPlan() {
        List<Map<String, Object>> plans = NativeLoaderRegistryCreativeBridge.plannedNativeCreativeTabs(
                new TestProfile(),
                List.of(
                        "echoashfallprotocol:industrial_aggregate",
                        "echoblockworks:nexus_gate"
                ),
                List.of(
                        "echoashfallprotocol:scrap_knife",
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoblockworks:nexus_shard"
                ),
                List.of("echoashfallprotocol:native_modules_tab"),
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:missing_descriptor_only_item"
                        ),
                        "surfaceIds", List.of("terminal", "index"),
                        "orderAnchor", "minecraft:building_blocks"
                ))
        );
        require(plans.size() == 2, "expected one declared Ashfall tab and one fallback module tab");

        Map<String, Object> ashfall = planById(plans, "echoashfallprotocol:native_modules_tab");
        require("echoashfallprotocol:native_modules_tab".equals(ashfall.get("tabId")),
                "Ashfall declaration must drive the native modules tab id");
        require(Boolean.TRUE.equals(ashfall.get("moduleScoped")), "Ashfall tab must be module scoped");
        require(Boolean.TRUE.equals(ashfall.get("nativeRegistryContentBacked")),
                "Ashfall tab must be backed by native registry content");
        require("echoashfallprotocol:portable_signal_scanner".equals(ashfall.get("iconItem")),
                "Ashfall tab icon must come from the declaration");
        require("declaration".equals(ashfall.get("iconSource")), "Ashfall tab icon source must be declaration");
        require("minecraft:building_blocks".equals(ashfall.get("orderAnchor")),
                "Ashfall tab order anchor must come from the declaration");
        require("parent_and_search_tabs".equals(ashfall.get("searchVisibility")),
                "Ashfall tab must use parent/search creative visibility");
        require(Boolean.TRUE.equals(ashfall.get("searchVisible")), "Ashfall tab must be search visible");
        require(stringList(ashfall.get("declaredSurfaceIds")).containsAll(List.of("terminal", "index")),
                "Ashfall tab must preserve declared surface ids");
        require(stringList(ashfall.get("creativeTabItemsFromNativeRegistry"))
                        .contains("echoashfallprotocol:portable_signal_scanner"),
                "Ashfall tab must populate from native registry items");
        require(stringList(ashfall.get("unbackedDeclaredCreativeTabItems"))
                        .equals(List.of("echoashfallprotocol:missing_descriptor_only_item")),
                "descriptor-only creative items must be reported as unbacked");
        require(!Boolean.TRUE.equals(ashfall.get("declaredCreativeTabItemsBackedByNativeRegistry")),
                "unbacked descriptor items must fail declaration backing proof");

        Map<String, Object> blockworks = planById(plans, "echoblockworks:native_modules");
        require("echoblockworks:native_modules".equals(blockworks.get("tabId")),
                "fallback namespace tab must use native_modules path");
        require(((Number) ashfall.get("orderIndex")).intValue() < ((Number) blockworks.get("orderIndex")).intValue(),
                "creative tab plan must preserve deterministic namespace ordering");
        require(String.valueOf(ashfall.get("sortKey")).compareTo(String.valueOf(blockworks.get("sortKey"))) < 0,
                "creative tab sort keys must preserve deterministic plan ordering");
        require("registry_fallback".equals(blockworks.get("iconSource")),
                "fallback namespace tab must record registry fallback icon source");
        require(stringList(blockworks.get("searchVisibleItemIds"))
                        .containsAll(List.of("echoblockworks:nexus_gate", "echoblockworks:nexus_shard")),
                "fallback namespace tab search items must mirror registry content");
    }

    private static void assertMultipleDeclaredCreativeGroupsInOneModule() {
        List<Map<String, Object>> plans = NativeLoaderRegistryCreativeBridge.plannedNativeCreativeTabs(
                new TestProfile(),
                List.of("echoashfallprotocol:industrial_aggregate"),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife",
                        "echoashfallprotocol:survival_cache"
                ),
                List.of(),
                List.of(
                        Map.of(
                                "registry", "creative_tab",
                                "id", "echoashfallprotocol:native_tools",
                                "titleKey", "itemGroup.EchoAshfallNativeTools",
                                "iconItem", "echoashfallprotocol:portable_signal_scanner",
                                "itemIds", List.of(
                                        "echoashfallprotocol:portable_signal_scanner",
                                        "echoashfallprotocol:scrap_knife"
                                ),
                                "surfaceIds", List.of("terminal"),
                                "orderAnchor", "minecraft:tools_and_utilities"
                        ),
                        Map.of(
                                "registry", "creative_tab",
                                "id", "echoashfallprotocol:native_supplies",
                                "titleKey", "itemGroup.EchoAshfallNativeSupplies",
                                "itemIds", List.of("echoashfallprotocol:survival_cache"),
                                "surfaceIds", List.of("index"),
                                "orderAnchor", "minecraft:ingredients"
                        )
                )
        );
        require(plans.size() == 2, "same-module declarations must create one native creative group per declaration");

        Map<String, Object> tools = planById(plans, "echoashfallprotocol:native_tools");
        Map<String, Object> supplies = planById(plans, "echoashfallprotocol:native_supplies");
        require("echoashfallprotocol:native_tools".equals(tools.get("creativeGroupId")),
                "declared native tools tab must become its own creative group");
        require("echoashfallprotocol:native_supplies".equals(supplies.get("creativeGroupId")),
                "declared native supplies tab must become its own creative group");
        require(((Number) tools.get("orderIndex")).intValue() < ((Number) supplies.get("orderIndex")).intValue(),
                "same-module creative groups must preserve declaration ordering");
        require("minecraft:tools_and_utilities".equals(tools.get("orderAnchor")),
                "tools group must preserve its declaration order anchor");
        require("minecraft:ingredients".equals(supplies.get("orderAnchor")),
                "supplies group must preserve its declaration order anchor");
        require("echoashfallprotocol:portable_signal_scanner".equals(tools.get("iconItem")),
                "tools group must use declaration icon");
        require("declaration".equals(tools.get("iconSource")),
                "tools group icon source must be declaration");
        require("echoashfallprotocol:survival_cache".equals(supplies.get("iconItem")),
                "supplies group without explicit icon must fall back to its registry-backed item");
        require("registry_fallback".equals(supplies.get("iconSource")),
                "supplies group icon source must record registry fallback");
        require(stringList(tools.get("creativeTabItemsFromNativeRegistry")).equals(List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                "tools group must use its declared registry-backed item subset");
        require(stringList(supplies.get("creativeTabItemsFromNativeRegistry")).equals(List.of(
                        "echoashfallprotocol:survival_cache"
                )),
                "supplies group must use its declared registry-backed item subset");
        require(stringList(tools.get("searchVisibleItemIds"))
                        .equals(stringList(tools.get("creativeTabItemsFromNativeRegistry"))),
                "tools search visibility must mirror its own group population");
        require(stringList(supplies.get("searchVisibleItemIds"))
                        .equals(stringList(supplies.get("creativeTabItemsFromNativeRegistry"))),
                "supplies search visibility must mirror its own group population");
        require(Boolean.TRUE.equals(tools.get("declaredCreativeTabItemsBackedByNativeRegistry")),
                "tools group declared items must be registry-backed");
        require(Boolean.TRUE.equals(supplies.get("declaredCreativeTabItemsBackedByNativeRegistry")),
                "supplies group declared items must be registry-backed");
    }

    private static void assertLiveCreativeTabRegistration() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "scrap_knife"),
                new TestItem("echoashfallprotocol:scrap_knife"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "industrial_aggregate"),
                new TestItem("echoashfallprotocol:industrial_aggregate"));

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of("echoashfallprotocol:industrial_aggregate"),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                ),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner"),
                        "surfaceIds", List.of("terminal", "index"),
                        "orderAnchor", "minecraft:building_blocks"
                ))
        );
        require(bridges.size() == 1, "live bridge must register one Ashfall creative tab");
        Map<String, Object> bridge = bridges.get(0);
        require(Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "live bridge must create a first-class native creative tab");
        require(Boolean.TRUE.equals(bridge.get("registered")),
                "live bridge must report the creative tab as registered");
        require("native_registry_creative_tab".equals(bridge.get("strategy")),
                "live bridge must use the native registry creative tab strategy");
        require(((Number) bridge.get("visibleItemCount")).intValue() == 1,
                "live bridge must expose the declared registry-backed tab population as visible");
        require(Boolean.TRUE.equals(bridge.get("nativeRegistryContentBacked")),
                "live bridge must preserve registry-backed tab population proof");

        TestCreativeModeTab tab = creativeTabRegistry.getValue(
                TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_modules_tab"));
        require(tab != null, "live creative tab registry must contain the Ashfall native modules tab");
        require("itemGroup.EchoAshfallNativeModules".equals(tab.title().value()),
                "live creative tab title must use the declaration translation key");
        require("echoashfallprotocol:portable_signal_scanner".equals(tab.icon().item().id()),
                "live creative tab icon must use the declaration icon item");
        require("echoashfallprotocol:portable_signal_scanner".equals(bridge.get("resolvedIconItem")),
                "live creative tab report must expose the actual resolved icon item");
        require(Boolean.TRUE.equals(bridge.get("declaredIconItemBackedByNativeRegistry")),
                "live creative tab declaration icon must be backed by native registry data");
        require(tab.beforeTabs().contains(TestCreativeModeTabs.BUILDING_BLOCKS),
                "live creative tab must preserve declaration ordering before building blocks");

        TestOutput output = new TestOutput();
        tab.emitItems(output);
        require(output.itemIds().equals(List.of("echoashfallprotocol:portable_signal_scanner")),
                "live creative tab output must be populated from the declared Native Loader registry item group");
        require(output.visibilities().stream().allMatch(TestTabVisibility.PARENT_AND_SEARCH_TABS::equals),
                "live creative tab output must use parent/search visibility");
    }

    private static void assertDeclaredCreativeTabSearchVisibilityDrivesLiveOutput() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));

        List<Map<String, Object>> plans = NativeLoaderRegistryCreativeBridge.plannedNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of("echoashfallprotocol:portable_signal_scanner"),
                List.of("echoashfallprotocol:parent_only_native_modules_tab"),
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:parent_only_native_modules_tab",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner"),
                        "searchVisibility", "parent_tabs",
                        "searchVisible", false
                ))
        );
        Map<String, Object> plan = planById(plans, "echoashfallprotocol:parent_only_native_modules_tab");
        require("parent_tabs".equals(plan.get("searchVisibility")),
                "declared parent-only creative-tab visibility must drive the native tab plan");
        require(!Boolean.TRUE.equals(plan.get("searchVisible")),
                "declared parent-only creative-tab visibility must not be marked search-visible");
        require(stringList(plan.get("searchVisibleItemIds")).isEmpty(),
                "parent-only creative-tab plan must not project search-visible item ids");

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of("echoashfallprotocol:portable_signal_scanner"),
                List.of("echoashfallprotocol:parent_only_native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:parent_only_native_modules_tab",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner"),
                        "searchVisibility", "parent_tabs",
                        "searchVisible", false
                ))
        );
        require(bridges.size() == 1, "parent-only live bridge must register one creative tab");
        Map<String, Object> bridge = bridges.get(0);
        require("parent_tabs".equals(bridge.get("searchVisibility")),
                "live creative tab report must preserve declared parent-only visibility");
        require(!Boolean.TRUE.equals(bridge.get("searchVisible")),
                "live creative tab report must preserve declared searchVisible=false");
        require(Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted")),
                "parent-only creative tab must satisfy release trust when live parent output proof matches");
        require(Boolean.TRUE.equals(bridge.get("creativeTabSearchOutputBacked")),
                "parent-only creative tab must not require search output proof");
        require(stringList(bridge.get("creativeTabSearchOutputProofItemIds")).isEmpty(),
                "parent-only creative tab must expose empty search proof ids");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(bridges)
                        .equals(List.of("echoashfallprotocol:portable_signal_scanner")),
                "parent-only creative tab visible aggregation must follow declared visibility proof");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(bridges) == 1,
                "parent-only creative tab must count as first-class presence when declared visibility is proven");

        TestCreativeModeTab tab = creativeTabRegistry.getValue(
                TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "parent_only_native_modules_tab"));
        TestOutput output = new TestOutput();
        tab.emitItems(output);
        require(output.itemIds().equals(List.of("echoashfallprotocol:portable_signal_scanner")),
                "parent-only creative tab output must still emit registry-backed items");
        require(output.visibilities().equals(List.of(TestTabVisibility.PARENT_TABS)),
                "declared parent-only creative-tab visibility must drive the live output visibility");
        require(output.searchTabStacks().isEmpty(),
                "parent-only creative tab output must not appear in the search collection");
    }

    private static void assertDeclaredCreativeTabItemsMustBeBackedOnLivePath() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of("echoashfallprotocol:portable_signal_scanner"),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:descriptor_only_missing_item"
                        )
                ))
        );
        require(bridges.size() == 1, "unbacked declared item fixture must return one creative tab report");
        Map<String, Object> bridge = bridges.get(0);
        require(Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "unbacked declared item fixture must still create a usable native product tab");
        require(stringList(bridge.get("unbackedDeclaredCreativeTabItems"))
                        .equals(List.of("echoashfallprotocol:descriptor_only_missing_item")),
                "live creative tab report must preserve unbacked declared descriptor-only items");
        require(!Boolean.TRUE.equals(bridge.get("declaredCreativeTabItemsBackedByNativeRegistry")),
                "live creative tab report must reject descriptor-only declared item backing");
        require(Boolean.TRUE.equals(bridge.get("creativeTabOutputBacked")),
                "live creative tab may prove output for the backed subset");
        require(!Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted")),
                "live creative tab with unbacked declared items must not satisfy release trust");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(bridges).isEmpty(),
                "live creative tab with unbacked declared items must not contribute visible release items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(bridges) == 0,
                "live creative tab with unbacked declared items must not count as first-class release presence");
    }

    private static void assertDeclaredCreativeTabIconMustResolveToNativeItem() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of("echoashfallprotocol:portable_signal_scanner"),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:descriptor_only_missing_icon",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner")
                ))
        );
        require(bridges.size() == 1, "missing declared icon fixture must return one creative tab report");
        Map<String, Object> bridge = bridges.get(0);
        require(Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "missing declared icon fixture must still create a native product tab with a fallback icon");
        require("echoashfallprotocol:descriptor_only_missing_icon".equals(bridge.get("declaredIconItem")),
                "missing declared icon fixture must preserve the descriptor icon id");
        require("echoashfallprotocol:portable_signal_scanner".equals(bridge.get("resolvedIconItem")),
                "missing declared icon fixture must report the native registry-backed fallback icon actually used");
        require("echoashfallprotocol:portable_signal_scanner".equals(bridge.get("iconItem")),
                "missing declared icon fixture must expose the live resolved icon as iconItem");
        require(!Boolean.TRUE.equals(bridge.get("declaredIconItemBackedByNativeRegistry")),
                "missing declared icon must not be marked registry-backed");
        require(Boolean.TRUE.equals(bridge.get("declaredIconItemFallbackUsed")),
                "missing declared icon must disclose fallback icon use");
        require(!Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted")),
                "native creative tab with a missing declared icon must not satisfy release trust");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(bridges).isEmpty(),
                "native creative tab with a missing declared icon must not contribute visible release items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(bridges) == 0,
                "native creative tab with a missing declared icon must not count as first-class release presence");

        TestCreativeModeTab tab = creativeTabRegistry.getValue(
                TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_modules_tab"));
        require(tab != null, "missing declared icon fixture must still register the product tab");
        require("echoashfallprotocol:portable_signal_scanner".equals(tab.icon().item().id()),
                "missing declared icon fixture must use the resolved native fallback icon in the live tab");
    }

    private static void assertCreatedCreativeTabMustProveLiveOutputItems() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("minecraft", "compass"),
                new TestItem("minecraft:compass"));

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of("echoashfallprotocol:portable_signal_scanner"),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner")
                ))
        );
        require(bridges.size() == 1, "missing live item fixture must return one creative tab report");
        Map<String, Object> bridge = bridges.get(0);
        require(Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "missing live item fixture must still register the requested native creative tab");
        require(Boolean.TRUE.equals(bridge.get("registered")),
                "missing live item fixture must report the tab as registered");
        require("minecraft:compass".equals(bridge.get("resolvedIconItem")),
                "missing live item fixture must disclose the non-native fallback icon actually used");
        require(!Boolean.TRUE.equals(bridge.get("resolvedIconItemBackedByNativeRegistry")),
                "non-native fallback icon must not be marked registry-backed");
        require(!Boolean.TRUE.equals(bridge.get("creativeTabOutputBacked")),
                "created tab must not claim output-backed proof when live items do not resolve");
        require(stringList(bridge.get("creativeTabOutputProofItemIds")).isEmpty(),
                "created tab with unresolved live items must expose empty output proof ids");
        require(!Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted")),
                "created tab with unresolved live output must not satisfy release trust");
        require(((Number) bridge.get("visibleItemCount")).intValue() == 0,
                "created tab with unresolved live output must not count planned items as visible");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(bridges).isEmpty(),
                "created tab with unresolved live output must not contribute visible native creative items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(bridges) == 0,
                "created tab with unresolved live output must not count as first-class release presence");

        TestCreativeModeTab tab = creativeTabRegistry.getValue(
                TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_modules_tab"));
        require(tab != null, "missing live item fixture must still create the product tab");
        TestOutput output = new TestOutput();
        tab.emitItems(output);
        require(output.itemIds().isEmpty(),
                "created product tab output must prove that unresolved native registry items were not emitted");
    }

    private static void assertLiveMultipleDeclaredCreativeGroupsInOneModule() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "scrap_knife"),
                new TestItem("echoashfallprotocol:scrap_knife"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "survival_cache"),
                new TestItem("echoashfallprotocol:survival_cache"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "industrial_aggregate"),
                new TestItem("echoashfallprotocol:industrial_aggregate"));

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of("echoashfallprotocol:industrial_aggregate"),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife",
                        "echoashfallprotocol:survival_cache"
                ),
                List.of(),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(
                        Map.of(
                                "registry", "creative_tab",
                                "id", "echoashfallprotocol:native_tools",
                                "titleKey", "itemGroup.EchoAshfallNativeTools",
                                "iconItem", "echoashfallprotocol:portable_signal_scanner",
                                "itemIds", List.of(
                                        "echoashfallprotocol:portable_signal_scanner",
                                        "echoashfallprotocol:scrap_knife"
                                ),
                                "orderAnchor", "minecraft:tools_and_utilities"
                        ),
                        Map.of(
                                "registry", "creative_tab",
                                "id", "echoashfallprotocol:native_supplies",
                                "titleKey", "itemGroup.EchoAshfallNativeSupplies",
                                "itemIds", List.of("echoashfallprotocol:survival_cache"),
                                "orderAnchor", "minecraft:ingredients"
                        )
                )
        );
        require(bridges.size() == 2, "live bridge must register one tab per declared same-module creative group");
        require(bridges.stream().allMatch(bridge -> Boolean.TRUE.equals(bridge.get("customTabCreated"))),
                "live same-module creative groups must be created as first-class native tabs");
        require(bridges.stream().allMatch(bridge -> Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted"))),
                "live same-module creative groups must be release-trusted");

        TestCreativeModeTab tools = creativeTabRegistry.getValue(
                TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_tools"));
        TestCreativeModeTab supplies = creativeTabRegistry.getValue(
                TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_supplies"));
        require(tools != null, "live registry must contain native tools creative group");
        require(supplies != null, "live registry must contain native supplies creative group");
        require("itemGroup.EchoAshfallNativeTools".equals(tools.title().value()),
                "live tools group title must use declaration title key");
        require("itemGroup.EchoAshfallNativeSupplies".equals(supplies.title().value()),
                "live supplies group title must use declaration title key");
        require("echoashfallprotocol:portable_signal_scanner".equals(tools.icon().item().id()),
                "live tools group icon must use declaration icon");
        require("echoashfallprotocol:survival_cache".equals(supplies.icon().item().id()),
                "live supplies group icon must fall back to its own registry-backed item");
        require(tools.beforeTabs().contains(TestCreativeModeTabs.TOOLS_AND_UTILITIES),
                "live tools group must preserve tools/utilities order anchor");
        require(supplies.beforeTabs().contains(TestCreativeModeTabs.INGREDIENTS),
                "live supplies group must preserve ingredients order anchor");

        TestOutput toolsOutput = new TestOutput();
        tools.emitItems(toolsOutput);
        require(toolsOutput.itemIds().equals(List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                "live tools group output must contain only its declared registry-backed item group");
        require(toolsOutput.visibilities().stream().allMatch(TestTabVisibility.PARENT_AND_SEARCH_TABS::equals),
                "live tools group output must use parent/search visibility");

        TestOutput suppliesOutput = new TestOutput();
        supplies.emitItems(suppliesOutput);
        require(suppliesOutput.itemIds().equals(List.of("echoashfallprotocol:survival_cache")),
                "live supplies group output must contain only its declared registry-backed item group");
        require(suppliesOutput.visibilities().stream().allMatch(TestTabVisibility.PARENT_AND_SEARCH_TABS::equals),
                "live supplies group output must use parent/search visibility");
    }

    private static void assertExistingCreativeTabIsPresentButNotCreated() {
        TestRegistry<TestCreativeModeTab> creativeTabRegistry = new TestRegistry<>();
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "scrap_knife"),
                new TestItem("echoashfallprotocol:scrap_knife"));
        TestIdentifier tabId = TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_modules_tab");
        TestCreativeModeTab existing = TestCreativeModeTab.builder()
                .title(TestComponent.literal("Existing Ashfall Native Modules"))
                .icon(() -> new TestItemStack(new TestItem("echoashfallprotocol:portable_signal_scanner")))
                .displayItems((parameters, output) -> {
                })
                .build();
        creativeTabRegistry.put(tabId, existing);

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                ),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:scrap_knife"
                        )
                ))
        );
        require(bridges.size() == 1, "existing-tab bridge must return one Ashfall creative tab report");
        Map<String, Object> bridge = bridges.get(0);
        require(Boolean.TRUE.equals(bridge.get("registered")),
                "existing exact native tab must still report registered");
        require(!Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "existing exact native tab must not be reported as created in this pass");
        require(Boolean.TRUE.equals(bridge.get("existingNativeCreativeTab")),
                "existing exact native tab must be identified as preexisting native content");
        require(!Boolean.TRUE.equals(bridge.get("firstClassNativeCreativeTabCreated")),
                "existing exact native tab must not count as newly created");
        require(Boolean.TRUE.equals(bridge.get("firstClassNativeCreativeTabPresent")),
                "existing exact native tab must count as first-class native tab presence");
        require(!Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted")),
                "existing exact native tab must not be trusted when its real output is empty");
        require(Boolean.TRUE.equals(bridge.get("nativeRegistryContentBacked")),
                "existing exact native tab must retain native registry-backed content evidence");
        require("existing_native_registry_tab".equals(bridge.get("creativeTabRegistrationMode")),
                "existing exact native tab must use the existing-tab registration mode");
        require(!Boolean.TRUE.equals(bridge.get("existingNativeCreativeTabOutputBacked")),
                "empty existing exact native tab must not claim output-backed content proof");
        require(stringList(bridge.get("existingNativeCreativeTabOutputProofItemIds")).isEmpty(),
                "empty existing exact native tab must expose empty output proof ids");
        require(((Number) bridge.get("visibleItemCount")).intValue() == 0,
                "empty existing exact native tab must not preserve planned item ids as visible content");
        require(creativeTabRegistry.getValue(tabId) == existing,
                "existing exact native tab must not be replaced by the bridge");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(bridges).isEmpty(),
                "empty existing exact native tab must not contribute planned visible native items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(bridges) == 0,
                "empty existing exact native tab must not count as release-visible first-class creative tab presence");
        require(NativeLoaderRegistryContentBridge.visibleCreativeTabItemCount(bridges) == 0,
                "empty existing exact native tab must not count visible registry-backed creative items");
        require(!NativeLoaderRegistryContentBridge.firstClassCreativeTabsAreRegistryBacked(bridges),
                "empty existing exact native tab must not satisfy registry-backed content metrics");

        TestRegistry<TestCreativeModeTab> populatedCreativeTabRegistry = new TestRegistry<>();
        TestCreativeModeTab populatedExisting = TestCreativeModeTab.builder()
                .title(TestComponent.literal("Existing Ashfall Native Modules"))
                .icon(() -> new TestItemStack(new TestItem("echoashfallprotocol:portable_signal_scanner")))
                .displayItems((parameters, output) -> {
                    output.accept(
                            new TestItemStack(new TestItem("echoashfallprotocol:portable_signal_scanner")),
                            TestTabVisibility.PARENT_AND_SEARCH_TABS
                    );
                    output.accept(
                            new TestItemStack(new TestItem("echoashfallprotocol:scrap_knife")),
                            TestTabVisibility.PARENT_AND_SEARCH_TABS
                    );
                })
                .build();
        populatedCreativeTabRegistry.put(tabId, populatedExisting);

        List<Map<String, Object>> populatedBridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                ),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                populatedCreativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:scrap_knife"
                        )
                ))
        );
        Map<String, Object> populatedBridge = populatedBridges.get(0);
        require(!Boolean.TRUE.equals(populatedBridge.get("customTabCreated")),
                "populated existing exact native tab must still not be reported as newly created");
        require(Boolean.TRUE.equals(populatedBridge.get("releaseCreativeTabTrusted")),
                "populated existing exact native tab must be trusted when real output proof matches registry-backed items");
        require(Boolean.TRUE.equals(populatedBridge.get("existingNativeCreativeTabOutputBacked")),
                "populated existing exact native tab must claim output-backed content proof");
        require(stringList(populatedBridge.get("existingNativeCreativeTabOutputProofItemIds")).equals(List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                "populated existing exact native tab must expose real output proof ids");
        require(Boolean.TRUE.equals(populatedBridge.get("existingNativeCreativeTabSearchOutputBacked")),
                "populated existing exact native tab must prove parent/search visibility for registry-backed items");
        require(Boolean.TRUE.equals(populatedBridge.get("creativeTabOutputVisibilityInspectable")),
                "populated existing exact native tab must inspect collection-backed search output visibility");
        require(stringList(populatedBridge.get("existingNativeCreativeTabSearchOutputProofItemIds")).equals(List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                "populated existing exact native tab must expose parent/search output proof ids");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(populatedBridges).equals(List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                "populated existing exact native tab must contribute registry-backed visible native items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(populatedBridges) == 1,
                "populated existing exact native tab must count as release-visible first-class creative tab presence");

        TestRegistry<TestCreativeModeTab> parentOnlyCreativeTabRegistry = new TestRegistry<>();
        TestCreativeModeTab parentOnlyExisting = TestCreativeModeTab.builder()
                .title(TestComponent.literal("Existing Ashfall Native Modules"))
                .icon(() -> new TestItemStack(new TestItem("echoashfallprotocol:portable_signal_scanner")))
                .displayItems((parameters, output) -> {
                    output.accept(
                            new TestItemStack(new TestItem("echoashfallprotocol:portable_signal_scanner")),
                            TestTabVisibility.PARENT_TABS
                    );
                    output.accept(
                            new TestItemStack(new TestItem("echoashfallprotocol:scrap_knife")),
                            TestTabVisibility.PARENT_TABS
                    );
                })
                .build();
        parentOnlyCreativeTabRegistry.put(tabId, parentOnlyExisting);
        List<Map<String, Object>> parentOnlyBridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                ),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                parentOnlyCreativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:scrap_knife"
                        )
                ))
        );
        Map<String, Object> parentOnlyBridge = parentOnlyBridges.get(0);
        require(Boolean.TRUE.equals(parentOnlyBridge.get("existingNativeCreativeTabOutputBacked")),
                "parent-only exact native tab may prove item output");
        require(Boolean.TRUE.equals(parentOnlyBridge.get("creativeTabOutputVisibilityInspectable")),
                "parent-only exact native tab must expose inspectable collection-backed search output visibility");
        require(stringList(parentOnlyBridge.get("existingNativeCreativeTabSearchOutputProofItemIds")).isEmpty(),
                "parent-only exact native tab must expose empty search output proof ids");
        require(!Boolean.TRUE.equals(parentOnlyBridge.get("existingNativeCreativeTabSearchOutputBacked")),
                "parent-only exact native tab must not prove parent/search visibility");
        require(!Boolean.TRUE.equals(parentOnlyBridge.get("releaseCreativeTabTrusted")),
                "parent-only exact native tab must not satisfy release trust without search-visible output");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(parentOnlyBridges).isEmpty(),
                "parent-only exact native tab must not contribute visible release items");
    }

    private static void assertMissingCreativeTabKeyIsNotTrustedFromGetValueFallback() {
        TestIdentifier tabId = TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "native_modules_tab");
        PhantomCreativeTabRegistry creativeTabRegistry = new PhantomCreativeTabRegistry(TestCreativeModeTab.builder()
                .title(TestComponent.literal("Phantom Ashfall Native Modules"))
                .icon(() -> new TestItemStack(new TestItem("echoashfallprotocol:portable_signal_scanner")))
                .displayItems((parameters, output) -> {
                })
                .build());
        TestRegistry<TestItemLike> itemRegistry = new TestRegistry<>();
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "portable_signal_scanner"),
                new TestItem("echoashfallprotocol:portable_signal_scanner"));
        itemRegistry.put(TestIdentifier.fromNamespaceAndPath("echoashfallprotocol", "scrap_knife"),
                new TestItem("echoashfallprotocol:scrap_knife"));

        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of(),
                List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                ),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                TestRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                creativeTabRegistry,
                itemRegistry,
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "titleKey", "itemGroup.EchoAshfallNativeModules",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:scrap_knife"
                        )
                ))
        );

        require(creativeTabRegistry.containsKey(tabId),
                "phantom getValue fallback must not prevent real native creative tab registration");
        Map<String, Object> bridge = bridges.get(0);
        require(Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "missing creative tab key must be created even when getValue returns a placeholder");
        require("created_native_registry_tab".equals(bridge.get("creativeTabRegistrationMode")),
                "missing creative tab key must use created-tab registration mode");
        require(Boolean.TRUE.equals(bridge.get("registered")),
                "missing creative tab key must report the newly registered tab");
    }

    private static void assertProjectedCreativeTabItemsAreNotVisibleReleaseEvidence() {
        List<Map<String, Object>> projectedTabs = List.of(Map.of(
                "tabId", "echoashfallprotocol:native_modules_tab",
                "registered", true,
                "firstClassNativeCreativeTabPresent", true,
                "nativeRegistryContentBacked", true,
                "releaseCreativeTabTrusted", false,
                "fallbackOnlyCreativeVisibility", false,
                "creativeTabItemsFromNativeRegistry", List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )
        ));
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(projectedTabs).isEmpty(),
                "projected creative tab items must not count as visible release evidence without trust");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(projectedTabs) == 0,
                "untrusted projected creative tab must not count as first-class creative tab presence");

        List<Map<String, Object>> fallbackProjectedTabs = List.of(Map.of(
                "tabId", "echoashfallprotocol:native_modules_tab",
                "registered", true,
                "firstClassNativeCreativeTabPresent", true,
                "nativeRegistryContentBacked", true,
                "releaseCreativeTabTrusted", true,
                "fallbackOnlyCreativeVisibility", true,
                "creativeTabItemsFromNativeRegistry", List.of("echoashfallprotocol:portable_signal_scanner")
        ));
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(fallbackProjectedTabs).isEmpty(),
                "fallback-only creative visibility must not count as visible native tab release evidence");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(fallbackProjectedTabs) == 0,
                "fallback-only creative visibility must not count as first-class creative tab presence");

        List<Map<String, Object>> emptyTrustedTabs = List.of(Map.of(
                "tabId", "echoashfallprotocol:empty_native_modules_tab",
                "registered", true,
                "firstClassNativeCreativeTabPresent", true,
                "nativeRegistryContentBacked", true,
                "releaseCreativeTabTrusted", true,
                "fallbackOnlyCreativeVisibility", false,
                "creativeTabItemsFromNativeRegistry", List.of()
        ));
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(emptyTrustedTabs).isEmpty(),
                "empty trusted creative tab must not count visible native tab items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(emptyTrustedTabs) == 0,
                "empty trusted creative tab must not count as first-class creative tab presence");

        List<Map<String, Object>> planOnlyTrustedTabs = List.of(Map.ofEntries(
                Map.entry("tabId", "echoashfallprotocol:plan_only_native_modules_tab"),
                Map.entry("registered", true),
                Map.entry("firstClassNativeCreativeTabPresent", true),
                Map.entry("nativeRegistryContentBacked", true),
                Map.entry("releaseCreativeTabTrusted", true),
                Map.entry("creativeTabOutputBacked", true),
                Map.entry("declaredIconItemBackedByNativeRegistry", true),
                Map.entry("resolvedIconItemBackedByNativeRegistry", true),
                Map.entry("fallbackOnlyCreativeVisibility", false),
                Map.entry("creativeTabItemsFromNativeRegistry", List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                Map.entry("creativeTabOutputProofItemIds", List.of("echoashfallprotocol:portable_signal_scanner"))
        ));
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(planOnlyTrustedTabs).isEmpty(),
                "trusted-looking creative tab plan items must not count without matching output proof");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(planOnlyTrustedTabs) == 0,
                "trusted-looking creative tab presence must require output proof for every registry-backed item");

        List<Map<String, Object>> searchUnbackedTrustedTabs = List.of(Map.ofEntries(
                Map.entry("tabId", "echoashfallprotocol:search_unbacked_native_modules_tab"),
                Map.entry("registered", true),
                Map.entry("firstClassNativeCreativeTabPresent", true),
                Map.entry("nativeRegistryContentBacked", true),
                Map.entry("releaseCreativeTabTrusted", true),
                Map.entry("creativeTabOutputBacked", true),
                Map.entry("creativeTabSearchOutputBacked", false),
                Map.entry("declaredCreativeTabItemsBackedByNativeRegistry", true),
                Map.entry("declaredIconItemBackedByNativeRegistry", true),
                Map.entry("resolvedIconItemBackedByNativeRegistry", true),
                Map.entry("fallbackOnlyCreativeVisibility", false),
                Map.entry("creativeTabItemsFromNativeRegistry", List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                Map.entry("creativeTabOutputProofItemIds", List.of(
                        "echoashfallprotocol:portable_signal_scanner",
                        "echoashfallprotocol:scrap_knife"
                )),
                Map.entry("creativeTabSearchOutputProofItemIds", List.of("echoashfallprotocol:portable_signal_scanner"))
        ));
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(searchUnbackedTrustedTabs).isEmpty(),
                "trusted-looking creative tab output must not count without complete search output proof");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(searchUnbackedTrustedTabs) == 0,
                "trusted-looking creative tab presence must require search output proof for every registry-backed item");

        List<Map<String, Object>> outputOrderedTabs = List.of(Map.ofEntries(
                Map.entry("tabId", "echoashfallprotocol:output_ordered_native_modules_tab"),
                Map.entry("registered", true),
                Map.entry("firstClassNativeCreativeTabPresent", true),
                Map.entry("nativeRegistryContentBacked", true),
                Map.entry("releaseCreativeTabTrusted", true),
                Map.entry("creativeTabOutputBacked", true),
                Map.entry("creativeTabSearchOutputBacked", true),
                Map.entry("declaredCreativeTabItemsBackedByNativeRegistry", true),
                Map.entry("declaredIconItemBackedByNativeRegistry", true),
                Map.entry("resolvedIconItemBackedByNativeRegistry", true),
                Map.entry("fallbackOnlyCreativeVisibility", false),
                Map.entry("creativeTabItemsFromNativeRegistry", List.of(
                        " EchoAshfallProtocol:Portable_Signal_Scanner ",
                        "echoashfallprotocol:scrap_knife"
                )),
                Map.entry("creativeTabOutputProofItemIds", List.of(
                        "echoashfallprotocol:scrap_knife",
                        " ECHOASHFALLPROTOCOL:PORTABLE_SIGNAL_SCANNER "
                )),
                Map.entry("creativeTabSearchOutputProofItemIds", List.of(
                        "echoashfallprotocol:scrap_knife",
                        " ECHOASHFALLPROTOCOL:PORTABLE_SIGNAL_SCANNER "
                ))
        ));
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(outputOrderedTabs).equals(List.of(
                        "echoashfallprotocol:scrap_knife",
                        "echoashfallprotocol:portable_signal_scanner"
                )),
                "visible native creative tab items must preserve live output proof order, not planned item order");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(outputOrderedTabs) == 1,
                "output-ordered creative tab with normalized complete proof must count as first-class release presence");
    }

    private static void assertCreativeTabFallbackIsNotTrustedAsRegistryBacked() {
        List<Map<String, Object>> bridges = NativeLoaderRegistryCreativeBridge.registerNativeCreativeTabs(
                new TestProfile(),
                List.of("echoashfallprotocol:industrial_aggregate"),
                List.of("echoashfallprotocol:portable_signal_scanner"),
                List.of("echoashfallprotocol:native_modules_tab"),
                TestIdentifier.class,
                BrokenRegistry.class,
                TestCreativeModeTab.class,
                TestCreativeModeTabs.class,
                TestComponent.class,
                TestItemStack.class,
                TestItemLike.class,
                TestTabVisibility.class,
                TestOutput.class,
                new TestRegistry<TestCreativeModeTab>(),
                new TestRegistry<TestItemLike>(),
                List.of(Map.of(
                        "registry", "creative_tab",
                        "id", "echoashfallprotocol:native_modules_tab",
                        "iconItem", "echoashfallprotocol:portable_signal_scanner",
                        "itemIds", List.of("echoashfallprotocol:portable_signal_scanner")
                ))
        );
        require(bridges.size() == 1, "fallback bridge must return one attempted tab report");
        Map<String, Object> bridge = bridges.get(0);
        require("existing_vanilla_creative_containers_fallback".equals(bridge.get("strategy")),
                "forced registration failure must report fallback strategy");
        require(!Boolean.TRUE.equals(bridge.get("customTabCreated")),
                "fallback bridge must not report a custom tab as created");
        require(!Boolean.TRUE.equals(bridge.get("registered")),
                "fallback bridge must not report the tab as registered");
        require(!Boolean.TRUE.equals(bridge.get("nativeRegistryContentBacked")),
                "fallback bridge must not be trusted as registry-backed tab content");
        require(!Boolean.TRUE.equals(bridge.get("releaseCreativeTabTrusted")),
                "fallback bridge must not be release-trusted creative-tab evidence");
        require(Boolean.TRUE.equals(bridge.get("fallbackOnlyCreativeVisibility")),
                "fallback bridge must explicitly identify fallback-only creative visibility");
        require(((Number) bridge.get("visibleItemCount")).intValue() == 0,
                "fallback bridge must not count planned registry items as visible product-tab content");
        require(NativeLoaderRegistryCreativeBridge.visibleNativeCreativeTabItems(bridges).isEmpty(),
                "fallback bridge must not contribute visible native creative tab items");
        require(NativeLoaderRegistryContentBridge.firstClassCreativeTabPresenceCount(bridges) == 0,
                "fallback bridge must not count as first-class native creative tab presence");
        require(!NativeLoaderRegistryContentBridge.firstClassCreativeTabsAreRegistryBacked(bridges),
                "fallback bridge must not satisfy registry-backed creative-tab content metrics");
    }

    public static final class BrokenRegistry {
        private BrokenRegistry() {
        }

        public static void register(TestRegistry<?> registry, TestIdentifier id, Object value) {
            throw new IllegalStateException("forced creative tab registration failure");
        }
    }

    private static void assertCreativeVisibilityBridgeDoesNotPromoteVanillaAugmentationAsNativeTab() {
        require(NativeLoaderRegistryCreativeVisibilityBridge.firstClassNativeCreativeTabBridgeApplied(Map.of(
                        "registeredCreativeTabCount", 1,
                        "nativeCreativeModuleTabContentVisible", false,
                        "creativeVisibilityBridgeApplied", false,
                        "augmentedCreativeTabCount", 0
                )),
                "registered first-class native creative tab count must satisfy native creative-tab bridge evidence");
        require(NativeLoaderRegistryCreativeVisibilityBridge.firstClassNativeCreativeTabBridgeApplied(Map.of(
                        "registeredCreativeTabCount", 0,
                        "nativeCreativeModuleTabContentVisible", true,
                        "creativeVisibilityBridgeApplied", true,
                        "augmentedCreativeTabCount", 0
                )),
                "native creative module tab content visibility must satisfy native creative-tab bridge evidence");
        require(!NativeLoaderRegistryCreativeVisibilityBridge.firstClassNativeCreativeTabBridgeApplied(Map.of(
                        "registeredCreativeTabCount", 0,
                        "nativeCreativeModuleTabContentVisible", false,
                        "creativeVisibilityBridgeApplied", true,
                        "augmentedCreativeTabCount", 2,
                        "visibleItemCount", 4,
                        "visibleItems", List.of(
                                "echoashfallprotocol:portable_signal_scanner",
                                "echoashfallprotocol:scrap_knife"
                        )
                )),
                "vanilla/search creative visibility augmentation must not masquerade as native creative-tab bridge evidence");
    }

    @SuppressWarnings("unchecked")
    private static void assertBootstrapRegistryHostPromotesAllFirstClassDeclarations() {
        try {
            Map<String, Object> aggregated = NativeLoaderRuntimeBridgeAggregator.aggregateSdkRegistryDeclarations(Map.of(
                    "qa_alias_module", Map.of(
                            "activated", true,
                            "nativeAdapterCodeExecuted", true,
                            "entrypoint", "qa.NativeModule",
                            "loadedClassName", "qa.NativeModule",
                            "registryBridge", Map.of("registrations", aliasFirstClassDeclarations())
                    )
            ));
            require(stringList(aggregated.get("creativeTabIds")).contains("qa:creative_tab"),
                    "aggregator must canonicalize creative_tabs alias into creative_tab input");
            require(((Number) aggregated.get("declarationCount")).intValue() == EchoNativeRegistryHost.firstClassRegistryKinds().size(),
                    "aggregator must retain all first-class alias declarations");
            List<String> aggregatedKinds = objectList(aggregated.get("declarations")).stream()
                    .map(declaration -> String.valueOf(declaration.get("registry")))
                    .distinct()
                    .sorted()
                    .toList();
            require(aggregatedKinds.equals(EchoNativeRegistryHost.firstClassRegistryKinds().stream().sorted().toList()),
                    "aggregator must canonicalize first-class alias registry names");

            List<Map<String, Object>> declarations = EchoNativeRegistryHost.firstClassRegistryKinds().stream()
                    .map(kind -> Map.<String, Object>of(
                            "registry", kind,
                            "id", "qa:" + kind,
                            "declarationMetadataToken", "qa-metadata-" + kind,
                            "source", "qa:first_class_bootstrap_declaration"
                    ))
                    .toList();
            List<Map<String, Object>> normalizedBridgeDeclarations =
                    normalizeRegistryDeclarations(mergeDeclarations(declarations, aliasFirstClassDeclarations()));
            require(normalizedBridgeDeclarations.size() == EchoNativeRegistryHost.firstClassRegistryKinds().size(),
                    "bootstrap registry bridge must deduplicate alias and canonical first-class declarations");
            List<Map<String, Object>> mergedListDeclarations = normalizeRegistryDeclarations(List.of(
                    Map.of(
                            "registry", "creative_tab",
                            "id", "qa:merged_tab",
                            "itemIds", List.of("qa:item_a"),
                            "surfaceIds", List.of("terminal"),
                            "declarationMetadataToken", "qa-merged-canonical"
                    ),
                    Map.of(
                            "registry", "creative_tabs",
                            "id", "qa:merged_tab",
                            "itemIds", List.of("qa:item_b", "qa:item_a"),
                            "surfaceIds", List.of("index")
                    )
            ));
            require(mergedListDeclarations.size() == 1,
                    "bootstrap registry bridge must merge duplicate canonical and alias creative-tab declarations");
            require(stringList(mergedListDeclarations.get(0).get("itemIds")).equals(List.of("qa:item_a", "qa:item_b")),
                    "bootstrap registry bridge must union duplicate creative-tab itemIds without losing canonical items");
            require(stringList(mergedListDeclarations.get(0).get("surfaceIds")).equals(List.of("terminal", "index")),
                    "bootstrap registry bridge must union duplicate creative-tab surfaceIds without losing canonical surfaces");
            Method method = NativeLoaderRegistryBridge.class.getDeclaredMethod(
                    "registerThroughNativeRegistryHost",
                    List.class,
                    List.class,
                    List.class,
                    List.class,
                    List.class
            );
            method.setAccessible(true);
            Map<String, Object> bridge = (Map<String, Object>) method.invoke(
                    null,
                    List.of("qa:block"),
                    List.of("qa:item"),
                    List.of("qa:creative_tab"),
                    List.of(Map.of("registry", "creative_tab", "id", "qa:creative_tab")),
                    normalizedBridgeDeclarations
            );
            require(((Number) bridge.get("registeredCount")).intValue() == EchoNativeRegistryHost.firstClassRegistryKinds().size(),
                    "bootstrap registry host must mutate every declared first-class registry kind");
            List<Map<String, Object>> registrations = objectList(bridge.get("registrations"));
            require("qa-metadata-item".equals(registrationProperties(registrations, "item", "qa:item")
                            .get("declarationMetadataToken")),
                    "bootstrap item id input must preserve richer SDK registry declaration metadata");
            require("qa-metadata-block".equals(registrationProperties(registrations, "block", "qa:block")
                            .get("declarationMetadataToken")),
                    "bootstrap block id input must preserve richer SDK registry declaration metadata");
            require("qa-metadata-creative_tab".equals(registrationProperties(
                            registrations,
                            "creative_tab",
                            "qa:creative_tab"
                    ).get("declarationMetadataToken")),
                    "bootstrap creative-tab id input must preserve richer SDK registry declaration metadata");
            Map<String, Object> mergedCreativeTabBridge = (Map<String, Object>) method.invoke(
                    null,
                    List.of(),
                    List.of(),
                    List.of("qa:merged_tab"),
                    List.of(Map.of(
                            "registry", "creative_tab",
                            "id", "qa:merged_tab",
                            "itemIds", List.of("qa:item_c"),
                            "surfaceIds", List.of("lens")
                    )),
                    mergedListDeclarations
            );
            List<Map<String, Object>> mergedRegistrations = objectList(mergedCreativeTabBridge.get("registrations"));
            Map<String, Object> mergedProperties = registrationProperties(
                    mergedRegistrations,
                    "creative_tab",
                    "qa:merged_tab"
            );
            require(stringList(mergedProperties.get("itemIds")).equals(List.of("qa:item_a", "qa:item_b", "qa:item_c")),
                    "bootstrap creative-tab registration must preserve unioned registry and declaration itemIds");
            require(stringList(mergedProperties.get("surfaceIds")).equals(List.of("terminal", "index", "lens")),
                    "bootstrap creative-tab registration must preserve unioned registry and declaration surfaceIds");
            require(((Number) bridge.get("unsupportedCount")).intValue() == 0,
                    "bootstrap registry host must not leave first-class declarations unsupported");
            require(((Number) bridge.get("failedCount")).intValue() == 0,
                    "bootstrap registry host must not fail first-class declarations");
            Map<String, Object> report = object(bridge.get("report"));
            require(((Number) report.get("trustedRegistryMutatedEntries")).intValue()
                            == EchoNativeRegistryHost.firstClassRegistryKinds().size(),
                    "bootstrap registry host report must count all first-class declarations as trusted mutation");
            require(stringList(report.get("registeredOnlyFirstClassRegistryKinds")).isEmpty(),
                    "bootstrap registry host must not report REGISTERED-only first-class declarations");
            require(stringList(report.get("untrustedMutationFirstClassRegistryKinds")).isEmpty(),
                    "bootstrap registry host must not report untrusted first-class mutation declarations");
            List<String> expectedRecordIds = EchoNativeRegistryHost.firstClassRegistryKinds().stream()
                    .map(kind -> kind + ":qa:" + kind)
                    .sorted()
                    .toList();
            Map<String, Object> bridgeEvidence = object(report.get("liveRegistryBridgeEvidence"));
            require(stringList(bridgeEvidence.get("mutatedRecordIds")).equals(expectedRecordIds),
                    "bootstrap registry bridge evidence must expose aggregate mutation record ids");
            require(((Number) bridgeEvidence.get("mutatedRecordCount")).intValue() == expectedRecordIds.size(),
                    "bootstrap registry bridge evidence must count aggregate mutation records");
            Map<String, Object> reconciliation = object(report.get("registryBridgeMutationReconciliation"));
            require(Boolean.TRUE.equals(reconciliation.get("bridgeEvidenceMatchesTrustedEntries")),
                    "bootstrap registry bridge aggregate evidence must reconcile with trusted host entries");
            require(stringList(reconciliation.get("missingFromBridgeEvidence")).isEmpty(),
                    "bootstrap registry bridge evidence must not miss trusted host entries");
            require(stringList(reconciliation.get("bridgeEvidenceWithoutTrustedEntry")).isEmpty(),
                    "bootstrap registry bridge evidence must not report stale aggregate ids");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("bootstrap first-class registry declaration verifier failed", exception);
        }
    }

    private static void assertBootstrapAppliedRegistryBridgeCanonicalizesDirectIds() {
        try {
            Object bridge = NativeLoaderBootstrapRegistryBridgeFactory.bootstrapAppliedLiveBridge();
            EchoNativeLoadStatus status = status(invokeBootstrapBridge(
                    bridge,
                    "register",
                    new Class<?>[]{String.class, String.class, String.class, String.class, Map.class},
                    "creative_tabs",
                    "qa",
                    "qa:direct_bootstrap_tab",
                    "native://agent3/bootstrap/direct-namespaced-creative-tab",
                    Map.of("idShape", "descriptor_full_id")
            ));
            require(status == EchoNativeLoadStatus.MUTATED,
                    "bootstrap applied registry bridge must mutate direct namespaced descriptor-shaped ids");
            Map<String, Object> localLookupRecord = object(invokeBootstrapBridge(
                    bridge,
                    "registryMutationRecord",
                    new Class<?>[]{String.class, String.class, String.class},
                    "creative_tab",
                    "qa",
                    "direct_bootstrap_tab"
            ));
            Map<String, Object> fullLookupRecord = object(invokeBootstrapBridge(
                    bridge,
                    "registryMutationRecord",
                    new Class<?>[]{String.class, String.class, String.class},
                    "creative_tabs",
                    "qa",
                    "qa:direct_bootstrap_tab"
            ));
            require("creative_tab".equals(localLookupRecord.get("registry")),
                    "bootstrap applied bridge must canonicalize direct registry aliases");
            require("qa:direct_bootstrap_tab".equals(localLookupRecord.get("fullId")),
                    "bootstrap applied bridge must canonicalize direct namespaced ids to one namespace:id full id");
            require(localLookupRecord.equals(fullLookupRecord),
                    "bootstrap applied bridge mutation lookup must accept both local and namespaced id forms");
            Map<String, Object> evidence = object(invokeBootstrapBridge(
                    bridge,
                    "registryEvidence",
                    new Class<?>[0]
            ));
            require(stringList(evidence.get("mutatedRecordIds"))
                            .equals(List.of("creative_tab:qa:direct_bootstrap_tab")),
                    "bootstrap applied bridge aggregate evidence must expose canonical direct namespaced mutation id");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("bootstrap applied registry bridge direct-id verifier failed", exception);
        }
    }

    private static Object invokeBootstrapBridge(
            Object bridge,
            String methodName,
            Class<?>[] parameterTypes,
            Object... args
    ) throws ReflectiveOperationException {
        Method method = bridge.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(bridge, args);
    }

    private static EchoNativeLoadStatus status(Object value) {
        if (value instanceof EchoNativeLoadStatus status) {
            return status;
        }
        return EchoNativeLoadStatus.valueOf(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> normalizeRegistryDeclarations(
            List<Map<String, Object>> declarations
    ) throws ReflectiveOperationException {
        Method method = NativeLoaderRegistryBridge.class.getDeclaredMethod("normalizeRegistryDeclarations", List.class);
        method.setAccessible(true);
        return (List<Map<String, Object>>) method.invoke(null, declarations);
    }

    private static List<Map<String, Object>> aliasFirstClassDeclarations() {
        return List.of(
                Map.of("registry", "items", "id", "qa:item"),
                Map.of("registry", "blocks", "id", "qa:block"),
                Map.of("registry", "entities", "id", "qa:entity"),
                Map.of("registry", "block_entities", "id", "qa:block_entity"),
                Map.of("registry", "menus", "id", "qa:menu"),
                Map.of("registry", "sounds", "id", "qa:sound"),
                Map.of("registry", "particles", "id", "qa:particle"),
                Map.of("registry", "effects", "id", "qa:effect"),
                Map.of("registry", "commands", "id", "qa:command"),
                Map.of("registry", "data_components", "id", "qa:data_component"),
                Map.of("registry", "recipes", "id", "qa:recipe"),
                Map.of("registry", "creative_tabs", "id", "qa:creative_tab"),
                Map.of("registry", "biomes", "id", "qa:biome"),
                Map.of("registry", "world_generators", "id", "qa:worldgen"),
                Map.of("registry", "client_assets", "id", "qa:client_asset")
        );
    }

    private static List<Map<String, Object>> mergeDeclarations(
            List<Map<String, Object>> exactDeclarations,
            List<Map<String, Object>> aliasDeclarations
    ) {
        List<Map<String, Object>> merged = new ArrayList<>(exactDeclarations);
        merged.addAll(aliasDeclarations);
        return List.copyOf(merged);
    }

    private static Map<String, Object> registrationProperties(
            List<Map<String, Object>> registrations,
            String registry,
            String id
    ) {
        return registrations.stream()
                .filter(registration -> registry.equals(String.valueOf(registration.get("registry")))
                        && id.equals(String.valueOf(registration.get("id"))))
                .findFirst()
                .map(registration -> object(registration.get("properties")))
                .orElseThrow(() -> new IllegalStateException(
                        "missing bootstrap registration properties for " + registry + ":" + id));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static Map<String, Object> planById(List<Map<String, Object>> plans, String tabId) {
        for (Map<String, Object> plan : plans) {
            if (tabId.equals(plan.get("tabId"))) {
                return plan;
            }
        }
        throw new IllegalStateException("missing creative tab plan: " + tabId + " in " + plans);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (Object item : iterable) {
            values.add(String.valueOf(item));
        }
        return List.copyOf(values);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return Map.copyOf(result);
    }

    private static List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : iterable) {
            result.add(object(item));
        }
        return List.copyOf(result);
    }

    private static final class TestProfile implements EchoNativeBootstrapProductProfile {
        @Override
        public String namespace() {
            return "echoashfallprotocol";
        }

        @Override
        public String nativeLoaderMainLabel() {
            return "Ashfall";
        }

        @Override
        public String nativeLoaderClientLabel() {
            return "Ashfall Client";
        }

        @Override
        public String nativeLoaderSessionMessage() {
            return "Ashfall session";
        }

        @Override
        public String nativeLoaderWindowTitle() {
            return "Ashfall";
        }

        @Override
        public String nativeLoaderAdapterCoreServiceId() {
            return "adaptercore";
        }

        @Override
        public String nativeLoaderRuntimeHostClass() {
            return "NativeRuntimeHost";
        }

        @Override
        public String nativeMinecraftRuntimeHostClass() {
            return "NativeMinecraftHost";
        }

        @Override
        public String nativeMinecraftRuntimeHostId() {
            return "minecraft";
        }

        @Override
        public String nativeLoaderBackendClass() {
            return "NativeBackend";
        }

        @Override
        public String nativeLoaderRuntimeLane() {
            return "echo.native";
        }

        @Override
        public String nativeUiActionCommand() {
            return "native.ui";
        }

        @Override
        public String nativeGameplayDisplayName() {
            return "Ashfall";
        }

        @Override
        public Map<String, List<String>> nativeCreativeTabPreferredIcons() {
            return Map.of("echoashfallprotocol", List.of("echoashfallprotocol:portable_signal_scanner"));
        }

        @Override
        public List<String> requiredGameplayHandlerEvents() {
            return List.of();
        }

        @Override
        public List<String> requiredAgent7WorldLiveHooks() {
            return List.of();
        }

        @Override
        public List<String> requiredLiveMutationSurfaces() {
            return List.of();
        }

        @Override
        public List<NativeEntityDefinition> nativeEntities() {
            return List.of();
        }
    }

    public record TestIdentifier(String namespace, String path) {
        public static TestIdentifier fromNamespaceAndPath(String namespace, String path) {
            return new TestIdentifier(namespace, path);
        }
    }

    public static class TestRegistry<T> {
        private final Map<TestIdentifier, T> values = new LinkedHashMap<>();

        public boolean containsKey(TestIdentifier id) {
            return values.containsKey(id);
        }

        public T get(TestIdentifier id) {
            return values.get(id);
        }

        public T getValue(TestIdentifier id) {
            return values.get(id);
        }

        public void put(TestIdentifier id, T value) {
            values.put(id, value);
        }

        @SuppressWarnings("unchecked")
        public static void register(TestRegistry<?> registry, TestIdentifier id, Object value) {
            ((TestRegistry<Object>) registry).put(id, value);
        }
    }

    public static final class PhantomCreativeTabRegistry extends TestRegistry<TestCreativeModeTab> {
        private final TestCreativeModeTab phantom;

        public PhantomCreativeTabRegistry(TestCreativeModeTab phantom) {
            this.phantom = phantom;
        }

        @Override
        public TestCreativeModeTab getValue(TestIdentifier id) {
            TestCreativeModeTab value = super.getValue(id);
            return value == null ? phantom : value;
        }
    }

    public interface TestItemLike {
        String id();
    }

    public record TestItem(String id) implements TestItemLike {
    }

    public record TestItemStack(TestItemLike item) {
    }

    public record TestComponent(String value, boolean translatable) {
        public static TestComponent translatable(String key) {
            return new TestComponent(key, true);
        }

        public static TestComponent literal(String text) {
            return new TestComponent(text, false);
        }
    }

    public static final class TestTabVisibility {
        public static final TestTabVisibility PARENT_AND_SEARCH_TABS = new TestTabVisibility("parent_and_search_tabs");
        public static final TestTabVisibility PARENT_TABS = new TestTabVisibility("parent_tabs");

        private final String id;

        private TestTabVisibility(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TestTabVisibility visibility && id.equals(visibility.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static final class TestOutput {
        private final List<TestItemStack> stacks = new ArrayList<>();
        private final List<TestTabVisibility> visibilities = new ArrayList<>();

        public void accept(TestItemStack stack, TestTabVisibility visibility) {
            stacks.add(stack);
            visibilities.add(visibility);
        }

        public List<TestItemStack> stacks() {
            return List.copyOf(stacks);
        }

        public List<TestItemStack> searchTabStacks() {
            List<TestItemStack> items = new ArrayList<>();
            for (int index = 0; index < stacks.size() && index < visibilities.size(); index++) {
                if (TestTabVisibility.PARENT_AND_SEARCH_TABS.equals(visibilities.get(index))) {
                    items.add(stacks.get(index));
                }
            }
            return List.copyOf(items);
        }

        public List<String> itemIds() {
            return stacks.stream().map(stack -> stack.item().id()).toList();
        }

        public List<TestTabVisibility> visibilities() {
            return List.copyOf(visibilities);
        }
    }

    public static final class TestCreativeModeTabs {
        public static final Object BUILDING_BLOCKS = new Object();
        public static final Object INGREDIENTS = new Object();
        public static final Object TOOLS_AND_UTILITIES = new Object();
        public static final Object COMBAT = new Object();
    }

    public static final class TestCreativeModeTab {
        private final TestComponent title;
        private final Supplier<Object> icon;
        private final DisplayItemsGenerator generator;
        private final List<Object> beforeTabs;

        private TestCreativeModeTab(
                TestComponent title,
                Supplier<Object> icon,
                DisplayItemsGenerator generator,
                List<Object> beforeTabs
        ) {
            this.title = title;
            this.icon = icon;
            this.generator = generator;
            this.beforeTabs = List.copyOf(beforeTabs);
        }

        public static Builder builder() {
            return new Builder();
        }

        public TestComponent title() {
            return title;
        }

        public TestItemStack icon() {
            return (TestItemStack) icon.get();
        }

        public List<Object> beforeTabs() {
            return beforeTabs;
        }

        public void emitItems(TestOutput output) {
            generator.accept(new Object(), output);
        }

        public List<TestItemStack> getDisplayItems() {
            return generatedOutput().stacks();
        }

        public List<TestItemStack> getSearchTabDisplayItems() {
            return generatedOutput().searchTabStacks();
        }

        private TestOutput generatedOutput() {
            TestOutput output = new TestOutput();
            if (generator != null) {
                generator.accept(new Object(), output);
            }
            return output;
        }

        public interface DisplayItemsGenerator {
            void accept(Object parameters, TestOutput output);
        }

        public static final class Builder {
            private TestComponent title;
            private Supplier<Object> icon;
            private DisplayItemsGenerator generator;
            private final List<Object> beforeTabs = new ArrayList<>();

            public Builder title(TestComponent title) {
                this.title = title;
                return this;
            }

            public Builder withTabsBefore(Object... tabs) {
                beforeTabs.addAll(List.of(tabs));
                return this;
            }

            public Builder icon(Supplier<Object> icon) {
                this.icon = icon;
                return this;
            }

            public Builder displayItems(DisplayItemsGenerator generator) {
                this.generator = generator;
                return this;
            }

            public TestCreativeModeTab build() {
                return new TestCreativeModeTab(title, icon, generator, beforeTabs);
            }
        }
    }
}
