import { Env } from './env'
import { Namespace } from './namespace'

type Query = string
type Index = string

export class SearchTemplate {
  namespace: Namespace
  env: Env
  index: string

  async getQuery(): Promise<Query> {
    if (this.env === 'local') {
      return this.fetchLocalQuery()
    } else {
      return this.fetchRemoteQuery()
    }
  }

  private async fetchLocalQuery(): Promise<Query> {
    return ''
  }

  private async fetchRemoteQuery(): Promise<Query> {
    return ''
  }
}

type SearchTemplateString = `${Env}/${Namespace}/${Index}`
