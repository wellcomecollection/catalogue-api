package weco.api.search.fixtures

import org.scalatest.Suite
import weco.api.search.elasticsearch.ResilientElasticClient

import java.time.Clock
import scala.concurrent.ExecutionContext.Implicits.global

trait ResilientElasticClientFixture extends IndexFixtures {
  this: Suite =>

  implicit val clock: Clock = Clock.systemUTC()

  lazy val resilientElasticClient: ResilientElasticClient =
    new ResilientElasticClient(() => elasticClient)
}
