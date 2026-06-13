#!/usr/bin/env node
import assert from 'node:assert/strict'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

const repoRoot = process.cwd()
const script = path.join(repoRoot, 'scripts', 'generate-ashfall-native-code-gate.mjs')

async function writeFile(root, relPath, value) {
  const filePath = path.join(root, relPath)
  await fs.mkdir(path.dirname(filePath), { recursive: true })
  await fs.writeFile(filePath, value, 'utf8')
}

function run(root, fakeScript) {
  return runCommand(root, process.execPath, [fakeScript])
}

function runCommand(root, command, commandArgs = []) {
  const args = [
    script,
    '--root',
    root,
    '--command',
    command,
  ]
  for (const commandArg of commandArgs) {
    args.push('--arg', commandArg)
  }
  return spawnSync(process.execPath, args, {
    cwd: repoRoot,
    encoding: 'utf8',
    windowsHide: true,
  })
}

const tmp = await fs.mkdtemp(path.join(os.tmpdir(), 'echo-ashfall-native-code-gate-test-'))
try {
  const passRoot = path.join(tmp, 'pass')
  const passScript = path.join(passRoot, 'fake-pass.mjs')
  await writeFile(passRoot, 'fake-pass.mjs', "console.log('gradle check passed')\n")
  const pass = run(passRoot, passScript)
  assert.equal(pass.status, 0, `${pass.stdout}\n${pass.stderr}`)
  const passReport = JSON.parse(await fs.readFile(path.join(passRoot, 'reports/echo-native/ashfall/native-code-gate.json'), 'utf8'))
  assert.equal(passReport.schemaVersion, 'echo.ashfall.native-code-gate.v1')
  assert.equal(passReport.status, 'PASS')
  assert.equal(passReport.summary.dryRunOnly, false)
  assert.equal(passReport.summary.commandExecuted, true)
  assert.equal(passReport.summary.evidenceRefreshAttempted, true)
  assert.equal(passReport.data.gradleCheckPassed, true)
  assert.equal(passReport.data.exitCode, 0)
  assert.equal(passReport.data.evidenceRefreshes.length, 3)
  assert.equal(passReport.data.evidenceRefreshes.every((entry) => entry.skipped === true), true)
  assert.match(passReport.data.stdoutTail, /gradle check passed/u)

  const failRoot = path.join(tmp, 'fail')
  const failScript = path.join(failRoot, 'fake-fail.mjs')
  await writeFile(failRoot, 'fake-fail.mjs', "console.error('gradle check failed')\nprocess.exit(7)\n")
  const fail = run(failRoot, failScript)
  assert.equal(fail.status, 1)
  const failReport = JSON.parse(await fs.readFile(path.join(failRoot, 'reports/echo-native/ashfall/native-code-gate.json'), 'utf8'))
  assert.equal(failReport.status, 'FAILED')
  assert.equal(failReport.summary.dryRunOnly, false)
  assert.equal(failReport.data.gradleCheckPassed, false)
  assert.equal(failReport.data.exitCode, 7)
  assert.equal(failReport.data.evidenceRefreshes.length, 3)
  assert.equal(failReport.data.evidenceRefreshes.every((entry) => entry.skipped === true), true)
  assert.match(failReport.data.stderrTail, /gradle check failed/u)

  if (process.platform === 'win32') {
    const batRoot = path.join(tmp, 'bat')
    const batPath = path.join(batRoot, 'fake-gradle.bat')
    await writeFile(batRoot, 'fake-gradle.bat', '@echo off\r\necho gradle bat %*\r\nexit /b 0\r\n')
    const bat = runCommand(batRoot, batPath, ['check', '--console=plain'])
    assert.equal(bat.status, 0, `${bat.stdout}\n${bat.stderr}`)
    assert.doesNotMatch(`${bat.stdout}\n${bat.stderr}`, /DEP0190/u)
    const batReport = JSON.parse(await fs.readFile(path.join(batRoot, 'reports/echo-native/ashfall/native-code-gate.json'), 'utf8'))
    assert.equal(batReport.status, 'PASS')
    assert.equal(batReport.data.command, batPath)
    assert.deepEqual(batReport.data.commandArgs, ['check', '--console=plain'])
    assert.match(batReport.data.stdoutTail, /gradle bat check --console=plain/u)
  }
} finally {
  await fs.rm(tmp, { recursive: true, force: true })
}

console.log('Ashfall Native code gate generator fixtures passed.')
