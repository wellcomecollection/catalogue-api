package weco.api.search.services

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.search.elasticsearch.ElasticsearchService
import weco.api.search.fixtures.{
  IndexFixtures,
  JsonHelpers,
  TestDocumentFixtures
}
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models._
import weco.api.search.models.request.WorkAggregationRequest

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class AggregationsTest
    extends AnyFunSpec
    with Matchers
    with IndexFixtures
    with SearchOptionsGenerators
    with JsonHelpers
    with TestDocumentFixtures {

  val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  it("returns more than 10 format aggregations") {
    withLocalWorksIndex { index =>
      indexTestDocuments(
        index,
        (0 to 22).map(i => s"works.every-format.$i"): _*
      )

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
    withLocalWorksIndex { index =>
      indexTestDocuments(
        index,
        (0 to 5).map(i => s"works.production.multi-year.$i"): _*
      )

      val searchOptions = createWorksSearchOptionsWith(
        aggregations = List(WorkAggregationRequest.ProductionDate),
        filters = List(
          DateRangeFilter(Some(LocalDate.of(1960, 1, 1)), None)
        )
      )

      val future = aggregationQuery(index, searchOptions)

      whenReady(future) {
        _.productionDates shouldBe Some(
          Aggregation(
            List(
              AggregationBucket(
                AggregationBucketData(id = "1960", label = "1960"),
                count = 2
              ),
              AggregationBucket(
                AggregationBucketData(id = "1962", label = "1962"),
                count = 1
              )
            )
          )
        )
      }
    }
  }

  describe("aggregations with filters") {
    val works =
      (0 to 22).map(i => s"works.examples.aggregation-with-filters-tests.$i")

    it("applies filters to their related aggregations") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        val searchOptions = createWorksSearchOptionsWith(
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.SubjectLabel),
          filters = List(FormatFilter(List("a")))
        )
        whenReady(aggregationQuery(index, searchOptions)) { aggs =>
          aggs.format should not be empty
          val buckets = aggs.format.get.buckets
          buckets.length shouldBe works.length
          buckets.map(b => b.data.label) should contain theSameElementsAs List(
            "Books",
            "Manuscripts",
            "Music",
            "Journals",
            "Maps",
            "E-videos",
            "Videos",
            "Archives and manuscripts",
            "Born-digital archives",
            "Audio",
            "E-journals",
            "Pictures",
            "Ephemera",
            "CD-Roms",
            "Film",
            "Mixed materials",
            "Digital Images",
            "3-D Objects",
            "E-sound",
            "Standing order",
            "E-books",
            "Student dissertations",
            "Web sites"
          )
        }
      }
    }

    it("applies all non-related filters to aggregations") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        val searchOptions = createWorksSearchOptionsWith(
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.SubjectLabel),
          filters = List(
            FormatFilter(List("a")),
            SubjectLabelFilter(Seq("fIbfVPkqaf"))
          )
        )
        whenReady(aggregationQuery(index, searchOptions)) { aggs =>
          val buckets = aggs.format.get.buckets
          buckets.length shouldBe 7
          buckets.map(b => b.data.label) should contain theSameElementsAs List(
            "Archives and manuscripts",
            "Books",
            "Born-digital archives",
            "Film",
            "Manuscripts",
            "Music",
            "Standing order"
          )
        }
      }
    }

    it("applies all filters to the results") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        val searchOptions = createWorksSearchOptionsWith(
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.SubjectLabel),
          filters = List(
            FormatFilter(List("a")),
            SubjectLabelFilter(Seq("6rJpSUKd2d"))
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
          .foreach(subjects => subjects should contain("6rJpSUKd2d"))
      }
    }

    it("correctly returns aggregation labels even when no search results are returned") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        val searchOptions = createWorksSearchOptionsWith(
           searchQuery = Option(SearchQuery("awdawdawdawda")), // A search query returning 0 results
          aggregations =
            List(WorkAggregationRequest.Format, WorkAggregationRequest.SubjectLabel),
          filters = List(
            FormatFilter(List("a")),
            SubjectLabelFilter(Seq("fIbfVPkqaf"))
          )
        )
        whenReady(aggregationQuery(index, searchOptions)) { aggs =>
          val buckets = aggs.format.get.buckets

          buckets.length shouldBe 1
          buckets.map(b => b.data.label) should contain theSameElementsAs List(
            "Books",
          )
        }
      }
    }
  }

  private def aggregationQuery(index: Index, searchOptions: WorkSearchOptions) =
    worksService
      .listOrSearch(index, searchOptions)
      .map(_.right.get.aggregations.get)
}
