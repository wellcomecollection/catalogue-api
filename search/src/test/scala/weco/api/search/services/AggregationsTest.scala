package weco.api.search.services

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.generators.{GenreGenerators, ProductionEventGenerators, SubjectGenerators, WorkGenerators}
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.generators.{PeriodGenerators, SearchOptionsGenerators}
import weco.api.search.models.request.WorkAggregationRequest
import weco.api.search.models.{Aggregation, AggregationBucket, DateRangeFilter, FormatFilter, SubjectFilter, WorkSearchOptions}
import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.work.{Format, Subject}

class AggregationsTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with IndexFixtures
    with SubjectGenerators
    with GenreGenerators
    with ProductionEventGenerators
    with PeriodGenerators
    with SearchOptionsGenerators
    with WorkGenerators
    with TestDocumentFixtures {

  val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  it("returns more than 10 format aggregations") {
    withLocalWorksIndex { index =>
      indexTestDocuments(index, (0 to 22).map(i => s"works.every-format.$i"): _*)

      val searchOptions = createWorksSearchOptionsWith(
        aggregations = List(WorkAggregationRequest.Format)
      )

      val future = aggregationQuery(index, searchOptions)

      whenReady(future) {
        _.format.get.buckets should have size 23
      }
    }
  }

  it("aggregate over filtered dates, using only 'from' date") {
    val periods = List(
      createPeriodForYear("1850"),
      createPeriodForYearRange("1850", "2000"),
      createPeriodForYearRange("1860", "1960"),
      createPeriodForYear("1960"),
      createPeriodForYearRange("1960", "1964"),
      createPeriodForYear("1962")
    )

    val works = periods.map { p =>
      indexedWork()
        .production(
          List(createProductionEvent.copy(dates = List(p)))
        )
    }

    withLocalWorksIndex { index =>
      insertIntoElasticsearch(index, works: _*)
      val searchOptions = createWorksSearchOptionsWith(
        aggregations = List(WorkAggregationRequest.ProductionDate),
        filters = List(
          DateRangeFilter(Some(LocalDate.of(1960, 1, 1)), None)
        )
      )
      whenReady(aggregationQuery(index, searchOptions)) { aggs =>
        aggs.productionDates shouldBe Some(
          Aggregation(
            List(
              AggregationBucket(createPeriodForYear("1960"), 2),
              AggregationBucket(createPeriodForYear("1962"), 1)
            )
          )
        )
      }
    }
  }

  describe("aggregations with filters") {
    val formats = Format.values
    val subjects = (0 to 5).map(_ => createSubject)
    val works = formats.zipWithIndex.map {
      case (format, i) =>
        indexedWork()
          .format(format)
          .subjects(List(subjects(i / subjects.size)))
    }

    it("applies filters to their related aggregations") {
      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, works: _*)
        val searchOptions = createWorksSearchOptionsWith(
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.Subject),
          filters = List(
            FormatFilter(List(Format.Books.id))
          )
        )
        whenReady(aggregationQuery(index, searchOptions)) { aggs =>
          aggs.format should not be empty
          val buckets = aggs.format.get.buckets
          buckets.length shouldBe formats.length
          buckets.map(_.data) should contain theSameElementsAs formats
        }
      }
    }

    it("applies all non-related filters to aggregations") {
      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, works: _*)
        val subjectQuery = subjects.head match {
          case Subject(IdState.Unidentifiable, label, _) => label
          case _                                         => "bilberry"
        }
        val searchOptions = createWorksSearchOptionsWith(
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.Subject),
          filters = List(
            FormatFilter(List(Format.Books.id)),
            SubjectFilter(Seq(subjectQuery))
          )
        )
        whenReady(aggregationQuery(index, searchOptions)) { aggs =>
          val buckets = aggs.format.get.buckets
          val expectedFormats = works.map { _.data.format.get }
          buckets.length shouldBe expectedFormats.length
          buckets.map(_.data) should contain theSameElementsAs expectedFormats
        }
      }
    }

    it("applies all filters to the results") {
      withLocalWorksIndex { index =>
        insertIntoElasticsearch(index, works: _*)
        val subjectQuery = subjects.head match {
          case Subject(IdState.Unidentifiable, label, _) => label
          case _                                         => "passionfruit"
        }
        val searchOptions = createWorksSearchOptionsWith(
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.Subject),
          filters = List(
            FormatFilter(List(Format.Books.id)),
            SubjectFilter(Seq(subjectQuery))
          )
        )
        val results =
          whenReady(worksService.listOrSearch(index, searchOptions)) {
            _.right.get.results
          }

        val resultFormats =
          results
            .flatMap(w => getKey(w.display, "workType"))
            .flatMap(wt => getKey(wt, "label"))
            .flatMap(_.asString)
            .toSet

        resultFormats shouldBe Set("Books")

        results
          .flatMap(w => getKey(w.display, "subjects"))
          .flatMap(_.asArray)
          .map(_.flatMap(s => getKey(s, "label")))
          .map(subjects => subjects.flatMap(_.asString).toSet)
          .foreach(subjects => subjects should contain(subjectQuery))
      }
    }
  }

  private def aggregationQuery(index: Index, searchOptions: WorkSearchOptions) =
    worksService
      .listOrSearch(index, searchOptions)
      .map(_.right.get.aggregations.get)
}
