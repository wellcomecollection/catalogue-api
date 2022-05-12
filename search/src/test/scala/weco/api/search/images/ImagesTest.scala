package weco.api.search.images

import weco.api.search.models.request.SingleImageIncludes

class ImagesTest extends ApiImagesTestBase {
  it("returns a list of images") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(
          imagesIndex, (0 to 6).map(i => s"images.different-licenses.$i"): _*
        )

        assertJsonResponse(routes, path = s"$rootPath/images") {
          Status.OK -> newImagesListResponse(ids = (0 to 6).map(i => s"images.different-licenses.$i"))
        }
    }
  }

  it("returns a single image when requested with ID") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(
          imagesIndex, (0 to 6).map(i => s"images.different-licenses.$i"): _*
        )

        assertJsonResponse(routes, path = s"$rootPath/images/5vamzxd8") {
          Status.OK ->
            getDisplayImage("images.different-licenses.0")
              .withIncludes(SingleImageIncludes.none)
              .noSpaces
        }
    }
  }

  it("returns only linked images when a source work ID is requested") {
    val workImages = (0 to 3).map(i => s"images.examples.linked-with-the-same-work.$i")

    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(
          imagesIndex, workImages :+ "images.examples.linked-with-another-work": _*,
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/images?query=cg1whgjz"
        ) {
          Status.OK -> newImagesListResponse(workImages)
        }
        assertJsonResponse(
          routes,
          path = s"$rootPath/images?query=ihnjwgMtGL"
        ) {
          Status.OK -> newImagesListResponse(workImages)
        }
        assertJsonResponse(
          routes,
          path = s"$rootPath/images?query=lSY3iydrQH"
        ) {
          Status.OK -> newImagesListResponse(workImages)
        }
    }
  }



  it("returns matching results when using work data") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(
          imagesIndex,
          "images.examples.bread-baguette",
          "images.examples.bread-focaccia",
          "images.examples.bread-mantou",
        )

        assertJsonResponse(routes, path = s"$rootPath/images?query=bread") {
          Status.OK -> newImagesListResponse(
            ids = List("images.examples.bread-baguette",
              "images.examples.bread-focaccia",
              "images.examples.bread-mantou"),
            strictOrdering = true
          )
        }
        assertJsonResponse(routes, path = s"$rootPath/images?query=focaccia") {
          Status.OK -> newImagesListResponse(ids = List("images.examples.bread-focaccia"))
        }
    }
  }

  it("returns matching results when using workdata from the redirected work") {
    withImagesApi {
      case (imagesIndex, routes) =>
        indexTestImages(
          imagesIndex,
          "images.examples.bread-baguette",
          "images.examples.bread-focaccia",
          "images.examples.bread-schiacciata",
        )

        assertJsonResponse(routes, s"$rootPath/images?query=bread") {
          Status.OK -> newImagesListResponse(
            ids = List("images.examples.bread-schiacciata", "images.examples.bread-baguette",
              "images.examples.bread-focaccia"),
            strictOrdering = true
          )
        }

        assertJsonResponse(routes, s"$rootPath/images?query=schiacciata") {
          Status.OK -> newImagesListResponse(
            ids = List(
              "images.examples.bread-schiacciata")
          )
        }
    }
  }
}
