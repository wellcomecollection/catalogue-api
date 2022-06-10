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
  MatchQuery,
  MultiMatchQuery
}
import com.sksamuel.elastic4s.requests.searches.span.{
  SpanFirstQuery,
  SpanTermQuery
}

case object WorksMultiMatcher {
  val titleFields = Seq(
    "search.titlesAndContributors",
    "search.titlesAndContributors.english",
    "search.titlesAndContributors.shingles"
  )

  def fieldsWithBoost(
    boost: Int,
    fields: Seq[String]
  ): Seq[FieldWithOptionalBoost] =
    fields.map(FieldWithOptionalBoost(_, Some(boost.toDouble)))

  private val languages =
    List("arabic", "bengali", "french", "german", "hindi", "italian")

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
            field = "data.title.shingles",
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
            Seq(
              "query.id",
              "query.identifiers.value",
              "query.items.id",
              "query.items.identifiers.value",
              "query.images.id",
              "query.images.identifiers.value",
              "data.referenceNumber"
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
              fields = fieldsWithBoost(boost = 100, fields = titleFields),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            ),
            MultiMatchQuery(
              q,
              queryName = Some("non-english titles and contributors"),
              fields = languages.map(
                language =>
                  FieldWithOptionalBoost(
                    s"search.titlesAndContributors.$language",
                    None
                  )
              ),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            )
          )
        ),
        MatchQuery(
          queryName = Some("relations"),
          field = "search.relations",
          value = q,
          operator = Some(AND),
          boost = Some(1000)
        ),
        MultiMatchQuery(
          q,
          queryName = Some("data"),
          `type` = Some(CROSS_FIELDS),
          operator = Some(AND),
          fields = Seq(
            (Some(1000), "data.contributors.agent.label"),
            (Some(10), "query.subjects.concepts.label"),
            (Some(10), "query.genres.concepts.label"),
            (Some(10), "data.production.*.label"),
            (None, "data.description"),
            (None, "data.physicalDescription"),
            (None, "data.language.label"),
            (None, "data.edition"),
            (None, "data.notes.contents"),
            (None, "data.lettering")
          ).map(f => FieldWithOptionalBoost(f._2, f._1.map(_.toDouble)))
        )
      )
      .minimumShouldMatch(1)
}
