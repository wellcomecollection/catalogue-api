package weco.api.search.images

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.GivenWhenThen
import weco.api.search.FacetingFeatures
import weco.api.search.fixtures.{JsonServer, LocalJsonServerFixture}
import weco.api.search.generators.{AggregationDocumentGenerators, BucketGenerators}
import weco.fixtures.TestWith

class ImagesFacetingTest
    extends FacetingFeatures
    with ApiImagesTestBase
    with AggregationDocumentGenerators
    with BucketGenerators
    with GivenWhenThen
    with LocalJsonServerFixture {
  protected val resourcePath: String = s"$rootPath/images"

  private val aggregatedImages =
    (0 to 6).map(i => s"images.different-licenses.$i")

  private def withFacetedAPI[R](
    docs: Option[Seq[TestDocument]]
  )(testWith: TestWith[JsonServer, R]): R =
    withImagesApi[R] {
      case (imagesIndex, route) =>
        docs match {
          case Some(docs) => indexLoadedTestDocuments(imagesIndex, docs)
          case None       => indexTestDocuments(imagesIndex, aggregatedImages: _*)
        }
        testWith(new WorksJsonServer(route))
    }

  protected val oneAggregation: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label"),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        toKeywordBucket(2, "Daguerreotype"),
        toKeywordBucket(1, "Oil Painting")
      )
    )
  )

  protected val twoAggregations: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label", "source.subjects.label"),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        toKeywordBucket(2, "Daguerreotype"),
        toKeywordBucket(1, "Oil Painting")
      ),
      "source.subjects.label" -> Seq(
        toKeywordBucket(2, "Fruit"),
        toKeywordBucket(2, "Surgery"),
        toKeywordBucket(1, "Nursing")
      )
    )
  )

  protected val queryAndAggregations: ScenarioData = ScenarioData(
    queryTerm = Some("mash"),
    aggregationFields =
      Seq("source.subjects.label", "source.contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "source.subjects.label" -> Seq(
        toKeywordBucket(2, "Fruit"),
        toKeywordBucket(1, "Nursing"),
        toKeywordBucket(1, "Surgery")
      ),
      "source.contributors.agent.label" -> Seq(
        toKeywordBucket(1, "BJ Hunnicut"),
        toKeywordBucket(1, "Margaret Houlihan")
      )
    )
  )

  protected val filterOneAggregateAnother: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(("source.subjects.label", "Fruit")),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(toKeywordBucket(2, "Daguerreotype"))
    )
  )

  protected val filterAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(("source.genres.label", "Oil%20Painting")),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        toKeywordBucket(2, "Daguerreotype"),
        toKeywordBucket(1, "Oil Painting")
      )
    )
  )

  protected val filterMultiAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.subjects.label"),
    filters = Seq(
      ("source.subjects.label", "Fruit"),
      ("source.subjects.label", "Nursing")
    ),
    expectedAggregationBuckets = Map(
      "source.subjects.label" -> Seq(
        toKeywordBucket(2, "Fruit"),
        toKeywordBucket(2, "Surgery"),
        toKeywordBucket(1, "Nursing")
      )
    )
  )

  protected val filterAndAggregateMultiFields: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.subjects.label", "source.genres.label"),
    filters = Seq(
      ("source.subjects.label", "Surgery"),
      ("source.genres.label", "Daguerreotype")
    ),
    expectedAggregationBuckets = Map(
      "source.subjects.label" -> Seq(
        toKeywordBucket(2, "Fruit"),
        toKeywordBucket(1, "Nursing"),
        toKeywordBucket(1, "Surgery") // Only one Surgery is a Daguerreotype
      ),
      "source.genres.label" -> Seq(
        toKeywordBucket(1, "Daguerreotype"), // Only one Daguerreotype is Surgery
        toKeywordBucket(1, "Oil Painting")
      )
    )
  )

  protected val mutexFilter: ScenarioData = ScenarioData(
    aggregationFields =
      Seq("source.subjects.label", "source.contributors.agent.label"),
    filters = Seq(
      ("source.subjects.label", "Fruit"),
      ("source.contributors.agent.label", "Linden%20Cullen")
    ),
    expectedAggregationBuckets = Map(
      "source.subjects.label" -> Seq(
        toKeywordBucket(1, "Surgery"),
        toKeywordBucket(0, "Fruit")
      ),
      "source.contributors.agent.label" -> Seq(
        toKeywordBucket(1, "BJ Hunnicut"),
        toKeywordBucket(1, "Margaret Houlihan"),
        toKeywordBucket(0, "Linden Cullen")
      )
    )
  )

  protected val emptyBucketFilter: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(
      ("source.subjects.label", "Xenobiology")
    ),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Nil
    )
  )

  protected val queryAndFilter: ScenarioData = ScenarioData(
    queryTerm = Some("mash"),
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(("source.subjects.label", "Surgery")),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        // Only Hunnicut is Surgery+mash
        toKeywordBucket(1, "Daguerreotype")
      )
    )
  )

  protected val uncommonTerm: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.contributors.agent.label"),
    filters = Seq(("source.contributors.agent.label", "Mark%20Sloan")),
    expectedAggregationBuckets = Map(
      "source.contributors.agent.label" -> (('a' to 't').map(
        n => toKeywordBucket(2, s"Beverley Crusher ($n)")
      ) :+ toKeywordBucket(1, "Mark Sloan"))
    )
  )

  protected val multipleUncommonTerms: ScenarioData = ScenarioData(
    filters = Seq(
      (
        "source.contributors.agent.label",
        "Mark%20Sloan,Yuri%20Zhivago,Beverley%20Crusher%20(z)"
      )
    ),
    aggregationFields = Seq("source.contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "source.contributors.agent.label" -> (Seq(
        toKeywordBucket(3, "Beverley Crusher (a)")
      ) ++ ('b' to 't').map(
        n => toKeywordBucket(2, s"Beverley Crusher ($n)")
      ) ++ Seq(
        toKeywordBucket(2, "Beverley Crusher (z)"),
        toKeywordBucket(1, "Mark Sloan"),
        toKeywordBucket(1, "Yuri Zhivago")
      ))
    )
  )

  protected val queryingUncommonTerms: ScenarioData = ScenarioData(
    queryTerm = Some("Zhivago"),
    filters = Seq(("source.contributors.agent.label", "Mark%20Sloan")),
    aggregationFields = Seq("source.contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "source.contributors.agent.label" -> Seq(
        toKeywordBucket(1, "Beverley Crusher (a)"),
        toKeywordBucket(1, "Yuri Zhivago"),
        toKeywordBucket(0, "Mark Sloan")
      )
    )
  )

  private val hunnicutDaguerreotype = createImageDocument(
    s"hunn1234",
    "mash tv",
    Map(
      "source.contributors.agent" -> Seq(createAggregatableField("BJ Hunnicut")),
      "source.genres" -> Seq(createAggregatableField("Daguerreotype")),
      "source.subjects" -> Seq(createAggregatableField("Fruit"), createAggregatableField("Surgery"))
    ),
    Map(
      "source.contributors.agent.label" -> Seq("BJ Hunnicut").asJson,
      "source.genres.label" -> Seq("Daguerreotype").asJson,
      "source.subjects.label" -> Seq("Fruit", "Surgery").asJson
    ),
  )
  private val houlihanDaguerrotype = createImageDocument(
    s"houl1234",
    "mash tv film",
    Map(
      "source.contributors.agent" -> Seq(createAggregatableField("Margaret Houlihan")),
      "source.genres" -> Seq(createAggregatableField("Daguerreotype")),
      "source.subjects" -> Seq(createAggregatableField("Fruit"), createAggregatableField("Nursing"))
    ),
    Map(
      "source.contributors.agent.label" -> Seq("Margaret Houlihan").asJson,
      "source.genres.label" -> Seq("Daguerreotype").asJson,
      "source.subjects.label" -> Seq("Fruit", "Nursing").asJson
    ),
  )
  private val cullenOilPainting = createImageDocument(
    s"cull1234",
    "holby tv",
    Map(
      "source.contributors.agent" -> Seq(createAggregatableField("Linden Cullen")),
      "source.genres" -> Seq(createAggregatableField("Oil Painting")),
      "source.subjects" -> Seq(createAggregatableField("Surgery"))
    ),
    Map(
      "source.contributors.agent.label" -> Seq("Linden Cullen").asJson,
      "source.genres.label" -> Seq("Oil Painting").asJson,
      "source.subjects.label" -> Seq("Surgery").asJson
    )
  )

  private val jonesNoGenre = createImageDocument(
    s"jone1234",
    "who tv",
    Map(
      "source.contributors.agent" -> Seq(createAggregatableField("Martha Jones")),
      "source.subjects" -> Seq(createAggregatableField("Xenobiology"))
    ),
    Map(
      "source.contributors.agent.label" -> Seq("Martha Jones").asJson,
      "source.subjects.label" -> Seq("Xenobiology").asJson
    )
  )

  private val threeImages = Seq(
    hunnicutDaguerreotype,
    houlihanDaguerrotype,
    cullenOilPainting
  )
  private val setWithOneGenre = Seq(
    cullenOilPainting,
    jonesNoGenre
  )

  private val top20AggregatableContributors = ('a' to 'z').map(n =>
    createAggregatableField(s"Beverley Crusher ($n)")
  )
  private val top21AggregatableContributors =
    top20AggregatableContributors ++ Seq(createAggregatableField("Mark Sloan"))
  private val top20FilterableContributors = top20AggregatableContributors.map(_.hcursor.downField("label").as[Json].toOption.get).asJson
  private val top21FilterableContributors = top21AggregatableContributors.map(_.hcursor.downField("label").as[Json].toOption.get).asJson

  private val top21Contributors = Seq(
    createImageDocument(
      s"abadcafe",
      "top 20 only",
      Map("source.contributors.agent" -> top20AggregatableContributors),
      Map("source.contributors.agent.label" -> top20FilterableContributors)
    ),
    createImageDocument(
      "goodcafe",
      "top 20 and hapax",
      Map("source.contributors.agent" -> top21AggregatableContributors),
      Map("source.contributors.agent.label" -> top21FilterableContributors)
    )
  )

  private val multipleUncommonContributors = top21Contributors :+ createImageDocument(
    "baadf00d",
    "top 1 and hapax legomenon",
    Map("source.contributors.agent" -> Seq(createAggregatableField("Yuri Zhivago"),createAggregatableField("Beverley Crusher (a)"))),
    Map("source.contributors.agent.label" -> Seq("Yuri Zhivago", "Beverley Crusher (a)").asJson)
  )

  private val givens: Map[String, Seq[TestDocument]] = Map(
    "a dataset with multiple aggregable fields" -> threeImages,
    "a dataset with queryable content and multiple aggregable fields" -> threeImages,
    "a dataset with multiple aggregable fields, where one record has a field which the others do not" -> setWithOneGenre,
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

}
