package uk.ac.wellcome.platform.api.common.fixtures

import uk.ac.wellcome.platform.api.common.services.source.CatalogueSource.{
  IdentifiersStub,
  ItemStub,
  WorkStub
}

trait CatalogueStubGenerators extends StacksIdentifiersGenerators {
  def createWorkStubWith(items: List[ItemStub]): WorkStub =
    WorkStub(
      id = createCanonicalId.toString(),
      items = items
    )

  def createItemStubWith(identifiers: List[IdentifiersStub]): ItemStub =
    ItemStub(
      id = maybe(createStacksItemIdentifier.value),
      identifiers = Some(identifiers)
    )
}
