import chalk from 'chalk'
import path from 'path'
import prompts from 'prompts'
import yargs from 'yargs'

const info = (message: string) => {
  console.log(chalk.blue(message))
}

const code = (message: string) => {
  console.log(`\n  ${chalk.bold.cyan(message)}\n`)
}

const success = (message: string) => {
  console.log(chalk.green('✔ ' + message))
}

const error = (message: string) => {
  console.error(chalk.red('✘ ' + message))
  process.exit(1)
}

const p = (dir: string[]) => {
  return path.join(__dirname, ...dir)
}

const pretty = (json: unknown) => {
  return JSON.stringify(json, null, 2)
}

async function gatherArgs(options) {
  const args = yargs(process.argv)
    .options(options)
    .exitProcess(false)
    .parseSync()

  // if the values aren't in the provided args, prompt the user to supply them
  for (const [key, value] of Object.entries(options)) {
    if (!(key in args)) {
      args[key] = await prompts({
        type: 'select',
        name: 'value',
        message: key,
        choices: value['choices'].map((choice) => ({
          title: choice,
          value: choice,
        })),
      }).then(({ value }) => value)
    }
  }

  return args
}

export { p, pretty, info, error, code, success, gatherArgs }
