package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.json.JacksonBuilder
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.fixtures.LocalResources
import weco.json.utils.JsonAssertions

class SearchQueryJsonTest
    extends AnyFunSpec
    with Matchers
    with JsonAssertions
    with LocalResources {
  it("matches the works JSON") {
    val fileJson = readResource("WorksMultiMatcherQuery.json")

    val queryJson = JacksonBuilder.writeAsString(
      QueryBuilderFn(
        WorksMultiMatcher("{{query}}")
          .filter(termQuery(field = "type", value = "Visible"))
      ).value
    )

    assertJsonStringsAreEqual(fileJson, queryJson)
  }

  it("matches the images JSON") {
    val fileJson = readResource("ImagesMultiMatcherQuery.json")

    val queryJson = JacksonBuilder.writeAsString(
      QueryBuilderFn(ImagesMultiMatcher("{{query}}")).value
    )
    assertJsonStringsAreEqual(fileJson, queryJson)
  }
}
