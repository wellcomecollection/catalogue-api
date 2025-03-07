package weco.api.items.services

import grizzled.slf4j.Logging
import weco.api.items.models.ContentApiVenue
import weco.api.stacks.models.SierraItemDataOps.ItemDataOps
import weco.api.stacks.models.{DisplayItemOps, SierraItemIdentifier}
import weco.catalogue.display_model.identifiers.DisplayIdentifierType
import weco.catalogue.display_model.locations.{
  DisplayAccessCondition,
  DisplayPhysicalLocation
}
import weco.catalogue.display_model.work.{AvailabilitySlot, DisplayItem}
import weco.sierra.http.SierraSource
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.errors.SierraItemLookupError
import weco.sierra.models.fields.{SierraItemDataEntries, SierraLocation}
import weco.sierra.models.identifiers.SierraItemNumber

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate, LocalDateTime}
import scala.concurrent.{ExecutionContext, Future}

/** Updates the AccessCondition of sierra items
  *
  *  This provides an up to date view on whether a hold
  *  can be placed on an item, and generates a list of dates
  *  when the item can be viewed in the library
  */
class SierraItemUpdater(
  sierraSource: SierraSource,
  venuesOpeningTimesLookup: VenuesOpeningTimesLookup,
  venueClock: Clock
)(implicit executionContext: ExecutionContext)
    extends ItemUpdater
    with Logging
    with DisplayItemOps {

  val identifierType: DisplayIdentifierType =
    DisplayIdentifierType.SierraSystemNumber

  private def lookupItems(
    staleItemIds: Seq[SierraItemNumber]
  ): Future[Map[SierraItemNumber, SierraItemData]] =
    sierraSource
      .lookupItemEntries(staleItemIds)
      .map { itemEither =>
        val items = itemEither match {
          case Right(SierraItemDataEntries(_, _, entries)) =>
            entries.map(entry => entry.id -> Some(entry)) toMap
          case Left(
                SierraItemLookupError.MissingItems(missingItems, itemsReturned)
              ) =>
            warn(s"Item lookup missing items: $missingItems")
            itemsReturned.map(entry => entry.id -> Some(entry)) toMap
          case Left(itemLookupError) =>
            error(s"Item lookup failed: $itemLookupError")
            Map.empty[SierraItemNumber, Option[SierraItemData]]
        }
        items collect { case (sierraItemNumber, Some(sierraItemData)) =>
          sierraItemNumber -> sierraItemData
        }
      }

  private def updateItem(
    item: DisplayItem,
    freshSierraItemsData: Map[SierraItemNumber, SierraItemData]
  ): Future[DisplayItem] =
    freshSierraItemsData.get(
      SierraItemIdentifier.fromSourceIdentifier(item.identifiers.head)
    ) match {
      case Some(freshSierraItem) =>
        val withUpdatedAccessCondition = updateAccessConditionIfExists(
          item,
          freshSierraItem.accessCondition(item.physicalLocationType)
        )
        setAvailableDates(
          withUpdatedAccessCondition,
          freshSierraItem.location
        )
      case _ => Future.successful(item)
    }

  /** Updates the AccessCondition for a single item
    *  We are interested in updating the status of an Item
    *  a library patron can request. These are items with a
    *  PhysicalLocation. In data sourced from Sierra we can
    *  only have one PhysicalLocation, so we update it if
    *  we find it.
    */
  private def updateAccessConditionIfExists(
    item: DisplayItem,
    accessCondition: Option[DisplayAccessCondition]
  ): DisplayItem =
    accessCondition match {
      case Some(accessCondition) =>
        val updatedItemLocations = item.locations.map {
          case physicalLocation: DisplayPhysicalLocation =>
            physicalLocation.copy(accessConditions = List(accessCondition))
          case location => location
        }
        item.copy(locations = updatedItemLocations)
      case None => item
    }

  /** Set availability slots for a single item
    *  - if its physicalAccessCondition exists and is requestable
    *  - based on its location
    *  If any of the above are not true/defined, we return the item without availableDates
    */
  private def setAvailableDates(
    item: DisplayItem,
    sierraItemLocation: Option[SierraLocation]
  ): Future[DisplayItem] =
    if (
      item.physicalAccessCondition.exists(
        _.isRequestable
      ) && sierraItemLocation.isDefined
    ) {
      val locationName = sierraItemLocation.get.code match {
        case "harop" | "hgboo" => "deepstore"
        case _                 => "library"
      }
      for {
        openingTimes <- getVenuesOpeningTimes(locationName)
        availableDates =
          if (openingTimes.nonEmpty) {
            locationName match {
              case "deepstore" => deepstoreItemAvailabilities(openingTimes)
              case "library"   => libraryItemAvailabilities(openingTimes)
            }
          } else {
            List.empty
          }
      } yield item.copy(availableDates = Some(availableDates))
    } else {
      Future.successful(item)
    }

  private def getVenuesOpeningTimes(
    locationName: String
  ): Future[Map[String, List[AvailabilitySlot]]] =
    venuesOpeningTimesLookup
      .byVenueName(locationName)
      .map(venuesEither => venuesEitherToAvailabilitySlotsMap(venuesEither))

  private def venuesEitherToAvailabilitySlotsMap(
    venuesEither: Either[VenueOpeningTimesLookupError, List[ContentApiVenue]]
  ): Map[String, List[AvailabilitySlot]] =
    venuesEither.toSeq
      .flatMap(venuesList =>
        venuesList
          .map(venue =>
            venue.title.toLowerCase() -> venue.openingTimes.map(openingTime =>
              AvailabilitySlot(openingTime.open, openingTime.close)
            )
          )
      ) toMap

  private def libraryItemAvailabilities(
    venuesOpeningTimes: Map[String, List[AvailabilitySlot]]
  ): List[AvailabilitySlot] = {
    val timeAtVenue = LocalDateTime.now(venueClock)
    val isWorkingDay = timeAtVenue.toLocalDate.isEqual(
      parseISOStringToLocalDate(venuesOpeningTimes("library").head.from)
    )
    if (timeAtVenue.getHour < 10 | !isWorkingDay) {
      venuesOpeningTimes("library").drop(1)
    } else {
      venuesOpeningTimes("library").drop(2)
    }
  }

  private def deepstoreItemAvailabilities(
    venuesOpeningTimes: Map[String, List[AvailabilitySlot]]
  ): List[AvailabilitySlot] = {
    // it take deepstore 10 working days to deliver the item to the library
    val firstDeepstoreAvailabilitySlot = venuesOpeningTimes(
      "deepstore"
    ).drop(10).head
    // the item is then available on subsequent library opening days
    venuesOpeningTimes(
      "library"
    ).filter(openingTime =>
      parseISOStringToLocalDate(openingTime.from)
        .isAfter(
          parseISOStringToLocalDate(firstDeepstoreAvailabilitySlot.from)
        )
    )
  }

  def updateItems(items: Seq[DisplayItem]): Future[Seq[DisplayItem]] = {
    val staleItemIds = items
      .filter(item => item.isStale)
      .map(item =>
        SierraItemIdentifier.fromSourceIdentifier(item.identifiers.head)
      )

    staleItemIds.size match {
      case 0 => Future.successful(items)
      case _ =>
        debug(s"Refreshing stale items $staleItemIds")
        for {
          sierraItemsData <- lookupItems(staleItemIds)
          updatedItems <- Future.sequence(
            items.map(updateItem(_, sierraItemsData))
          )
        } yield updatedItems
    }
  }

  private def parseISOStringToLocalDate(isoString: String): LocalDate =
    LocalDate.parse(
      isoString,
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    )
}
