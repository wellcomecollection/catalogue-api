package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl.{
  boolQuery,
  bulk,
  indexInto,
  search,
  termQuery
}
import com.sksamuel.elastic4s.analysis.Analysis
import com.sksamuel.elastic4s.fields.{KeywordField, TextField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.index.IndexFixtures
import weco.elasticsearch.IndexConfig
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.json.JsonUtil.toJson
import io.circe.generic.auto._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.circe.hitReaderWithCirce
import com.sksamuel.elastic4s.requests.searches.MultiSearchRequest
import org.scalatest.EitherValues
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import weco.fixtures.{RandomGenerators, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global

class ElasticsearchServiceTest
    extends AnyFunSpec
    with Matchers
    with IndexFixtures
    with EitherValues
    with RandomGenerators
    with ElasticsearchFixtures {

  case class ExampleThing(id: String, name: String)

  def randomThing = ExampleThing(
    id = randomAlphanumeric(10).toLowerCase,
    name = randomAlphanumeric(10).toLowerCase
  )

  def searchRequestForThingByName(index: Index, name: String) = {
    search(index)
      .query(
        boolQuery.filter(
          termQuery(field = "name", value = name)
        )
      )
      .size(1)
  }

  def withExampleIndex[R](
    thingsToIndex: List[ExampleThing]
  )(testWith: TestWith[Index, R]): R = {
    val id = KeywordField("id")
    val name = TextField("name")
    val mapping = MappingDefinition(properties = List(id, name))
    val analysis = Analysis(analyzers = List.empty)
    val indexConfig = IndexConfig(mapping, analysis)

    withLocalIndex(indexConfig) { index =>
      val result = elasticClient.execute(
        bulk(
          thingsToIndex.map { thing =>
            val jsonDoc = toJson(thing).get
            indexInto(index.name)
              .id(thing.id)
              .doc(jsonDoc)
          }
        ).refreshImmediately
      )

      whenReady(result, Timeout(Span(30, Seconds))) { _ =>
        getSizeOf(index) shouldBe thingsToIndex.size

        testWith(index)
      }
    }
  }

  describe("executeMultiSearchRequest") {
    it("performs a multiSearchRequest") {

      val thingsToIndex = 0.to(randomInt(5, 10)).map(_ => randomThing).toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)

        val searchRequests = thingsToIndex.map { thing =>
          searchRequestForThingByName(
            index = index,
            name = thing.name
          )
        }

        val multiSearchRequest = MultiSearchRequest(searchRequests)
        val multiSearchResponseFuture =
          elasticsearchService.executeMultiSearchRequest(multiSearchRequest)

        whenReady(multiSearchResponseFuture) { multiSearchResponseEither =>
          val multiSearchResponse = multiSearchResponseEither.right.value
          val queryResults =
            multiSearchResponse.items.map(_.response.right.value)
          val returnedThings = queryResults.flatMap(_.to[ExampleThing])

          returnedThings.toSet shouldBe thingsToIndex.toSet
        }
      }
    }
  }

  describe("executeSearchRequest") {
    it("performs a searchRequest") {

      val thingsToIndex = 0.to(randomInt(5, 10)).map(_ => randomThing).toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingToQueryFor = thingsToIndex.head

        val searchRequest = searchRequestForThingByName(
          index = index,
          name = thingToQueryFor.name
        )

        val searchResponseFuture =
          elasticsearchService.executeSearchRequest(searchRequest)

        whenReady(searchResponseFuture) { searchResponseEither =>
          val searchResponse = searchResponseEither.right.value
          val queryResult = searchResponse.to[ExampleThing].head

          queryResult shouldBe thingToQueryFor
        }
      }
    }
  }
}
