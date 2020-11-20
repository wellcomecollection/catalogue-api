package uk.ac.wellcome.platform.api

import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.model.ContentTypes
import io.circe.Json
import org.scalatest.prop.TableDrivenPropertyChecks
import uk.ac.wellcome.platform.api.models.SearchQueryType
import uk.ac.wellcome.platform.api.rest._
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase

import scala.reflect.runtime.universe._

class ApiSwaggerTest
  extends ApiWorksTestBase
    with Matchers
    with JsonHelpers
    with TableDrivenPropertyChecks {
  val worksEndpoint = "/works"
  val workEndpoint = "/works/{id}"
  val imageEndpoint = "/images/{id}"
  val imagesEndpoint = "/images"
  it("should return valid json object") {
    checkSwaggerJson { json =>
      json.isObject shouldBe true
    }
  }

  it("should contain info") {
    checkSwaggerJson { json =>
      val info = getKey(json, "info")
      info.isEmpty shouldBe false
      getKey(info.get, "version") shouldBe Some(Json.fromString("v2"))
      getKey(info.get, "description") shouldBe Some(
        Json.fromString("Search our collections"))
      getKey(info.get, "title") shouldBe Some(Json.fromString("Catalogue"))
    }
  }

  it("should contain servers") {
    checkSwaggerJson { json =>
      getKey(json, "servers") shouldBe Some(
        Json.arr(
          Json.obj(
            "url" -> Json.fromString("https://api-testing.local/catalogue/v2/")
          )
        )
      )
    }
  }

  it("sets a non-empty description and summary on all paths") {
    val testCases = Table(
      "endpoint",
      workEndpoint,
      worksEndpoint,
      imageEndpoint,
      imagesEndpoint
    )

    forAll(testCases) { endpointString =>
      checkSwaggerJson { json =>
        val endpoint = getEndpoint(json, endpointString)

        getKey(endpoint, "description").isEmpty shouldBe false
        getKey(endpoint, "summary").isEmpty shouldBe false
      }
    }
  }

  describe("includes the route and query parameters on all paths") {
    it("single work endpoint") {
      val swaggerParams = getParameterNames(workEndpoint)

      val routeParam = "id"
      val queryParams = getPublicQueryParams[SingleWorkParams]

      val internalParams = queryParams :+ routeParam

      assert(
        swaggerParams.length == internalParams.length,
        s"swaggerParams  = ${swaggerParams.sorted}internalParams = ${internalParams.sorted}"
      )
    }

    it("multiple works endpoint") {
      val swaggerParams = getParameterNames(worksEndpoint)

      val queryParams = getPublicQueryParams[MultipleWorksParams]

      assert(
        swaggerParams.length == queryParams.length,
        s"swaggerParams = ${swaggerParams.sorted}\nactualParams  = ${queryParams.sorted}"
      )
    }

    it("single image endpoint") {
      val swaggerParams = getParameterNames(imageEndpoint)

      val routeParam = "id"
      val queryParams = getPublicQueryParams[SingleImageParams]

      val internalParams = queryParams :+ routeParam

      assert(
        swaggerParams.length == internalParams.length,
        s"swaggerParams  = ${swaggerParams.sorted}internalParams = ${internalParams.sorted}"
      )
    }

    it("multiple images endpoint") {
      val swaggerParams = getParameterNames(imagesEndpoint)

      val queryParams = getPublicQueryParams[MultipleImagesParams]

      assert(
        swaggerParams.length == queryParams.length,
        s"swaggerParams = ${swaggerParams.sorted}\nactualParams  = ${queryParams.sorted}"
      )
    }

    def getPublicQueryParams[T: TypeTag]: Seq[String] =
      typeOf[T].members
        .collect {
          case m: MethodSymbol if m.isCaseAccessor => m.name.toString
        }
        .filterNot {
          _ == "_index"
        }
        .toSeq
        .map { _.replace("$u002E", ".") }

    def getParameterNames(endpointString: String): List[String] =
      checkSwaggerJson { json =>
        val endpointSwagger = getEndpoint(json, endpointString)

        val parameters =
          getKey(endpointSwagger, "parameters")
            .flatMap { _.asArray }
            .map { params => params.flatMap { getKey(_, "name") } }
            .get
            .map { _.asString.get }
            .toList

        assertDistinct(parameters)
        parameters
      }
  }

  it("should contain schemas") {
    checkSwaggerJson { json =>
      val numSchemas = getKey(json, "components")
        .flatMap(components => getKey(components, "schemas"))
        .flatMap(getLength)
      numSchemas.isEmpty shouldBe false
      numSchemas.get should be > 20
    }
  }

  it("should not contain lots of List* schemas") {
    checkSwaggerJson { json =>
      val listSchemas = getKey(json, "components")
        .flatMap(components => getKey(components, "schemas"))
        .map { schemas =>
          getKeys(schemas).filter(key => key.startsWith("List"))
        }
      listSchemas.isEmpty shouldBe false
      listSchemas.get shouldBe Nil
    }
  }

  it("should not contain lots of Display* schemas") {
    checkSwaggerJson { json =>
      val listSchemas = getKey(json, "components")
        .flatMap(components => getKey(components, "schemas"))
        .map { schemas =>
          getKeys(schemas).filter(key => key.startsWith("Display"))
        }
      listSchemas.isEmpty shouldBe false
      listSchemas.get shouldBe Nil
    }
  }

  it("should contain aggregation schemas") {
    checkSwaggerJson { json =>
      val schemas = getKey(json, "components")
        .flatMap(components => getKey(components, "schemas"))
        .map(getKeys)
      schemas.isEmpty shouldBe false
      schemas.get should contain allOf (
        "GenreAggregation",
        "FormatAggregation",
        "PeriodAggregation",
        "SubjectAggregation",
        "LanguageAggregation",
        "GenreAggregationBucket",
        "FormatAggregationBucket",
        "PeriodAggregationBucket",
        "SubjectAggregationBucket",
        "LanguageAggregationBucket",
      )
    }
  }

  describe("includes bounds for the pageSize parameter") {
    it("images endpoint") {
      checkBoundsOnPagesizeParameters(imagesEndpoint)
    }

    it("works endpoint") {
      checkBoundsOnPagesizeParameters(worksEndpoint)
    }

    def checkBoundsOnPagesizeParameters(endpointString: String): Unit = {
      checkSwaggerJson { json =>
        val endpoint = getEndpoint(json, endpointString = endpointString)

        val pageSizeParam = getParameter(endpoint, name = "pageSize").get

        val schema = getKey(pageSizeParam, key = "schema").get

        getNumericKey(schema, key = "minimum") shouldBe Some(
          PaginationLimits.minSize)
        getNumericKey(schema, key = "maximum") shouldBe Some(
          PaginationLimits.maxSize)
        getNumericKey(schema, key = "default") shouldBe Some(10)
        getKey(schema, key = "type").flatMap { _.asString } shouldBe Some(
          "integer")
      }
    }
  }

  describe("includes bounds for the page parameter") {
    it("images endpoint") {
      checkPageParameter(imagesEndpoint)
    }

    it("works endpoint") {
      checkPageParameter(worksEndpoint)
    }

    def checkPageParameter(endpointString: String): Unit = {
      checkSwaggerJson { json =>
        val endpoint = getEndpoint(json, endpointString = endpointString)

        val pageSizeParam = getParameter(endpoint, name = "page").get

        val schema = getKey(pageSizeParam, key = "schema").get

        getNumericKey(schema, key = "minimum") shouldBe Some(1)
        getNumericKey(schema, key = "default") shouldBe Some(1)
        getKey(schema, key = "type").flatMap { _.asString } shouldBe Some(
          "integer")
      }
    }
  }

  //. We write this test for this specific parameter as it's used by the frontend
  it("should contain `_queryType parameter with valid `allowedValues`") {
    checkSwaggerJson { json =>
      val _queryType =
        getParameter(getEndpoint(json, worksEndpoint), "_queryType")

      val queryTypeAllowedValues =
        _queryType.get.hcursor
          .downField("schema")
          .get[List[String]]("enum")
          .toOption
          .getOrElse(List())

      queryTypeAllowedValues should contain theSameElementsAs SearchQueryType.allowed
        .map(_.name)
    }
  }

  private def assertDistinct(values: Seq[Any]) =
    assert(
      values.distinct.size == values.size,
      s"Duplicates: ${values.filter { v => values.count(v == _) > 1 }.distinct}"
    )

  private def checkSwaggerJson[T](f: Json => T): T =
    withApi { routes =>
      Get(s"/$apiPrefix/swagger.json") ~> routes ~> check {
        status shouldEqual Status.OK
        contentType shouldEqual ContentTypes.`application/json`
        f(parseJson(responseAs[String]))
      }
    }
}
