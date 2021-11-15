import chalk from 'chalk'
import path from 'path'

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
  console.error('✘ ' + chalk.red(message))
  process.exit(1)
}

const p = (dir: string[]) => {
  return path.join(__dirname, ...dir)
}

const pretty = (json: unknown) => {
  return JSON.stringify(json, null, 2)
}

const histogram = (arr: number[], binWidth: number = 250) => {
  var max = Math.max(...arr)
  var min = Math.min(...arr)
  var numberOfBins = Math.ceil((max - min + 1) / binWidth)
  var counts = Array(numberOfBins).fill(0)
  arr.forEach((x) => counts[Math.floor((x - min) / binWidth)]++)

  const hist = Object.fromEntries(
    counts.map((x, i) => {
      const binBottom = i * binWidth
      const binTop = (i + 1) * binWidth
      return [`${binBottom}-${binTop}`, x]
    })
  )
  return hist
}

export { p, pretty, info, error, code, success, histogram }
