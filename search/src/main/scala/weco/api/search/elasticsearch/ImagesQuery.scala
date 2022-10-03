package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.common.DocumentRef
import com.sksamuel.elastic4s.requests.common.Operator.{AND, OR}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQueryBuilderType.{
  BEST_FIELDS,
  CROSS_FIELDS
}
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery
}

case object ImageSimilarity {
  def blended: (String, Index) => Query =
    lshQuery("query.inferredData.lshEncodedFeatures", "query.inferredData.palette")

  def color: (String, Index) => Query =
    lshQuery("query.inferredData.palette")

  def features: (String, Index) => Query =
    lshQuery("query.inferredData.lshEncodedFeatures")

  private def lshQuery(fields: String*)(q: String, index: Index): Query = {
    val documentRef = DocumentRef(index, q)

    moreLikeThisQuery(fields)
      .likeDocs(List(documentRef))
      .copy(
        minTermFreq = Some(1),
        minDocFreq = Some(1),
        maxQueryTerms = Some(1000),
        minShouldMatch = Some("1")
      )
  }
}

case object ImagesMultiMatcher {
  private val titleFields = Seq(
    "query.source.alternativeTitles",
    "query.source.title.english",
    "query.source.title.shingles",
    "query.source.title"
  )

  private val idFields: Seq[FieldWithOptionalBoost] =
    Seq(
      "query.source.id",
      "query.source.identifiers.value",
      "query.canonicalId",
      "query.sourceIdentifier.value"
    ).map(FieldWithoutBoost(_))

  private val dataFields: Seq[FieldWithOptionalBoost] = {
    val boostedFields =
      Seq(
        (1000, "query.source.contributors.agent.label"),
        (10, "query.source.subjects.concepts.label"),
        (10, "query.source.genres.concepts.label"),
        (10, "query.production.label"),
      ).map { case (boost, field) => FieldWithBoost(field, boost) }

    val unboostedFields =
      Seq(
        "query.source.description",
        "query.source.physicalDescription",
        "query.source.languages.label",
        "query.source.edition",
        "query.source.collectionPath.path",
        "query.source.collectionPath.label",
      ).map(FieldWithoutBoost(_))

    boostedFields ++ unboostedFields
  }

  private val languages =
    List("arabic", "bengali", "french", "german", "hindi", "italian")

  def apply(q: String): BoolQuery =
    boolQuery()
      .should(
        MultiMatchQuery(
          fields = idFields,
          queryName = Some("identifiers"),
          text = q,
          `type` = Some(BEST_FIELDS),
          operator = Some(OR),
          analyzer = Some("whitespace_analyzer")
        ).boost(1000),
        MultiMatchQuery(
          q,
          queryName = Some("data"),
          `type` = Some(CROSS_FIELDS),
          operator = Some(AND),
          fields = dataFields
        ),
        dismax(
          queries = Seq(
            BoolQuery(
              queryName = Some("canonical title prefix"),
              boost = Some(1000.0),
              must = List(
                prefixQuery("query.source.title.keyword", q),
                matchPhraseQuery("query.source.title", q)
              )
            ),
            MultiMatchQuery(
              q,
              queryName = Some("title exact spellings"),
              fields = titleFields.map(field =>
                FieldWithBoost(field, boost = 100)
              ),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            ),
            MultiMatchQuery(
              q,
              queryName = Some("non-english text"),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND),
              fields = languages.flatMap(
                language =>
                  Seq(
                    s"query.source.title.$language",
                    s"query.source.notes.$language",
                    s"query.source.lettering.$language"
                  )
              ).map(field =>
                FieldWithBoost(field, boost = 100)
              )
            )
          )
        )
      )
      .minimumShouldMatch(1)
}
