package weco.api.items.fixtures

import weco.catalogue.internal_model.identifiers.IdState
import weco.catalogue.internal_model.locations.{AccessCondition, LocationType, PhysicalLocation}
import weco.catalogue.internal_model.work.Item
import weco.catalogue.internal_model.work.generators.{ItemsGenerators, WorkGenerators}
import weco.catalogue.source_model.generators.SierraGenerators
import weco.catalogue.source_model.sierra.identifiers.SierraItemNumber

trait ItemsApiGenerators
  extends WorkGenerators
  with ItemsGenerators
  with SierraGenerators {

  def createPhysicalItemWith(
    sierraItemNumber: SierraItemNumber,
    accessCondition: AccessCondition
  ): Item[IdState.Identified] = {

    val physicalItemLocation: PhysicalLocation = createPhysicalLocationWith(
      accessConditions = List(accessCondition),
      locationType = LocationType.ClosedStores
      // TODO: Update this to use methods on createPhysicalLocationWith
    ).copy(license = None, shelfmark = None)

    val itemSourceIdentifier = createSierraSystemSourceIdentifierWith(
      value = sierraItemNumber.withCheckDigit,
      ontologyType = "Item"
    )

    createIdentifiedItemWith(
      sourceIdentifier = itemSourceIdentifier,
      locations = List(physicalItemLocation),
    )
  }
}
