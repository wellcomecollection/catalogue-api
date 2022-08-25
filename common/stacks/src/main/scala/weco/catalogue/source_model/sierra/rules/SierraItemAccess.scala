package weco.catalogue.source_model.sierra.rules

import grizzled.slf4j.Logging
import weco.api.stacks.models.{CatalogueAccessMethod, CatalogueLocationType}
import weco.catalogue.display_model.locations.{CatalogueAccessStatus, DisplayAccessCondition, DisplayLocationType}
import weco.catalogue.source_model.sierra.source.{OpacMsg, Status}
import weco.sierra.models.SierraQueryOps
import weco.sierra.models.data.SierraItemData

/** There are multiple sources of truth for item information in Sierra, and whether
  * a given item can be requested online.
  *
  * The catalogue pipeline contains our canonical implementation of these rules,
  * and the front-end will prevent users from requesting the majority of items
  * that are blocked by these rules (anything that's long-term unavailable).
  *
  * This is a simplified implementation of these rules, to cover cases where an item
  * has only just become unrequestable, e.g. if two users try to request the
  * same item at the same time.
  *
  * If it returns None, that means we should use the access condition from the catalogue API.
  *
  * We remove rules that would end in a "definitely not requestable" state, because
  * those items should be filtered out by the front end.
  *
  */
object SierraItemAccess extends SierraQueryOps with Logging {
  def apply(
    location: Option[DisplayLocationType],
    itemData: SierraItemData
  ): (Option[DisplayAccessCondition], Option[String]) = {
    val accessCondition = createAccessCondition(
      holdCount = itemData.holdCount,
      status = itemData.status,
      opacmsg = itemData.opacmsg,
      rulesForRequestingResult = SierraRulesForRequesting(itemData),
      locationTypeId = location.map(_.id),
      itemData = itemData
    )

    (accessCondition, itemData.displayNote) match {
      // If the item note is already on the access condition, we don't need to copy it.
      case (Some(ac), displayNote) if ac.note == displayNote =>
        (Some(ac), None)

      // If the item note is an access note but there's already an access note on the
      // access condition, we discard the item note.
      //
      // Otherwise, we copy the item note onto the access condition.
      case (Some(ac), Some(displayNote))
        if ac.note.isDefined && displayNote.isAccessNote =>
        (Some(ac), None)
      case (Some(ac), Some(displayNote))
        if ac.note.isEmpty && displayNote.isAccessNote =>
        (Some(ac.copy(note = Some(displayNote))), None)

      // If the item note is nothing to do with the access condition, we return it to
      // be copied onto the item.
      case (ac, displayNote) => (ac, displayNote)
    }
  }

  private def createAccessCondition(
    holdCount: Option[Int],
    status: Option[String],
    opacmsg: Option[String],
    rulesForRequestingResult: Option[NotRequestable],
    locationTypeId: Option[String],
    itemData: SierraItemData
  ): Option[DisplayAccessCondition] =
    (holdCount, status, opacmsg, rulesForRequestingResult, locationTypeId) match {

      // Items in the closed stores that aren't prevented from being requested get
      // the "Online request" condition.
      //
      // Example: b18799966 / i17571170
      case (
        Some(0),
        Some(Status.Available),
        Some(OpacMsg.OnlineRequest),
        None,
        Some(CatalogueLocationType.ClosedStores)) =>
        Some(
          DisplayAccessCondition(
            method = CatalogueAccessMethod.OnlineRequest,
            status = CatalogueAccessStatus.Open
          )
        )

      // An item which is restricted can be requested online -- the user will have to fill in
      // any paperwork when they actually visit the library.
      //
      // Example: b29459126 / i19023340
      case (
        Some(0),
        Some(Status.Available),
        Some(OpacMsg.Restricted),
        None,
        Some(CatalogueLocationType.ClosedStores)) =>
        Some(
          DisplayAccessCondition(
            method = CatalogueAccessMethod.OnlineRequest,
            status = CatalogueAccessStatus.Restricted
          )
        )

      // If an item is on hold for another reader, it can't be requested -- even
      // if it would ordinarily be requestable.
      //
      // Note that an item on hold goes through two stages:
      //
      //  1. A reader places a hold, but the item is still in the store.
      //     The status is still "-" (Available)
      //  2. A staff member collects the item from the store, and places it on the holdshelf
      //     Then the status becomes "!" (On holdshelf)  This is reflected in the rules for requesting.
      //
      // It is possible for an item to have a non-zero hold count but still be available
      // for requesting, e.g. some of our long-lived test holds didn't get cleared properly.
      // If an item seems to be stuck on a non-zero hold count, ask somebody to check Sierra.
      case (Some(holdCount), _, _, _, Some(CatalogueLocationType.ClosedStores))
        if holdCount > 0 =>
        Some(
          DisplayAccessCondition(
            method = CatalogueAccessMethod.NotRequestable,
            status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
            note = Some(
              "Item is in use by another reader. Please ask at Library Enquiry Desk."),
            terms = None
          )
        )


      case (
        _,
        _,
        _,
        Some(NotRequestable.InUseByAnotherReader(_)),
        Some(CatalogueLocationType.ClosedStores)) =>
        Some(
          DisplayAccessCondition(
            method = CatalogueAccessMethod.NotRequestable,
            status = Some(CatalogueAccessStatus.TemporarilyUnavailable),
            note = Some(
              "Item is in use by another reader. Please ask at Library Enquiry Desk."),
            terms = None
          )
        )

      // If we can't work out how this item should be handled, then we fall back to
      // the access condition from the catalogue API.
      case (holdCount, status, opacmsg, isRequestable, location) =>
        warn(
          s"Unable to assign access status for item ${itemData.id.withCheckDigit}: " +
            s"holdCount=$holdCount, status=$status, " +
            s"opacmsg=$opacmsg, isRequestable=$isRequestable, location=$location"
        )

        None
    }

  implicit class ItemDataAccessOps(itemData: SierraItemData) {
    def status: Option[String] =
      itemData.fixedFields.get("88").map { _.value.trim }

    def opacmsg: Option[String] =
      itemData.fixedFields.get("108").map { _.value.trim }
  }

  // The display note field has been used for multiple purposes, in particular:
  //
  //  1) Distinguishing between different copies of an item, so people know
  //     which item to request, e.g. "impression lacking lettering"
  //  2) Recording information about how to access the item, e.g. "please email us"
  //
  // This method uses a few heuristics to guess whether a given note is actually information
  // about access that we should copy to the "terms" field.
  private implicit class NoteStringOps(note: String) {
    def isAccessNote: Boolean =
      containsAnyOf(
        "unavailable",
        "access",
        "please contact",
        "@wellcomecollection.org",
        "offsite",
        "shelved at"
      )

    private def containsAnyOf(substrings: String*): Boolean =
      substrings.exists(note.toLowerCase.contains(_))
  }
}
