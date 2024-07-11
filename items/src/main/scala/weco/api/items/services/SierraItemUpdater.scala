package weco.api.items.services

import grizzled.slf4j.Logging
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
  *
  */
class SierraItemUpdater(
  sierraSource: SierraSource,
  venuesOpeningTimesLookup: VenuesOpeningTimesLookup,
  venueClock: Clock
)(
  implicit executionContext: ExecutionContext
) extends ItemUpdater
    with Logging
    with DisplayItemOps {

  val identifierType: DisplayIdentifierType =
    DisplayIdentifierType.SierraSystemNumber

  private def lookupItems(
    staleItemIds: Seq[SierraItemNumber]
  ): Future[Map[SierraItemNumber, SierraItemData]] = {
    val itemsEither = sierraSource.lookupItemEntries(staleItemIds)

    itemsEither.map(itemEither => {
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
      items collect {
        case (sierraItemNumber, Some(sierraItemData)) =>
          sierraItemNumber -> sierraItemData
      }
    })
  }

  private def updateItem(
    item: DisplayItem,
    freshSierraItemsData: Future[Map[SierraItemNumber, SierraItemData]]
  ): Future[DisplayItem] =
    for {
      freshSierraItemData <- freshSierraItemsData
      updatedItem: DisplayItem = freshSierraItemData.get(
        SierraItemNumber(item.id)
      ) match {
        case Some(freshSierraItem) =>
          val freshAccessCondition: DisplayAccessCondition =
            freshSierraItem.accessCondition(item.physicalLocationType)
          val withUpdatedAccessCondition =
            updateAccessCondition(item, freshAccessCondition)
          setAvailableDates(
            withUpdatedAccessCondition,
            freshSierraItem.location
          )
        case _ => item
      }
    } yield updatedItem

  /** Updates the AccessCondition for a single item
    *
    *  We are interested in updating the status of an Item
    *  a library patron can request. These are items with a
    *  PhysicalLocation. In data sourced from Sierra we can
    *  only have one PhysicalLocation, so we update it if
    *  we find it.
    *
    */
  def updateAccessCondition(
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

  def setAvailableDates(
    item: DisplayItem,
    sierraItemLocation: SierraLocation
  ): Future[DisplayItem] = {
    // there is only ever one location per physicalItem and one accessCondition per location,
    // but this may sometimes be empty if it could not be fetched.
    val accessCondition = item.locations.headOption
      .flatMap(_.accessConditions.headOption)

    val locationName = sierraItemLocation.code match {
      case "harop" => "deepstore"
      case _       => "library"
    }

    if (accessCondition.exists(_.isRequestable)) {
      locationName match {
        case "deepstore" =>
          deepstoreItemAvailabilities(getVenuesOpeningTimes(locationName)).map(
            availableDates =>
              item.copy(
                availableDates = Some(availableDates)
              )
          )
        case "library" =>
          libraryItemAvailabilities(getVenuesOpeningTimes(locationName)).map(
            availableDates =>
              item.copy(
                availableDates = Some(availableDates)
              )
          )
      }
    } else {
      Future.successful(item)
    }
  }

  private def getVenuesOpeningTimes(
    locationName: String
  ): Future[Map[String, List[AvailabilitySlot]]] =
    for {
      venues <- venuesOpeningTimesLookup
        .byVenueName(locationName)

      venuesOpeningTimes = venues match {
        case Right(venues) =>
          venues
            .map(
              venue =>
                venue.title -> venue.openingTimes.map(
                  openingTime =>
                    AvailabilitySlot(openingTime.open, openingTime.close)
              )
          ) toMap
        case Left(venueOpeningTimesLookupError) =>
          error(
            s"Venue opening times lookup failed: $venueOpeningTimesLookupError"
          )
          Map.empty[String, List[AvailabilitySlot]]
      }
    } yield venuesOpeningTimes

  private def libraryItemAvailabilities(
    venuesOpeningTimes: Future[Map[String, List[AvailabilitySlot]]]
  ): Future[List[AvailabilitySlot]] =
    for {
      venuesOpeningTimes <- venuesOpeningTimes

      timeAtVenue = LocalDateTime.now(venueClock)
      isWorkingDay = timeAtVenue.toLocalDate.isEqual(
        LocalDate.parse(
          venuesOpeningTimes("library").head.from,
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        )
      )
      availabilitySlots = if (timeAtVenue.getHour < 10 | !isWorkingDay) {
        venuesOpeningTimes("library").drop(1)
      } else {
        venuesOpeningTimes("library").drop(2)
      }

    } yield availabilitySlots

  private def deepstoreItemAvailabilities(
    venuesOpeningTimes: Future[Map[String, List[AvailabilitySlot]]]
  ): Future[List[AvailabilitySlot]] =
    for {
      venuesOpeningTimes <- venuesOpeningTimes

      firstAvailabilitySlot = venuesOpeningTimes(
        "deepstore"
      ).head
      subsequentLibraryAvailabilitySlots = venuesOpeningTimes(
        "library"
      ).filter(
        openingTime => openingTime.from > firstAvailabilitySlot.from
      )

    } yield firstAvailabilitySlot :: subsequentLibraryAvailabilitySlots

  def updateItems(items: Seq[DisplayItem]): Future[Seq[DisplayItem]] = {
    val staleItemIds = items
      .filter(item => item.isStale)
      .map(
        item => SierraItemIdentifier.fromSourceIdentifier(item.identifiers.head)
      )

    debug(
      s"Refreshing stale items $staleItemIds"
    )

    val freshSierraItemsData = lookupItems(staleItemIds)

    Future.sequence(
      items.map(item => updateItem(item, freshSierraItemsData))
    )
  }
}
