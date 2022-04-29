package weco.api.search.works

class WorksFiltersTest extends ApiWorksTestBase {
  it("combines multiple filters") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,

          path = s"$rootPath/works?genres.label=4fR1f4tFlV&subjects.label=ArEtlVdV0j"
        ) {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
        }
    }
  }

  it("filters by item location type") {
    val worksLocation = (0 to 4).map(i => s"works.locations.$i")

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksLocation: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?items.locations.locationType=iiif-image"
        ) {
          Status.OK -> newWorksListResponse(ids = Seq("works.locations.1", "works.locations.2"))
        }

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?items.locations.locationType=iiif-image,closed-stores"
        ) {
          Status.OK -> newWorksListResponse(ids = Seq("works.locations.1", "works.locations.2", "works.locations.4"))
        }
    }
  }

  it("filters by format") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksFormat: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?workType=a") {
          Status.OK -> newWorksListResponse(ids = worksFormatBooks)
        }

        assertJsonResponse(routes, path = s"$rootPath/works?workType=a,i") {
          Status.OK -> newWorksListResponse(ids = worksFormatBooks ++ worksFormatAudio)
        }

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?query=work%20with%20format&workType=a,i"
        ) {
          Status.OK -> newWorksListResponse(ids = worksFormatBooks ++ worksFormatAudio)
        }
    }
  }

  it("filters by work type") {
    val collectionWorks = (0 to 2).map(i => s"works.workType.$i")
    val seriesWorks = (3 to 4).map(i => s"works.workType.$i")
    val sectionWork = (5 to 8).map(i => s"works.workType.$i")

    val works = collectionWorks ++ seriesWorks ++ sectionWork

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, s"$rootPath/works?type=Collection") {
          Status.OK -> newWorksListResponse(ids = collectionWorks)
        }

        assertJsonResponse(routes, s"$rootPath/works?type=Collection,Series") {
          Status.OK -> newWorksListResponse(ids = collectionWorks ++ seriesWorks)
        }
    }
  }

  describe("filtering by date range") {
    val works = Seq(
      "work-production.1098",
      "work-production.1900",
      "work-production.1904",
      "work-production.1976",
      "work-production.2020"
    )

    it("filters by date range") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works?production.dates.from=1900-01-01&production.dates.to=1960-01-01"
          ) {
            Status.OK -> newWorksListResponse(ids = Seq(
              "work-production.1900",
              "work-production.1904",
            ))
          }
      }
    }

    it("filters by from date") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?production.dates.from=1900-01-01"
          ) {
            Status.OK -> newWorksListResponse(ids = Seq(
              "work-production.1900",
              "work-production.1904",
              "work-production.1976",
              "work-production.2020",
            ))
          }
      }
    }

    it("filters by to date") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, works: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?production.dates.to=1960-01-01"
          ) {
            Status.OK -> newWorksListResponse(ids = Seq(
              "work-production.1098",
              "work-production.1900",
              "work-production.1904",
            ))
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

  it("filters by language") {
    val worksLanguageEng = Set(
      "works.languages.0.eng",
      "works.languages.1.eng",
      "works.languages.2.eng",
      "works.languages.3.eng+swe",
      "works.languages.4.eng+swe+tur"
    )
    val worksLanguageSwe = Set(
      "works.languages.3.eng+swe",
      "works.languages.4.eng+swe+tur",
      "works.languages.5.swe",
    )
    val worksLanguagesTur = Set(
      "works.languages.4.eng+swe+tur",
      "works.languages.6.tur",
    )

    val works = (worksLanguageEng ++ worksLanguageSwe ++ worksLanguagesTur).toSeq

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?languages=eng") {
          Status.OK -> newWorksListResponse(ids = worksLanguageEng.toSeq)
        }

        assertJsonResponse(routes, path = s"$rootPath/works?languages=eng,tur") {
          Status.OK -> newWorksListResponse(ids = (worksLanguageEng ++ worksLanguagesTur).toSeq)
        }
    }
  }

  it("filters by genre") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?genres.label=4fR1f4tFlV") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
        }

        assertJsonResponse(routes, path = s"$rootPath/works?genres.label=4fR1f4tFlV,D06AWAxFOW") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1"))
        }
    }
  }

  it("filters by subject") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?subjects.label=ArEtlVdV0j") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
        }

        assertJsonResponse(routes, path = s"$rootPath/works?subjects.label=ArEtlVdV0j,5LLMVvWxgX") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1"))
        }
    }
  }

  it("filters by contributor") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?contributors.agent.label=person-o8xazs") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
        }

        assertJsonResponse(routes, path = s"$rootPath/works?contributors.agent.label=person-o8xazs,person-Lu2Xsa") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1"))
        }
    }
  }

  it("filters by item license") {
    val works = (0 to 4).map(i => s"works.items-with-licenses.$i")

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?items.locations.license=cc-by") {
          Status.OK -> newWorksListResponse(ids = Seq("works.items-with-licenses.0", "works.items-with-licenses.1", "works.items-with-licenses.3"))
        }

        assertJsonResponse(routes, path = s"$rootPath/works?items.locations.license=cc-by,cc-by-nc") {
          Status.OK -> newWorksListResponse(ids = Seq("works.items-with-licenses.0", "works.items-with-licenses.1", "works.items-with-licenses.2", "works.items-with-licenses.3"))
        }
    }
  }

  describe("filters by identifier") {
    it("filters by a sourceIdentifier") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?identifiers=cQYSxE7gRG") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
          }
      }
    }

    it("filters by multiple sourceIdentifiers") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?identifiers=cQYSxE7gRG,mGMGKNlQnl") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1"))
          }
      }
    }

    it("filters by an otherIdentifier") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?identifiers=eG0HzUX6yZ") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.1"))
          }
      }
    }

    it("filters by a mixture of identifiers") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?identifiers=cQYSxE7gRG,eG0HzUX6yZ") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1"))
          }
      }
    }
  }

  it("filters by access status") {
    val works = (0 to 6).map(i => s"works.accessStatus.$i")

    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, works: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?items.locations.accessConditions.status=restricted,closed"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("works.accessStatus.0", "works.accessStatus.1", "works.accessStatus.2")
          )
        }

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?items.locations.accessConditions.status=licensed-resources"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("works.accessStatus.5", "works.accessStatus.6")
          )
        }

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?items.locations.accessConditions.status=!restricted,!closed"
        ) {
          Status.OK -> newWorksListResponse(
            ids = Seq("works.accessStatus.3", "works.accessStatus.4", "works.accessStatus.5", "works.accessStatus.6")
          )
        }
    }
  }

  it("filters by availabilities") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?availabilities=closed-stores") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1", "work.visible.everything.2"))
        }

        assertJsonResponse(routes, path = s"$rootPath/works?availabilities=open-shelves") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.2"))
        }
    }
  }

  it("filters by partOf") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(routes, path = s"$rootPath/works?partOf=dza7om88") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
        }

        assertJsonResponse(routes, path = s"$rootPath/works?partOf.title=title-BnN4RHJX7O") {
          Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
        }
    }
  }

  describe("item filters") {
    it("filters by canonical ID on items") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?items=ca3anii6") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
          }

          assertJsonResponse(routes, path = s"$rootPath/works?items=ca3anii6,kdcpazds") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.1"))
          }
      }
    }

    it("looks up source identifiers") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?items.identifiers=hKyStbKjx1") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0"))
          }

          assertJsonResponse(routes, path = s"$rootPath/works?items.identifiers=hKyStbKjx1,xz9bfcN5BF") {
            Status.OK -> newWorksListResponse(ids = Seq("work.visible.everything.0", "work.visible.everything.2"))
          }
      }
    }
  }
}
