package dev.echo.nativeplatform.testkit;

import dev.echo.nativeplatform.contracts.EchoNativeCapabilityNegotiation;
import dev.echo.nativeplatform.contracts.EchoNativeDependencyGraphDiagnostics;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import dev.echo.nativeplatform.contracts.EchoNativeModuleHealthTelemetry;
import dev.echo.nativeplatform.contracts.EchoNativeMutationReceipt;
import dev.echo.nativeplatform.contracts.EchoNativeParityReport;
import dev.echo.nativeplatform.contracts.EchoNativeRegistryService;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeLane;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceMutation;

import java.util.List;
import java.util.Map;

public final class EchoNativeSdkTestkitSmoke {
    private EchoNativeSdkTestkitSmoke() {
    }

    public static void main(String[] args) {
        EchoNativeSdkTestkit.Environment env = EchoNativeSdkTestkit.client("sdk_test_addon");

        env.registry().register(env.mutation("blocks", "register", "sdk_test_addon:glowstone_lamp",
                Map.of("blockstate", "sdk_test_addon:blockstates/glowstone_lamp",
                        "model", "sdk_test_addon:models/block/glowstone_lamp",
                        "texture", "sdk_test_addon:textures/block/glowstone_lamp",
                        "lang", "Glowstone Lamp")));
        env.registry().deferredRegister(env.mutation("items", "deferredRegister", "sdk_test_addon:lamp_wand"));
        env.registry().registerDataComponent(env.mutation("data_components", "registerDataComponent", "sdk_test_addon:charge"));
        env.registry().registerBlockEntity(env.mutation("block_entities", "registerBlockEntity", "sdk_test_addon:lamp_entity"));
        env.registry().registerCreativeTab(env.mutation("creative_tabs", "registerCreativeTab", "sdk_test_addon:tools"));
        env.registry().registerLootModifier(env.mutation("loot_modifiers", "registerLootModifier", "sdk_test_addon:lamp_bonus"));
        env.registry().registerRecipe(env.mutation("recipes", "registerRecipe", "sdk_test_addon:lamp_recipe",
                Map.of("inputs", List.of("minecraft:glowstone"), "outputs", List.of("sdk_test_addon:glowstone_lamp"))));
        env.registry().registerTag(env.mutation("tags", "registerTag", "sdk_test_addon:mineable/wrench"));
        env.events().subscribe(env.mutation("events", "subscribe", "client_tick"));
        env.events().publish(env.mutation("events", "publish", "client_tick"));
        env.commands().register(env.mutation("commands", "register", "sdktest"));
        env.config().register(env.mutation("configs", "register", "client"));
        env.config().write(env.mutation("configs", "write", "client", Map.of("enabled", true)));
        env.network().registerPacket(env.mutation("network", "registerPacket", "sdk_test_addon:sync_lamp"));
        env.resources().registerReloadListener(env.mutation("resources", "registerReloadListener", "sdk_test_addon:client_assets"));
        env.resources().reload(env.mutation("resources", "reload", "client_assets"));
        env.resources().runDatagen(env.mutation("datagen", "runDatagen", "sdk_test_addon:generated_assets"));
        env.resources().hotReload(env.mutation("resources", "hotReload", "client_assets"));
        env.capabilities().register(env.mutation("capabilities", "register", "sdk_test_addon:charge"));
        env.capabilities().mutate(env.mutation("capabilities", "mutate", "sdk_test_addon:charge", Map.of("value", 3)));
        env.capabilities().registerIntegration(env.mutation("integrations", "registerIntegration", "sdk_test_addon:echoindex"));
        EchoNativeCapabilityNegotiation negotiation = env.capabilities().negotiate(env.mutation(
                "capabilities",
                "negotiate",
                "sdk_test_addon:charge",
                Map.of("requestedVersion", "1", "selectedVersion", "1", "supported", true)));
        if (!negotiation.supported() || !"1".equals(negotiation.selectedVersion())) {
            throw new IllegalStateException("Expected capability negotiation to select version 1, got " + negotiation);
        }
        env.attachments().attach(env.mutation("attachments", "attach", "player:lamp_state"));
        env.worldgen().registerFeature(env.mutation("worldgen", "registerFeature", "sdk_test_addon:lamp_geode"));
        env.worldgen().placeStructure(env.mutation("worldgen", "placeStructure", "sdk_test_addon:lamp_ruin"));
        env.render().registerLayer(env.mutation("render", "registerLayer", "sdk_test_addon:lamp_glow"));
        env.render().registerRenderHook(env.mutation("render_hooks", "registerRenderHook", "sdk_test_addon:lamp_render_tick"));
        env.render().registerHudOverlay(env.mutation("hud_overlays", "registerHudOverlay", "sdk_test_addon:lamp_meter"));
        env.screens().registerSurface(env.mutation("screens", "registerSurface", "sdk_test_addon:lamp_menu"));
        env.screens().registerMenu(env.mutation("menus", "registerMenu", "sdk_test_addon:lamp_menu"));
        env.screens().registerKeybind(env.mutation("keybinds", "registerKeybind", "key.sdk_test_addon.lamp"));
        env.saveData().write(env.mutation("save_data", "write", "lamp_state", Map.of("lit", true)));
        env.lifecycle().phase(env.mutation("lifecycle", "phase", "client_started"));
        env.lifecycle().registerGameTest(env.mutation("game_tests", "registerGameTest", "sdk_test_addon:lamp_smoke"));
        env.lifecycle().runGameTest(env.mutation("game_tests", "runGameTest", "sdk_test_addon:lamp_smoke"));

        EchoNativeMutationReceipt duplicate = env.network().registerPacket(
                env.mutation("network", "registerPacket", "sdk_test_addon:sync_lamp"));
        if (duplicate.status() != EchoNativeLoadStatus.FAILED) {
            throw new IllegalStateException("Expected duplicate packet registration to fail, got " + duplicate);
        }

        EchoNativeServiceMutation serverOnlyRender = EchoNativeSdkTestkit.mutation(
                env.moduleId(),
                "render",
                "registerLayer",
                "sdk_test_addon:server_only_layer",
                EchoNativeRuntimeSide.SERVER
        );
        EchoNativeMutationReceipt wrongSide = env.render().registerLayer(serverOnlyRender);
        if (wrongSide.status() != EchoNativeLoadStatus.FAILED) {
            throw new IllegalStateException("Expected client test host to reject server render mutation, got " + wrongSide);
        }

        env.goldenParity().requireOnlyTypedReceipts();
        env.goldenParity().requireMutatedServices(
                "echo.native.registry",
                "echo.native.events",
                "echo.native.commands",
                "echo.native.config",
                "echo.native.network",
                "echo.native.resources",
                "echo.native.capabilities",
                "echo.native.attachments",
                "echo.native.worldgen",
                "echo.native.render",
                "echo.native.screens",
                "echo.native.save_data",
                "echo.native.lifecycle"
        );
        env.goldenParity().requireMutatedSurfaces(
                "blocks",
                "items",
                "data_components",
                "block_entities",
                "creative_tabs",
                "loot_modifiers",
                "recipes",
                "tags",
                "events",
                "commands",
                "configs",
                "network",
                "resources",
                "datagen",
                "capabilities",
                "integrations",
                "attachments",
                "worldgen",
                "render",
                "render_hooks",
                "hud_overlays",
                "screens",
                "menus",
                "keybinds",
                "save_data",
                "lifecycle",
                "game_tests"
        );

        if (env.registry().snapshot(env.moduleId()).definitions().size() != 7) {
            throw new IllegalStateException("Expected seven immediate registry definitions in fake registry snapshot");
        }
        if (!Boolean.TRUE.equals(env.saveData().read(env.mutation("save_data", "read", "lamp_state")).get("lit"))) {
            throw new IllegalStateException("Expected fake save-data host to retain written value");
        }
        if (!env.serviceRegistry().service("echo.native.registry", EchoNativeRegistryService.class).isPresent()) {
            throw new IllegalStateException("Expected typed registry service lookup to succeed");
        }
        if (!env.moduleFixture("dev.echo.test.SdkAddonEntrypoint").moduleDescriptor().hasEntrypoint()) {
            throw new IllegalStateException("Expected generated module fixture to have an entrypoint");
        }

        if (env.lifecycle().runtimeLane(env.mutation("lifecycle", "runtimeLane", "lane")) != EchoNativeRuntimeLane.STANDALONE) {
            throw new IllegalStateException("Expected SDK testkit runtime lane to be standalone");
        }
        EchoNativeParityReport typedReport = env.lifecycle().parityReport(env.mutation(
                "parity",
                "parityReport",
                "sdk",
                Map.of("requiredSurfaces", List.of("blocks", "network", "render", "save_data"))));
        if (!typedReport.passed()) {
            throw new IllegalStateException("Expected typed lifecycle parity report to pass: " + typedReport);
        }
        EchoNativeModuleHealthTelemetry telemetry = env.lifecycle().healthTelemetry(
                env.mutation("telemetry", "healthTelemetry", env.moduleId()));
        if (telemetry.mutatedReceiptCount() == 0 || telemetry.receiptCount() == 0) {
            throw new IllegalStateException("Expected health telemetry to report mutation receipts: " + telemetry);
        }
        EchoNativeDependencyGraphDiagnostics graph = env.lifecycle().dependencyGraph(env.mutation(
                "dependency_graph",
                "dependencyGraph",
                env.moduleId(),
                Map.of("resolvedOrder", List.of("echo-native-contracts", env.moduleId()))));
        if (!graph.deterministic() || !graph.resolvedOrder().contains(env.moduleId())) {
            throw new IllegalStateException("Expected deterministic dependency graph containing addon: " + graph);
        }

        Map<String, Object> report = env.goldenParity().parityReport("blocks", "network", "render", "save_data");
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new IllegalStateException("Expected golden parity report to pass: " + report);
        }

        long failedCount = env.receipts().stream()
                .filter(receipt -> receipt.status() == EchoNativeLoadStatus.FAILED)
                .count();
        if (failedCount != 2) {
            throw new IllegalStateException("Expected two intentional failure receipts, got " + failedCount
                    + " from " + List.copyOf(env.receipts()));
        }
    }
}
