import path from 'path'

const p = (dir: string[]) => {
  return path.join(__dirname, ...dir)
}

const pretty = (json: unknown) => {
  return JSON.stringify(json, null, 2)
}

export { p, pretty }
