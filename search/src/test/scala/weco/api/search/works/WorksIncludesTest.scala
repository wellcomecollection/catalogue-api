package weco.api.search.works

class WorksIncludesTest extends ApiWorksTestBase {
  describe("identifiers includes") {
    it(
      "includes a list of identifiers on a list endpoint if we pass ?include=identifiers"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=identifiers"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "pageSize" : 10,
                 |  "results" : [
                 |    {
                 |      "alternativeTitles" : [
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "id" : "jfzz4ou9",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "Is1ajgcgP8"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "FhqVjVef8B"
                 |        }
                 |      ],
                 |      "title" : "A work with all the include-able fields",
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "alternativeTitles" : [
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "id" : "nlqnlwch",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "calm-record-id",
                 |            "label" : "Calm RecordIdentifier",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "jfcicmGMGK"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "aGKxAEeG0H"
                 |        }
                 |      ],
                 |      "title" : "A work with all the include-able fields",
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "alternativeTitles" : [
                 |      ],
                 |      "availabilities" : [
                 |      ],
                 |      "id" : "tmdfbk5k",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "Aic5qOhRoS"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "UfcQYSxE7g"
                 |        }
                 |      ],
                 |      "title" : "A work with all the include-able fields",
                 |      "type" : "Work"
                 |    }
                 |  ],
                 |  "totalPages" : 1,
                 |  "totalResults" : 3,
                 |  "type" : "ResultList"
                 |}
                 |""".stripMargin
          }
      }
    }

    it(
      "includes a list of identifiers on a single work endpoint if we pass ?include=identifiers"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=identifiers"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "alternativeTitles" : [
                 |  ],
                 |  "availabilities" : [
                 |  ],
                 |  "id" : "tmdfbk5k",
                 |  "identifiers" : [
                 |    {
                 |      "identifierType" : {
                 |        "id" : "miro-image-number",
                 |        "label" : "Miro image number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "value" : "Aic5qOhRoS",
                 |      "type" : "Identifier"
                 |    },
                 |    {
                 |      "identifierType" : {
                 |        "id" : "sierra-system-number",
                 |        "label" : "Sierra system number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "value" : "UfcQYSxE7g",
                 |      "type" : "Identifier"
                 |    }
                 |  ],
                 |  "title" : "A work with all the include-able fields",
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  it("renders the items if the items include is present") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works/tmdfbk5k?include=items"
        ) {
          Status.OK ->
            s"""
               |{
               |  "alternativeTitles" : [
               |  ],
               |  "availabilities" : [
               |  ],
               |  "id" : "tmdfbk5k",
               |  "items" : [
               |    {
               |      "id" : "ca3anii6",
               |      "locations" : [
               |        {
               |          "accessConditions" : [
               |          ],
               |          "license" : {
               |            "id" : "cc-by",
               |            "label" : "Attribution 4.0 International (CC BY 4.0)",
               |            "type" : "License",
               |            "url" : "http://creativecommons.org/licenses/by/4.0/"
               |          },
               |          "locationType" : {
               |            "id" : "iiif-presentation",
               |            "label" : "IIIF Presentation API",
               |            "type" : "LocationType"
               |          },
               |          "type" : "DigitalLocation",
               |          "url" : "https://iiif.wellcomecollection.org/image/oRi.jpg/info.json"
               |        }
               |      ],
               |      "type" : "Item"
               |    },
               |    {
               |      "id" : "tuqkgha7",
               |      "locations" : [
               |        {
               |          "accessConditions" : [
               |          ],
               |          "credit" : "Credit line: ZX3jETt",
               |          "license" : {
               |            "id" : "cc-by",
               |            "label" : "Attribution 4.0 International (CC BY 4.0)",
               |            "type" : "License",
               |            "url" : "http://creativecommons.org/licenses/by/4.0/"
               |          },
               |          "linkText" : "Link text: 934EUQbvCh",
               |          "locationType" : {
               |            "id" : "iiif-presentation",
               |            "label" : "IIIF Presentation API",
               |            "type" : "LocationType"
               |          },
               |          "type" : "DigitalLocation",
               |          "url" : "https://iiif.wellcomecollection.org/image/xlG.jpg/info.json"
               |        }
               |      ],
               |      "type" : "Item"
               |    },
               |    {
               |      "locations" : [
               |        {
               |          "accessConditions" : [
               |          ],
               |          "license" : {
               |            "id" : "cc-by",
               |            "label" : "Attribution 4.0 International (CC BY 4.0)",
               |            "type" : "License",
               |            "url" : "http://creativecommons.org/licenses/by/4.0/"
               |          },
               |          "locationType" : {
               |            "id" : "iiif-presentation",
               |            "label" : "IIIF Presentation API",
               |            "type" : "LocationType"
               |          },
               |          "type" : "DigitalLocation",
               |          "url" : "https://iiif.wellcomecollection.org/image/xtr.jpg/info.json"
               |        }
               |      ],
               |      "type" : "Item"
               |    }
               |  ],
               |  "title" : "A work with all the include-able fields",
               |  "type" : "Work"
               |}
               |""".stripMargin
        }
    }
  }

  describe("subject includes") {
    it(
      "includes a list of subjects on a list endpoint if we pass ?include=subjects"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=subjects") {
            Status.OK ->
              """
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "subjects" : [
                        {
                          "concepts" : [
                            {
                              "label" : "hKQNGv31h63sJts",
                              "type" : "Concept"
                            },
                            {
                              "label" : "RuBvzw4YQmyoWab",
                              "type" : "Concept"
                            },
                            {
                              "label" : "AgXxwlS5TtSeuKJ",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "dXe1K5osW8",
                          "type" : "Subject"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "AUKxcPeEWECb7cJ",
                              "type" : "Concept"
                            },
                            {
                              "label" : "uhQpXNFDoA7rpYP",
                              "type" : "Concept"
                            },
                            {
                              "label" : "iDfifsBwXAEPbWR",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "t4fspOh5tl",
                          "type" : "Subject"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "subjects" : [
                        {
                          "concepts" : [
                            {
                              "label" : "VvWxgXRSdhZCyeu",
                              "type" : "Concept"
                            },
                            {
                              "label" : "lPkNaP0Clgfwapm",
                              "type" : "Concept"
                            },
                            {
                              "label" : "D7jxiopplRbppKZ",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "zUX6yZ5LLM",
                          "type" : "Subject"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "EIy1m22EMkmWOjl",
                              "type" : "Concept"
                            },
                            {
                              "label" : "KvfRGKp22pTPNg1",
                              "type" : "Concept"
                            },
                            {
                              "label" : "Fb7hLZCWP2ToHCa",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "MbAm0vPSF4",
                          "type" : "Subject"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "id" : "tmdfbk5k",
                      "subjects" : [
                        {
                          "concepts" : [
                            {
                              "label" : "goKOwWLrIbnrzZj",
                              "type" : "Concept"
                            },
                            {
                              "label" : "i3JH82kKuArEtlV",
                              "type" : "Concept"
                            },
                            {
                              "label" : "dV0jg08I834KKSX",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "RGOo9Fg6ic",
                          "type" : "Subject"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "FakoqsVT1GlsNpY",
                              "type" : "Concept"
                            },
                            {
                              "label" : "pthDMBLQZhG54Nz",
                              "type" : "Concept"
                            },
                            {
                              "label" : "omzMOR7nUmbDY87",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "k1WGWfqE6x",
                          "type" : "Subject"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                 """
          }
      }
    }

    it(
      "includes a list of subjects on a single work endpoint if we pass ?include=subjects"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=subjects"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "alternativeTitles" : [
                 |  ],
                 |  "availabilities" : [
                 |  ],
                 |  "id" : "tmdfbk5k",
                 |  "subjects" : [
                 |    {
                 |      "concepts" : [
                 |        {
                 |          "label" : "goKOwWLrIbnrzZj",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "i3JH82kKuArEtlV",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "dV0jg08I834KKSX",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "label" : "RGOo9Fg6ic",
                 |      "type" : "Subject"
                 |    },
                 |    {
                 |      "concepts" : [
                 |        {
                 |          "label" : "FakoqsVT1GlsNpY",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "pthDMBLQZhG54Nz",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "omzMOR7nUmbDY87",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "label" : "k1WGWfqE6x",
                 |      "type" : "Subject"
                 |    }
                 |  ],
                 |  "title" : "A work with all the include-able fields",
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("genre includes") {
    it(
      "includes a list of genres on a list endpoint if we pass ?include=genres"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=genres") {
            Status.OK ->
              s"""
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "genres" : [
                        {
                          "concepts" : [
                            {
                              "label" : "MAgtlZTK6GyLzSr",
                              "type" : "Concept"
                            },
                            {
                              "label" : "eGLHq3k05FqagUa",
                              "type" : "Concept"
                            },
                            {
                              "label" : "uios0IzDAQA5geM",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "aGNi2HdKsP",
                          "type" : "Genre"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "ZGNVldxfQFhVctB",
                              "type" : "Concept"
                            },
                            {
                              "label" : "FwsNPAX3Wg9bz6n",
                              "type" : "Concept"
                            },
                            {
                              "label" : "2QtjPUbfZXARrSC",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "ZT0FFS0zAA",
                          "type" : "Genre"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "genres" : [
                        {
                          "concepts" : [
                            {
                              "label" : "WAxFOW2wBgqY0L1",
                              "type" : "Concept"
                            },
                            {
                              "label" : "ZF1PYZIWOP5xioq",
                              "type" : "Concept"
                            },
                            {
                              "label" : "rw7OhiAUBgzynPV",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "1SqaQCD06A",
                          "type" : "Genre"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "e6G99aHcLRRBkyn",
                              "type" : "Concept"
                            },
                            {
                              "label" : "24xZvMlY5yFST4h",
                              "type" : "Concept"
                            },
                            {
                              "label" : "zY1cKoWGXrGHlOd",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "yy1BmoiqJb",
                          "type" : "Genre"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "genres" : [
                        {
                          "concepts" : [
                            {
                              "label" : "IHQR23GK9tQdPt3",
                              "type" : "Concept"
                            },
                            {
                              "label" : "acHhNKnNq4fR1f4",
                              "type" : "Concept"
                            },
                            {
                              "label" : "tFlVnFnK1Qv0bPi",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "Uw1LvlTE5c",
                          "type" : "Genre"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "DgAW3Gj5M4V6Fk1",
                              "type" : "Concept"
                            },
                            {
                              "label" : "Uu2KL5QXsRV8G10",
                              "type" : "Concept"
                            },
                            {
                              "label" : "ObxxWE1MLX8biK5",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "YMZquffyNP",
                          "type" : "Genre"
                        }
                      ],
                      "id" : "tmdfbk5k",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it(
      "includes a list of genres on a single work endpoint if we pass ?include=genres"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=genres"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "genres" : [
                    {
                      "concepts" : [
                        {
                          "label" : "IHQR23GK9tQdPt3",
                          "type" : "Concept"
                        },
                        {
                          "label" : "acHhNKnNq4fR1f4",
                          "type" : "Concept"
                        },
                        {
                          "label" : "tFlVnFnK1Qv0bPi",
                          "type" : "Concept"
                        }
                      ],
                      "label" : "Uw1LvlTE5c",
                      "type" : "Genre"
                    },
                    {
                      "concepts" : [
                        {
                          "label" : "DgAW3Gj5M4V6Fk1",
                          "type" : "Concept"
                        },
                        {
                          "label" : "Uu2KL5QXsRV8G10",
                          "type" : "Concept"
                        },
                        {
                          "label" : "ObxxWE1MLX8biK5",
                          "type" : "Concept"
                        }
                      ],
                      "label" : "YMZquffyNP",
                      "type" : "Genre"
                    }
                  ],
                  "id" : "tmdfbk5k",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("contributor includes") {
    it(
      "includes a list of contributors on a list endpoint if we pass ?include=contributors"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=contributors"
          ) {
            Status.OK ->
              s"""
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "contributors" : [
                        {
                          "agent" : {
                            "label" : "person-UFES8H2",
                            "type" : "Person"
                          },
                          "primary" : true,
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        },
                        {
                          "agent" : {
                            "label" : "person-LQeClX",
                            "type" : "Person"
                          },
                          "primary" : true,
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "contributors" : [
                        {
                          "agent" : {
                            "label" : "person-kGPDWLL",
                            "type" : "Person"
                          },
                          "primary" : true,
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        },
                        {
                          "agent" : {
                            "label" : "person-2Xsatm0",
                            "type" : "Person"
                          },
                          "primary" : true,
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "contributors" : [
                        {
                          "agent" : {
                            "label" : "person-W9SVIX0fEg",
                            "type" : "Person"
                          },
                          "primary" : true,
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        },
                        {
                          "agent" : {
                            "label" : "person-IWtB2H6",
                            "type" : "Person"
                          },
                          "primary" : true,
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        }
                      ],
                      "id" : "tmdfbk5k",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it(
      "includes a list of contributors on a single work endpoint if we pass ?include=contributors"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=contributors"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "contributors" : [
                    {
                      "agent" : {
                        "label" : "person-W9SVIX0fEg",
                        "type" : "Person"
                      },
                      "primary" : true,
                      "roles" : [
                      ],
                      "type" : "Contributor"
                    },
                    {
                      "agent" : {
                        "label" : "person-IWtB2H6",
                        "type" : "Person"
                      },
                      "primary" : true,
                      "roles" : [
                      ],
                      "type" : "Contributor"
                    }
                  ],
                  "id" : "tmdfbk5k",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("production includes") {
    it(
      "includes a list of production events on a list endpoint if we pass ?include=production"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=production"
          ) {
            Status.OK ->
              s"""
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "production" : [
                        {
                          "agents" : [
                            {
                              "label" : "CtekEl0aPD",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "tepg6",
                              "type" : "Period"
                            }
                          ],
                          "label" : "BQOO1Lux1bNFDaOKTsDKbwser",
                          "places" : [
                            {
                              "label" : "Vibco3T5a2",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        },
                        {
                          "agents" : [
                            {
                              "label" : "H9vtTXh5eC",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "S91lO",
                              "type" : "Period"
                            }
                          ],
                          "label" : "aLKLw3A3d2SgHOvziH7duUdyn",
                          "places" : [
                            {
                              "label" : "ba2c8ykXyK",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "production" : [
                        {
                          "agents" : [
                            {
                              "label" : "Pe7X4srn7y",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "dL6XA",
                              "type" : "Period"
                            }
                          ],
                          "label" : "jdGFZw7o2IL5cpTq2szbf8JXF",
                          "places" : [
                            {
                              "label" : "RL51HaiqMv",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        },
                        {
                          "agents" : [
                            {
                              "label" : "ITHk4Fal6y",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "LEuJL",
                              "type" : "Period"
                            }
                          ],
                          "label" : "AgnoMEDJADyhgl97NLnSGT3Oo",
                          "places" : [
                            {
                              "label" : "iRibBOUDMp",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "id" : "tmdfbk5k",
                      "production" : [
                        {
                          "agents" : [
                            {
                              "label" : "dUXSLHjmev",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "8ssuR",
                              "type" : "Period"
                            }
                          ],
                          "label" : "zRpFAch6oMgo8xazsI6am8fhY",
                          "places" : [
                            {
                              "label" : "Nr11C9xxYE",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        },
                        {
                          "agents" : [
                            {
                              "label" : "frr0xAeKZI",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "7aEhJ",
                              "type" : "Period"
                            }
                          ],
                          "label" : "0QNRzQw5sJ896lVoGuWXqO9eq",
                          "places" : [
                            {
                              "label" : "zvroF4g2k9",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it(
      "includes a list of production on a single work endpoint if we pass ?include=production"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=production"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "production" : [
                    {
                      "agents" : [
                        {
                          "label" : "dUXSLHjmev",
                          "type" : "Person"
                        }
                      ],
                      "dates" : [
                        {
                          "label" : "8ssuR",
                          "type" : "Period"
                        }
                      ],
                      "label" : "zRpFAch6oMgo8xazsI6am8fhY",
                      "places" : [
                        {
                          "label" : "Nr11C9xxYE",
                          "type" : "Place"
                        }
                      ],
                      "type" : "ProductionEvent"
                    },
                    {
                      "agents" : [
                        {
                          "label" : "frr0xAeKZI",
                          "type" : "Person"
                        }
                      ],
                      "dates" : [
                        {
                          "label" : "7aEhJ",
                          "type" : "Period"
                        }
                      ],
                      "label" : "0QNRzQw5sJ896lVoGuWXqO9eq",
                      "places" : [
                        {
                          "label" : "zvroF4g2k9",
                          "type" : "Place"
                        }
                      ],
                      "type" : "ProductionEvent"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("languages includes") {
    it("includes languages on a list endpoint if we pass ?include=languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works?include=languages"
          ) {
            Status.OK ->
              s"""
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "languages" : [
                        {
                          "id" : "6hh",
                          "label" : "PPssCoB",
                          "type" : "Language"
                        },
                        {
                          "id" : "EHn",
                          "label" : "ZViPXHQHS",
                          "type" : "Language"
                        },
                        {
                          "id" : "7Jx",
                          "label" : "9xrQ6L",
                          "type" : "Language"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "languages" : [
                        {
                          "id" : "yc6",
                          "label" : "AydChzs",
                          "type" : "Language"
                        },
                        {
                          "id" : "hi6",
                          "label" : "ux209O7",
                          "type" : "Language"
                        },
                        {
                          "id" : "GkI",
                          "label" : "x8vXQy",
                          "type" : "Language"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "id" : "tmdfbk5k",
                      "languages" : [
                        {
                          "id" : "qbV",
                          "label" : "83c9SMhIH",
                          "type" : "Language"
                        },
                        {
                          "id" : "2DH",
                          "label" : "Ap8e8Su",
                          "type" : "Language"
                        },
                        {
                          "id" : "yMV",
                          "label" : "QTT2Ir",
                          "type" : "Language"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it("includes languages on a work endpoint if we pass ?include=languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=languages"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "languages" : [
                    {
                      "id" : "qbV",
                      "label" : "83c9SMhIH",
                      "type" : "Language"
                    },
                    {
                      "id" : "2DH",
                      "label" : "Ap8e8Su",
                      "type" : "Language"
                    },
                    {
                      "id" : "yMV",
                      "label" : "QTT2Ir",
                      "type" : "Language"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("notes includes") {
    it("includes notes on the list endpoint if we pass ?include=notes") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=notes") {
            Status.OK ->
              s"""
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "notes" : [
                        {
                          "contents" : [
                            "0NVp7GvR"
                          ],
                          "noteType" : {
                            "id" : "general-note",
                            "label" : "Notes",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        },
                        {
                          "contents" : [
                            "6uOUAu",
                            "RE2canCUR",
                            "No7cLokW4L"
                          ],
                          "noteType" : {
                            "id" : "location-of-duplicates",
                            "label" : "Location of duplicates",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "notes" : [
                        {
                          "contents" : [
                            "MM9NU3A"
                          ],
                          "noteType" : {
                            "id" : "general-note",
                            "label" : "Notes",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        },
                        {
                          "contents" : [
                            "6KqXfdMBn"
                          ],
                          "noteType" : {
                            "id" : "location-of-duplicates",
                            "label" : "Location of duplicates",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        },
                        {
                          "contents" : [
                            "LHOqPv8Cs",
                            "YxC3RLI"
                          ],
                          "noteType" : {
                            "id" : "funding-info",
                            "label" : "Funding information",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "id" : "tmdfbk5k",
                      "notes" : [
                        {
                          "contents" : [
                            "hy1sdDDR"
                          ],
                          "noteType" : {
                            "id" : "general-note",
                            "label" : "Notes",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        },
                        {
                          "contents" : [
                            "FaY5fj9BpO"
                          ],
                          "noteType" : {
                            "id" : "location-of-duplicates",
                            "label" : "Location of duplicates",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        },
                        {
                          "contents" : [
                            "KLQFKzgro",
                            "PkIHAHMj"
                          ],
                          "noteType" : {
                            "id" : "funding-info",
                            "label" : "Funding information",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it("includes notes on the single work endpoint if we pass ?include=notes") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=notes"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "notes" : [
                    {
                      "contents" : [
                        "hy1sdDDR"
                      ],
                      "noteType" : {
                        "id" : "general-note",
                        "label" : "Notes",
                        "type" : "NoteType"
                      },
                      "type" : "Note"
                    },
                    {
                      "contents" : [
                        "FaY5fj9BpO"
                      ],
                      "noteType" : {
                        "id" : "location-of-duplicates",
                        "label" : "Location of duplicates",
                        "type" : "NoteType"
                      },
                      "type" : "Note"
                    },
                    {
                      "contents" : [
                        "KLQFKzgro",
                        "PkIHAHMj"
                      ],
                      "noteType" : {
                        "id" : "funding-info",
                        "label" : "Funding information",
                        "type" : "NoteType"
                      },
                      "type" : "Note"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("image includes") {
    it(
      "includes a list of images on the list endpoint if we pass ?include=images"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=images") {
            Status.OK ->
              """
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "images" : [
                        {
                          "id" : "y3fstphe",
                          "type" : "Image"
                        },
                        {
                          "id" : "whuucljo",
                          "type" : "Image"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "images" : [
                        {
                          "id" : "do1dzyoo",
                          "type" : "Image"
                        },
                        {
                          "id" : "jfro7laj",
                          "type" : "Image"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "id" : "tmdfbk5k",
                      "images" : [
                        {
                          "id" : "eoedbdmz",
                          "type" : "Image"
                        },
                        {
                          "id" : "w1yfrq9a",
                          "type" : "Image"
                        }
                      ],
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it(
      "includes a list of images on a single work endpoint if we pass ?include=images"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=images"
          ) {
            Status.OK ->
              """
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "images" : [
                    {
                      "id" : "eoedbdmz",
                      "type" : "Image"
                    },
                    {
                      "id" : "w1yfrq9a",
                      "type" : "Image"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("relation includes") {
    it("includes parts") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=parts"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "parts" : [
                    {
                      "id" : "a7om88xm",
                      "title" : "title-nN4RHJX7OP",
                      "totalDescendentParts" : 0,
                      "totalParts" : 0,
                      "type" : "Work"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }

    it("includes partOf") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=partOf"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "partOf" : [
                    {
                      "id" : "nrvdy0jg",
                      "partOf" : [
                        {
                          "id" : "0cs6cerb",
                          "title" : "title-b1iZslIT5y",
                          "totalDescendentParts" : 5,
                          "totalParts" : 1,
                          "type" : "Work"
                        }
                      ],
                      "title" : "title-MS5Hy6x38N",
                      "totalDescendentParts" : 4,
                      "totalParts" : 3,
                      "type" : "Work"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }

    it("includes precededBy") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=precededBy"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "precededBy" : [
                    {
                      "id" : "1gnd7b0m",
                      "title" : "title-tnetMtnM6n",
                      "totalDescendentParts" : 0,
                      "totalParts" : 0,
                      "type" : "Work"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }

    it("includes succeededBy") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=succeededBy"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "id" : "tmdfbk5k",
                  "succeededBy" : [
                    {
                      "id" : "uxg4ed5m",
                      "title" : "title-Ia7Ze4ZWUM",
                      "totalDescendentParts" : 0,
                      "totalParts" : 0,
                      "type" : "Work"
                    }
                  ],
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }

  describe("holdings includes") {
    it("on the list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=holdings") {
            Status.OK ->
              s"""
                {
                  "pageSize" : 10,
                  "results" : [
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "open-shelves",
                          "label" : "Open shelves",
                          "type" : "Availability"
                        },
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "holdings" : [
                        {
                          "enumeration" : [
                            "ZThZZrd6",
                            "RqVgEX",
                            "JnvNoiuf",
                            "WNuyDmwtJx",
                            "AIELEn",
                            "rWrk5Fv",
                            "CEKkIZfsA",
                            "Pwunml",
                            "w7s12i"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "license" : {
                              "id" : "ogl",
                              "label" : "Open Government Licence",
                              "type" : "License",
                              "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"
                            },
                            "locationType" : {
                              "id" : "open-shelves",
                              "label" : "Open shelves",
                              "type" : "LocationType"
                            },
                            "shelfmark" : "Shelfmark: 1E6F0oBW",
                            "type" : "PhysicalLocation"
                          },
                          "note" : "libcScRo1F",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "hldbPB44o8",
                            "1009JS",
                            "Ibf5YS",
                            "xm2Kats",
                            "MXhTGxz9",
                            "fcN5BFmTAn",
                            "zGP9LZNM0",
                            "Cw6Z9r",
                            "1RAcTOrmR"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "locationType" : {
                              "id" : "closed-stores",
                              "label" : "Closed stores",
                              "type" : "LocationType"
                            },
                            "shelfmark" : "Shelfmark: uH8tqe0Ga",
                            "type" : "PhysicalLocation"
                          },
                          "note" : "PTlqNXqKK",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "D4qF34g",
                            "XCYEAsAu4Y"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "license" : {
                              "id" : "ogl",
                              "label" : "Open Government Licence",
                              "type" : "License",
                              "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"
                            },
                            "locationType" : {
                              "id" : "open-shelves",
                              "label" : "Open shelves",
                              "type" : "LocationType"
                            },
                            "type" : "PhysicalLocation"
                          },
                          "type" : "Holdings"
                        }
                      ],
                      "id" : "jfzz4ou9",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                        {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "holdings" : [
                        {
                          "enumeration" : [
                            "UNX23BmQh",
                            "YWQPxyMQ",
                            "7dlsrFBR8Q",
                            "LIBD4V4eC",
                            "5soceYhE",
                            "Xco4CAZl",
                            "NX6YfF9ung",
                            "68x12CqOH",
                            "tD46Q9sHcs"
                          ],
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "nsuXoz",
                            "RLuAZL",
                            "a0DogXkz2",
                            "M4S3tuy",
                            "RUMGx0",
                            "x6rK1BxL",
                            "cV6Wb9Wq"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "license" : {
                              "id" : "ogl",
                              "label" : "Open Government Licence",
                              "type" : "License",
                              "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"
                            },
                            "locationType" : {
                              "id" : "closed-stores",
                              "label" : "Closed stores",
                              "type" : "LocationType"
                            },
                            "type" : "PhysicalLocation"
                          },
                          "note" : "TmY1as",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "LrUlQ4SYJ",
                            "p3ABwY",
                            "lV04E1A9I2",
                            "3fXDCODR4",
                            "FZmdTEa6",
                            "UlrveN",
                            "yaDKbXVDr",
                            "ZWmlAid",
                            "7LBYp6y",
                            "0xMucBp"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "license" : {
                              "id" : "pdm",
                              "label" : "Public Domain Mark",
                              "type" : "License",
                              "url" : "https://creativecommons.org/share-your-work/public-domain/pdm/"
                            },
                            "locationType" : {
                              "id" : "closed-stores",
                              "label" : "Closed stores",
                              "type" : "LocationType"
                            },
                            "shelfmark" : "Shelfmark: GLGn78mfcQ",
                            "type" : "PhysicalLocation"
                          },
                          "note" : "Bu2SeMYa",
                          "type" : "Holdings"
                        }
                      ],
                      "id" : "nlqnlwch",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
                    {
                      "alternativeTitles" : [
                      ],
                      "availabilities" : [
                      ],
                      "holdings" : [
                        {
                          "enumeration" : [
                            "RTIPEagBbz",
                            "ToDhoB",
                            "3OoUAz",
                            "AFPJNg",
                            "liIHvp",
                            "yCpsgbKX",
                            "6B6XI29t",
                            "C1KbU70mrq",
                            "m9N8dAz",
                            "1bAErlgjqn"
                          ],
                          "note" : "8o5wdlW2gc",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "mVMvzez7JF",
                            "7FYx3e0",
                            "ZI12L0U9K8",
                            "yAtfsUkFIG",
                            "BSj9Gf",
                            "Mi4wC1H",
                            "ZomzsnWpc",
                            "aAA6nO9h",
                            "XJqcYm9tl",
                            "p1QVJo"
                          ],
                          "note" : "xSAn4PBcz",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "Gbjg3Ifi",
                            "HlycAdT2L",
                            "laGIDVwG",
                            "CtLHFeu"
                          ],
                          "note" : "dQ5Aoz3",
                          "type" : "Holdings"
                        }
                      ],
                      "id" : "tmdfbk5k",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    }
                  ],
                  "totalPages" : 1,
                  "totalResults" : 3,
                  "type" : "ResultList"
                }
                """
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/tmdfbk5k?include=holdings"
          ) {
            Status.OK ->
              s"""
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "holdings" : [
                    {
                      "note" : "8o5wdlW2gc",
                      "enumeration" : [
                        "RTIPEagBbz",
                        "ToDhoB",
                        "3OoUAz",
                        "AFPJNg",
                        "liIHvp",
                        "yCpsgbKX",
                        "6B6XI29t",
                        "C1KbU70mrq",
                        "m9N8dAz",
                        "1bAErlgjqn"
                      ],
                      "type" : "Holdings"
                    },
                    {
                      "note" : "xSAn4PBcz",
                      "enumeration" : [
                        "mVMvzez7JF",
                        "7FYx3e0",
                        "ZI12L0U9K8",
                        "yAtfsUkFIG",
                        "BSj9Gf",
                        "Mi4wC1H",
                        "ZomzsnWpc",
                        "aAA6nO9h",
                        "XJqcYm9tl",
                        "p1QVJo"
                      ],
                      "type" : "Holdings"
                    },
                    {
                      "note" : "dQ5Aoz3",
                      "enumeration" : [
                        "Gbjg3Ifi",
                        "HlycAdT2L",
                        "laGIDVwG",
                        "CtLHFeu"
                      ],
                      "type" : "Holdings"
                    }
                  ],
                  "id" : "tmdfbk5k",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }
}
