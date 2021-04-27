package weco.catalogue.snapshot_generator.iterators

import com.sksamuel.elastic4s.ElasticClient
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.index.IndexFixtures
import uk.ac.wellcome.models.work.generators.WorkGenerators

class ElasticsearchWorksIteratorTest
  extends AnyFunSpec
    with Matchers
    with IndexFixtures
    with WorkGenerators {

  implicit val client: ElasticClient = elasticClient
  val iterator = new ElasticsearchWorksIterator()

  it("returns all the works in the index") {
    withLocalWorksIndex { index =>
      val works = indexedWorks(count = 10)
      insertIntoElasticsearch(index, works: _*)

      iterator.scroll(index).toList should contain theSameElementsAs works
    }
  }

  it("filters non visible works") {
    withLocalWorksIndex { index =>
      val visibleWorks = indexedWorks(count = 10)
      val invisibleWorks = indexedWorks(count = 3).map { _.invisible() }

      val works = visibleWorks ++ invisibleWorks
      insertIntoElasticsearch(index, works: _*)

      iterator.scroll(index).toList should contain theSameElementsAs visibleWorks
    }
  }
}
