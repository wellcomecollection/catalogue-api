package weco.api.search.works

import akka.http.scaladsl.server.Route
import org.scalatest.Assertion
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.fixtures.TestWith

import java.net.URLEncoder

class WorksFiltersTest extends ApiWorksTestBase with TableDrivenPropertyChecks {

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

  describe("filtering works by Format") {
    it("when listing works") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksFormat: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?workType=k"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq("works.formats.9.Pictures")
            )
          }
      }
    }

    it("filters by multiple formats") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksFormat: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?workType=k,d"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.formats.4.Journals",
                "works.formats.5.Journals",
                "works.formats.6.Journals",
                "works.formats.9.Pictures"
              )
            )
          }
      }
    }
  }

  describe("filtering works by type") {
    val works = Seq(
      "works.examples.different-work-types.Collection",
      "works.examples.different-work-types.Series",
      "works.examples.different-work-types.Section"
    )

    it("when listing works") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?type=Collection") {
            Status.OK -> worksListResponse(
              ids = Seq("works.examples.different-work-types.Collection")
            )
          }
      }
    }

    it("filters by multiple types") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?type=Collection,Series"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.examples.different-work-types.Collection",
                "works.examples.different-work-types.Series"
              )
            )
          }
      }
    }

    it("when searching works") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?query=rats&type=Series,Section"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.examples.different-work-types.Series",
                "works.examples.different-work-types.Section"
              )
            )
          }
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

  describe("filtering works by language") {
    val languageWorks = Seq(
      "works.languages.0.eng",
      "works.languages.1.eng",
      "works.languages.2.eng",
      "works.languages.3.eng+swe",
      "works.languages.4.eng+swe+tur",
      "works.languages.5.swe",
      "works.languages.6.tur"
    )

    it("filters by language") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, languageWorks: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?languages=eng") {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.languages.0.eng",
                "works.languages.1.eng",
                "works.languages.2.eng",
                "works.languages.3.eng+swe",
                "works.languages.4.eng+swe+tur"
              )
            )
          }
      }
    }

    it("filters by multiple comma separated languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, languageWorks: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?languages=swe,tur"
          ) {
            Status.OK -> worksListResponse(
              ids = Seq(
                "works.languages.3.eng+swe",
                "works.languages.4.eng+swe+tur",
                "works.languages.5.swe",
                "works.languages.6.tur"
              )
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

  describe("filtering works by genre") {
    val annualReportsWork = s"works.examples.genre-filters-tests.0"
    val pamphletsWork = "works.examples.genre-filters-tests.1"
    val psychologyWork = "works.examples.genre-filters-tests.2"
    val darwinWork = "works.examples.genre-filters-tests.3"
    val mostThingsWork = "works.examples.genre-filters-tests.4"
    val nothingWork = "works.examples.genre-filters-tests.5"

    val works =
      List(
        annualReportsWork,
        pamphletsWork,
        psychologyWork,
        darwinWork,
        mostThingsWork,
        nothingWork
      )

    val testCases = Table(
      ("query", "expectedIds", "clue"),
      ("Annual reports.", Seq(annualReportsWork), "single match single genre"),
      (
        "Pamphlets.",
        Seq(pamphletsWork, mostThingsWork),
        "multi match single genre"
      ),
      (
        "Annual reports.,Pamphlets.",
        Seq(annualReportsWork, pamphletsWork, mostThingsWork),
        "comma separated"
      ),
      (
        """Annual reports.,"Psychology, Pathological"""",
        Seq(annualReportsWork, psychologyWork, mostThingsWork),
        "commas in quotes"
      ),
      (
        """"Darwin \"Jones\", Charles","Psychology, Pathological",Pamphlets.""",
        Seq(darwinWork, psychologyWork, mostThingsWork, pamphletsWork),
        "escaped quotes in quotes"
      )
    )

    it("filters by genres as a comma separated list") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          forAll(testCases) {
            (query: String, expectedIds: Seq[String], clue: String) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  path =
                    s"$rootPath/works?genres.label=${URLEncoder.encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(expectedIds)
                }
              }
          }
      }
    }
  }

  describe("filtering works") {
    val sanitationWork = "works.examples.subject-filters-tests.0"
    val londonWork = "works.examples.subject-filters-tests.1"
    val psychologyWork = "works.examples.subject-filters-tests.2"
    val darwinWork = "works.examples.subject-filters-tests.3"
    val mostThingsWork = "works.examples.subject-filters-tests.4"
    val nothingWork = "works.examples.subject-filters-tests.5"

    val works =
      List(
        sanitationWork,
        londonWork,
        psychologyWork,
        darwinWork,
        mostThingsWork,
        nothingWork
      )

    val testCases = Table(
      ("filterName", "query", "expectedIds", "clue"),
      (
        "subjects.label",
        "Sanitation.",
        Seq(sanitationWork),
        "single match single subject"
      ),
      (
        "subjects.label",
        "London (England)",
        Seq(londonWork, mostThingsWork),
        "multi match single subject"
      ),
      (
        "subjects.label",
        "Sanitation.,London (England)",
        Seq(sanitationWork, londonWork, mostThingsWork),
        "comma separated"
      ),
      (
        "subjects.label",
        """Sanitation.,"Psychology, Pathological"""",
        Seq(sanitationWork, psychologyWork, mostThingsWork),
        "commas in quotes"
      ),
      (
        "subjects.label",
        """"Darwin \"Jones\", Charles","Psychology, Pathological",London (England)""",
        Seq(darwinWork, psychologyWork, londonWork, mostThingsWork),
        "escaped quotes in quotes"
      )
    )

    it("filters by subjects") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          forAll(testCases) {
            (
              filterName,
              query: String,
              expectedIds: Seq[String],
              clue: String
            ) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  path =
                    s"$rootPath/works?$filterName=${URLEncoder.encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(expectedIds)
                }
              }
          }
      }
    }
  }

  describe("filtering works by contributors") {
    val patriciaWork = "works.examples.contributor-filters-tests.0"
    val karlMarxWork = "works.examples.contributor-filters-tests.1"
    val jakePaulWork = "works.examples.contributor-filters-tests.2"
    val darwinWork = "works.examples.contributor-filters-tests.3"
    val patriciaDarwinWork = "works.examples.contributor-filters-tests.4"
    val noContributorsWork = "works.examples.contributor-filters-tests.5"

    val works = List(
      patriciaWork,
      karlMarxWork,
      jakePaulWork,
      darwinWork,
      patriciaDarwinWork,
      noContributorsWork
    )

    val testCases = Table(
      ("query", "expectedIds", "clue"),
      ("Karl Marx", Seq(karlMarxWork), "single match"),
      (
        """"Bath, Patricia"""",
        Seq(patriciaWork, patriciaDarwinWork),
        "multi match"
      ),
      (
        "Karl Marx,Jake Paul",
        Seq(karlMarxWork, jakePaulWork),
        "comma separated"
      ),
      (
        """"Bath, Patricia",Karl Marx""",
        Seq(patriciaWork, patriciaDarwinWork, karlMarxWork),
        "commas in quotes"
      ),
      (
        """"Bath, Patricia",Karl Marx,"Darwin \"Jones\", Charles"""",
        Seq(patriciaWork, karlMarxWork, darwinWork, patriciaDarwinWork),
        "quotes in quotes"
      )
    )

    it("filters by contributors as a comma separated list") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          forAll(testCases) {
            (query: String, expectedIds: Seq[String], clue: String) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  path = s"$rootPath/works?contributors.agent.label=${URLEncoder
                    .encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(expectedIds)
                }
              }
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
