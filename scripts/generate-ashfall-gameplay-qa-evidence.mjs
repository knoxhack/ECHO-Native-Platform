#!/usr/bin/env node
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

const DEFAULT_PACK_ID = 'ashfall'
const DEFAULT_MANUAL_EVIDENCE = 'fixtures/ashfall/gameplay-qa/manual-evidence.json'
const DEFAULT_OUT = 'fixtures/ashfall/tester-playable-evidence.json'
const PNG_SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])
const ZIP_SIGNATURES = [
  Buffer.from([0x50, 0x4b, 0x03, 0x04]),
  Buffer.from([0x50, 0x4b, 0x05, 0x06]),
  Buffer.from([0x50, 0x4b, 0x07, 0x08]),
]

function usage() {
  return `Usage: node scripts/generate-ashfall-gameplay-qa-evidence.mjs [options]

Generates the Ashfall Phase 8 gameplay QA evidence consumed by the Release Index
readiness gate. The generator fails closed unless it can tie every release
claim to Native Platform tester evidence plus manual gameplay QA evidence.

Options:
  --root <dir>             Native Platform repository root. Default: current directory.
  --pack-id <id>           Pack id. Default: ${DEFAULT_PACK_ID}.
  --manual <path>          Manual gameplay QA evidence file. Default: ${DEFAULT_MANUAL_EVIDENCE}.
  --out <path>             Output readiness evidence file. Default: ${DEFAULT_OUT}.
  --help                   Print this help text.
`
}

function parseArgs(argv) {
  const args = {
    root: process.cwd(),
    packId: DEFAULT_PACK_ID,
    manual: DEFAULT_MANUAL_EVIDENCE,
    out: DEFAULT_OUT,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--root') args.root = argv[++index]
    else if (arg === '--pack-id') args.packId = argv[++index]
    else if (arg === '--manual') args.manual = argv[++index]
    else if (arg === '--out') args.out = argv[++index]
    else if (arg === '--help') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  args.root = path.resolve(args.root)
  args.manual = path.isAbsolute(args.manual) ? args.manual : path.join(args.root, args.manual)
  args.out = path.isAbsolute(args.out) ? args.out : path.join(args.root, args.out)
  args.reportsDir = path.join(args.root, 'reports', 'echo-native', args.packId)
  return args
}

async function readJsonIfExists(filePath) {
  try {
    return JSON.parse(await fs.readFile(filePath, 'utf8'))
  } catch (error) {
    if (error.code === 'ENOENT') return null
    throw error
  }
}

async function fileExists(filePath) {
  try {
    const stat = await fs.stat(filePath)
    return stat.isFile()
  } catch {
    return false
  }
}

async function fileSize(filePath) {
  return (await fs.stat(filePath)).size
}

async function fileHasSignature(filePath, signatures) {
  const length = Math.max(...signatures.map((signature) => signature.length))
  const handle = await fs.open(filePath, 'r')
  try {
    const buffer = Buffer.alloc(length)
    const result = await handle.read(buffer, 0, buffer.length, 0)
    return signatures.some((signature) => (
      result.bytesRead >= signature.length &&
      buffer.subarray(0, signature.length).equals(signature)
    ))
  } finally {
    await handle.close()
  }
}

function rel(root, filePath) {
  return path.relative(root, filePath).replace(/\\/g, '/')
}

function getPath(value, pointer) {
  if (!pointer) return value
  return pointer.split('.').reduce((current, part) => {
    if (current === undefined || current === null) return undefined
    return current[part]
  }, value)
}

function acceptedStatus(report) {
  return report?.status === 'PASS' || report?.status === 'PASS_WITH_WARNINGS'
}

function notEpoch(value) {
  return typeof value === 'string' && value.length > 0 && value !== '1970-01-01T00:00:00Z'
}

function evidenceBool(manual, name) {
  return manual?.claims?.[name] === true || manual?.data?.[name] === true || manual?.[name] === true
}

function evidenceList(manual, name) {
  const value = manual?.[name] ?? manual?.data?.[name] ?? manual?.claims?.[name]
  return Array.isArray(value) ? value : []
}

function requireListCount(manual, name, minItems, diagnostics) {
  const items = evidenceList(manual, name)
  if (items.length < minItems) {
    diagnostics.push(`Manual gameplay QA evidence ${name} must include at least ${minItems} file path(s); found ${items.length}.`)
  }
  return items
}

function requireListMatch(items, pattern, label, diagnostics) {
  if (!items.some((item) => pattern.test(String(item)))) {
    diagnostics.push(`Manual gameplay QA evidence is missing ${label}.`)
  }
}

async function verifyEvidenceFiles(root, manual, diagnostics) {
  const groups = [
    { name: 'supportingFiles', label: 'supporting file' },
    { name: 'screenshots', label: 'screenshot', signatures: [PNG_SIGNATURE], signatureLabel: 'PNG' },
    { name: 'serverLogs', label: 'server log' },
    { name: 'saveSnapshots', label: 'save snapshot', signatures: ZIP_SIGNATURES, signatureLabel: 'ZIP archive' },
  ]
  for (const group of groups) {
    for (const raw of evidenceList(manual, group.name)) {
      if (typeof raw !== 'string' || raw.trim() === '') {
        diagnostics.push(`Manual evidence includes an empty ${group.label} path.`)
        continue
      }
      const target = path.resolve(root, raw)
      const relative = path.relative(root, target)
      if (relative.startsWith('..') || path.isAbsolute(relative)) {
        diagnostics.push(`Manual evidence path points outside the repository: ${raw}`)
        continue
      }
      if (!(await fileExists(target))) {
        diagnostics.push(`Manual evidence file is missing: ${raw}`)
        continue
      }
      if ((await fileSize(target)) === 0) {
        diagnostics.push(`Manual evidence file is empty: ${raw}`)
        continue
      }
      if (group.signatures && !(await fileHasSignature(target, group.signatures))) {
        diagnostics.push(`Manual evidence ${group.label} is not a ${group.signatureLabel}: ${raw}`)
      }
    }
  }
}

function verifyManualEvidenceShape(manual, diagnostics) {
  const supportingFiles = requireListCount(manual, 'supportingFiles', 4, diagnostics)
  const screenshots = requireListCount(manual, 'screenshots', 2, diagnostics)
  const serverLogs = requireListCount(manual, 'serverLogs', 2, diagnostics)
  const saveSnapshots = requireListCount(manual, 'saveSnapshots', 2, diagnostics)
  requireListMatch(screenshots, /(^|[/\\])(first[-_]?launch|first[-_]?hour)[^/\\]*\.png$/iu, 'a first-launch or first-hour screenshot', diagnostics)
  requireListMatch(screenshots, /(^|[/\\])(server[-_]?client[-_]?export|server[-_]?export|client[-_]?export)[^/\\]*\.png$/iu, 'a server/client export screenshot', diagnostics)
  requireListMatch(supportingFiles, /(^|[/\\])route[-_]?verification[^/\\]*\.md$/iu, 'route verification notes', diagnostics)
  requireListMatch(supportingFiles, /(^|[/\\])ending[-_]?verification[^/\\]*\.md$/iu, 'ending verification notes', diagnostics)
  requireListMatch(serverLogs, /(^|[/\\])dedicated[-_]?server[^/\\]*\.log$/iu, 'a dedicated server log', diagnostics)
  requireListMatch(serverLogs, /(^|[/\\])client[-_]?export[^/\\]*\.log$/iu, 'a client export log', diagnostics)
  requireListMatch(saveSnapshots, /(^|[/\\])fresh[-_]?world[^/\\]*\.zip$/iu, 'a fresh-world save snapshot', diagnostics)
  requireListMatch(saveSnapshots, /(^|[/\\])reloaded[-_]?world[^/\\]*\.zip$/iu, 'a reloaded-world save snapshot', diagnostics)
}

function requireReport(report, name, diagnostics) {
  if (!report) {
    diagnostics.push(`Required upstream Native Platform report is missing: reports/echo-native/ashfall/${name}`)
    return false
  }
  if (!acceptedStatus(report)) {
    diagnostics.push(`Required upstream Native Platform report is not passing: ${name} status=${report.status ?? '(missing)'}`)
    return false
  }
  if (!notEpoch(report.generatedAt)) {
    diagnostics.push(`Required upstream Native Platform report has placeholder generatedAt: ${name}`)
    return false
  }
  if (report.summary?.dryRunOnly === true || report.data?.dryRunOnly === true || report.data?.reportOnly === true) {
    diagnostics.push(`Required upstream Native Platform report is dry-run/report-only: ${name}`)
    return false
  }
  return true
}

function boolFromReport(report, paths) {
  return paths.some((pointer) => getPath(report, pointer) === true)
}

function numericFromReport(report, paths) {
  for (const pointer of paths) {
    const value = Number(getPath(report, pointer))
    if (Number.isFinite(value)) return value
  }
  return 0
}

async function generate(args) {
  const diagnostics = []
  const testerReportPath = path.join(args.reportsDir, 'tester-playable-evidence.json')
  const baselineReportPath = path.join(args.reportsDir, 'minecraft-baseline-playability.json')
  const crashReportPath = path.join(args.reportsDir, 'native-loader-beta-crash-intake.json')
  const testerReport = await readJsonIfExists(testerReportPath)
  const baselineReport = await readJsonIfExists(baselineReportPath)
  const crashReport = await readJsonIfExists(crashReportPath)
  const manual = await readJsonIfExists(args.manual)

  const testerAccepted = requireReport(testerReport, 'tester-playable-evidence.json', diagnostics)
  const baselineAccepted = requireReport(baselineReport, 'minecraft-baseline-playability.json', diagnostics)
  const crashAccepted = requireReport(crashReport, 'native-loader-beta-crash-intake.json', diagnostics)

  if (!manual) {
    diagnostics.push(`Manual gameplay QA evidence is missing: ${rel(args.root, args.manual)}`)
  } else {
    if (manual.schemaVersion !== 'echo.ashfall.gameplay-qa.manual.v1') {
      diagnostics.push(`Manual gameplay QA evidence schemaVersion must be echo.ashfall.gameplay-qa.manual.v1, found ${manual.schemaVersion ?? '(missing)'}`)
    }
    if (manual.packId !== args.packId) diagnostics.push(`Manual gameplay QA evidence packId must be ${args.packId}`)
    if (manual.dryRunOnly === true || manual.reportOnly === true || manual.data?.dryRunOnly === true) {
      diagnostics.push('Manual gameplay QA evidence must not be dry-run/report-only.')
    }
    if (!notEpoch(manual.generatedAt)) diagnostics.push('Manual gameplay QA evidence generatedAt must be current evidence, not epoch or missing.')
    verifyManualEvidenceShape(manual, diagnostics)
    await verifyEvidenceFiles(args.root, manual, diagnostics)
  }

  const testerPlayable = testerAccepted && boolFromReport(testerReport, ['data.baselinePlayableEvidence', 'data.playerJoinObserved'])
  const baselinePlayable = baselineAccepted && boolFromReport(baselineReport, ['data.baselinePlayable', 'data.minecraftWorldLoaded'])
  const worldSavePresent = boolFromReport(testerReport, ['data.worldSavePresent']) || boolFromReport(baselineReport, ['data.worldSavePresent'])
  const noCrashFromReports = crashAccepted &&
    boolFromReport(crashReport, ['data.noCrashEvidence']) &&
    getPath(crashReport, 'data.crashSignalInLatestLog') === false &&
    Number(getPath(crashReport, 'data.crashReportCount') ?? 0) === 0
  const screenshotCount = numericFromReport(testerReport, ['data.screenshotCount'])

  const data = {
    realClientFirstHourSmoke: Boolean(testerPlayable && baselinePlayable && evidenceBool(manual, 'realClientFirstHourSmoke')),
    freshWorldCreated: Boolean(worldSavePresent && evidenceBool(manual, 'freshWorldCreated')),
    saveReloadVerified: evidenceBool(manual, 'saveReloadVerified'),
    routeVerified: evidenceBool(manual, 'routeVerified'),
    dedicatedServerSmoke: evidenceBool(manual, 'dedicatedServerSmoke'),
    serverClientExportSmoke: evidenceBool(manual, 'serverClientExportSmoke'),
    endingVerified: evidenceBool(manual, 'endingVerified'),
    noCrashEvidence: Boolean(noCrashFromReports && evidenceBool(manual, 'noCrashEvidence')),
    testerPlayableReport: rel(args.root, testerReportPath),
    baselinePlayabilityReport: rel(args.root, baselineReportPath),
    crashIntakeReport: rel(args.root, crashReportPath),
    manualEvidence: rel(args.root, args.manual),
    screenshotCount,
    supportingFiles: evidenceList(manual, 'supportingFiles'),
    screenshots: evidenceList(manual, 'screenshots'),
    serverLogs: evidenceList(manual, 'serverLogs'),
    saveSnapshots: evidenceList(manual, 'saveSnapshots'),
  }

  for (const [key, value] of Object.entries(data)) {
    if (typeof value === 'boolean' && value !== true) diagnostics.push(`Gameplay QA claim is not proven: ${key}`)
  }

  const status = diagnostics.length === 0 ? 'PASS' : 'FAILED'
  return {
    schemaVersion: 'echo.ashfall.gameplay-qa.evidence.v1',
    generatedAt: new Date().toISOString(),
    status,
    summary: {
      dryRunOnly: status !== 'PASS',
      blockingDiagnostics: diagnostics.length,
      diagnosticCount: diagnostics.length,
      diagnostics,
    },
    data,
  }
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    process.stdout.write(usage())
    return
  }
  const report = await generate(args)
  await writeJson(args.out, report)
  process.stdout.write(`${JSON.stringify({
    ok: report.status === 'PASS',
    status: report.status,
    out: rel(args.root, args.out),
    diagnostics: report.summary.blockingDiagnostics,
  }, null, 2)}\n`)
  if (report.status !== 'PASS') process.exitCode = 1
}

await main()
