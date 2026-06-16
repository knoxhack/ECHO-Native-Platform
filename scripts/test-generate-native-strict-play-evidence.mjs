#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import assert from 'node:assert/strict'
import { generateNativeStrictPlayEvidence } from './generate-native-strict-play-evidence.mjs'

async function writeJson(root, relativePath, value) {
  const target = path.join(root, relativePath)
  await fs.mkdir(path.dirname(target), { recursive: true })
  await fs.writeFile(target, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function readJson(root, relativePath) {
  return JSON.parse(await fs.readFile(path.join(root, relativePath), 'utf8'))
}

const root = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-native-strict-play-'))
const modulesRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-modules-strict-play-'))

try {
  await writeJson(modulesRoot, 'reports/echo-native/core-module-integration-audit.json', {
    schema: 'echo.native.core_module_integration_audit.v1',
    modules: [
      { moduleId: 'echocore', nativeIntegrationStatus: 'LEGACY_ADAPTER_BRIDGEABLE' },
      { moduleId: 'echoindex', nativeIntegrationStatus: 'LEGACY_ADAPTER_BRIDGEABLE' },
    ],
  })
  await writeJson(root, 'build/native-all-bridgeable-module-artifact-load-state/native-all-bridgeable-module-artifact-load-state.json', {
    schema: 'echo.native.core_module_load_state_smoke.v1',
    failedModuleCount: 0,
    modules: [{ moduleId: 'echocore' }, { moduleId: 'echoindex' }],
  })
  await writeJson(root, 'build/agent5/ui-bridge-contract/agent5-ui-bridge-contract.json', {
    schema: 'echo.native.agent5.ui_bridge_contract_smoke.v1',
    status: 'PASS',
    moduleIds: ['echoindex', 'echohudcore'],
    trustedMutations: ['ui route accepted'],
    visibleRoutes: ['echoindex:index'],
    saveEvidence: ['settings persisted'],
    networkEvidence: ['ui sync ack'],
  })
  await writeJson(root, 'build/native-all-module-creative-tab-visibility/native-all-module-creative-tab-visibility.json', {
    schema: 'echo.native.all_module_creative_tab_visibility.v1',
    status: 'PASS',
    runtime: 'echo_native',
    allModules: true,
    moduleIds: ['echohudcore', 'echoindex', 'echolens'],
    registryBackedModuleIds: ['echohudcore', 'echoindex', 'echolens'],
    visibleParentModuleIds: ['echohudcore', 'echoindex', 'echolens'],
    visibleSearchModuleIds: ['echohudcore', 'echoindex', 'echolens'],
    selectableModuleIds: ['echohudcore', 'echoindex', 'echolens'],
    playableModuleIds: ['echohudcore', 'echoindex', 'echolens'],
    sourceReports: [
      {
        path: 'reports/echo-native/all-module-creative-tab-live-evidence.json',
        found: true,
        status: 'PASS',
        schema: 'echo.native.all_module_creative_tab_live_evidence.v1',
      },
    ],
    modules: [
      { moduleId: 'echohudcore', registryBacked: true, visibleParent: true, visibleSearch: true, selectable: true, playable: true },
      { moduleId: 'echoindex', registryBacked: true, visibleParent: true, visibleSearch: true, selectable: true, playable: true },
      { moduleId: 'echolens', registryBacked: true, visibleParent: true, visibleSearch: true, selectable: true, playable: true },
    ],
    blockers: [],
  })
  await writeJson(root, 'build/native-agent2-client-routes/native-client-route-ownership.json', {
    schema: 'echo.native.agent2.client_route_ownership.v1',
    exitGate: 'Native route dispatch opened Terminal, Index, Lens, HoloMap, HUD, menu, and loading actions without NeoForge event ownership.',
    neoForgeEventOwnershipRequired: false,
    requiredSurfaces: ['terminal', 'index', 'lens', 'holomap', 'hud', 'main_menu', 'world_setup', 'loading_screen'],
    unownedRouteStatus: 'UNSUPPORTED',
    unknownInputBindingStatus: 'UNSUPPORTED',
    hudOverlayLifecycleNativeOwned: true,
    sharedClientOverlayRouteOwned: true,
    dispatchResults: {
      'menu.new_run': true,
      'loading.render': true,
      'loading.progress': true,
      'loading.complete': true,
    },
    directPublicSdkDispatchResults: {
      'menu.open': true,
      'menu.new_run': true,
      'world_setup.open': true,
      'world_setup.create': true,
      'loading.open': true,
      'loading.render': true,
      'loading.progress': true,
      'loading.complete': true,
    },
    inputDispatchResults: {
      'menu.new_run': true,
    },
    builtInProductRoutes: {
      main_menu: { moduleId: 'echo-native-loader', surfaceId: 'echo-native-loader:main_menu' },
      loading_screen: { moduleId: 'echo-native-loader', surfaceId: 'echo-native-loader:loading_screen' },
    },
    builtInProductSurfaceState: {
      main_menu: { nativeProductUiReady: true },
      loading_screen: { nativeProductUiReady: true },
    },
    builtInProductRendererFrames: {
      main_menu: { status: 'MUTATED', nativeProductUiReady: true },
      loading_screen: { status: 'MUTATED', nativeProductUiReady: true },
    },
    actionDispatchEvidence: {
      dispatchCount: 2,
      events: [
        {
          routeModuleId: 'echolens',
          handledHandlerId: 'echolens:lens:scan',
          route: { moduleId: 'echolens', surfaceId: 'echolens:lens' },
        },
      ],
    },
    directPublicSdkDispatchGateResults: { terminal: true, index: true },
    directPublicSdkInputDispatchGateResults: { input: true },
    directPublicSdkLifecycleGateResults: { lifecycle: true },
    productionClientRouteRegistrationGateResults: { registered: true },
    routeTableOwnerHandlerGateResults: { handlers: true },
    terminalNativeRouteStateGateResults: { terminal: true },
    holoMapNativeRouteStateGateResults: { holomap: true },
    productRouteStateGateResults: { menu: true, loading: true },
    nativeWindowPumpGateResults: { frames: true },
    clientWindowPumpServiceGateResults: { services: true },
  })
  await writeJson(root, 'build/agent4/registry-content/native-agent4-registry-content-state.json', {
    schema: 'echo.native.agent4.registry_content_smoke_state.v1',
    status: 'SKIPPED',
    moduleIds: ['echocontentcore'],
    blockers: ['missing registry audit inputs'],
  })
  await writeJson(root, 'build/agent4/world-startup/native-agent4-world-startup.json', {
    schema: 'echo.native.agent4.world_startup_smoke.v1',
    status: 'PASS',
    moduleIds: ['echoworldcore', 'echoblockworks'],
    trustedMutations: ['fresh product world plan creates Ashfall world'],
    saveEvidence: ['product world open marker, staged datapack, level.dat, player, and level evidence verified'],
  })
  await writeJson(root, 'build/agent9/machine-runtime-host/agent9-machine-runtime-host.json', {
    schema: 'echo.native.agent9.machine_runtime_host_smoke.v1',
    status: 'PASS',
    moduleIds: ['echomachinecore'],
  })
  await writeJson(root, 'build/mutation-truth-gate/native-mutation-truth-gate.json', {
    schema: 'echo.native.mutation_truth_gate.v1',
    status: 'PASS',
    moduleIds: ['truth_gate_module'],
  })

  const { written } = await generateNativeStrictPlayEvidence({ root, modulesRoot })
  assert.equal(written.length, 5)

  const fullCatalog = await readJson(root, 'build/native-full-catalog-play/native-full-catalog-play.json')
  assert.equal(fullCatalog.status, 'PASS')
  assert.deepEqual(fullCatalog.requiredFor, ['lifecycle'])
  assert.equal(fullCatalog.allModules, true)
  assert.deepEqual(fullCatalog.moduleIds, ['echocore', 'echoindex'])
  assert.deepEqual(fullCatalog.missingExpectedModuleIds, [])

  const ui = await readJson(root, 'build/native-ui-surfaces/native-ui-surfaces.json')
  if (ui.status !== 'PASS') {
    console.dir({ blockers: ui.blockers, surfaceProofs: ui.surfaceProofs }, { depth: null })
  }
  assert.equal(ui.status, 'PASS')
  assert.deepEqual(ui.moduleIds, ['echoblockworks', 'echohudcore', 'echoindex', 'echolens', 'echoworldcore'])
  assert.ok(ui.visibleRoutes.includes('echolens:lens'))
  assert.deepEqual(ui.routeRegisteredSurfaces, ['creative_tab_catalog', 'main_menu', 'create_world', 'loading_screen'])
  assert.deepEqual(ui.liveHostReceiptSurfaces, ['main_menu', 'create_world', 'loading_screen'])
  assert.deepEqual(ui.actionableSurfaces, ['creative_tab_catalog', 'main_menu', 'create_world', 'loading_screen'])
  assert.equal(ui.surfaceProofs.find((proof) => proof.surface === 'creative_tab_catalog').proofMode, 'headless_native_creative_tab_bridge')

  const registry = await readJson(root, 'build/native-registry-content/native-registry-content.json')
  assert.equal(registry.status, 'PARTIAL')
  assert.ok(registry.blockers.some((blocker) => blocker.includes('Agent 4 registry content')))
  assert.deepEqual(registry.moduleIds, ['echoblockworks', 'echomachinecore', 'echoworldcore'])

  const actions = await readJson(root, 'build/native-block-actions/native-block-actions.json')
  assert.equal(actions.status, 'PASS')
  assert.deepEqual(actions.moduleIds, ['echomachinecore', 'truth_gate_module'])

  const saveNetwork = await readJson(root, 'build/native-save-network/native-save-network.json')
  assert.equal(saveNetwork.status, 'PASS')
  assert.deepEqual(saveNetwork.moduleIds, ['echoblockworks', 'echohudcore', 'echoindex', 'echomachinecore', 'echoworldcore', 'truth_gate_module'])

  await writeJson(modulesRoot, 'reports/echo-native/core-module-integration-audit.json', {
    schema: 'echo.native.core_module_integration_audit.v1',
    modules: [
      { moduleId: 'echocore', nativeIntegrationStatus: 'LEGACY_ADAPTER_BRIDGEABLE' },
      { moduleId: 'echoindex', nativeIntegrationStatus: 'LEGACY_ADAPTER_BRIDGEABLE' },
      { moduleId: 'echodeepreachprotocol', nativeIntegrationStatus: 'LEGACY_ADAPTER_BRIDGEABLE' },
    ],
  })
  await generateNativeStrictPlayEvidence({ root, modulesRoot })
  const staleFullCatalog = await readJson(root, 'build/native-full-catalog-play/native-full-catalog-play.json')
  assert.equal(staleFullCatalog.status, 'FAIL')
  assert.equal(staleFullCatalog.allModules, false)
  assert.deepEqual(staleFullCatalog.missingExpectedModuleIds, ['echodeepreachprotocol'])
  assert.ok(staleFullCatalog.blockers.some((blocker) => blocker.includes('echodeepreachprotocol')))
} finally {
  await fs.rm(root, { recursive: true, force: true })
  await fs.rm(modulesRoot, { recursive: true, force: true })
}

console.log('generate-native-strict-play-evidence tests passed')
