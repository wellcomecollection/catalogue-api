package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.index.IndexFixtures
import weco.catalogue.internal_model.work.generators.{
  ContributorGenerators,
  GenreGenerators,
  SubjectGenerators
}
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.{SearchQuery, SearchQueryType}
import weco.api.search.services.WorksService
import weco.catalogue.internal_model.generators.ImageGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.work.WorkState.Indexed
import weco.catalogue.internal_model.work.{CollectionPath, Work}

import scala.concurrent.ExecutionContext.Implicits.global

class WorksQueryTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with SearchOptionsGenerators
    with SubjectGenerators
    with GenreGenerators
    with ImageGenerators
    with ContributorGenerators {

  describe("Free text query functionality") {

    it("searches the canonicalId") {
      withLocalWorksIndex { index =>
        val work = indexedWork(canonicalId = CanonicalId("12345678"))

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "12345678",
          expectedMatches = List(work)
        )
      }
    }

    it("searches the sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val work = indexedWork()
        val workNotMatching = indexedWork()

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = work.sourceIdentifier.value,
          expectedMatches = List(work)
        )
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        val work = indexedWork()
          .otherIdentifiers(List(createSourceIdentifier))

        val workNotMatching = indexedWork()
          .otherIdentifiers(List(createSourceIdentifier))

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = work.data.otherIdentifiers.head.value,
          expectedMatches = List(work)
        )
      }
    }

    it("searches the items.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        val item1 = createIdentifiedItem
        val item2 = createIdentifiedItem

        val work1 = indexedWork().items(List(item1))
        val work2 = indexedWork().items(List(item2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = item1.id.canonicalId.underlying,
          expectedMatches = List(work1)
        )
      }
    }

    it("searches the items.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val item1 = createIdentifiedItem
        val item2 = createIdentifiedItem

        val work1 = indexedWork().items(List(item1))
        val work2 = indexedWork().items(List(item2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = item1.id.sourceIdentifier.value,
          expectedMatches = List(work1)
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

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = item1.id.otherIdentifiers.head.value,
          expectedMatches = List(work1)
        )
      }
    }

    it("searches the images.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        val image1 = createImageData.toIdentified
        val image2 = createImageData.toIdentified

        val work1 = indexedWork().imageData(List(image1))
        val work2 = indexedWork().imageData(List(image2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = image1.id.canonicalId.underlying,
          expectedMatches = List(work1)
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

        assertResultsMatchForAllowedQueryTypes(
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

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = work.state.canonicalId.underlying,
          expectedMatches = List(work)
        )
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        val work = indexedWork(canonicalId = CanonicalId("12345678"))

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "123",
          expectedMatches = List()
        )
      }
    }

    it("matches IDs case insensitively") {
      withLocalWorksIndex { index =>
        val work1 = indexedWork(canonicalId = CanonicalId("AbCDeF12"))
        val work2 = indexedWork(canonicalId = CanonicalId("bloopybo"))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = work1.state.canonicalId.underlying.toLowerCase(),
          expectedMatches = List(work1)
        )
      }
    }

    it("matches multiple IDs") {
      withLocalWorksIndex { index =>
        val work1 = indexedWork()
        val work2 = indexedWork()
        val work3 = indexedWork()

        insertIntoElasticsearch(index, work1, work2, work3)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = s"${work1.state.canonicalId} ${work2.state.canonicalId}",
          List(work1, work2)
        )
      }
    }

    it("doesn't match partially matching IDs") {
      withLocalWorksIndex { index =>
        val work1 = indexedWork()
        val work2 = indexedWork()

        // We've put spaces in this as some Miro IDs are sentences
        val work3 = indexedWork(
          sourceIdentifier = createMiroSourceIdentifierWith(
            value = "Oxford English Dictionary"
          )
        )

        insertIntoElasticsearch(index, work1, work2, work3)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query =
            s"${work1.state.canonicalId} ${work2.state.canonicalId} Oxford",
          expectedMatches = List(work1, work2)
        )
      }
    }

    it("searches for contributors") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .contributors(List(createPersonContributorWith("Matching")))
        val notMatchingWork = indexedWork()
          .contributors(List(createPersonContributorWith("Notmatching")))

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "matching",
          expectedMatches = List(matchingWork)
        )
      }
    }

    it("Searches for genres") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .genres(List(createGenreWithMatchingConcept("Matching")))
        val notMatchingWork = indexedWork()
          .genres(List(createGenreWithMatchingConcept("Notmatching")))

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "matching",
          expectedMatches = List(matchingWork)
        )
      }
    }

    it("Searches for subjects") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .subjects(List(createSubjectWithMatchingConcept("Matching")))
        val notMatchingWork = indexedWork()
          .subjects(List(createSubjectWithMatchingConcept("Notmatching")))

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "matching",
          expectedMatches = List(matchingWork)
        )
      }
    }

    it("Searches lettering") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .lettering(
            "Old Mughal minaret near Shahjahanabad (Delhi), Ghulam Ali Khan, early XIX century"
          )
        val notMatchingWork = indexedWork()
          .lettering("Not matching")

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "shahjahanabad",
          expectedMatches = List(matchingWork)
        )
      }
    }

    it("Searches for collection in collectionPath.path") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
        val notMatchingWork = indexedWork()
          .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "PPCPB",
          expectedMatches = List(matchingWork)
        )
      }
    }
  }

  it("Searches for collection in collectionPath.label") {
    withLocalWorksIndex { index =>
      val matchingWork = indexedWork()
        .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
      val notMatchingWork = indexedWork()
        .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))

      insertIntoElasticsearch(index, matchingWork, notMatchingWork)

      assertResultsMatchForAllowedQueryTypes(
        index,
        query = "PP/CRI",
        expectedMatches = List(matchingWork)
      )
    }
  }

  private def assertResultsMatchForAllowedQueryTypes(
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
        results should contain theSameElementsAs expectedMatches
      }
    }

  private val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )
}
