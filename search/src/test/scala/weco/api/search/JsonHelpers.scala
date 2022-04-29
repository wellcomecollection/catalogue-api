package weco.api.search

import io.circe.Json
import org.scalatest.matchers.should.Matchers

trait JsonHelpers extends Matchers {
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
}
