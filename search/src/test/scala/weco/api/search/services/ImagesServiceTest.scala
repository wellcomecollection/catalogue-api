package weco.api.search.services

import com.sksamuel.elastic4s.Index
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import weco.api.search.elasticsearch.{
  DocumentNotFoundError,
  ElasticsearchService,
  IndexNotFoundError
}
import weco.api.search.fixtures.{IndexFixtures, TestDocumentFixtures}
import weco.api.search.models.index.IndexedImage
import weco.api.search.models.SimilarityMetric

import scala.concurrent.ExecutionContext.Implicits.global

class ImagesServiceTest
    extends AnyFunSpec
    with ScalaFutures
    with IndexFixtures
    with EitherValues
    with TestDocumentFixtures {

  val elasticsearchService = new ElasticsearchService(elasticClient)

  val imagesService = new ImagesService(elasticsearchService)

  describe("findById") {
    it("fetches an Image by ID") {
      withLocalImagesIndex { index =>
        indexTestDocuments(index, "images.everything")

        val expectedImage =
          IndexedImage(
            display = getDisplayImage("images.everything"),
            query = getQueryImage("images.everything")
          )

        val future = imagesService.findById(
          id = getTestImageId("images.everything")
        )(index)
        val actualImage = whenReady(future) {
          _.right.value
        }

        expectedImage shouldEqual actualImage
      }
    }

    it("returns a DocumentNotFoundError if no image can be found") {
      withLocalImagesIndex { index =>
        val id = "nopenope"
        val future = imagesService.findById(id)(index)

        whenReady(future) {
          _ shouldBe Left(DocumentNotFoundError(id))
        }
      }
    }

    it("returns an ElasticsearchError if Elasticsearch returns an error") {
      val index = createIndex
      val id = "nopenope"
      val future = imagesService.findById(id)(index)

      whenReady(future) { err =>
        err.left.value shouldBe a[IndexNotFoundError]
        err.left.value
          .asInstanceOf[IndexNotFoundError]
          .index shouldBe index.name
      }
    }
  }

  describe("retrieveSimilarImages") {
    it("gets images using a blended similarity metric by default") {
      withLocalImagesIndex { index =>
        indexTestDocuments(
          index,
          (0 to 5).map(i => s"images.similar-features-and-palettes.$i"): _*
        )

        val expectedImages = (1 to 5).map(
          i =>
            IndexedImage(
              display =
                getDisplayImage(s"images.similar-features-and-palettes.$i"),
              query = getQueryImage(s"images.similar-features-and-palettes.$i")
            )
        )

        val future =
          imagesService.retrieveSimilarImages(
            index,
            imageId = getTestImageId("images.similar-features-and-palettes.0"),
            image = IndexedImage(
              display =
                getDisplayImage(s"images.similar-features-and-palettes.0"),
              query = getQueryImage(s"images.similar-features-and-palettes.0")
            ),
            minScore = Some(0)
          )

        whenReady(future) {
          _ should contain theSameElementsAs expectedImages
        }
      }
    }

    it("gets images with similar features") {
      withLocalImagesIndex { index =>
        indexTestDocuments(
          index,
          (0 to 5).map(i => s"images.similar-features.$i"): _*
        )

        val expectedImages = (1 to 5).map(
          i =>
            IndexedImage(
              display = getDisplayImage(s"images.similar-features.$i"),
              query = getQueryImage(s"images.similar-features.$i")
            )
        )

        val future =
          imagesService
            .retrieveSimilarImages(
              index,
              imageId = getTestImageId("images.similar-features.0"),
              image = IndexedImage(
                display = getDisplayImage(s"images.similar-features.0"),
                query = getQueryImage(s"images.similar-features.0")
              ),
              similarityMetric = SimilarityMetric.Features,
              minScore = Some(0)
            )

        whenReady(future) {
          _ should contain theSameElementsAs expectedImages
        }
      }
    }

    it("gets images with similar color palettes") {
      withLocalImagesIndex { index =>
        indexTestDocuments(
          index,
          (0 to 5).map(i => s"images.similar-palettes.$i"): _*
        )

        val expectedImages = (1 to 5).map(
          i =>
            IndexedImage(
              display = getDisplayImage(s"images.similar-palettes.$i"),
              query = getQueryImage(s"images.similar-palettes.$i")
            )
        )

        val future =
          imagesService
            .retrieveSimilarImages(
              index,
              imageId = getTestImageId("images.similar-palettes.0"),
              image = IndexedImage(
                display = getDisplayImage(s"images.similar-palettes.0"),
                query = getQueryImage(s"images.similar-palettes.0")
              ),
              similarityMetric = SimilarityMetric.Colors,
              minScore = Some(0)
            )

        whenReady(future) {
          _ should contain theSameElementsAs expectedImages
        }
      }
    }

    it("does not blend similarity metrics when specific ones are requested") {
      withLocalImagesIndex { index =>
        indexTestDocuments(
          index,
          (0 to 5).map(i => s"images.similar-features-and-palettes.$i"): _*
        )

        val colorResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            imageId = getTestImageId("images.similar-features-and-palettes.0"),
            image = IndexedImage(
              display =
                getDisplayImage(s"images.similar-features-and-palettes.0"),
              query = getQueryImage(s"images.similar-features-and-palettes.0")
            ),
            similarityMetric = SimilarityMetric.Colors,
            minScore = Some(0)
          )
        val blendedResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            imageId = getTestImageId("images.similar-features-and-palettes.0"),
            image = IndexedImage(
              display =
                getDisplayImage(s"images.similar-features-and-palettes.0"),
              query = getQueryImage(s"images.similar-features-and-palettes.0")
            ),
            similarityMetric = SimilarityMetric.Blended,
            minScore = Some(0)
          )
        whenReady(colorResultsFuture) { colorResults =>
          whenReady(blendedResultsFuture) { blendedResults =>
            colorResults should not contain
              theSameElementsInOrderAs(blendedResults)
          }
        }
      }
    }

    it("returns Nil when no visually similar images can be found") {
      withLocalImagesIndex { index =>
        indexTestDocuments(index, "images.everything")

        whenReady(
          imagesService.retrieveSimilarImages(
            index,
            imageId = getTestImageId("images.everything"),
            image = IndexedImage(
              display = getDisplayImage("images.everything"),
              query = getQueryImage("images.everything")
            ),
            minScore = Some(0)
          )
        ) {
          _ shouldBe empty
        }
      }
    }

    it("returns Nil when Elasticsearch returns an error") {
      val future =
        imagesService
          .retrieveSimilarImages(
            Index("doesn't exist"),
            imageId = "nopenope",
            image = IndexedImage(
              display = getDisplayImage("images.everything"),
              query = getQueryImage("images.everything")
            ),
            minScore = Some(0)
          )

      whenReady(future) {
        _ shouldBe empty
      }
    }
  }
}
