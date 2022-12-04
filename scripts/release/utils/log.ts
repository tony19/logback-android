import colors from 'picocolors'

export const log = (message = '', ...optionalParams: any[]) =>
  console.log(colors.white(message), ...optionalParams)

export const info = (message = '', ...optionalParams: any[]) =>
  console.info(colors.white(message), ...optionalParams)

export const error = (message = '', ...optionalParams: any[]) =>
  console.error(colors.red(message), ...optionalParams)

export const warn = (message = '', ...optionalParams: any[]) =>
  console.warn(colors.yellow(message), ...optionalParams)

export const verbose = (message = '', ...optionalParams: any[]) =>
  console.debug(colors.dim(message), ...optionalParams)

export const debug = (message = '', ...optionalParams: any[]) =>
  console.debug(colors.dim(message), ...optionalParams)

export const say = (message = '', ...optionalParams: any[]) =>
  console.log(colors.cyan(message), ...optionalParams)

export const success = (message = '', ...optionalParams: any[]) =>
  console.log(colors.green(message), ...optionalParams)
