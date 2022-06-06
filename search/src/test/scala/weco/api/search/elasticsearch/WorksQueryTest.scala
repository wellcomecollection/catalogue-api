package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.api.search.fixtures.{IndexFixtures, TestDocumentFixtures}
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.{SearchQuery, SearchQueryType}
import weco.api.search.services.WorksService

import scala.concurrent.ExecutionContext.Implicits.global

class WorksQueryTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with SearchOptionsGenerators
    with TestDocumentFixtures {

  describe("Free text query functionality") {
    it("searches the canonicalId") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2twopft1",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("searches the sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "Uaequ81tpB",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "UfcQYSxE7g",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches the items.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ca3anii6",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches the items.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "hKyStbKjx1",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    val worksWithItemIdentifiers =
      (0 to 4).map(i => s"works.items-with-other-identifiers.$i")

    it("searches the items.otherIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksWithItemIdentifiers: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "xJJHLpGvU7",
          expectedMatches = List("works.items-with-other-identifiers.0")
        )
      }
    }

    it("searches the images.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "jh1bfhkx",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches the images.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "auL5Gzybrl",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("matches when searching for an ID") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, visibleWorks: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2twopft1",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2twopft1",
          expectedMatches = List("works.visible.0")
        )

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7sji",
          expectedMatches = List()
        )
      }
    }

    it("matches IDs case insensitively") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2TWOPFT1",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("matches multiple IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2TWOPFT1 dph7sjip",
          expectedMatches = List("works.visible.0", "works.visible.1")
        )
      }
    }

    it("doesn't match partially matching source identifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2twopft1",
          expectedMatches = List("works.visible.0")
        )

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "2two",
          expectedMatches = List()
        )
      }
    }

    it("searches for contributors") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "person-W9SVIX0fEg",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches genre labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "IHQR23GK9tQdPt3",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches subject labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "goKOwWLrIbnrzZj",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches lettering") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, "work-title-dodo", "work-title-mouse")

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "A line of legible ligatures",
          expectedMatches = List("work-title-dodo")
        )
      }
    }

    it("searches for collection in collectionPath.path") {
      withLocalWorksIndex { index =>
        indexTestDocuments(
          index,
          "works.collection-path.NUFFINK",
          "works.collection-path.PPCRI"
        )

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "PPCRI",
          expectedMatches = List("works.collection-path.PPCRI")
        )
      }
    }

    it("searches for collection in collectionPath.label") {
      withLocalWorksIndex { index =>
        indexTestDocuments(
          index,
          "works.collection-path.NUFFINK",
          "works.collection-path.PPCRI"
        )

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "PP/CRI",
          expectedMatches = List("works.collection-path.PPCRI")
        )
      }
    }
  }

  private def assertResultsMatchForAllowedQueryTypes(
    index: Index,
    query: String,
    expectedMatches: List[String]
  ): List[Assertion] =
    SearchQueryType.allowed map { queryType =>
      val future = worksService.listOrSearch(
        index,
        searchOptions = createWorksSearchOptionsWith(
          searchQuery = Some(SearchQuery(query, queryType))
        )
      )

      val results = whenReady(future) {
        _.right.value.results
      }

      withClue(s"Using: ${queryType.name}") {
        results.size shouldBe expectedMatches.size
        results should contain theSameElementsAs expectedMatches.map(
          getVisibleWork
        )
      }
    }

  private val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )
}
