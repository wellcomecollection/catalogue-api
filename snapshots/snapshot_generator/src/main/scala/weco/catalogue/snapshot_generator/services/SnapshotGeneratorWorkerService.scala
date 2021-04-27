package weco.catalogue.snapshot_generator.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.typesafe.Runnable
import weco.catalogue.snapshot_generator.models.SnapshotJob

import scala.concurrent.Future
import scala.util.Try

class SnapshotGeneratorWorkerService(
  snapshotService: SnapshotService,
  sqsStream: SQSStream[NotificationMessage],
  messageSender: MessageSender[_]
) extends Runnable {

  def run(): Future[Done] =
    sqsStream.foreach(
      this.getClass.getSimpleName,
      (msg: NotificationMessage) => Future.fromTry(processMessage(msg)))

  private def processMessage(message: NotificationMessage): Try[Unit] =
    for {
      snapshotJob <- fromJson[SnapshotJob](message.body)
      completedSnapshotJob <- snapshotService.generateSnapshot(snapshotJob)
      _ <- messageSender.sendT(completedSnapshotJob)
    } yield ()
}
