package weco.api.search.fixtures

import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.apache.pekko.http.scaladsl.model.{ContentTypes, StatusCode, Uri}
import org.apache.pekko.http.scaladsl.model.headers.Host
import org.apache.pekko.http.scaladsl.server.Route
import com.sksamuel.elastic4s.Index
import io.circe.parser.parse
import io.circe.Json
import org.scalatest.{Assertion, Suite}
import weco.api.search.SearchApi
import weco.fixtures.TestWith
import weco.api.search.models.{ApiConfig, ElasticConfig}

trait ApiFixture
    extends ScalatestRouteTest
    with ResilientElasticClientFixture {
  this: Suite =>
  val Status = org.apache.pekko.http.scaladsl.model.StatusCodes

  val publicRootUri: String

  implicit def defaultHostInfo: DefaultHostInfo = DefaultHostInfo(
    host = Host(apiConfig.publicHost),
    securedConnection = if (apiConfig.publicScheme == "https") true else false
  )

  lazy val apiConfig = ApiConfig(
    publicRootUri = Uri(publicRootUri),
    defaultPageSize = 10
  )

  def withRouter[R](
    elasticConfig: ElasticConfig
  )(testWith: TestWith[Route, R]): R = {
    val router = new SearchApi(
      resilientElasticClient,
      elasticConfig,
      apiConfig = apiConfig,
      pipelineDate = "pipeline-date"
    )

    testWith(router.routes)
  }

  def withApi[R](testWith: TestWith[Route, R]): R = {
    val elasticConfig = ElasticConfig.forDefaultCluster(
      worksIndexName = Some("worksIndex-notused"),
      imagesIndexName = Some("imagesIndex-notused"),
      serviceName = "catalogue_api"
    )

    withRouter(elasticConfig) { route =>
      testWith(route)
    }
  }

  def withWorksApi[R](testWith: TestWith[(Index, Route), R]): R =
    withLocalWorksIndex { worksIndex =>
      val elasticConfig = ElasticConfig.forDefaultCluster(
        worksIndexName = Some(worksIndex.name),
        imagesIndexName = Some("imagesIndex-notused"),
        serviceName = "catalogue_api"
      )

      withRouter(elasticConfig) { route =>
        testWith((worksIndex, route))
      }
    }

  def withImagesApi[R](testWith: TestWith[(Index, Route), R]): R =
    withLocalImagesIndex { imagesIndex =>
      val elasticConfig = ElasticConfig.forDefaultCluster(
        worksIndexName = Some("worksIndex-notused"),
        imagesIndexName = Some(imagesIndex.name),
        serviceName = "catalogue_api"
      )

      withRouter(elasticConfig) { route =>
        testWith((imagesIndex, route))
      }
    }

  /** 
   * Create a multi-cluster API for testing with multiple Elasticsearch indices.
   * Each cluster gets its own index, and the elasticCluster param can route between them.
   */
  def withMultiClusterApi[R](
    defaultElastic: ElasticConfig,
    additionalElastics: Map[String, ElasticConfig]
  )(testWith: TestWith[(Map[String, Index], Route), R]): R = {
    withLocalWorksIndex { defaultIndex =>
      // Create indices for additional clusters recursively
      createAdditionalIndices(additionalElastics.keys.toList) { additionalIndicesList =>
        val additionalIndices = additionalElastics.keys.zip(additionalIndicesList).toMap
        val allIndices = Map("default" -> defaultIndex) ++ additionalIndices
        
        // Update cluster configs with actual index names
        val defaultElasticWithIndex = defaultElastic.copy(
          worksIndex = Some(defaultIndex.name)
        )
        val additionalElasticsWithIndices = additionalElastics.map {
          case (name, config) =>
            name -> config.copy(worksIndex = Some(additionalIndices(name).name))
        }
        
        // Build the multi-cluster API (using same resilient client for all in tests)
        val router = new SearchApi(
          elasticClient = resilientElasticClient,
          elasticConfig = defaultElasticWithIndex,
          additionalElasticClients = additionalElasticsWithIndices.keys.map(_ -> resilientElasticClient).toMap,
          additionalElasticConfigs = additionalElasticsWithIndices,
          apiConfig = apiConfig,
          pipelineDate = "pipeline-date"
        )
        
        testWith((allIndices, router.routes))
      }
    }
  }
  
  private def createAdditionalIndices[R](clusterNames: List[String])(testWith: TestWith[List[Index], R]): R = {
    def createRecursive(remaining: List[String], accumulated: List[Index]): R = {
      remaining match {
        case Nil => testWith(accumulated)
        case _ :: tail =>
          withLocalWorksIndex { index =>
            createRecursive(tail, accumulated :+ index)
          }
      }
    }
    createRecursive(clusterNames, List.empty)
  }

  def assertJsonResponse(
    routes: Route,
    path: String,
    unordered: Boolean = false
  )(expectedResponse: (StatusCode, String)): Assertion =
    expectedResponse match {
      case (expectedStatus, expectedJson) =>
        def responseJson: Json = eventually {
          Get(path) ~> routes ~> check {
            contentType shouldEqual ContentTypes.`application/json`
            status shouldEqual expectedStatus
            parseJson(responseAs[String], unordered)
          }
        }
        responseJson shouldEqual parseJson(expectedJson, unordered)
    }

  def assertJsonResponseLike(
    routes: Route,
    path: String
  )(assertion: Json => Assertion): Assertion = {
    def responseJson = eventually {
      Get(path) ~> routes ~> check {
        status shouldEqual Status.OK
        contentType shouldEqual ContentTypes.`application/json`
        parseJson(responseAs[String])
      }
    }
    assertion(responseJson)
  }

  def assertJsonResponseContains(
    routes: Route,
    path: String,
    locator: Json => Json,
    expectedJson: String
  ): Assertion =
    assertJsonResponseLike(routes, path) { responseJson =>
      locator(responseJson) shouldEqual parseJson(
        expectedJson
      )
    }

  def assertRedirectResponse(routes: Route, path: String)(
    expectedResponse: (StatusCode, String)
  ) =
    eventually {
      expectedResponse match {
        case (expectedStatus, expectedLocation) =>
          Get(path) ~> routes ~> check {
            status shouldEqual expectedStatus
            header("Location").map(_.value) shouldEqual Some(expectedLocation)
          }
      }
    }

  def parseJson(string: String, unordered: Boolean = false): Json =
    parse(string) match {
      case Right(json) => sortedJson(unordered)(json)
      case Left(err) =>
        throw new RuntimeException(
          s"Asked to compare a string that wasn't JSON. Error: $err. JSON:\n$string"
        )
    }

  private def sortedJson(unordered: Boolean)(json: Json): Json =
    json.arrayOrObject(
      json,
      array => {
        val arr = array.map(sortedJson(unordered))
        if (unordered) {
          Json.arr(arr.sortBy(_.toString): _*)
        } else {
          Json.arr(arr: _*)
        }
      },
      obj =>
        Json.obj(
          obj.toList
            .map {
              case (key, value) =>
                (key, sortedJson(unordered)(value))
            }
            .sortBy(tup => tup._1): _*
        )
    )
}
