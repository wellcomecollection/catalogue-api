package weco.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{Index, Response}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.work.generators.WorkGenerators
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.fixtures.TestWith
import weco.json.JsonUtil._

class ElasticsearchScannerTest
    extends AnyFunSpec
    with Matchers
    with ElasticsearchFixtures
    with WorkGenerators {
  val scanner = new ElasticsearchScanner()(elasticClient)

  case class Shape(sides: Int, colour: String)

  val redSquare = Shape(sides = 4, colour = "red")
  val greenSquare = Shape(sides = 4, colour = "green")
  val blueSquare = Shape(sides = 4, colour = "blue")
  val redTriangle = Shape(sides = 3, colour = "red")

  val shapes = Seq(redSquare, greenSquare, blueSquare, redTriangle)

  def withIndex[R](shapes: Seq[Shape])(testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex(config = IndexConfig.empty) { index =>
      shapes.foreach { s =>
        val resp = elasticClient.execute {
          indexInto(index.name).doc(toJson(s).get)
        }.await

        resp.isSuccess shouldBe true
      }

      eventually {
        val response: Response[SearchResponse] = elasticClient.execute {
          search(index).matchAllQuery()
        }.await

        response.result.hits.hits should have size shapes.size
      }

      testWith(index)
    }

  it("finds every document that matches the query") {
    withIndex(shapes) { index =>
      val squares =
        scanner
          .scroll[Shape](
            search(index)
              .query(termQuery("sides", "4"))
          )
          .toList

      squares should contain theSameElementsAs Seq(
        redSquare,
        greenSquare,
        blueSquare
      )

      val redShapes =
        scanner
          .scroll[Shape](
            search(index)
              .query(termQuery("colour", "red"))
          )
          .toList

      redShapes should contain theSameElementsAs Seq(redSquare, redTriangle)
    }
  }

  it("fetches more documents than the bulk size") {
    val miniScroller = new ElasticsearchScanner()(elasticClient, bulkSize = 1)

    withIndex(shapes) { index =>
      val redShapes =
        miniScroller
          .scroll[Shape](
            search(index)
              .query(termQuery("colour", "red"))
          )
          .toList

      redShapes should contain theSameElementsAs Seq(redSquare, redTriangle)
    }
  }
}
