import { GetServerSideProps, NextPage } from 'next'
import { SearchTemplate, SearchTemplateString } from '../types/searchTemplate'
import { getTemplates, listIndices } from '../services/search-templates'

import { H1 } from '../components/H'
import SearchTemplateTests from '../components/SearchTemplateTests'
import { removeEmpty } from '../services/utils'

type Props = {
  searchTemplates: SearchTemplate[]
}

export const getServerSideProps: GetServerSideProps<Props> = async () => {
  const indices = await listIndices()
  const templateStrings = indices.map(
    (index) => `production/${index}` as SearchTemplateString
  )
  const searchTemplates = await getTemplates(templateStrings)
  return {
    props: removeEmpty({ searchTemplates }),
  }
}

export const Index: NextPage<Props> = ({ searchTemplates }) => {
  return (
    <>
      <div className="mb-4 max-w-2xl">
        <H1>Search relevance evaluation</H1>
        <p className="mb-2">
          Rank is a tool help ensure the continued quality of our search ranking
          whilst helping us iterate to improve it.
        </p>
      </div>
      <SearchTemplateTests searchTemplates={searchTemplates} />
    </>
  )
}

export default Index
