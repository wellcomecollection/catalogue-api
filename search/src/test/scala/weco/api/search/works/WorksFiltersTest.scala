package weco.api.search.works

import org.scalatest.Assertion
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.work.generators.ItemsGenerators
import org.scalatest.prop.TableDrivenPropertyChecks
import weco.api.search.fixtures.TestDocumentFixtures
import weco.catalogue.internal_model.locations.AccessStatus.LicensedResources
import weco.catalogue.internal_model.locations._
import weco.catalogue.internal_model.work._
import weco.catalogue.internal_model.work.WorkState.Indexed

import java.net.URLEncoder

class WorksFiltersTest
    extends ApiWorksTestBase
    with ItemsGenerators
    with TestDocumentFixtures
    with TableDrivenPropertyChecks {

  it("combines multiple filters") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path =
            s"$rootPath/works?genres.label=4fR1f4tFlV&subjects.label=ArEtlVdV0j"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("work.visible.everything.0")
          )
        }
    }
  }

  it("filters works by item LocationType") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, "work.items-with-location-types.0", "work.items-with-location-types.1", "work.items-with-location-types.2")

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?items.locations.locationType=iiif-presentation,closed-stores"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("work.items-with-location-types.1", "work.items-with-location-types.2")
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
            Status.OK -> newWorksListResponse(
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
            s"$rootPath/works?workType=k,d"
          ) {
            Status.OK -> newWorksListResponse(
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
    val collectionWork =
      indexedWork().title("rats").workType(WorkType.Collection)
    val seriesWork =
      indexedWork().title("rats").workType(WorkType.Series)
    val sectionWork =
      indexedWork().title("rats").workType(WorkType.Section)

    val works = Seq(collectionWork, seriesWork, sectionWork)

    it("when listing works") {
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, works: _*)

          assertJsonResponse(routes, s"$rootPath/works?type=Collection") {
            Status.OK -> worksListResponse(works = Seq(collectionWork))
          }
      }
    }

    it("filters by multiple types") {
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works?type=Collection,Series",
            unordered = true
          ) {
            Status.OK -> worksListResponse(
              works = Seq(collectionWork, seriesWork).sortBy {
                _.state.canonicalId
              }
            )
          }
      }
    }

    it("when searching works") {
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works?query=rats&type=Series,Section",
            unordered = true
          ) {
            Status.OK -> worksListResponse(
              works = Seq(seriesWork, sectionWork).sortBy {
                _.state.canonicalId
              }
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
            Status.OK -> newWorksListResponse(
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
            Status.OK -> newWorksListResponse(
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
            Status.OK -> newWorksListResponse(
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
            Status.OK -> newWorksListResponse(
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
            Status.OK -> newWorksListResponse(
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

  describe("filtering works by genre") {
    val annualReports = createGenreWith("Annual reports.")
    val pamphlets = createGenreWith("Pamphlets.")
    val psychology = createGenreWith("Psychology, Pathological")
    val darwin = createGenreWith("Darwin \"Jones\", Charles")

    val annualReportsWork = indexedWork().genres(List(annualReports))
    val pamphletsWork = indexedWork().genres(List(pamphlets))
    val psychologyWork = indexedWork().genres(List(psychology))
    val darwinWork =
      indexedWork().genres(List(darwin))
    val mostThingsWork =
      indexedWork().genres(List(pamphlets, psychology, darwin))
    val nothingWork = indexedWork()

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
      ("query", "results", "clue"),
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
          insertIntoElasticsearch(worksIndex, works: _*)

          forAll(testCases) {
            (
              query: String,
              results: Seq[Work.Visible[WorkState.Indexed]],
              clue: String
            ) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  s"$rootPath/works?genres.label=${URLEncoder.encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(works = results.sortBy {
                    _.state.canonicalId
                  })
                }
              }
          }
      }
    }
  }

  describe("filtering works by subject") {
    val sanitation = createSubjectWith("Sanitation.")
    val london = createSubjectWith("London (England)")
    val psychology = createSubjectWith("Psychology, Pathological")
    val darwin = createSubjectWith("Darwin \"Jones\", Charles")

    val sanitationWork = indexedWork().subjects(List(sanitation))
    val londonWork = indexedWork().subjects(List(london))
    val psychologyWork = indexedWork().subjects(List(psychology))
    val darwinWork =
      indexedWork().subjects(List(darwin))
    val mostThingsWork =
      indexedWork().subjects(List(london, psychology, darwin))
    val nothingWork = indexedWork()

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
      ("query", "results", "clue"),
      ("Sanitation.", Seq(sanitationWork), "single match single subject"),
      (
        "London (England)",
        Seq(londonWork, mostThingsWork),
        "multi match single subject"
      ),
      (
        "Sanitation.,London (England)",
        Seq(sanitationWork, londonWork, mostThingsWork),
        "comma separated"
      ),
      (
        """Sanitation.,"Psychology, Pathological"""",
        Seq(sanitationWork, psychologyWork, mostThingsWork),
        "commas in quotes"
      ),
      (
        """"Darwin \"Jones\", Charles","Psychology, Pathological",London (England)""",
        Seq(darwinWork, psychologyWork, londonWork, mostThingsWork),
        "escaped quotes in quotes"
      )
    )

    it("filters by subjects as a comma separated list") {
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, works: _*)
          forAll(testCases) {
            (
              query: String,
              results: Seq[Work.Visible[WorkState.Indexed]],
              clue: String
            ) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  s"$rootPath/works?subjects.label=${URLEncoder.encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(works = results.sortBy {
                    _.state.canonicalId
                  })
                }
              }
          }
      }
    }
  }

  describe("filtering works by contributors") {
    val patricia = Contributor(agent = Person("Bath, Patricia"), roles = Nil)
    val karlMarx = Contributor(agent = Person("Karl Marx"), roles = Nil)
    val jakePaul = Contributor(agent = Person("Jake Paul"), roles = Nil)
    val darwin =
      Contributor(agent = Person("Darwin \"Jones\", Charles"), roles = Nil)

    val patriciaWork = indexedWork().contributors(List(patricia))
    val karlMarxWork =
      indexedWork().contributors(List(karlMarx))
    val jakePaulWork =
      indexedWork().contributors(List(jakePaul))
    val darwinWork = indexedWork().contributors(List(darwin))
    val patriciaDarwinWork = indexedWork()
      .contributors(List(patricia, darwin))
    val noContributorsWork = indexedWork().contributors(Nil)

    val works = List(
      patriciaWork,
      karlMarxWork,
      jakePaulWork,
      darwinWork,
      patriciaDarwinWork,
      noContributorsWork
    )

    val testCases = Table(
      ("query", "results", "clue"),
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
          insertIntoElasticsearch(worksIndex, works: _*)
          forAll(testCases) {
            (
              query: String,
              results: Seq[Work.Visible[WorkState.Indexed]],
              clue: String
            ) =>
              withClue(clue) {
                assertJsonResponse(
                  routes,
                  s"$rootPath/works?contributors.agent.label=${URLEncoder
                    .encode(query, "UTF-8")}"
                ) {
                  Status.OK -> worksListResponse(works = results.sortBy {
                    _.state.canonicalId
                  })
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
            Status.OK -> newWorksListResponse(
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
            Status.OK -> newWorksListResponse(
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
            path = s"$rootPath/works?identifiers=cQYSxE7gRG"
          ) {
            Status.OK -> newWorksListResponse(
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
            path = s"$rootPath/works?identifiers=cQYSxE7gRG,mGMGKNlQnl"
          ) {
            Status.OK -> newWorksListResponse(
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
            path = s"$rootPath/works?identifiers=eG0HzUX6yZ"
          ) {
            Status.OK -> newWorksListResponse(
              ids = Seq("work.visible.everything.1")
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
            path = s"$rootPath/works?identifiers=eG0HzUX6yZ,ji3JH82kKu"
          ) {
            Status.OK -> newWorksListResponse(
              ids =
                Seq("work.visible.everything.0", "work.visible.everything.1")
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
            path = s"$rootPath/works?identifiers=cQYSxE7gRG,eG0HzUX6yZ"
          ) {
            Status.OK -> newWorksListResponse(
              ids =
                Seq("work.visible.everything.0", "work.visible.everything.1")
            )
          }
      }
    }
  }

  describe("Access status filter") {
    def work(status: AccessStatus): Work.Visible[Indexed] =
      indexedWork()
        .items(
          List(
            createIdentifiedItemWith(
              locations = List(
                createDigitalLocationWith(
                  accessConditions = List(
                    AccessCondition(
                      method = AccessMethod.ManualRequest,
                      status = Some(status)
                    )
                  )
                )
              )
            )
          )
        )

    val workA = work(AccessStatus.Restricted)
    val workB = work(AccessStatus.Restricted)
    val workC = work(AccessStatus.Closed)
    val workD = work(AccessStatus.Open)
    val workE = work(AccessStatus.OpenWithAdvisory)
    val workF = work(
      AccessStatus.LicensedResources(relationship = LicensedResources.Resource)
    )
    val workG = work(
      AccessStatus
        .LicensedResources(relationship = LicensedResources.RelatedResource)
    )

    val works = Seq(workA, workB, workC, workD, workE, workF, workG)

    it("includes works by access status") {
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, works: _*)
          assertJsonResponse(
            routes,
            s"$rootPath/works?items.locations.accessConditions.status=restricted,closed"
          ) {
            Status.OK -> worksListResponse(
              works = Seq(workA, workB, workC).sortBy(_.state.canonicalId)
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
          insertIntoElasticsearch(worksIndex, works: _*)
          assertJsonResponse(
            routes,
            s"$rootPath/works?items.locations.accessConditions.status=licensed-resources"
          ) {
            Status.OK -> worksListResponse(
              works = Seq(workF, workG).sortBy(_.state.canonicalId)
            )
          }
      }
    }

    it("excludes works by access status") {
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, works: _*)
          assertJsonResponse(
            routes,
            s"$rootPath/works?items.locations.accessConditions.status=!restricted,!closed"
          ) {
            Status.OK -> worksListResponse(
              works =
                Seq(workD, workE, workF, workG).sortBy(_.state.canonicalId)
            )
          }
      }
    }
  }

  describe("availabilities filter") {
    it("filters by availability ID") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?availabilities=open-shelves"
          ) {
            Status.OK -> newWorksListResponse(
              ids = Seq("work.visible.everything.2")
            )
          }
      }
    }

    it("filters by multiple comma-separated availability IDs") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything ++ visibleWorks: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?availabilities=open-shelves,closed-stores"
          ) {
            Status.OK -> newWorksListResponse(
              ids = Seq(
                "work.visible.everything.0",
                "work.visible.everything.1",
                "work.visible.everything.2"
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

          assertJsonResponse(routes, path = s"$rootPath/works?partOf=dza7om88") {
            Status.OK -> newWorksListResponse(
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
            path = s"$rootPath/works?partOf=title-BnN4RHJX7O"
          ) {
            Status.OK -> newWorksListResponse(
              ids = Seq("work.visible.everything.0")
            )
          }
      }
    }
  }

  describe("item filters") {
    val item1 = createIdentifiedItem
    val item2 = createIdentifiedItem
    val item3 = createIdentifiedItemWith(
      otherIdentifiers = List(createSourceIdentifier)
    )
    val item4 = createIdentifiedItemWith(
      otherIdentifiers = List(
        createSourceIdentifier,
        createSourceIdentifier
      )
    )

    val workA = indexedWork().items(List(item1, item2))
    val workB = indexedWork().items(List(item1))
    val workC = indexedWork().items(List(item2, item3))
    val workD = indexedWork().items(List(item3, item4))

    it("filters by canonical ID on items") {
      assertItemsFilterWorks(
        path = s"$rootPath/works?items=${item1.id.canonicalId}",
        expectedWorks = Seq(workA, workB)
      )

      assertItemsFilterWorks(
        path = s"$rootPath/works?items=${item3.id.canonicalId}",
        expectedWorks = Seq(workC, workD)
      )
    }

    it("looks up multiple canonical IDs") {
      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items=${item1.id.canonicalId},${item3.id.canonicalId}",
        expectedWorks = Seq(workA, workB, workC, workD)
      )

      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items=${item2.id.canonicalId},${item3.id.canonicalId}",
        expectedWorks = Seq(workA, workC, workD)
      )

      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items=${item3.id.canonicalId},${item4.id.canonicalId}",
        expectedWorks = Seq(workC, workD)
      )
    }

    it("looks up source identifiers") {
      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items.identifiers=${item3.id.sourceIdentifier.value}",
        expectedWorks = Seq(workC, workD)
      )

      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items.identifiers=${item1.id.sourceIdentifier.value}",
        expectedWorks = Seq(workA, workB)
      )
    }

    it("looks up multiple source identifiers") {
      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items.identifiers=${item2.id.sourceIdentifier.value},${item3.id.sourceIdentifier.value}",
        expectedWorks = Seq(workA, workC, workD)
      )
    }

    it("looks up other identifiers") {
      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items.identifiers=${item4.id.otherIdentifiers.head.value}",
        expectedWorks = Seq(workD)
      )
    }

    it("looks up multiple other identifiers") {
      assertItemsFilterWorks(
        path =
          s"$rootPath/works?items.identifiers=${item4.id.otherIdentifiers.head.value},${item3.id.otherIdentifiers.head.value}",
        expectedWorks = Seq(workC, workD)
      )
    }

    def assertItemsFilterWorks(
      path: String,
      expectedWorks: Seq[Work.Visible[Indexed]]
    ): Assertion =
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(worksIndex, workA, workB, workC, workD)

          assertJsonResponse(routes, path) {
            Status.OK -> worksListResponse(
              works = expectedWorks.sortBy(_.state.canonicalId)
            )
          }
      }
  }
}
