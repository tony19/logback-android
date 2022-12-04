import { run } from './run'

export async function checkForJava(minJavaVersion = 11) {
  let stdout = ''
  let stderr = ''
  try {
    const res = await run('java', ['-version'], { stdio: 'pipe' })
    stdout = res.stdout
    stderr = res.stderr
  } catch (err) {
    throw new Error(`Cannot run \`java -version\`. Is Java ${minJavaVersion} installed on path?`)
  }

  const javaVersion = stdout.match(/version "(.*)"/)?.[1] || stderr.match(/version "(.*)"/)?.at(1)
  if (!javaVersion) {
    throw new Error('No Java version found in output of `java -version`')
  }
  let [major, minor] = javaVersion.split('.').map(Number)
  if (major === 1) major = minor
  if (major < minJavaVersion) {
    throw new Error(`Java version ${javaVersion} is incompatible. Install Java ${minJavaVersion} or later.`)
  }
}
