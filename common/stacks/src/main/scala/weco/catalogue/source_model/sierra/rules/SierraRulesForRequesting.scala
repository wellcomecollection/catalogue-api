package weco.catalogue.source_model.sierra.rules

import weco.sierra.models.data.SierraItemData

/** The Rules for Requesting are a set of rules in Sierra that can block an item
  * from being requested, and if so, optionally explain to the user why an item
  * can't be requested.
  *
  * The catalogue pipeline contains our canonical implementation of these rules,
  * and the front-end will prevent users from requesting the majority of items
  * that are blocked by these rules (anything that's long-term unavailable).
  *
  * This is a simplified implementation of these rules, to cover cases where an item
  * has only just become unrequestable, e.g. if two users try to request the
  * same item at the same time.
  *
  * If it returns None, that means we couldn't make a positive determination about
  * whether this item was requestable, rather than knowing it's definitely requestable.
  *
  * We remove rules that would end in a "definitely not requestable" state, because
  * those items should be filtered out by the front end.
  */
object SierraRulesForRequesting {
  def apply(itemData: SierraItemData): Option[NotRequestable] =
    itemData match {

      // These cases cover the lines:
      //
      //    v|i||87||~|0||
      //    v|i|8|||e|||
      //    q|i||88||=|!||Item is in use by another reader. Please ask at Enquiry Desk.
      //
      // How they work:
      //
      //    v|i||87||~|0||      # If fixed field 87 (loan rule) is not-equal to zero OR
      //    v|i|8|||e|||        # If variable field with tag 8 exists OR
      //    q|i||88||=|!||      # If fixed field 88 (status) equals '!'
      //
      // Notes:
      //    - Some items are missing fixed field 87 but are requestable using Encore.
      //      The Sierra API docs suggest the default loan rule is '0', so I'm assuming
      //      a missing FF87 doesn't block requesting.
      //    - I haven't found an example of an item with tag 8, so I'm skipping that rule
      //      for now.  TODO: Find an example of this.
      //
      case i
          if i.fixedField("87").getOrElse("0") != "0" || i
            .fixedField("88")
            .contains("!") =>
        Some(
          NotRequestable.InUseByAnotherReader(
            "Item is in use by another reader. Please ask at Enquiry Desk."
          )
        )

      case _ => None
    }

  private implicit class ItemDataOps(itemData: SierraItemData) {
    def fixedField(code: String): Option[String] =
      itemData.fixedFields.get(code).map(_.value.trim)
  }
}
