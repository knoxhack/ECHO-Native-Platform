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

function hasArtifact(node) {
  if (Array.isArray(node)) return node.some(hasArtifact)
  if (!node || typeof node !== 'object') return false
  if ((node.url || node.downloadUrl) && (node.sha256 || node.sha512) && (node.file || node.name || node.filename)) return true
  return Object.values(node).some(hasArtifact)
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

if (!entry) {
  errors.push(`Release Index product entry not found: ${productId}`)
} else {
  if (entry.kind !== expectedKind) errors.push(`Expected kind ${expectedKind}, got ${entry.kind}`)
  if (entry.sourceRepo !== expectedSourceRepo) errors.push(`Expected sourceRepo ${expectedSourceRepo}, got ${entry.sourceRepo}`)
  if (!Array.isArray(entry.compatibility) || !entry.compatibility.includes(expectedCompatibility)) {
    errors.push(`Expected compatibility ${expectedCompatibility}`)
  }
  if (entry.validation !== 'approved') warnings.push(`Product entry validation is ${entry.validation ?? 'missing'}, not approved.`)
  if (!hasArtifact(entry.artifacts)) warnings.push('Product entry has no indexed updater artifact.')
  if (strict && warnings.length) errors.push(...warnings)
}

console.log(JSON.stringify({
  status: errors.length ? 'failed' : 'passed',
  strict,
  productId,
  channelUrl,
  validation: entry?.validation ?? null,
  version: entry?.version ?? null,
  sourceRepo: entry?.sourceRepo ?? null,
  warnings,
  errors,
}, null, 2))

if (errors.length) process.exit(1)
