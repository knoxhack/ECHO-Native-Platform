#!/usr/bin/env node
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

const repoRoot = process.cwd()
const script = path.join(repoRoot, 'scripts', 'generate-ashfall-gameplay-qa-evidence.mjs')
const PNG_BYTES = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00])
const ZIP_BYTES = Buffer.from([0x50, 0x4b, 0x03, 0x04, 0x00])

async function writeJson(root, relPath, value) {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function writeText(root, relPath, value = 'fixture\n') {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, value, 'utf8')
}

async function writeBytes(root, relPath, value) {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, value)
}

function run(root) {
  return spawnSync(process.execPath, [script, '--root', root], {
    cwd: repoRoot,
    encoding: 'utf8',
    windowsHide: true,
  })
}

function report(data) {
  return {
    status: 'PASS',
    generatedAt: '2026-06-11T00:00:00Z',
    summary: {
      dryRunOnly: false,
      blockingDiagnostics: 0,
      diagnosticCount: 0,
    },
    data,
  }
}

async function writePassingFixture(root) {
  await writeJson(root, 'reports/echo-native/ashfall/tester-playable-evidence.json', report({
    baselinePlayableEvidence: true,
    playerJoinObserved: true,
    worldSavePresent: true,
    screenshotCount: 2,
  }))
  await writeJson(root, 'reports/echo-native/ashfall/minecraft-baseline-playability.json', report({
    baselinePlayable: true,
    minecraftWorldLoaded: true,
    worldSavePresent: true,
  }))
  await writeJson(root, 'reports/echo-native/ashfall/native-loader-beta-crash-intake.json', report({
    noCrashEvidence: true,
    crashSignalInLatestLog: false,
    crashReportCount: 0,
  }))
  await writeText(root, 'fixtures/ashfall/gameplay-qa/evidence/first-hour-notes.md')
  await writeText(root, 'fixtures/ashfall/gameplay-qa/evidence/save-reload-notes.md')
  await writeText(root, 'fixtures/ashfall/gameplay-qa/evidence/route-verification.md')
  await writeText(root, 'fixtures/ashfall/gameplay-qa/evidence/ending-verification.md')
  await writeBytes(root, 'fixtures/ashfall/gameplay-qa/evidence/screenshots/first-launch.png', PNG_BYTES)
  await writeBytes(root, 'fixtures/ashfall/gameplay-qa/evidence/screenshots/server-client-export.png', PNG_BYTES)
  await writeText(root, 'fixtures/ashfall/gameplay-qa/evidence/server/dedicated-server.log')
  await writeText(root, 'fixtures/ashfall/gameplay-qa/evidence/server/client-export.log')
  await writeBytes(root, 'fixtures/ashfall/gameplay-qa/evidence/saves/fresh-world.zip', ZIP_BYTES)
  await writeBytes(root, 'fixtures/ashfall/gameplay-qa/evidence/saves/reloaded-world.zip', ZIP_BYTES)
  await writeJson(root, 'fixtures/ashfall/gameplay-qa/manual-evidence.json', {
    schemaVersion: 'echo.ashfall.gameplay-qa.manual.v1',
    packId: 'ashfall',
    generatedAt: '2026-06-11T00:05:00Z',
    dryRunOnly: false,
    claims: {
      realClientFirstHourSmoke: true,
      freshWorldCreated: true,
      saveReloadVerified: true,
      routeVerified: true,
      dedicatedServerSmoke: true,
      serverClientExportSmoke: true,
      endingVerified: true,
      noCrashEvidence: true,
    },
    screenshots: [
      'fixtures/ashfall/gameplay-qa/evidence/screenshots/first-launch.png',
      'fixtures/ashfall/gameplay-qa/evidence/screenshots/server-client-export.png',
    ],
    serverLogs: [
      'fixtures/ashfall/gameplay-qa/evidence/server/dedicated-server.log',
      'fixtures/ashfall/gameplay-qa/evidence/server/client-export.log',
    ],
    saveSnapshots: [
      'fixtures/ashfall/gameplay-qa/evidence/saves/fresh-world.zip',
      'fixtures/ashfall/gameplay-qa/evidence/saves/reloaded-world.zip',
    ],
    supportingFiles: [
      'fixtures/ashfall/gameplay-qa/evidence/first-hour-notes.md',
      'fixtures/ashfall/gameplay-qa/evidence/save-reload-notes.md',
      'fixtures/ashfall/gameplay-qa/evidence/route-verification.md',
      'fixtures/ashfall/gameplay-qa/evidence/ending-verification.md',
    ],
  })
}

const tmp = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-ashfall-gameplay-qa-test-'))
try {
  const passRoot = path.join(tmp, 'pass')
  await writePassingFixture(passRoot)
  const pass = run(passRoot)
  assert.equal(pass.status, 0, `${pass.stdout}\n${pass.stderr}`)
  const passReport = JSON.parse(await fs.readFile(path.join(passRoot, 'fixtures/ashfall/tester-playable-evidence.json'), 'utf8'))
  assert.equal(passReport.status, 'PASS')
  assert.equal(passReport.summary.dryRunOnly, false)
  assert.equal(passReport.data.realClientFirstHourSmoke, true)
  assert.equal(passReport.data.serverClientExportSmoke, true)
  assert.equal(passReport.data.endingVerified, true)

  const failRoot = path.join(tmp, 'fail')
  await writePassingFixture(failRoot)
  await fs.rm(path.join(failRoot, 'fixtures/ashfall/gameplay-qa/manual-evidence.json'))
  const fail = run(failRoot)
  assert.equal(fail.status, 1)
  const failReport = JSON.parse(await fs.readFile(path.join(failRoot, 'fixtures/ashfall/tester-playable-evidence.json'), 'utf8'))
  assert.equal(failReport.status, 'FAILED')
  assert.equal(failReport.data.routeVerified, false)
  assert(failReport.summary.diagnostics.some((diagnostic) => /Manual gameplay QA evidence is missing/u.test(diagnostic)))

  const emptyEvidenceRoot = path.join(tmp, 'empty-evidence')
  await writePassingFixture(emptyEvidenceRoot)
  const manualPath = path.join(emptyEvidenceRoot, 'fixtures/ashfall/gameplay-qa/manual-evidence.json')
  const manual = JSON.parse(await fs.readFile(manualPath, 'utf8'))
  manual.supportingFiles = []
  manual.screenshots = []
  manual.serverLogs = []
  manual.saveSnapshots = []
  await fs.writeFile(manualPath, `${JSON.stringify(manual, null, 2)}\n`, 'utf8')
  const emptyEvidence = run(emptyEvidenceRoot)
  assert.equal(emptyEvidence.status, 1)
  const emptyEvidenceReport = JSON.parse(await fs.readFile(path.join(emptyEvidenceRoot, 'fixtures/ashfall/tester-playable-evidence.json'), 'utf8'))
  assert.equal(emptyEvidenceReport.status, 'FAILED')
  assert.equal(emptyEvidenceReport.data.routeVerified, true)
  assert(emptyEvidenceReport.summary.diagnostics.some((diagnostic) => /screenshots must include at least 2/u.test(diagnostic)))
  assert(emptyEvidenceReport.summary.diagnostics.some((diagnostic) => /first-launch or first-hour screenshot/u.test(diagnostic)))

  const invalidScreenshotRoot = path.join(tmp, 'invalid-screenshot')
  await writePassingFixture(invalidScreenshotRoot)
  await writeText(invalidScreenshotRoot, 'fixtures/ashfall/gameplay-qa/evidence/screenshots/first-launch.png', 'not a png\n')
  const invalidScreenshot = run(invalidScreenshotRoot)
  assert.equal(invalidScreenshot.status, 1)
  const invalidScreenshotReport = JSON.parse(await fs.readFile(path.join(invalidScreenshotRoot, 'fixtures/ashfall/tester-playable-evidence.json'), 'utf8'))
  assert(invalidScreenshotReport.summary.diagnostics.some((diagnostic) => /Manual evidence screenshot is not a PNG/u.test(diagnostic)))

  const invalidSaveRoot = path.join(tmp, 'invalid-save')
  await writePassingFixture(invalidSaveRoot)
  await writeText(invalidSaveRoot, 'fixtures/ashfall/gameplay-qa/evidence/saves/fresh-world.zip', 'not a zip\n')
  const invalidSave = run(invalidSaveRoot)
  assert.equal(invalidSave.status, 1)
  const invalidSaveReport = JSON.parse(await fs.readFile(path.join(invalidSaveRoot, 'fixtures/ashfall/tester-playable-evidence.json'), 'utf8'))
  assert(invalidSaveReport.summary.diagnostics.some((diagnostic) => /Manual evidence save snapshot is not a ZIP archive/u.test(diagnostic)))
} finally {
  await fs.rm(tmp, { recursive: true, force: true })
}

console.log('Ashfall gameplay QA evidence generator fixtures passed.')
