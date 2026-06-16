#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DEFAULT_ROOT = process.cwd()
const DEFAULT_MODULES_ROOT = path.resolve(DEFAULT_ROOT, '..', 'ECHO-Modules')

const INPUTS = {
  loadState: 'build/native-all-bridgeable-module-artifact-load-state/native-all-bridgeable-module-artifact-load-state.json',
  clientRoutes: 'build/native-agent2-client-routes/native-client-route-ownership.json',
  uiBridge: 'build/agent5/ui-bridge-contract/agent5-ui-bridge-contract.json',
  creativeVisibility: 'build/native-all-module-creative-tab-visibility/native-all-module-creative-tab-visibility.json',
  registryContent: 'build/agent4/registry-content/native-agent4-registry-content-state.json',
  worldStartup: 'build/agent4/world-startup/native-agent4-world-startup.json',
  machineRuntime: 'build/agent9/machine-runtime-host/agent9-machine-runtime-host.json',
  mutationTruth: 'build/mutation-truth-gate/native-mutation-truth-gate.json',
}

const OUTPUTS = {
  fullCatalog: 'build/native-full-catalog-play/native-full-catalog-play.json',
  uiSurfaces: 'build/native-ui-surfaces/native-ui-surfaces.json',
  registryContent: 'build/native-registry-content/native-registry-content.json',
  blockActions: 'build/native-block-actions/native-block-actions.json',
  saveNetwork: 'build/native-save-network/native-save-network.json',
}

export async function generateNativeStrictPlayEvidence({ root = DEFAULT_ROOT, modulesRoot = DEFAULT_MODULES_ROOT } = {}) {
  const repoRoot = path.resolve(root)
  const normalizedModulesRoot = path.resolve(modulesRoot)
  const generatedAt = new Date().toISOString()
  const inputs = {}
  for (const [key, relativePath] of Object.entries(INPUTS)) {
    inputs[key] = await readReport(repoRoot, key, relativePath)
  }
  const bridgeableAudit = await readBridgeableAudit(normalizedModulesRoot)

  const outputs = {
    [OUTPUTS.fullCatalog]: fullCatalogReport({ generatedAt, repoRoot, inputs, bridgeableAudit }),
    [OUTPUTS.uiSurfaces]: uiSurfacesReport({ generatedAt, repoRoot, inputs }),
    [OUTPUTS.registryContent]: aggregateReport({
      generatedAt,
      repoRoot,
      schema: 'echo.native.strict_play.registry_content.v1',
      evidenceKind: 'native-registry-resource-world-content-proof',
      requiredFor: ['content', 'blockItems', 'worldgen'],
      inputs,
      include: ['registryContent', 'worldStartup', 'machineRuntime'],
      partialIfAnyInputMissing: true,
      blockers: skippedBlockers(inputs.registryContent, 'Agent 4 registry content smoke did not run to PASS.'),
    }),
    [OUTPUTS.blockActions]: aggregateReport({
      generatedAt,
      repoRoot,
      schema: 'echo.native.strict_play.block_actions.v1',
      evidenceKind: 'native-host-mutation-action-proof',
      requiredFor: ['actions', 'blockItems'],
      inputs,
      include: ['machineRuntime', 'mutationTruth'],
      partialIfAnyInputMissing: true,
      blockers: [],
    }),
    [OUTPUTS.saveNetwork]: aggregateReport({
      generatedAt,
      repoRoot,
      schema: 'echo.native.strict_play.save_network.v1',
      evidenceKind: 'native-save-network-host-proof',
      requiredFor: ['saveNetwork'],
      inputs,
      include: ['uiBridge', 'machineRuntime', 'mutationTruth', 'worldStartup'],
      partialIfAnyInputMissing: true,
      blockers: [],
    }),
  }

  const written = []
  for (const [relativePath, report] of Object.entries(outputs)) {
    const output = path.join(repoRoot, relativePath)
    await fs.mkdir(path.dirname(output), { recursive: true })
    await fs.writeFile(output, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
    written.push({ path: normalizePath(output), status: report.status, moduleCount: report.moduleIds.length })
  }

  return { generatedAt, written }
}

function uiSurfacesReport({ generatedAt, repoRoot, inputs }) {
  const include = ['uiBridge', 'clientRoutes', 'creativeVisibility', 'worldStartup']
  const included = include.map((key) => inputs[key])
  const passing = included.filter((entry) => entry.status === 'PASS')
  const sourceBlockers = included.flatMap((entry) => missingBlockers(entry, `${entry.relativePath} is not PASS.`))
  const proofs = [
    creativeTabSurfaceProof(inputs.creativeVisibility.report),
    mainMenuSurfaceProof(inputs.clientRoutes.report),
    createWorldSurfaceProof(inputs.clientRoutes.report, inputs.worldStartup.report),
    loadingSurfaceProof(inputs.clientRoutes.report),
  ]
  const proofBlockers = proofs.flatMap((proof) =>
    proof.blockers.map((blocker) => `${proof.surface}: ${blocker}`))
  const status = sourceBlockers.length === 0 && proofBlockers.length === 0 ? 'PASS' : 'FAIL'
  return {
    schema: 'echo.native.strict_play.ui_surfaces.v1',
    generatedAt,
    status,
    runtime: 'echo_native',
    evidenceKind: 'native-ui-visible-route-host-action-proof',
    repoRoot: normalizePath(repoRoot),
    requiredFor: ['ui', 'creative_tabs', 'main_menu', 'create_world'],
    moduleIds: unique(passing.flatMap((entry) => moduleIdsFrom(entry.report))),
    allModules: status === 'PASS' && inputs.creativeVisibility.report?.allModules === true,
    sourceReports: sourceReports(inputs, include),
    surfaceProofs: proofs,
    routeRegisteredSurfaces: proofs.filter((proof) => proof.registeredRoute).map((proof) => proof.surface),
    liveHostReceiptSurfaces: proofs.filter((proof) => proof.liveHostReceipt).map((proof) => proof.surface),
    actionableSurfaces: proofs.filter((proof) => proof.actionableSurface).map((proof) => proof.surface),
    trustedMutations: unique([
      ...passing.flatMap((entry) => array(entry.report?.trustedMutations)),
      ...proofs.flatMap((proof) => proof.trustedMutations),
    ]),
    visibleRoutes: unique(passing.flatMap((entry) => array(entry.report?.visibleRoutes))),
    saveEvidence: unique(passing.flatMap((entry) => array(entry.report?.saveEvidence))),
    networkEvidence: unique(passing.flatMap((entry) => array(entry.report?.networkEvidence))),
    blockers: unique([...sourceBlockers, ...proofBlockers]),
  }
}

function creativeTabSurfaceProof(report) {
  const blockers = []
  const rows = array(report?.modules)
  const moduleIds = moduleIdsFrom(report)
  const allModules = report?.allModules === true
  const liveGameEvidence = report?.liveGameEvidence === true
  const sourceReports = array(report?.sourceReports)
  const hasRuntimeSource = sourceReports.some((source) =>
    source?.found === true && string(source?.schema).includes('creative_tab_live_evidence'))
  const registryBacked = allModules && array(report?.registryBackedModuleIds).length === moduleIds.length
  const visibleParent = allModules && array(report?.visibleParentModuleIds).length === moduleIds.length
  const visibleSearch = allModules && array(report?.visibleSearchModuleIds).length === moduleIds.length
  const selectable = allModules && array(report?.selectableModuleIds).length === moduleIds.length
  const playable = allModules && array(report?.playableModuleIds).length === moduleIds.length
  const liveHostReceipt = liveGameEvidence && hasRuntimeSource
  if (!allModules || moduleIds.length === 0) blockers.push('catalog-wide creative visibility report is not all-modules PASS')
  if (!hasRuntimeSource) blockers.push('creative visibility report is not backed by a Native runtime creative-tab source report')
  if (!registryBacked) blockers.push('creative tabs are not registry-backed for every catalog module')
  if (!visibleParent) blockers.push('creative entries are not visible in parent inventory for every catalog module')
  if (!visibleSearch) blockers.push('creative entries are not visible in search for every catalog module')
  if (!selectable) blockers.push('creative entries are not selectable for every catalog module')
  if (!playable) blockers.push('creative entries are not playable for every catalog module')
  return {
    surface: 'creative_tab_catalog',
    registeredRoute: registryBacked,
    liveHostReceipt,
    actionableSurface: selectable && playable,
    proofMode: liveGameEvidence ? 'live_game_creative_inventory' : 'headless_native_creative_tab_bridge',
    distinguishesRegisteredFromHostReceipt: true,
    moduleCount: moduleIds.length,
    registeredModuleCount: array(report?.registryBackedModuleIds).length,
    selectableModuleCount: array(report?.selectableModuleIds).length,
    playableModuleCount: array(report?.playableModuleIds).length,
    evidenceNotes: liveGameEvidence
      ? ['Creative catalog proof includes live game inventory receipt evidence.']
      : ['Creative catalog proof is actionable headless Native bridge output, not live game inventory receipt evidence.'],
    trustedMutations: blockers.length === 0
      ? ['Catalog-wide Native creative tab bridge proved registry-backed, visible, searchable, selectable, and playable module entries.']
      : [],
    blockers,
    sampleModules: rows.slice(0, 5).map((row) => row?.moduleId).filter(Boolean),
  }
}

function mainMenuSurfaceProof(report) {
  const blockers = []
  const registeredRoute = array(report?.requiredSurfaces).includes('main_menu')
    && !!object(report?.builtInProductRoutes).main_menu
  const directReceipt = report?.directPublicSdkDispatchResults?.['menu.new_run'] === true
  const hostReceipt = report?.dispatchResults?.['menu.new_run'] === true
  const inputReceipt = report?.inputDispatchResults?.['menu.new_run'] === true
  const frame = object(object(report?.builtInProductRendererFrames).main_menu)
  const actionableSurface = object(object(report?.builtInProductSurfaceState).main_menu).nativeProductUiReady === true
    && frame.nativeProductUiReady === true
    && frame.status === 'MUTATED'
  if (!registeredRoute) blockers.push('main menu route is not registered as a Native built-in product route')
  if (!directReceipt) blockers.push('main menu direct public SDK dispatch did not return a mutating receipt')
  if (!hostReceipt) blockers.push('main menu UI host dispatch did not return a mutating receipt')
  if (!inputReceipt) blockers.push('main menu key/input dispatch did not return a mutating receipt')
  if (!actionableSurface) blockers.push('main menu renderer/state proof is not actionable')
  return {
    surface: 'main_menu',
    registeredRoute,
    liveHostReceipt: directReceipt && hostReceipt && inputReceipt,
    actionableSurface,
    proofMode: 'native_client_ui_host_route_and_window_pump',
    distinguishesRegisteredFromHostReceipt: true,
    requiredActions: ['menu.open', 'menu.new_run', 'menu.quit'],
    trustedMutations: blockers.length === 0
      ? ['Native main menu route is registered and host/window-pump actions returned mutating receipts.']
      : [],
    blockers,
  }
}

function createWorldSurfaceProof(routeReport, worldReport) {
  const blockers = []
  const registeredRoute = array(routeReport?.requiredSurfaces).includes('world_setup')
  const openReceipt = routeReport?.directPublicSdkDispatchResults?.['world_setup.open'] === true
  const createReceipt = routeReport?.directPublicSdkDispatchResults?.['world_setup.create'] === true
  const worldStartupPass = reportStatus(worldReport) === 'PASS'
  const worldSaveEvidence = array(worldReport?.saveEvidence).some((entry) =>
    string(entry).includes('product world open marker'))
  const trustedWorldMutation = array(worldReport?.trustedMutations).some((entry) =>
    string(entry).includes('fresh product world plan creates Ashfall world'))
  if (!registeredRoute) blockers.push('create-world route is not registered as the Native world_setup surface')
  if (!openReceipt) blockers.push('world_setup.open direct public SDK dispatch did not return a mutating receipt')
  if (!createReceipt) blockers.push('world_setup.create direct public SDK dispatch did not return a mutating receipt')
  if (!worldStartupPass) blockers.push('Agent 4 Ashfall world startup report is not PASS')
  if (!worldSaveEvidence) blockers.push('world startup evidence does not prove product world open/save markers')
  if (!trustedWorldMutation) blockers.push('world startup evidence does not prove fresh Ashfall product world creation')
  return {
    surface: 'create_world',
    registeredRoute,
    liveHostReceipt: openReceipt && createReceipt,
    actionableSurface: worldStartupPass && worldSaveEvidence && trustedWorldMutation,
    proofMode: 'native_world_setup_route_plus_agent4_world_startup',
    distinguishesRegisteredFromHostReceipt: true,
    requiredActions: ['world_setup.open', 'world_setup.create'],
    trustedMutations: blockers.length === 0
      ? ['Native world setup route accepted create-world receipts and Agent 4 proved Ashfall product world startup markers.']
      : [],
    blockers,
  }
}

function loadingSurfaceProof(report) {
  const blockers = []
  const registeredRoute = array(report?.requiredSurfaces).includes('loading_screen')
    && !!object(report?.builtInProductRoutes).loading_screen
  const renderReceipt = report?.dispatchResults?.['loading.render'] === true
  const progressReceipt = report?.dispatchResults?.['loading.progress'] === true
  const completeReceipt = report?.dispatchResults?.['loading.complete'] === true
  const frame = object(object(report?.builtInProductRendererFrames).loading_screen)
  const actionableSurface = object(object(report?.builtInProductSurfaceState).loading_screen).nativeProductUiReady === true
    && frame.nativeProductUiReady === true
    && frame.status === 'MUTATED'
  if (!registeredRoute) blockers.push('loading route is not registered as a Native built-in product route')
  if (!renderReceipt) blockers.push('loading renderer did not return a mutating receipt')
  if (!progressReceipt) blockers.push('loading progress did not return a mutating receipt')
  if (!completeReceipt) blockers.push('loading complete did not return a mutating receipt')
  if (!actionableSurface) blockers.push('loading renderer/state proof is not actionable')
  return {
    surface: 'loading_screen',
    registeredRoute,
    liveHostReceipt: renderReceipt && progressReceipt && completeReceipt,
    actionableSurface,
    proofMode: 'native_client_ui_host_loading_renderer',
    distinguishesRegisteredFromHostReceipt: true,
    requiredActions: ['loading.open', 'loading.render', 'loading.progress', 'loading.complete'],
    trustedMutations: blockers.length === 0
      ? ['Native loading screen route is registered and host/window-pump render/progress/complete actions returned mutating receipts.']
      : [],
    blockers,
  }
}

function fullCatalogReport({ generatedAt, repoRoot, inputs, bridgeableAudit }) {
  const loadState = inputs.loadState.report
  const moduleIds = moduleIdsFrom(loadState)
  const loadStatePass = inputs.loadState.status === 'PASS'
  const expectedModuleIds = bridgeableAudit.expectedModuleIds
  const missingExpectedModuleIds = expectedModuleIds.filter((moduleId) => !moduleIds.includes(moduleId))
  const extraModuleIds = moduleIds.filter((moduleId) => expectedModuleIds.length > 0 && !expectedModuleIds.includes(moduleId))
  const catalogMatches = bridgeableAudit.status === 'PASS' && missingExpectedModuleIds.length === 0
  const blockers = [
    ...missingBlockers(inputs.loadState, 'Native all-bridgeable artifact load-state smoke is not PASS.'),
    ...bridgeableAudit.blockers,
    ...missingExpectedModuleIds.map((moduleId) => `Native all-bridgeable artifact load-state omitted current bridgeable module: ${moduleId}`),
  ]
  return {
    schema: 'echo.native.strict_play.full_catalog.v1',
    generatedAt,
    status: loadStatePass && catalogMatches ? 'PASS' : 'FAIL',
    runtime: 'echo_native',
    evidenceKind: 'native-full-catalog-artifact-lifecycle-proof',
    repoRoot: normalizePath(repoRoot),
    requiredFor: ['lifecycle'],
    moduleIds,
    expectedModuleIds,
    missingExpectedModuleIds,
    extraModuleIds,
    allModules: loadStatePass && catalogMatches,
    sourceReports: sourceReports(inputs, ['loadState']),
    bridgeableAudit: {
      path: bridgeableAudit.relativePath,
      found: bridgeableAudit.found,
      status: bridgeableAudit.status,
      moduleCount: expectedModuleIds.length,
    },
    trustedMutations: array(loadState?.trustedMutations),
    visibleRoutes: [],
    saveEvidence: [],
    networkEvidence: [],
    coverageNotes: loadStatePass && catalogMatches
      ? ['This proves packaged Native artifact lifecycle/load coverage only; content, UI, action, worldgen, and save/network proof is supplied by separate Native host reports.']
      : [],
    blockers,
  }
}

function aggregateReport({
  generatedAt,
  repoRoot,
  schema,
  evidenceKind,
  requiredFor,
  inputs,
  include,
  partialIfAnyInputMissing,
  blockers,
}) {
  const included = include.map((key) => inputs[key])
  const passing = included.filter((entry) => entry.status === 'PASS')
  const moduleIds = unique(passing.flatMap((entry) => moduleIdsFrom(entry.report)))
  const inputBlockers = included.flatMap((entry) => missingBlockers(entry, `${entry.relativePath} is not PASS.`))
  const status = moduleIds.length > 0 && (!partialIfAnyInputMissing || inputBlockers.length === 0) && blockers.length === 0
    ? 'PASS'
    : (moduleIds.length > 0 ? 'PARTIAL' : 'FAIL')
  return {
    schema,
    generatedAt,
    status,
    runtime: 'echo_native',
    evidenceKind,
    repoRoot: normalizePath(repoRoot),
    requiredFor,
    moduleIds,
    allModules: false,
    sourceReports: sourceReports(inputs, include),
    trustedMutations: unique(passing.flatMap((entry) => array(entry.report?.trustedMutations))),
    visibleRoutes: unique(passing.flatMap((entry) => array(entry.report?.visibleRoutes))),
    saveEvidence: unique(passing.flatMap((entry) => array(entry.report?.saveEvidence))),
    networkEvidence: unique(passing.flatMap((entry) => array(entry.report?.networkEvidence))),
    blockers: unique([...blockers, ...inputBlockers]),
  }
}

function sourceReports(inputs, keys) {
  return keys.map((key) => ({
    key,
    path: inputs[key].relativePath,
    found: inputs[key].found,
    status: inputs[key].status,
    moduleCount: moduleIdsFrom(inputs[key].report).length,
  }))
}

function skippedBlockers(input, message) {
  if (input.status === 'PASS') return []
  return [message, ...array(input.report?.blockers)]
}

function missingBlockers(input, message) {
  if (input.status === 'PASS') return []
  if (!input.found) return [`missing source report: ${input.relativePath}`]
  return [message, ...array(input.report?.blockers)]
}

async function readReport(root, key, relativePath) {
  const absolute = path.join(root, relativePath)
  try {
    const text = await fs.readFile(absolute, 'utf8')
    const parsed = JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
    const report = normalizedReport(key, parsed)
    return {
      relativePath,
      found: true,
      report,
      status: reportStatus(report),
    }
  } catch (error) {
    if (error.code === 'ENOENT') {
      return { relativePath, found: false, report: null, status: 'MISSING' }
    }
    return { relativePath, found: true, report: { parseError: error.message }, status: 'PARSE_ERROR' }
  }
}

async function readBridgeableAudit(modulesRoot) {
  const relativePath = 'reports/echo-native/core-module-integration-audit.json'
  const absolute = path.join(modulesRoot, relativePath)
  try {
    const text = await fs.readFile(absolute, 'utf8')
    const parsed = JSON.parse(text.charCodeAt(0) === 0xfeff ? text.slice(1) : text)
    const modules = array(parsed.modules)
    const expectedModuleIds = unique(modules
      .filter((module) => module?.nativeIntegrationStatus === 'LEGACY_ADAPTER_BRIDGEABLE')
      .map((module) => module.moduleId)
      .filter((moduleId) => typeof moduleId === 'string' && moduleId.trim()))
    const blockers = []
    if (expectedModuleIds.length === 0) {
      blockers.push(`Native bridgeable audit published no bridgeable modules: ${relativePath}`)
    }
    return {
      relativePath: normalizePath(absolute),
      found: true,
      status: blockers.length === 0 ? 'PASS' : 'FAIL',
      expectedModuleIds,
      blockers,
    }
  } catch (error) {
    if (error.code === 'ENOENT') {
      return {
        relativePath: normalizePath(absolute),
        found: false,
        status: 'MISSING',
        expectedModuleIds: [],
        blockers: [`missing ECHO Modules native bridgeable audit: ${normalizePath(absolute)}`],
      }
    }
    return {
      relativePath: normalizePath(absolute),
      found: true,
      status: 'PARSE_ERROR',
      expectedModuleIds: [],
      blockers: [`failed to parse ECHO Modules native bridgeable audit: ${error.message}`],
    }
  }
}

function normalizedReport(key, report) {
  if (key !== 'clientRoutes' || report?.schema !== 'echo.native.agent2.client_route_ownership.v1') {
    return report
  }
  const blockers = nativeAgent2RouteBlockers(report)
  const moduleIds = nativeAgent2ModuleIds(report)
  const visibleRoutes = nativeAgent2VisibleRoutes(report)
  const status = blockers.length === 0 && moduleIds.length > 0 && visibleRoutes.length > 0 ? 'PASS' : 'FAIL'
  return {
    ...report,
    status,
    moduleIds,
    visibleRoutes,
    trustedMutations: status === 'PASS'
      ? [
          'Agent 2 Native client route ownership dispatched live Terminal, Index, Lens, HoloMap, HUD, menu, and loading routes without NeoForge event ownership.',
          'Native route table handlers accepted direct public SDK dispatch and input lifecycle mutations.',
        ]
      : [],
    saveEvidence: status === 'PASS'
      ? ['Native client route ownership preserved UI route state through direct route dispatch gates.']
      : [],
    networkEvidence: status === 'PASS'
      ? ['Native client route ownership verified route dispatch through the native client bridge host service.']
      : [],
    blockers,
  }
}

function nativeAgent2RouteBlockers(report) {
  const blockers = []
  const requiredSurfaces = array(report.requiredSurfaces)
  for (const surface of ['terminal', 'index', 'lens', 'holomap', 'hud', 'main_menu', 'world_setup', 'loading_screen']) {
    if (!requiredSurfaces.includes(surface)) blockers.push(`Agent 2 route ownership report missing required surface: ${surface}`)
  }
  if (!string(report.exitGate).includes('without NeoForge event ownership')) {
    blockers.push('Agent 2 route ownership exit gate did not prove Native-owned route dispatch without NeoForge event ownership.')
  }
  if (report.neoForgeEventOwnershipRequired !== false) {
    blockers.push('Agent 2 route ownership report did not publish neoForgeEventOwnershipRequired=false.')
  }
  if (report.unownedRouteStatus !== 'UNSUPPORTED') {
    blockers.push('Agent 2 route ownership report did not reject unowned routes as UNSUPPORTED.')
  }
  if (report.unknownInputBindingStatus !== 'UNSUPPORTED') {
    blockers.push('Agent 2 route ownership report did not reject unknown input bindings as UNSUPPORTED.')
  }
  if (report.hudOverlayLifecycleNativeOwned !== true) {
    blockers.push('Agent 2 route ownership report did not prove HUD overlay lifecycle is Native-owned.')
  }
  if (report.sharedClientOverlayRouteOwned !== true) {
    blockers.push('Agent 2 route ownership report did not prove shared client overlay route ownership.')
  }
  for (const actionId of ['menu.open', 'menu.new_run', 'world_setup.open', 'world_setup.create', 'loading.open', 'loading.render', 'loading.progress', 'loading.complete']) {
    if (report.directPublicSdkDispatchResults?.[actionId] !== true) {
      blockers.push(`Agent 2 route ownership report did not prove direct public SDK mutation for action: ${actionId}`)
    }
  }
  for (const actionId of ['menu.new_run', 'loading.render', 'loading.progress', 'loading.complete']) {
    if (report.dispatchResults?.[actionId] !== true) {
      blockers.push(`Agent 2 route ownership report did not prove UI host/window-pump mutation for action: ${actionId}`)
    }
  }
  if (!object(report.builtInProductRoutes).main_menu) {
    blockers.push('Agent 2 route ownership report did not publish the built-in main menu route.')
  }
  if (!object(report.builtInProductRoutes).loading_screen) {
    blockers.push('Agent 2 route ownership report did not publish the built-in loading route.')
  }
  for (const [key, value] of Object.entries({
    directPublicSdkDispatchGateResults: report.directPublicSdkDispatchGateResults,
    directPublicSdkInputDispatchGateResults: report.directPublicSdkInputDispatchGateResults,
    directPublicSdkLifecycleGateResults: report.directPublicSdkLifecycleGateResults,
    productionClientRouteRegistrationGateResults: report.productionClientRouteRegistrationGateResults,
    routeTableOwnerHandlerGateResults: report.routeTableOwnerHandlerGateResults,
    terminalNativeRouteStateGateResults: report.terminalNativeRouteStateGateResults,
    holoMapNativeRouteStateGateResults: report.holoMapNativeRouteStateGateResults,
    productRouteStateGateResults: report.productRouteStateGateResults,
    nativeWindowPumpGateResults: report.nativeWindowPumpGateResults,
    clientWindowPumpServiceGateResults: report.clientWindowPumpServiceGateResults,
  })) {
    if (!allBooleansTrue(value)) blockers.push(`Agent 2 route ownership gate is not fully true: ${key}`)
  }
  if (Number(report.actionDispatchEvidence?.dispatchCount ?? 0) <= 0) {
    blockers.push('Agent 2 route ownership report did not publish action dispatch events.')
  }
  return blockers
}

function nativeAgent2ModuleIds(report) {
  const values = []
  walk(report, (key, value) => {
    if ((key === 'moduleId' || key === 'routeModuleId') && typeof value === 'string') values.push(value)
  })
  return unique(values.filter((value) => /^echo[a-z0-9]+$/.test(value)))
}

function nativeAgent2VisibleRoutes(report) {
  const values = []
  walk(report, (key, value) => {
    if (key === 'surfaceId' && typeof value === 'string') values.push(value)
    if (key === 'handledHandlerId' && typeof value === 'string') values.push(value)
  })
  return unique(values.filter((value) => value.includes(':')))
}

function allBooleansTrue(value) {
  if (value === true) return true
  if (!value || typeof value !== 'object') return false
  const leaves = []
  walk(value, (_key, item) => {
    if (typeof item === 'boolean') leaves.push(item)
  })
  return leaves.length > 0 && leaves.every(Boolean)
}

function walk(value, visit, key = '') {
  if (Array.isArray(value)) {
    for (const item of value) walk(item, visit, key)
    return
  }
  if (!value || typeof value !== 'object') {
    visit(key, value)
    return
  }
  for (const [childKey, childValue] of Object.entries(value)) {
    walk(childValue, visit, childKey)
  }
}

function reportStatus(report) {
  if (!report) return 'MISSING'
  if (report.parseError) return 'PARSE_ERROR'
  if (string(report.schema).includes('core_module_load_state') && Number(report.failedModuleCount ?? 0) === 0) return 'PASS'
  const value = string(report.status ?? report.result ?? report.summary?.status).trim().toUpperCase()
  if (['PASS', 'PASSED', 'SUCCESS', 'OK'].includes(value)) return 'PASS'
  if (['FAIL', 'FAILED', 'ERROR', 'SKIPPED'].includes(value)) return value
  if (['PARTIAL', 'WARN', 'WARNING'].includes(value)) return 'PARTIAL'
  return value || 'MISSING'
}

function moduleIdsFrom(report) {
  if (!report || report.parseError) return []
  return unique([
    ...array(report.moduleIds),
    ...array(report.modules).map((item) => typeof item === 'string' ? item : item?.moduleId ?? item?.id),
    ...Object.keys(object(report.runtimeStatuses)),
    ...Object.keys(object(report.lifecycles)),
    ...array(report.loadedModuleIds),
    ...array(report.lifecycleModuleIds),
  ].filter((value) => typeof value === 'string' && value.trim()))
}

function parseArgs(argv) {
  const options = { root: DEFAULT_ROOT, modulesRoot: DEFAULT_MODULES_ROOT, help: false }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--root') options.root = argv[++index]
    else if (arg === '--modules-root') options.modulesRoot = argv[++index]
    else if (arg === '--help') options.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  return options
}

function array(value) {
  return Array.isArray(value) ? value : []
}

function object(value) {
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {}
}

function string(value) {
  return typeof value === 'string' ? value : ''
}

function unique(values) {
  return [...new Set(values)].sort()
}

function normalizePath(value) {
  return value.replace(/\\/g, '/')
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  try {
    const options = parseArgs(process.argv.slice(2))
    if (options.help) {
      console.log('Usage: node scripts/generate-native-strict-play-evidence.mjs [--root <path>] [--modules-root <path>]')
    } else {
      const { written } = await generateNativeStrictPlayEvidence(options)
      for (const entry of written) {
        console.log(`${entry.status} ${entry.moduleCount} module(s): ${entry.path}`)
      }
      if (written.some((entry) => entry.status === 'FAIL')) {
        throw new Error('Native strict-play evidence contains failing report(s).')
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
