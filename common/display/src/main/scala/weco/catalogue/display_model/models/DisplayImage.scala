package weco.catalogue.display_model.models

import io.circe.generic.extras.JsonKey
import weco.catalogue.internal_model.image.{Image, ImageState}

case class DisplayImage(
  id: String,
  thumbnail: DisplayDigitalLocation,
  locations: Seq[DisplayDigitalLocation],
  source: DisplayImageSource,
  visuallySimilar: Option[Seq[DisplayImage]],
  withSimilarColors: Option[Seq[DisplayImage]],
  withSimilarFeatures: Option[Seq[DisplayImage]],
  @JsonKey("type") ontologyType: String = "Image"
)

object DisplayImage {

  def apply(
    image: Image[ImageState.Indexed],
    includes: ImageIncludes
  ): DisplayImage =
    new DisplayImage(
      id = image.id,
      thumbnail = DisplayDigitalLocation(image.state.derivedData.thumbnail),
      locations = image.locations.map(DisplayDigitalLocation(_)),
      source = DisplayImageSource(image.source, includes),
      visuallySimilar = None,
      withSimilarColors = None,
      withSimilarFeatures = None
    )
}
