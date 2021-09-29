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
import weco.elasticsearch.{ElasticClientBuilder, IndexConfig}
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.json.JsonUtil.toJson
import io.circe.generic.auto._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ElasticClient, Index}
import com.sksamuel.elastic4s.circe.hitReaderWithCirce
import com.sksamuel.elastic4s.requests.searches.{
  MultiSearchRequest,
  SearchRequest
}
import org.scalatest.EitherValues
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.fixtures.{RandomGenerators, TestWith}

import scala.concurrent.ExecutionContext.Implicits.global

class ElasticsearchServiceTest
    extends AnyFunSpec
    with Matchers
    with IndexFixtures
    with EitherValues
    with RandomGenerators
    with IdentifiersGenerators
    with ElasticsearchFixtures {

  case class ExampleThing(id: CanonicalId, name: String)
  case class DifferentExampleThing(id: CanonicalId, age: Int)

  val badElasticClient: ElasticClient = ElasticClientBuilder.create(
    hostname = "noresolve",
    port = 9200,
    protocol = "http",
    username = "elastic",
    password = "changeme",
    compressionEnabled = false
  )

  def randomThing: ExampleThing = ExampleThing(
    id = createCanonicalId,
    name = randomAlphanumeric(10).toLowerCase
  )

  def searchRequestForThingByName(index: Index, name: String): SearchRequest =
    search(index)
      .query(
        boolQuery.filter(
          termQuery(field = "name", value = name)
        )
      )
      .size(1)

  def withExampleIndex[R](
    thingsToIndex: List[ExampleThing]
  )(testWith: TestWith[Index, R]): R = {
    val id = KeywordField("id")
    val name = TextField("name")
    val mapping = MappingDefinition(properties = List(id, name))
    val analysis = Analysis(analyzers = List.empty)
    val indexConfig = IndexConfig(mapping, analysis)

    withLocalElasticsearchIndex(config = indexConfig) { index =>
      val result = elasticClient.execute(
        bulk(
          thingsToIndex.map { thing =>
            val jsonDoc = toJson(thing).get
            indexInto(index.name)
              .id(thing.id.underlying)
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

  describe("findById") {
    it("finds documents by ID") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingToQueryFor = thingsToIndex.head

        val findByIdFuture =
          elasticsearchService.findById[ExampleThing](thingToQueryFor.id)(index)

        whenReady(findByIdFuture) {
          _.right.value shouldBe thingToQueryFor
        }
      }
    }

    it("fails if deserialising to a different type than stored") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingToQueryFor = thingsToIndex.head

        val findByIdFuture =
          elasticsearchService.findById[DifferentExampleThing](
            thingToQueryFor.id
          )(index)

        whenReady(findByIdFuture.failed) {
          _.getMessage should include("Unable to parse JSON")
        }
      }
    }

    it("should return an appropriate error if no ID matches") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val badId = createCanonicalId

        val findByIdFuture =
          elasticsearchService.findById[ExampleThing](badId)(index)

        whenReady(findByIdFuture) {
          _.left.value shouldBe a[DocumentNotFoundError[_]]
        }
      }
    }

    it("should return an appropriate error if the index does not exist") {
      val elasticsearchService = new ElasticsearchService(elasticClient)
      val badIndex = createIndex

      val findByIdFuture = elasticsearchService.findById[ExampleThing](
        createCanonicalId
      )(badIndex)

      whenReady(findByIdFuture) {
        _.left.value shouldBe a[IndexNotFoundError]
      }
    }

    it("fails if it cannot connect to Elasticsearch") {
      val elasticsearchService = new ElasticsearchService(badElasticClient)
      val badIndex = createIndex

      val findByIdFuture = elasticsearchService.findById[ExampleThing](
        createCanonicalId
      )(badIndex)

      whenReady(findByIdFuture.failed) {
        _.getMessage should include("UnknownHostException")
      }
    }
  }

  describe("findBySearch") {
    it("finds documents by search") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingToQueryFor = thingsToIndex.head

        val searchRequest = searchRequestForThingByName(
          index = index,
          name = thingToQueryFor.name
        )

        val findBySearchFuture =
          elasticsearchService.findBySearch[ExampleThing](searchRequest)

        whenReady(findBySearchFuture) {
          _.right.value shouldBe Seq(thingToQueryFor)
        }
      }
    }

    it("fails if deserialising to a different type than stored") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingToQueryFor = thingsToIndex.head

        val searchRequest = searchRequestForThingByName(
          index = index,
          name = thingToQueryFor.name
        )

        val findBySearchFuture =
          elasticsearchService.findBySearch[DifferentExampleThing](
            searchRequest
          )

        whenReady(findBySearchFuture.failed) {
          _.getMessage should include("Unable to parse JSON")
        }
      }
    }

    it("returns an appropriate error when the specified index does not exist") {
      val elasticsearchService = new ElasticsearchService(elasticClient)
      val badIndex = createIndex

      val searchRequest = searchRequestForThingByName(
        index = badIndex,
        name = randomAlphanumeric(10)
      )

      val findBySearchFuture =
        elasticsearchService.findBySearch[ExampleThing](searchRequest)

      whenReady(findBySearchFuture) {
        _.left.value shouldBe a[IndexNotFoundError]
      }
    }

    it("fails if it cannot connect to Elasticsearch") {
      val elasticsearchService = new ElasticsearchService(badElasticClient)

      val searchRequest = searchRequestForThingByName(
        index = createIndex,
        name = randomAlphanumeric(10)
      )

      val findByMultiSearchFuture = elasticsearchService
        .findBySearch[ExampleThing](searchRequest)

      whenReady(findByMultiSearchFuture.failed) {
        _.getMessage should include("UnknownHostException")
      }
    }
  }

  describe("findByMultiSearch") {
    it("finds documents by performing a MultiSearch") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingsToQueryFor = thingsToIndex.slice(0, 3)

        val searchRequests = thingsToQueryFor.map { thing =>
          searchRequestForThingByName(
            index = index,
            name = thing.name
          )
        }

        val multiSearchRequest = MultiSearchRequest(searchRequests)

        val findByMultiSearchFuture = elasticsearchService
          .findByMultiSearch[ExampleThing](multiSearchRequest)

        whenReady(findByMultiSearchFuture) { results =>
          val (errorEither, foundBySearchEither) = results.partition(_.isLeft)
          val errors = errorEither.map(_.left.value)
          val foundBySearch = foundBySearchEither.map(_.right.value)

          errors should have length (0)
          foundBySearch shouldBe thingsToQueryFor.map(Seq(_))
        }
      }
    }

    it("fails if deserialising to a different type than stored") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingsToQueryFor = thingsToIndex.slice(0, 3)

        val searchRequests = thingsToQueryFor.map { thing =>
          searchRequestForThingByName(
            index = index,
            name = thing.name
          )
        }

        val multiSearchRequest = MultiSearchRequest(searchRequests)

        val findByMultiSearchFuture = elasticsearchService
          .findByMultiSearch[DifferentExampleThing](multiSearchRequest)

        whenReady(findByMultiSearchFuture.failed) {
          _.getMessage should include("Unable to parse JSON")
        }
      }
    }

    it("returns errors for queries that fail") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingsToQueryFor = thingsToIndex.slice(0, 3)
        val badIndex = createIndex

        val searchRequests = searchRequestForThingByName(
          index = badIndex,
          name = randomAlphanumeric(10)
        ) +: thingsToQueryFor.map { thing =>
          searchRequestForThingByName(
            index = index,
            name = thing.name
          )
        }

        val multiSearchRequest = MultiSearchRequest(searchRequests)

        val findByMultiSearchFuture = elasticsearchService
          .findByMultiSearch[ExampleThing](multiSearchRequest)

        whenReady(findByMultiSearchFuture) { results =>
          val expectedFailure :: expectedSuccess = results

          expectedFailure.left.value shouldBe a[IndexNotFoundError]
          expectedSuccess.map(_.right.value) shouldBe thingsToQueryFor.map(
            Seq(_)
          )
        }
      }
    }

    it("fails if it cannot connect to Elasticsearch") {
      val elasticsearchService = new ElasticsearchService(badElasticClient)

      val searchRequests = Seq(
        searchRequestForThingByName(
          index = createIndex,
          name = randomAlphanumeric(10)
        )
      )

      val multiSearchRequest = MultiSearchRequest(searchRequests)

      val findByMultiSearchFuture = elasticsearchService
        .findByMultiSearch[ExampleThing](multiSearchRequest)

      whenReady(findByMultiSearchFuture.failed) {
        _.getMessage should include("UnknownHostException")
      }
    }
  }

  describe("executeMultiSearchRequest") {
    it("performs a multiSearchRequest") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingsToQueryFor = thingsToIndex.slice(0, 3)

        val searchRequests = thingsToQueryFor.map { thing =>
          searchRequestForThingByName(
            index = index,
            name = thing.name
          )
        }

        val multiSearchRequest = MultiSearchRequest(searchRequests)
        val multiSearchResponseFuture =
          elasticsearchService.executeMultiSearchRequest(multiSearchRequest)

        whenReady(multiSearchResponseFuture) { results =>
          val (errorEither, searchResponsesEither) = results.partition(_.isLeft)
          val errors = errorEither.map(_.left.value)
          val searchResponses =
            searchResponsesEither.flatMap(_.right.value.to[ExampleThing])

          errors should have length (0)
          searchResponses shouldBe thingsToQueryFor
        }
      }
    }

    it("returns errors for queries that fail") {
      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingsToQueryFor = thingsToIndex.slice(0, 3)
        val badIndex = createIndex

        val searchRequests = searchRequestForThingByName(
          index = badIndex,
          name = randomAlphanumeric(10)
        ) +: thingsToQueryFor.map { thing =>
          searchRequestForThingByName(
            index = index,
            name = thing.name
          )
        }

        val multiSearchRequest = MultiSearchRequest(searchRequests)
        val multiSearchResponseFuture =
          elasticsearchService.executeMultiSearchRequest(multiSearchRequest)

        whenReady(multiSearchResponseFuture) { results =>
          val expectedFailure :: expectedSuccess = results

          expectedFailure.left.value shouldBe a[IndexNotFoundError]
          expectedSuccess.flatMap(_.right.value.to[ExampleThing]) shouldBe thingsToQueryFor
        }
      }
    }

    it("fails if it cannot connect to Elasticsearch") {
      val elasticsearchService = new ElasticsearchService(badElasticClient)

      val searchRequests = Seq(
        searchRequestForThingByName(
          index = createIndex,
          name = randomAlphanumeric(10)
        )
      )

      val multiSearchRequest = MultiSearchRequest(searchRequests)

      val findByMultiSearchFuture = elasticsearchService
        .executeMultiSearchRequest(multiSearchRequest)

      whenReady(findByMultiSearchFuture.failed) {
        _.getMessage should include("UnknownHostException")
      }
    }
  }

  describe("executeSearchRequest") {
    it("performs a searchRequest") {

      val thingsToIndex = collectionOf(min = 5, max = 10) { randomThing } toList

      withExampleIndex(thingsToIndex) { index =>
        val elasticsearchService = new ElasticsearchService(elasticClient)
        val thingToQueryFor = thingsToIndex.head

        val searchRequest = searchRequestForThingByName(
          index = index,
          name = thingToQueryFor.name
        )

        val searchResponseFuture =
          elasticsearchService.executeSearchRequest(searchRequest)

        whenReady(searchResponseFuture) {
          _.right.value.to[ExampleThing] shouldBe Seq(thingToQueryFor)
        }
      }
    }

    it("returns an appropriate error when the specified index does not exist") {
      val elasticsearchService = new ElasticsearchService(elasticClient)
      val badIndex = createIndex

      val searchRequest = searchRequestForThingByName(
        index = badIndex,
        name = randomAlphanumeric(10)
      )

      val searchResponseFuture =
        elasticsearchService.executeSearchRequest(searchRequest)

      whenReady(searchResponseFuture) {
        _.left.value shouldBe a[IndexNotFoundError]
      }
    }

    it("fails if it cannot connect to Elasticsearch") {
      val elasticsearchService = new ElasticsearchService(badElasticClient)

      val searchRequest = searchRequestForThingByName(
        index = createIndex,
        name = randomAlphanumeric(10)
      )

      val findByMultiSearchFuture = elasticsearchService
        .executeSearchRequest(searchRequest)

      whenReady(findByMultiSearchFuture.failed) {
        _.getMessage should include("UnknownHostException")
      }
    }
  }
}
