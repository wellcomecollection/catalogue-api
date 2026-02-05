package weco.api.search.services

import weco.api.search.elasticsearch.templateSearch.TemplateSearchRequest
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._

/**
  * Builder for template-based search requests.
  *
  * = Scoring =
  * Searches created by this trait may be ranked by query, or by KNN,
  * but not both.
  * If both a query term and a KNN are requested, then the
  * query term becomes a filter for the KNN part, rather than
  * populating a full query for itself.
  * This is because the score for a document that matches both
  * a query and a knn is the sum of both, giving no sensible provision to
  * score a document that matches both over one that very highly
  * matches one but not the other.
  *
  * = Sorting =
  * Results can be sorted by date (up or down) or score, and if neither
  * value is given, results will be sorted by id.
  * If both date and score sorting are requested, then results are sorted
  *  by date and then score.
  */
trait TemplateSearchBuilder extends Encoders {
  // Template for the "query" part of the request.
  // This is expected to be a mustache template in which
  // the query term to be used in the search is represented by a variable
  // called "query".
  // This preserves the existing behaviour of /search-templates.json
  val queryTemplate: String

  // KNN based searches rank by similarity, meaning that the complexity
  // of a full query is not required, and could be misleading to read.
  // A query may have boosts for finding things like exact matches
  // or values in certain fields, and, if copied into the knn filter
  // would be executed in filter context and be discarded.
  // When running a KNN search, the query term becomes a simple filter
  // on whether the document contains the term in any searchable field.
  // If there are fields that should be ignored, then this default will
  // be inappropriate, and can be overridden.
  protected val knnFilter: String = """
    |{
    |  "multi_match": {
    |    "query": "{{query}}",
    |    "operator": "and",
    |    "type": "best_fields",
    |    "tie_breaker": 0.4,
    |    "fields": "query.*"
    |  }
    |}
    |""".stripMargin

  private def semanticKnn(field: String): String =
    s"""
       |{
       |  "knn": {
       |    "field": "$field",
       |    "k": 50,
       |    "similarity": 1,
       |    "num_candidates": 500,
       |    "query_vector_builder": {
       |      "text_embedding": {
       |        "model_id": "openai-text_embedding-muvikv9j5f",
       |        "model_text": "{{query}}"
       |      }
       |    }
       |  }
       |}
       |""".stripMargin

  protected val semanticQueryTemplate: String =
    s"""
       |{
       |  "bool": {
       |    "should": [
       |      ${semanticKnn("query.titleSemantic")},
       |      ${semanticKnn("query.descriptionSemantic")}
       |    ]
       |  }
       |}
       |""".stripMargin

  lazy protected val lexicalQuery: String =
    s"""
       |      "bool": {
       |          {{#query}}
       |          "must": $queryTemplate,
       |          {{/query}}
       |          "filter": {{#toJson}}preFilter{{/toJson}}
       |       }
       |""".stripMargin

  // Importantly, this is *not* JSON, due to the `{{#` sequences, so must be created and sent as a string.
  lazy protected val source: String =
    s"""
       |{
       |  {{#includeSemantic}}
       |    "retriever": {
       |      "rrf": {
       |        "retrievers": [
       |          {
       |            "standard": {
       |              "query": {
       |                $lexicalQuery
       |              }
       |            }
       |          },
       |          {
       |            "standard": {
       |              "query": {
       |                "bool": {
       |                  "must": $semanticQueryTemplate,
       |                  "filter": {{#toJson}}preFilter{{/toJson}}
       |                }
       |              }
       |            }
       |          }
       |        ],
       |        "rank_window_size": 25,
       |        "rank_constant": 20
       |      }
       |    },
       |  {{/includeSemantic}}
       |  {{^includeSemantic}}
       |    {{#knn}}
       |      "knn": {
       |        {{#query}}
       |          "filter": $knnFilter,
       |        {{/query}}
       |        {{#similarityThreshold}}
       |          "similarity": {{similarityThreshold}},
       |        {{/similarityThreshold}}
       |        "field": "{{field}}",
       |        "k": {{k}},
       |        "num_candidates": {{numCandidates}},
       |        "query_vector": {{#toJson}}queryVector{{/toJson}}
       |    },
       |    {{/knn}}
       |    {{^knn}}
       |      "query": {
       |        $lexicalQuery
       |      },
       |    {{/knn}}
       |  {{/includeSemantic}}
       |
       |  {{#postFilter}}
       |  "post_filter": {{#toJson}}postFilter{{/toJson}},
       |  {{/postFilter}}
       |
       |  "from": "{{from}}",
       |  "size": "{{size}}",
       |  "_source": {
       |    "includes": {{#toJson}}includes{{/toJson}}
       |  },
       |
       |  {{#aggs}}
       |  "aggs": {{#toJson}}aggs{{/toJson}},
       |  {{/aggs}}
       |
       |  {{^includeSemantic}}
       |  "sort": [
       |    {{#sortByDate}}
       |    {
       |      "{{sortField}}": {
       |        "order": "{{sortByDate}}",
       |        "missing": "_last"
       |      }
       |    },
       |    {{/sortByDate}}
       |    {{#sortByScore}}
       |    {
       |      "_score": {
       |        "order": "desc"
       |      }
       |    },
       |    {{/sortByScore}}
       |    {
       |      "query.id": {
       |        "order": "asc"
       |      }
       |    }
       |  ],
       |  {{/includeSemantic}}
       |
       |  "track_total_hits": true
       |}
       |""".stripMargin
    // Normalise all whitespace - it only exists to make the code more
    // readable here, but because this eventually gets converted into
    // a string containing loads of escape characters, all the '\n'
    // sequences and wide gaps actually make it harder to read.
    // (it also saves a handful of bytes per request)
      .replaceAll("\\s+", " ")

  def searchRequest(
    indexes: Seq[String],
    params: SearchTemplateParams
  ): TemplateSearchRequest =
    TemplateSearchRequest(indexes, source, params.asJson.deepDropNullValues)

}
