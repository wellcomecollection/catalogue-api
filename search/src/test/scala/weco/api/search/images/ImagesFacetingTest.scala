package weco.api.search.images

import org.scalatest.GivenWhenThen
import weco.api.search.FacetingFeatures
import weco.api.search.fixtures.{JsonServer, LocalJsonServerFixture}
import weco.api.search.generators.{
  AggregationDocumentGenerators,
  BucketGenerators
}
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
        toUnidentifiedBucket(2, "Daguerreotype"),
        toUnidentifiedBucket(1, "Oil Painting")
      )
    )
  )

  protected val twoAggregations: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label", "source.subjects.label"),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        toUnidentifiedBucket(2, "Daguerreotype"),
        toUnidentifiedBucket(1, "Oil Painting")
      ),
      "source.subjects.label" -> Seq(
        toUnidentifiedBucket(2, "Fruit"),
        toUnidentifiedBucket(2, "Surgery"),
        toUnidentifiedBucket(1, "Nursing")
      )
    )
  )

  protected val queryAndAggregations: ScenarioData = ScenarioData(
    queryTerm = Some("mash"),
    aggregationFields =
      Seq("source.subjects.label", "source.contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "source.subjects.label" -> Seq(
        toUnidentifiedBucket(2, "Fruit"),
        toUnidentifiedBucket(1, "Nursing"),
        toUnidentifiedBucket(1, "Surgery")
      ),
      "source.contributors.agent.label" -> Seq(
        toUnidentifiedBucket(1, "BJ Hunnicut"),
        toUnidentifiedBucket(1, "Margaret Houlihan")
      )
    )
  )

  protected val filterOneAggregateAnother: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(("source.subjects.label", "Fruit")),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(toUnidentifiedBucket(2, "Daguerreotype"))
    )
  )

  protected val filterAndAggregateSame: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(("source.genres.label", "Oil%20Painting")),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        toUnidentifiedBucket(2, "Daguerreotype"),
        toUnidentifiedBucket(1, "Oil Painting")
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
        toUnidentifiedBucket(2, "Fruit"),
        toUnidentifiedBucket(2, "Surgery"),
        toUnidentifiedBucket(1, "Nursing")
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
        toUnidentifiedBucket(2, "Fruit"),
        toUnidentifiedBucket(1, "Nursing"),
        toUnidentifiedBucket(1, "Surgery") // Only one Surgery is a Daguerreotype
      ),
      "source.genres.label" -> Seq(
        toUnidentifiedBucket(1, "Daguerreotype"), // Only one Daguerreotype is Surgery
        toUnidentifiedBucket(1, "Oil Painting")
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
        toUnidentifiedBucket(1, "Surgery"),
        toUnidentifiedBucket(0, "Fruit")
      ),
      "source.contributors.agent.label" -> Seq(
        toUnidentifiedBucket(1, "BJ Hunnicut"),
        toUnidentifiedBucket(1, "Margaret Houlihan"),
        toUnidentifiedBucket(0, "Linden Cullen")
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

  protected val queryAndFilter: ScenarioData = ScenarioData()
  protected val uncommonTerm: ScenarioData = ScenarioData()
  protected val multipleUncommonTerms: ScenarioData = ScenarioData()
  protected val queryingUncommonTerms: ScenarioData = ScenarioData()

  private val hunnicutDaguerreotype = createImageDocument(
    s"hunn1234",
    "mash tv",
    Map(
      "source.contributors.agent.label" -> Seq("BJ Hunnicut"),
      "source.genres.label" -> Seq("Daguerreotype"),
      "source.subjects.label" -> Seq("Fruit", "Surgery")
    )
  )
  private val houlihanDaguerrotype = createImageDocument(
    s"houl1234",
    "mash tv film",
    Map(
      "source.contributors.agent.label" -> Seq("Margaret Houlihan"),
      "source.genres.label" -> Seq("Daguerreotype"),
      "source.subjects.label" -> Seq("Fruit", "Nursing")
    )
  )
  private val cullenOilPainting = createImageDocument(
    s"cull1234",
    "holby tv",
    Map(
      "source.contributors.agent.label" -> Seq("Linden Cullen"),
      "source.genres.label" -> Seq("Oil Painting"),
      "source.subjects.label" -> Seq("Surgery")
    )
  )

  private val jonesNoGenre = createImageDocument(
    s"jone1234",
    "who tv",
    Map(
      "source.contributors.agent.label" -> Seq("Martha Jones"),
      "source.subjects.label" -> Seq("Xenobiology")
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
  private val givens: Map[String, Seq[TestDocument]] = Map(
    "a dataset with multiple aggregable fields" -> threeImages,
    "a dataset with queryable content and multiple aggregable fields" -> threeImages,
    "a dataset with multiple aggregable fields, where one record has a field which the others do not" -> setWithOneGenre
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
