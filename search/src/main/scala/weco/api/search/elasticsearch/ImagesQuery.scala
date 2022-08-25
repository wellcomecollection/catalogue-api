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
    lshQuery("state.inferredData.lshEncodedFeatures", "inferredData.palette")

  def color: (String, Index) => Query =
    lshQuery("state.inferredData.palette")

  def features: (String, Index) => Query =
    lshQuery("state.inferredData.lshEncodedFeatures")

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

  private def boostedWorkFields(
    boost: Option[Double] = None,
    fields: Seq[String]
  ): Seq[FieldWithOptionalBoost] =
    fields.map(f => FieldWithOptionalBoost(s"source.$f", boost))

  private val titleFields = Seq(
    "data.alternativeTitles",
    "data.title.english",
    "data.title.shingles",
    "data.title"
  )

  private val idFields: Seq[FieldWithOptionalBoost] = boostedWorkFields(
    fields = Seq(
      "id.canonicalId",
      "id.sourceIdentifier.value",
      "data.otherIdentifiers.value"
    )
  ) ++ Seq(
    "state.canonicalId",
    "state.sourceIdentifier.value"
  ).map(FieldWithOptionalBoost(_, None))

  private val dataFields: Seq[FieldWithOptionalBoost] = boostedWorkFields(
    boost = Some(1000.0),
    fields = Seq("data.contributors.agent.label")
  ) ++
    boostedWorkFields(
      boost = Some(10.0),
      fields = Seq(
        "data.subjects.concepts.label",
        "data.genres.concepts.label",
        "data.production.*.label"
      )
    ) ++
    boostedWorkFields(
      fields = Seq(
        "data.description",
        "data.physicalDescription",
        "data.language.label",
        "data.edition",
        "data.collectionPath.path",
        "data.collectionPath.label"
      )
    )

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
                prefixQuery("source.data.title.keyword", q),
                matchPhraseQuery("source.data.title", q)
              )
            ),
            MultiMatchQuery(
              q,
              queryName = Some("title exact spellings"),
              fields = boostedWorkFields(
                boost = Some(100.0),
                fields = titleFields
              ),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            ),
            MultiMatchQuery(
              q,
              queryName = Some("non-english text"),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND),
              fields = boostedWorkFields(
                boost = Some(100.0),
                fields = languages
                  .flatMap(
                    language =>
                      Seq(
                        s"data.title.$language",
                        s"data.notes.$language",
                        s"data.lettering.$language"
                      )
                  )
              )
            )
          )
        )
      )
      .minimumShouldMatch(1)
}
