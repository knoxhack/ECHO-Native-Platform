#!/usr/bin/env node
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

const repoRoot = process.cwd()
const script = path.join(repoRoot, 'scripts', 'generate-ashfall-native-playability-evidence.mjs')

async function writeJson(root, relPath, value) {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function writeFile(root, relPath, value) {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, value, 'utf8')
}

function run(root, fakeScript) {
  return spawnSync(process.execPath, [
    script,
    '--root',
    root,
    '--command',
    process.execPath,
    '--arg',
    fakeScript,
  ], {
    cwd: repoRoot,
    encoding: 'utf8',
    windowsHide: true,
  })
}

function upstreamReport(status = 'PASS') {
  return {
    schema: 'echo.native.fixture.v1',
    generatedAt: '2026-06-11T00:00:00Z',
    status,
    summary: {
      dryRunOnly: false,
      blockingDiagnostics: status === 'PASS' ? 0 : 1,
    },
    data: {},
  }
}

const tmp = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-ashfall-playability-refresh-test-'))
try {
  const passRoot = path.join(tmp, 'pass')
  await writeFile(passRoot, 'fake-pass.mjs', `
import fs from 'node:fs/promises'
import path from 'node:path'
const root = process.cwd()
await fs.mkdir(path.join(root, 'reports/echo-native/ashfall'), { recursive: true })
const report = ${JSON.stringify(upstreamReport())}
await fs.writeFile(path.join(root, 'reports/echo-native/ashfall/tester-playable-evidence.json'), JSON.stringify(report, null, 2))
await fs.writeFile(path.join(root, 'reports/echo-native/ashfall/minecraft-baseline-playability.json'), JSON.stringify(report, null, 2))
`)
  const pass = run(passRoot, path.join(passRoot, 'fake-pass.mjs'))
  assert.equal(pass.status, 0, `${pass.stdout}\n${pass.stderr}`)
  const passReport = JSON.parse(await fs.readFile(path.join(passRoot, 'reports/echo-native/ashfall/native-playability-evidence-refresh.json'), 'utf8'))
  assert.equal(passReport.status, 'PASS')
  assert.equal(passReport.summary.dryRunOnly, false)
  assert.equal(passReport.data.observedReports.length, 2)
  assert.equal(passReport.data.observedReports.every((entry) => entry.present), true)

  const missingRoot = path.join(tmp, 'missing')
  await writeFile(missingRoot, 'fake-missing.mjs', "console.error('no reports written')\n")
  const missing = run(missingRoot, path.join(missingRoot, 'fake-missing.mjs'))
  assert.equal(missing.status, 1)
  const missingReport = JSON.parse(await fs.readFile(path.join(missingRoot, 'reports/echo-native/ashfall/native-playability-evidence-refresh.json'), 'utf8'))
  assert.equal(missingReport.status, 'FAILED')
  assert.equal(missingReport.data.observedReports.every((entry) => !entry.present), true)
  assert(missingReport.issues.some((issue) => issue.code === 'ECHO-ASHFALL-PLAYABILITY-REPORT-MISSING'))

  const staleRoot = path.join(tmp, 'stale')
  await writeJson(staleRoot, 'reports/echo-native/ashfall/tester-playable-evidence.json', {
    ...upstreamReport(),
    generatedAt: '1970-01-01T00:00:00Z',
  })
  await writeJson(staleRoot, 'reports/echo-native/ashfall/minecraft-baseline-playability.json', upstreamReport())
  await writeFile(staleRoot, 'fake-stale.mjs', "console.log('reports already present')\n")
  const stale = run(staleRoot, path.join(staleRoot, 'fake-stale.mjs'))
  assert.equal(stale.status, 1)
  const staleReport = JSON.parse(await fs.readFile(path.join(staleRoot, 'reports/echo-native/ashfall/native-playability-evidence-refresh.json'), 'utf8'))
  assert.equal(staleReport.status, 'FAILED')
  assert(staleReport.issues.some((issue) => issue.code === 'ECHO-ASHFALL-PLAYABILITY-REPORT-STALE'))
} finally {
  await fs.rm(tmp, { recursive: true, force: true })
}

console.log('Ashfall Native playability evidence refresh fixtures passed.')
