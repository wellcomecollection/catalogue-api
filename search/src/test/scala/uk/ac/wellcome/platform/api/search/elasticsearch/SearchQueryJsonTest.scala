package uk.ac.wellcome.platform.api.search.elasticsearch

import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.json.JacksonBuilder
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.utils.JsonAssertions

import scala.io.Source

class SearchQueryJsonTest extends AnyFunSpec with Matchers with JsonAssertions {
  it("matches the works JSON") {
    val queries = Map(
      "WorksMultiMatcherQuery.json" -> WorksMultiMatcherQuery,
      "WorksWithRelationsQuery.json" -> WorksWithRelationsQuery
    )

    queries.foreach {
      case (filePath, query) => {
        val fileJson =
          Source
            .fromResource(filePath)
            .getLines
            .mkString

        val queryJson = JacksonBuilder.writeAsString(
          QueryBuilderFn(query("{{query}}")).value
        )
        assertJsonStringsAreEqual(fileJson, queryJson)
      }
    }

  }

  it("matches the images JSON") {
    val fileJson =
      Source
        .fromResource("ImagesMultiMatcherQuery.json")
        .getLines
        .mkString

    val queryJson = JacksonBuilder.writeAsString(
      QueryBuilderFn(ImagesMultiMatcher("{{query}}")).value
    )
    assertJsonStringsAreEqual(fileJson, queryJson)
  }
}
