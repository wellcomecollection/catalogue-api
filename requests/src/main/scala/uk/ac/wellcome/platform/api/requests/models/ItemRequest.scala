package uk.ac.wellcome.platform.api.requests.models

case class ItemRequest(
  // Note: there isn't a 1:1 mapping between works and items, so given
  // only an item ID you can't necessarily take a user back to the work page
  // they were looking at when they requested the item.
  //
  // We save the work ID in anticipation of a future change where we record it,
  // so we can do that redirection properly.  It isn't currently used.
  workId: String,
  itemId: String,
  `type`: String = "ItemRequest"
)
