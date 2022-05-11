package weco.api.search.services

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.api.search.elasticsearch.{
  DocumentNotFoundError,
  ElasticsearchService,
  IndexNotFoundError
}
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models._
import weco.api.search.models.request.WorkAggregationRequest
import weco.catalogue.internal_model.generators.IdentifiersGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.Format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global

class WorksServiceTest
    extends AnyFunSpec
    with IndexFixtures
    with Matchers
    with EitherValues
    with SearchOptionsGenerators
    with IdentifiersGenerators
    with TestDocumentFixtures {

  val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  val defaultWorksSearchOptions: WorkSearchOptions = createWorksSearchOptions

  describe("listOrSearch") {
    it("gets records in Elasticsearch") {
      assertListOrSearchResultIsCorrect(
        allWorks = works,
        expectedWorks = visibleWorks,
        expectedTotalResults = visibleWorks.length
      )
    }

    it("returns 0 pages when no results are available") {
      assertListOrSearchResultIsCorrect(
        allWorks = Seq(),
        expectedWorks = Seq(),
        expectedTotalResults = 0
      )
    }

    it("returns an empty result set when asked for a page that does not exist") {
      assertListOrSearchResultIsCorrect(
        allWorks = visibleWorks,
        expectedWorks = Seq(),
        expectedTotalResults = visibleWorks.length,
        worksSearchOptions = createWorksSearchOptionsWith(pageNumber = 4)
      )
    }

    it("filters records by format") {
      assertListOrSearchResultIsCorrect(
        allWorks = worksFormat,
        expectedWorks = worksFormatBooks,
        expectedTotalResults = worksFormatBooks.length,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(FormatFilter(Seq("a")))
        )
      )

      assertListOrSearchResultIsCorrect(
        allWorks = worksFormat,
        expectedWorks = worksFormatBooks ++ worksFormatAudio,
        expectedTotalResults = worksFormatBooks.length + worksFormatAudio.length,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(FormatFilter(Seq("a", "i")))
        )
      )
    }

    it("returns a Left[ElasticError] if there's an Elasticsearch error") {
      val index = createIndex

      val future = worksService.listOrSearch(
        index = index,
        searchOptions = defaultWorksSearchOptions
      )

      whenReady(future) { err =>
        err.left.value shouldBe a[IndexNotFoundError]
        err.left.value
          .asInstanceOf[IndexNotFoundError]
          .index shouldBe index.name
      }
    }

    it("only finds results that match a query if doing a full-text search") {
      assertListOrSearchResultIsCorrect(
        allWorks = List("work-title-dodo", "work-title-mouse"),
        expectedWorks = List(),
        expectedTotalResults = 0,
        worksSearchOptions =
          createWorksSearchOptionsWith(searchQuery = Some(SearchQuery("cat")))
      )

      assertListOrSearchResultIsCorrect(
        allWorks = List("work-title-dodo", "work-title-mouse"),
        expectedWorks = List("work-title-dodo"),
        expectedTotalResults = 1,
        worksSearchOptions =
          createWorksSearchOptionsWith(searchQuery = Some(SearchQuery("dodo")))
      )
    }

    it("doesn't throw an exception if passed an invalid query string") {
      // unmatched quotes are a lexical error in the Elasticsearch parser
      assertListOrSearchResultIsCorrect(
        allWorks = List("work-title-dodo"),
        expectedWorks = List("work-title-dodo"),
        expectedTotalResults = 1,
        worksSearchOptions = createWorksSearchOptionsWith(
          searchQuery = Some(SearchQuery("dodo \""))
        )
      )
    }

    it("returns everything if we ask for a limit > result size") {
      assertListOrSearchResultIsCorrect(
        allWorks = visibleWorks,
        expectedWorks = visibleWorks,
        expectedTotalResults = visibleWorks.size,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = visibleWorks.length + 1
        )
      )
    }

    it("returns a page from the beginning of the result set") {
      assertListOrSearchResultIsCorrect(
        allWorks = visibleWorks,
        expectedWorks = visibleWorks.slice(0, 2),
        expectedTotalResults = visibleWorks.size,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = 2
        )
      )
    }

    it("returns a page from halfway through the result set") {
      assertListOrSearchResultIsCorrect(
        allWorks = visibleWorks,
        expectedWorks = visibleWorks.slice(2, 4),
        expectedTotalResults = visibleWorks.size,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = 2,
          pageNumber = 2
        )
      )
    }

    it("returns a page from the end of the result set") {
      assertListOrSearchResultIsCorrect(
        allWorks = visibleWorks,
        expectedWorks = visibleWorks.slice(4, 6),
        expectedTotalResults = visibleWorks.size,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = 2,
          pageNumber = 3
        )
      )
    }

    it("returns an empty page if asked for a limit > result size") {
      assertListOrSearchResultIsCorrect(
        allWorks = visibleWorks,
        expectedWorks = Seq(),
        expectedTotalResults = visibleWorks.size,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = visibleWorks.size * 2,
          pageNumber = 2
        )
      )
    }
  }

  describe("simple query string syntax") {
    it("uses only PHRASE simple query syntax") {
      assertListOrSearchResultIsCorrect(
        allWorks = List("works.title-query-syntax"),
        expectedWorks = List("works.title-query-syntax"),
        expectedTotalResults = 1,
        worksSearchOptions = createWorksSearchOptionsWith(
          searchQuery = Some(
            SearchQuery(
              "+a -title | with (all the simple) query~4 syntax operators in it*"
            )
          )
        )
      )
    }

    it(
      "doesn't throw a too_many_clauses exception when passed a query that creates too many clauses"
    ) {
      // This query uses precedence and would exceed the default 1024 clauses
      assertListOrSearchResultIsCorrect(
        allWorks = List("works.title-query-parens"),
        expectedWorks = List("works.title-query-parens"),
        expectedTotalResults = 1,
        worksSearchOptions = createWorksSearchOptionsWith(
          searchQuery = Some(SearchQuery("(a b c d e) h"))
        )
      )
    }

    it("aggregates formats") {
      val expectedAggregations = WorkAggregations(
        format = Some(
          Aggregation(
            List(
              AggregationBucket(
                data = Format.Books,
                count = worksFormatBooks.length
              ),
              AggregationBucket(
                data = Format.Journals,
                count = worksFormatJournals.length
              ),
              AggregationBucket(
                data = Format.Audio,
                count = worksFormatAudio.length
              ),
              AggregationBucket(
                data = Format.Pictures,
                count = worksFormatPictures.length
              )
            )
          )
        )
      )

      assertListOrSearchResultIsCorrect(
        allWorks = worksFormat,
        expectedWorks = worksFormat,
        expectedTotalResults = worksFormat.length,
        expectedAggregations = Some(expectedAggregations),
        worksSearchOptions = createWorksSearchOptionsWith(
          aggregations = List(WorkAggregationRequest.Format)
        )
      )
    }
  }

  describe("filter works by date") {
    val (fromDate1900, toDate1962) = {
      val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      (
        LocalDate.parse("01/01/1900", formatter),
        LocalDate.parse("01/01/1962", formatter)
      )
    }

    it("filters records by date range") {
      assertListOrSearchResultIsCorrect(
        allWorks = Seq(
          "work-production.1098",
          "work-production.1900",
          "work-production.1904",
          "work-production.1976",
          "work-production.2020"
        ),
        expectedWorks = Seq(
          "work-production.1900",
          "work-production.1904"
        ),
        expectedTotalResults = 2,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(
            DateRangeFilter(
              fromDate = Some(fromDate1900),
              toDate = Some(toDate1962)
            )
          )
        )
      )
    }

    it("filters records by from date") {
      assertListOrSearchResultIsCorrect(
        allWorks = Seq(
          "work-production.1098",
          "work-production.1900",
          "work-production.1904",
          "work-production.1976",
          "work-production.2020"
        ),
        expectedWorks = Seq(
          "work-production.1900",
          "work-production.1904",
          "work-production.1976",
          "work-production.2020"
        ),
        expectedTotalResults = 4,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(
            DateRangeFilter(
              fromDate = Some(fromDate1900),
              toDate = None
            )
          )
        )
      )
    }

    it("filters records by to date") {
      assertListOrSearchResultIsCorrect(
        allWorks = Seq(
          "work-production.1098",
          "work-production.1900",
          "work-production.1904",
          "work-production.1976",
          "work-production.2020"
        ),
        expectedWorks = Seq(
          "work-production.1098",
          "work-production.1900",
          "work-production.1904"
        ),
        expectedTotalResults = 3,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(
            DateRangeFilter(
              fromDate = None,
              toDate = Some(toDate1962)
            )
          )
        )
      )
    }
  }

  describe("findById") {
    it("gets a Work by id") {
      withLocalWorksIndex { index =>
        indexTestWorks(index, "works.visible.0")

        val future = worksService.findById(id = CanonicalId("7sjip63h"))(index)

        whenReady(future) {
          _ shouldBe Right(getVisibleWork("works.visible.0"))
        }
      }

    }

    it("returns a DocumentNotFoundError if there is no work") {
      withLocalWorksIndex { index =>
        val id = createCanonicalId
        val future = worksService.findById(id = id)(index)

        whenReady(future) {
          _ shouldBe Left(DocumentNotFoundError(id))
        }
      }
    }

    it("returns an ElasticsearchError if there's an Elasticsearch error") {
      val index = createIndex
      val future = worksService.findById(id = createCanonicalId)(index)

      whenReady(future) { err =>
        err.left.value shouldBe a[IndexNotFoundError]
        err.left.value
          .asInstanceOf[IndexNotFoundError]
          .index shouldBe index.name
      }
    }
  }

  private def assertListOrSearchResultIsCorrect(
    allWorks: Seq[String],
    expectedWorks: Seq[String],
    expectedTotalResults: Int,
    expectedAggregations: Option[WorkAggregations] = None,
    worksSearchOptions: WorkSearchOptions = createWorksSearchOptions
  ): Assertion =
    withLocalWorksIndex { index =>
      indexTestWorks(index, allWorks: _*)

      val future = worksService.listOrSearch(index, worksSearchOptions)

      whenReady(future) { result =>
        result.isRight shouldBe true

        val works = result.right.get
        works.results should contain theSameElementsAs expectedWorks.map(
          getVisibleWork
        )
        works.totalResults shouldBe expectedTotalResults
        works.aggregations shouldBe expectedAggregations
      }
    }
}
