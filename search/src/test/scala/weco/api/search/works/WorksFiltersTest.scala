package weco.api.search.works

import akka.http.scaladsl.server.Route
import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.fixtures.TestWith

class WorksFiltersTest
    extends AnyFunSpec
    with ApiWorksTestBase
    with TableDrivenPropertyChecks {

  it("works") {
    withWorksApi {
      case (worksIndex, routes) =>
        assertJsonResponse(
          routes,
          path = "/works?workType=a%3C&aggregations=workType"
        ) {
          Status.OK -> worksListResponse(
            id = Seq("work.visible.everything.0")
          )
        }
    }
  }

  it("combines multiple filters") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/works?genres.label=Uw1LvlTE5c&subjects.label=RGOo9Fg6ic"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq("work.visible.everything.0")
          )
        }
    }
  }

  it("filters works by item LocationType") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(
          worksIndex,
          "work.items-with-location-types.0",
          "work.items-with-location-types.1",
          "work.items-with-location-types.2"
        )

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/works?items.locations.locationType=iiif-presentation,closed-stores"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              "work.items-with-location-types.1",
              "work.items-with-location-types.2"
            )
          )
        }
    }
  }

  describe("filtering works by date range") {
    val productionWorks = Seq(
      "work-production.1098",
      "work-production.1900",
      "work-production.1904",
      "work-production.1976",
      "work-production.2020"
    )

    it("filters by date range") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, productionWorks: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/works?production.dates.from=1900-01-01&production.dates.to=1960-01-01"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq("work-production.1900", "work-production.1904")
            )
          }
      }
    }

    it("filters by from date") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, productionWorks: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?production.dates.from=1900-01-01"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "work-production.1900",
                "work-production.1904",
                "work-production.1976",
                "work-production.2020"
              )
            )
          }
      }
    }

    it("filters by to date") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, productionWorks: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?production.dates.to=1960-01-01"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "work-production.1098",
                "work-production.1900",
                "work-production.1904"
              )
            )
          }
      }
    }

    it("errors on invalid date") {
      withApi { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/works?production.dates.from=INVALID"
        ) {
          Status.BadRequest ->
            badRequest(
              "production.dates.from: Invalid date encoding. Expected YYYY-MM-DD"
            )
        }
      }
    }
  }

  describe("filtering by genre concept ids") {

    // This work has a single compound Genre.
    // It should only be matched by the primary genre id
    val goodCafeSecondaryBaadFoodWork = s"works.examples.genre-filters-tests.0"
    val goodCafeWork = "works.examples.genre-filters-tests.1"
    val baadFoodWork = "works.examples.genre-filters-tests.2"
    val noConceptWork = "works.examples.genre-filters-tests.3"
    // This work has a multiple Genres, both goodcafe and baadfood are
    // present as primary concepts within the genres.
    // It should be matched by either id
    val bothGenresWork = s"works.examples.genre-filters-tests.4"

    def withGenreIdFilterRecords(
      testWith: TestWith[Route, Assertion]
    ): Assertion =
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(
            worksIndex,
            goodCafeSecondaryBaadFoodWork,
            goodCafeWork,
            baadFoodWork,
            noConceptWork,
            bothGenresWork
          )
          testWith(routes)
      }

    it("does not apply the filter if there are no values provided") {
      withGenreIdFilterRecords { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/works?genres.concepts="
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              goodCafeSecondaryBaadFoodWork,
              goodCafeWork,
              baadFoodWork,
              noConceptWork,
              bothGenresWork
            )
          )
        }

      }
    }

    it("filters by one concept id") {
      withGenreIdFilterRecords { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/works?genres.concepts=baadf00d"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              bothGenresWork,
              baadFoodWork
            )
          )
        }
      }
    }

    it(
      "filters containing multiple concept ids return documents containing ANY of the requested ids"
    ) {
      withGenreIdFilterRecords { routes =>
        assertJsonResponse(
          routes,
          path = s"$rootPath/works?genres.concepts=g00dcafe,baadf00d"
        ) {
          Status.OK -> worksListResponse(
            ids = Seq(
              goodCafeSecondaryBaadFoodWork,
              goodCafeWork,
              baadFoodWork,
              bothGenresWork
            )
          )
        }
      }
    }
  }

  describe("filtering works by license") {
    it("filters by license") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksLicensed: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?items.locations.license=cc-by"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.items-with-licenses.0",
                "works.items-with-licenses.1",
                "works.items-with-licenses.3"
              )
            )
          }
      }
    }

    it("filters by multiple licenses") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksLicensed: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?items.locations.license=cc-by,cc-by-nc"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.items-with-licenses.0",
                "works.items-with-licenses.1",
                "works.items-with-licenses.2",
                "works.items-with-licenses.3"
              )
            )
          }
      }
    }
  }

  describe("Identifiers filter") {
    it("filters by a sourceIdentifier") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?identifiers=Aic5qOhRoS"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq("work.visible.everything.0")
            )
          }
      }
    }

    it("filters by multiple sourceIdentifiers") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?identifiers=Aic5qOhRoS,LMVvWxgXRS"
          ) {
            Status.OK -> worksListResponse(
              ids =
                Seq("work.visible.everything.0", "work.visible.everything.1")
            )
          }
      }
    }

    it("filters by an otherIdentifier") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?identifiers=Hq3k05Fqag"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq("work.visible.everything.2")
            )
          }
      }
    }

    it("filters by multiple otherIdentifiers") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?identifiers=UfcQYSxE7g,Hq3k05Fqag"
          ) {
            Status.OK -> worksListResponse(
              ids =
                Seq("work.visible.everything.0", "work.visible.everything.2")
            )
          }
      }
    }

    it("filters by mixed identifiers") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?identifiers=Aic5qOhRoS,Hq3k05Fqag"
          ) {
            Status.OK -> worksListResponse(
              ids =
                Seq("work.visible.everything.0", "work.visible.everything.2")
            )
          }
      }
    }
  }

  describe("Access status filter") {
    val works =
      (0 to 6).map(i => s"works.examples.access-status-filters-tests.$i")

    it("includes works by access status") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/works?items.locations.accessConditions.status=restricted,closed"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(0, 1, 2).map(
                i => s"works.examples.access-status-filters-tests.$i"
              )
            )
          }
      }
    }

    // The licensed resources access status is a bit special: it's a case class that
    // takes two values rather than a value.
    //
    // Check we're handling it correctly.
    it("includes works which are licensed resources") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/works?items.locations.accessConditions.status=licensed-resources"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(5, 6).map(
                i => s"works.examples.access-status-filters-tests.$i"
              )
            )
          }
      }
    }

    it("excludes works by access status") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path =
              s"$rootPath/works?items.locations.accessConditions.status=!restricted,!closed"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(3, 4, 5, 6).map(
                i => s"works.examples.access-status-filters-tests.$i"
              )
            )
          }
      }
    }
  }

  describe("availabilities filter") {
    val worksAvailabilities = Seq(
      "works.examples.availabilities.open-only",
      "works.examples.availabilities.closed-only",
      "works.examples.availabilities.online-only",
      "works.examples.availabilities.everywhere",
      "works.examples.availabilities.nowhere"
    )
    it("filters by availability ID") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksAvailabilities: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?availabilities=open-shelves"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.examples.availabilities.open-only",
                "works.examples.availabilities.everywhere"
              )
            )
          }
      }
    }

    it("filters by multiple comma-separated availability IDs") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksAvailabilities: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?availabilities=open-shelves,closed-stores"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.examples.availabilities.open-only",
                "works.examples.availabilities.closed-only",
                "works.examples.availabilities.everywhere"
              )
            )
          }
      }
    }
  }

  describe("relation filters") {
    it("filters partOf by id") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?partOf=nrvdy0jg") {
            Status.OK -> worksListResponse(
              ids = Seq("work.visible.everything.0")
            )
          }
      }
    }

    it("filters partOf by title") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?partOf=title-MS5Hy6x38N"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq("work.visible.everything.0")
            )
          }
      }
    }
  }

  describe("item filters") {
    it("filters by canonical ID on items") {
      assertItemsFilterWorks(
        path = s"$rootPath/works?items=a7xxlndb",
        expectedIds = Seq("work.visible.everything.0")
      )

      assertItemsFilterWorks(
        path = s"$rootPath/works?items=sr0le4q0",
        expectedIds = Seq("work.visible.everything.1")
      )
    }

    it("looks up multiple canonical IDs") {
      forAll(
        Table(
          ("ids", "works"),
          (
            "a7xxlndb,sr0le4q0",
            Seq("work.visible.everything.0", "work.visible.everything.1")
          ),
          (
            "sr0le4q0,9indplmm",
            Seq("work.visible.everything.1", "work.visible.everything.2")
          ),
          (
            "b7xfovxp,a7xxlndb",
            Seq("work.visible.everything.2", "work.visible.everything.0")
          )
        )
      ) { (ids, works) =>
        assertItemsFilterWorks(
          path = s"$rootPath/works?items=$ids",
          expectedIds = works
        )
      }
    }

    it("looks up source identifiers") {
      assertItemsFilterWorks(
        path = s"$rootPath/works?items.identifiers=dG0mvvCJtU",
        expectedIds = Seq("work.visible.everything.0")
      )

      assertItemsFilterWorks(
        path = s"$rootPath/works?items.identifiers=fYVwu7Y7Y7",
        expectedIds = Seq("work.visible.everything.1")
      )
    }

    it("looks up multiple source identifiers") {
      assertItemsFilterWorks(
        path = s"$rootPath/works?items.identifiers=GWWFxlGgZX,HYZ1N6BwQR",
        expectedIds =
          Seq("work.visible.everything.0", "work.visible.everything.1")
      )
    }

    def assertItemsFilterWorks(
      path: String,
      expectedIds: Seq[String]
    ): Assertion =
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path) {
            Status.OK -> worksListResponse(ids = expectedIds)
          }
      }
  }
}
