const removeEmpty = <T extends Record<string, unknown>>(obj: T): T => {
  return JSON.parse(JSON.stringify(obj))
}

export { removeEmpty }
