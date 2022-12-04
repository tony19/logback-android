import prompts from 'prompts'
import colors from 'picocolors'
import { getVersionChoices, isValidVersion, nextSnapshotVer } from './version'

export async function promptForNextVersion(currentVersion: string) {
  const versionChoices = getVersionChoices(currentVersion)
  const { releaseIndex }: { releaseIndex: number } = await prompts({
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
  const { version }: { version: string } = await prompts({
    type: 'text',
    name: 'version',
    message: 'Input custom version',
    initial: defaultValue,
  })
  return version
}

export async function promptToConfirmRelease(tag: string) {
  const { yes }: { yes: boolean } = await prompts({
    type: 'toggle',
    name: 'yes',
    message: `Release ${colors.yellow(tag)} ?`,
    active: 'yes',
    inactive: 'no',
  })
  return yes
}

export async function promptToPushHead() {
  const { yes }: { yes: boolean } = await prompts({
    type: 'toggle',
    name: 'yes',
    message: `Push changes ?`,
    active: 'yes',
    inactive: 'no',
  })
  return yes
}

export async function promptToBuildAndReleaseToSonatype() {
  const { yes }: { yes: boolean } = await prompts({
    type: 'toggle',
    name: 'yes',
    message: `Build and release to Sonatype ?`,
    active: 'yes',
    inactive: 'no',
  })
  return yes
}
