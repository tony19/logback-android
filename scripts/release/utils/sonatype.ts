import { readKeysFromPropertiesFile } from './properties'
import { runIfNotDry } from './run'

export async function buildAndReleaseToSonatype({ cwd } : { cwd?: string }) {
  return runIfNotDry('./gradlew', [
    '--no-configuration-cache',

    // tasks from io.github.gradle-nexus.publish-plugin
    'publishToSonatype',
    'closeAndReleaseSonatypeStagingRepository',
  ], { stdio: 'pipe', cwd })
}

export async function checkLocalPropertiesForRequiredKeys(localPropsFilePath: string) {
  // https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/#setting-up-publication-in-your-project
  let requiredKeys: Record<string, string | null> = {
    'signing.keyId': null, // PUBKEY_LAST8 from `gpg --list-keys` (last 8 digits of pub key)
    'signing.password': null, // password for key (can be empty if key has no password)
    'signing.key': null, // output from `gpg --export-secret-keys <PUBKEY_LAST8> | base64`
    ossrhUsername: null, // Sonatype JIRA username
    ossrhPassword: null, // Sonatype JIRA password
    sonatypeStagingProfileId: null, // profile ID from https://oss.sonatype.org/#stagingProfiles (login to https://oss.sonatype.org/, open staging profiles, select target profile, and copy profile ID from path in address bar)
  }

  requiredKeys = await readKeysFromPropertiesFile(localPropsFilePath, requiredKeys)

  const missingKeys = Object.keys(requiredKeys).filter((key) => {
    const value = requiredKeys[key]
    // if value is null, it was not found in the file
    if (value === null) return true

    // ok for password to be empty
    return !value && key !== 'signing.password'
  })

  if (missingKeys.length) {
    const helpMessages: Record<string, string> = {
      'signing.keyId': 'PUBKEY_LAST8 from \`gpg --list-keys\` (last 8 digits of pub key)',
      'signing.password': 'password for key (can be empty if key has no password)',
      'signing.key': 'output from \`gpg --export-secret-keys <PUBKEY_LAST8> | base64\`',
      ossrhUsername: 'Sonatype JIRA username',
      ossrhPassword: 'Sonatype JIRA password',
      sonatypeStagingProfileId: 'profile ID from https://oss.sonatype.org/#stagingProfiles (select profile, and copy profile ID from hash in address bar)',
    }
    const errorMessage = `Missing required keys in ${localPropsFilePath}:

${missingKeys.map((key) => `${key.padEnd(17)} # ${helpMessages[key]}` ).join('\n')}`
    throw new Error(errorMessage)
  }
}
