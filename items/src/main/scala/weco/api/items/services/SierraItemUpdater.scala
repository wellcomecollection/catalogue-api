package weco.api.items.services

import grizzled.slf4j.Logging
import weco.api.stacks.models.{DisplayItemOps, SierraItemIdentifier}
import weco.catalogue.display_model.identifiers.DisplayIdentifierType
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayLocationType,
  DisplayPhysicalLocation
}
import weco.catalogue.display_model.work.{AvailabilitySlot, DisplayItem}
import weco.sierra.http.SierraSource
import weco.sierra.models.errors.SierraItemLookupError
import weco.sierra.models.fields.SierraItemDataEntries
import weco.sierra.models.identifiers.SierraItemNumber

import java.time.{Clock, LocalDateTime, ZonedDateTime}
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

/** Updates the AccessCondition of sierra items
  *
  *  This provides an up to date view on whether a hold
  *  can be placed on an item.
  *
  */
class SierraItemUpdater(
  sierraSource: SierraSource,
  venueOpeningTimesLookup: VenueOpeningTimesLookup,
  clock: Clock
)(
  implicit executionContext: ExecutionContext
) extends ItemUpdater
    with Logging
    with DisplayItemOps {

  import weco.api.stacks.models.SierraItemDataOps._

  val identifierType = DisplayIdentifierType.SierraSystemNumber

  /** Updates the AccessCondition for a single item
    *
    *  We are interested in updating the status of an Item
    *  a library patron can request. These are items with a
    *  PhysicalLocation. In data sourced from Sierra we can
    *  only have one PhysicalLocation, so we update it if
    *  we find it.
    *
    */
  private def updateAccessCondition(
    item: DisplayItem,
    accessCondition: DisplayAccessCondition
  ): DisplayItem = {
    val updatedItemLocations = item.locations.map {
      case physicalLocation: DisplayPhysicalLocation =>
        physicalLocation.copy(accessConditions = List(accessCondition))
      case location => location
    }

    item.copy(locations = updatedItemLocations)
  }

  private def getAccessConditions(
    existingItems: Map[SierraItemNumber, Option[DisplayLocationType]]
  ): Future[Map[SierraItemNumber, DisplayAccessCondition]] =
    for {
      itemEither <- sierraSource.lookupItemEntries(existingItems.keys.toSeq)

      maybeAccessConditions: Map[SierraItemNumber, Option[
        DisplayAccessCondition
      ]] = itemEither match {
        case Right(SierraItemDataEntries(_, _, entries)) =>
          entries
            .map(item => {
              val location = existingItems.get(item.id).flatten
              item.id -> item.accessCondition(location)
            })
            .toMap
        case Left(
            SierraItemLookupError.MissingItems(missingItems, itemsReturned)
            ) =>
          warn(s"Item lookup missing items: $missingItems")
          itemsReturned
            .map(item => {
              val location = existingItems.get(item.id).flatten
              item.id -> item.accessCondition(location)
            })
            .toMap
        case Left(itemLookupError) =>
          error(s"Item lookup failed: $itemLookupError")
          Map.empty[SierraItemNumber, Option[DisplayAccessCondition]]
      }

      accessConditions = maybeAccessConditions
        .collect { case (itemId, Some(ac)) => itemId -> ac }

    } yield accessConditions

  private def setAvailableDates(
    item: DisplayItem
  ): Future[DisplayItem] = {
    // there is only ever one location per physicalItem and one accessCondition per location,
    // but this may sometimes be empty if it could not be fetched.
    val accessCondition = item.locations.headOption
      .flatMap(_.accessConditions.headOption)

    if (accessCondition.exists(_.isRequestable)) {
      getAvailableDates(accessCondition.head)
        .map(availableDates => item.copy(availableDates = Some(availableDates)))
    } else {
      Future.successful(item)
    }
  }

  private def getAvailableDates(
    accessCondition: DisplayAccessCondition
  ): Future[List[AvailabilitySlot]] = {
    val venueFromDisplayAccessCondition = Map(
      "online-request" -> "library"
      // other venue to be added as DisplayAccessMethod id -> content-api venue title
    )

    val timeAtVenue = LocalDateTime.now(clock)
    val leadTimeInDays = accessCondition.method.id match {
      case "online-request" if timeAtVenue.getHour < 10  => 1
      case "online-request" if timeAtVenue.getHour >= 10 => 2
    }
    def daysAwayFromNow(slot: AvailabilitySlot): Int = {
      val openingTime = ZonedDateTime.parse(slot.to)
      ChronoUnit.DAYS.between(timeAtVenue, openingTime).toInt
    }

    venueOpeningTimesLookup
      .byVenueName(
        venueFromDisplayAccessCondition.get(accessCondition.method.id)
      )
      .map {
        case Right(venue) =>
          venue.openingTimes
            .map(
              openClose => AvailabilitySlot(openClose.open, openClose.close)
            )
            // the list of AvailabilitySlots, as returned from VenueOpeningTimesLookup, starts at "today"
            // however, it takes ${leadTimeInDays} days for the item to be fetched from stores
            // so we need to drop slots that are less than ${leadTimeInDays} days away from today
            .dropWhile(slot => daysAwayFromNow(slot) < leadTimeInDays)
        case Left(venueOpeningTimesLookupError) =>
          error(
            s"Venue opening times lookup failed: $venueOpeningTimesLookupError"
          )
          List.empty
      }
  }

  def updateItems(items: Seq[DisplayItem]): Future[Seq[DisplayItem]] = {
    // item number -> item
    val itemMap = items.map { item =>
      SierraItemIdentifier.fromSourceIdentifier(item.identifiers.head) -> item
    } toMap

    val staleItems = itemMap
      .filter { case (_, item) => item.isStale }
      .map { case (itemId, item) => itemId -> item.physicalLocationType }

    debug(
      s"Asked to update items ${itemMap.keySet}, refreshing stale items ${staleItems.keySet}"
    )

    val allItems = for {
      accessConditions <- staleItems.size match {
        case 0 =>
          Future.successful(Map.empty[SierraItemNumber, DisplayAccessCondition])
        case _ => getAccessConditions(staleItems)
      }

      updatedItems = itemMap
        .map {
          case (sierraId, item) =>
            accessConditions.get(sierraId) match {
              case Some(updatedAc) => updateAccessCondition(item, updatedAc)
              case None            => item
            }
        }
        .map(setAvailableDates)

    } yield Future.sequence(updatedItems.toSeq)
    allItems.flatten
  }
}
