package uk.ac.wellcome.platform.api.search.elasticsearch

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
import uk.ac.wellcome.models.index.WorksAnalysis.{languages, whitespaceAnalyzer}

case object WorksWithRelationsQuery
    extends SearchQuery
    with SearchQueryHelpers {
  val titleFields = Seq(
    "data.title",
    "data.title.english",
    "data.title.shingles",
    "data.alternativeTitles"
  )

  def apply(q: String): BoolQuery = {
    boolQuery()
      .should(
        MultiMatchQuery(
          q,
          queryName = Some("identifiers"),
          `type` = Some(BEST_FIELDS),
          operator = Some(OR),
          analyzer = Some(whitespaceAnalyzer.name),
          fields = fieldsWithBoost(
            boost = 1000,
            Seq(
              "state.canonicalId",
              "state.sourceIdentifier.value",
              "data.otherIdentifiers.value",
              "data.items.id.canonicalId",
              "data.items.id.sourceIdentifier.value",
              "data.items.id.otherIdentifiers.value",
              "data.imageData.id.canonicalId",
              "data.imageData.id.sourceIdentifier.value",
              "data.imageData.id.otherIdentifiers.value"
            )
          )
        ),
        /**
          * This is the different ways we can match on the title fields
          * - title prefix: An exact match, in order
          * - title exact spellings: Exact spellings as they have been catalogued
          * - title alternative spellings: Alternative spellings which people might search for e.g. in transliterations
          */
        dismax(
          queries = Seq(
            BoolQuery(
              queryName = Some("title prefix"),
              boost = Some(1000),
              must = List(
                prefixQuery("data.title.keyword", q),
                matchPhraseQuery("data.title", q)
              )
            ),
            MultiMatchQuery(
              q,
              queryName = Some("title exact spellings"),
              fields = fieldsWithBoost(boost = 100, fields = titleFields),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND)
            ),
            MultiMatchQuery(
              q,
              queryName = Some("title alternative spellings"),
              fields = fieldsWithBoost(boost = 80, fields = titleFields),
              `type` = Some(BEST_FIELDS),
              operator = Some(AND),
              fuzziness = Some("AUTO")
            ),
            MultiMatchQuery(
              q,
              queryName = Some("non-english titles"),
              fields = languages.map(
                language =>
                  FieldWithOptionalBoost(s"data.title.${language}", None)
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
            (Some(10), "data.subjects.concepts.label"),
            (Some(10), "data.genres.concepts.label"),
            (Some(10), "data.production.*.label"),
            (None, "data.description"),
            (None, "data.physicalDescription"),
            (None, "data.language.label"),
            (None, "data.edition"),
            (None, "data.notes.content"),
            (None, "data.lettering")
          ).map(f => FieldWithOptionalBoost(f._2, f._1.map(_.toDouble)))
        )
      )
      .minimumShouldMatch(1)
  }
}
