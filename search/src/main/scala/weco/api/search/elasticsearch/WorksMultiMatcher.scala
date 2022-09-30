package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.Operator.{AND, OR}
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQueryBuilderType.{
  BEST_FIELDS,
  CROSS_FIELDS
}
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MultiMatchQuery
}
import com.sksamuel.elastic4s.requests.searches.span.{
  SpanFirstQuery,
  SpanTermQuery
}

case object WorksMultiMatcher {
  val titleAndContributorFields = Seq(
    "query.title",
    "query.alternativeTitles",
    "query.contributors.agent.label"
  )

  val nonEnglishLanguages = List(
    "arabic",
    "bengali",
    "french",
    "german",
    "hindi",
    "italian"
  )

  def fieldsWithBoost(
    boost: Int,
    fields: Seq[String]
  ): Seq[FieldWithOptionalBoost] =
    fields.map(FieldWithOptionalBoost(_, Some(boost.toDouble)))

  def apply(q: String): BoolQuery =
    boolQuery()
      .should(
        // This prioritises exact matches at the start of titles.
        //
        // e.g. if we had three works
        //
        //      Human genetic information : science, law, and ethics
        //      International journal of law and information technology
        //      Information law : compliance for librarians and information professionals
        //
        // and somebody searches for "Information law", all other things being equal,
        // we want to prioritise the third result.
        //
        // See https://github.com/wellcomecollection/catalogue-api/issues/466
        SpanFirstQuery(
          SpanTermQuery(
            field = "query.title.shingles",
            value = q
          ),
          boost = Some(1000),
          queryName = Some("start of title"),
          end = 1
        ),
        MultiMatchQuery(
          q,
          queryName = Some("identifiers"),
          `type` = Some(BEST_FIELDS),
          operator = Some(OR),
          analyzer = Some("whitespace_analyzer"),
          fields = fieldsWithBoost(
            boost = 1000,
            fields = Seq(
              "query.id",
              "query.identifiers.value",
              "query.items.id",
              "query.items.identifiers.value",
              "query.images.id",
              "query.images.identifiers.value",
              "query.referenceNumber"
            )
          )
        ),
        /**
          * This is the different ways we can match on the title fields
          * - title exact spellings: Exact spellings as they have been catalogued
          * - title alternative spellings: Alternative spellings which people might search for e.g. in transliterations
          */
        dismax(
          queries = Seq(
            MultiMatchQuery(
              q,
              queryName = Some("title and contributor exact spellings"),
              fields = fieldsWithBoost(
                boost = 100,
                fields = titleAndContributorFields.flatMap(field =>
                  Seq(
                    field,
                    s"$field.english",
                    s"$field.shingles"
                  )
                )
              ),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            ),
            MultiMatchQuery(
              q,
              queryName = Some("non-english titles and contributors"),
              fields =
                titleAndContributorFields.flatMap(field =>
                  nonEnglishLanguages.map(language =>
                    FieldWithOptionalBoost(
                      field = s"$field.$language",
                      boost = None
                    )
                  )
                ),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            )
          )
        ),
        bool(
          shouldQueries = List(
            MultiMatchQuery(
              q,
              queryName = Some("relations text"),
              `type` = Some(CROSS_FIELDS),
              operator = Some(OR),
              fields = Seq(
                FieldWithOptionalBoost("query.title", boost = Some(100)),
                FieldWithOptionalBoost("query.description", boost = Some(10))
              )
            )
          ),
          mustQueries = List(
            MultiMatchQuery(
              q,
              queryName = Some("relations paths"),
              operator = Some(OR),
              fields = Seq(
                FieldWithOptionalBoost("query.collectionPath.path.clean", None),
                FieldWithOptionalBoost(
                  "query.collectionPath.label.cleanPath",
                  None
                ),
                FieldWithOptionalBoost("query.collectionPath.label", None),
                FieldWithOptionalBoost("query.collectionPath.path.keyword", None)
              )
            )
          ),
          notQueries = Nil
        ),
        MultiMatchQuery(
          q,
          queryName = Some("data"),
          `type` = Some(CROSS_FIELDS),
          operator = Some(AND),
          fields = Seq(
            (Some(1000), "query.contributors.agent.label"),
            (Some(10), "query.subjects.concepts.label"),
            (Some(10), "query.genres.concepts.label"),
            (Some(10), "query.production.label"),
            (None, "query.description"),
            (None, "query.physicalDescription"),
            (None, "query.languages.label"),
            (None, "query.edition"),
            (None, "query.notes.contents"),
            (None, "query.lettering")
          ).map { case (boost, field) => FieldWithOptionalBoost(field, boost.map(_.toDouble)) }
        )
      )
      .minimumShouldMatch(1)
}
