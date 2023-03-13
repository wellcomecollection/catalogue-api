package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import weco.api.search.fixtures.{IndexFixtures, TestDocumentFixtures}
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.{SearchQuery, SearchQueryType}
import weco.api.search.services.WorksService
import weco.fixtures.TestWith

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

        assertForQueryResults(index, query = "2twopft1") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("works.visible.0")
        }
      }
    }

    it("searches the sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertForQueryResults(index, query = "Uaequ81tpB") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("works.visible.0")
        }
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "UfcQYSxE7g") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches the canonical ID on items") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "ca3anii6") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches the source identifiers on items") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "hKyStbKjx1") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    val worksWithItemIdentifiers =
      (0 to 4).map(i => s"works.items-with-other-identifiers.$i")

    it("searches the items.otherIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksWithItemIdentifiers: _*)

        assertForQueryResults(index, query = "xJJHLpGvU7") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork(
            "works.items-with-other-identifiers.0"
          )
        }
      }
    }

    it("searches the images.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "eoedbdmz") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches the images.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "auL5Gzybrl") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("matches when searching for an ID") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, visibleWorks: _*)

        assertForQueryResults(index, query = "2twopft1") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("works.visible.0")
        }
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertForQueryResults(index, query = "7sji") { results =>
          results shouldBe empty
        }
      }
    }

    it("matches IDs case insensitively") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertForQueryResults(index, query = "2TWOPFT1") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("works.visible.0")
        }
      }
    }

    it("matches multiple IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertForQueryResults(index, query = "2TWOPFT1 dph7sjip") { results =>
          results.size shouldBe 2
          results should contain theSameElementsAs List(
            "works.visible.0",
            "works.visible.1"
          ).map(getVisibleWork)
        }
      }
    }

    it("doesn't match partially matching source identifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertForQueryResults(index, query = "2two") { results =>
          results shouldBe empty
        }
      }
    }

    it("searches for contributors") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "person-W9SVIX0fEg") { results =>
          // We don't mind about number of results for a label search
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches genre labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "IHQR23GK9tQdPt3") { results =>
          // We don't mind about number of results for a label search
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches subject labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "goKOwWLrIbnrzZj") { results =>
          // We don't mind about number of results for a label search
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches lettering") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, "work-title-dodo", "work-title-mouse")

        assertForQueryResults(index, query = "A line of legible ligatures") {
          results =>
            results.head shouldBe getVisibleWork("work-title-dodo")
        }
      }
    }

    it("searches for collection in collectionPath.path") {
      withLocalWorksIndex { index =>
        indexTestDocuments(
          index,
          "works.collection-path.NUFFINK",
          "works.collection-path.PPCRI"
        )

        assertForQueryResults(index, query = "PPCRI") { results =>
          results.head shouldBe getVisibleWork("works.collection-path.PPCRI")
        }
      }
    }

    it("searches for collection in collectionPath.label") {
      withLocalWorksIndex { index =>
        indexTestDocuments(
          index,
          "works.collection-path.NUFFINK",
          "works.collection-path.PPCRI"
        )

        assertForQueryResults(index, query = "PP/CRI") { results =>
          results.head shouldBe getVisibleWork("works.collection-path.PPCRI")
        }
      }
    }
  }

  private def assertForQueryResults[R](index: Index, query: String)(
    testWith: TestWith[Seq[IndexedWork.Visible], R]
  ) = SearchQueryType.allowed map { queryType =>
    val future = worksService.listOrSearch(
      index,
      searchOptions = createWorksSearchOptionsWith(
        searchQuery = Some(SearchQuery(query, queryType))
      )
    )

    val results = whenReady(future) {
      _.right.value.results
    }

    withClue(s"[Using query: ${queryType.name}]") {
      testWith(results)
    }
  }

  private val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )
}
