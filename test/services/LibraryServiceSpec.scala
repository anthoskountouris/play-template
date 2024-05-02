package services

import baseSpec.BaseSpec
import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsValue, Json, OFormat}

import scala.concurrent.ExecutionContext

class LibraryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite{
  val mockConnector: LibraryConnector = mock[LibraryConnector]
  implicit val executionContext:ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val testService = new LibraryService(mockConnector)

  val gameOfThrones: JsValue = Json.obj(
    "id" -> "someId",
    "title" -> "A Game of Thrones",
    "description" -> "The best book!!!",
    "pageCount" -> 100
  )

  "getGoogleBook" should {
    val url: String = "testUrl"

    "return a book" in {
      (mockConnector.get[Book](_:String)(_: OFormat[Book], _: ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.rightT(gameOfThrones.as[Book]))
        .once()

      whenReady(testService.getGoogleBook(urlOverride = Some(url), search = "", term = "").value) { result =>
        result shouldBe Right(gameOfThrones.as[Book])
      }
      /*
      Since we are expecting a future from the connector, it's best to use the whenReady
      method from the test helpers, this allows for the result to be waited for as the
      Future type can be seen as a placeholder for a value we don't have yet.
      */
    }

    "return an error" in {
      val url: String = "testurl"

      (mockConnector.get[Book]( _:String)( _:OFormat[Book], _:ExecutionContext))
        .expects(url, *, *)
        .returning(EitherT.leftT(APIError.BadAPIResponse(500, "Could not connect")))
        .once()

      whenReady(testService.getGoogleBook(urlOverride = Some(url), search="", term="").value) {
        result => result shouldBe Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }

}
