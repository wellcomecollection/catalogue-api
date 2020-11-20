package uk.ac.wellcome.platform.api

import io.circe.Json
import uk.ac.wellcome.platform.api.works.ApiWorksTestBase

trait JsonHelpers extends ApiWorksTestBase {
  protected def getParameter(endpoint: Json, name: String): Option[Json] =
    getKey(endpoint, "parameters")
      .flatMap(_.asArray)
      .flatMap(
        _.toList.find(getKey(_, "name").flatMap(_.asString).contains(name))
      )

  protected def getEndpoint(json: Json, endpointString: String): Json = {
    val endpoint = getKey(json, "paths")
      .flatMap(paths => getKey(paths, endpointString))
      .flatMap(path => getKey(path, "get"))

    endpoint.isEmpty shouldBe false
    endpoint.get
  }

  protected def getKeys(json: Json): List[String] =
    json.arrayOrObject(
      Nil,
      _ => Nil,
      obj => obj.keys.toList
    )

  protected def getKey(json: Json, key: String): Option[Json] =
    json.arrayOrObject(
      None,
      _ => None,
      obj => obj.toMap.get(key)
    )

  protected def getLength(json: Json): Option[Int] =
    json.arrayOrObject(
      None,
      arr => Some(arr.length),
      obj => Some(obj.keys.toList.length)
    )

  protected def getNumericKey(json: Json, key: String): Option[Int] =
    getKey(json, key = key)
      .flatMap { _.asNumber }
      .flatMap { _.toInt }
}
