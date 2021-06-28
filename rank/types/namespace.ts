export const namespaces = ['works', 'images'] as const
export type Namespace = typeof namespaces[number]

export function isNamespace(v: any): v is Namespace {
  return v && namespaces.includes(v.toString())
}

export function getNamespaceFromIndexName(
  index: string,
  fallback: Namespace = 'works'
): Namespace | undefined {
  // we prefix our domain names with ccr--
  const noCcr = index.replace('ccr--', '')
  return noCcr.startsWith('works')
    ? 'works'
    : noCcr.startsWith('images')
    ? 'images'
    : fallback
}
