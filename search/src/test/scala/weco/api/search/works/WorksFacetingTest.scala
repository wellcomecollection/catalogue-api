package weco.api.search.works

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.GivenWhenThen
import weco.api.search.FacetingFeatures
import weco.api.search.fixtures.{JsonServer, LocalJsonServerFixture}
import weco.api.search.generators.{AggregationDocumentGenerators, BucketGenerators}
import weco.fixtures.TestWith

class WorksFacetingTest
    extends FacetingFeatures
    with ApiWorksTestBase
    with AggregationDocumentGenerators
    with BucketGenerators
    with GivenWhenThen
    with LocalJsonServerFixture {

  protected val resourcePath: String = s"$rootPath/works"

  private val workTypeBuckets = Seq(
    Seq(
      (4, "a", "Books"),
      (3, "d", "Journals"),
      (2, "i", "Audio"),
      (1, "k", "Pictures")
    ) map { case (count, code, label) =>
      toKeywordBucket(count, code, label)
    }: _*
  )
  private val languageBuckets = Seq(
    Seq(
      (4, "bak", "Bashkir"),
      (3, "que", "Quechua"),
      (2, "mar", "Marathi"),
      (1, "che", "Chechen")
    ) map { case (count, code, label) =>
      toKeywordBucket(count, code, label)
    }: _*
  )
  private val capybaraWorkTypeBuckets = Seq(
    Seq(
      (2, "a", "Books"),
      (1, "d", "Journals")
    ) map { case (count, code, label) =>
      toKeywordBucket(count, code, label)
    }: _*
  )

  private val capybaraLanguageBuckets = Seq(
    Seq(
      (2, "mar", "Marathi"),
      (1, "bak", "Bashkir")
    ) map { case (count, code, label) =>
      toKeywordBucket(count, code, label)
    }: _*
  )

  private val marathiWorkTypeBuckets = Seq(
    Seq(
      (1, "a", "Books"),
      (1, "d", "Journals")
    ) map { case (count, code, label) =>
      toKeywordBucket(count, code, label)
    }: _*
  )

  private val bookLanguagesBuckets = Seq(
    Seq(
      (3, "bak", "Bashkir"),
      (1, "mar", "Marathi")
    ) map { case (count, code, label) =>
      toKeywordBucket(count, code, label)
    }: _*
  )

  private val aggregatedWorks =
    (0 to 9).map(i => s"works.examples.filtered-aggregations-tests.$i")

  private val top20AggregatableContributors = ('a' to 'z').map(n =>
    createAggregatableField(s"Beverley Crusher ($n)")
  )
  private val top21AggregatableContributors =
    top20AggregatableContributors ++ Seq(createAggregatableField("Mark Sloan"))
  private val top20FilterableContributors = top20AggregatableContributors.map(_.hcursor.downField("label").as[Json].toOption.get).asJson
  private val top21FilterableContributors = top21AggregatableContributors.map(_.hcursor.downField("label").as[Json].toOption.get).asJson


  private val top21Contributors = Seq(
    createWorkDocument(
      "abadcafe",
      "top 20 only",
      Map("contributors.agent" -> top20AggregatableContributors),
      Map("contributors.agent.label" -> top20FilterableContributors)
    ),
    createWorkDocument(
      "goodcafe",
      "top 20 and hapax",
      Map("contributors.agent" -> top21AggregatableContributors),
      Map("contributors.agent.label" -> top21FilterableContributors)
    )
  )

  private val multipleUncommonContributors =
    top21Contributors :+ createWorkDocument(
      "baadf00d",
      "top 1 and hapax legomenon",
      Map("contributors.agent" -> Seq(createAggregatableField("Yuri Zhivago"),createAggregatableField("Beverley Crusher (a)"))),
      Map("contributors.agent.label" -> Seq("Yuri Zhivago", "Beverley Crusher (a)").asJson)
    )

  private val givens = Map[String, Seq[TestDocument]](
    "a dataset with some common aggregable values and a less common one" -> top21Contributors,
    "a dataset with two uncommon terms in two different documents and some common terms that are not present in one of those documents" -> multipleUncommonContributors
  )

  override protected def Given[R](msg: String)(
    testWith: TestWith[JsonServer, R]
  ): R = {
    super[GivenWhenThen].Given(msg)
    withFacetedAPI[R](givens.get(msg)) {
      testWith(_)
    }
  }

  private def withFacetedAPI[R](
    docs: Option[Seq[TestDocument]]
  )(testWith: TestWith[JsonServer, R]): R =
    withWorksApi[R] { case (worksIndex, route) =>
      docs match {
        case Some(docs) => indexLoadedTestDocuments(worksIndex, docs)
        case None       => indexTestDocuments(worksIndex, aggregatedWorks: _*)
      }
      testWith(new WorksJsonServer(route))
    }

  protected val oneAggregation: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    expectedAggregationBuckets = Map("workType" -> workTypeBuckets)
  )

  protected val twoAggregations: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType", "languages"),
    expectedAggregationBuckets = Map(
      "workType" -> workTypeBuckets,
      "languages" -> languageBuckets
    )
  )

  protected val queryAndAggregations: ScenarioData = ScenarioData(
    queryTerm = Some("capybara"),
    aggregationFields = Seq("workType", "languages"),
    expectedAggregationBuckets = Map(
      "workType" -> capybaraWorkTypeBuckets,
      "languages" -> capybaraLanguageBuckets
    )
  )

  protected val filterOneAggregateAnother: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    filters = Seq(("languages", "mar")),
    expectedAggregationBuckets = Map(
      "workType" -> marathiWorkTypeBuckets
    )
  )

  protected val filterAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    filters = Seq(("workType", "a")),
    expectedAggregationBuckets = Map(
      "workType" -> workTypeBuckets
    )
  )

  protected val filterMultiAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType"),
    filters = Seq(("workType", "a"), ("workType", "d")),
    expectedAggregationBuckets = Map(
      "workType" -> workTypeBuckets
    )
  )

  protected val filterAndAggregateMultiFields: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType", "languages"),
    filters = Seq(("workType", "a"), ("languages", "mar")),
    expectedAggregationBuckets = Map(
      "workType" -> marathiWorkTypeBuckets,
      "languages" -> bookLanguagesBuckets
    )
  )

  protected val mutexFilter: ScenarioData = ScenarioData(
    aggregationFields = Seq("workType", "languages"),
    filters = Seq(("workType", "k"), ("languages", "mar")),
    expectedAggregationBuckets = Map(
      "workType" -> (marathiWorkTypeBuckets :+ toKeywordBucket(
        0,
        "k",
        "Pictures"
      )),
      "languages" -> Seq(
        toKeywordBucket(1, "que", "Quechua"),
        toKeywordBucket(0, "mar", "Marathi")
      )
    )
  )

  protected val emptyBucketFilter: ScenarioData = ScenarioData(
    aggregationFields = Seq("subjects.label"),
    expectedAggregationBuckets = Map(
      "subjects.label" -> Nil
    )
  )

  protected val queryAndFilter: ScenarioData = ScenarioData(
    queryTerm = Some("tapirs"),
    aggregationFields = Seq("workType"),
    filters = Seq(("languages", "que")),
    // tapirs alone would be Pictures(1), Books(1), Journals(1)
    // Quechua alone would be Pictures(1), Journals(2)
    expectedAggregationBuckets = Map(
      "workType" -> Seq(
        toKeywordBucket(1, "d", "Journals"),
        toKeywordBucket(1, "k", "Pictures")
      )
    )
  )

  protected val uncommonTerm: ScenarioData = ScenarioData(
    aggregationFields = Seq("contributors.agent.label"),
    filters = Seq(("contributors.agent.label", "Mark%20Sloan")),
    expectedAggregationBuckets = Map(
      "contributors.agent.label" -> (('a' to 't').map(n =>
        toKeywordBucket(2, s"Beverley Crusher ($n)")
      ) :+ toKeywordBucket(1, "Mark Sloan"))
    )
  )

  protected val multipleUncommonTerms: ScenarioData = ScenarioData(
    filters = Seq(
      (
        "contributors.agent.label",
        "Mark%20Sloan,Yuri%20Zhivago,Beverley%20Crusher%20(z)"
      )
    ),
    aggregationFields = Seq("contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "contributors.agent.label" -> (Seq(
        toKeywordBucket(3, "Beverley Crusher (a)")
      ) ++ ('b' to 't').map(n =>
        toKeywordBucket(2, s"Beverley Crusher ($n)")
      ) ++ Seq(
        toKeywordBucket(2, "Beverley Crusher (z)"),
        toKeywordBucket(1, "Mark Sloan"),
        toKeywordBucket(1, "Yuri Zhivago")
      ))
    )
  )

  protected val queryingUncommonTerms: ScenarioData = ScenarioData(
    queryTerm = Some("Zhivago"),
    filters = Seq(("contributors.agent.label", "Mark%20Sloan")),
    aggregationFields = Seq("contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "contributors.agent.label" -> Seq(
        toKeywordBucket(1, "Beverley Crusher (a)"),
        toKeywordBucket(1, "Yuri Zhivago"),
        toKeywordBucket(0, "Mark Sloan")
      )
    )
  )

}
