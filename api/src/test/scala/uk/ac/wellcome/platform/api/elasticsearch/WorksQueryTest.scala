package uk.ac.wellcome.platform.api.elasticsearch

import scala.concurrent.ExecutionContext.Implicits.global
import com.sksamuel.elastic4s.{ElasticError, Index}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchResponse}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.models.work.generators.{
  ContributorGenerators,
  GenreGenerators,
  ImageGenerators,
  SubjectGenerators,
  WorkGenerators
}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.api.generators.SearchOptionsGenerators
import uk.ac.wellcome.platform.api.models.{
  SearchOptions,
  SearchQuery,
  SearchQueryType
}
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.platform.api.services.{
  ElasticsearchService,
  WorksRequestBuilder
}
import WorkState.Indexed
import org.scalatest.Assertion

class WorksQueryTest
    extends AnyFunSpec
    with Matchers
    with ElasticsearchFixtures
    with SearchOptionsGenerators
    with SubjectGenerators
    with GenreGenerators
    with WorkGenerators
    with ImageGenerators
    with ContributorGenerators {

  val searchService = new ElasticsearchService(elasticClient)

  describe("Free text query functionality") {

    it("searches the canonicalId") {
      withLocalWorksIndex { index =>
        val work = indexedWork(canonicalId = "abc123")

        val query = "abc123"

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val work = indexedWork()
        val workNotMatching = indexedWork()
        val query = work.sourceIdentifier.value

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        val work = indexedWork()
          .otherIdentifiers(List(createSourceIdentifier))

        val workNotMatching = indexedWork()
          .otherIdentifiers(List(createSourceIdentifier))

        val query = work.data.otherIdentifiers.head.value

        insertIntoElasticsearch(index, work, workNotMatching)

        assertResultsMatchForAllowedQueryTypes(index, query, List(work))
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
          query = item1.id.canonicalId,
          matches = List(work1)
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
          matches = List(work1)
        )
      }
    }

    it("searches the items.otherIdentifiers") {
      withLocalWorksIndex { index =>
        val item1 = createIdentifiedItemWith(
          otherIdentifiers = List(createSourceIdentifier))
        val item2 = createIdentifiedItemWith(
          otherIdentifiers = List(createSourceIdentifier))

        val work1 = indexedWork().items(List(item1))
        val work2 = indexedWork().items(List(item2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = item1.id.otherIdentifiers.head.value,
          matches = List(work1)
        )
      }
    }

    it("searches the images.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        val image1 = createUnmergedImage.toIdentified
        val image2 = createUnmergedImage.toIdentified

        val work1 = indexedWork().images(List(image1))
        val work2 = indexedWork().images(List(image2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = image1.id.canonicalId,
          matches = List(work1)
        )
      }
    }

    it("searches the images.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        val image1 = createUnmergedImage.toIdentified
        val image2 = createUnmergedImage.toIdentified

        val work1 = indexedWork().images(List(image1))
        val work2 = indexedWork().images(List(image2))

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = image1.id.sourceIdentifier.value,
          matches = List(work1)
        )
      }
    }

    it("matches when searching for an ID") {
      withLocalWorksIndex { index =>
        val work: Work.Visible[Indexed] = indexedWork()

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = work.state.canonicalId,
          matches = List(work)
        )
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        val work = indexedWork(canonicalId = "1234567")

        insertIntoElasticsearch(index, work)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "123",
          matches = List()
        )
      }
    }

    it("matches IDs case insensitively") {
      withLocalWorksIndex { index =>
        val work1 = indexedWork(canonicalId = "AbCDeF1234")
        val work2 = indexedWork(canonicalId = "bloopybloop")

        insertIntoElasticsearch(index, work1, work2)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = work1.state.canonicalId.toLowerCase(),
          matches = List(work1)
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
        val work3 = indexedWork(canonicalId = "Oxford English Dictionary")

        insertIntoElasticsearch(index, work1, work2, work3)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query =
            s"${work1.state.canonicalId} ${work2.state.canonicalId} Oxford",
          matches = List(work1, work2)
        )
      }
    }

    it("searches for contributors") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .contributors(List(createPersonContributorWith("Matching")))
        val notMatchingWork = indexedWork()
          .contributors(List(createPersonContributorWith("Notmatching")))

        val query = "matching"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }

    it("Searches for genres") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .genres(List(createGenreWithMatchingConcept("Matching")))
        val notMatchingWork = indexedWork()
          .genres(List(createGenreWithMatchingConcept("Notmatching")))

        val query = "matching"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }

    it("Searches for subjects") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .subjects(List(createSubjectWithMatchingConcept("Matching")))
        val notMatchingWork = indexedWork()
          .subjects(List(createSubjectWithMatchingConcept("Notmatching")))

        val query = "matching"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }

    it("Searches lettering") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .lettering(
            "Old Mughal minaret near Shahjahanabad (Delhi), Ghulam Ali Khan, early XIX century")
        val notMatchingWork = indexedWork()
          .lettering("Not matching")

        val query = "shahjahanabad"

        insertIntoElasticsearch(index, matchingWork, notMatchingWork)

        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }

    it("Searches for collection in collectionPath.path") {
      withLocalWorksIndex { index =>
        val matchingWork = indexedWork()
          .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
        val notMatchingWork = indexedWork()
          .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))
        val query = "PPCPB"
        insertIntoElasticsearch(index, matchingWork, notMatchingWork)
        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
      }
    }
  }

  it("Searches for collection in collectionPath.label") {
    withLocalWorksIndex { index =>
      val matchingWork = indexedWork()
        .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
      val notMatchingWork = indexedWork()
        .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))
      val query = "PP/CRI"
      insertIntoElasticsearch(index, matchingWork, notMatchingWork)
      assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
    }
  }

  private def assertResultsMatchForAllowedQueryTypes(
    index: Index,
    query: String,
    matches: List[Work[Indexed]]): List[Assertion] =
    SearchQueryType.allowed map { queryType =>
      val results = searchResults(
        index,
        searchOptions = createWorksSearchOptionsWith(
          searchQuery = Some(SearchQuery(query, queryType))))

      withClue(s"Using: ${queryType.name}") {
        results.size shouldBe matches.size
        results should contain theSameElementsAs matches
      }
    }

  private def searchResults(
    index: Index,
    searchOptions: SearchOptions): List[Work[Indexed]] = {
    val searchResponseFuture =
      searchService.executeSearch(searchOptions, WorksRequestBuilder, index)
    whenReady(searchResponseFuture) { response =>
      searchResponseToWorks(response)
    }
  }

  private def searchResponseToWorks(
    response: Either[ElasticError, SearchResponse]): List[Work[Indexed]] =
    response.right.get.hits.hits.map { searchHit: SearchHit =>
      jsonToWork(searchHit.sourceAsString)
    }.toList

  private def jsonToWork(document: String): Work[Indexed] =
    fromJson[Work[Indexed]](document).get
}
