import { GetServerSideProps, NextPage } from 'next'
import { H2, H3 } from '../components/H'
import { SearchTemplate, namespaces } from '../types/searchTemplate'

import SearchTemplateTests from '../components/SearchTemplateTests'
import { getTemplates } from '../services/search-templates'
import { removeEmpty } from '../services/utils'

type Props = {
  searchTemplates: SearchTemplate[]
}

export const getServerSideProps: GetServerSideProps<Props> = async () => {
  const searchTemplates = await getTemplates()
  return {
    props: removeEmpty({
      searchTemplates,
    }),
  }
}

export const Dev: NextPage<Props> = ({ searchTemplates }) => {
  return (
    <>
      <H2>Tests running</H2>
      {namespaces.map((namespace) => {
        const namespacedTemplates = searchTemplates.filter(
          (template) => template.namespace === namespace
        )
        return (
          <div key={namespace}>
            <H3>{namespace}</H3>
            <ol key={namespace} className="list-decimal list-inside ml-5">
              {namespacedTemplates.map((template) => {
                return <li key={template.id}>{template.id}</li>
              })}
            </ol>
          </div>
        )
      })}
      <div className="mt-8">
        <SearchTemplateTests searchTemplates={searchTemplates} />
      </div>
    </>
  )
}

export default Dev
