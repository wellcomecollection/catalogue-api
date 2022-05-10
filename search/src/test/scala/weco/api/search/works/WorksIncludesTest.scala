package weco.api.search.works

class WorksIncludesTest extends ApiWorksTestBase {
  describe("identifiers includes") {
    it(
      "includes a list of identifiers on a list endpoint if we pass ?include=identifiers"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestWorks(worksIndex, worksEverything: _*)

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
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "id" : "oo9fg6ic",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "cQYSxE7gRG"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "ji3JH82kKu"
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
                 |      "id" : "ou9z1esm",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "calm-record-id",
                 |            "label" : "Calm RecordIdentifier",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "gcgP8jfZZ4"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "ef8BdXe1K5"
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
                 |      "id" : "wchkoofm",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "calm-record-id",
                 |            "label" : "Calm RecordIdentifier",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "mGMGKNlQnl"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "eG0HzUX6yZ"
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=identifiers"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "alternativeTitles" : [
                 |  ],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "closed-stores",
                 |      "label" : "Closed stores",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "id" : "oo9fg6ic",
                 |  "identifiers" : [
                 |    {
                 |      "identifierType" : {
                 |        "id" : "miro-image-number",
                 |        "label" : "Miro image number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "type" : "Identifier",
                 |      "value" : "cQYSxE7gRG"
                 |    },
                 |    {
                 |      "identifierType" : {
                 |        "id" : "sierra-system-number",
                 |        "label" : "Sierra system number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "type" : "Identifier",
                 |      "value" : "ji3JH82kKu"
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
        indexTestWorks(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works/oo9fg6ic?include=items"
        ) {
          Status.OK ->
            s"""
               |{
               |  "alternativeTitles" : [
               |  ],
               |  "availabilities" : [
               |    {
               |      "id" : "closed-stores",
               |      "label" : "Closed stores",
               |      "type" : "Availability"
               |    }
               |  ],
               |  "id" : "oo9fg6ic",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=subjects") {
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
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "id" : "oo9fg6ic",
                 |      "subjects" : [
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "g08I834KKSXk1WG",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "WfqE6xFakoqsVT1",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "GlsNpYpthDMBLQZ",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "ArEtlVdV0j",
                 |          "type" : "Subject"
                 |        },
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "OR7nUmbDY87Uw1L",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "vlTE5cIHQR23GK9",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "tQdPt3acHhNKnNq",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "hG54NzomzM",
                 |          "type" : "Subject"
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
                 |      "id" : "ou9z1esm",
                 |      "subjects" : [
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "31h63sJtsRuBvzw",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "4YQmyoWabAgXxwl",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "S5TtSeuKJt4fspO",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "osW8hKQNGv",
                 |          "type" : "Subject"
                 |        },
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "eEWECb7cJuhQpXN",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "FDoA7rpYPiDfifs",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "BwXAEPbWRaGNi2H",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "h5tlAUKxcP",
                 |          "type" : "Subject"
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
                 |      "id" : "wchkoofm",
                 |      "subjects" : [
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "RSdhZCyeulPkNaP",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "0ClgfwapmD7jxio",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "pplRbppKZMbAm0v",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "5LLMVvWxgX",
                 |          "type" : "Subject"
                 |        },
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "2EMkmWOjlKvfRGK",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "p22pTPNg1Fb7hLZ",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "CWP2ToHCa1SqaQC",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "PSF4EIy1m2",
                 |          "type" : "Subject"
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
      "includes a list of subjects on a single work endpoint if we pass ?include=subjects"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=subjects"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "alternativeTitles" : [
                 |  ],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "closed-stores",
                 |      "label" : "Closed stores",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "id" : "oo9fg6ic",
                 |  "subjects" : [
                 |    {
                 |      "concepts" : [
                 |        {
                 |          "label" : "g08I834KKSXk1WG",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "WfqE6xFakoqsVT1",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "GlsNpYpthDMBLQZ",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "label" : "ArEtlVdV0j",
                 |      "type" : "Subject"
                 |    },
                 |    {
                 |      "concepts" : [
                 |        {
                 |          "label" : "OR7nUmbDY87Uw1L",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "vlTE5cIHQR23GK9",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "tQdPt3acHhNKnNq",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "label" : "hG54NzomzM",
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
          indexTestWorks(worksIndex, worksEverything: _*)

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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "genres" : [
                        {
                          "concepts" : [
                            {
                              "label" : "nFnK1Qv0bPiYMZq",
                              "type" : "Concept"
                            },
                            {
                              "label" : "uffyNPDgAW3Gj5M",
                              "type" : "Concept"
                            },
                            {
                              "label" : "4V6Fk1Uu2KL5QXs",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "4fR1f4tFlV",
                          "type" : "Genre"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "WE1MLX8biK5UW9S",
                              "type" : "Concept"
                            },
                            {
                              "label" : "VIX0fEgCIWtB2H6",
                              "type" : "Concept"
                            },
                            {
                              "label" : "8ssuRzRpFAch6oM",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "RV8G10Obxx",
                          "type" : "Genre"
                        }
                      ],
                      "id" : "oo9fg6ic",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
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
                              "label" : "TK6GyLzSreGLHq3",
                              "type" : "Concept"
                            },
                            {
                              "label" : "k05FqagUauios0I",
                              "type" : "Concept"
                            },
                            {
                              "label" : "zDAQA5geMZT0FFS",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "dKsPMAgtlZ",
                          "type" : "Genre"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "xfQFhVctBFwsNPA",
                              "type" : "Concept"
                            },
                            {
                              "label" : "X3Wg9bz6n2QtjPU",
                              "type" : "Concept"
                            },
                            {
                              "label" : "bfZXARrSCGUFES8",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "0zAAZGNVld",
                          "type" : "Genre"
                        }
                      ],
                      "id" : "ou9z1esm",
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
                              "label" : "2wBgqY0L1ZF1PYZ",
                              "type" : "Concept"
                            },
                            {
                              "label" : "IWOP5xioqrw7Ohi",
                              "type" : "Concept"
                            },
                            {
                              "label" : "AUBgzynPVyy1Bmo",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "D06AWAxFOW",
                          "type" : "Genre"
                        },
                        {
                          "concepts" : [
                            {
                              "label" : "HcLRRBkyn24xZvM",
                              "type" : "Concept"
                            },
                            {
                              "label" : "lY5yFST4hzY1cKo",
                              "type" : "Concept"
                            },
                            {
                              "label" : "WGXrGHlOd9kGPDW",
                              "type" : "Concept"
                            }
                          ],
                          "label" : "iqJbe6G99a",
                          "type" : "Genre"
                        }
                      ],
                      "id" : "wchkoofm",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=genres"
          ) {
            Status.OK ->
              s"""
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
                          "label" : "nFnK1Qv0bPiYMZq",
                          "type" : "Concept"
                        },
                        {
                          "label" : "uffyNPDgAW3Gj5M",
                          "type" : "Concept"
                        },
                        {
                          "label" : "4V6Fk1Uu2KL5QXs",
                          "type" : "Concept"
                        }
                      ],
                      "label" : "4fR1f4tFlV",
                      "type" : "Genre"
                    },
                    {
                      "concepts" : [
                        {
                          "label" : "WE1MLX8biK5UW9S",
                          "type" : "Concept"
                        },
                        {
                          "label" : "VIX0fEgCIWtB2H6",
                          "type" : "Concept"
                        },
                        {
                          "label" : "8ssuRzRpFAch6oM",
                          "type" : "Concept"
                        }
                      ],
                      "label" : "RV8G10Obxx",
                      "type" : "Genre"
                    }
                  ],
                  "id" : "oo9fg6ic",
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
          indexTestWorks(worksIndex, worksEverything: _*)

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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "contributors" : [
                        {
                          "agent" : {
                            "label" : "person-o8xazs",
                            "type" : "Person"
                          },
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        },
                        {
                          "agent" : {
                            "label" : "person-6am8fhYNr",
                            "type" : "Person"
                          },
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        }
                      ],
                      "id" : "oo9fg6ic",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
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
                            "label" : "person-2bLQeClX",
                            "type" : "Person"
                          },
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        },
                        {
                          "agent" : {
                            "label" : "person-epg6BQO",
                            "type" : "Person"
                          },
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        }
                      ],
                      "id" : "ou9z1esm",
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
                            "label" : "person-Lu2Xsa",
                            "type" : "Person"
                          },
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        },
                        {
                          "agent" : {
                            "label" : "person-m0dL6XAj",
                            "type" : "Person"
                          },
                          "roles" : [
                          ],
                          "type" : "Contributor"
                        }
                      ],
                      "id" : "wchkoofm",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=contributors"
          ) {
            Status.OK ->
              s"""
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
                        "label" : "person-o8xazs",
                        "type" : "Person"
                      },
                      "roles" : [
                      ],
                      "type" : "Contributor"
                    },
                    {
                      "agent" : {
                        "label" : "person-6am8fhYNr",
                        "type" : "Person"
                      },
                      "roles" : [
                      ],
                      "type" : "Contributor"
                    }
                  ],
                  "id" : "oo9fg6ic",
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
          indexTestWorks(worksIndex, worksEverything: _*)

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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "oo9fg6ic",
                      "production" : [
                        {
                          "agents" : [
                            {
                              "label" : "6lVoGuWXqO",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "9eqzv",
                              "type" : "Period"
                            }
                          ],
                          "label" : "11C9xxYEdUXSLHjmev7aEhJ0Q",
                          "places" : [
                            {
                              "label" : "NRzQw5sJ89",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        },
                        {
                          "agents" : [
                            {
                              "label" : "Ap8e8SuyMV",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "HQTT2",
                              "type" : "Period"
                            }
                          ],
                          "label" : "roF4g2k9frr0xAeKZIqbV783c",
                          "places" : [
                            {
                              "label" : "9SMhIH2DH1",
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
                      "id" : "ou9z1esm",
                      "production" : [
                        {
                          "agents" : [
                            {
                              "label" : "kEl0aPDS91",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "lOaLK",
                              "type" : "Period"
                            }
                          ],
                          "label" : "O1Lux1bNFDaOKTsDKbwserVib",
                          "places" : [
                            {
                              "label" : "co3T5a2Cte",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        },
                        {
                          "agents" : [
                            {
                              "label" : "tTXh5eC6hh",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "PPPss",
                              "type" : "Period"
                            }
                          ],
                          "label" : "Lw3A3d2SgHOvziH7duUdynba2",
                          "places" : [
                            {
                              "label" : "c8ykXyKH9v",
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
                      "id" : "wchkoofm",
                      "production" : [
                        {
                          "agents" : [
                            {
                              "label" : "e7X4srn7yL",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "EuJLA",
                              "type" : "Period"
                            }
                          ],
                          "label" : "dGFZw7o2IL5cpTq2szbf8JXFR",
                          "places" : [
                            {
                              "label" : "L51HaiqMvP",
                              "type" : "Place"
                            }
                          ],
                          "type" : "ProductionEvent"
                        },
                        {
                          "agents" : [
                            {
                              "label" : "THk4Fal6yy",
                              "type" : "Person"
                            }
                          ],
                          "dates" : [
                            {
                              "label" : "c6PAy",
                              "type" : "Period"
                            }
                          ],
                          "label" : "gnoMEDJADyhgl97NLnSGT3Ooi",
                          "places" : [
                            {
                              "label" : "RibBOUDMpI",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=production"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "production" : [
                    {
                      "agents" : [
                        {
                          "label" : "6lVoGuWXqO",
                          "type" : "Person"
                        }
                      ],
                      "dates" : [
                        {
                          "label" : "9eqzv",
                          "type" : "Period"
                        }
                      ],
                      "label" : "11C9xxYEdUXSLHjmev7aEhJ0Q",
                      "places" : [
                        {
                          "label" : "NRzQw5sJ89",
                          "type" : "Place"
                        }
                      ],
                      "type" : "ProductionEvent"
                    },
                    {
                      "agents" : [
                        {
                          "label" : "Ap8e8SuyMV",
                          "type" : "Person"
                        }
                      ],
                      "dates" : [
                        {
                          "label" : "HQTT2",
                          "type" : "Period"
                        }
                      ],
                      "label" : "roF4g2k9frr0xAeKZIqbV783c",
                      "places" : [
                        {
                          "label" : "9SMhIH2DH1",
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
          indexTestWorks(worksIndex, worksEverything: _*)

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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "oo9fg6ic",
                      "languages" : [
                        {
                          "id" : "Iry",
                          "label" : "y1sdDDR",
                          "type" : "Language"
                        },
                        {
                          "id" : "CyK",
                          "label" : "QFKzgro69P",
                          "type" : "Language"
                        },
                        {
                          "id" : "kIH",
                          "label" : "HMjgKFaY5",
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
                      "id" : "ou9z1esm",
                      "languages" : [
                        {
                          "id" : "CoB",
                          "label" : "HnJZViP",
                          "type" : "Language"
                        },
                        {
                          "id" : "XHQ",
                          "label" : "S7Jxv9xrQ6",
                          "type" : "Language"
                        },
                        {
                          "id" : "Lp6",
                          "label" : "OUAue10NVp",
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
                      "id" : "wchkoofm",
                      "languages" : [
                        {
                          "id" : "dCh",
                          "label" : "shi6cu",
                          "type" : "Language"
                        },
                        {
                          "id" : "x20",
                          "label" : "O7GkIFx8",
                          "type" : "Language"
                        },
                        {
                          "id" : "vXQ",
                          "label" : "PLHOqPv",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=languages"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "languages" : [
                    {
                      "id" : "Iry",
                      "label" : "y1sdDDR",
                      "type" : "Language"
                    },
                    {
                      "id" : "CyK",
                      "label" : "QFKzgro69P",
                      "type" : "Language"
                    },
                    {
                      "id" : "kIH",
                      "label" : "HMjgKFaY5",
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
          indexTestWorks(worksIndex, worksEverything: _*)

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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "oo9fg6ic",
                      "notes" : [
                        {
                          "contents" : [
                            "j9BpOrhd8m"
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
                            "ov3fzhoi",
                            "zybrlE"
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
                            "YEJhvwXauL"
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
                      "id" : "ou9z1esm",
                      "notes" : [
                        {
                          "contents" : [
                            "GvRAnRE2",
                            "kW4LcPBC1S",
                            "uRC7Fyj"
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
                            "nCURCTNo7c"
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
                      "id" : "wchkoofm",
                      "notes" : [
                        {
                          "contents" : [
                            "CsB8YxC3",
                            "Isn6KqXfd"
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
                            "n6NMM9"
                          ],
                          "noteType" : {
                            "id" : "funding-info",
                            "label" : "Funding information",
                            "type" : "NoteType"
                          },
                          "type" : "Note"
                        },
                        {
                          "contents" : [
                            "3Au9wn3b"
                          ],
                          "noteType" : {
                            "id" : "general-note",
                            "label" : "Notes",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=notes"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "notes" : [
                    {
                      "contents" : [
                        "j9BpOrhd8m"
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
                        "ov3fzhoi",
                        "zybrlE"
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
                        "YEJhvwXauL"
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=images") {
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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "id" : "oo9fg6ic",
                      "images" : [
                        {
                          "id" : "01bta4ru",
                          "type" : "Image"
                        },
                        {
                          "id" : "odhob23o",
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
                      "id" : "ou9z1esm",
                      "images" : [
                        {
                          "id" : "i8qttsmr",
                          "type" : "Image"
                        },
                        {
                          "id" : "o1f5oxzt",
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
                      "id" : "wchkoofm",
                      "images" : [
                        {
                          "id" : "jo80nlaw",
                          "type" : "Image"
                        },
                        {
                          "id" : "gbgygwvo",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=images"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "images" : [
                    {
                      "id" : "01bta4ru",
                      "type" : "Image"
                    },
                    {
                      "id" : "odhob23o",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=parts"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "parts" : [
                    {
                      "id" : "b1gnd7b0",
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

    it("includes partOf") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=partOf"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "partOf" : [
                    {
                      "id" : "dza7om88",
                      "partOf" : [
                        {
                          "id" : "nelnrvdy",
                          "title" : "title-grMS5Hy6x3",
                          "totalDescendentParts" : 5,
                          "totalParts" : 1,
                          "type" : "Work"
                        }
                      ],
                      "title" : "title-BnN4RHJX7O",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=precededBy"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "precededBy" : [
                    {
                      "id" : "uxg4ed5m",
                      "title" : "title-a7Ze4ZWUMQ",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=succeededBy"
          ) {
            Status.OK ->
              s"""
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
                  "id" : "oo9fg6ic",
                  "succeededBy" : [
                    {
                      "id" : "mdfbk5kj",
                      "title" : "title-QgPzDrUplG",
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
          indexTestWorks(worksIndex, worksEverything: _*)

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
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "Availability"
                        }
                      ],
                      "holdings" : [
                        {
                          "enumeration" : [
                            "iIHvpnyCp",
                            "gbKXe6",
                            "6XI29tA",
                            "1KbU70mrqZ",
                            "9N8dAz6",
                            "bAErlg"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "license" : {
                              "id" : "cc-by",
                              "label" : "Attribution 4.0 International (CC BY 4.0)",
                              "type" : "License",
                              "url" : "http://creativecommons.org/licenses/by/4.0/"
                            },
                            "locationType" : {
                              "id" : "closed-stores",
                              "label" : "Closed stores",
                              "type" : "LocationType"
                            },
                            "shelfmark" : "Shelfmark: wTJbrfYCV2",
                            "type" : "PhysicalLocation"
                          },
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
                          "note" : "BxSAn4PBcz",
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
                      "id" : "oo9fg6ic",
                      "title" : "A work with all the include-able fields",
                      "type" : "Work"
                    },
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
                            "dJnvNoiufK"
                          ],
                          "location" : {
                            "accessConditions" : [
                            ],
                            "label" : "locationLabel",
                            "locationType" : {
                              "id" : "open-shelves",
                              "label" : "Open shelves",
                              "type" : "LocationType"
                            },
                            "shelfmark" : "Shelfmark: yDmwtJxhA",
                            "type" : "PhysicalLocation"
                          },
                          "note" : "ZZrd6sRqV",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "CEKkIZfsA",
                            "Pwunml",
                            "w7s12i",
                            "Rh1E6F0o"
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
                              "id" : "open-shelves",
                              "label" : "Open shelves",
                              "type" : "LocationType"
                            },
                            "shelfmark" : "Shelfmark: 2jPTlqN",
                            "type" : "PhysicalLocation"
                          },
                          "note" : "EnBrWrk5",
                          "type" : "Holdings"
                        },
                        {
                          "enumeration" : [
                            "o8l1009JS"
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
                            "type" : "PhysicalLocation"
                          },
                          "type" : "Holdings"
                        }
                      ],
                      "id" : "ou9z1esm",
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
                      "id" : "wchkoofm",
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
          indexTestWorks(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=holdings"
          ) {
            Status.OK ->
              s"""
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
                        "iIHvpnyCp",
                        "gbKXe6",
                        "6XI29tA",
                        "1KbU70mrqZ",
                        "9N8dAz6",
                        "bAErlg"
                      ],
                      "location" : {
                        "accessConditions" : [
                        ],
                        "label" : "locationLabel",
                        "license" : {
                          "id" : "cc-by",
                          "label" : "Attribution 4.0 International (CC BY 4.0)",
                          "type" : "License",
                          "url" : "http://creativecommons.org/licenses/by/4.0/"
                        },
                        "locationType" : {
                          "id" : "closed-stores",
                          "label" : "Closed stores",
                          "type" : "LocationType"
                        },
                        "shelfmark" : "Shelfmark: wTJbrfYCV2",
                        "type" : "PhysicalLocation"
                      },
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
                      "note" : "BxSAn4PBcz",
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
                  "id" : "oo9fg6ic",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                }
                """
          }
      }
    }
  }
}
