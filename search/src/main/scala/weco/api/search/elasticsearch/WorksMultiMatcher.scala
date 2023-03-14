package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.Operator.OR
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.queries.matches.MultiMatchQueryBuilderType.{
  BEST_FIELDS,
  CROSS_FIELDS,
  MOST_FIELDS
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
  val titleFields = Seq(
    "query.titlesAndContributors",
    "query.titlesAndContributors.english",
    "query.titlesAndContributors.shingles"
  )

  def fieldsWithBoost(
    boost: Int,
    fields: Seq[String]
  ): Seq[FieldWithOptionalBoost] =
    fields.map(FieldWithBoost(_, boost))

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
              "query.referenceNumber",
              // TODO: Do we need to be querying this field at this point?
              // Could we delete it and query the individual fields instead?
              "query.allIdentifiers"
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
                fields = titleFields
              ),
              `type` = Some(BEST_FIELDS),
              operator = Some(OR)
            ).minimumShouldMatch("-30%"),
            MultiMatchQuery(
              q,
              queryName = Some("non-english titles and contributors"),
              fields = List(
                "arabic",
                "bengali",
                "french",
                "german",
                "hindi",
                "italian"
              ).map(
                language =>
                  FieldWithoutBoost(s"query.titlesAndContributors.$language")
              ),
              `type` = Some(BEST_FIELDS),
              operator = Some(OR)
            ).minimumShouldMatch("-30%")
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
                FieldWithBoost("query.title", boost = 100),
                FieldWithBoost("query.description", boost = 10)
              )
            )
          ),
          mustQueries = List(
            MultiMatchQuery(
              q,
              queryName = Some("relations paths"),
              operator = Some(OR),
              fields = Seq(
                FieldWithoutBoost("query.collectionPath.path.clean"),
                FieldWithoutBoost("query.collectionPath.label.cleanPath"),
                FieldWithoutBoost("query.collectionPath.label"),
                FieldWithoutBoost("query.collectionPath.path.keyword")
              )
            )
          ),
          notQueries = Nil
        ),
        MultiMatchQuery(
          q,
          queryName = Some("data"),
          `type` = Some(CROSS_FIELDS),
          operator = Some(OR),
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
          ).map {
            case (boost, field) =>
              FieldWithOptionalBoost(field, boost.map(_.toDouble))
          }
        ).minimumShouldMatch("-30%"),
        MultiMatchQuery(
          q,
          queryName = Some("shingles cased"),
          `type` = Some(MOST_FIELDS),
          operator = Some(OR),
          fields = Seq(
            (Some(1000), "query.title.shingles_cased"),
            (Some(100), "query.alternativeTitles.shingles_cased"),
            (Some(10), "query.partOf.title.shingles_cased")
          ).map {
            case (boost, field) =>
              FieldWithOptionalBoost(field, boost.map(_.toDouble))
          }
        ).minimumShouldMatch("-30%")
      )
      .minimumShouldMatch(1)
}
