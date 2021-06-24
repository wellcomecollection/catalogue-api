import path from 'path'
import chalk from 'chalk'

const info = (message: string) => {
  console.log(chalk.blue(message))
}

const error = (message: string) => {
  console.error(chalk.red(message))
  process.exit(1)
}

const p = (dir: string[]) => {
  return path.join(__dirname, ...dir)
}

const pretty = (json: unknown) => {
  return JSON.stringify(json, null, 2)
}

export { p, pretty, info, error }
