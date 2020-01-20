package uk.ac.wellcome.platform.api.services

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.common.Operator
import com.sksamuel.elastic4s.requests.searches.queries.matches.{
  FieldWithOptionalBoost,
  MatchQuery,
  MultiMatchQuery,
  MultiMatchQueryBuilderType
}
import com.sksamuel.elastic4s.requests.searches.queries.{
  BoolQuery,
  ConstantScore,
  Query
}
import uk.ac.wellcome.platform.api.models.SearchQuery
import uk.ac.wellcome.platform.api.models.SearchQueryType.{
  FixedFields,
  IdSearch
}

case class ElasticsearchQueryBuilder(searchQuery: SearchQuery) {
  lazy val query: BoolQuery = searchQuery.queryType match {
    case IdSearch => IdSearchQuery(searchQuery.query).elasticQuery
    case FixedFields =>
      FixedFieldsQuery(searchQuery.query).elasticQuery
  }
}

object QueryDefaults {
  val defaultMSM = "60%"
  val defaultBoostedFields: Seq[(String, Option[Double])] = Seq(
    ("data.title", Some(9.0)),
    // Because subjects and genres have been indexed differently
    // We need to query them slightly differently
    // TODO: (jamesgorrie) think of a more sustainable way of doing this
    // maybe having a just a list of terms that we use terms queries to query against,
    // and then have more structured data underlying
    ("data.subjects.concepts.label", Some(8.0)),
    ("data.genres.concepts.label", Some(8.0)),
    ("data.description", Some(3.0)),
    ("data.contributors.*", Some(2.0)),
    ("data.alternativeTitles", None),
    ("data.physicalDescription", None),
    ("data.production.*.label", None),
    ("data.language.label", None),
    ("data.edition", None),
  )

  val englishBoostedFields: Seq[(String, Option[Double])] = Seq(
    ("data.title.english", Some(9.0)),
    // Because subjects and genres have been indexed differently
    // We need to query them slightly differently
    // TODO: (jamesgorrie) think of a more sustainable way of doing this
    // maybe having a just a list of terms that we use terms queries to query against,
    // and then have more structured data underlying
    ("data.subjects.concepts.label", Some(8.0)),
    ("data.genres.concepts.label", Some(8.0)),
    ("data.description.english", Some(3.0)),
    ("data.contributors.*", Some(2.0)),
    ("data.alternativeTitles.english", None),
    ("data.physicalDescription.english", None),
    ("data.production.*.label", None),
    ("data.language.label", None),
    ("data.edition", None),
  )
}

sealed trait ElasticsearchQuery {
  val q: String
  val elasticQuery: Query
}

final case class IdQuery(q: String) extends ElasticsearchQuery {
  lazy val idFields = Seq(
    "canonicalId.text",
    "sourceIdentifier.value.text",
    "data.otherIdentifiers.value.text",
    "data.items.id.canonicalId.text",
    "data.items.id.sourceIdentifier.value.text",
    "data.items.id.otherIdentifiers.value.text",
  )
  lazy val elasticQuery =
    MultiMatchQuery(
      fields = idFields.map(FieldWithOptionalBoost(_, None)),
      text = q,
      `type` = Some(MultiMatchQueryBuilderType.CROSS_FIELDS)
    )
}

final case class TitleQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "title.english",
      value = q,
      operator = Some(Operator.And))
}

final case class GenreQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "genres.concepts.label",
      value = q,
      operator = Some(Operator.And))
}

final case class SubjectQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "subjects.concepts.label",
      value = q,
      operator = Some(Operator.And))
}

final case class FixedTitleQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "data.title.english",
      value = q,
      operator = Some(Operator.And))
}

final case class FixedGenreQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "data.genres.concepts.label",
      value = q,
      operator = Some(Operator.And))
}

final case class FixedSubjectQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "data.subjects.concepts.label",
      value = q,
      operator = Some(Operator.And))
}

final case class ContributorQuery(q: String) extends ElasticsearchQuery {
  lazy val elasticQuery =
    MatchQuery(
      field = "data.contributors.agent.label",
      value = q,
      operator = Some(Operator.And))
}

final case class IdSearchQuery(q: String) extends ElasticsearchQuery {
  import QueryDefaults._

  val fields = defaultBoostedFields map {
    case (field, boost) =>
      FieldWithOptionalBoost(field = field, boost = boost)
  }

  val baseQuery = MultiMatchQuery(
    text = q,
    fields = fields,
    minimumShouldMatch = Some(defaultMSM),
    `type` = Some(MultiMatchQueryBuilderType.CROSS_FIELDS)
  )

  lazy val elasticQuery =
    bool(
      shouldQueries = Seq(),
      notQueries = Seq(),
      mustQueries = Seq(
        bool(
          mustQueries = Seq(),
          notQueries = Seq(),
          shouldQueries = Seq(
            ConstantScore(IdQuery(q).elasticQuery, boost = Some(5000)),
            ConstantScore(
              query = TitleQuery(q).elasticQuery,
              boost = Some(2000)),
            ConstantScore(
              query = GenreQuery(q).elasticQuery,
              boost = Some(1000)),
            ConstantScore(
              query = SubjectQuery(q).elasticQuery,
              boost = Some(1000)),
            baseQuery
          )
        ))
    )
}

final case class FixedFieldsQuery(q: String) extends ElasticsearchQuery {
  import QueryDefaults._

  val fields = englishBoostedFields map {
    case (field, boost) =>
      FieldWithOptionalBoost(field = field, boost = boost)
  }

  val baseQuery = MultiMatchQuery(
    text = q,
    fields = fields,
    minimumShouldMatch = Some(defaultMSM),
    `type` = Some(MultiMatchQueryBuilderType.CROSS_FIELDS)
  )

  lazy val elasticQuery = bool(
    shouldQueries = Seq(
      ConstantScore(
        query = FixedGenreQuery(q).elasticQuery,
        boost = Some(2000)),
      ConstantScore(
        query = FixedSubjectQuery(q).elasticQuery,
        boost = Some(2000)),
      ConstantScore(
        query = ContributorQuery(q).elasticQuery,
        boost = Some(2000)),
      ConstantScore(query = FixedTitleQuery(q).elasticQuery, boost = Some(1000))
    ),
    mustQueries = Seq(baseQuery),
    notQueries = Seq()
  )
}
