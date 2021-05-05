package uk.ac.wellcome.platform.api.search.services

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.{ElasticError, Index}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.{EitherValues, OptionValues}
import uk.ac.wellcome.platform.api.search.models.{QueryConfig, SimilarityMetric}
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.index.IndexFixtures
import weco.api.search.elasticsearch.ElasticsearchService
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
            .findById(id = image.state.canonicalId)(index)) {
          _.right.value.value shouldBe image
        }
      }
    }

    it("returns a None if no image can be found") {
      withLocalImagesIndex { index =>
        whenReady(
          imagesService
            .findById(createCanonicalId)(index)) {
          _.right.value shouldBe None
        }
      }
    }

    it("returns a Left[ElasticError] if Elasticsearch returns an error") {
      whenReady(
        imagesService
          .findById(createCanonicalId)(Index("parsnips"))) {
        _.left.value shouldBe a[ElasticError]
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
            .retrieveSimilarImages(index, images.head)) { results =>
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
              images.head,
              similarityMetric = SimilarityMetric.Features)) { results =>
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
              images.head,
              similarityMetric = SimilarityMetric.Colors)) { results =>
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
            images.head,
            similarityMetric = SimilarityMetric.Colors)
        val blendedResultsFuture = imagesService
          .retrieveSimilarImages(
            index,
            images.head,
            similarityMetric = SimilarityMetric.Blended)
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

        whenReady(imagesService.retrieveSimilarImages(index, image)) {
          _ shouldBe empty
        }
      }
    }

    it("returns Nil when Elasticsearch returns an error") {
      whenReady(
        imagesService
          .retrieveSimilarImages(
            Index("doesn't exist"),
            createImageData.toIndexedImage)) {
        _ shouldBe empty
      }
    }
  }
}
