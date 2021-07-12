package weco.api.items.fixtures

import weco.catalogue.internal_model.locations.{AccessCondition, LocationType, PhysicalLocation}
import weco.catalogue.internal_model.work.generators.{ItemsGenerators, WorkGenerators}

trait Generators
  extends WorkGenerators
  with ItemsGenerators{

  def createPhysicalItemWith(sierraItemIdentifier: String,
                                     accessCondition: AccessCondition) = {
    val physicalItemLocation: PhysicalLocation = createPhysicalLocationWith(
      accessConditions = List(accessCondition),
      locationType = LocationType.ClosedStores
    ).copy(license = None, shelfmark = None)

    // Sierra identifiers without check digits are always 7 digits
    require(sierraItemIdentifier.matches("^\\d{7}$"))

    val physicalItemSierraIdentifierWithCheckDigit = f"${sierraItemIdentifier}5"
    val physicalItemSierraSourceIdentifier =
      f"i${physicalItemSierraIdentifierWithCheckDigit}"

    val itemSourceIdentifier = createSierraSystemSourceIdentifierWith(
      value = physicalItemSierraSourceIdentifier,
      ontologyType = "Item"
    )

    createIdentifiedItemWith(
      sourceIdentifier = itemSourceIdentifier,
      locations = List(physicalItemLocation),
    )
  }
}
