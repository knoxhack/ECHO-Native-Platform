package dev.echo.nativeplatform.bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;

public final class EchoNativeAgent3AshfallCreativeTabWiringGateMain {
    private EchoNativeAgent3AshfallCreativeTabWiringGateMain() {
    }

    public static void main(String[] args) throws Exception {
        Path workspace = Path.of("").toAbsolutePath().normalize().getParent();
        Path providerPath = workspace.resolve("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/nativebridge/AshfallNativeProductBridgeProvider.java");
        Path creativeTabsPath = workspace.resolve("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/registry/ModCreativeTabs.java");
        Path nativeModulePath = workspace.resolve("addons/echoashfallprotocol/src/main/java/com/knoxhack/echoashfallprotocol/EchoAshfallNativeModule.java");
        String provider = Files.readString(providerPath);
        String creativeTabs = Files.readString(creativeTabsPath);
        String nativeModule = Files.readString(nativeModulePath);
        String registryBridge = between(
                provider,
                "private static final class AshfallProductRegistryBridge",
                "private static final class AshfallProductClientSurfaceBridge"
        );

        require(provider.contains("nativeModulesCreativeTabRegistryHook"),
                "Ashfall product bridge must use a first-class native modules creative-tab hook");
        require(provider.contains("\"id\", \"echoashfallprotocol:native_modules_tab\""),
                "Ashfall product bridge must register the native modules tab id, not a generic module projection");
        require(provider.contains("\"nativeCreativeTabRegistryBacked\", source.sourceResolved()"),
                "Ashfall product bridge hook must only mark the native modules tab as registry-backed when the typed source resolves");
        require(provider.contains("\"nativeCreativeTabSourceBacked\", source.sourceResolved()"),
                "Ashfall product bridge hook must only mark the native modules tab as source-backed when the typed source resolves");
        require(provider.contains("\"nativeCreativeTabPopulationOwnerClass\"")
                        && provider.contains("\"com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs\""),
                "Ashfall product bridge hook must name ModCreativeTabs as the population owner");
        require(provider.contains("\"orderAnchor\", \"minecraft:building_blocks\"")
                        && provider.contains("\"orderStrategy\", \"with_tabs_before_anchor\""),
                "Ashfall product bridge hook must carry the live native modules creative-tab ordering metadata");
        require(provider.contains("\"searchVisibility\", \"parent_and_search_tabs\"")
                        && provider.contains("\"searchVisible\", true"),
                "Ashfall product bridge hook must carry native modules creative-tab search visibility metadata");
        require(provider.contains("\"nativeCreativeTabPopulationOwnerMember\"")
                        && provider.contains("\"nativeLoaderRegistryBackedCreativeItemIds\""),
                "Ashfall product bridge hook must name the Native Loader registry-backed item source");
        require(provider.contains("\"itemIds\", source.itemIds()"),
                "Ashfall product bridge hook must carry the full native modules tab population into registry hook properties");
        require(provider.contains("\"registryBackedItemIds\", source.registryBackedItemIds()"),
                "Ashfall product bridge hook must carry the registry-backed native modules item source separately");
        require(provider.contains("\"featuredItemIds\", source.featuredItemIds()"),
                "Ashfall product bridge hook must carry featured native modules items as ordering metadata");
        require(provider.contains("\"sourceNamespaces\", source.sourceNamespaces()"),
                "Ashfall product bridge hook must carry source namespaces from ModCreativeTabs");
        require(!provider.contains("\"itemIds\", ASHFALL_NATIVE_MODULE_CREATIVE_FEATURED_ITEMS"),
                "Ashfall product bridge hook must not use the featured list as the full native modules tab population");
        require(provider.contains("ModCreativeTabs.nativeModuleCreativeItemIds()"),
                "Ashfall product bridge hook must directly read the full native modules tab source method");
        require(provider.contains("ModCreativeTabs.nativeLoaderRegistryBackedCreativeItemIds()"),
                "Ashfall product bridge hook must directly read the Native Loader registry-backed item source method");
        require(provider.contains("ModCreativeTabs.nativeModuleCreativeFeaturedItemIds()"),
                "Ashfall product bridge hook must directly read the featured native modules item source method");
        require(provider.contains("ModCreativeTabs.nativeModuleCreativeNamespaces()"),
                "Ashfall product bridge hook must directly read the native modules namespace source method");
        require(!provider.contains("Class.forName(\n                    MOD_CREATIVE_TABS_CLASS")
                        && !provider.contains("getMethod(methodName)"),
                "Ashfall product bridge must not harvest ModCreativeTabs through reflection");
        require(provider.contains("\"nativeCreativeTabFullPopulationOwnerMember\"")
                        && provider.contains("\"nativeModuleCreativeItemIds\""),
                "Ashfall product bridge hook must name the full population owner member");
        require(provider.contains("\"nativeCreativeTabFeaturedOwnerMember\"")
                        && provider.contains("\"nativeModuleCreativeFeaturedItemIds\""),
                "Ashfall product bridge hook must name the featured item owner member");
        require(provider.contains("\"nativeCreativeTabNamespaceOwnerMember\"")
                        && provider.contains("\"nativeModuleCreativeNamespaces\""),
                "Ashfall product bridge hook must name the namespace owner member");
        require(provider.contains("\"nativeCreativeTabSourceResolvedFromRuntime\"")
                        && provider.contains("\"nativeCreativeTabFallbackPopulationUsed\"")
                        && provider.contains("\"nativeCreativeTabFallbackOnlyEvidence\"")
                        && provider.contains("\"releaseCreativeTabSourceTrusted\", source.sourceResolved()"),
                "Ashfall product bridge hook must distinguish runtime source resolution from fallback population");
        require(provider.contains("\"fallback_native_module_item_ids_pre_minecraft\""),
                "Ashfall product bridge hook must label fallback native modules population as pre-Minecraft fallback evidence");
        require(!provider.contains("\"echoashfallprotocol:module\""),
                "Ashfall product bridge must not emit descriptor-only echoashfallprotocol:module creative-tab evidence");
        require(registryBridge.contains("registryEvidence()")
                        && registryBridge.contains("registryMutationRecord(String registry, String namespace, String id)"),
                "Ashfall product registry bridge must expose aggregate and per-entry registry mutation proof");
        require(registryBridge.contains("mutatedRecordIds")
                        && registryBridge.contains("mutatedRecords")
                        && registryBridge.contains("productNativeRegistryTableMutated"),
                "Ashfall product registry bridge evidence must report mutated registry table records");
        require(registryBridge.contains("return EchoNativeLoadStatus.MUTATED"),
                "Ashfall product registry bridge must return MUTATED after first-class registry table mutation");
        require(!registryBridge.contains("return EchoNativeLoadStatus.REGISTERED"),
                "Ashfall product registry bridge must not treat accepted registry declarations as REGISTERED-only evidence");
        require(provider.contains("EchoNativeRegistryHost.firstClassRegistryKinds().contains(type)"),
                "Ashfall product registry bridge must support the authoritative Native Loader first-class registry kind list");
        require(provider.contains("normalizedRegistrySurface(String registry)")
                        && provider.contains("case \"creativegroup\", \"creativegroups\", \"creative_group\", \"creative_groups\"")
                        && provider.contains("\"creative_tab\", \"creative_tabs\" -> \"creative_tab\""),
                "Ashfall product registry bridge must canonicalize creative-tab registry aliases");
        require(provider.contains("\"world_generator\", \"world_generators\", \"worldgens\" -> \"worldgen\""),
                "Ashfall product registry bridge must canonicalize worldgen registry aliases");
        require(provider.contains("\"asset\", \"assets\", \"clientasset\", \"clientassets\", \"client_asset\", \"client_assets\" -> \"client_asset\""),
                "Ashfall product registry bridge must canonicalize client-asset registry aliases");
        require(provider.contains("\"datacomponent\", \"datacomponents\", \"data_component\", \"data_components\" -> \"data_component\""),
                "Ashfall product registry bridge must canonicalize data-component registry aliases");

        require(creativeTabs.contains("NATIVE_LOADER_REGISTRY_BACKED_TAB_ID")
                        && creativeTabs.contains("EchoAshfallProtocol.MODID + \":native_modules_tab\""),
                "Ashfall ModCreativeTabs must expose the Native Loader registry-backed tab id");
        require(creativeTabs.contains("nativeLoaderRegistryBackedCreativeItemIds()"),
                "Ashfall ModCreativeTabs must expose registry-backed native module item ids");
        require(creativeTabs.contains("nativeModuleCreativeItemIds()")
                        && creativeTabs.contains("nativeModuleCreativeFeaturedItemIds()")
                        && creativeTabs.contains("nativeModuleCreativeNamespaces()"),
                "Ashfall ModCreativeTabs must expose full population, featured item, and namespace sources");
        require(creativeTabs.contains("displayItems((parameters, output) -> acceptNativeModuleItems(output))")
                        && creativeTabs.contains("nativeLoaderRegistryBackedItems().stream()"),
                "Ashfall native modules tab display items must use the same registry-backed item source");
        require(creativeTabs.contains(".withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)"),
                "Ashfall native modules source tab must use the same ordering anchor as the Native Loader hook");
        require(creativeTabs.contains("BuiltInRegistries.ITEM.stream()")
                        && creativeTabs.contains(".filter(ModCreativeTabs::isNativeModuleRegistryItem)"),
                "Ashfall native modules tab must populate from the live BuiltInRegistries item table");
        require(creativeTabs.contains("ModItems.ITEMS.getEntries()")
                        && creativeTabs.contains("ModBlocks.BLOCK_ITEMS.getEntries()"),
                "Ashfall native modules tab must include module item and block-item registry data");
        require(creativeTabs.contains(".filter(itemId -> !itemId.isBlank())")
                        && !creativeTabs.contains("getDescriptionId()"),
                "Ashfall native modules tab source lists must fail closed to live registry item ids");
        require(nativeModule.contains("ModCreativeTabs.nativeModuleCreativeItemIds()")
                        && nativeModule.contains("ModCreativeTabs.nativeModuleCreativeFeaturedItemIds()")
                        && nativeModule.contains("ModCreativeTabs.nativeModuleCreativeNamespaces()"),
                "Ashfall native module descriptor must directly wire to ModCreativeTabs source methods");
        require(nativeModule.contains("\"orderAnchor\", \"minecraft:building_blocks\"")
                        && nativeModule.contains("\"orderStrategy\", \"with_tabs_before_anchor\"")
                        && nativeModule.contains("\"searchVisibility\", \"parent_and_search_tabs\"")
                        && nativeModule.contains("\"searchVisible\", true"),
                "Ashfall native module descriptor must preserve native modules tab ordering and search metadata");
        require(!nativeModule.contains("Class.forName(\"com.knoxhack.echoashfallprotocol.registry.ModCreativeTabs\")")
                        && !nativeModule.contains("getMethod(methodName)"),
                "Ashfall native module descriptor must not harvest ModCreativeTabs through reflection");

        System.out.println("agent3 ashfall creative tab wiring gate PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static String between(String value, String start, String end) {
        int startIndex = value.indexOf(start);
        if (startIndex < 0) {
            return "";
        }
        int endIndex = value.indexOf(end, startIndex + start.length());
        if (endIndex < 0) {
            return value.substring(startIndex);
        }
        return value.substring(startIndex, endIndex);
    }
}
