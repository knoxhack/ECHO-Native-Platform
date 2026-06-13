#!/usr/bin/env node
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import crypto from 'node:crypto'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

const repoRoot = process.cwd()
const script = path.join(repoRoot, 'scripts', 'generate-ashfall-native-public-beta-evidence.mjs')
const ZIP_BYTES = Buffer.from([0x50, 0x4b, 0x03, 0x04, 0x0a, 0x00, 0x00, 0x00])

function sha256(buffer) {
  return crypto.createHash('sha256').update(buffer).digest('hex')
}

async function writeJson(root, relPath, value) {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function writeFile(root, relPath, value = 'fixture\n') {
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

async function writeEvidenceFiles(root, sessionCount = 3) {
  const packageBytes = Buffer.concat([ZIP_BYTES, Buffer.from('ashfall public beta package\n', 'utf8')])
  await writeFile(root, 'fixtures/ashfall/native-public-beta/package.zip', packageBytes)
  await writeFile(root, 'fixtures/ashfall/native-public-beta/echo-release.json', '{"pack":"ashfall-native-edition"}\n')
  await writeFile(root, 'fixtures/ashfall/native-public-beta/latest.log', '[Render thread/INFO]: Minecraft title reached\n')
  await writeFile(root, 'fixtures/ashfall/native-public-beta/crash-review.md', 'No crash reports or crash signatures found.\n')
  await writeFile(root, 'fixtures/ashfall/native-public-beta/support-runbook.md', 'Support intake and escalation steps.\n')
  await writeFile(root, 'fixtures/ashfall/native-public-beta/rollback-plan.md', 'Rollback to previous public alpha assets.\n')
  await writeFile(root, 'fixtures/ashfall/native-public-beta/known-limitations.md', 'Known limitations are published.\n')

  const sessions = []
  for (let index = 1; index <= sessionCount; index += 1) {
    const logPath = `fixtures/ashfall/native-public-beta/session-${index}.log`
    const notesPath = `fixtures/ashfall/native-public-beta/session-${index}.md`
    const bundlePath = `fixtures/ashfall/native-public-beta/session-${index}-support.zip`
    await writeFile(root, logPath, `session ${index} title world loaded no crash\n`)
    await writeFile(root, notesPath, `session ${index} notes\n`)
    await writeFile(root, bundlePath, Buffer.concat([ZIP_BYTES, Buffer.from(`support bundle ${index}\n`, 'utf8')]))
    sessions.push({
      id: `session-${index}`,
      tester: `tester-${index}`,
      startedAt: `2026-06-10T0${index}:00:00Z`,
      endedAt: `2026-06-10T0${index}:45:00Z`,
      durationMinutes: 45,
      buildId: 'ashfall-native-0.1.0-rc1',
      logPath,
      notesPath,
      supportBundlePath: bundlePath,
      launchedViaNativeLoader: true,
      minecraftReachedTitle: true,
      worldLoaded: true,
      supportBundleExported: true,
      noCrash: true,
    })
  }

  await writeJson(root, 'fixtures/ashfall/native-public-beta/manual-evidence.json', {
    schemaVersion: 'echo.ashfall.native-public-beta.manual-evidence.v1',
    generatedAt: '2026-06-10T12:00:00Z',
    releaseCandidate: {
      buildId: 'ashfall-native-0.1.0-rc1',
      artifactSha256: 'a'.repeat(64),
      releaseManifestPath: 'fixtures/ashfall/native-public-beta/echo-release.json',
    },
    sessions,
    crashReview: {
      reviewedAt: '2026-06-10T13:00:00Z',
      latestLogPath: 'fixtures/ashfall/native-public-beta/latest.log',
      reviewPath: 'fixtures/ashfall/native-public-beta/crash-review.md',
      noCrashEvidence: true,
      crashSignalInLatestLog: false,
      crashReportCount: 0,
    },
    publicBeta: {
      publicBetaOpen: true,
      publicBetaReady: true,
      testerPackageReady: true,
      supportBundleExportReady: true,
      rollbackReady: true,
      knownLimitationsPublished: true,
      candidatePackageId: 'ashfall-native-public-beta-candidate',
      packagePath: 'fixtures/ashfall/native-public-beta/package.zip',
      packageSha256: sha256(packageBytes),
      supportRunbookPath: 'fixtures/ashfall/native-public-beta/support-runbook.md',
      rollbackPlanPath: 'fixtures/ashfall/native-public-beta/rollback-plan.md',
      knownLimitationsPath: 'fixtures/ashfall/native-public-beta/known-limitations.md',
    },
  })
}

const tmp = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-ashfall-native-beta-evidence-test-'))
try {
  const passRoot = path.join(tmp, 'pass')
  await writeEvidenceFiles(passRoot, 3)
  const pass = run(passRoot)
  assert.equal(pass.status, 0, `${pass.stdout}\n${pass.stderr}`)
  const passPayload = JSON.parse(pass.stdout)
  assert.equal(passPayload.ok, true)
  const sessionReport = JSON.parse(await fs.readFile(path.join(passRoot, 'reports/echo-native/ashfall/native-loader-beta-session-proof-matrix.json'), 'utf8'))
  const crashReport = JSON.parse(await fs.readFile(path.join(passRoot, 'reports/echo-native/ashfall/native-loader-beta-crash-intake.json'), 'utf8'))
  const publicReport = JSON.parse(await fs.readFile(path.join(passRoot, 'reports/echo-native/ashfall/public-beta-tester-package-readiness.json'), 'utf8'))
  assert.equal(sessionReport.status, 'PASS')
  assert.equal(sessionReport.summary.dryRunOnly, false)
  assert.equal(sessionReport.data.reportOnly, false)
  assert.equal(sessionReport.data.qualifiedSessionCount, 3)
  assert.equal(sessionReport.data.targetInternalSessionCount, 3)
  assert.equal(sessionReport.generatedAt, '2026-06-10T12:00:00Z')
  assert.equal(crashReport.status, 'PASS')
  assert.equal(crashReport.summary.dryRunOnly, false)
  assert.equal(crashReport.data.noCrashEvidence, true)
  assert.equal(crashReport.data.crashSignalInLatestLog, false)
  assert.equal(publicReport.status, 'PASS')
  assert.equal(publicReport.summary.dryRunOnly, false)
  assert.equal(publicReport.data.publicBetaOpen, true)
  assert.equal(publicReport.data.publicBetaReady, true)
  assert.equal(publicReport.data.testerPackageReady, true)
  assert.equal(publicReport.data.supportBundleExportReady, true)

  const missingRoot = path.join(tmp, 'missing')
  const missing = run(missingRoot)
  assert.equal(missing.status, 1)
  const missingSession = JSON.parse(await fs.readFile(path.join(missingRoot, 'reports/echo-native/ashfall/native-loader-beta-session-proof-matrix.json'), 'utf8'))
  assert.equal(missingSession.status, 'FAILED')
  assert.equal(missingSession.summary.dryRunOnly, true)
  assert.match(missingSession.issues[0].summary, /Manual beta evidence file is missing/u)

  const incompleteRoot = path.join(tmp, 'incomplete')
  await writeEvidenceFiles(incompleteRoot, 2)
  const incomplete = run(incompleteRoot)
  assert.equal(incomplete.status, 1)
  const incompletePublic = JSON.parse(await fs.readFile(path.join(incompleteRoot, 'reports/echo-native/ashfall/public-beta-tester-package-readiness.json'), 'utf8'))
  assert.equal(incompletePublic.status, 'FAILED')
  assert.equal(incompletePublic.summary.dryRunOnly, true)
  assert(incompletePublic.issues.some((item) => /SESSION-COUNT-LOW/u.test(item.code)))

  const missingSupportRoot = path.join(tmp, 'missing-support')
  await writeEvidenceFiles(missingSupportRoot, 3)
  await fs.rm(path.join(missingSupportRoot, 'fixtures/ashfall/native-public-beta/session-2-support.zip'))
  const missingSupport = run(missingSupportRoot)
  assert.equal(missingSupport.status, 1)
  const missingSupportSession = JSON.parse(await fs.readFile(path.join(missingSupportRoot, 'reports/echo-native/ashfall/native-loader-beta-session-proof-matrix.json'), 'utf8'))
  assert.equal(missingSupportSession.status, 'FAILED')
  assert.equal(missingSupportSession.summary.dryRunOnly, true)
  assert(missingSupportSession.issues.some((item) => item.code === 'ECHO-ASHFALL-BETA-EVIDENCE-FILE-MISSING' && /Session support bundle for session-2/u.test(item.summary)))

  const invalidPackageZipRoot = path.join(tmp, 'invalid-package-zip')
  await writeEvidenceFiles(invalidPackageZipRoot, 3)
  await writeFile(invalidPackageZipRoot, 'fixtures/ashfall/native-public-beta/package.zip', 'not a zip\n')
  const invalidPackageEvidencePath = path.join(invalidPackageZipRoot, 'fixtures/ashfall/native-public-beta/manual-evidence.json')
  const invalidPackageEvidence = JSON.parse(await fs.readFile(invalidPackageEvidencePath, 'utf8'))
  invalidPackageEvidence.publicBeta.packageSha256 = sha256(Buffer.from('not a zip\n', 'utf8'))
  await writeJson(invalidPackageZipRoot, 'fixtures/ashfall/native-public-beta/manual-evidence.json', invalidPackageEvidence)
  const invalidPackageZip = run(invalidPackageZipRoot)
  assert.equal(invalidPackageZip.status, 1)
  const invalidPackagePublic = JSON.parse(await fs.readFile(path.join(invalidPackageZipRoot, 'reports/echo-native/ashfall/public-beta-tester-package-readiness.json'), 'utf8'))
  assert(invalidPackagePublic.issues.some((item) => item.code === 'ECHO-ASHFALL-BETA-EVIDENCE-ZIP-INVALID' && /Public beta package/u.test(item.summary)))

  const invalidSupportZipRoot = path.join(tmp, 'invalid-support-zip')
  await writeEvidenceFiles(invalidSupportZipRoot, 3)
  await writeFile(invalidSupportZipRoot, 'fixtures/ashfall/native-public-beta/session-1-support.zip', 'not a zip\n')
  const invalidSupportZip = run(invalidSupportZipRoot)
  assert.equal(invalidSupportZip.status, 1)
  const invalidSupportSession = JSON.parse(await fs.readFile(path.join(invalidSupportZipRoot, 'reports/echo-native/ashfall/native-loader-beta-session-proof-matrix.json'), 'utf8'))
  assert(invalidSupportSession.issues.some((item) => item.code === 'ECHO-ASHFALL-BETA-EVIDENCE-ZIP-INVALID' && /Session support bundle for session-1/u.test(item.summary)))
} finally {
  await fs.rm(tmp, { recursive: true, force: true })
}

console.log('Ashfall Native public beta evidence reducer fixtures passed.')
