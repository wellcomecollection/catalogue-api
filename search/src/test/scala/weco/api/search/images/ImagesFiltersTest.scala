package weco.api.search.images

import akka.http.scaladsl.server.Route
import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import weco.fixtures.TestWith

class ImagesFiltersTest extends AnyFunSpec with ApiImagesTestBase {
  describe("filtering images by license") {
    it("filters by license") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            (0 to 6).map(i => s"images.different-licenses.$i"): _*
          )

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?locations.license=cc-by"
          ) {
            Status.OK -> imagesListResponse(
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
          indexTestDocuments(
            imagesIndex,
            (0 to 2)
              .map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path =
              s"""$rootPath/images?source.contributors.agent.label="Machiavelli,%20Niccolo""""
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq("images.examples.contributor-filter-tests.0")
            )
          }
      }
    }

    it("filters by multiple contributors") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            (0 to 2)
              .map(i => s"images.examples.contributor-filter-tests.$i"): _*
          )

          assertJsonResponse(
            routes,
            path =
              s"""$rootPath/images?source.contributors.agent.label="Machiavelli,%20Niccolo",Edward%20Said"""
          ) {
            Status.OK -> imagesListResponse(
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
    def withGenreFilterRecords(
      testWith: TestWith[Route, Assertion]
    ): Assertion =
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            (0 to 2).map(i => s"images.examples.genre-filter-tests.$i"): _*
          )
          testWith(routes)
      }

    it("filters by genres from the canonical source work") {
      withGenreFilterRecords { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/images?source.genres.label=Carrot%20counselling"
        ) {
          Status.OK -> imagesListResponse(
            ids = Seq("images.examples.genre-filter-tests.0")
          )
        }
      }
    }

    it("does not filter by genres from the redirected source work") {
      withGenreFilterRecords { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/images?source.genres.label=Dodo%20divination"
        ) {
          Status.OK -> emptyJsonResult
        }
      }
    }

    it("filters by multiple genres") {
      withGenreFilterRecords { routes =>
        assertJsonResponse(
          routes,
          path =
            s"$rootPath/images?source.genres.label=Carrot%20counselling,Emu%20entrepreneurship"
        ) {
          Status.OK -> imagesListResponse(
            ids = Seq(
              "images.examples.genre-filter-tests.0",
              "images.examples.genre-filter-tests.2"
            )
          )
        }
      }
    }

    describe("filtering by genre concept ids") {
      it("does not apply the filter if there are no values provided") {
        withGenreFilterRecords { routes =>
          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.concepts="
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "images.examples.genre-filter-tests.0",
                "images.examples.genre-filter-tests.1",
                "images.examples.genre-filter-tests.2"
              )
            )
          }

        }
      }
      it("filters by one concept id") {
        withGenreFilterRecords { routes =>
          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.concepts=baadf00d"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "images.examples.genre-filter-tests.2"
              )
            )
          }

        }
      }
      it(
        "filters containing multiple concept ids return documents containing ANY of the requested ids"
      ) {
        withGenreFilterRecords { routes =>
          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.genres.concepts=g00dcafe,baadf00d"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "images.examples.genre-filter-tests.0",
                "images.examples.genre-filter-tests.2"
              )
            )
          }

        }
      }
    }

  }

  describe("filtering images by source subjects") {
    val images = Seq(
      "screwdrivers-1",
      "screwdrivers-2",
      "sounds",
      "squirrel,sample",
      "squirrel,screwdriver"
    ).map(s => s"images.subjects.$s")

    it("filters by subjects") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, images: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images?source.subjects.label=Simple%20screwdrivers"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "images.subjects.screwdrivers-1",
                "images.subjects.screwdrivers-2",
                "images.subjects.squirrel,screwdriver"
              )
            )
          }
      }
    }

    it("filters by multiple subjects") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, images: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images?source.subjects.label=Square%20sounds,Struck%20samples"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "images.subjects.sounds",
                "images.subjects.squirrel,sample"
              )
            )
          }
      }
    }
  }

  describe("filtering images by source date range") {
    val productionImages = Seq(
      "image-production.1098",
      "image-production.1900",
      "image-production.1904",
      "image-production.1976",
      "image-production.2020"
    )

    it("filters by date range") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, productionImages: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/images?source.production.dates.from=1900-01-01&source.production.dates.to=1960-01-01"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq("image-production.1900", "image-production.1904")
            )
          }
      }
    }

    it("filters by from date") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, productionImages: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.production.dates.from=1900-01-01"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "image-production.1900",
                "image-production.1904",
                "image-production.1976",
                "image-production.2020"
              )
            )
          }
      }
    }

    it("filters by to date") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(imagesIndex, productionImages: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/images?source.production.dates.to=1960-01-01"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "image-production.1098",
                "image-production.1900",
                "image-production.1904"
              )
            )
          }
      }
    }

    it("errors on invalid date") {
      // the one with valid test-documents
      withApi { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/images?source.production.dates.from=INVALID"
        ) {
          Status.BadRequest ->
            badRequest(
              "source.production.dates.from: Invalid date encoding. Expected YYYY-MM-DD"
            )
        }
      }
    }
  }

  describe("filtering images by color") {
    it("scores by nearest neighbour") {
      withImagesApi {
        case (imagesIndex, routes) =>
          indexTestDocuments(
            imagesIndex,
            "images.examples.color-filter-tests.red",
            "images.examples.color-filter-tests.even-less-red",
            "images.examples.color-filter-tests.slightly-less-red",
            "images.examples.color-filter-tests.blue"
          )

          assertJsonResponse(
            routes,
            path = f"$rootPath/images?color=FF0000"
          ) {
            Status.OK -> imagesListResponse(
              ids = Seq(
                "images.examples.color-filter-tests.red",
                "images.examples.color-filter-tests.slightly-less-red",
                "images.examples.color-filter-tests.even-less-red",
                "images.examples.color-filter-tests.blue"
              ),
              strictOrdering = true
            )
          }
      }
    }
  }

}
