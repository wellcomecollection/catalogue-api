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
import weco.api.search.fixtures.{
  ResilientElasticClientFixture,
  TestDocumentFixtures
}
import weco.api.search.models.ImageSearchOptions
import weco.api.search.models.index.IndexedImage

import scala.concurrent.ExecutionContext.Implicits.global

class ImagesServiceTest
    extends AnyFunSpec
    with ScalaFutures
    with ResilientElasticClientFixture
    with EitherValues
    with TestDocumentFixtures {

  val elasticsearchService = new ElasticsearchService(resilientElasticClient)

  val imagesService = new ImagesService(elasticsearchService)

  describe("findById") {
    it("fetches an Image by ID") {
      withLocalImagesIndex { index =>
        indexTestDocuments(index, "images.everything")

        val expectedImage =
          IndexedImage(
            display = getDisplayImage("images.everything"),
            vectorValues = getVectorValuesImage("images.everything")
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

  describe("listOrSearch") {
    it("returns the display document for each image") {
      withLocalImagesIndex { index =>
        val ids = (0 to 5).map(i => s"images.similar-features.$i")
        indexTestDocuments(index, ids: _*)

        val expectedImages = ids.map(
          id => IndexedImage.Display(display = getDisplayImage(id))
        )

        val future =
          imagesService.listOrSearch(index, ImageSearchOptions())

        whenReady(future) {
          _.right.value.results should contain theSameElementsAs expectedImages
        }
      }
    }

    // Asserts on the raw hit: the Display decoder ignores extra keys, so it
    // would not notice the feature vectors coming back.
    it("fetches only the display document from Elasticsearch") {
      withLocalImagesIndex { index =>
        val ids = (0 to 5).map(i => s"images.similar-features.$i")
        indexTestDocuments(index, ids: _*)

        val request =
          ImagesRequestBuilder.request(ImageSearchOptions(), index).value

        whenReady(elasticsearchService.executeTemplateSearchRequest(request)) {
          response =>
            val hits = response.right.value.hits.hits
            hits should have size ids.size
            every(hits.map(_.sourceAsMap.keySet)) shouldBe Set("display")
        }
      }
    }
  }

  describe("retrieveSimilarImages") {
    it("gets images with similar features") {
      withLocalImagesIndex { index =>
        indexTestDocuments(
          index,
          (0 to 5).map(i => s"images.similar-features.$i"): _*
        )

        val expectedImages = (1 to 5).map(
          i =>
            IndexedImage.Display(
              display = getDisplayImage(s"images.similar-features.$i")
          )
        )

        val future =
          imagesService
            .retrieveSimilarImages(
              index,
              imageId = getTestImageId("images.similar-features.0"),
              image = IndexedImage(
                display = getDisplayImage(s"images.similar-features.0"),
                vectorValues = getVectorValuesImage("images.similar-features.0")
              ),
              minScore = Some(0)
            )

        whenReady(future) {
          _ should contain theSameElementsAs expectedImages
        }
      }
    }

    // Raw-hit assertion, as above: the knn request bypasses the template.
    it("fetches only the display document from Elasticsearch") {
      withLocalImagesIndex { index =>
        indexTestDocuments(
          index,
          (0 to 5).map(i => s"images.similar-features.$i"): _*
        )

        val request = ImagesRequestBuilder.requestWithSimilarFeatures(
          index,
          getTestImageId("images.similar-features.0"),
          IndexedImage(
            display = getDisplayImage("images.similar-features.0"),
            vectorValues = getVectorValuesImage("images.similar-features.0")
          ),
          5,
          0.0
        )

        whenReady(elasticsearchService.executeSearchRequest(request)) {
          response =>
            val hits = response.right.value.hits.hits
            hits should have size 5
            every(hits.map(_.sourceAsMap.keySet)) shouldBe Set("display")
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
              vectorValues = getVectorValuesImage("images.everything")
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
              vectorValues = getVectorValuesImage("images.everything")
            ),
            minScore = Some(0)
          )

      whenReady(future) {
        _ shouldBe empty
      }
    }
  }
}
