import https from 'node:https'
import fs from 'node:fs/promises'
import path from 'node:path'

const productId = process.env.ECHO_RELEASE_INDEX_PRODUCT_ID || 'echo-native-platform'
const expectedSourceRepo = process.env.ECHO_RELEASE_INDEX_SOURCE_REPO || 'knoxhack/ECHO-Native-Platform'
const expectedKind = process.env.ECHO_RELEASE_INDEX_KIND || 'runtime'
const expectedCompatibility = process.env.ECHO_RELEASE_INDEX_COMPATIBILITY || 'ashfall-native-edition'
const channelUrl =
  process.env.ECHO_RELEASE_INDEX_CHANNEL_URL ||
  'https://raw.githubusercontent.com/knoxhack/ECHO-Release-Index/main/channels/alpha/launcher-channel.json'
const localIndexRoot = process.env.ECHO_RELEASE_INDEX_ROOT
const strict = process.argv.includes('--strict')
const checkUrls = process.argv.includes('--check-urls') || process.env.ECHO_RELEASE_INDEX_CHECK_URLS === 'true'
const requiredArtifactRoles = (process.env.ECHO_RELEASE_INDEX_REQUIRED_ARTIFACT_ROLES || 'archive')
  .split(',')
  .map((role) => role.trim())
  .filter(Boolean)

function readHttpsJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, { headers: { accept: 'application/json', 'user-agent': 'echo-native-platform' } }, (response) => {
        const statusCode = response.statusCode ?? 0
        if (statusCode < 200 || statusCode >= 300) {
          response.resume()
          reject(new Error(`Release Index request failed with HTTP ${statusCode}: ${url}`))
          return
        }
        const chunks = []
        response.on('data', (chunk) => chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk)))
        response.on('end', () => {
          try {
            resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')))
          } catch (error) {
            reject(error)
          }
        })
      })
      .on('error', reject)
  })
}

function checkArtifactUrl(url) {
  return new Promise((resolve) => {
    const request = https.request(url, { method: 'HEAD', headers: { 'user-agent': 'echo-native-platform' } }, (response) => {
      const statusCode = response.statusCode ?? 0
      response.resume()
      resolve({
        url,
        statusCode,
        ok: statusCode >= 200 && statusCode < 400,
      })
    })
    request.setTimeout(15000, () => {
      request.destroy()
      resolve({ url, statusCode: 0, ok: false, error: 'timeout' })
    })
    request.on('error', (error) => resolve({ url, statusCode: 0, ok: false, error: error.message }))
    request.end()
  })
}

function artifactRecords(node, role = 'asset') {
  const records = []
  const visit = (value, valueRole) => {
    if (Array.isArray(value)) {
      value.forEach((item) => visit(item, valueRole))
      return
    }
    if (!value || typeof value !== 'object') return
    const name = value.file ?? value.name ?? value.filename
    const url = value.url ?? value.downloadUrl
    const sha256 = value.sha256
    if (name && url && sha256) {
      records.push({
        role: valueRole,
        name: String(name),
        url: String(url),
        sha256: String(sha256),
      })
    }
    Object.entries(value).forEach(([key, child]) => visit(child, key))
  }
  visit(node, role)
  return records
}

function isTrustedArtifact(artifact) {
  return Boolean(
    artifact &&
      /^https:\/\/github\.com\/[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+\/releases\/download\//.test(artifact.url) &&
      /^[a-f0-9]{64}$/i.test(artifact.sha256)
  )
}

function missingArtifactRoles(entry) {
  const records = artifactRecords(entry.artifacts)
  return requiredArtifactRoles.filter((role) => !records.some((artifact) => artifact.role === role && isTrustedArtifact(artifact)))
}

async function validateArtifactUrls(entry) {
  if (!checkUrls || !entry) return []
  const records = artifactRecords(entry.artifacts)
  const failures = []
  for (const artifact of records) {
    const result = await checkArtifactUrl(artifact.url)
    if (!result.ok) {
      failures.push(`${artifact.role} artifact ${artifact.name} URL returned ${result.statusCode || result.error}: ${artifact.url}`)
    }
  }
  return failures
}

async function findProductEntry() {
  if (localIndexRoot) {
    const entryPath = path.join(localIndexRoot, 'products', `${productId.replace(/^echo-/, '')}.json`)
    return JSON.parse(await fs.readFile(entryPath, 'utf8'))
  }

  const channel = await readHttpsJson(channelUrl)
  const catalogUrls = Array.isArray(channel.catalogUrls)
    ? channel.catalogUrls
    : Object.values(channel.catalogUrls ?? {}).flat()
  for (const catalogUrl of catalogUrls) {
    const entry = await readHttpsJson(catalogUrl)
    if (entry.id === productId) return entry
  }
  return null
}

const entry = await findProductEntry()
const errors = []
const warnings = []
const urlErrors = await validateArtifactUrls(entry)
errors.push(...urlErrors)

if (!entry) {
  errors.push(`Release Index product entry not found: ${productId}`)
} else {
  if (entry.kind !== expectedKind) errors.push(`Expected kind ${expectedKind}, got ${entry.kind}`)
  if (entry.sourceRepo !== expectedSourceRepo) errors.push(`Expected sourceRepo ${expectedSourceRepo}, got ${entry.sourceRepo}`)
  if (!Array.isArray(entry.compatibility) || !entry.compatibility.includes(expectedCompatibility)) {
    errors.push(`Expected compatibility ${expectedCompatibility}`)
  }
  if (entry.validation !== 'approved') warnings.push(`Product entry validation is ${entry.validation ?? 'missing'}, not approved.`)
  const missingRoles = missingArtifactRoles(entry)
  if (missingRoles.length) {
    warnings.push(`Product entry has no exact indexed artifact for role(s): ${missingRoles.join(', ')}.`)
  }
  if (strict && warnings.length) errors.push(...warnings)
}

console.log(JSON.stringify({
  status: errors.length ? 'failed' : 'passed',
  strict,
  productId,
  channelUrl,
  checkUrls,
  validation: entry?.validation ?? null,
  version: entry?.version ?? null,
  sourceRepo: entry?.sourceRepo ?? null,
  requiredArtifactRoles,
  warnings,
  errors,
}, null, 2))

if (errors.length) process.exit(1)
