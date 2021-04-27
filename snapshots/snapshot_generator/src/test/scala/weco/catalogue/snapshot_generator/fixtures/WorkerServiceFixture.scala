package weco.catalogue.snapshot_generator.fixtures

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.Index
import org.scalatest.Suite
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import weco.catalogue.snapshot_generator.services.SnapshotGeneratorWorkerService

trait WorkerServiceFixture extends SnapshotServiceFixture with SQS {
  this: Suite =>
  def withWorkerService[R](
    queue: Queue,
    messageSender: MemoryMessageSender,
    worksIndex: Index)(testWith: TestWith[SnapshotGeneratorWorkerService, R])(
                            implicit actorSystem: ActorSystem): R =
    withSnapshotService(worksIndex) { snapshotService =>
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
