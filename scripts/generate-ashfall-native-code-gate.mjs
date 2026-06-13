#!/usr/bin/env node
import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import process from 'node:process'

const DEFAULT_REPORT = 'reports/echo-native/ashfall/native-code-gate.json'
const DEFAULT_COMMAND = process.platform === 'win32' ? '.\\gradlew.bat' : './gradlew'
const DEFAULT_ARGS = ['check', '--console=plain', '--no-daemon']
const RELEASE_EVIDENCE_REFRESH_SCRIPTS = [
  'scripts/generate-ashfall-native-playability-evidence.mjs',
  'scripts/generate-ashfall-native-public-beta-evidence.mjs',
  'scripts/generate-ashfall-gameplay-qa-evidence.mjs',
]
const OUTPUT_TAIL_LIMIT = 12000

function usage() {
  return `Usage: node scripts/generate-ashfall-native-code-gate.mjs [options]

Runs the Native Platform Gradle code gate and writes the Phase 5 Ashfall release
evidence report consumed by ECHO-Release-Index.

Options:
  --root <dir>       ECHO-Native-Platform root. Default: current directory.
  --out <path>       Report path. Default: ${DEFAULT_REPORT}.
  --command <cmd>    Command to run. Default: ${DEFAULT_COMMAND}.
  --arg <value>      Add one command argument. Replaces the default Gradle args on first use.
  --skip-evidence-refresh
                     Do not rerun Phase 7/8 evidence reducers after Gradle check.
  --help             Print this help text.
`
}

function parseArgs(argv) {
  const args = {
    root: process.cwd(),
    out: DEFAULT_REPORT,
    command: DEFAULT_COMMAND,
    commandArgs: [...DEFAULT_ARGS],
    customArgs: false,
  }
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index]
    if (arg === '--root') args.root = path.resolve(argv[++index])
    else if (arg === '--out') args.out = argv[++index]
    else if (arg === '--command') args.command = argv[++index]
    else if (arg === '--arg') {
      if (!args.customArgs) {
        args.commandArgs = []
        args.customArgs = true
      }
      args.commandArgs.push(argv[++index])
    } else if (arg === '--skip-evidence-refresh') args.skipEvidenceRefresh = true
    else if (arg === '--help' || arg === '-h') args.help = true
    else throw new Error(`Unknown argument: ${arg}`)
  }
  args.outPath = path.isAbsolute(args.out) ? args.out : path.join(args.root, args.out)
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

async function writeJson(filePath, value) {
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function exists(filePath) {
  try {
    const stat = await fs.stat(filePath)
    return stat.isFile()
  } catch {
    return false
  }
}

function isWindowsBatchCommand(command) {
  return process.platform === 'win32' && /\.(?:bat|cmd)$/iu.test(path.basename(command))
}

function commandInvocation(args) {
  if (!isWindowsBatchCommand(args.command)) {
    return {
      command: args.command,
      commandArgs: args.commandArgs,
    }
  }
  return {
    command: process.env.ComSpec || 'cmd.exe',
    commandArgs: ['/d', '/s', '/c', args.command, ...args.commandArgs],
  }
}

function runCommand(args) {
  return new Promise((resolve) => {
    const startedAt = Date.now()
    const invocation = commandInvocation(args)
    const child = spawn(invocation.command, invocation.commandArgs, {
      cwd: args.root,
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
        durationMs: Date.now() - startedAt,
        stdout,
        stderr,
        spawnError: null,
      })
    })
  })
}

async function runEvidenceRefresh(args, scriptRelPath) {
  const scriptPath = path.join(args.root, scriptRelPath)
  if (!(await exists(scriptPath))) {
    return {
      script: scriptRelPath,
      skipped: true,
      reason: 'script-not-found',
    }
  }

  return new Promise((resolve) => {
    const startedAt = Date.now()
    const child = spawn(process.execPath, [scriptPath, '--root', args.root], {
      cwd: args.root,
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
        script: scriptRelPath,
        skipped: false,
        exitCode: null,
        signal: null,
        durationMs: Date.now() - startedAt,
        stdoutTail: tail(stdout),
        stderrTail: tail(`${stderr}${stderr ? '\n' : ''}${error.message}`),
        spawnError: error.message,
      })
    })
    child.on('close', (exitCode, signal) => {
      resolve({
        script: scriptRelPath,
        skipped: false,
        exitCode,
        signal,
        durationMs: Date.now() - startedAt,
        stdoutTail: tail(stdout),
        stderrTail: tail(stderr),
        spawnError: null,
      })
    })
  })
}

async function refreshReleaseEvidence(args) {
  if (args.skipEvidenceRefresh) return []
  const refreshes = []
  for (const scriptRelPath of RELEASE_EVIDENCE_REFRESH_SCRIPTS) {
    refreshes.push(await runEvidenceRefresh(args, scriptRelPath))
  }
  return refreshes
}

function report(args, result, evidenceRefreshes) {
  const passed = result.exitCode === 0 && result.spawnError === null
  const issue = passed ? [] : [{
    code: 'ECHO-ASHFALL-NATIVE-CODE-GATE-FAILED',
    severity: 'ERROR',
    packId: 'ashfall',
    title: 'Native Platform code gate failed',
    summary: result.spawnError
      ? `Failed to start ${args.command}: ${result.spawnError}`
      : `${args.command} ${args.commandArgs.join(' ')} exited with ${result.exitCode}${result.signal ? ` signal ${result.signal}` : ''}.`,
    likelyFiles: ['build.gradle', 'settings.gradle', 'echo-native-*'],
    suggestedFix: 'Fix the Native Platform Gradle check failure and rerun this script.',
  }]
  return {
    schemaVersion: 'echo.ashfall.native-code-gate.v1',
    generatedAt: new Date().toISOString(),
    generator: 'generate-ashfall-native-code-gate.mjs',
    packId: 'ashfall',
    status: passed ? 'PASS' : 'FAILED',
    summary: {
      blockingDiagnostics: issue.length,
      diagnosticCount: issue.length,
      dryRunOnly: false,
      commandExecuted: true,
      durationMs: result.durationMs,
      evidenceRefreshAttempted: !args.skipEvidenceRefresh,
    },
    issues: issue,
    data: {
      gradleCheckPassed: passed,
      commandExecuted: true,
      command: args.command,
      commandArgs: args.commandArgs,
      exitCode: result.exitCode,
      signal: result.signal,
      durationMs: result.durationMs,
      stdoutTail: tail(result.stdout),
      stderrTail: tail(result.stderr),
      evidenceRefreshes,
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
  const evidenceRefreshes = await refreshReleaseEvidence(args)
  const payload = report(args, result, evidenceRefreshes)
  await writeJson(args.outPath, payload)
  process.stdout.write(`${JSON.stringify({
    ok: payload.status === 'PASS',
    status: payload.status,
    out: rel(args.root, args.outPath),
    exitCode: result.exitCode,
    durationMs: result.durationMs,
    evidenceRefreshes: evidenceRefreshes.map((entry) => ({
      script: entry.script,
      skipped: entry.skipped === true,
      exitCode: entry.exitCode ?? null,
    })),
  }, null, 2)}\n`)
  if (payload.status !== 'PASS') process.exitCode = 1
}

await main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`)
  process.exitCode = 1
})
