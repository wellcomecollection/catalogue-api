package uk.ac.wellcome.platform.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.queries.QueryBuilderFn
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import uk.ac.wellcome.json.utils.JsonAssertions

class SearchQueryJsonTest extends AnyFunSpec with Matchers with JsonAssertions {
  it("matches the works JSON") {
    val fileJson =
      Source
        .fromResource("WorksMultiMatcherQuery.json")
        .getLines
        .mkString

    val queryJson = QueryBuilderFn(WorksMultiMatcher("{{query}}")).string()
    assertJsonStringsAreEqual(fileJson, queryJson)
  }

  it("matches the images JSON") {
    val fileJson =
      Source
        .fromResource("ImagesMultiMatcherQuery.json")
        .getLines
        .mkString

    val queryJson = QueryBuilderFn(ImagesMultiMatcher("{{query}}")).string()
    assertJsonStringsAreEqual(fileJson, queryJson)
  }
}
