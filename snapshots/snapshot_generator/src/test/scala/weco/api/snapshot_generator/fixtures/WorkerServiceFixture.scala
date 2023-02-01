package weco.api.snapshot_generator.fixtures

import akka.actor.ActorSystem
import org.scalatest.Suite
import weco.api.snapshot_generator.services.SnapshotGeneratorWorkerService
import weco.fixtures.TestWith
import weco.messaging.fixtures.SQS
import weco.messaging.fixtures.SQS.Queue
import weco.messaging.memory.MemoryMessageSender
import weco.messaging.sns.NotificationMessage

trait WorkerServiceFixture extends SnapshotServiceFixture with SQS {
  this: Suite =>
  def withWorkerService[R](
    queue: Queue,
    messageSender: MemoryMessageSender
  )(
    testWith: TestWith[SnapshotGeneratorWorkerService, R]
  )(implicit actorSystem: ActorSystem): R =
    withSnapshotService() { snapshotService =>
      withSQSStream[NotificationMessage, R](queue) { sqsStream =>
        val workerService = new SnapshotGeneratorWorkerService(
          snapshotService = snapshotService,
          sqsStream = sqsStream,
          messageSender = messageSender
        )

        workerService.run()

        testWith(workerService)
      }
    }
}
