package weco.api.snapshot_generator.services

import akka.Done
import weco.api.snapshot_generator.models.SnapshotJob
import weco.json.JsonUtil._
import weco.messaging.MessageSender
import weco.messaging.sns.NotificationMessage
import weco.messaging.sqs.SQSStream
import weco.typesafe.Runnable

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
