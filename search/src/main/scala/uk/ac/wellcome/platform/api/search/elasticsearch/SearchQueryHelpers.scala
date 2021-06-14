package uk.ac.wellcome.platform.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.queries.matches.FieldWithOptionalBoost

trait SearchQueryHelpers {
  def fieldsWithBoost(
    boost: Int,
    fields: Seq[String]
  ): Seq[FieldWithOptionalBoost] =
    fields.map(FieldWithOptionalBoost(_, Some(boost.toDouble)))
}
