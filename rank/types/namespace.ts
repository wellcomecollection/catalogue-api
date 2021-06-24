const namespaces = ['works', 'images'] as const
type Namespace = typeof namespaces[number]

function isNamespace(v: any): v is Namespace {
  return namespaces.includes(v.toString())
}

function getNamespaceFromIndexName(
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

export { namespaces, isNamespace }
export type { Namespace }
