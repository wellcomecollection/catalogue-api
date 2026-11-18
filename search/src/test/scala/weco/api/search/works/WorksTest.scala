package weco.api.search.works

import org.scalatest.funspec.AnyFunSpec
import weco.api.search.models.request.WorksIncludes

class WorksTest extends AnyFunSpec with ApiWorksTestBase {
  it("returns a list of works") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works") {
          Status.OK -> worksListResponse(visibleWorks)
        }
    }
  }

  it("returns a single work when requested with id") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "works.visible.0")

        assertJsonResponse(routes, path = s"$rootPath/works/afza5vpu") {
          Status.OK -> s"""
            {
              "id" : "afza5vpu",
              "title" : "52RvEJgwBuNO6n9",
              "alternativeTitles": [],
              "availabilities": [],
              "type": "Work"
            }
          """
        }
    }
  }

  it("returns optional fields when they exist") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-with-edition-and-duration")

        assertJsonResponse(routes, path = s"$rootPath/works/mwheb72m") {
          Status.OK ->
            """
              |{
              |  "id" : "mwheb72m",
              |  "title" : "dyagzJHsC6FYLTR",
              |  "alternativeTitles" : [],
              |  "availabilities" : [],
              |  "edition" : "Special edition",
              |  "duration" : 3600,
              |  "type" : "Work"
              |}
              |""".stripMargin
        }
    }
  }

  it(
    "returns the requested page of results when requested with page & pageSize"
  ) {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, visibleWorks: _*)

        val totalPages = math.ceil(visibleWorks.length / 2.0).toInt

        assertJsonResponse(routes, path = s"$rootPath/works?page=1&pageSize=2") {
          Status.OK -> s"""
            {
              "type": "ResultList",
              "pageSize": 2,
              "totalPages": $totalPages,
              "totalResults": ${visibleWorks.length},
              "nextPage": "$publicRootUri/works?page=2&pageSize=2",
              "results": [
                ${getVisibleWork("works.visible.0").display
            .withIncludes(WorksIncludes.none)},
                ${getVisibleWork("works.visible.1").display
            .withIncludes(WorksIncludes.none)}
              ]
            }
          """
        }

        assertJsonResponse(routes, path = s"$rootPath/works?page=2&pageSize=2") {
          Status.OK -> s"""
            {
              "type": "ResultList",
              "pageSize": 2,
              "totalPages": $totalPages,
              "totalResults": ${visibleWorks.length},
              "prevPage": "$publicRootUri/works?page=1&pageSize=2",
              "nextPage": "$publicRootUri/works?page=3&pageSize=2",
              "results": [
                ${getVisibleWork("works.visible.2").display
            .withIncludes(WorksIncludes.none)},
                ${getVisibleWork("works.visible.3").display
            .withIncludes(WorksIncludes.none)}
              ]
            }
          """
        }

        assertJsonResponse(routes, path = s"$rootPath/works?page=3&pageSize=2") {
          Status.OK -> s"""
            {
              "type": "ResultList",
              "pageSize": 2,
              "totalPages": $totalPages,
              "totalResults": ${visibleWorks.length},
              "prevPage": "$publicRootUri/works?page=2&pageSize=2",
              "results": [
                ${getVisibleWork("works.visible.4").display
            .withIncludes(WorksIncludes.none)}
              ]
            }
          """
        }
    }
  }

  it("ignores parameters that are unused when making an API request") {
    withWorksApi {
      case (_, routes) =>
        assertJsonResponse(routes, path = s"$rootPath/works?foo=bar") {
          Status.OK -> emptyJsonResult
        }
    }
  }

  it("returns matching results if doing a full-text search") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-title-dodo", "work-title-mouse")

        assertJsonResponse(routes, s"$rootPath/works?query=cat") {
          Status.OK -> emptyJsonResult
        }

        assertJsonResponse(routes, s"$rootPath/works?query=dodo") {
          Status.OK -> worksListResponse(ids = Seq("work-title-dodo"))
        }
    }
  }

  it("shows the thumbnail field if available") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work-thumbnail")

        assertJsonResponse(routes, path = s"$rootPath/works/oavuuc7j") {
          Status.OK -> """
            {
              "id" : "oavuuc7j",
              "title" : "gMFjKzORB4HFR64",
              "alternativeTitles" : [],
              "thumbnail" : {
                "locationType" : {
                  "id" : "iiif-presentation",
                  "label" : "IIIF Presentation API",
                  "type" : "LocationType"
                },
                "url" : "https://iiif.wellcomecollection.org/image/hFz.jpg/info.json",
                "license" : {
                  "id" : "cc-by",
                  "label" : "Attribution 4.0 International (CC BY 4.0)",
                  "url" : "http://creativecommons.org/licenses/by/4.0/",
                  "type" : "License"
                },
                "accessConditions" : [
                ],
                "type" : "DigitalLocation"
              },
              "availabilities" : [],
              "type" : "Work"
            }
          """
        }
    }
  }

  it("sorts by production date") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-production.1098",
          "work-production.1900",
          "work-production.1904"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-production.1098",
              "work-production.1900",
              "work-production.1904"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-production.1098",
          "work-production.1900",
          "work-production.1904"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=production.dates&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-production.1904",
              "work-production.1900",
              "work-production.1098"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by digital location created date") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-digital-location.2020",
          "work-digital-location.2022",
          "work-digital-location.2021"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=items.locations.createdDate"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-digital-location.2020",
              "work-digital-location.2021",
              "work-digital-location.2022"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by digital location created date in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-digital-location.2020",
          "work-digital-location.2022",
          "work-digital-location.2021"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=items.locations.createdDate&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-digital-location.2022",
              "work-digital-location.2021",
              "work-digital-location.2020"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("returns documents whose digital location has no createdDate last") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work-digital-location.2020",
          "work-digital-location.no-date",
          "work-digital-location.2021"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=items.locations.createdDate"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work-digital-location.2020",
              "work-digital-location.2021",
              "work-digital-location.no-date"
            ),
            strictOrdering = true
          )
        }
    }
  }

  // These works have collection paths which only sort correctly when sorted
  // naturally, i.e. when the numeric segments are compared as numbers rather
  // than as strings:
  //
  //    SA/SRT/C2/9  <  SA/SRT/C2/010  <  SA/SRT/C2/010/1  <  SA/SRT/C10/1
  val collectionPathSortWorks = Seq(
    "works.collection-path-sort.0",
    "works.collection-path-sort.1",
    "works.collection-path-sort.2",
    "works.collection-path-sort.3"
  )

  it("sorts by collection path") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, collectionPathSortWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=collectionPath"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.collection-path-sort.0",
              "works.collection-path-sort.1",
              "works.collection-path-sort.2",
              "works.collection-path-sort.3"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by collection path in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, collectionPathSortWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=collectionPath&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.collection-path-sort.3",
              "works.collection-path-sort.2",
              "works.collection-path-sort.1",
              "works.collection-path-sort.0"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("returns documents with no collection path last") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "works.collection-path-sort.0",
          "works.collection-path-sort.4",
          "works.collection-path-sort.1"
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=collectionPath"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.collection-path-sort.0",
              "works.collection-path-sort.1",
              "works.collection-path-sort.4"
            ),
            strictOrdering = true
          )
        }
    }
  }

  // A case-sensitive sort would put every capitalised title before "capybara" and "rats".
  val titleSortWorks = Seq(
    "works.archive.GC253.series", // Correspondence
    "works.examples.filtered-aggregations-tests.0", // rats
    "works.archive.GC253.root", // Papers relating to the history of vaccination
    "works.examples.filtered-aggregations-tests.1", // capybara
    "works.archive.GC253.item" // Letter from Edward Jenner to an unidentified correspondent
  )

  it("sorts by title, ignoring case") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, titleSortWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=title"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.examples.filtered-aggregations-tests.1",
              "works.archive.GC253.series",
              "works.archive.GC253.item",
              "works.archive.GC253.root",
              "works.examples.filtered-aggregations-tests.0"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts by title in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, titleSortWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=title&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.examples.filtered-aggregations-tests.0",
              "works.archive.GC253.root",
              "works.archive.GC253.item",
              "works.archive.GC253.series",
              "works.examples.filtered-aggregations-tests.1"
            ),
            strictOrdering = true
          )
        }
    }
  }

  // An empty title is a value that sorts before any other; a missing one goes last.
  val untitledSortWorks = Seq(
    "works.examples.filtered-aggregations-tests.0", // rats
    "works.title-sort-untitled.0", // no title
    "works.examples.filtered-aggregations-tests.1", // capybara
    "works.title-sort-untitled.1" // ""
  )

  it("sorts an empty title first and a missing title last") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, untitledSortWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=title"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.title-sort-untitled.1",
              "works.examples.filtered-aggregations-tests.1",
              "works.examples.filtered-aggregations-tests.0",
              "works.title-sort-untitled.0"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("sorts a missing title last in descending order") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, untitledSortWorks: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=title&sortOrder=desc"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.examples.filtered-aggregations-tests.0",
              "works.examples.filtered-aggregations-tests.1",
              "works.title-sort-untitled.1",
              "works.title-sort-untitled.0"
            ),
            strictOrdering = true
          )
        }
    }
  }

  // Numbers compare as text, so "C10" sorts before "C2" and "010" before "9".
  it("sorts numbers in titles as text") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          collectionPathSortWorks :+ "works.collection-path-sort.4": _*
        )

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?sort=title"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "works.collection-path-sort.4",
              "works.collection-path-sort.3",
              "works.collection-path-sort.1",
              "works.collection-path-sort.2",
              "works.collection-path-sort.0"
            ),
            strictOrdering = true
          )
        }
    }
  }

  it("returns a tally of work types") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/management/_workTypes") {
          Status.OK ->
            s"""
              |{
              |  "Visible": ${visibleWorks.length},
              |  "Invisible": ${invisibleWorks.length},
              |  "Deleted": ${deletedWorks.length},
              |  "Redirected": ${redirectedWorks.length}
              |}
              |""".stripMargin
        }
    }
  }
}
