package weco.api.search.json

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.models.request.{WorkInclude, WorksIncludes}

class CatalogueJsonUtilTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with CatalogueJsonUtil
    with TableDrivenPropertyChecks {

  // This is a blob of JSON that includes a field with a particular name, including
  // at the top level, nested in an array, and nested in an object.
  def nestedJsonWithField(fieldName: String): Json =
    parse(
      s"""
        {
          "$fieldName": ["V00123", "b12345"],
          "name": "A picture of a cat",
          "arrayField": [
            {
              "$fieldName": ["V00123-1"],
              "name": "The first image"
            },
            {
              "$fieldName": ["V00123-2"],
              "name": "The second image"
            }
          ],
          "objectField": {
            "V00456": {
              "$fieldName": ["V00456", "b56789"],
              "name": "A picture of a dog"
            }
          }
        }
        """
    ).right.get

  def nestedJsonWithoutField: Json =
    parse(
      """
        {
          "name": "A picture of a cat",
          "arrayField": [
            {
              "name": "The first image"
            },
            {
              "name": "The second image"
            }
          ],
          "objectField": {
            "V00456": {
              "name": "A picture of a dog"
            }
          }
        }
        """
    ).right.get

  def jsonWithField(fieldName: String): Json =
    parse(
      s"""
        {
          "$fieldName": ["V00123", "b12345"],
          "name": "A picture of a fish"
        }
        """
    ).right.get

  def jsonWithoutField: Json =
    parse(
      """
        {
          "name": "A picture of a fish"
        }
        """
    ).right.get

  val testCases = Table(
    ("fieldName", "workInclude"),
    ("identifiers", WorkInclude.Identifiers),
    ("items", WorkInclude.Items),
    ("holdings", WorkInclude.Holdings),
    ("subjects", WorkInclude.Subjects),
    ("genres", WorkInclude.Genres),
    ("contributors", WorkInclude.Contributors),
    ("production", WorkInclude.Production),
    ("languages", WorkInclude.Languages),
    ("notes", WorkInclude.Notes),
    ("images", WorkInclude.Images),
    ("parts", WorkInclude.Parts),
    ("partOf", WorkInclude.PartOf),
    ("precededBy", WorkInclude.PrecededBy),
    ("succeededBy", WorkInclude.SucceededBy)
  )

  describe("WorkJsonOps") {

    // Note: identifiers are a special case because we expect them to be nested
    // throughout the Work JSON; we don't expect other fields to appear anywhere
    // but at the top level.
    //
    // What to do if we see, e.g. "items" in a nested position, is undefined.
    // We don't know what the correct behaviour is (because it doesn't occur)
    // so we don't test it.
    it("includes/omits identifiers based on the work include") {
      nestedJsonWithField("identifiers").withIncludes(
        WorksIncludes(WorkInclude.Identifiers)
      ) shouldBe nestedJsonWithField("identifiers")

      nestedJsonWithField("identifiers").withIncludes(WorksIncludes.none) shouldBe nestedJsonWithoutField
    }

    it("includes/omits fields based on the work include") {
      forAll(testCases) {
        case (fieldName, fieldInclude) =>
          withClue(s"includes $fieldName if the include is present") {
            val includes = WorksIncludes(fieldInclude)

            jsonWithField(fieldName).withIncludes(includes) shouldBe jsonWithField(
              fieldName
            )
          }

          withClue(s"omits $fieldName if the include is missing") {
            val includes = WorksIncludes.none

            jsonWithField(fieldName).withIncludes(includes) shouldBe jsonWithoutField
          }
      }
    }

    it("omits all the fields which aren't included") {
      val startingJson = parse(
        s"""
        {
          "name": "An engraving of a fish",
          "items": [
            {
              "name": "The first item"
            },
            {
              "name": "The second item"
            }
          ],
          "holdings": [
            "The first holdings",
            "The second holdings"
          ],
          "subjects": [
            "marine life",
            "aquatics"
          ]
        }
        """
      ).right.get

      val expectedJson = parse(
        s"""
        {
          "name": "An engraving of a fish",
          "holdings": [
            "The first holdings",
            "The second holdings"
          ]
        }
        """
      ).right.get

      startingJson.withIncludes(WorksIncludes(WorkInclude.Holdings)) shouldBe expectedJson
    }
  }
}
