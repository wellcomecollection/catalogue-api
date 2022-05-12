package weco.api.search.services

import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.{EitherValues, OptionValues}
import weco.api.search.elasticsearch.{DocumentNotFoundError, ElasticsearchService, IndexNotFoundError}
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.models.index.IndexedImage
import weco.api.search.models.{QueryConfig, SimilarityMetric}
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.index.IndexFixtures

import scala.concurrent.ExecutionContext.Implicits.global

class ImagesServiceTest
    extends AnyFunSpec
    with ScalaFutures
    with IndexFixtures
    with EitherValues
    with OptionValues
    with IdentifiersGenerators
    with TestDocumentFixtures {

  val elasticsearchService = new ElasticsearchService(elasticClient)

  val imagesService = new ImagesService(
    elasticsearchService,
    QueryConfig(
      paletteBinSizes = Seq(Seq(4, 6, 9), Seq(2, 4, 6), Seq(1, 3, 5)),
      paletteBinMinima = Seq(0f, 10f / 256, 10f / 256)
    )
  )

  describe("findById") {
    it("fetches an Image by ID") {
      withLocalImagesIndex { index =>
        indexTestImages(index, "images.everything")

        val expectedImage = IndexedImage(display = getDisplayImage("images.everything"))

        val future = imagesService.findById(id = CanonicalId("ggpvgjra"))(index)
        val actualImage = whenReady(future) {
          _.right.value
        }

        expectedImage shouldBe actualImage
      }
    }

    it("returns a DocumentNotFoundError if no image can be found") {
      withLocalImagesIndex { index =>
        val id = createCanonicalId
        val future = imagesService.findById(id)(index)

        whenReady(future) {
          _ shouldBe Left(DocumentNotFoundError(id))
        }
      }
    }

    it("returns an ElasticsearchError if Elasticsearch returns an error") {
      val index = createIndex
      val future = imagesService.findById(createCanonicalId)(index)

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
        indexTestImages(
          index,
          (0 to 5).map(i => s"images.similar-features-and-palettes.$i"): _*
        )

        val expectedImages = (1 to 5).map(i =>
          IndexedImage(display = getDisplayImage(s"images.similar-features-and-palettes.$i"))
        )

        val future = imagesService.retrieveSimilarImages(index, imageId = "fxlggzx3")

        whenReady(future) {
          _ shouldBe expectedImages
        }
      }
    }

    it("gets images with similar features") {
      withLocalImagesIndex { index =>
        indexTestImages(
          index,
          (0 to 5).map(i => s"images.similar-features.$i"): _*
        )

        val expectedImages = (1 to 5).map(i =>
          IndexedImage(display = getDisplayImage(s"images.similar-features.$i"))
        )

        val future =
          imagesService
            .retrieveSimilarImages(
              index,
              imageId = "1bxltcv6",
              similarityMetric = SimilarityMetric.Features
            )

        whenReady(future) {
          _ should contain theSameElementsAs expectedImages
        }
      }
    }

    it("gets images with similar color palettes") {
      withLocalImagesIndex { index =>
        indexTestImages(
          index,
          (0 to 5).map(i => s"images.similar-palettes.$i"): _*
        )

        val expectedImages = (1 to 5).map(i =>
          IndexedImage(display = getDisplayImage(s"images.similar-palettes.$i"))
        )

        val future =
          imagesService
            .retrieveSimilarImages(
              index,
              imageId = "tsmrwj5f",
              similarityMetric = SimilarityMetric.Colors
            )

        whenReady(future) {
          _ should contain theSameElementsAs expectedImages
        }
      }
    }

    it("does not blend similarity metrics when specific ones are requested") {
      withLocalImagesIndex { index =>
        indexTestImages(
          index,
          (0 to 5).map(i => s"images.similar-features-and-palettes.$i"): _*
        )

        val colorResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            imageId = "fxlggzx3",
            similarityMetric = SimilarityMetric.Colors
          )
        val blendedResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            imageId = "fxlggzx3",
            similarityMetric = SimilarityMetric.Blended
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
        indexTestImages(index, "images.everything")

        whenReady(imagesService.retrieveSimilarImages(index, imageId = "ggpvgjra")) {
          _ shouldBe empty
        }
      }
    }

    it("returns Nil when Elasticsearch returns an error") {
      val future =
        imagesService
          .retrieveSimilarImages(
            Index("doesn't exist"),
            imageId = createCanonicalId.underlying
          )

      whenReady(future) {
        _ shouldBe empty
      }
    }
  }
}
