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
                 |
                 |{
                 |  "pageSize" : 10,
                 |  "results" : [
                 |    {
                 |      "alternativeTitles" : [
                 |      ],
                 |      "availabilities" : [
                 |      ],
                 |      "id" : "dhzcyeul",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "LMVvWxgXRS"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "calm-record-id",
                 |            "label" : "Calm RecordIdentifier",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "mD7jxioppl"
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
                 |      "id" : "kspmagtl",
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "bWRaGNi2Hd"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "type" : "Identifier",
                 |          "value" : "Hq3k05Fqag"
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
                 |    {
                 |      "id" : "closed-stores",
                 |      "label" : "Closed stores",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "id" : "tmdfbk5k",
                 |  "identifiers" : [
                 |    {
                 |      "identifierType" : {
                 |        "id" : "miro-image-number",
                 |        "label" : "Miro image number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "type" : "Identifier",
                 |      "value" : "Aic5qOhRoS"
                 |    },
                 |    {
                 |      "identifierType" : {
                 |        "id" : "sierra-system-number",
                 |        "label" : "Sierra system number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "type" : "Identifier",
                 |      "value" : "UfcQYSxE7g"
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
               |    {
               |      "id" : "closed-stores",
               |      "label" : "Closed stores",
               |      "type" : "Availability"
               |    }
               |  ],
               |  "id" : "tmdfbk5k",
               |  "items" : [
               |    {
               |      "id" : "a7xxlndb",
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
               |          "url" : "https://iiif.wellcomecollection.org/image/3jE.jpg/info.json"
               |        }
               |      ],
               |      "type" : "Item"
               |    },
               |    {
               |      "id" : "ejk7jwcd",
               |      "locations" : [
               |        {
               |          "accessConditions" : [
               |          ],
               |          "credit" : "Credit line: MnIN8jLSj0",
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
               |          "url" : "https://iiif.wellcomecollection.org/image/ds1.jpg/info.json"
               |        }
               |      ],
               |      "type" : "Item"
               |    },
               |    {
               |      "locations" : [
               |        {
               |          "accessConditions" : [
               |          ],
               |          "credit" : "Credit line: ZN9ToblN",
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
               |          "url" : "https://iiif.wellcomecollection.org/image/OSl.jpg/info.json"
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
                    ],
                    "id" : "dhzcyeul",
                    "subjects" : [
                      {
                        "concepts" : [
                          {
                            "label" : "0vPSF4EIy1m22EM",
                            "type" : "Concept"
                          },
                          {
                            "label" : "kmWOjlKvfRGKp22",
                            "type" : "Concept"
                          },
                          {
                            "label" : "pTPNg1Fb7hLZCWP",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "RbppKZMbAm",
                        "type" : "Subject"
                      },
                      {
                        "concepts" : [
                          {
                            "label" : "QCD06AWAxFOW2wB",
                            "type" : "Concept"
                          },
                          {
                            "label" : "gqY0L1ZF1PYZIWO",
                            "type" : "Concept"
                          },
                          {
                            "label" : "P5xioqrw7OhiAUB",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "2ToHCa1Sqa",
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
                    "id" : "kspmagtl",
                    "subjects" : [
                      {
                        "concepts" : [
                          {
                            "label" : "AQA5geMZT0FFS0z",
                            "type" : "Concept"
                          },
                          {
                            "label" : "AAZGNVldxfQFhVc",
                            "type" : "Concept"
                          },
                          {
                            "label" : "tBFwsNPAX3Wg9bz",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "Uauios0IzD",
                        "type" : "Subject"
                      },
                      {
                        "concepts" : [
                          {
                            "label" : "ZXARrSCGUFES8H2",
                            "type" : "Concept"
                          },
                          {
                            "label" : "bLQeClXtepg6BQO",
                            "type" : "Concept"
                          },
                          {
                            "label" : "O1Lux1bNFDaOKTs",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "6n2QtjPUbf",
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
                 |
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
                    ],
                    "genres" : [
                      {
                        "concepts" : [
                          {
                            "id" : "moiqjbe6",
                            "label" : "G99aHcLRRBkyn24",
                            "type" : "Genre"
                          },
                          {
                            "id" : "4hzy1cko",
                            "label" : "WGXrGHlOd9kGPDW",
                            "type" : "Concept"
                          },
                          {
                            "id" : "l6xajdgf",
                            "label" : "Zw7o2IL5cpTq2sz",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "gzynPVyy1B",
                        "type" : "Genre"
                      },
                      {
                        "concepts" : [
                          {
                            "id" : "srn7yleu",
                            "label" : "JLAgnoMEDJADyhg",
                            "type" : "Genre"
                          },
                          {
                            "id" : "oiribbou",
                            "label" : "DMpITHk4Fal6yyc",
                            "type" : "Concept"
                          },
                          {
                            "id" : "6cux209o",
                            "label" : "7GkIFx8vXQyPLHO",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "aiqMvPe7X4",
                        "type" : "Genre"
                      }
                    ],
                    "id" : "dhzcyeul",
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
                            "id" : "co3t5a2c",
                            "label" : "tekEl0aPDS91lOa",
                            "type" : "Genre"
                          },
                          {
                            "id" : "hovzih7d",
                            "label" : "uUdynba2c8ykXyK",
                            "type" : "Concept"
                          },
                          {
                            "id" : "hhpppssc",
                            "label" : "oBEHnJZViPXHQHS",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "DKbwserVib",
                        "type" : "Genre"
                      },
                      {
                        "concepts" : [
                          {
                            "id" : "vp7gvran",
                            "label" : "RE2canCURCTNo7c",
                            "type" : "Genre"
                          },
                          {
                            "id" : "sheurc7f",
                            "label" : "yjsJNyP6az68MBx",
                            "type" : "Concept"
                          },
                          {
                            "id" : "heqgsnmx",
                            "label" : "vD5wGC18lSi8qtT",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "6uOUAue10N",
                        "type" : "Genre"
                      }
                    ],
                    "id" : "kspmagtl",
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
                            "id" : "ihqr23gk",
                            "label" : "9tQdPt3acHhNKnN",
                            "type" : "Genre"
                          },
                          {
                            "id" : "nfnk1qv0",
                            "label" : "bPiYMZquffyNPDg",
                            "type" : "Concept"
                          },
                          {
                            "id" : "k1uu2kl5",
                            "label" : "QXsRV8G10ObxxWE",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "Uw1LvlTE5c",
                        "type" : "Genre"
                      },
                      {
                        "concepts" : [
                          {
                            "id" : "iwtb2h68",
                            "label" : "ssuRzRpFAch6oMg",
                            "type" : "Genre"
                          },
                          {
                            "id" : "fhynr11c",
                            "label" : "9xxYEdUXSLHjmev",
                            "type" : "Concept"
                          },
                          {
                            "id" : "w5sj896l",
                            "label" : "VoGuWXqO9eqzvro",
                            "type" : "Concept"
                          }
                        ],
                        "label" : "9SVIX0fEgC",
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
                        "id" : "ihqr23gk",
                        "label" : "9tQdPt3acHhNKnN",
                        "type" : "Genre"
                      },
                      {
                        "id" : "nfnk1qv0",
                        "label" : "bPiYMZquffyNPDg",
                        "type" : "Concept"
                      },
                      {
                        "id" : "k1uu2kl5",
                        "label" : "QXsRV8G10ObxxWE",
                        "type" : "Concept"
                      }
                    ],
                    "label" : "Uw1LvlTE5c",
                    "type" : "Genre"
                  },
                  {
                    "concepts" : [
                      {
                        "id" : "iwtb2h68",
                        "label" : "ssuRzRpFAch6oMg",
                        "type" : "Genre"
                      },
                      {
                        "id" : "fhynr11c",
                        "label" : "9xxYEdUXSLHjmev",
                        "type" : "Concept"
                      },
                      {
                        "id" : "w5sj896l",
                        "label" : "VoGuWXqO9eqzvro",
                        "type" : "Concept"
                      }
                    ],
                    "label" : "9SVIX0fEgC",
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
                    ],
                    "contributors" : [
                      {
                        "agent" : {
                          "label" : "person-RLIsn6KqXf",
                          "type" : "Person"
                        },
                        "primary" : true,
                        "roles" : [
                        ],
                        "type" : "Contributor"
                      },
                      {
                        "agent" : {
                          "label" : "person-MBn6NMM",
                          "type" : "Person"
                        },
                        "primary" : true,
                        "roles" : [
                        ],
                        "type" : "Contributor"
                      }
                    ],
                    "id" : "dhzcyeul",
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
                          "label" : "person-h9jhXW",
                          "type" : "Person"
                        },
                        "primary" : true,
                        "roles" : [
                        ],
                        "type" : "Contributor"
                      },
                      {
                        "agent" : {
                          "label" : "person-QdWhUu",
                          "type" : "Person"
                        },
                        "primary" : true,
                        "roles" : [
                        ],
                        "type" : "Contributor"
                      }
                    ],
                    "id" : "kspmagtl",
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
                          "label" : "person-eKZIqbV783",
                          "type" : "Person"
                        },
                        "primary" : true,
                        "roles" : [
                        ],
                        "type" : "Contributor"
                      },
                      {
                        "agent" : {
                          "label" : "person-9SMhIH",
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
                ],
                "contributors" : [
                  {
                    "agent" : {
                      "label" : "person-eKZIqbV783",
                      "type" : "Person"
                    },
                    "primary" : true,
                    "roles" : [
                    ],
                    "type" : "Contributor"
                  },
                  {
                    "agent" : {
                      "label" : "person-9SMhIH",
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
                    ],
                    "id" : "dhzcyeul",
                    "production" : [
                      {
                        "agents" : [
                          {
                            "label" : "Y8dO1DZyoO",
                            "type" : "Person"
                          }
                        ],
                        "dates" : [
                          {
                            "label" : "9NU3A",
                            "type" : "Period"
                          }
                        ],
                        "label" : "u9wn3b70uQnR7JVa4h6zo4kiw",
                        "places" : [
                          {
                            "label" : "lz0oyrQVeB",
                            "type" : "Place"
                          }
                        ],
                        "type" : "ProductionEvent"
                      },
                      {
                        "agents" : [
                          {
                            "label" : "JB4CuJFro7",
                            "type" : "Person"
                          }
                        ],
                        "dates" : [
                          {
                            "label" : "SCKOJ",
                            "type" : "Period"
                          }
                        ],
                        "label" : "o80nLAWSy9B3e17RLNSLFMReQ",
                        "places" : [
                          {
                            "label" : "prYosMnVGM",
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
                    "id" : "kspmagtl",
                    "production" : [
                      {
                        "agents" : [
                          {
                            "label" : "XdJnvNoiuf",
                            "type" : "Person"
                          }
                        ],
                        "dates" : [
                          {
                            "label" : "cLjO4",
                            "type" : "Period"
                          }
                        ],
                        "label" : "4DIByCcllibcScRo1F5OxZThZ",
                        "places" : [
                          {
                            "label" : "Zrd6sRqVgE",
                            "type" : "Place"
                          }
                        ],
                        "type" : "ProductionEvent"
                      },
                      {
                        "agents" : [
                          {
                            "label" : "nmldw7s12i",
                            "type" : "Person"
                          }
                        ],
                        "dates" : [
                          {
                            "label" : "KWNuy",
                            "type" : "Period"
                          }
                        ],
                        "label" : "DmwtJxhAIELEnBrWrk5FvZCEK",
                        "places" : [
                          {
                            "label" : "kIZfsAkPwu",
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
                    "id" : "tmdfbk5k",
                    "production" : [
                      {
                        "agents" : [
                          {
                            "label" : "o69PkIHAHM",
                            "type" : "Person"
                          }
                        ],
                        "dates" : [
                          {
                            "label" : "2DH1A",
                            "type" : "Period"
                          }
                        ],
                        "label" : "p8e8SuyMVHQTT2Iryhy1sdDDR",
                        "places" : [
                          {
                            "label" : "CyKLQFKzgr",
                            "type" : "Place"
                          }
                        ],
                        "type" : "ProductionEvent"
                      },
                      {
                        "agents" : [
                          {
                            "label" : "5GzybrlEoE",
                            "type" : "Person"
                          }
                        ],
                        "dates" : [
                          {
                            "label" : "jgKFa",
                            "type" : "Period"
                          }
                        ],
                        "label" : "Y5fj9BpOrhd8mwmov3fzhoiiC",
                        "places" : [
                          {
                            "label" : "YEJhvwXauL",
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
                ],
                "id" : "tmdfbk5k",
                "production" : [
                  {
                    "agents" : [
                      {
                        "label" : "o69PkIHAHM",
                        "type" : "Person"
                      }
                    ],
                    "dates" : [
                      {
                        "label" : "2DH1A",
                        "type" : "Period"
                      }
                    ],
                    "label" : "p8e8SuyMVHQTT2Iryhy1sdDDR",
                    "places" : [
                      {
                        "label" : "CyKLQFKzgr",
                        "type" : "Place"
                      }
                    ],
                    "type" : "ProductionEvent"
                  },
                  {
                    "agents" : [
                      {
                        "label" : "5GzybrlEoE",
                        "type" : "Person"
                      }
                    ],
                    "dates" : [
                      {
                        "label" : "jgKFa",
                        "type" : "Period"
                      }
                    ],
                    "label" : "Y5fj9BpOrhd8mwmov3fzhoiiC",
                    "places" : [
                      {
                        "label" : "YEJhvwXauL",
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
                    ],
                    "id" : "dhzcyeul",
                    "languages" : [
                      {
                        "id" : "LAJ",
                        "label" : "BGyGwVoRP",
                        "type" : "Language"
                      },
                      {
                        "id" : "6Il",
                        "label" : "5rHofWvU",
                        "type" : "Language"
                      },
                      {
                        "id" : "NX2",
                        "label" : "BmQh5YW",
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
                    "id" : "kspmagtl",
                    "languages" : [
                      {
                        "id" : "BRh",
                        "label" : "E6F0oBW",
                        "type" : "Language"
                      },
                      {
                        "id" : "52j",
                        "label" : "TlqNXqK",
                        "type" : "Language"
                      },
                      {
                        "id" : "K4Q",
                        "label" : "hldbPB44o8",
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
                    "id" : "tmdfbk5k",
                    "languages" : [
                      {
                        "id" : "Dbd",
                        "label" : "zJH1bfh",
                        "type" : "Language"
                      },
                      {
                        "id" : "Kx4",
                        "label" : "fSAq54gq",
                        "type" : "Language"
                      },
                      {
                        "id" : "QEh",
                        "label" : "QxoZn47",
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
                ],
                "id" : "tmdfbk5k",
                "languages" : [
                  {
                    "id" : "Dbd",
                    "label" : "zJH1bfh",
                    "type" : "Language"
                  },
                  {
                    "id" : "Kx4",
                    "label" : "fSAq54gq",
                    "type" : "Language"
                  },
                  {
                    "id" : "QEh",
                    "label" : "QxoZn47",
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
                    ],
                    "id" : "dhzcyeul",
                    "notes" : [
                      {
                        "contents" : [
                          "BD4V4eC"
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
                          "PxyMQW"
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
                          "lsrFBR8QS",
                          "soceYh"
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
                    "id" : "kspmagtl",
                    "notes" : [
                      {
                        "contents" : [
                          "KatsdMX"
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
                          "1009JS",
                          "Gxz9bfcN5B"
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
                          "bf5YSWx"
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
                        "id" : "closed-stores",
                        "label" : "Closed stores",
                        "type" : "Availability"
                      }
                    ],
                    "id" : "tmdfbk5k",
                    "notes" : [
                      {
                        "contents" : [
                          "AX01BTa4R"
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
                          "pneMkt"
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
                          "w1YfRQ9an",
                          "5wdlW2gcpt"
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
                ],
                "id" : "tmdfbk5k",
                "notes" : [
                  {
                    "contents" : [
                      "AX01BTa4R"
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
                      "pneMkt"
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
                      "w1YfRQ9an",
                      "5wdlW2gcpt"
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

  it("includes formerFrequency if we pass ?include=formerFrequency") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?include=formerFrequency"
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
                  ],
                  "formerFrequency" : [
                    "Published in 2001",
                    "Published in 2002"
                  ],
                  "id" : "dhzcyeul",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "formerFrequency" : [
                    "Published in 2001",
                    "Published in 2002"
                  ],
                  "id" : "kspmagtl",
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
                  "formerFrequency" : [
                    "Published in 2001",
                    "Published in 2002"
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

  it("includes designation if we pass ?include=designation") {
    withWorksApi {
      case (worksIndex, routes) =>
        indexTestDocuments(worksIndex, worksEverything: _*)

        assertJsonResponse(
          routes,
          path = s"$rootPath/works?include=designation"
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
                  ],
                  "designation" : [
                    "Designation #1",
                    "Designation #2",
                    "Designation #3"
                  ],
                  "id" : "dhzcyeul",
                  "title" : "A work with all the include-able fields",
                  "type" : "Work"
                },
                {
                  "alternativeTitles" : [
                  ],
                  "availabilities" : [
                  ],
                  "designation" : [
                    "Designation #1",
                    "Designation #2",
                    "Designation #3"
                  ],
                  "id" : "kspmagtl",
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
                  "designation" : [
                    "Designation #1",
                    "Designation #2",
                    "Designation #3"
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
                    ],
                    "id" : "dhzcyeul",
                    "images" : [
                      {
                        "id" : "9shcs9eq",
                        "type" : "Image"
                      },
                      {
                        "id" : "ogxkz2rm",
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
                    "id" : "kspmagtl",
                    "images" : [
                      {
                        "id" : "6fuh8tqe",
                        "type" : "Image"
                      },
                      {
                        "id" : "ej3tqgsk",
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
                    "id" : "tmdfbk5k",
                    "images" : [
                      {
                        "id" : "ihvpnycp",
                        "type" : "Image"
                      },
                      {
                        "id" : "rlgjqnwt",
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
                ],
                "id" : "tmdfbk5k",
                "images" : [
                  {
                    "id" : "ihvpnycp",
                    "type" : "Image"
                  },
                  {
                    "id" : "rlgjqnwt",
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
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
                    ],
                    "holdings" : [
                      {
                        "enumeration" : [
                          "1BxLTcV6Wb",
                          "WqoNZK",
                          "xU3ERjrH",
                          "Bu2SeMYa",
                          "rwLrUlQ4SY",
                          "7p3ABwYulV",
                          "4E1A9I2C3",
                          "XDCODR4",
                          "FZmdTEa6",
                          "UlrveN"
                        ],
                        "type" : "Holdings"
                      },
                      {
                        "enumeration" : [
                          "G0xMucB",
                          "DkJGLGn",
                          "8mfcQf",
                          "kDCPAZd",
                          "cCnNOd",
                          "zVPOkLMXQ",
                          "nzM0sw6VCd",
                          "7FN4wzU",
                          "mSjqoeX"
                        ],
                        "note" : "idk7LBY",
                        "type" : "Holdings"
                      },
                      {
                        "enumeration" : [
                          "37zKdSgd",
                          "TKdzCFovRO",
                          "4WLHpAvI",
                          "NXwdJ7UobR",
                          "k4GLtR",
                          "vuraPvkrZ"
                        ],
                        "type" : "Holdings"
                      }
                    ],
                    "id" : "dhzcyeul",
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
                          "ieDDRLFu",
                          "yZOrDf50Hq",
                          "LqcxXMND",
                          "FWEKKm2rQ2",
                          "mZq6OHvSNm",
                          "yYUvJ6",
                          "jSbmBb",
                          "EN5U1Lj",
                          "3YYXiwayO"
                        ],
                        "note" : "MVhaLIuxnE",
                        "type" : "Holdings"
                      },
                      {
                        "enumeration" : [
                          "ESu6j0cajz",
                          "IwEsWu",
                          "kQm30wp"
                        ],
                        "note" : "qavjz8rXQ",
                        "type" : "Holdings"
                      },
                      {
                        "enumeration" : [
                          "8y3h9IEu",
                          "L1QHuLHN",
                          "3UU0BuJ",
                          "EQ4PswL",
                          "PCNURg",
                          "6avc628S",
                          "u9fWqc",
                          "mcETEt4h"
                        ],
                        "type" : "Holdings"
                      }
                    ],
                    "id" : "kspmagtl",
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
                          "zRFFmVMv",
                          "ez7JFZ7F",
                          "x3e01Z",
                          "12L0U9K"
                        ],
                        "note" : "rYBxSAn4",
                        "type" : "Holdings"
                      },
                      {
                        "enumeration" : [
                          "C1HRZomzs",
                          "WpcRaAA6",
                          "O9h5XJq",
                          "Ym9tlQp1Q",
                          "JoGECfaB"
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
                          "type" : "PhysicalLocation"
                        },
                        "type" : "Holdings"
                      },
                      {
                        "enumeration" : [
                          "UHlycAdT2L",
                          "laGIDVwG",
                          "CtLHFeu",
                          "f5tz94vKZ",
                          "UW7NcA3an",
                          "I6DhKyS",
                          "bKjx1oRi6",
                          "fEDi0YO"
                        ],
                        "note" : "z3mr2Gbjg3",
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
                  {
                    "id" : "closed-stores",
                    "label" : "Closed stores",
                    "type" : "Availability"
                  }
                ],
                "holdings" : [
                  {
                    "enumeration" : [
                      "zRFFmVMv",
                      "ez7JFZ7F",
                      "x3e01Z",
                      "12L0U9K"
                    ],
                    "note" : "rYBxSAn4",
                    "type" : "Holdings"
                  },
                  {
                    "enumeration" : [
                      "C1HRZomzs",
                      "WpcRaAA6",
                      "O9h5XJq",
                      "Ym9tlQp1Q",
                      "JoGECfaB"
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
                      "type" : "PhysicalLocation"
                    },
                    "type" : "Holdings"
                  },
                  {
                    "enumeration" : [
                      "UHlycAdT2L",
                      "laGIDVwG",
                      "CtLHFeu",
                      "f5tz94vKZ",
                      "UW7NcA3an",
                      "I6DhKyS",
                      "bKjx1oRi6",
                      "fEDi0YO"
                    ],
                    "note" : "z3mr2Gbjg3",
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
