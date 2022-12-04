import { promises } from 'node:fs'

const { readFile, writeFile } = promises

export async function readKeysFromPropertiesFile<T extends Record<string, string | null>>(filePath: string, keyValuePairs: T): Promise<Record<string, string | null>> {
  let fileContents = ''
  try {
    fileContents = await readFile(filePath, { encoding: 'utf-8' })
  } catch (err: any) {
    if (err.code === 'ENOENT') {
      return keyValuePairs
    }
    throw err
  }

  const lines = fileContents.split('\n')
  const keyValuePairsCopy = { ...keyValuePairs }
  const requiredKeys = Object.keys(keyValuePairsCopy)

  for (const line of lines) {
    // ignore comments
    if (line.trim().startsWith('#')) {
      continue
    }
    const [key, value] = line.split('=', 2)
    // ignore line if version key is not present
    if (!requiredKeys.includes(key.trim())) {
      continue
    }

    const cleanKey = key.trim()

    // get value without quotes or trailing comments/spaces
    const cleanValue = value.split('#', 1)[0].trim()
      .replace(/^['"]/, '')
      .replace(/['"]$/, '')

    ;(keyValuePairsCopy as any)[cleanKey] = cleanValue

    if (requiredKeys.every((k) => keyValuePairsCopy[k])) {
      break
    }
  }

  return keyValuePairsCopy
}

export async function updatePropertiesFile(filePath: string, replacements: Record<string, string>): Promise<boolean> {
  const fileContents = await readFile(filePath, 'utf-8')
  const lines = fileContents.split('\n')
  const searchKeys = Object.keys(replacements)
  let hasChanges = false

  const newLines = lines.map((line) => {
    // preserve comments
    if (line.trim().startsWith('#')) {
      return line
    }
    const [key] = line.split('=', 1)
    // preserve line if the key is not present
    if (!searchKeys.includes(key.trim())) {
      return line
    }

    hasChanges = true

    const cleanKey = key.trim()
    const newValue = replacements[cleanKey]

    const newLine = line.replace(/=(\s*)([^#]*)(#?)/, (match: string, spacesAfterEqual: string, value: string, commentMarker: string) => {
      let replacement = `=${spacesAfterEqual}${newValue}`
      if (commentMarker) {
        // We don't support preserving spaces between the value and
        // the comment marker, so just put one space between them.
        replacement += ` ${commentMarker}`
      }
      return replacement
    })

    return newLine
  })

  if (hasChanges) {
    await writeFile(filePath, newLines.join('\n'))
  }
  return hasChanges
}