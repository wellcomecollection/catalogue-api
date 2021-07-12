const removeEmpty = <T extends Record<string, unknown>>(obj: T): T => {
  return JSON.parse(JSON.stringify(obj))
}

const passEmoji = (pass: boolean): string => {
  switch (pass) {
    case true:
      return '✔'
    case false:
      return '✘'
    default:
      return '?'
  }
}

export { removeEmpty, passEmoji }
