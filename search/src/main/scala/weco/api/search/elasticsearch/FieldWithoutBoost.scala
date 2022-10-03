package weco.api.search.elasticsearch

import com.sksamuel.elastic4s.requests.searches.queries.matches.FieldWithOptionalBoost

object FieldWithBoost {
  def apply(field: String, boost: Int): FieldWithOptionalBoost =
    FieldWithOptionalBoost(field, boost = Some(boost.toDouble))
}

object FieldWithoutBoost {
  def apply(field: String): FieldWithOptionalBoost =
    FieldWithOptionalBoost(field, boost = None)
}
