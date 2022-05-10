package weco.api.search.images

import weco.api.search.fixtures.TestDocumentFixtures
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.generators.GenreGenerators
import weco.catalogue.internal_model.locations.License
import weco.catalogue.internal_model.work._

class ImagesAggregationsTest extends ApiImagesTestBase with GenreGenerators with TestDocumentFixtures {
  it("aggregates by license") {
    val images = (0 to 6).map(i => s"images.different-licenses.$i")

    val displayImages = images.map(getDisplayImage(_).noSpaces)

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?aggregations=locations.license"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "license": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data" : ${license(License.CCBY)},
                      "count" : 5,
                      "type" : "AggregationBucket"
                    },
                    {
                      "data" : ${license(License.PDM)},
                      "count" : 2,
                      "type" : "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${displayImages.mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by the canonical source's contributor agent labels") {
    val carrots = Agent("carrots")
    val parrots = Organisation("parrots")
    val parrotsMeeting = Meeting("parrots")
    val rats = Person("rats")

    val images = List(
      createImageData.toIndexedImageWith(
        parentWork = identifiedWork()
          .contributors(List(carrots).map(Contributor(_, roles = Nil)))
      ),
      createImageData.toIndexedImageWith(
        parentWork = identifiedWork().contributors(
          List(carrots, parrots).map(Contributor(_, roles = Nil))
        ),
        redirectedWork = Some(
          identifiedWork().contributors(
            List(parrots, parrotsMeeting).map(Contributor(_, roles = Nil))
          )
        )
      ),
      createImageData.toIndexedImageWith(
        parentWork = identifiedWork().contributors(
          List(carrots, parrotsMeeting).map(Contributor(_, roles = Nil))
        ),
        redirectedWork = Some(
          identifiedWork()
            .contributors(List(rats).map(Contributor(_, roles = Nil)))
        )
      )
    )

    withImagesApi {
      case (imagesIndex, routes) =>
        insertImagesIntoElasticsearch(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          s"$rootPath/images?aggregations=source.contributors.agent.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "source.contributors.agent.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data": ${abstractAgent(carrots)},
                      "count": 3,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": ${abstractAgent(parrotsMeeting)},
                      "count": 1,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": ${abstractAgent(parrots)},
                      "count": 1,
                      "type": "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${images
            .sortBy { _.state.canonicalId }
            .map(imageResponse)
            .mkString(",")}
              ]
            }
          """
        }
    }
  }

  it("aggregates by the canonical source's genres") {
    val carrotCounselling =
      createGenreWith("Carrot counselling", concepts = Nil)
    val dodoDivination = createGenreWith("Dodo divination", concepts = Nil)
    val emuEntrepreneurship =
      createGenreWith("Emu entrepreneurship", concepts = Nil)
    val falconFinances = createGenreWith("Falcon finances", concepts = Nil)

    val carrotCounsellingImage = createImageData.toIndexedImageWith(
      parentWork = identifiedWork().genres(List(carrotCounselling))
    )
    val redirectedDodoDivinationImage = createImageData.toIndexedImageWith(
      redirectedWork = Some(identifiedWork().genres(List(dodoDivination)))
    )
    val carrotEmuFalconImage =
      createImageData.toIndexedImageWith(
        parentWork = identifiedWork().genres(
          List(emuEntrepreneurship, falconFinances, carrotCounselling)
        )
      )

    val images = List(
      carrotCounsellingImage,
      redirectedDodoDivinationImage,
      carrotEmuFalconImage
    )

    withImagesApi {
      case (imagesIndex, routes) =>
        insertImagesIntoElasticsearch(imagesIndex, images: _*)

        assertJsonResponse(
          routes,
          s"$rootPath/images?aggregations=source.genres.label"
        ) {
          Status.OK -> s"""
            {
              ${resultList(totalResults = images.size)},
              "aggregations": {
                "type" : "Aggregations",
                "source.genres.label": {
                  "type" : "Aggregation",
                  "buckets": [
                    {
                      "data": ${genre(carrotCounselling)},
                      "count": 2,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": ${genre(emuEntrepreneurship)},
                      "count": 1,
                      "type": "AggregationBucket"
                    },
                    {
                      "data": ${genre(falconFinances)},
                      "count": 1,
                      "type": "AggregationBucket"
                    }
                  ]
                }
              },
              "results": [
                ${images
            .sortBy { _.state.canonicalId }
            .map(imageResponse)
            .mkString(",")}
              ]
            }
          """
        }
    }
  }
}
