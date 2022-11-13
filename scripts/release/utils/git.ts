import path from 'node:path'
import colors from 'picocolors'
import { info, debug, log, warn } from './log'
import { run, runIfNotDry } from './run'

export async function tagHead(tag: string, message: string) {
  info(`tagging as ${colors.bgCyan(tag)} "${message}"`)

  const { stdout: tagExists } = await run('git', ['ls-remote', 'origin', tag])
  if (tagExists) {
    warn(`tag ${colors.bgCyan(tag)} already exists on remote`)
  }
  return runIfNotDry('git', ['tag', '-af', tag, '-m', message])
}

export async function getChangedFilenames({ cwd }: { cwd?: string } ) {
  const { stdout } = await run('git', ['status', '--short', '--untracked-files'], { stdio: 'pipe', cwd })
  return stdout.split(/\n/g).filter(line => /^\s*[M|?]/.test(line)).map(line => line.replace(/^\s*[M/?]+\s+/g, '')) || []
}

export interface CommitOptions {
  cwd?: string
  message?: string
}
export async function commitChangedFiles({ message, cwd }: CommitOptions, ...filenames: (string | RegExp)[]) {
  const filterForFilenames = (name: string) => {
    return filenames.some(pattern => {
      const changed = (pattern instanceof RegExp)
        ? pattern.test(name)
        : path.resolve(pattern) === path.resolve(cwd!, name)
      return changed
    })
  }

  const changedFiles = (await getChangedFilenames({ cwd })).filter(filterForFilenames)

  if (changedFiles.length) {
    debug('committing changes')
    await runIfNotDry('git', ['add', ...changedFiles], { cwd })

    const gitCommitOptions = ['commit', '-m', message || '']
    if (!message?.trim()) {
      gitCommitOptions.push('--allow-empty-message')
    }

    await runIfNotDry('git', gitCommitOptions, { cwd })
  } else {
    debug('no changes to commit')
    return
  }
}

export async function getLatestTag(tagPrefix?: string): Promise<string> {
  const tags = (await run('git', ['tag'], { stdio: 'pipe' })).stdout
    .split(/\n/)
    .filter(Boolean)

  return tags
    .filter(tag => !tagPrefix || tag.startsWith(tagPrefix))
    .sort()
    .reverse()[0]
}

export async function pushHead(tag: string) {
  await runIfNotDry('git', ['push', '--atomic', 'origin', 'HEAD', tag])
}

export async function logRecentCommits(tagPrefix?: string): Promise<void> {
  const tag = await getLatestTag(tagPrefix)
  if (!tag) return
  const sha = await run('git', ['rev-list', '-n', '1', tag], {
    stdio: 'pipe'
  }).then((res) => res.stdout.trim())
  log(
    colors.bold(
      `\n${colors.blue(`i`)} Commits since ${colors.green(tag)} ${colors.gray(`(${sha.slice(0, 5)})`)}`
    )
  )
  await run(
    'git',
    [
      '--no-pager',
      'log',
      `${sha}^..HEAD`,
      '--oneline',
    ],
    { stdio: 'inherit' }
  )
  log()
}