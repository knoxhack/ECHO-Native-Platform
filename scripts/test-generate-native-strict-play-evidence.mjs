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

try {
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

  const { written } = await generateNativeStrictPlayEvidence({ root })
  assert.equal(written.length, 5)

  const fullCatalog = await readJson(root, 'build/native-full-catalog-play/native-full-catalog-play.json')
  assert.equal(fullCatalog.status, 'PARTIAL')
  assert.equal(fullCatalog.allModules, true)
  assert.deepEqual(fullCatalog.moduleIds, ['echocore', 'echoindex'])

  const ui = await readJson(root, 'build/native-ui-surfaces/native-ui-surfaces.json')
  assert.equal(ui.status, 'PASS')
  assert.deepEqual(ui.moduleIds, ['echohudcore', 'echoindex'])

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
} finally {
  await fs.rm(root, { recursive: true, force: true })
}

console.log('generate-native-strict-play-evidence tests passed')
