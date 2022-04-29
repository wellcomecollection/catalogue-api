package weco.api.search.works

class WorksIncludesTest extends ApiWorksTestBase {
  describe("omits all the optional fields if we don't pass any includes") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "4ed5mjia",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
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

  describe("includes the identifiers with ?include=identifiers") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=identifiers") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "4ed5mjia",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "sierra-system-number",
                 |            "label" : "Sierra system number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "value" : "IT6YYrhUXG",
                 |          "type" : "Identifier"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "value" : "c5qOhRoStm",
                 |          "type" : "Identifier"
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
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "value" : "ePka3wv3Pt",
                 |          "type" : "Identifier"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "calm-record-id",
                 |            "label" : "Calm RecordIdentifier",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "value" : "mmaiXrIFIY",
                 |          "type" : "Identifier"
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
                 |      "identifiers" : [
                 |        {
                 |          "identifierType" : {
                 |            "id" : "miro-image-number",
                 |            "label" : "Miro image number",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "value" : "BwQR6HvzRu",
                 |          "type" : "Identifier"
                 |        },
                 |        {
                 |          "identifierType" : {
                 |            "id" : "calm-record-id",
                 |            "label" : "Calm RecordIdentifier",
                 |            "type" : "IdentifierType"
                 |          },
                 |          "value" : "97X7fYVwu7",
                 |          "type" : "Identifier"
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=identifiers"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "identifiers" : [
                 |    {
                 |      "identifierType" : {
                 |        "id" : "sierra-system-number",
                 |        "label" : "Sierra system number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "value" : "IT6YYrhUXG",
                 |      "type" : "Identifier"
                 |    },
                 |    {
                 |      "identifierType" : {
                 |        "id" : "miro-image-number",
                 |        "label" : "Miro image number",
                 |        "type" : "IdentifierType"
                 |      },
                 |      "value" : "c5qOhRoStm",
                 |      "type" : "Identifier"
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

  describe("includes the items with ?include=items") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=items") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "4ed5mjia",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "items" : [
                 |        {
                 |          "id" : "bdq5aoz3",
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/UHl.jpg/info.json",
                 |              "credit" : "Credit line: cAdT2Lil",
                 |              "linkText" : "Link text: IDVwGmC",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "id" : "lhfeubf5",
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/NcA.jpg/info.json",
                 |              "credit" : "Credit line: aniI6DhK",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/fED.jpg/info.json",
                 |              "credit" : "Credit line: 0YOdTl",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
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
                 |      "items" : [
                 |        {
                 |          "id" : "j7p3abwy",
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/C3f.jpg/info.json",
                 |              "linkText" : "Link text: TEa66Ul",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "id" : "venlyadk",
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/idk.jpg/info.json",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/78m.jpg/info.json",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
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
                 |      "items" : [
                 |        {
                 |          "id" : "swxm2kat",
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/fcN.jpg/info.json",
                 |              "credit" : "Credit line: BFmTAn",
                 |              "linkText" : "Link text: GP9LZNM",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "id" : "wcw6z9rm",
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/fuH.jpg/info.json",
                 |              "credit" : "Credit line: tqe0GaHLA",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
                 |        },
                 |        {
                 |          "locations" : [
                 |            {
                 |              "locationType" : {
                 |                "id" : "iiif-presentation",
                 |                "label" : "IIIF Presentation API",
                 |                "type" : "LocationType"
                 |              },
                 |              "url" : "https://iiif.wellcomecollection.org/image/3hD.jpg/info.json",
                 |              "credit" : "Credit line: qF34gvXCY",
                 |              "license" : {
                 |                "id" : "cc-by",
                 |                "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |                "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |                "type" : "License"
                 |              },
                 |              "accessConditions" : [
                 |              ],
                 |              "type" : "DigitalLocation"
                 |            }
                 |          ],
                 |          "type" : "Item"
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia?include=items") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
                 |  "title" : "A work with all the include-able fields",
                 |  "alternativeTitles" : [],
                 |  "items" : [
                 |    {
                 |      "id" : "bdq5aoz3",
                 |      "locations" : [
                 |        {
                 |          "locationType" : {
                 |            "id" : "iiif-presentation",
                 |            "label" : "IIIF Presentation API",
                 |            "type" : "LocationType"
                 |          },
                 |          "url" : "https://iiif.wellcomecollection.org/image/UHl.jpg/info.json",
                 |          "credit" : "Credit line: cAdT2Lil",
                 |          "linkText" : "Link text: IDVwGmC",
                 |          "license" : {
                 |            "id" : "cc-by",
                 |            "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |            "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |            "type" : "License"
                 |          },
                 |          "accessConditions" : [
                 |          ],
                 |          "type" : "DigitalLocation"
                 |        }
                 |      ],
                 |      "type" : "Item"
                 |    },
                 |    {
                 |      "id" : "lhfeubf5",
                 |      "locations" : [
                 |        {
                 |          "locationType" : {
                 |            "id" : "iiif-presentation",
                 |            "label" : "IIIF Presentation API",
                 |            "type" : "LocationType"
                 |          },
                 |          "url" : "https://iiif.wellcomecollection.org/image/NcA.jpg/info.json",
                 |          "credit" : "Credit line: aniI6DhK",
                 |          "license" : {
                 |            "id" : "cc-by",
                 |            "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |            "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |            "type" : "License"
                 |          },
                 |          "accessConditions" : [
                 |          ],
                 |          "type" : "DigitalLocation"
                 |        }
                 |      ],
                 |      "type" : "Item"
                 |    },
                 |    {
                 |      "locations" : [
                 |        {
                 |          "locationType" : {
                 |            "id" : "iiif-presentation",
                 |            "label" : "IIIF Presentation API",
                 |            "type" : "LocationType"
                 |          },
                 |          "url" : "https://iiif.wellcomecollection.org/image/fED.jpg/info.json",
                 |          "credit" : "Credit line: 0YOdTl",
                 |          "license" : {
                 |            "id" : "cc-by",
                 |            "label" : "Attribution 4.0 International (CC BY 4.0)",
                 |            "url" : "http://creativecommons.org/licenses/by/4.0/",
                 |            "type" : "License"
                 |          },
                 |          "accessConditions" : [
                 |          ],
                 |          "type" : "DigitalLocation"
                 |        }
                 |      ],
                 |      "type" : "Item"
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

  describe("includes the subjects with ?include=subjects") {
    it("on a list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=subjects") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "4ed5mjia",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "subjects" : [
                 |        {
                 |          "label" : "dFbK5kJngQ",
                 |          "concepts" : [
                 |            {
                 |              "label" : "gPzDrUplGUfcQYS",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "xE7gRGOo9Fg6icg",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "oKOwWLrIbnrzZji",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Subject"
                 |        },
                 |        {
                 |          "label" : "3JH82kKuAr",
                 |          "concepts" : [
                 |            {
                 |              "label" : "EtlVdV0jg08I834",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "KKSXk1WGWfqE6xF",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "akoqsVT1GlsNpYp",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Subject"
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
                 |      "subjects" : [
                 |        {
                 |          "label" : "3dXkoMXh3J",
                 |          "concepts" : [
                 |            {
                 |              "label" : "UHm6omslvZsK5XC",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "jfcicmGMGKNlQnl",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "wchKoofmzHYUpi3",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Subject"
                 |        },
                 |        {
                 |          "label" : "VaGKxAEeG0",
                 |          "concepts" : [
                 |            {
                 |              "label" : "HzUX6yZ5LLMVvWx",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "gXRSdhZCyeulPkN",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "aP0ClgfwapmD7jx",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Subject"
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
                 |      "subjects" : [
                 |        {
                 |          "label" : "Y7Y7K6CHoS",
                 |          "concepts" : [
                 |            {
                 |              "label" : "1VmVU9NNHeoQ3ea",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "6fUp2HbJRuXO6LS",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "h7zZgfvJGys0LrE",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Subject"
                 |        },
                 |        {
                 |          "label" : "eYBuPDIs1a",
                 |          "concepts" : [
                 |            {
                 |              "label" : "jgcgP8jfZZ4OU9Z",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "1esMjA2VuoPaFhq",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "VjVef8BdXe1K5os",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Subject"
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=subjects"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=genres") {
            Status.OK ->
              s"""
                 |{
                 |  "type": "ResultList",
                 |  "pageSize": 10,
                 |  "totalPages": 1,
                 |  "totalResults": 3,
                 |  "results": [
                 |    {
                 |      "id" : "4ed5mjia",
                 |      "title" : "A work with all the include-able fields",
                 |      "alternativeTitles" : [],
                 |      "genres" : [
                 |        {
                 |          "label" : "thDMBLQZhG",
                 |          "concepts" : [
                 |            {
                 |              "label" : "54NzomzMOR7nUmb",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "DY87Uw1LvlTE5cI",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "HQR23GK9tQdPt3a",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Genre"
                 |        },
                 |        {
                 |          "label" : "cHhNKnNq4f",
                 |          "concepts" : [
                 |            {
                 |              "label" : "R1f4tFlVnFnK1Qv",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "0bPiYMZquffyNPD",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "gAW3Gj5M4V6Fk1U",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Genre"
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
                 |      "genres" : [
                 |        {
                 |          "label" : "iopplRbppK",
                 |          "concepts" : [
                 |            {
                 |              "label" : "ZMbAm0vPSF4EIy1",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "m22EMkmWOjlKvfR",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "GKp22pTPNg1Fb7h",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Genre"
                 |        },
                 |        {
                 |          "label" : "LZCWP2ToHC",
                 |          "concepts" : [
                 |            {
                 |              "label" : "a1SqaQCD06AWAxF",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "OW2wBgqY0L1ZF1P",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "YZIWOP5xioqrw7O",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Genre"
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
                 |      "genres" : [
                 |        {
                 |          "label" : "W8hKQNGv31",
                 |          "concepts" : [
                 |            {
                 |              "label" : "h63sJtsRuBvzw4Y",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "QmyoWabAgXxwlS5",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "TtSeuKJt4fspOh5",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Genre"
                 |        },
                 |        {
                 |          "label" : "tlAUKxcPeE",
                 |          "concepts" : [
                 |            {
                 |              "label" : "WECb7cJuhQpXNFD",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "oA7rpYPiDfifsBw",
                 |              "type" : "Concept"
                 |            },
                 |            {
                 |              "label" : "XAEPbWRaGNi2HdK",
                 |              "type" : "Concept"
                 |            }
                 |          ],
                 |          "type" : "Genre"
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia?include=genres") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

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
                 |      "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=contributors"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

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
                 |      "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=production"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

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
                 |      "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=languages"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

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
                 |      "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia?include=notes") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

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
                 |      "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia?include=images") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

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
                 |      "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=holdings"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia?include=parts") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(routes, s"$rootPath/works/4ed5mjia?include=partOf") {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=precededBy"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
          indexExampleDocuments(worksIndex, everythingWorks: _*)

          assertJsonResponse(
            routes,
            s"$rootPath/works/4ed5mjia?include=succeededBy"
          ) {
            Status.OK ->
              s"""
                 |{
                 |  "id" : "4ed5mjia",
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
