package weco.api.requests.services

import grizzled.slf4j.Logging
import weco.api.stacks.http.{SierraItemDataEntries, SierraSource}
import weco.api.stacks.models.{SierraErrorCode, SierraHoldsList, SierraItemIdentifier, StacksUserHolds}
import weco.api.stacks.services.ItemLookup
import weco.sierra.models.data.SierraItemData
import weco.sierra.models.identifiers.SierraPatronNumber

import scala.concurrent.{ExecutionContext, Future}

// TODO: Use in lookupRequests

class SierraRequestsService(
                             sierraSource: SierraSource,
                             itemLookup: ItemLookup,
                             holdLimit: Int = 10
                           )(implicit executionContext: ExecutionContext) extends Logging {
  def getSierraHolds(
                          patronNumber: SierraPatronNumber
                        ) = {

    for {
      holds <- sierraSource
        .listHolds(patronNumber)
        .flatMap {
          case Right(holds) => Future(holds)
          case Left(err) => Future.failed(new Throwable(s"$err"))
        }

      sourceIdentifiers = holds.entries.map { hold =>
        SierraItemIdentifier.toSourceIdentifier(
          SierraItemIdentifier.fromUrl(hold.record)
        )
      }

      holdsMap = sourceIdentifiers.zip(holds.entries).toMap

      itemLookupResults <- Future.sequence(
        sourceIdentifiers.map(itemLookup.bySourceIdentifier)
        .map(_.map {
          case Right(item) => Some(item)
          case Left(err) =>
            warn(s"$err")
            None
        })
        .map(_.recover {
          case err =>
            warn(s"$err")
            None
        })
      ).map(
        _.flatten
      )

      itemHoldTuples = itemLookupResults.flatMap { item =>
        holdsMap.get(item.id.sourceIdentifier).map { hold =>
          (hold, item)
        }
      }

    } yield itemHoldTuples
  }
}
