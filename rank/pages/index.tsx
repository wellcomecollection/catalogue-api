import { GetServerSideProps, NextPage } from 'next'
import { SearchTemplate, getTemplates } from '../services/search-templates'
import { removeEmpty } from '../utils'
import SearchTemplateTests from '../components/SearchTemplateTests'
import { H1 } from '../components/H'

type Props = {
  searchTemplates: SearchTemplate[]
}

function get(str: string) {
  const [env, namespace, index] = str.split('/')
  return { env, namespace, index }
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query,
}) => {
  const tests = (
    Array.isArray(query.test) ? query.test : query.test ? [query.test] : []
  ).map((test) => get(test))

  const searchTemplates = await getTemplates({ prod: true })
  return {
    props: removeEmpty({
      searchTemplates: searchTemplates,
    }),
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
