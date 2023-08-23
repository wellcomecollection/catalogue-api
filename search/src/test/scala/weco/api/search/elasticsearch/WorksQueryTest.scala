package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.Index
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import weco.api.search.fixtures.{IndexFixtures, TestDocumentFixtures}
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.SearchQuery
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

        assertForQueryResults(index, query = "ejk7jwcd") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches the source identifiers on items") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "GWWFxlGgZX") { results =>
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

        assertForQueryResults(index, query = "MKDvFJ5itR") { results =>
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

        assertForQueryResults(index, query = "ihvpnycp") { results =>
          results.size shouldBe 1
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches the images.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "N8dAz61bAE") { results =>
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

        assertForQueryResults(index, query = "person-eKZIqbV783") { results =>
          // We don't mind about number of results for a label search
          results.head shouldBe getVisibleWork("work.visible.everything.0")
        }
      }
    }

    it("searches genre labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertForQueryResults(index, query = "9tQdPt3acHhNKnN") { results =>
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

    it("accurately reports total hits") {
      info(
        """By default, Elasticsearch only returns total hits up to 10000.
          | Ensure that the number we return to the API is an accurate reflection
          | of the number of matches, and has not been cut down to this 10k max.
          |""".stripMargin
      )
      withLocalWorksIndex { index =>
        val docs: Seq[TestDocument] = (0 to 10010) map { i =>
          val docId = s"id$i"
          val doc = Json.obj(
            "display" -> Json
              .obj("id" -> docId.asJson, "title" -> s"work number $i".asJson),
            "query" -> Json.obj(
              "title" -> Seq(s"work number $i").asJson
            ),
            "type" -> "Visible".asJson
          )

          TestDocument(
            docId,
            doc
          )
        }
        indexLoadedTestDocuments(index, docs)

        val future = worksService.listOrSearch(
          index,
          searchOptions = createWorksSearchOptionsWith(
            searchQuery = Some(SearchQuery("work"))
          )
        )

        whenReady(future) { r =>
          val v = r.right.value
          r.right.value.totalResults should be > 10000
          println(v)
        }
      }

    }

  }

  private def assertForQueryResults[R](index: Index, query: String)(
    testWith: TestWith[Seq[IndexedWork.Visible], R]
  ) = {
    val future = worksService.listOrSearch(
      index,
      searchOptions = createWorksSearchOptionsWith(
        searchQuery = Some(SearchQuery(query))
      )
    )

    val results = whenReady(future) {
      _.right.value.results
    }

    testWith(results)
  }

  private val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )
}
