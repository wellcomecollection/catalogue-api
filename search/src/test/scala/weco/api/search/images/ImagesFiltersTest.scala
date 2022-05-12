package weco.api.search.images

class ImagesFiltersTest extends ApiImagesTestBase {
  describe("filtering images by license") {
    it("filters by license") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            (0 to 6).map(i => s"images.different-licenses.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?locations.license=cc-by"
          ) {
            Status.OK -> newImagesListResponse(
              ids = (0 to 4).map(i => s"images.different-licenses.$i")
            )
          }
      }
    }
  }

  describe("filtering images by source contributors") {
    it("filters by contributors from the canonical source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            (0 to 2)
              .map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path =
              s"""$rootPath/images?source.contributors.agent.label="Machiavelli,%20Niccolo""""
          ) {
            Status.OK -> newImagesListResponse(
              ids = Seq("images.examples.contributor-filter-tests.0")
            )
          }
      }
    }

    it("does not filter by contributors from the redirected source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            (0 to 2)
              .map(i => s"images.examples.contributor-filter-tests.$i"): _*
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
            imagesIndex,
            (0 to 2)
              .map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path =
              s"""$rootPath/images?source.contributors.agent.label="Machiavelli,%20Niccolo",Edward%20Said"""
          ) {
            Status.OK -> newImagesListResponse(
              List(
                "images.examples.contributor-filter-tests.0",
                "images.examples.contributor-filter-tests.1"
              )
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
            imagesIndex,
            (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.label=Carrot%20counselling"
          ) {
            Status.OK -> newImagesListResponse(
              ids = Seq("images.examples.genre-filter-tests.0")
            )
          }
      }
    }

    it("does not filter by genres from the redirected source work") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
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
            imagesIndex,
            (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images?source.genres.label=Carrot%20counselling,Emu%20entrepreneurship"
          ) {
            Status.OK -> newImagesListResponse(
              ids = Seq(
                "images.examples.genre-filter-tests.0",
                "images.examples.genre-filter-tests.2"
              )
            )
          }
      }
    }
  }

  describe("filtering images by color") {
    it("filters by color") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            "images.examples.color-filter-tests.red",
            "images.examples.color-filter-tests.blue"
          )

          assertJsonResponse(routes, path = f"$rootPath/images?color=ff0000") {
            Status.OK -> newImagesListResponse(
              ids = Seq("images.examples.color-filter-tests.red")
            )
          }
      }
    }

    it("filters by multiple colors") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            "images.examples.color-filter-tests.red",
            "images.examples.color-filter-tests.blue"
          )

          // TODO: This test would pass if it was returning every image.
          //
          // We should add a green image and check it gets filtered out correctly.
          //
          // See https://github.com/wellcomecollection/catalogue-api/issues/432
          assertJsonResponse(
            routes,
            path = f"$rootPath/images?color=ff0000,0000ff"
          ) {
            Status.OK -> newImagesListResponse(
              ids = Seq(
                "images.examples.color-filter-tests.red",
                "images.examples.color-filter-tests.blue"
              )
            )
          }
      }
    }

    it("scores by number of color bin matches") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestImages(
            imagesIndex,
            "images.examples.color-filter-tests.red",
            "images.examples.color-filter-tests.even-less-red",
            "images.examples.color-filter-tests.slightly-less-red",
            "images.examples.color-filter-tests.blue"
          )

          assertJsonResponse(routes, path = f"$rootPath/images?color=ff0000") {
            Status.OK -> newImagesListResponse(
              ids = Seq(
                "images.examples.color-filter-tests.red",
                "images.examples.color-filter-tests.slightly-less-red",
                "images.examples.color-filter-tests.even-less-red"
              ),
              strictOrdering = true
            )
          }
      }
    }
  }
}
