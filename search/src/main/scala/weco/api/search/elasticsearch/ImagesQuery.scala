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
import weco.api.search.models.index.IndexedImage
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery
}
import io.circe.syntax._
import io.circe.{Json, JsonObject}

case object ImageSimilarity {
  def blended: (String, IndexedImage, Index) => JsonObject =
    // For now, we're replacing the blended lsh query with a single knn query
    // onthe image feature vector. We'll come back to blend the features and
    // colours after some more testing.
    //
    // lshQuery(
    //   "query.inferredData.lshEncodedFeatures",
    //   "query.inferredData.palette"
    // )
    knnQuery("query.inferredData.reducedFeatures")

  def color: (String, IndexedImage, Index) => Query =
    lshQuery("query.inferredData.palette")

  private def lshQuery(
    fields: String*
  )(imageId: String, image: IndexedImage, index: Index): Query = {
    val documentRef = DocumentRef(index, imageId)

    moreLikeThisQuery(fields)
      .likeDocs(List(documentRef))
      .copy(
        minTermFreq = Some(1),
        minDocFreq = Some(1),
        maxQueryTerms = Some(1000),
        minShouldMatch = Some("1")
      )
  }

  def features: (String, IndexedImage, Index) => JsonObject =
    knnQuery("query.inferredData.reducedFeatures")

  private def knnQuery(
    field: String
  )(imageId: String, image: IndexedImage, index: Index): JsonObject =
    Json
      .obj(
        "knn" -> Json.obj(
          "field" -> field.asJson,
          "query_vector" -> image.reducedFeatures.asJson,
          "k" -> 10.asJson,
          "num_candidates" -> 100.asJson,
          "filter" -> Json.obj(
            "bool" -> Json.obj(
              "must_not" -> Json.obj(
                "ids" -> Json.obj(
                  "values" -> Json.arr(Json.fromString(imageId))
                )
              )
            )
          )
        )
      )
      .asObject
      .get
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
      "query.id",
      "query.sourceIdentifier.value"
    ).map(FieldWithoutBoost(_))

  private val dataFields: Seq[FieldWithOptionalBoost] = {
    val boostedFields =
      Seq(
        (1000, "query.source.contributors.agent.label"),
        (10, "query.source.subjects.concepts.label"),
        (10, "query.source.genres.concepts.label"),
        (10, "query.source.production.label")
      ).map { case (boost, field) => FieldWithBoost(field, boost) }

    val unboostedFields =
      Seq(
        "query.source.description",
        "query.source.physicalDescription",
        "query.source.languages.label",
        "query.source.edition",
        "query.source.collectionPath.path",
        "query.source.collectionPath.label"
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
              fields =
                titleFields.map(field => FieldWithBoost(field, boost = 100)),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            ),
            MultiMatchQuery(
              q,
              queryName = Some("non-english text"),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND),
              fields = languages
                .flatMap(
                  language =>
                    Seq(
                      s"query.source.title.$language",
                      s"query.source.notes.$language",
                      s"query.source.lettering.$language"
                  )
                )
                .map(field => FieldWithBoost(field, boost = 100))
            )
          )
        )
      )
      .minimumShouldMatch(1)
}
