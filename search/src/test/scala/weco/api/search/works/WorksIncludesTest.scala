package weco.api.search.works

class WorksIncludesTest extends ApiWorksTestBase {
  describe("omits all the optional fields if we don't pass any includes") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works") {
            Status.OK -> newWorksListResponse(worksEverything)
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "closed-stores",
                 |      "label" : "Closed stores",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the identifiers with ?include=identifiers") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=identifiers") {
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

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=identifiers") {
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

  describe("includes the items with ?include=items") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=items") {
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
                 |      "items" : [
                 |        {
                 |          "id" : "ca3anii6",
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/oRi.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "id" : "tuqkgha7",
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "credit" : "Credit line: ZX3jETt",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "linkText" : "Link text: 934EUQbvCh",
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/xlG.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/xtr.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
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
                 |      "items" : [
                 |        {
                 |          "id" : "atsdmxht",
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "credit" : "Credit line: YzGP9LZNM",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "linkText" : "Link text: Cw6Z9r",
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/mTA.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "id" : "1ractorm",
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/GaH.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/qF3.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
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
                 |      "items" : [
                 |        {
                 |          "id" : "kdcpazds",
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/kLM.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "id" : "ujmsjqoe",
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/mrK.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "locations" : [
                 |            {
                 |              "accessConditions" : [
                 |              ],
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "type" : "License",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/"
                 |              },
                 |              "linkText" : "Link text: WLHpAvI",
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "type" : "DigitalLocation",
                 |              "url" : "https://iiif.wellcomecollection.org/image/gdc.jpg/info.json"
                 |            }
                 |          ],
                 |          "type" : "Item"
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

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=items") {
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
  }

  describe("includes the subjects with ?include=subjects") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=subjects") {
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

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=subjects") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "subjects" : [
                 |    {
                 |      "label" : "dFbK5kJngQ",
                 |      "concepts" : [
                 |        {
                 |          "label" : "gPzDrUplGUfcQYS",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "xE7gRGOo9Fg6icg",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "oKOwWLrIbnrzZji",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "type" : "Subject"
                 |    },
                 |    {
                 |      "label" : "3JH82kKuAr",
                 |      "concepts" : [
                 |        {
                 |          "label" : "EtlVdV0jg08I834",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "KKSXk1WGWfqE6xF",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "akoqsVT1GlsNpYp",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "type" : "Subject"
                 |    }
                 |  ],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the genres with ?include=genres") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=genres") {
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
                 |      "genres" : [
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "nFnK1Qv0bPiYMZq",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "uffyNPDgAW3Gj5M",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "4V6Fk1Uu2KL5QXs",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "4fR1f4tFlV",
                 |          "type" : "Genre"
                 |        },
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "WE1MLX8biK5UW9S",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "VIX0fEgCIWtB2H6",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "8ssuRzRpFAch6oM",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "RV8G10Obxx",
                 |          "type" : "Genre"
                 |        }
                 |      ],
                 |      "id" : "oo9fg6ic",
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
                 |      "genres" : [
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "TK6GyLzSreGLHq3",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "k05FqagUauios0I",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "zDAQA5geMZT0FFS",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "dKsPMAgtlZ",
                 |          "type" : "Genre"
                 |        },
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "xfQFhVctBFwsNPA",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "X3Wg9bz6n2QtjPU",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "bfZXARrSCGUFES8",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "0zAAZGNVld",
                 |          "type" : "Genre"
                 |        }
                 |      ],
                 |      "id" : "ou9z1esm",
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
                 |      "genres" : [
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "2wBgqY0L1ZF1PYZ",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "IWOP5xioqrw7Ohi",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "AUBgzynPVyy1Bmo",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "D06AWAxFOW",
                 |          "type" : "Genre"
                 |        },
                 |        {
                 |          "concepts" : [
                 |            {
                 |              "label" : "HcLRRBkyn24xZvM",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "lY5yFST4hzY1cKo",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "WGXrGHlOd9kGPDW",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "label" : "iqJbe6G99a",
                 |          "type" : "Genre"
                 |        }
                 |      ],
                 |      "id" : "wchkoofm",
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

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=genres") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "genres" : [
                 |    {
                 |      "label" : "thDMBLQZhG",
                 |      "concepts" : [
                 |        {
                 |          "label" : "54NzomzMOR7nUmb",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "DY87Uw1LvlTE5cI",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "HQR23GK9tQdPt3a",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "type" : "Genre"
                 |    },
                 |    {
                 |      "label" : "cHhNKnNq4f",
                 |      "concepts" : [
                 |        {
                 |          "label" : "R1f4tFlVnFnK1Qv",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "0bPiYMZquffyNPD",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "gAW3Gj5M4V6Fk1U",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "type" : "Genre"
                 |    }
                 |  ],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the contributors with ?include=contributors") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=contributors") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "oo9fg6ic",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "contributors" : [
                 |        {
                 |          "agent" : {
                 |            "label" : "person-2KL5QXs",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        },
                 |        {
                 |          "agent" : {
                 |            "label" : "person-V8G10ObxxW",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        }
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "kdf7if8n",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "contributors" : [
                 |        {
                 |          "agent" : {
                 |            "label" : "person-iAUBgzynPV",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        },
                 |        {
                 |          "agent" : {
                 |            "label" : "person-y1BmoiqJ",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        }
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "vcaaacmd",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "contributors" : [
                 |        {
                 |          "agent" : {
                 |            "label" : "person-PMAgtlZT",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        },
                 |        {
                 |          "agent" : {
                 |            "label" : "person-6GyLzSr",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        }
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=contributors") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "contributors" : [
                 |    {
                 |      "agent" : {
                 |        "label" : "person-2KL5QXs",
                 |        "type" : "Person"
                 |      },
                 |      "roles" : [
                 |      ],
                 |      "type" : "Contributor"
                 |    },
                 |    {
                 |      "agent" : {
                 |        "label" : "person-V8G10ObxxW",
                 |        "type" : "Person"
                 |      },
                 |      "roles" : [
                 |      ],
                 |      "type" : "Contributor"
                 |    }
                 |  ],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the production events with ?include=production") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=production") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "oo9fg6ic",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "production" : [
                 |        {
                 |          "label" : "E1MLX8biK5UW9SVIX0fEgCIWt",
                 |          "places" : [
                 |            {
                 |              "label" : "B2H68ssuRz",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "agents" : [
                 |            {
                 |              "label" : "RpFAch6oMg",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "o8xaz",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        },
                 |        {
                 |          "label" : "sI6am8fhYNr11C9xxYEdUXSLH",
                 |          "places" : [
                 |            {
                 |              "label" : "jmev7aEhJ0",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "agents" : [
                 |            {
                 |              "label" : "QNRzQw5sJ8",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "96lVo",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "kdf7if8n",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "production" : [
                 |        {
                 |          "label" : "be6G99aHcLRRBkyn24xZvMlY5",
                 |          "places" : [
                 |            {
                 |              "label" : "yFST4hzY1c",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "agents" : [
                 |            {
                 |              "label" : "KoWGXrGHlO",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "d9kGP",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        },
                 |        {
                 |          "label" : "DWLLu2Xsatm0dL6XAjdGFZw7o",
                 |          "places" : [
                 |            {
                 |              "label" : "2IL5cpTq2s",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "agents" : [
                 |            {
                 |              "label" : "zbf8JXFRL5",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "1Haiq",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "vcaaacmd",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "production" : [
                 |        {
                 |          "label" : "eGLHq3k05FqagUauios0IzDAQ",
                 |          "places" : [
                 |            {
                 |              "label" : "A5geMZT0FF",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "agents" : [
                 |            {
                 |              "label" : "S0zAAZGNVl",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "dxfQF",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        },
                 |        {
                 |          "label" : "hVctBFwsNPAX3Wg9bz6n2QtjP",
                 |          "places" : [
                 |            {
                 |              "label" : "UbfZXARrSC",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "agents" : [
                 |            {
                 |              "label" : "GUFES8H2bL",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "QeClX",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=production") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "production" : [
                 |    {
                 |      "label" : "E1MLX8biK5UW9SVIX0fEgCIWt",
                 |      "places" : [
                 |        {
                 |          "label" : "B2H68ssuRz",
                 |          "type" : "Place"
                 |        }
                 |      ],
                 |      "agents" : [
                 |        {
                 |          "label" : "RpFAch6oMg",
                 |          "type" : "Person"
                 |        }
                 |      ],
                 |      "dates" : [
                 |        {
                 |          "label" : "o8xaz",
                 |          "type" : "Period"
                 |        }
                 |      ],
                 |      "type" : "ProductionEvent"
                 |    },
                 |    {
                 |      "label" : "sI6am8fhYNr11C9xxYEdUXSLH",
                 |      "places" : [
                 |        {
                 |          "label" : "jmev7aEhJ0",
                 |          "type" : "Place"
                 |        }
                 |      ],
                 |      "agents" : [
                 |        {
                 |          "label" : "QNRzQw5sJ8",
                 |          "type" : "Person"
                 |        }
                 |      ],
                 |      "dates" : [
                 |        {
                 |          "label" : "96lVo",
                 |          "type" : "Period"
                 |        }
                 |      ],
                 |      "type" : "ProductionEvent"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the languages with ?include=languages") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=languages") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "oo9fg6ic",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "languages" : [
                 |        {
                 |          "id" : "GuW",
                 |          "label" : "qO9eqz",
                 |          "type" : "Language"
                 |        },
                 |        {
                 |          "id" : "vro",
                 |          "label" : "4g2k9fr",
                 |          "type" : "Language"
                 |        },
                 |        {
                 |          "id" : "r0x",
                 |          "label" : "eKZIqbV783",
                 |          "type" : "Language"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "kdf7if8n",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "languages" : [
                 |        {
                 |          "id" : "MvP",
                 |          "label" : "7X4srn7",
                 |          "type" : "Language"
                 |        },
                 |        {
                 |          "id" : "yLE",
                 |          "label" : "JLAgnoMED",
                 |          "type" : "Language"
                 |        },
                 |        {
                 |          "id" : "JAD",
                 |          "label" : "hgl97NL",
                 |          "type" : "Language"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "vcaaacmd",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "languages" : [
                 |        {
                 |          "id" : "tep",
                 |          "label" : "6BQOO1Lu",
                 |          "type" : "Language"
                 |        },
                 |        {
                 |          "id" : "x1b",
                 |          "label" : "FDaOKTsDK",
                 |          "type" : "Language"
                 |        },
                 |        {
                 |          "id" : "bws",
                 |          "label" : "rVibco3T",
                 |          "type" : "Language"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=languages") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "languages" : [
                 |    {
                 |      "id" : "GuW",
                 |      "label" : "qO9eqz",
                 |      "type" : "Language"
                 |    },
                 |    {
                 |      "id" : "vro",
                 |      "label" : "4g2k9fr",
                 |      "type" : "Language"
                 |    },
                 |    {
                 |      "id" : "r0x",
                 |      "label" : "eKZIqbV783",
                 |      "type" : "Language"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the notes with ?include=notes") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=notes") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "oo9fg6ic",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "notes" : [
                 |        {
                 |          "contents" : [
                 |            "9SMhIH"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "general-note",
                 |            "label" : "Notes",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        },
                 |        {
                 |          "contents" : [
                 |            "H1Ap8e",
                 |            "ryhy1sdD"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "funding-info",
                 |            "label" : "Funding information",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        },
                 |        {
                 |          "contents" : [
                 |            "uyMVHQTT"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "location-of-duplicates",
                 |            "label" : "Location of duplicates",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "kdf7if8n",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "notes" : [
                 |        {
                 |          "contents" : [
                 |            "SGT3OoiRi"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "general-note",
                 |            "label" : "Notes",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        },
                 |        {
                 |          "contents" : [
                 |            "OUDMpI",
                 |            "k4Fal6yy",
                 |            "PAydChzsh"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "funding-info",
                 |            "label" : "Funding information",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "vcaaacmd",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "notes" : [
                 |        {
                 |          "contents" : [
                 |            "a2CtekE",
                 |            "KLw3A3d2S"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "location-of-duplicates",
                 |            "label" : "Location of duplicates",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        },
                 |        {
                 |          "contents" : [
                 |            "aPDS91lO",
                 |            "OvziH7du"
                 |          ],
                 |          "noteType" : {
                 |            "id" : "funding-info",
                 |            "label" : "Funding information",
                 |            "type" : "NoteType"
                 |          },
                 |          "type" : "Note"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=notes") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "notes" : [
                 |    {
                 |      "contents" : [
                 |        "9SMhIH"
                 |      ],
                 |      "noteType" : {
                 |        "id" : "general-note",
                 |        "label" : "Notes",
                 |        "type" : "NoteType"
                 |      },
                 |      "type" : "Note"
                 |    },
                 |    {
                 |      "contents" : [
                 |        "H1Ap8e",
                 |        "ryhy1sdD"
                 |      ],
                 |      "noteType" : {
                 |        "id" : "funding-info",
                 |        "label" : "Funding information",
                 |        "type" : "NoteType"
                 |      },
                 |      "type" : "Note"
                 |    },
                 |    {
                 |      "contents" : [
                 |        "uyMVHQTT"
                 |      ],
                 |      "noteType" : {
                 |        "id" : "location-of-duplicates",
                 |        "label" : "Location of duplicates",
                 |        "type" : "NoteType"
                 |      },
                 |      "type" : "Note"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the image data with ?include=images") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=images") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "oo9fg6ic",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "images" : [
                 |        {
                 |          "id" : "mov3fzho",
                 |          "type" : "Image"
                 |        },
                 |        {
                 |          "id" : "fsaq54gq",
                 |          "type" : "Image"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "kdf7if8n",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "images" : [
                 |        {
                 |          "id" : "sn6kqxfd",
                 |          "type" : "Image"
                 |        },
                 |        {
                 |          "id" : "veby8do1",
                 |          "type" : "Image"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "vcaaacmd",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "images" : [
                 |        {
                 |          "id" : "xhqhs7jx",
                 |          "type" : "Image"
                 |        },
                 |        {
                 |          "id" : "clokw4lc",
                 |          "type" : "Image"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=images") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "images" : [
                 |    {
                 |      "id" : "mov3fzho",
                 |      "type" : "Image"
                 |    },
                 |    {
                 |      "id" : "fsaq54gq",
                 |      "type" : "Image"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes the holdings with ?include=holdings") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=holdings") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "oo9fg6ic",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "holdings" : [
                 |        {
                 |          "enumeration" : [
                 |            "01BTa4R",
                 |            "lw1YfRQ9a",
                 |            "VApneM",
                 |            "t8o5wd",
                 |            "W2gcptwRT"
                 |          ],
                 |          "location" : {
                 |            "locationType" : {
                 |              "id" : "open-shelves",
                 |              "label" : "Open shelves",
                 |              "type" : "LocationType"
                 |            },
                 |            "label" : "locationLabel",
                 |            "license" : {
                 |              "id" : "ogl",
                 |              "label" : "Open Government Licence",
                 |              "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
                 |              "type" : "License"
                 |            },
                 |            "shelfmark" : "Shelfmark: agBbzkToD",
                 |            "accessConditions" : [
                 |            ],
                 |            "type" : "PhysicalLocation"
                 |          },
                 |          "type" : "Holdings"
                 |        },
                 |        {
                 |          "enumeration" : [
                 |            "FPJNgpliIH",
                 |            "pnyCpsgbK",
                 |            "e6B6XI29t",
                 |            "C1KbU70mrq",
                 |            "m9N8dAz",
                 |            "1bAErlgjqn"
                 |          ],
                 |          "type" : "Holdings"
                 |        },
                 |        {
                 |          "note" : "xSAn4PBcz",
                 |          "enumeration" : [
                 |            "mVMvzez7JF",
                 |            "7FYx3e0",
                 |            "ZI12L0U9K8",
                 |            "yAtfsUkFIG",
                 |            "BSj9Gf",
                 |            "Mi4wC1H",
                 |            "ZomzsnWpc",
                 |            "aAA6nO9h",
                 |            "XJqcYm9tl",
                 |            "p1QVJo"
                 |          ],
                 |          "type" : "Holdings"
                 |        }
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "kdf7if8n",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "holdings" : [
                 |        {
                 |          "note" : "ZyoOSCK",
                 |          "enumeration" : [
                 |            "80nLAW",
                 |            "y9B3e17",
                 |            "LNSLFMReQp",
                 |            "YosMnVGM",
                 |            "B4CuJFr",
                 |            "7LAJgBGyGw"
                 |          ],
                 |          "location" : {
                 |            "locationType" : {
                 |              "id" : "closed-stores",
                 |              "label" : "Closed stores",
                 |              "type" : "LocationType"
                 |            },
                 |            "label" : "locationLabel",
                 |            "license" : {
                 |              "id" : "pdm",
                 |              "label" : "Public Domain Mark",
                 |              "url" : "https://creativecommons.org/share-your-work/public-domain/pdm/",
                 |              "type" : "License"
                 |            },
                 |            "accessConditions" : [
                 |            ],
                 |            "type" : "PhysicalLocation"
                 |          },
                 |          "type" : "Holdings"
                 |        },
                 |        {
                 |          "enumeration" : [
                 |            "QPxyMQW",
                 |            "dlsrFBR8Q",
                 |            "LIBD4V4eC",
                 |            "5soceYhE",
                 |            "Xco4CAZl",
                 |            "NX6YfF9ung",
                 |            "68x12CqOH",
                 |            "tD46Q9sHcs",
                 |            "EqOHuG"
                 |          ],
                 |          "type" : "Holdings"
                 |        },
                 |        {
                 |          "enumeration" : [
                 |            "RLuAZL",
                 |            "a0DogXkz2",
                 |            "M4S3tuy",
                 |            "RUMGx0",
                 |            "x6rK1BxL",
                 |            "cV6Wb9Wq",
                 |            "NZKZxU",
                 |            "ERjrHYBu2S"
                 |          ],
                 |          "location" : {
                 |            "locationType" : {
                 |              "id" : "open-shelves",
                 |              "label" : "Open shelves",
                 |              "type" : "LocationType"
                 |            },
                 |            "label" : "locationLabel",
                 |            "license" : {
                 |              "id" : "ogl",
                 |              "label" : "Open Government Licence",
                 |              "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
                 |              "type" : "License"
                 |            },
                 |            "shelfmark" : "Shelfmark: a0rwLrUlQ4",
                 |            "accessConditions" : [
                 |            ],
                 |            "type" : "PhysicalLocation"
                 |          },
                 |          "type" : "Holdings"
                 |        }
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        },
                 |        {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    },
                 |    {
                 |      "id" : "vcaaacmd",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "holdings" : [
                 |        {
                 |          "enumeration" : [
                 |            "jsJNyP6a",
                 |            "68MBxtQg",
                 |            "ny3fst",
                 |            "HEqgsnmXvD"
                 |          ],
                 |          "type" : "Holdings"
                 |        },
                 |        {
                 |          "enumeration" : [
                 |            "9jhXW1Q",
                 |            "WhUucLjO44",
                 |            "IByCcllibc",
                 |            "cRo1F5OxZT",
                 |            "ZZrd6sRqV",
                 |            "EXdJnvNoi"
                 |          ],
                 |          "type" : "Holdings"
                 |        },
                 |        {
                 |          "enumeration" : [
                 |            "ZCEKkIZfs",
                 |            "kPwunmldw7",
                 |            "12iBRh1E6",
                 |            "0oBW52jPT",
                 |            "qNXqKK4Q",
                 |            "hldbPB44o8"
                 |          ],
                 |          "location" : {
                 |            "locationType" : {
                 |              "id" : "closed-stores",
                 |              "label" : "Closed stores",
                 |              "type" : "LocationType"
                 |            },
                 |            "label" : "locationLabel",
                 |            "license" : {
                 |              "id" : "ogl",
                 |              "label" : "Open Government Licence",
                 |              "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
                 |              "type" : "License"
                 |            },
                 |            "shelfmark" : "Shelfmark: 09JSeIbf",
                 |            "accessConditions" : [
                 |            ],
                 |            "type" : "PhysicalLocation"
                 |          },
                 |          "type" : "Holdings"
                 |        }
                 |      ],
                 |      "availabilities" : [
                 |        {
                 |          "id" : "closed-stores",
                 |          "label" : "Closed stores",
                 |          "type" : "Availability"
                 |        }
                 |      ],
                 |      "type" : "Work"
                 |    }
                 |  ]
                 |}
                 |""".stripMargin
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=holdings") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "holdings" : [
                 |    {
                 |      "enumeration" : [
                 |        "01BTa4R",
                 |        "lw1YfRQ9a",
                 |        "VApneM",
                 |        "t8o5wd",
                 |        "W2gcptwRT"
                 |      ],
                 |      "location" : {
                 |        "locationType" : {
                 |          "id" : "open-shelves",
                 |          "label" : "Open shelves",
                 |          "type" : "LocationType"
                 |        },
                 |        "label" : "locationLabel",
                 |        "license" : {
                 |          "id" : "ogl",
                 |          "label" : "Open Government Licence",
                 |          "url" : "http://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
                 |          "type" : "License"
                 |        },
                 |        "shelfmark" : "Shelfmark: agBbzkToD",
                 |        "accessConditions" : [
                 |        ],
                 |        "type" : "PhysicalLocation"
                 |      },
                 |      "type" : "Holdings"
                 |    },
                 |    {
                 |      "enumeration" : [
                 |        "FPJNgpliIH",
                 |        "pnyCpsgbK",
                 |        "e6B6XI29t",
                 |        "C1KbU70mrq",
                 |        "m9N8dAz",
                 |        "1bAErlgjqn"
                 |      ],
                 |      "type" : "Holdings"
                 |    },
                 |    {
                 |      "note" : "xSAn4PBcz",
                 |      "enumeration" : [
                 |        "mVMvzez7JF",
                 |        "7FYx3e0",
                 |        "ZI12L0U9K8",
                 |        "yAtfsUkFIG",
                 |        "BSj9Gf",
                 |        "Mi4wC1H",
                 |        "ZomzsnWpc",
                 |        "aAA6nO9h",
                 |        "XJqcYm9tl",
                 |        "p1QVJo"
                 |      ],
                 |      "type" : "Holdings"
                 |    }
                 |  ],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }

  describe("includes relation-based information") {
    it("?include=parts") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=parts") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "parts" : [
                 |    {
                 |      "id" : "jgrms5hy",
                 |      "title" : "title-6x38NrC4O6",
                 |      "totalParts" : 0,
                 |      "totalDescendentParts" : 0,
                 |      "type" : "Work"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }

    it("?include=partOf") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=partOf") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "partOf" : [
                 |    {
                 |      "id" : "bxb1izsl",
                 |      "title" : "title-IT5y03R9bs",
                 |      "partOf" : [
                 |        {
                 |          "id" : "i5c8ouwi",
                 |          "title" : "title-qUUo6BR4gN",
                 |          "totalParts" : 1,
                 |          "totalDescendentParts" : 5,
                 |          "type" : "Work"
                 |        }
                 |      ],
                 |      "totalParts" : 3,
                 |      "totalDescendentParts" : 4,
                 |      "type" : "Work"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }

    it("?include=precededBy") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=precededBy") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "precededBy" : [
                 |    {
                 |      "id" : "8xmbnn4r",
                 |      "title" : "title-HJX7OPKBgh",
                 |      "totalParts" : 0,
                 |      "totalDescendentParts" : 0,
                 |      "type" : "Work"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }

    it("?include=succeededBy") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, s"$rootPath/works/oo9fg6ic?include=succeededBy") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "availabilities" : [
                 |    {
                 |      "id" : "open-shelves",
                 |      "label" : "Open shelves",
                 |      "type" : "Availability"
                 |    }
                 |  ],
                 |  "succeededBy" : [
                 |    {
                 |      "id" : "7b0m5tne",
                 |      "title" : "title-tMtnM6n9bK",
                 |      "totalParts" : 0,
                 |      "totalDescendentParts" : 0,
                 |      "type" : "Work"
                 |    }
                 |  ],
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
          }
      }
    }
  }
}
