import { readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import semver, { type ReleaseType } from 'semver'
export { valid as isValidVersion } from 'semver'
import { run } from './run'
import {
  readKeysFromPropertiesFile,
  updatePropertiesFile,
} from './properties'
import { info } from './log'

export interface VersionInfo {
  slf4jVersion: string
  version: string
  VERSION_NAME: string
}

export interface VersionChoice {
  title: string
  version: string
  nextVersion: string
}

export async function getVersionInfoFromGradleProperties(gradleFilePath: string) {
  return readKeysFromPropertiesFile(gradleFilePath, {
    slf4jVersion: '',
    version: '',
    VERSION_NAME: '',
  })
}

export function cleanVer(version: string) {
  return semver.clean(version) || version
}

export function nextVer(version: string, type: ReleaseType = 'patch') {
  return semver.inc(version, type)!
}

export function nextSnapshotVer(version: string, type: ReleaseType = 'patch') {
  return nextVer(version, type) + '-SNAPSHOT'
}

export function getVersionChoices(currentVersion: string): VersionChoice[] {
  let versionChoices: VersionChoice[] = [
    {
      title: 'next',
      version: cleanVer(currentVersion),
      nextVersion: nextSnapshotVer(cleanVer(currentVersion), 'patch'),
    },
    {
      title: 'patch',
      version: nextVer(currentVersion, 'patch'),
      nextVersion: nextSnapshotVer(nextVer(currentVersion, 'patch'), 'patch'),
    },
    {
      title: 'minor',
      version: nextVer(currentVersion, 'minor'),
      nextVersion: nextSnapshotVer(nextVer(currentVersion, 'minor'), 'patch'),
    },
    {
      title: 'major',
      version: nextVer(currentVersion, 'major'),
      nextVersion: nextSnapshotVer(nextVer(currentVersion, 'major'), 'patch'),
    },
    {
      title: 'custom',
      version: 'custom',
      nextVersion: '',
    },
  ]

  versionChoices = versionChoices.map((i) => {
    i.title = `${i.title} (${i.version})`
    return i
  })

  return versionChoices
}

export function updateVersionInReadme(readmeFilePath: string, replacements: [RegExp, string][]): boolean {
  info(`updating ${readmeFilePath}`)
  const readmeFile = readFileSync(readmeFilePath, 'utf-8')
  const newFileContents = replacements.reduce((acc, [regex, replacement]) => {
    return acc.replace(regex, replacement)
  }, readmeFile)

  let hasChanges = false
  if (readmeFile !== newFileContents) {
    hasChanges = true
    writeFileSync(readmeFilePath, newFileContents)
  }
  return hasChanges
}

export async function updateVersionInGradleProperties(gradleFilePath: string, version: string) {
  info(`updating ${gradleFilePath}`)
  return updatePropertiesFile(gradleFilePath, {
    VERSION_NAME: version,
    version,
  })
}

export async function getActiveVersion(npmName: string): Promise<string> {
  return (await run('npm', ['info', npmName, 'version'], { stdio: 'pipe' }))
    .stdout
}

export async function generateChangelog(changelogFilePath: string) {
  // get base filename of changelog
  const changelogFilename = path.basename(changelogFilePath)

  // get parent directory of changelog
  const changelogDir = path.dirname(changelogFilePath)

  const changelogArgs = [
    'conventional-changelog',
    '-p',
    'angular',
    '-i',
    changelogFilename,
    '-s',
    '--commit-path',
    changelogDir,
  ]

  await run('npx', changelogArgs, { cwd: changelogDir })
}
