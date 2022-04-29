package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.Index
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues}
import weco.api.search.JsonHelpers
import weco.api.search.fixtures.TestDocumentFixtures
import weco.api.search.generators.SearchOptionsGenerators
import weco.api.search.models.index.IndexedWork
import weco.api.search.models.{SearchQuery, SearchQueryType}
import weco.api.search.services.WorksService
import weco.catalogue.internal_model.index.IndexFixtures

import scala.concurrent.ExecutionContext.Implicits.global

class WorksQueryTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with IndexFixtures
    with SearchOptionsGenerators
    with JsonHelpers
    with TestDocumentFixtures {

  describe("Free text query functionality") {
    it("searches the canonicalId") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(index, query = "7sjip63h", expectedWorks = List("works.visible.0"))
      }
    }

    it("searches the source identifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(index, query = "ejTwv1NdpH", expectedWorks = List("works.visible.0"))
      }
    }

    it("searches the otherIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(index, query = "ji3JH82kKu", expectedWorks = List("work.visible.everything.0"))
      }
    }

    it("searches the items.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ca3anii6",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    it("searches the items.sourceIdentifiers") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "hKyStbKjx1",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    ignore("searches the items.otherIdentifiers") {
      // TODO: Create a test document with multiple item identifiers
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "hKyStbKjx1",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    it("searches the images.canonicalId as keyword") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "01bta4ru",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    ignore("searches the images.sourceIdentifiers") {
      // TODO: How does this work at all?
      withLocalWorksIndex { index =>
//        val image1 = createImageData.toIdentified
//        val image2 = createImageData.toIdentified
//
//        val work1 = indexedWork().imageData(List(image1))
//        val work2 = indexedWork().imageData(List(image2))
//
//        insertIntoElasticsearch(index, work1, work2)
//
//        assertResultsMatchForAllowedQueryTypes(
//          index,
//          query = image1.id.sourceIdentifier.value,
//          matches = List(work1)
//        )
      }
    }

    it("doesn't match on partial IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7sji",
          expectedWorks = List()
        )
      }
    }

    it("matches IDs case insensitively") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7SJIP63H",
          expectedWorks = List("works.visible.0")
        )
      }
    }

    it("matches multiple IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7SJIP63H ob2ruvbb",
          expectedWorks = List("works.visible.0", "works.visible.0")
        )
      }
    }

    it("doesn't match partially matching IDs") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, works: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "7SJIP63H ob2r",
          expectedWorks = List("works.visible.0")
        )
      }
    }

    it("searches contributors") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "Person:person-o8xazs",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    it("searches genres") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "4fR1f4tFlV",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    it("searches subjects") {
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ArEtlVdV0j",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }

    ignore("searches lettering") {
      // TODO: Create a test document with lettering
      withLocalWorksIndex { index =>
        indexTestDocuments(index, worksEverything: _*)

        assertResultsMatchForAllowedQueryTypes(
          index,
          query = "ArEtlVdV0j",
          expectedWorks = List("work.visible.everything.0")
        )
      }
    }
//
//    it("Searches for collection in collectionPath.path") {
//      withLocalWorksIndex { index =>
//        val matchingWork = indexedWork()
//          .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
//        val notMatchingWork = indexedWork()
//          .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))
//        val query = "PPCPB"
//        insertIntoElasticsearch(index, matchingWork, notMatchingWork)
//        assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
//      }
//    }
  }

//  it("Searches for collection in collectionPath.label") {
//    withLocalWorksIndex { index =>
//      val matchingWork = indexedWork()
//        .collectionPath(CollectionPath("PPCPB", label = Some("PP/CRI")))
//      val notMatchingWork = indexedWork()
//        .collectionPath(CollectionPath("NUFFINK", label = Some("NUF/FINK")))
//      val query = "PP/CRI"
//      insertIntoElasticsearch(index, matchingWork, notMatchingWork)
//      assertResultsMatchForAllowedQueryTypes(index, query, List(matchingWork))
//    }
//  }

  private def assertResultsMatchForAllowedQueryTypes(
    index: Index,
    query: String,
    expectedWorks: List[String]
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

      val expectedDocuments = getTestDocuments(expectedWorks)
        .map { testDoc =>
          val displayDoc = getKey(testDoc.document, "display").get
          IndexedWork.Visible(displayDoc)
        }

      withClue(s"Using: ${queryType.name}") {
        results should contain theSameElementsAs expectedDocuments
      }
    }

  private val worksService = new WorksService(
    elasticsearchService = new ElasticsearchService(elasticClient)
  )
}
