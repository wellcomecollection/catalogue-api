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

  protected val queryAndFilter: ScenarioData = ScenarioData(
    queryTerm = Some("mash"),
    aggregationFields = Seq("source.genres.label"),
    filters = Seq(("source.subjects.label", "Surgery")),
    expectedAggregationBuckets = Map(
      "source.genres.label" -> Seq(
        // Only Hunnicut is Surgery+mash
        toUnidentifiedBucket(1, "Daguerreotype")
      )
    )
  )

  protected val uncommonTerm: ScenarioData = ScenarioData(
    aggregationFields = Seq("source.contributors.agent.label"),
    filters = Seq(("source.contributors.agent.label", "Mark%20Sloan")),
    expectedAggregationBuckets = Map(
      "source.contributors.agent.label" -> (('a' to 't').map(
        n => toUnidentifiedBucket(2, s"Beverley Crusher ($n)")
      ) :+ toUnidentifiedBucket(1, "Mark Sloan"))
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
        toUnidentifiedBucket(3, "Beverley Crusher (a)")
      ) ++ ('b' to 't').map(
        n => toUnidentifiedBucket(2, s"Beverley Crusher ($n)")
      ) ++ Seq(
        toUnidentifiedBucket(2, "Beverley Crusher (z)"),
        toUnidentifiedBucket(1, "Mark Sloan"),
        toUnidentifiedBucket(1, "Yuri Zhivago")
      ))
    )
  )

  protected val queryingUncommonTerms: ScenarioData = ScenarioData(
    queryTerm = Some("Zhivago"),
    filters = Seq(("source.contributors.agent.label", "Mark%20Sloan")),
    aggregationFields = Seq("source.contributors.agent.label"),
    expectedAggregationBuckets = Map(
      "source.contributors.agent.label" -> Seq(
        toUnidentifiedBucket(1, "Beverley Crusher (a)"),
        toUnidentifiedBucket(1, "Yuri Zhivago"),
        toUnidentifiedBucket(0, "Mark Sloan")
      )
    )
  )

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

  private val top21Contributors = Seq(
    createImageDocument(
      s"abadcafe",
      "top 20 only",
      Map(
        "source.contributors.agent.label" -> ('a' to 'z')
          .map(n => s"Beverley Crusher ($n)")
      )
    ),
    createImageDocument(
      "goodcafe",
      "top 20 and hapax",
      Map(
        "source.contributors.agent.label" -> (('a' to 'z')
          .map(n => s"Beverley Crusher ($n)") :+ "Mark Sloan")
      )
    )
  )

  private val multipleUncommonContributors = top21Contributors :+ createImageDocument(
    "baadf00d",
    "top 1 and hapax legomenon",
    Map(
      "source.contributors.agent.label" -> Seq(
        "Yuri Zhivago",
        "Beverley Crusher (a)"
      )
    )
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
