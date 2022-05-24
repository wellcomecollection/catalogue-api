package weco.api.requests.services

import akka.http.scaladsl.model.{HttpResponse, Uri}
import io.circe.Json
import io.circe.parser._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.api.requests.fixtures.ItemLookupFixture
import weco.api.requests.models.RequestedItemWithWork
import weco.catalogue.display_model.identifiers.{
  DisplayIdentifier,
  DisplayIdentifierType
}
import weco.catalogue.display_model.work.DisplayItem
import weco.catalogue.internal_model.identifiers.CanonicalId
import weco.fixtures.RandomGenerators
import weco.http.client.{HttpGet, MemoryHttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ItemLookupTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with ScalaFutures
    with IntegrationPatience
    with ItemLookupFixture
    with RandomGenerators {

  describe("byCanonicalId") {
    it("finds a work with the same item ID") {
      val item1 = IdentifiedItemStub()
      val item2 = IdentifiedItemStub()
      val item3 = IdentifiedItemStub()

      val workA = WorkStub(items = List(item1, item2))
      val workB = WorkStub(items = List(item2, item3))

      val responses = Seq(
        (
          catalogueItemRequest(item1.id),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueItemRequest(item2.id),
          catalogueWorkResponse(Seq(workA, workB))
        ),
        (
          catalogueItemRequest(item3.id),
          catalogueWorkResponse(Seq(workB))
        )
      )

      withItemLookup(responses) { lookup =>
        Seq(item1, item2, item3).foreach { item =>
          val future =
            lookup.byCanonicalId(item.id)

          whenReady(future) {
            _ shouldBe Right(
              DisplayItem(
                id = Some(item.id.underlying),
                identifiers =
                  (item.sourceIdentifier +: item.otherIdentifiers).toList,
                locations = List()
              )
            )
          }
        }
      }
    }

    it("returns a NotFoundError if there is no such work") {
      val id = createCanonicalId

      val responses = Seq(
        (
          catalogueItemRequest(id),
          catalogueWorkResponse(Seq())
        )
      )

      val future = withItemLookup(responses) {
        _.byCanonicalId(id)
      }

      whenReady(future) {
        _.left.value shouldBe a[ItemNotFoundError]
      }
    }

    it("returns an error if the API has an error") {
      val brokenClient = new MemoryHttpClient(responses = Seq()) with HttpGet {
        override val baseUri: Uri = Uri("http://catalogue:9001")

        override def get(
          path: Uri.Path,
          params: Map[String, String]
        ): Future[HttpResponse] =
          Future.failed(new Throwable("BOOM!"))
      }

      val future = withActorSystem { implicit actorSystem =>
        val lookup = new ItemLookup(brokenClient)

        lookup.byCanonicalId(createCanonicalId)
      }

      whenReady(future) {
        _.left.value shouldBe a[UnknownItemError]
      }
    }
  }

  describe("bySourceIdentifier") {
    it("finds items by source identifier") {
      val item1 = IdentifiedItemStub()
      val item2 = IdentifiedItemStub()
      val item3 = IdentifiedItemStub()

      val workA = WorkStub(sourceIdentifier = "A", items = List(item1, item2))
      val workB = WorkStub(sourceIdentifier = "B", items = List(item2, item3))

      val responses = Seq(
        (
          catalogueSourceIdsRequest(
            item1.sourceIdentifier,
            item2.sourceIdentifier
          ),
          catalogueWorkResponse(Seq(workA, workB))
        )
      )

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(
          Seq(
            item1.sourceIdentifier,
            item2.sourceIdentifier
          )
        )
      }

      whenReady(future) {
        _ shouldBe List(
          Right(
            RequestedItemWithWork(
              workId = workA.id,
              workTitle = workA.title,
              item = itemAsJson(item1)
            )
          ),
          Right(
            RequestedItemWithWork(
              workId = workA.id,
              workTitle = workA.title,
              item = itemAsJson(item2)
            )
          )
        )
      }
    }

    it("handles a work where some items have no identifiers") {
      val item1 = IdentifiedItemStub()
      val item2 = IdentifiedItemStub()
      val item3 = UnidentifiedItemStub()

      val workA = WorkStub(sourceIdentifier = "A", items = List(item1, item2))
      val workB = WorkStub(sourceIdentifier = "B", items = List(item2, item3))

      val responses = Seq(
        (
          catalogueSourceIdsRequest(
            item1.sourceIdentifier,
            item2.sourceIdentifier
          ),
          catalogueWorkResponse(Seq(workA, workB))
        )
      )

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(
          Seq(
            item1.sourceIdentifier,
            item2.sourceIdentifier
          )
        )
      }

      whenReady(future) {
        _ shouldBe List(
          Right(
            RequestedItemWithWork(
              workId = workA.id,
              workTitle = workA.title,
              item = itemAsJson(item1)
            )
          ),
          Right(
            RequestedItemWithWork(
              workId = workA.id,
              workTitle = workA.title,
              item = itemAsJson(item2)
            )
          )
        )
      }
    }

    it("returns not found errors where ID matches cannot be found") {
      val item1 = IdentifiedItemStub()
      val item2 = IdentifiedItemStub()
      val item3 = IdentifiedItemStub()
      val item4 = IdentifiedItemStub()

      val workA = WorkStub(sourceIdentifier = "A", items = List(item1, item2))
      val workB = WorkStub(sourceIdentifier = "B", items = List(item2, item3))

      val responses = Seq(
        (
          catalogueSourceIdsRequest(
            item1.sourceIdentifier,
            item4.sourceIdentifier,
            item3.sourceIdentifier
          ),
          catalogueWorkResponse(Seq(workA, workB))
        )
      )

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(
          Seq(
            item1.sourceIdentifier,
            item4.sourceIdentifier,
            item3.sourceIdentifier
          )
        )
      }

      whenReady(future) { result =>
        result should have size 3

        result(0).value shouldBe RequestedItemWithWork(
          workId = workA.id,
          workTitle = workA.title,
          item = itemAsJson(item1)
        )
        result(1).left.value shouldBe a[ItemNotFoundError]
        result(2).value shouldBe RequestedItemWithWork(
          workId = workB.id,
          workTitle = workB.title,
          item = itemAsJson(item3)
        )
      }
    }

    it("chooses work details based on work source id ordering") {
      val item1 = IdentifiedItemStub()
      val item2 = IdentifiedItemStub()
      val item3 = IdentifiedItemStub()

      val workA = WorkStub(sourceIdentifier = "A", items = List(item1, item2))
      val workB = WorkStub(sourceIdentifier = "B", items = List(item2, item3))

      val responses = Seq(
        (
          catalogueSourceIdsRequest(item1.sourceIdentifier),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueSourceIdsRequest(item2.sourceIdentifier),
          catalogueWorkResponse(Seq(workB, workA))
        ),
        (
          catalogueSourceIdsRequest(item3.sourceIdentifier),
          catalogueWorkResponse(Seq(workB))
        )
      )

      withItemLookup(responses) { lookup =>
        Seq((workA, item1), (workA, item2), (workB, item3)).foreach {
          case (work, item) =>
            val future =
              lookup.bySourceIdentifier(Seq(item.sourceIdentifier))

            whenReady(future) {
              _ shouldBe List(
                Right(
                  RequestedItemWithWork(
                    workId = work.id,
                    workTitle = work.title,
                    item = itemAsJson(item)
                  )
                )
              )
            }
        }
      }
    }

    it("only matches on the first identifier") {
      val item1 = IdentifiedItemStub(
        otherIdentifiers = List(createSourceIdentifier)
      )
      val item2 = IdentifiedItemStub(
        otherIdentifiers = List(createSourceIdentifier)
      )
      val item3 = IdentifiedItemStub(
        otherIdentifiers = List(createSourceIdentifier)
      )

      val workA = WorkStub(sourceIdentifier = "A", items = List(item1, item2))
      val workB = WorkStub(sourceIdentifier = "B", items = List(item2, item3))

      val responses = Seq(
        (
          catalogueSourceIdsRequest(item1.sourceIdentifier),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueSourceIdsRequest(item1.otherIdentifiers.head),
          catalogueWorkResponse(Seq(workA))
        ),
        (
          catalogueSourceIdsRequest(item2.sourceIdentifier),
          catalogueWorkResponse(Seq(workA, workB))
        ),
        (
          catalogueSourceIdsRequest(item2.otherIdentifiers.head),
          catalogueWorkResponse(Seq(workA, workB))
        ),
        (
          catalogueSourceIdsRequest(item3.sourceIdentifier),
          catalogueWorkResponse(Seq(workB))
        ),
        (
          catalogueSourceIdsRequest(item3.otherIdentifiers.head),
          catalogueWorkResponse(Seq(workB))
        )
      )

      withItemLookup(responses) { lookup =>
        List((workA, item1), (workA, item2), (workB, item3)).foreach {
          case (work, item) =>
            whenReady(lookup.bySourceIdentifier(List(item.sourceIdentifier))) {
              _ shouldBe List(
                Right(
                  RequestedItemWithWork(
                    workId = work.id,
                    workTitle = work.title,
                    item = itemAsJson(item)
                  )
                )
              )
            }

            whenReady(
              lookup.bySourceIdentifier(List(item.otherIdentifiers.head))
            ) { result =>
              result should have size 1
              result.head.left.value shouldBe a[ItemNotFoundError]
            }
        }
      }
    }

    it("returns an error if the API lookup fails") {
      val brokenClient = new MemoryHttpClient(responses = Seq()) with HttpGet {
        override val baseUri: Uri = Uri("http://catalogue:9001")

        override def get(
          path: Uri.Path,
          params: Map[String, String]
        ): Future[HttpResponse] =
          Future.failed(new Throwable("BOOM!"))
      }

      val future = withActorSystem { implicit actorSystem =>
        val lookup = new ItemLookup(brokenClient)

        lookup.bySourceIdentifier(Seq(createSourceIdentifier))
      }

      whenReady(future) { err =>
        err should have size 1
        err.head.left.value shouldBe a[UnknownItemError]
      }
    }

    it("can fetch an empty list of items") {
      val responses = Seq()

      val future = withItemLookup(responses) {
        _.bySourceIdentifier(itemIdentifiers = Seq())
      }

      whenReady(future) {
        _ shouldBe empty
      }
    }
  }

  private def createSourceIdentifier: DisplayIdentifier =
    DisplayIdentifier(
      identifierType = DisplayIdentifierType(
        id = "miro-image-number",
        label = "Miro image number"
      ),
      value = randomAlphanumeric(length = 10)
    )

  private def createCanonicalId: CanonicalId =
    CanonicalId(randomAlphanumeric(length = 8))

  sealed trait ItemStub
  case class IdentifiedItemStub(
    id: CanonicalId = createCanonicalId,
    sourceIdentifier: DisplayIdentifier = createSourceIdentifier,
    otherIdentifiers: Seq[DisplayIdentifier] = List()
  ) extends ItemStub
  case class UnidentifiedItemStub() extends ItemStub

  case class WorkStub(
    id: CanonicalId = createCanonicalId,
    items: Seq[ItemStub],
    sourceIdentifier: String = randomAlphanumeric(),
    title: Option[String] = Some(s"title-${randomAlphanumeric()}")
  )

  private def catalogueWorkResponse(works: Seq[WorkStub]): HttpResponse =
    HttpResponse(
      entity = createJsonHttpEntityWith(
        s"""
           |{
           |  "results": [ ${works.map(catalogueWorkJson).mkString(",")} ]
           |}
           |""".stripMargin
      )
    )

  private def itemAsJson(item: ItemStub): Json =
    item match {
      case IdentifiedItemStub(id, sourceIdentifier, otherIdentifiers) => {
        val identifiers =
          (sourceIdentifier +: otherIdentifiers).map(sourceIdentifier => s"""
             |{
             |  "identifierType": {
             |    "id": "${sourceIdentifier.identifierType.id}",
             |    "label": "${sourceIdentifier.identifierType.label}",
             |    "type": "IdentifierType"
             |  },
             |  "value": "${sourceIdentifier.value}",
             |  "type": "Identifier"
             |}
             |""".stripMargin)

        parse(
          s"""
             |{
             |  "id": "$id",
             |  "identifiers": [
             |    ${identifiers.mkString(",")}
             |  ],
             |  "locations": [],
             |  "type": "Item"
             |}
             |""".stripMargin
        ).right.get
      }

      case UnidentifiedItemStub() =>
        parse(
          s"""
             |{
             |  "identifiers": [],
             |  "locations": [],
             |  "type": "Item"
             |}
             |""".stripMargin
        ).right.get
    }

  private def catalogueWorkJson(w: WorkStub): String = {
    val itemJson = w.items.map(itemAsJson(_).noSpaces)

    s"""
       |{
       |  "id": "${w.id}",
       |  "title": "${w.title.get}",
       |  "identifiers": [
       |    {
       |      "identifierType": {
       |        "id": "sierra-system-number",
       |        "label": "Sierra system number",
       |        "type": "IdentifierType"
       |      },
       |      "value": "${w.sourceIdentifier}",
       |      "type": "Identifier"
       |    }
       |  ],
       |  "items": [ ${itemJson.mkString(",")} ]
       |}
       |""".stripMargin
  }
}
