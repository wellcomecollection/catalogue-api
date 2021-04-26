package uk.ac.wellcome.display.test.util

import io.circe.Encoder
import org.scalatest.Assertion
import uk.ac.wellcome.json.utils.JsonAssertions
import weco.http.json.DisplayJsonUtil

trait JsonMapperTestUtil extends JsonAssertions {

  def assertObjectMapsToJson[T](value: T, expectedJson: String)(
    implicit encoder: Encoder[T]): Assertion =
    assertJsonStringsAreEqual(DisplayJsonUtil.toJson(value), expectedJson)
}
