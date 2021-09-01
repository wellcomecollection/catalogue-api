type Namespace = 'works' | 'images'

type Env = 'local' | 'remote'

type Query = string

type Index = string

class SearchTemplate {
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
