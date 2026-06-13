#!/usr/bin/env node
import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

const DEFAULT_PACK_ID = 'ashfall'
const DEFAULT_OUT = 'reports/echo-native/ashfall/native-playability-evidence-refresh.json'
const DEFAULT_COMMAND = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew'
const DEFAULT_ARGS = [
  'runNativeQaCli',
  '--args=phase13 intake tester-evidence fixtures/ashfall',
  '--console=plain',
  '--no-daemon',
]
const OBSERVED_REPORTS = [
  'tester-playable-evidence.json',
  'minecraft-baseline-playability.json',
]
const OUTPUT_TAIL_LIMIT = 12000

function usage() {
  return `Usage: node scripts/generate-ashfall-native-playability-evidence.mjs [options]

Runs the Native Platform QA tester-evidence intake for Ashfall and writes a
refresh report. The intake is allowed to fail closed, but it must leave current
upstream reports for the Phase 8 gameplay reducer to inspect.

Options:
  --root <dir>       ECHO-Native-Platform root. Default: current directory.
  --pack-id <id>     Pack id. Default: ${DEFAULT_PACK_ID}.
  --out <path>       Refresh report path. Default: ${DEFAULT_OUT}.
  --command <cmd>    Command to run. Default: ${DEFAULT_COMMAND}.
  --arg <value>      Add one command argument. Replaces default args on first use.
  --help             Print this help text.
`
}

function parseArgs(argv) {
  const args = {
    root: process.cwd(),
    packId: DEFAULT_PACK_ID,
    out: DEFAULT_OUT,
    command: DEFAULT_COMMAND,
    commandArgs: [...DEFAULT_ARGS],
    customArgs: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--root') args.root = path.resolve(argv[++index])
    else if (arg === '--pack-id') args.packId = argv[++index]
    else if (arg === '--out') args.out = argv[++index]
    else if (arg === '--command') args.command = argv[++index]
    else if (arg === '--arg') {
      if (!args.customArgs) {
        args.commandArgs = []
        args.customArgs = true
      }
      args.commandArgs.push(argv[++index])
    } else if (arg === '--help' || arg === '-h') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  args.outPath = path.isAbsolute(args.out) ? args.out : path.join(args.root, args.out)
  args.reportsDir = path.join(args.root, 'reports', 'echo-native', args.packId)
  return args
}

function tail(value) {
  const text = String(value ?? '')
  return text.length <= OUTPUT_TAIL_LIMIT ? text : text.slice(text.length - OUTPUT_TAIL_LIMIT)
}

function rel(root, filePath) {
  const relative = path.relative(root, filePath).replace(/\\/g, '/')
  return relative && !relative.startsWith('../') && relative !== '..' ? relative : filePath.replace(/\\/g, '/')
}

async function readJsonIfExists(filePath) {
  try {
    return JSON.parse(await fs.readFile(filePath, 'utf8'))
  } catch (error) {
    if (error.code === 'ENOENT') return null
    throw error
  }
}

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

function runCommand(args) {
  return new Promise((resolve) => {
    const startedAt = Date.now()
    const generatedAt = new Date().toISOString()
    const child = spawn(args.command, args.commandArgs, {
      cwd: args.root,
      env: {
        ...process.env,
        ECHO_NATIVE_GENERATED_AT: process.env.ECHO_NATIVE_GENERATED_AT || generatedAt,
      },
      shell: process.platform === 'win32' && /(?:^|[\\/])gradlew\.bat$/iu.test(args.command),
      windowsHide: true,
    })
    let stdout = ''
    let stderr = ''
    child.stdout.setEncoding('utf8')
    child.stderr.setEncoding('utf8')
    child.stdout.on('data', (chunk) => { stdout += chunk })
    child.stderr.on('data', (chunk) => { stderr += chunk })
    child.on('error', (error) => {
      resolve({
        exitCode: null,
        signal: null,
        generatedAt,
        durationMs: Date.now() - startedAt,
        stdout,
        stderr: `${stderr}${stderr ? '\n' : ''}${error.message}`,
        spawnError: error.message,
      })
    })
    child.on('close', (exitCode, signal) => {
      resolve({
        exitCode,
        signal,
        generatedAt,
        durationMs: Date.now() - startedAt,
        stdout,
        stderr,
        spawnError: null,
      })
    })
  })
}

async function observedReports(args) {
  const reports = []
  for (const fileName of OBSERVED_REPORTS) {
    const filePath = path.join(args.reportsDir, fileName)
    const report = await readJsonIfExists(filePath)
    reports.push({
      path: rel(args.root, filePath),
      present: report !== null,
      status: report?.status ?? null,
      generatedAt: report?.generatedAt ?? null,
      dryRunOnly: report?.summary?.dryRunOnly ?? null,
      blockingDiagnostics: report?.summary?.blockingDiagnostics ?? null,
    })
  }
  return reports
}

function report(args, result, reports) {
  const missingReports = reports.filter((entry) => !entry.present)
  const staleReports = reports.filter((entry) => entry.generatedAt === '1970-01-01T00:00:00Z')
  const blockingDiagnostics =
    (result.exitCode === 0 && result.spawnError === null ? 0 : 1) +
    missingReports.length +
    staleReports.length
  const issues = []
  if (result.exitCode !== 0 || result.spawnError !== null) {
    issues.push({
      code: 'ECHO-ASHFALL-PLAYABILITY-INTAKE-FAILED',
      severity: 'ERROR',
      packId: args.packId,
      title: 'Ashfall playability evidence intake failed',
      summary: result.spawnError
        ? `Failed to start ${args.command}: ${result.spawnError}`
        : `${args.command} ${args.commandArgs.join(' ')} exited with ${result.exitCode}${result.signal ? ` signal ${result.signal}` : ''}.`,
      likelyFiles: ['fixtures/ashfall/isolated-runtime/game', `reports/echo-native/${args.packId}`],
      suggestedFix: 'Run the Ashfall native client/tester flow, create or load a world, then rerun tester-evidence intake.',
    })
  }
  for (const entry of missingReports) {
    issues.push({
      code: 'ECHO-ASHFALL-PLAYABILITY-REPORT-MISSING',
      severity: 'ERROR',
      packId: args.packId,
      title: 'Ashfall playability upstream report is missing',
      summary: `${entry.path} was not written by the tester-evidence intake.`,
      likelyFiles: [entry.path],
      suggestedFix: 'Fix the QA CLI intake command before running the Phase 8 gameplay reducer.',
    })
  }
  for (const entry of staleReports) {
    issues.push({
      code: 'ECHO-ASHFALL-PLAYABILITY-REPORT-STALE',
      severity: 'ERROR',
      packId: args.packId,
      title: 'Ashfall playability upstream report has placeholder time',
      summary: `${entry.path} still has generatedAt=1970-01-01T00:00:00Z.`,
      likelyFiles: [entry.path],
      suggestedFix: 'Run the intake through this wrapper or set ECHO_NATIVE_GENERATED_AT before invoking the QA CLI.',
    })
  }
  return {
    schemaVersion: 'echo.ashfall.native-playability-evidence-refresh.v1',
    generatedAt: new Date().toISOString(),
    generator: 'generate-ashfall-native-playability-evidence.mjs',
    packId: args.packId,
    status: blockingDiagnostics === 0 ? 'PASS' : 'FAILED',
    summary: {
      blockingDiagnostics,
      diagnosticCount: issues.length,
      dryRunOnly: false,
      commandExecuted: true,
      durationMs: result.durationMs,
    },
    issues,
    data: {
      command: args.command,
      commandArgs: args.commandArgs,
      exitCode: result.exitCode,
      signal: result.signal,
      durationMs: result.durationMs,
      stdoutTail: tail(result.stdout),
      stderrTail: tail(result.stderr),
      observedReports: reports,
      generatedAtProvidedToQaCli: result.generatedAt,
    },
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    process.stdout.write(usage())
    return
  }
  const result = await runCommand(args)
  const reports = await observedReports(args)
  const payload = report(args, result, reports)
  await writeJson(args.outPath, payload)
  process.stdout.write(`${JSON.stringify({
    ok: payload.status === 'PASS',
    status: payload.status,
    out: rel(args.root, args.outPath),
    exitCode: result.exitCode,
    reports: reports.map((entry) => ({
      path: entry.path,
      present: entry.present,
      status: entry.status,
      dryRunOnly: entry.dryRunOnly,
    })),
  }, null, 2)}\n`)
  if (payload.status !== 'PASS') process.exitCode = 1
}

await main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`)
  process.exitCode = 1
})
