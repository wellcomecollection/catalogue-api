export const envs = ['remote', 'local'] as const
export type Env = typeof envs[number]
export function isEnv(v: any): v is Env {
    return v && envs.includes(v.toString())
}

export const namespaces = ['works', 'images'] as const
export type Namespace = typeof namespaces[number]
export function isNamespace(v: any): v is Namespace {
    return v && namespaces.includes(v.toString())
}

export type Query = string | unknown
export type Index = string
export type SearchTemplateString = `${Env}/${Index}`

export class SearchTemplate {
    index: Index
    namespace: Namespace
    env: Env
    query: Query
    string: SearchTemplateString

    constructor(str: SearchTemplateString) {
        const [env, index] = str.split('/')
        this.env = env as Env
        this.index = index as Index
        this.namespace = getNamespaceFromIndexName(index)
        this.string = `${this.env}/${this.index}`
    }
}

export type ApiSearchTemplateRes = {
    templates: {
        id: string
        index: Index
        query: string // this is a JSON string
    }[]
}

export function getNamespaceFromIndexName(
    index: Index,
    fallback: Namespace = 'works'
): Namespace | undefined {
    // we prefix our index names with ccr--
    const noCcr = index.replace('ccr--', '')
    return noCcr.startsWith('works')
        ? 'works'
        : noCcr.startsWith('images')
        ? 'images'
        : fallback
}
