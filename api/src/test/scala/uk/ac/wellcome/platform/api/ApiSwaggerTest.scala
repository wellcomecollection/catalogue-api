package uk.ac.wellcome.platform.api

import org.scalatest.Matchers
import akka.http.scaladsl.model.ContentTypes
import io.circe.Json

import uk.ac.wellcome.platform.api.works.v2.ApiV2WorksTestBase

class ApiSwaggerTest extends ApiV2WorksTestBase with Matchers {

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

  it("should contain single work endpoint in paths") {
    checkSwaggerJson { json =>
      val endpoint = getKey(json, "paths")
        .flatMap(paths => getKey(paths, "/works/:id"))
        .flatMap(path => getKey(path, "get"))
      endpoint.isEmpty shouldBe false
      getKey(endpoint.get, "description").isEmpty shouldBe false
      getKey(endpoint.get, "summary").isEmpty shouldBe false
      val numParams = getKey(endpoint.get, "parameters")
        .flatMap(params => getLength(params))
      numParams shouldBe Some(2)
    }
  }

  it("should contain multiple work endpoints in paths") {
    checkSwaggerJson { json =>
      val endpoint = getKey(json, "paths")
        .flatMap(paths => getKey(paths, "/works"))
        .flatMap(path => getKey(path, "get"))
      endpoint.isEmpty shouldBe false
      getKey(endpoint.get, "description").isEmpty shouldBe false
      getKey(endpoint.get, "summary").isEmpty shouldBe false
      val numParams = getKey(endpoint.get, "parameters")
        .flatMap(params => getLength(params))
      numParams shouldBe Some(11)
    }
  }

  it("should contain schemas") {
    checkSwaggerJson { json =>
      val numSchemas = getKey(json, "components")
        .flatMap(components => getKey(components, "schemas"))
        .flatMap(getLength)
      numSchemas.isEmpty shouldBe false
      numSchemas.get should be > 40
    }
  }

  private def getKey(json: Json, key: String): Option[Json] =
    json.arrayOrObject(
      None,
      _ => None,
      obj => obj.toMap.get(key)
    )

  private def getLength(json: Json): Option[Int] =
    json.arrayOrObject(
      None,
      arr => Some(arr.length),
      obj => Some(obj.keys.toList.length)
    )

  private def checkSwaggerJson(f: Json => Unit) =
    withApi {
      case (indexV2, routes) =>
        Get(s"/$apiPrefix/swagger.json") ~> routes ~> check {
          status shouldEqual Status.OK
          contentType shouldEqual ContentTypes.`application/json`
          f(parseJson(responseAs[String]).toOption.get)
        }
    }
}
