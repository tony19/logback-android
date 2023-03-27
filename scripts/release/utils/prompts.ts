import prompts, { type PromptObject } from 'prompts'
import colors from 'picocolors'
import { getVersionChoices, isValidVersion, nextSnapshotVer } from './version'

async function promptButExitIfCancelled<T>(args: PromptObject): Promise<T> {
  const res = await prompts(args)
  if (!res || !Object.keys(res).length) {
    throw new Error('aborted')
  }
  return res as T
}

export async function promptForNextVersion(currentVersion: string) {
  const versionChoices = getVersionChoices(currentVersion)
  const { releaseIndex } = await promptButExitIfCancelled<{ releaseIndex: number }>({
    type: 'select',
    name: 'releaseIndex',
    message: 'Select release type',
    choices: versionChoices,
  })
  const release = versionChoices[releaseIndex]

  let targetVersion = ''
  let nextVersion = ''

  if (!release) {
    // exited out of prompts with CTRL+C
    return { version: targetVersion, nextVersion }

  } else if (release.version === 'custom') {
    targetVersion = await promptForCustomVersion(currentVersion)
    nextVersion = nextSnapshotVer(targetVersion)

  } else {
    targetVersion = release.version
    nextVersion = release.nextVersion
  }

  if (!isValidVersion(targetVersion)) {
    throw new Error(`invalid target version: ${targetVersion}`)
  }

  return { version: targetVersion, nextVersion }
}

export async function promptForCustomVersion(defaultValue: string) {
  const { version } = await promptButExitIfCancelled<{ version: string }>({
    type: 'text',
    name: 'version',
    message: 'Input custom version',
    initial: defaultValue,
  })
  return version
}

export async function promptToConfirmRelease(tag: string) {
  const { yes } = await promptButExitIfCancelled<{ yes: boolean }>({
    type: 'toggle',
    name: 'yes',
    message: `Release ${colors.yellow(tag)} ?`,
    active: 'yes',
    inactive: 'no',
  })
  return yes
}

export async function promptToPushHead() {
  const { yes } = await promptButExitIfCancelled<{ yes: boolean }>({
    type: 'toggle',
    name: 'yes',
    message: `Push changes ?`,
    active: 'yes',
    inactive: 'no',
  })
  return yes
}

export async function promptToBuildAndReleaseToSonatype() {
  const { yes } = await promptButExitIfCancelled<{ yes: boolean }>({
    type: 'toggle',
    name: 'yes',
    message: `Build and release to Sonatype ?`,
    active: 'yes',
    inactive: 'no',
  })
  return yes
}
