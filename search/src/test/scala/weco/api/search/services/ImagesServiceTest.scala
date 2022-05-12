package weco.api.search.services

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.{EitherValues, OptionValues}
import weco.api.search.models.QueryConfig
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.index.IndexFixtures
import weco.api.search.elasticsearch.{
  DocumentNotFoundError,
  ElasticsearchService,
  IndexNotFoundError
}
import weco.api.search.models.{QueryConfig, SimilarityMetric}
import weco.catalogue.internal_model.generators.ImageGenerators

class ImagesServiceTest
    extends AnyFunSpec
    with ScalaFutures
    with IndexFixtures
    with ImageGenerators
    with EitherValues
    with OptionValues {

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
        val image = createImageData.toIndexedImage
        insertImagesIntoElasticsearch(index, image)

        whenReady(
          imagesService
            .findById(id = image.state.canonicalId)(index)
        ) {
          _.right.value shouldBe image
        }
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
        val images =
          createSimilarImages(6, similarFeatures = true, similarPalette = true)
        insertImagesIntoElasticsearch(index, images: _*)

        whenReady(
          imagesService
            .retrieveSimilarImages(index, images.head.id)
        ) { results =>
          results should not be empty
          results should contain theSameElementsAs images.tail
        }
      }
    }

    it("gets images with similar features") {
      withLocalImagesIndex { index =>
        val images =
          createSimilarImages(6, similarFeatures = true, similarPalette = false)
        insertImagesIntoElasticsearch(index, images: _*)

        whenReady(
          imagesService
            .retrieveSimilarImages(
              index,
              images.head.id,
              similarityMetric = SimilarityMetric.Features
            )
        ) { results =>
          results should not be empty
          results should contain theSameElementsAs images.tail
        }
      }
    }

    it("gets images with similar color palettes") {
      withLocalImagesIndex { index =>
        val images =
          createSimilarImages(6, similarFeatures = false, similarPalette = true)
        insertImagesIntoElasticsearch(index, images: _*)

        whenReady(
          imagesService
            .retrieveSimilarImages(
              index,
              images.head.id,
              similarityMetric = SimilarityMetric.Colors
            )
        ) { results =>
          results should not be empty
          results should contain theSameElementsAs images.tail
        }
      }
    }

    it("does not blend similarity metrics when specific ones are requested") {
      withLocalImagesIndex { index =>
        val images =
          createSimilarImages(6, similarFeatures = true, similarPalette = false)
        insertImagesIntoElasticsearch(index, images: _*)

        val colorResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            images.head.id,
            similarityMetric = SimilarityMetric.Colors
          )
        val blendedResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            images.head.id,
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
        val image = createImageData.toIndexedImage
        insertImagesIntoElasticsearch(index, image)

        whenReady(imagesService.retrieveSimilarImages(index, image.id)) {
          _ shouldBe empty
        }
      }
    }

    it("returns Nil when Elasticsearch returns an error") {
      whenReady(
        imagesService
          .retrieveSimilarImages(
            Index("doesn't exist"),
            createImageData.toIndexedImage.id
          )
      ) {
        _ shouldBe empty
      }
    }
  }
}
