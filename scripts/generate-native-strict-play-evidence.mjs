#!/usr/bin/env node
import { promises as fs } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const DEFAULT_ROOT = process.cwd()

const INPUTS = {
  loadState: 'build/native-all-bridgeable-module-artifact-load-state/native-all-bridgeable-module-artifact-load-state.json',
  uiBridge: 'build/agent5/ui-bridge-contract/agent5-ui-bridge-contract.json',
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

export async function generateNativeStrictPlayEvidence({ root = DEFAULT_ROOT } = {}) {
  const repoRoot = path.resolve(root)
  const generatedAt = new Date().toISOString()
  const inputs = {}
  for (const [key, relativePath] of Object.entries(INPUTS)) {
    inputs[key] = await readReport(repoRoot, relativePath)
  }

  const outputs = {
    [OUTPUTS.fullCatalog]: fullCatalogReport({ generatedAt, repoRoot, inputs }),
    [OUTPUTS.uiSurfaces]: aggregateReport({
      generatedAt,
      repoRoot,
      schema: 'echo.native.strict_play.ui_surfaces.v1',
      evidenceKind: 'native-ui-visible-route-proof',
      requiredFor: ['ui'],
      inputs,
      include: ['uiBridge'],
      partialIfAnyInputMissing: false,
      blockers: [],
    }),
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

function fullCatalogReport({ generatedAt, repoRoot, inputs }) {
  const loadState = inputs.loadState.report
  const moduleIds = moduleIdsFrom(loadState)
  const loadStatePass = inputs.loadState.status === 'PASS'
  return {
    schema: 'echo.native.strict_play.full_catalog.v1',
    generatedAt,
    status: loadStatePass ? 'PARTIAL' : 'FAIL',
    runtime: 'echo_native',
    evidenceKind: 'native-full-catalog-load-proof-not-full-play',
    repoRoot: normalizePath(repoRoot),
    requiredFor: ['lifecycle', 'content'],
    moduleIds,
    allModules: loadStatePass,
    sourceReports: sourceReports(inputs, ['loadState']),
    trustedMutations: array(loadState?.trustedMutations),
    visibleRoutes: [],
    saveEvidence: [],
    networkEvidence: [],
    blockers: loadStatePass
      ? [
          'All bridgeable modules load from packaged Native artifacts, but this is not full player-facing content/action/UI proof.',
          'Strict-play content still requires registry/resource/action/world/save reports from Native host services.',
        ]
      : missingBlockers(inputs.loadState, 'Native all-bridgeable artifact load-state smoke is not PASS.'),
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

async function readReport(root, relativePath) {
  const absolute = path.join(root, relativePath)
  try {
    const report = JSON.parse(await fs.readFile(absolute, 'utf8'))
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
  const options = { root: DEFAULT_ROOT, help: false }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--root') options.root = argv[++index]
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
      console.log('Usage: node scripts/generate-native-strict-play-evidence.mjs [--root <path>]')
    } else {
      const { written } = await generateNativeStrictPlayEvidence(options)
      for (const entry of written) {
        console.log(`${entry.status} ${entry.moduleCount} module(s): ${entry.path}`)
      }
    }
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
