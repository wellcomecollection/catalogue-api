package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.json.JacksonBuilder
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.services.WorksTemplateSearchBuilder
import weco.fixtures.LocalResources
import weco.json.utils.JsonAssertions

class SearchQueryJsonTest
  extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with LocalResources {
  it("matches the works JSON") {
    // This test is essentially redundant, given that the test is basically doing
    // the same thing as the SUT, just with the file in a different location.
    // However, I'm leaving them both in until after the refactor is complete
    // for Images as well, to ensure that the search-templates.json endpoint
    // continues to work as expected.
    val fileJson = readResource("WorksMultiMatcherQuery.json")
    assertJsonStringsAreEqual(fileJson, WorksTemplateSearchBuilder.queryTemplate)
  }

  it("matches the images JSON") {
    val fileJson = readResource("ImagesMultiMatcherQuery.json")

    val queryJson = JacksonBuilder.writeAsString(
      QueryBuilderFn(ImagesMultiMatcher("{{query}}")).value
    )
    assertJsonStringsAreEqual(fileJson, queryJson)
  }
}
