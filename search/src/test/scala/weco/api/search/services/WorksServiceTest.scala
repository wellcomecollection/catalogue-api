package weco.api.search.services

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.api.search.JsonHelpers
import weco.api.search.elasticsearch.{DocumentNotFoundError, ElasticsearchService, IndexNotFoundError}
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.request.WorkAggregationRequest
import weco.api.search.models.{Aggregation, AggregationBucket, FormatFilter, SearchQuery, WorkAggregations, WorkSearchOptions}
import weco.api.search.works.ApiWorksTestBase
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.Format.{Audio, Books, Journals, Pictures}

class WorksServiceTest
    extends AnyFunSpec
    with IndexFixtures
    with Matchers
    with EitherValues
    with SearchOptionsGenerators
    with ApiWorksTestBase
    with JsonHelpers {

  val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )

  describe("listOrSearch") {
    it("lists visible works") {
      assertListOrSearchResultIsCorrect(
        allWorks = listOfWorks,
        expectedWorks = List(
          "works.visible.0",
          "works.visible.1",
          "works.visible.2",
          "works.visible.3",
          "works.visible.4",
        ),
        expectedTotalResults = 5
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
        allWorks = listOfWorks,
        expectedWorks = Seq(),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(pageNumber = 4)
      )
    }

    it("filters results by format") {
      assertListOrSearchResultIsCorrect(
        allWorks = formatWorks,
        expectedWorks = Seq("works.formats.0.Books", "works.formats.1.Books", "works.formats.2.Books", "works.formats.3.Books"),
        expectedTotalResults = 4,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(FormatFilter(Seq("a")))
        )
      )
    }

    it("filters results by multiple formats") {
      assertListOrSearchResultIsCorrect(
        allWorks = formatWorks,
        expectedWorks = Seq("works.formats.0.Books", "works.formats.1.Books", "works.formats.2.Books", "works.formats.3.Books", "works.formats.9.Pictures"),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(
          filters = List(FormatFilter(Seq("a", "k")))
        )
      )
    }

    it("returns a Left[ElasticError] if there's an Elasticsearch error") {
      val index = createIndex

      val future = worksService.listOrSearch(
        index = index,
        searchOptions = createWorksSearchOptions
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
        allWorks = List("work-title-dodo", "work-title-mouse"),
        expectedWorks = List("work-title-dodo"),
        expectedTotalResults = 1,
        worksSearchOptions =
          createWorksSearchOptionsWith(searchQuery = Some(SearchQuery("dodo \"")))
      )
    }

    it("returns everything if we ask for a limit > result size") {
      assertListOrSearchResultIsCorrect(
        allWorks = listOfWorks,
        expectedWorks = List(
          "works.visible.0",
          "works.visible.1",
          "works.visible.2",
          "works.visible.3",
          "works.visible.4",
        ),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = listOfWorks.length + 1
        )
      )
    }

    it("returns a page from the beginning of the result set") {
      assertListOrSearchResultIsCorrect(
        allWorks = listOfWorks,
        expectedWorks = List(
          "works.visible.0",
          "works.visible.1",
        ),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageSize = 2
        )
      )
    }

    it("returns a page from halfway through the result set") {
      assertListOrSearchResultIsCorrect(
        allWorks = listOfWorks,
        expectedWorks = List(
          "works.visible.2",
          "works.visible.3",
        ),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageNumber = 2,
          pageSize = 2
        )
      )
    }

    it("returns a page from the end of the result set") {
      assertListOrSearchResultIsCorrect(
        allWorks = listOfWorks,
        expectedWorks = List(
          "works.visible.4"
        ),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageNumber = 3,
          pageSize = 2
        )
      )
    }

    it("returns an empty page if asked for a limit > result size") {
      assertListOrSearchResultIsCorrect(
        allWorks = listOfWorks,
        expectedWorks = List(),
        expectedTotalResults = 5,
        worksSearchOptions = createWorksSearchOptionsWith(
          pageNumber = listOfWorks.length * 2,
          pageSize = 1
        )
      )
    }

    it("aggregates formats") {
      val expectedAggregations = WorkAggregations(
        Some(
          Aggregation(
            List(
              AggregationBucket(data = Books, count = 4),
              AggregationBucket(data = Journals, count = 3),
              AggregationBucket(data = Audio, count = 2),
              AggregationBucket(data = Pictures, count = 1),
            )
          )
        ),
        None
      )

      assertListOrSearchResultIsCorrect(
        allWorks = formatWorks,
        expectedWorks = formatWorks,
        expectedTotalResults = 10,
        expectedAggregations = Some(expectedAggregations),
        worksSearchOptions = createWorksSearchOptionsWith(
          aggregations = List(WorkAggregationRequest.Format)
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
  }

  describe("findById") {
    it("gets a Work by id") {
      withLocalWorksIndex { index =>
        indexExampleDocuments(index, "works.visible.0")

        val future = worksService.findById(id = CanonicalId("1tpb2two"))(index)

        whenReady(future) {
          _ shouldBe Right(getIndexedWork("works.visible.0"))
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
      indexExampleDocuments(index, allWorks: _*)

      val future = worksService.listOrSearch(index, worksSearchOptions)

      val expectedResults = expectedWorks.map(getIndexedWork)

      whenReady(future) { result =>
        result.isRight shouldBe true

        val works = result.right.get
        works.results should contain theSameElementsAs expectedResults
        works.totalResults shouldBe expectedTotalResults
        works.aggregations shouldBe expectedAggregations
      }
    }

  private def getIndexedWork(id: String): IndexedWork.Visible = {
    val doc = getExampleDocuments(Seq(id)).head
    val display = getKey(doc.document, "display").get
    IndexedWork.Visible(display)
  }
}
