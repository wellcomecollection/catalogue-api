const removeEmpty = <T extends Record<string, unknown>>(obj: T): T => {
  return JSON.parse(JSON.stringify(obj))
}

const groupBy = <T, K extends keyof any>(list: T[], getKey: (item: T) => K) =>
  list.reduce((previous, currentItem) => {
    const group = getKey(currentItem)
    if (!previous[group]) previous[group] = []
    previous[group].push(currentItem)
    return previous
  }, {} as Record<K, T[]>)

export { removeEmpty, groupBy }
