#!/usr/bin/env node
import crypto from 'node:crypto'
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

const DEFAULT_EVIDENCE = 'fixtures/ashfall/native-public-beta/manual-evidence.json'
const DEFAULT_REPORTS_DIR = 'reports/echo-native/ashfall'
const TARGET_SESSION_COUNT = 3
const REQUIRED_PUBLIC_BETA_FLAGS = [
  'publicBetaOpen',
  'publicBetaReady',
  'testerPackageReady',
  'supportBundleExportReady',
  'rollbackReady',
  'knownLimitationsPublished',
]
const ZIP_SIGNATURES = [
  Buffer.from([0x50, 0x4b, 0x03, 0x04]),
  Buffer.from([0x50, 0x4b, 0x05, 0x06]),
  Buffer.from([0x50, 0x4b, 0x07, 0x08]),
]
const SHA256_PATTERN = /^[a-f0-9]{64}$/iu

function usage() {
  return `Usage: node scripts/generate-ashfall-native-public-beta-evidence.mjs [options]

Reduces real Ashfall Native beta evidence into the three Phase 7 reports consumed
by ECHO-Release-Index release readiness.

Options:
  --root <dir>          ECHO-Native-Platform root. Default: current directory.
  --evidence <path>     Manual evidence JSON. Default: ${DEFAULT_EVIDENCE}.
  --reports-dir <path>  Output report directory. Default: ${DEFAULT_REPORTS_DIR}.
  --help                Print this help text.
`
}

function parseArgs(argv) {
  const args = {
    root: process.cwd(),
    evidence: DEFAULT_EVIDENCE,
    reportsDir: DEFAULT_REPORTS_DIR,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--root') args.root = path.resolve(argv[++index])
    else if (arg === '--evidence') args.evidence = argv[++index]
    else if (arg === '--reports-dir') args.reportsDir = argv[++index]
    else if (arg === '--help' || arg === '-h') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  args.evidencePath = path.isAbsolute(args.evidence) ? args.evidence : path.join(args.root, args.evidence)
  args.reportsDirPath = path.isAbsolute(args.reportsDir) ? args.reportsDir : path.join(args.root, args.reportsDir)
  return args
}

async function readJson(filePath) {
  return JSON.parse(await fs.readFile(filePath, 'utf8'))
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function rel(root, filePath) {
  const relative = path.relative(root, filePath).replace(/\\/g, '/')
  return relative && !relative.startsWith('../') && relative !== '..' ? relative : filePath.replace(/\\/g, '/')
}

function issue(diagnostics, code, severity, summary, likelyFiles = []) {
  diagnostics.push({
    code,
    severity,
    packId: 'ashfall',
    title: summary,
    summary,
    likelyFiles,
    suggestedFix: 'Add real beta evidence, rerun this reducer, then rerun Ashfall release readiness.',
  })
}

function isBlocking(diagnostic) {
  return ['ERROR', 'FATAL'].includes(diagnostic.severity)
}

function generatedAt(manual) {
  return manual?.generatedAt && manual.generatedAt !== '1970-01-01T00:00:00Z'
    ? manual.generatedAt
    : new Date().toISOString()
}

function status(diagnostics) {
  if (diagnostics.some((diagnostic) => diagnostic.severity === 'FATAL')) return 'BLOCKED'
  if (diagnostics.some((diagnostic) => diagnostic.severity === 'ERROR')) return 'FAILED'
  if (diagnostics.some((diagnostic) => diagnostic.severity === 'WARNING')) return 'PASS_WITH_WARNINGS'
  return 'PASS'
}

function summary(diagnostics, dryRunOnly) {
  return {
    blockingDiagnostics: diagnostics.filter(isBlocking).length,
    diagnosticCount: diagnostics.length,
    dryRunOnly,
  }
}

function safeEvidencePath(root, value, label, diagnostics) {
  if (typeof value !== 'string' || value.trim() === '') {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-PATH-MISSING', 'ERROR', `${label} evidence path is missing.`)
    return null
  }
  const target = path.resolve(root, value)
  const relative = path.relative(path.resolve(root), target)
  if (relative === '' || relative.startsWith('..') || path.isAbsolute(relative)) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-PATH-UNSAFE', 'ERROR', `${label} evidence path points outside repo: ${value}.`, [value])
    return null
  }
  return target
}

async function requireEvidenceFile(root, value, label, diagnostics) {
  const target = safeEvidencePath(root, value, label, diagnostics)
  if (!target) return false
  try {
    const stat = await fs.stat(target)
    if (!stat.isFile()) {
      issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-FILE-MISSING', 'ERROR', `${label} evidence path is not a file: ${value}.`, [value])
      return false
    }
    return true
  } catch {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-FILE-MISSING', 'ERROR', `${label} evidence file is missing: ${value}.`, [value])
    return false
  }
}

async function isZipFile(filePath) {
  const handle = await fs.open(filePath, 'r')
  try {
    const signature = Buffer.alloc(4)
    const result = await handle.read(signature, 0, signature.length, 0)
    return result.bytesRead === signature.length && ZIP_SIGNATURES.some((expected) => signature.equals(expected))
  } finally {
    await handle.close()
  }
}

async function requireEvidenceZipFile(root, value, label, diagnostics) {
  const ok = await requireEvidenceFile(root, value, label, diagnostics)
  if (!ok) return false
  const target = path.resolve(root, value)
  if (!(await isZipFile(target))) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-ZIP-INVALID', 'ERROR', `${label} evidence file is not a ZIP archive: ${value}.`, [value])
    return false
  }
  return true
}

function requireSha(value, label, diagnostics) {
  if (!SHA256_PATTERN.test(String(value ?? ''))) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-SHA-MISSING', 'ERROR', `${label} must include a SHA-256 digest.`)
    return false
  }
  return true
}

function requireCurrentTimestamp(value, label, diagnostics) {
  if (typeof value !== 'string' || value.trim() === '' || value === '1970-01-01T00:00:00Z') {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-TIMESTAMP-MISSING', 'ERROR', `${label} must include a current non-placeholder timestamp.`)
    return false
  }
  return true
}

async function sha256File(filePath) {
  const hash = crypto.createHash('sha256')
  hash.update(await fs.readFile(filePath))
  return hash.digest('hex')
}

async function validatePackageSha(root, publicBeta, diagnostics) {
  const packagePath = publicBeta?.packagePath
  const ok = await requireEvidenceZipFile(root, packagePath, 'Public beta package', diagnostics)
  const expected = publicBeta?.packageSha256
  if (!requireSha(expected, 'Public beta package', diagnostics) || !ok) return false
  const actual = await sha256File(path.resolve(root, packagePath))
  if (actual !== String(expected).toLowerCase()) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-PACKAGE-SHA-MISMATCH', 'ERROR', `Public beta package SHA-256 mismatch: expected ${expected}, found ${actual}.`, [packagePath])
    return false
  }
  return true
}

async function validateSessions(root, manual, diagnostics) {
  const sessions = Array.isArray(manual?.sessions) ? manual.sessions : []
  if (sessions.length < TARGET_SESSION_COUNT) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-SESSION-COUNT-LOW', 'ERROR', `Ashfall Native beta requires ${TARGET_SESSION_COUNT} clean internal sessions; found ${sessions.length}.`, [DEFAULT_EVIDENCE])
  }
  const ids = new Set()
  const qualified = []
  for (const session of sessions) {
    const id = String(session?.id ?? '').trim()
    if (!id) {
      issue(diagnostics, 'ECHO-ASHFALL-BETA-SESSION-ID-MISSING', 'ERROR', 'A beta session is missing id.')
      continue
    }
    if (ids.has(id)) {
      issue(diagnostics, 'ECHO-ASHFALL-BETA-SESSION-ID-DUPLICATE', 'ERROR', `Duplicate beta session id ${id}.`)
      continue
    }
    ids.add(id)
    const sessionDiagnostics = []
    for (const [field, label] of [
      ['logPath', 'Session log'],
      ['notesPath', 'Session notes'],
    ]) {
      await requireEvidenceFile(root, session[field], `${label} for ${id}`, sessionDiagnostics)
    }
    await requireEvidenceZipFile(root, session.supportBundlePath, `Session support bundle for ${id}`, sessionDiagnostics)
    requireCurrentTimestamp(session.startedAt, `${id} startedAt`, sessionDiagnostics)
    requireCurrentTimestamp(session.endedAt, `${id} endedAt`, sessionDiagnostics)
    if (!(Number(session.durationMinutes ?? 0) > 0)) {
      issue(sessionDiagnostics, 'ECHO-ASHFALL-BETA-SESSION-DURATION-MISSING', 'ERROR', `${id} must include a positive durationMinutes.`)
    }
    const expectedBuildId = manual?.releaseCandidate?.buildId
    if (expectedBuildId && session.buildId !== expectedBuildId) {
      issue(sessionDiagnostics, 'ECHO-ASHFALL-BETA-SESSION-BUILD-MISMATCH', 'ERROR', `${id} buildId must match release candidate ${expectedBuildId}.`)
    }
    requireSha(session.artifactSha256 ?? manual?.releaseCandidate?.artifactSha256, `${id} artifact`, sessionDiagnostics)
    for (const [field, label] of [
      ['launchedViaNativeLoader', 'launched via Native Loader'],
      ['minecraftReachedTitle', 'reached Minecraft title'],
      ['worldLoaded', 'loaded a world'],
      ['supportBundleExported', 'exported a support bundle'],
      ['noCrash', 'completed without crash'],
    ]) {
      if (session?.[field] !== true) {
        issue(sessionDiagnostics, 'ECHO-ASHFALL-BETA-SESSION-CLAIM-MISSING', 'ERROR', `${id} did not prove it ${label}.`)
      }
    }
    if (sessionDiagnostics.length > 0) {
      diagnostics.push(...sessionDiagnostics)
      continue
    }
    qualified.push({
      id,
      tester: session.tester ?? null,
      startedAt: session.startedAt ?? null,
      endedAt: session.endedAt ?? null,
      durationMinutes: Number(session.durationMinutes ?? 0),
      buildId: session.buildId ?? manual?.releaseCandidate?.buildId ?? null,
      artifactSha256: session.artifactSha256 ?? manual?.releaseCandidate?.artifactSha256 ?? null,
      logPath: session.logPath,
      notesPath: session.notesPath,
      supportBundlePath: session.supportBundlePath ?? null,
      noCrash: true,
    })
  }
  if (qualified.length < TARGET_SESSION_COUNT) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-QUALIFIED-SESSION-COUNT-LOW', 'ERROR', `Only ${qualified.length} qualified clean session(s) were accepted; target is ${TARGET_SESSION_COUNT}.`, [DEFAULT_EVIDENCE])
  }
  return qualified
}

async function validateCrashReview(root, manual, diagnostics) {
  const crash = manual?.crashReview ?? {}
  requireCurrentTimestamp(crash.reviewedAt, 'Crash review reviewedAt', diagnostics)
  await requireEvidenceFile(root, crash.latestLogPath, 'Crash review latest log', diagnostics)
  await requireEvidenceFile(root, crash.reviewPath, 'Crash review notes', diagnostics)
  if (crash.noCrashEvidence !== true) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-NO-CRASH-EVIDENCE-MISSING', 'ERROR', 'Crash review must prove noCrashEvidence=true.')
  }
  if (crash.crashSignalInLatestLog !== false) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-LATEST-LOG-CRASH-SIGNAL', 'ERROR', 'Crash review latest log must prove crashSignalInLatestLog=false.')
  }
  if (Number(crash.crashReportCount ?? 0) !== 0) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-CRASH-REPORTS-PRESENT', 'ERROR', `Crash review found ${crash.crashReportCount} crash report(s).`)
  }
  return {
    noCrashEvidence: crash.noCrashEvidence === true,
    crashSignalInLatestLog: crash.crashSignalInLatestLog === true,
    crashReportCount: Number(crash.crashReportCount ?? 0),
    latestLog: crash.latestLogPath ?? '',
    reviewPath: crash.reviewPath ?? null,
    reviewedAt: crash.reviewedAt ?? null,
  }
}

async function validatePublicBeta(root, manual, diagnostics) {
  const publicBeta = manual?.publicBeta ?? {}
  for (const flag of REQUIRED_PUBLIC_BETA_FLAGS) {
    if (publicBeta[flag] !== true) {
      issue(diagnostics, 'ECHO-ASHFALL-PUBLIC-BETA-FLAG-MISSING', 'ERROR', `Public beta evidence must set ${flag}=true.`)
    }
  }
  await validatePackageSha(root, publicBeta, diagnostics)
  for (const [field, label] of [
    ['supportRunbookPath', 'Support runbook'],
    ['rollbackPlanPath', 'Rollback plan'],
    ['knownLimitationsPath', 'Known limitations'],
  ]) {
    await requireEvidenceFile(root, publicBeta[field], label, diagnostics)
  }
  return {
    publicBetaOpen: publicBeta.publicBetaOpen === true,
    publicBetaReady: publicBeta.publicBetaReady === true,
    publicReleaseReady: publicBeta.publicBetaReady === true,
    testerPackageReady: publicBeta.testerPackageReady === true,
    testerSafePackageReady: publicBeta.testerPackageReady === true && publicBeta.rollbackReady === true,
    supportBundleExportReady: publicBeta.supportBundleExportReady === true,
    supportBundleLocalOnly: false,
    rollbackReady: publicBeta.rollbackReady === true,
    knownLimitationsPublished: publicBeta.knownLimitationsPublished === true,
    candidatePackageId: publicBeta.candidatePackageId ?? 'ashfall-native-public-beta-candidate',
    packagePath: publicBeta.packagePath ?? null,
    supportRunbookPath: publicBeta.supportRunbookPath ?? null,
    rollbackPlanPath: publicBeta.rollbackPlanPath ?? null,
    knownLimitationsPath: publicBeta.knownLimitationsPath ?? null,
  }
}

function report(schema, phase, statusValue, diagnostics, data, dryRunOnly, generated) {
  return {
    schema,
    generatedAt: generated,
    generator: 'generate-ashfall-native-public-beta-evidence.mjs',
    packId: 'ashfall',
    status: statusValue,
    summary: summary(diagnostics, dryRunOnly),
    issues: diagnostics,
    data: {
      packId: 'ashfall',
      phase,
      generatedEvidenceAt: generated,
      reportOnly: dryRunOnly,
      diagnosticsCaptured: true,
      diagnosticCount: diagnostics.length,
      ...data,
    },
  }
}

function failedReports(args, message) {
  const diagnostics = []
  issue(diagnostics, 'ECHO-ASHFALL-BETA-MANUAL-EVIDENCE-MISSING', 'ERROR', message, [rel(args.root, args.evidencePath)])
  const generated = new Date().toISOString()
  return reportsFor(args, null, diagnostics, [], {
    noCrashEvidence: false,
    crashSignalInLatestLog: false,
    crashReportCount: 0,
    latestLog: '',
  }, {
    publicBetaOpen: false,
    publicBetaReady: false,
    publicReleaseReady: false,
    testerPackageReady: false,
    testerSafePackageReady: false,
    supportBundleExportReady: false,
    supportBundleLocalOnly: true,
    rollbackReady: false,
    knownLimitationsPublished: false,
  }, generated, true)
}

function reportsFor(args, manual, diagnostics, qualifiedSessions, crashReview, publicBeta, generated, dryRunOnly) {
  const statusValue = status(diagnostics)
  const sessionData = {
    summary: statusValue === 'PASS' ? 'Beta soak session proof matrix accepted real internal sessions.' : 'Beta soak session proof matrix is blocked.',
    qualifiedSessionCount: qualifiedSessions.length,
    targetInternalSessionCount: TARGET_SESSION_COUNT,
    sessionProofs: qualifiedSessions,
    publicBetaOpen: publicBeta.publicBetaOpen === true,
  }
  const crashData = {
    summary: statusValue === 'PASS' ? 'Crash evidence reviewed with no crash signal.' : 'Crash evidence is blocked.',
    crashReports: [],
    ...crashReview,
  }
  const publicBetaData = {
    summary: statusValue === 'PASS' ? 'Tester package readiness accepted real public beta evidence.' : 'Tester package readiness is blocked.',
    releaseCandidate: manual?.releaseCandidate ?? null,
    ...publicBeta,
  }

  return new Map([
    ['native-loader-beta-session-proof-matrix.json', report('echo.native.native_loader_beta_session_proof_matrix.v1', 'phase7_native_public_beta_sessions', statusValue, diagnostics, sessionData, dryRunOnly, generated)],
    ['native-loader-beta-crash-intake.json', report('echo.native.native_loader_beta_crash_intake.v1', 'phase7_native_public_beta_crash_intake', statusValue, diagnostics, crashData, dryRunOnly, generated)],
    ['public-beta-tester-package-readiness.json', report('echo.native.public_beta_tester_package_readiness.v1', 'phase7_public_beta_tester_package', statusValue, diagnostics, publicBetaData, dryRunOnly, generated)],
  ])
}

async function generate(args) {
  let manual
  try {
    manual = await readJson(args.evidencePath)
  } catch (error) {
    if (error.code === 'ENOENT') return failedReports(args, `Manual beta evidence file is missing: ${rel(args.root, args.evidencePath)}.`)
    throw error
  }

  const diagnostics = []
  if (manual.schemaVersion !== 'echo.ashfall.native-public-beta.manual-evidence.v1') {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-SCHEMA-MISMATCH', 'ERROR', `Manual beta evidence schemaVersion is ${manual.schemaVersion ?? '(missing)'}.`)
  }
  if (manual.reportOnly === true || manual.dryRunOnly === true || manual.data?.dryRunOnly === true) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-EVIDENCE-DRY-RUN', 'ERROR', 'Manual beta evidence is marked reportOnly/dryRunOnly.')
  }
  requireCurrentTimestamp(manual.generatedAt, 'Manual beta evidence generatedAt', diagnostics)
  if (typeof manual?.releaseCandidate?.buildId !== 'string' || manual.releaseCandidate.buildId.trim() === '' || /fill-me/iu.test(manual.releaseCandidate.buildId)) {
    issue(diagnostics, 'ECHO-ASHFALL-BETA-CANDIDATE-BUILD-ID-MISSING', 'ERROR', 'Release candidate buildId must identify the tested Ashfall Native build.')
  }
  requireSha(manual?.releaseCandidate?.artifactSha256, 'Release candidate artifact', diagnostics)
  await requireEvidenceFile(args.root, manual?.releaseCandidate?.releaseManifestPath, 'Release candidate release manifest', diagnostics)

  const qualifiedSessions = await validateSessions(args.root, manual, diagnostics)
  const crashReview = await validateCrashReview(args.root, manual, diagnostics)
  const publicBeta = await validatePublicBeta(args.root, manual, diagnostics)
  const dryRunOnly = diagnostics.some(isBlocking)
  return reportsFor(args, manual, diagnostics, qualifiedSessions, crashReview, publicBeta, generatedAt(manual), dryRunOnly)
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    process.stdout.write(usage())
    return
  }

  const reports = await generate(args)
  for (const [name, payload] of reports) {
    await writeJson(path.join(args.reportsDirPath, name), payload)
  }
  const entries = [...reports.entries()].map(([name, payload]) => ({
    name,
    status: payload.status,
    dryRunOnly: payload.summary.dryRunOnly,
    blockingDiagnostics: payload.summary.blockingDiagnostics,
  }))
  const ok = entries.every((entry) => ['PASS', 'PASS_WITH_WARNINGS'].includes(entry.status) && entry.dryRunOnly === false)
  process.stdout.write(`${JSON.stringify({ ok, reportsDir: rel(args.root, args.reportsDirPath), reports: entries }, null, 2)}\n`)
  if (!ok) process.exitCode = 1
}

await main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`)
  process.exitCode = 1
})
