package weco.api.search.images

import weco.catalogue.internal_model.Implicits._

class ImagesFiltersTest extends ApiImagesTestBase {
  describe("filtering images by license") {
    it("filters by license") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 6).map(i => s"images.different-licenses.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?locations.license=cc-by"
          ) {
            Status.OK -> newImagesListResponse(ids = (0 to 4).map(i => s"images.different-licenses.$i"))
          }
      }
    }
  }

  describe("filtering images by source contributors") {
    it("filters by contributors from the canonical source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 2).map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"""$rootPath/images?source.contributors.agent.label="Machiavelli,%20Niccolo""""
          ) {
            Status.OK -> newImagesListResponse(ids = Seq("images.examples.contributor-filter-tests.0"))
          }
      }
    }

    it("does not filter by contributors from the redirected source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 2).map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.contributors.agent.label=Hypatia"
          ) {
            Status.OK -> emptyJsonResult
          }
      }
    }

    it("filters by multiple contributors") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 2).map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"""$rootPath/images?source.contributors.agent.label="Machiavelli,%20Niccolo",Edward%20Said"""
          ) {
            Status.OK -> newImagesListResponse(
              List("images.examples.contributor-filter-tests.0", "images.examples.contributor-filter-tests.1")
            )
          }
      }
    }
  }

  describe("filtering images by source genres") {
    it("filters by genres from the canonical source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.label=Carrot%20counselling"
          ) {
            Status.OK -> newImagesListResponse(ids = Seq("images.examples.genre-filter-tests.0"))
          }
      }
    }

    it("does not filter by genres from the redirected source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.label=Dodo%20divination"
          ) {
            Status.OK -> emptyJsonResult
          }
      }
    }

    it("filters by multiple genres") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex, (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.label=Carrot%20counselling,Emu%20entrepreneurship"
          ) {
            Status.OK -> newImagesListResponse(ids =
              Seq(
                "images.examples.genre-filter-tests.0",
                "images.examples.genre-filter-tests.2"
              )
            )
          }
      }
    }
  }

  describe("filtering images by color") {
    val redImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "7/0",
            "7/0",
            "7/0",
            "71/1",
            "71/1",
            "71/1",
            "268/2",
            "268/2",
            "268/2"
          )
        )
      )
    )
    val blueImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "9/0",
            "9/0",
            "9/0",
            "5/0",
            "74/1",
            "74/1",
            "74/1",
            "35/1",
            "50/1",
            "29/1",
            "38/1",
            "273/2",
            "273/2",
            "273/2",
            "187/2",
            "165/2",
            "115/2",
            "129/2"
          )
        )
      )
    )
    val slightlyLessRedImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "7/0",
            "71/1",
            "71/1",
            "71/1"
          )
        )
      )
    )
    val evenLessRedImage = createImageData.toIndexedImageWith(
      inferredData = createInferredData.map(
        _.copy(
          palette = List(
            "7/0",
            "7/0",
            "7/0"
          )
        )
      )
    )

    it("filters by color") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(imagesIndex, redImage, blueImage)
          assertJsonResponse(routes, f"$rootPath/images?color=ff0000") {
            Status.OK -> imagesListResponse(
              images = Seq(redImage)
            )
          }
      }
    }

    it("filters by multiple colors") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(imagesIndex, redImage, blueImage)
          assertJsonResponse(
            routes,
            f"$rootPath/images?color=ff0000,0000ff",
            unordered = true
          ) {
            Status.OK -> imagesListResponse(
              images = Seq(blueImage, redImage)
            )
          }
      }
    }

    it("scores by number of color bin matches") {
      withImagesApi {
        case (imagesIndex, routes) =>
          insertImagesIntoElasticsearch(
            imagesIndex,
            redImage,
            slightlyLessRedImage,
            evenLessRedImage,
            blueImage
          )
          assertJsonResponse(routes, f"$rootPath/images?color=ff0000") {
            Status.OK -> imagesListResponse(
              images = Seq(redImage, slightlyLessRedImage, evenLessRedImage)
            )
          }
      }
    }
  }
}
