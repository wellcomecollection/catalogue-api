package weco.api.stacks.models

import java.time.LocalDate

/** Days on which readers cannot collect requested items, even though the
  * library is open. Days when the library is closed come from the venue
  * opening times in the Content API and don't need listing here.
  *
  * The items API stops offering these dates and the requests API rejects
  * them. Both services are redeployed when this is merged. Add a date as
  * LocalDate.of(2026, 10, 13) and remove it once it has passed.
  */
object BlockedCollectionDates {
  val dates: Set[LocalDate] = Set()
}
