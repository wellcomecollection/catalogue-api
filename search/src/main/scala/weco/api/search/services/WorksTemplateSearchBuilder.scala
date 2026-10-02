package weco.api.search.services

import scala.io.Source

trait WorksTemplateSearchBuilder extends TemplateSearchBuilder {

  val queryTemplate: String =
    Source.fromResource("WorksQuery.json").mkString

  override protected lazy val source: String =
    normaliseSource(
      s"""
       |{
       |  "query": $lexicalQuery,
       |  "sort": $sort,
       |  $commonQueryFields
       |}
       |""".stripMargin
    )
}

object WorksTemplateSearchBuilder extends WorksTemplateSearchBuilder
