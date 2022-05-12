package weco.api.stacks.models

case class CatalogueIdentifierType(id: String)

case class CatalogueIdentifier(
  identifierType: CatalogueIdentifierType,
  value: String
)
