import { GetServerSideProps, NextPage } from 'next'
import { SearchTemplate, getTemplates } from '../services/search-templates'
import { removeEmpty } from '../utils'
import { Env } from '../types/env'
import SearchTemplateTests from '../components/SearchTemplateTests'
import Form, { TextInput, Label, Button } from '../components/Form'
import { H2, H3 } from '../components/H'
import { Namespace, namespaces } from '../types/namespace'

type Props = {
  searchTemplates: SearchTemplate[]
  testsQuery: string[]
}

function getTemplatePropsFromString(str: string) {
  const [namespace, env, index] = str.split('/')
  return { namespace, env, index }
}

export const getServerSideProps: GetServerSideProps<Props> = async ({
  query,
}) => {
  const testsQuery = Array.isArray(query.test)
    ? query.test
    : query.test
    ? [query.test]
    : []

  const tests = testsQuery.map((test) => getTemplatePropsFromString(test))
  const searchTemplates = await getTemplates({ prod: true, local: true })
  const testTemplates = tests.map((test) => {
    const template = searchTemplates.find(
      ({ namespace, env }) => test.env === env && test.namespace === namespace
    )
    return {
      ...template,
      namespace: test.namespace as Namespace,
      env: test.env as Env,
      index: test.index,
    }
  })

  return {
    props: removeEmpty({
      searchTemplates: searchTemplates.concat(testTemplates),
      testsQuery: testsQuery,
    }),
  }
}

export const Index2: NextPage<Props> = ({ searchTemplates, testsQuery }) => {
  return (
    <>
      <Form>
        <Label htmlFor="new-test">namspace/env/index</Label>
        <div className="flex">
          <TextInput id="new-test" name="test" />
          <Button>Add</Button>
        </div>
        {/* This is so it appends new tests on */}
        {testsQuery.map((str) => (
          <input type="hidden" name="test" value={str} key={str} />
        ))}
      </Form>

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

export default Index2
