import path from 'node:path'
import {
  promptForNextVersion,
  promptToConfirmRelease,
  promptToPushHead,
  promptToBuildAndReleaseToSonatype,
} from './utils/prompts'
import {
  getVersionInfoFromGradleProperties,
  updateVersionInGradleProperties,
  updateVersionInReadme,
  nextSnapshotVer,
  generateChangelog,
} from './utils/version'
import {
  logRecentCommits,
  tagHead,
  commitChangedFiles,
  pushHead,
} from './utils/git'
import {
  buildAndReleaseToSonatype,
  checkLocalPropertiesForRequiredKeys,
} from './utils/sonatype'
import {
  error,
  log,
  say,
  success,
} from './utils/log'
import { checkForJava } from './utils/java'

async function main(): Promise<void> {
  const cwd = path.resolve(__dirname, '../../')
  const gradleFilePath = `${cwd}/gradle.properties`
  const readmeFilePath = `${cwd}/README.md`
  const changelogFilePath = `${cwd}/CHANGELOG.md`
  const tagPrefix = 'v_'

  await checkForJava()
  await checkLocalPropertiesForRequiredKeys(`${cwd}/local.properties`)
  await logRecentCommits(tagPrefix)

  const { version: currentVersion, slf4jVersion: configuredSlf4jVersion } = await getVersionInfoFromGradleProperties(gradleFilePath)
  const slf4jVersion = configuredSlf4jVersion || ''

  if (!currentVersion) {
    throw new Error('no version found in gradle.properties')
  }
  if (!slf4jVersion) {
    throw new Error('no slf4jVersion found in gradle.properties')
  }

  const { version: targetVersion, nextVersion } = await promptForNextVersion(currentVersion)
  if (!targetVersion) {
    // exited out of prompts with CTRL+C
    return
  }
  if (!nextVersion) {
    throw new Error('no nextVersion found')
  }

  const tag = `${tagPrefix}${targetVersion}`

  if (!await promptToConfirmRelease(tag)) {
    return
  }

  say(`\nUpdating release version to ${targetVersion} ...`)
  await updateVersionInGradleProperties(gradleFilePath, targetVersion)
  await updateVersionInReadme(readmeFilePath, [
    [/logback-android:\d+\.\d+\.\d+(?!-SNAPSHOT)/g, `logback-android:${targetVersion}`],
    [/logback-android:\d+\.\d+\.\d+-SNAPSHOT/g, `logback-android:${nextVersion}`],
    [/logback-android-\d+\.\d+\.\d+/g, `logback-android-${targetVersion}`],
    [/slf4j-api:\d+\.\d+\.\d+(-SNAPSHOT)?/g, `slf4j-api:${slf4jVersion}`],
  ])

  if (await promptToBuildAndReleaseToSonatype()) {
    say('\nBuilding and releasing to Sonatype ...')
    await buildAndReleaseToSonatype({ cwd })
  }
  // say('\nGenerating changelog ...')
  // await generateChangelog(changelogFilePath)

  await commitChangedFiles({ cwd, message: `chore: release ${tag}` }, readmeFilePath, gradleFilePath, changelogFilePath)
  await tagHead(tag, `logback-android-${targetVersion}`)

  say(`\nUpdating snapshot version to ${nextVersion} ...`)
  await updateVersionInGradleProperties(gradleFilePath, nextVersion)
//   await updateVersionInReadme(readmeFilePath, [
//     [/logback-android:\d+\.\d+\.\d+(?!-SNAPSHOT)/g, `logback-android:${nextVersion}`],
//     [/logback-android:\d+\.\d+\.\d+-SNAPSHOT/g, `logback-android:${nextSnapshotVer(nextVersion)}`],
//   ])

  await commitChangedFiles({ cwd, message: `chore: update SNAPSHOT version` }, readmeFilePath, gradleFilePath)

  // visual spacer
  log()

  if (await promptToPushHead()) {
    say('\nPushing changes ...')
    await pushHead(tag)
  }

  success('\n✅ Done!')
}

main().catch((err) => {
  error(err.message)
  // debug(err.stack)
  process.exit(1)
})