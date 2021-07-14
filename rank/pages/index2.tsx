import { GetServerSideProps, NextPage } from 'next'
import { SearchTemplate, getTemplates } from '../services/search-templates'
import { removeEmpty } from '../utils'
import SearchTemplateTests from '../components/SearchTemplateTests'

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

export const Index2: NextPage<Props> = ({ searchTemplates }) => {
  return <SearchTemplateTests searchTemplates={searchTemplates} />
}

export default Index2
