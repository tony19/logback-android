import colors from 'picocolors'
import type { Options as ExecaOptions, ExecaReturnValue } from 'execa'
import { execa } from 'execa'
import minimist from 'minimist'
import { log, debug } from './log'

export const args = minimist(process.argv.slice(2))

export const isDryRun = !!args.dry

if (isDryRun) {
  log(colors.inverse(colors.yellow(' DRY RUN ')))
  log()
}

export async function run(
  bin: string,
  args: string[],
  opts: ExecaOptions<string> = {}
): Promise<ExecaReturnValue<string>> {
  return execa(bin, args, { stdio: 'inherit', ...opts })
}

export async function dryRun(
  bin: string,
  args: string[],
  opts?: ExecaOptions<string>
): Promise<{ stdout: string }> {
  // wrap spaced args in quotes
  const formattedArgs = args.map((arg) => {
    if (/\s/.test(arg) && !/^['"]/.test(arg)) {
      return `"${arg}"`
    } else {
      return arg
    }
  })

  const stdout = `[dryrun] $ ${bin} ${formattedArgs.join(' ')}`
  debug(stdout, opts || '')
  return { stdout }
}

export const runIfNotDry = isDryRun ? dryRun : run
