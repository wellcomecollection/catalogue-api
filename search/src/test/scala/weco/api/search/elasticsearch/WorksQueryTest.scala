package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.api.search.fixtures.TestDocumentFixtures
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.index.IndexFixtures
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.{SearchQuery, SearchQueryType}
import weco.api.search.services.WorksService
import weco.catalogue.internal_model.generators.ImageGenerators
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.internal_model.work.{CollectionPath, Work}

import scala.concurrent.ExecutionContext.Implicits.global

class WorksQueryTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with SearchOptionsGenerators
    with ImageGenerators
    with TestDocumentFixtures {

  describe("Free text query functionality") {
    it("searches the canonicalId") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7sjip63h",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("searches the sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ejTwv1NdpH",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ji3JH82kKu",
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

    it("searches the items.otherIdentifiers") {
      withLocalWorksIndex { index =>
        val item1 = createIdentifiedItemWith(
          otherIdentifiers = List(createSourceIdentifier)
        )
        val item2 = createIdentifiedItemWith(
          otherIdentifiers = List(createSourceIdentifier)
        )

        val work1 = indexedWork().items(List(item1))
        val work2 = indexedWork().items(List(item2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypesOld(
          index,
          query = item1.id.otherIdentifiers.head.value,
          expectedMatches = List(work1)
        )
      }
    }

    it("searches the images.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "01bta4ru",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches the images.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val image1 = createImageData.toIdentified
        val image2 = createImageData.toIdentified

        val work1 = indexedWork().imageData(List(image1))
        val work2 = indexedWork().imageData(List(image2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypesOld(
          index,
          query = image1.id.sourceIdentifier.value,
          expectedMatches = List(work1)
        )
      }
    }

    it("matches when searching for an ID") {
      withLocalWorksIndex { index =>
        val work: Work.Visible[Indexed] = indexedWork()

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypesOld(
          index,
          query = work.state.canonicalId.underlying,
          expectedMatches = List(work)
        )
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7sjip63h",
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
          query = "7SJIP63H",
          expectedMatches = List("works.visible.0")
        )
      }
    }

    it("matches multiple IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7SJIP63H ob2ruvbb",
          expectedMatches = List("works.visible.0", "works.visible.1")
        )
      }
    }

    it("doesn't match partially matching source identifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ejTwv1NdpH",
          expectedMatches = List("works.visible.0")
        )

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ejTwv",
          expectedMatches = List()
        )
      }
    }

    it("searches for contributors") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "person-o8xazs",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches genre labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "nFnK1Qv0bPiYMZq",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches subject labels") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "g08I834KKSXk1WG",
          expectedMatches = List("work.visible.everything.0")
        )
      }
    }

    it("searches lettering") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .lettering(
            "Old Mughal minaret near Shahjahanabad (Delhi), Ghulam Ali Khan, early XIX century"
          )
        val notMatchingWork = indexedWork()
          .lettering("Not matching")

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypesOld(
          index,
          query = "shahjahanabad",
          expectedMatches = List(matchingWork)
        )
      }
    }

    it("searches for collection in collectionPath.path") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
        val notMatchingWork = indexedWork()
          .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypesOld(
          index,
          query = "PPCPB",
          expectedMatches = List(matchingWork)
        )
      }
    }

    it("searches for collection in collectionPath.label") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
        val notMatchingWork = indexedWork()
          .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypesOld(
          index,
          query = "PP/CRI",
          expectedMatches = List(matchingWork)
        )
      }
    }
  }

  private def assertResultsMatchForAllowedQueryTypesOld(
    index: Index,
    query: String,
    expectedMatches: List[Work[Indexed]]
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
          IndexedWork(_)
        )
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
