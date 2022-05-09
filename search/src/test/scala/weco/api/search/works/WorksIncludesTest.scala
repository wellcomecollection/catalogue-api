package weco.api.search.works

import com.sksamuel.elastic4s.Index
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.generators.ImageGenerators
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.catalogue.internal_model.languages.Language
import weco.catalogue.internal_model.work._

class WorksIncludesTest extends ApiWorksTestBase with ImageGenerators {

  val canonicalId1 = CanonicalId("00000000")
  val canonicalId2 = CanonicalId("11111111")

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
          indexTestDocuments(worksIndex, worksEverything: _*)

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
        indexTestDocuments(worksIndex, worksEverything: _*)

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
          indexTestDocuments(worksIndex, worksEverything: _*)

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
          indexTestDocuments(worksIndex, worksEverything: _*)

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
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(routes, path = s"$rootPath/works?include=genres") {
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

    it(
      "includes a list of genres on a single work endpoint if we pass ?include=genres"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=genres"
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
                 |  "genres" : [
                 |    {
                 |      "concepts" : [
                 |        {
                 |          "label" : "nFnK1Qv0bPiYMZq",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "uffyNPDgAW3Gj5M",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "4V6Fk1Uu2KL5QXs",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "label" : "4fR1f4tFlV",
                 |      "type" : "Genre"
                 |    },
                 |    {
                 |      "concepts" : [
                 |        {
                 |          "label" : "WE1MLX8biK5UW9S",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "VIX0fEgCIWtB2H6",
                 |          "type" : "Concept"
                 |        },
                 |        {
                 |          "label" : "8ssuRzRpFAch6oM",
                 |          "type" : "Concept"
                 |        }
                 |      ],
                 |      "label" : "RV8G10Obxx",
                 |      "type" : "Genre"
                 |    }
                 |  ],
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
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
                 |      "contributors" : [
                 |        {
                 |          "agent" : {
                 |            "label" : "person-o8xazs",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        },
                 |        {
                 |          "agent" : {
                 |            "label" : "person-6am8fhYNr",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
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
                 |      "contributors" : [
                 |        {
                 |          "agent" : {
                 |            "label" : "person-2bLQeClX",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        },
                 |        {
                 |          "agent" : {
                 |            "label" : "person-epg6BQO",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
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
                 |      "contributors" : [
                 |        {
                 |          "agent" : {
                 |            "label" : "person-Lu2Xsa",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
                 |        },
                 |        {
                 |          "agent" : {
                 |            "label" : "person-m0dL6XAj",
                 |            "type" : "Person"
                 |          },
                 |          "roles" : [
                 |          ],
                 |          "type" : "Contributor"
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

    it(
      "includes a list of contributors on a single work endpoint if we pass ?include=contributors"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=contributors"
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
                 |  "contributors" : [
                 |    {
                 |      "agent" : {
                 |        "label" : "person-o8xazs",
                 |        "type" : "Person"
                 |      },
                 |      "roles" : [
                 |      ],
                 |      "type" : "Contributor"
                 |    },
                 |    {
                 |      "agent" : {
                 |        "label" : "person-6am8fhYNr",
                 |        "type" : "Person"
                 |      },
                 |      "roles" : [
                 |      ],
                 |      "type" : "Contributor"
                 |    }
                 |  ],
                 |  "id" : "oo9fg6ic",
                 |  "title" : "A work with all the include-able fields",
                 |  "type" : "Work"
                 |}
                 |""".stripMargin
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
                 |      "production" : [
                 |        {
                 |          "agents" : [
                 |            {
                 |              "label" : "6lVoGuWXqO",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "9eqzv",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "label" : "11C9xxYEdUXSLHjmev7aEhJ0Q",
                 |          "places" : [
                 |            {
                 |              "label" : "NRzQw5sJ89",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        },
                 |        {
                 |          "agents" : [
                 |            {
                 |              "label" : "Ap8e8SuyMV",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "HQTT2",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "label" : "roF4g2k9frr0xAeKZIqbV783c",
                 |          "places" : [
                 |            {
                 |              "label" : "9SMhIH2DH1",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
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
                 |      "production" : [
                 |        {
                 |          "agents" : [
                 |            {
                 |              "label" : "kEl0aPDS91",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "lOaLK",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "label" : "O1Lux1bNFDaOKTsDKbwserVib",
                 |          "places" : [
                 |            {
                 |              "label" : "co3T5a2Cte",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        },
                 |        {
                 |          "agents" : [
                 |            {
                 |              "label" : "tTXh5eC6hh",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "PPPss",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "label" : "Lw3A3d2SgHOvziH7duUdynba2",
                 |          "places" : [
                 |            {
                 |              "label" : "c8ykXyKH9v",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
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
                 |      "production" : [
                 |        {
                 |          "agents" : [
                 |            {
                 |              "label" : "e7X4srn7yL",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "EuJLA",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "label" : "dGFZw7o2IL5cpTq2szbf8JXFR",
                 |          "places" : [
                 |            {
                 |              "label" : "L51HaiqMvP",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
                 |        },
                 |        {
                 |          "agents" : [
                 |            {
                 |              "label" : "THk4Fal6yy",
                 |              "type" : "Person"
                 |            }
                 |          ],
                 |          "dates" : [
                 |            {
                 |              "label" : "c6PAy",
                 |              "type" : "Period"
                 |            }
                 |          ],
                 |          "label" : "gnoMEDJADyhgl97NLnSGT3Ooi",
                 |          "places" : [
                 |            {
                 |              "label" : "RibBOUDMpI",
                 |              "type" : "Place"
                 |            }
                 |          ],
                 |          "type" : "ProductionEvent"
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
      "includes a list of production on a single work endpoint if we pass ?include=production"
    ) {
      withWorksApi {
        case (worksIndex, routes) =>
          indexTestDocuments(worksIndex, worksEverything: _*)

          assertJsonResponse(
            routes,
            path = s"$rootPath/works/oo9fg6ic?include=production"
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
                 |  "production" : [
                 |    {
                 |      "agents" : [
                 |        {
                 |          "label" : "6lVoGuWXqO",
                 |          "type" : "Person"
                 |        }
                 |      ],
                 |      "dates" : [
                 |        {
                 |          "label" : "9eqzv",
                 |          "type" : "Period"
                 |        }
                 |      ],
                 |      "label" : "11C9xxYEdUXSLHjmev7aEhJ0Q",
                 |      "places" : [
                 |        {
                 |          "label" : "NRzQw5sJ89",
                 |          "type" : "Place"
                 |        }
                 |      ],
                 |      "type" : "ProductionEvent"
                 |    },
                 |    {
                 |      "agents" : [
                 |        {
                 |          "label" : "Ap8e8SuyMV",
                 |          "type" : "Person"
                 |        }
                 |      ],
                 |      "dates" : [
                 |        {
                 |          "label" : "HQTT2",
                 |          "type" : "Period"
                 |        }
                 |      ],
                 |      "label" : "roF4g2k9frr0xAeKZIqbV783c",
                 |      "places" : [
                 |        {
                 |          "label" : "9SMhIH2DH1",
                 |          "type" : "Place"
                 |        }
                 |      ],
                 |      "type" : "ProductionEvent"
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

  describe("languages includes") {
    it("includes languages on a list endpoint if we pass ?include=languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          val english = Language(label = "English", id = "eng")
          val turkish = Language(label = "Turkish", id = "tur")
          val swedish = Language(label = "Swedish", id = "swe")

          val work1 =
            indexedWork(canonicalId = canonicalId1)
              .languages(List(english, turkish))
          val work2 =
            indexedWork(canonicalId = canonicalId2).languages(List(swedish))

          insertIntoElasticsearch(worksIndex, work1, work2)

          assertJsonResponse(routes, s"$rootPath/works?include=languages") {
            Status.OK -> s"""
              {
                ${resultList(totalResults = 2)},
                "results": [
                 {
                   "type": "Work",
                   "id": "${work1.state.canonicalId}",
                   "title": "${work1.data.title.get}",
                   "alternativeTitles": [],
                   "availabilities": [${availabilities(
              work1.state.availabilities
            )}],
                   "languages": [ ${languages(work1.data.languages)}]
                 },
                 {
                   "type": "Work",
                   "id": "${work2.state.canonicalId}",
                   "title": "${work2.data.title.get}",
                   "alternativeTitles": [],
                   "availabilities": [${availabilities(
              work2.state.availabilities
            )}],
                   "languages": [ ${languages(work2.data.languages)}]
                 }
                ]
              }
            """
          }
      }
    }

    it("includes languages on a work endpoint if we pass ?include=languages") {
      withWorksApi {
        case (worksIndex, routes) =>
          val english = Language(label = "English", id = "eng")
          val turkish = Language(label = "Turkish", id = "tur")
          val swedish = Language(label = "Swedish", id = "swe")

          val work = indexedWork().languages(List(english, turkish, swedish))

          insertIntoElasticsearch(worksIndex, work)

          assertJsonResponse(
            routes,
            s"$rootPath/works/${work.state.canonicalId}?include=languages"
          ) {
            Status.OK -> s"""
              {
                ${singleWorkResult()},
                "id": "${work.state.canonicalId}",
                "title": "${work.data.title.get}",
                "alternativeTitles": [],
                "availabilities": [${availabilities(work.state.availabilities)}],
                "languages": [ ${languages(work.data.languages)}]
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
          val work1 = indexedWork(canonicalId = canonicalId1)
            .notes(
              List(
                Note(contents = "GN1", noteType = NoteType.GeneralNote),
                Note(contents = "FI1", noteType = NoteType.FundingInformation)
              )
            )
          val work2 = indexedWork(canonicalId = canonicalId2)
            .notes(
              List(
                Note(contents = "GN2.1", noteType = NoteType.GeneralNote),
                Note(contents = "GN2.2", noteType = NoteType.GeneralNote)
              )
            )

          insertIntoElasticsearch(worksIndex, work1, work2)
          assertJsonResponse(routes, s"$rootPath/works?include=notes") {
            Status.OK -> s"""
              {
                ${resultList(totalResults = 2)},
                "results": [
                   {
                     "type": "Work",
                     "id": "${work1.state.canonicalId}",
                     "title": "${work1.data.title.get}",
                     "alternativeTitles": [],
                     "availabilities": [${availabilities(
              work1.state.availabilities
            )}],
                     "notes": [
                       {
                         "noteType": {
                           "id": "general-note",
                           "label": "Notes",
                           "type": "NoteType"
                         },
                         "contents": ["GN1"],
                         "type": "Note"
                       },
                       {
                         "noteType": {
                           "id": "funding-info",
                           "label": "Funding information",
                           "type": "NoteType"
                         },
                         "contents": ["FI1"],
                         "type": "Note"
                       }
                     ]
                   },
                   {
                     "type": "Work",
                     "id": "${work2.state.canonicalId}",
                     "title": "${work2.data.title.get}",
                     "alternativeTitles": [],
                     "availabilities": [${availabilities(
              work2.state.availabilities
            )}],
                     "notes": [
                       {
                         "noteType": {
                           "id": "general-note",
                           "label": "Notes",
                           "type": "NoteType"
                         },
                         "contents": ["GN2.1", "GN2.2"],
                         "type": "Note"
                       }
                     ]
                  }
                ]
              }
            """
          }
      }
    }

    it("includes notes on the single work endpoint if we pass ?include=notes") {
      withWorksApi {
        case (worksIndex, routes) =>
          val work =
            indexedWork().notes(
              List(
                Note(contents = "A", noteType = NoteType.GeneralNote),
                Note(contents = "B", noteType = NoteType.GeneralNote)
              )
            )
          insertIntoElasticsearch(worksIndex, work)
          assertJsonResponse(
            routes,
            s"$rootPath/works/${work.state.canonicalId}?include=notes"
          ) {
            Status.OK -> s"""
              {
                ${singleWorkResult()},
                "id": "${work.state.canonicalId}",
                "title": "${work.data.title.get}",
                "alternativeTitles": [],
                "availabilities": [${availabilities(work.state.availabilities)}],
                "notes": [
                   {
                     "noteType": {
                       "id": "general-note",
                       "label": "Notes",
                       "type": "NoteType"
                     },
                     "contents": ["A", "B"],
                     "type": "Note"
                   }
                ]
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
          val works = List(
            indexedWork()
              .imageData(
                (1 to 3)
                  .map(_ => createImageData.toIdentified)
                  .toList
              ),
            indexedWork()
              .imageData(
                (1 to 3)
                  .map(_ => createImageData.toIdentified)
                  .toList
              )
          ).sortBy { _.state.canonicalId }

          insertIntoElasticsearch(worksIndex, works: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=images") {
            Status.OK -> s"""
              {
                ${resultList(totalResults = works.size)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${works.head.state.canonicalId}",
                    "title": "${works.head.data.title.get}",
                    "alternativeTitles": [],
                    "availabilities": [${availabilities(
              works.head.state.availabilities
            )}],
                    "images": [${workImageIncludes(works.head.data.imageData)}]
                  },
                  {
                    "type": "Work",
                    "id": "${works(1).state.canonicalId}",
                    "title": "${works(1).data.title.get}",
                    "alternativeTitles": [],
                    "availabilities": [${availabilities(
              works(1).state.availabilities
            )}],
                    "images": [${workImageIncludes(works(1).data.imageData)}]
                  }
                ]
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
          val images =
            (1 to 3).map(_ => createImageData.toIdentified).toList
          val work = indexedWork().imageData(images)

          insertIntoElasticsearch(worksIndex, work)

          assertJsonResponse(
            routes,
            s"$rootPath/works/${work.state.canonicalId}?include=images"
          ) {
            Status.OK -> s"""
              {
                ${singleWorkResult()},
                "id": "${work.state.canonicalId}",
                "title": "${work.data.title.get}",
                "alternativeTitles": [],
                "availabilities": [${availabilities(work.state.availabilities)}],
                "images": [${workImageIncludes(images)}]
              }
            """
          }
      }
    }
  }

  describe("relation includes") {
    def seriesWork(
      seriesTitle: String,
      title: String
    ): Work.Visible[WorkState.Indexed] =
      indexedWork(
        sourceIdentifier =
          createSourceIdentifierWith(value = s"$seriesTitle-$title"),
        relations = Relations(ancestors = List(SeriesRelation(seriesTitle)))
      ).title(title)
        .workType(WorkType.Standard)
    def work(
      path: String,
      workType: WorkType
    ): Work.Visible[WorkState.Indexed] =
      indexedWork(sourceIdentifier = createSourceIdentifierWith(value = path))
        .collectionPath(CollectionPath(path = path))
        .title(path)
        .workType(workType)

    val work0 = work("0", WorkType.Collection)
    val workA = work("0/a", WorkType.Section)
    val workB = work("0/a/b", WorkType.Standard)
    val workD = work("0/a/d", WorkType.Standard)
    val workE = work("0/a/c/e", WorkType.Standard)

    val workC =
      indexedWork(
        sourceIdentifier = createSourceIdentifierWith(value = "0/a/c"),
        relations = Relations(
          ancestors = List(
            Relation(work0, 0, 1, 5),
            Relation(workA, 1, 3, 4)
          ),
          children = List(Relation(workE, 3, 0, 0)),
          siblingsPreceding = List(Relation(workB, 2, 0, 0)),
          siblingsSucceeding = List(Relation(workD, 2, 0, 0))
        )
      ).collectionPath(CollectionPath(path = "0/a/c"))
        .title("0/a/c")
        .workType(WorkType.Series)

    def storeWorks(index: Index) =
      insertIntoElasticsearch(index, work0, workA, workB, workC, workD, workE)

    it("includes parts") {
      withWorksApi {
        case (worksIndex, routes) =>
          storeWorks(worksIndex)
          assertJsonResponse(
            routes,
            s"$rootPath/works/${workC.state.canonicalId}?include=parts"
          ) {
            Status.OK -> s"""
            {
              ${singleWorkResult("Series")},
              "id": "${workC.state.canonicalId}",
              "title": "0/a/c",
              "alternativeTitles": [],
              "availabilities": [${availabilities(workC.state.availabilities)}],
              "parts": [{
                "id": "${workE.state.canonicalId}",
                "title": "0/a/c/e",
                "totalParts": 0,
                "totalDescendentParts": 0,
                "type": "Work"
              }]
            }
          """
          }
      }
    }

    it("includes partOf") {
      withWorksApi {
        case (worksIndex, routes) =>
          storeWorks(worksIndex)
          assertJsonResponse(
            routes,
            s"$rootPath/works/${workC.state.canonicalId}?include=partOf"
          ) {
            Status.OK -> s"""
            {
              ${singleWorkResult("Series")},
              "id": "${workC.state.canonicalId}",
              "title": "0/a/c",
              "alternativeTitles": [],
              "availabilities": [${availabilities(workC.state.availabilities)}],
              "partOf": [
                {
                  "id": "${workA.state.canonicalId}",
                  "title": "0/a",
                  "totalParts": 3,
                  "totalDescendentParts": 4,
                  "type": "Section",
                  "partOf": [{
                    "id": "${work0.state.canonicalId}",
                    "title": "0",
                    "totalParts": 1,
                    "totalDescendentParts": 5,
                    "type": "Collection"
                  }
                ]
              }]
            }
          """
          }
      }
    }
    it("includes partOf representing membership of a Series") {
      val workInSeries = seriesWork(
        seriesTitle = "I am a series",
        title = "I am part of a series"
      )
      withWorksApi {
        case (worksIndex, routes) =>
          insertIntoElasticsearch(
            index = worksIndex,
            workInSeries
          )
          assertJsonResponse(
            routes,
            s"$rootPath/works/${workInSeries.state.canonicalId}?include=partOf"
          ) {
            Status.OK -> s"""
            {
              ${singleWorkResult("Work")},
              "id": "${workInSeries.state.canonicalId}",
              "title": "I am part of a series",
              "alternativeTitles": [],
              "availabilities": [${availabilities(
              workInSeries.state.availabilities
            )}],
              "partOf": [
                {
                  "title":  "I am a series",
                  "totalParts": 0,
                  "totalDescendentParts": 0,
                  "type": "Series"
                }
              ]
            }
          """
          }
      }
    }
    it("includes precededBy") {
      withWorksApi {
        case (worksIndex, routes) =>
          storeWorks(worksIndex)
          assertJsonResponse(
            routes,
            s"$rootPath/works/${workC.state.canonicalId}?include=precededBy"
          ) {
            Status.OK -> s"""
            {
              ${singleWorkResult("Series")},
              "id": "${workC.state.canonicalId}",
              "title": "0/a/c",
              "alternativeTitles": [],
              "availabilities": [${availabilities(workC.state.availabilities)}],
              "precededBy": [{
                "id": "${workB.state.canonicalId}",
                "title": "0/a/b",
                "totalParts": 0,
                "totalDescendentParts": 0,
                "type": "Work"
              }]
            }
          """
          }
      }
    }

    it("includes succeededBy") {
      withWorksApi {
        case (worksIndex, routes) =>
          storeWorks(worksIndex)
          assertJsonResponse(
            routes,
            s"$rootPath/works/${workC.state.canonicalId}?include=succeededBy"
          ) {
            Status.OK -> s"""
            {
              ${singleWorkResult("Series")},
              "id": "${workC.state.canonicalId}",
              "title": "0/a/c",
              "alternativeTitles": [],
              "availabilities": [${availabilities(workC.state.availabilities)}],
              "succeededBy": [{
                "id": "${workD.state.canonicalId}",
                "title": "0/a/d",
                "totalParts": 0,
                "totalDescendentParts": 0,
                "type": "Work"
              }]
            }
          """
          }
      }
    }
  }

  describe("holdings includes") {
    def createHoldings(count: Int): List[Holdings] =
      (1 to count).map { _ =>
        Holdings(
          note = chooseFrom(None, Some(randomAlphanumeric())),
          enumeration =
            collectionOf(min = 0, max = 10) { randomAlphanumeric() }.toList,
          location = chooseFrom(None, Some(createPhysicalLocation))
        )
      }.toList

    it("on the list endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          val works = List(
            indexedWork().holdings(createHoldings(3)),
            indexedWork().holdings(createHoldings(4))
          ).sortBy { _.state.canonicalId }

          insertIntoElasticsearch(worksIndex, works: _*)

          assertJsonResponse(routes, s"$rootPath/works?include=holdings") {
            Status.OK -> s"""
              {
                ${resultList(totalResults = works.size)},
                "results": [
                  {
                    "type": "Work",
                    "id": "${works.head.state.canonicalId}",
                    "title": "${works.head.data.title.get}",
                    "alternativeTitles": [],
                    "availabilities": [${availabilities(
              works.head.state.availabilities
            )}],
                    "holdings": [${listOfHoldings(works.head.data.holdings)}]
                  },
                  {
                    "type": "Work",
                    "id": "${works(1).state.canonicalId}",
                    "title": "${works(1).data.title.get}",
                    "alternativeTitles": [],
                    "availabilities": [${availabilities(
              works(1).state.availabilities
            )}],
                    "holdings": [${listOfHoldings(works(1).data.holdings)}]
                  }
                ]
              }
            """
          }
      }
    }

    it("on a single work endpoint") {
      withWorksApi {
        case (worksIndex, routes) =>
          val work = indexedWork().holdings(createHoldings(3))

          insertIntoElasticsearch(worksIndex, work)

          assertJsonResponse(
            routes,
            s"$rootPath/works/${work.state.canonicalId}?include=holdings"
          ) {
            Status.OK -> s"""
              {
                ${singleWorkResult()},
                "id": "${work.state.canonicalId}",
                "title": "${work.data.title.get}",
                "alternativeTitles": [],
                "availabilities": [${availabilities(work.state.availabilities)}],
                "holdings": [${listOfHoldings(work.data.holdings)}]
              }
            """
          }
      }
    }
  }
}
