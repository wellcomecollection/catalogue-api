package weco.api.search.json

import io.circe.Json

object JsonOps {
  implicit class ImplicitJsonOps(j: Json) {

    /** Remove a key from the JSON if the JSON is an object and contains the key,
      * do nothing otherwise.
      *
      * This only removes keys at the top level.
      *
      * e.g.
      *
      *     removeKey({"name": "square", "colour": "red"}, "colour")
      *     ~> {"name": "square"}
      *
      *     removeKey({"name": "square", "colour": "red"}, "sides")
      *     ~> {"name": "square", "colour": "red"}
      *
      *     removeKey("blue triangle", "sides")
      *     ~> "blue triangle"
      */
    def removeKey(key: String): Json =
      j.mapObject(_.remove(key))

    /** Removes a key from anywhere in a block of JSON, even if it's deeply nested.
      *
      * e.g.
      *
      *     removeKey({"name": "square", "colour": {"red": 255, "green": 0, "blue": 0}}, "blue")
      *     ~> {"name": "square", "colour": {"red": 255, "green": 0}}
      */
    def removeKeyRecursively(key: String): Json =
      j.removeKey(key)
        .mapArray(jsonArr => jsonArr.map(v => v.removeKeyRecursively(key)))
        .mapObject(jsonObj =>
          jsonObj.mapValues(v => v.removeKeyRecursively(key))
        )
  }
}
