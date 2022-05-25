package weco.api.search.fixtures

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.{ElasticDsl, Index, Response}
import org.scalatest.{Assertion, Suite}
import weco.elasticsearch.test.fixtures.ElasticsearchFixtures
import weco.fixtures.{fixture, Fixture, LocalResources, TestWith}

trait IndexFixtures extends ElasticsearchFixtures with LocalResources {
  this: Suite =>
  private def withIndex[R](sourceName: String): Fixture[Index, R] =
    fixture[Index, R](
      create = {
        val index = createIndex

        elasticClient
          .execute(
            ElasticDsl
              .createIndex(index.name)
              .source(readResource(sourceName))
          )
          .await

        // Elasticsearch is eventually consistent, so the future
        // completing doesn't actually mean that the index exists yet
        eventuallyIndexExists(index)

        index
      },
      destroy = eventuallyDeleteIndex
    )

  private def eventuallyDeleteIndex(index: Index): Assertion = {
    elasticClient.execute(deleteIndex(index.name))

    eventually {
      assert(
        !indexDoesExist(index),
        s"Index $index has not been deleted"
      )
    }
  }

  private def indexDoesExist(index: Index): Boolean = {
    val response: Response[IndexExistsResponse] =
      elasticClient
        .execute(indexExists(index.name))
        .await

    response.result.exists
  }

  def withLocalWorksIndex[R](testWith: TestWith[Index, R]): R =
    withIndex("WorksIndexConfig.json") { index =>
      testWith(index)
    }

  def withLocalImagesIndex[R](testWith: TestWith[Index, R]): R =
    withIndex("ImagesIndexConfig.json") { index =>
      testWith(index)
    }

  def getSizeOf(index: Index): Long =
    elasticClient
      .execute { count(index.name) }
      .await
      .result
      .count
}
