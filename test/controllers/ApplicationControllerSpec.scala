package controllers
import akka.util.ByteString
import baseSpec.BaseSpecWithApplication
import models.DataModel
import org.mongodb.scala.result
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.libs.Comet.initialHtmlChunk.body
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Result}
import play.api.test.Helpers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class ApplicationControllerSpec extends BaseSpecWithApplication{
  val TestApplicationController = new ApplicationController(
   component, repository, executionContext)
  /*
  - This creates a test version of your controller that you can reuse for testing and
  call your new Action methods on.
  - Note component comes from the BaseSpecWithApplication, it is created as an instance
  of controller components and injected into the Controller.
   */

  private val dataModel: DataModel = DataModel (
    "abcd",
    "Anthos Kountouris",
    "Software Developer",
    100)

  private val updatedDataModel:DataModel = dataModel.copy(description="Dog Sitter")

  "ApplicationController .index" should {
    /*
    - Creates a new value result and assigns it the outcome of calling
    the function index() on the controller.
    - The FakeRequest() is needed to mimic an incoming HTTP request,
    the same as hitting the route in the browser.

    The await() function, usually accessed through scala.concurrent.Await,
    provides a way to pause the execution of the current thread until the Future is resolved.
     */
    "return 200 OK" in {
      beforeEach()
      val resultFuture = TestApplicationController.index()(FakeRequest())
//      println("Result Future: " + resultFuture) // Debug: print the Future object
      val result = Await.result(resultFuture, 2.seconds).header.status // Correctly awaiting the result
//      println("HTTP Status with .status: " + result) // Debug: print the status
      result shouldBe OK
      afterEach()
    }
    /*
    - Uses a helper method called status() to pull out the HTTP response status of calling the function
    - shouldBe is just one of the many ways of doing assertions in unit tests
    - Because Play's todoo method actually returns a 501 / NOT_IMPLEMENTED response, we can assert that the code we wrote in our controller means that a HTTP 501 is returned
    - status(result) shouldBe Status.NOT_IMPLEMENTED is the same as writing status(result) shouldBe 501

     */
  }

  "ApplicationController .create" should {
  "create a book in the database" in {
    beforeEach()
    val request: FakeRequest[JsValue] = buildPost("/api").withBody[JsValue](Json.toJson(dataModel))
    val createdResult: Future[Result] = TestApplicationController.create()(request)
    val result = await(createdResult).header.status // Correctly awaiting the result
    result shouldBe Status.CREATED
//    println("The result is, " + result)
//    status(createdResult) shouldBe Status.BAD_REQUEST

    afterEach()
    }
  }

  "ApplicationController .read()" should {
    "find a book in the database by id" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson((dataModel)))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // when the book with the id exists
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())

      await(readResult).header.status shouldBe Status.OK
      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      // when the book id does not exist in the database
      val readResult2: Future[Result] = TestApplicationController.read("aaaa")(FakeRequest())
      await(readResult2).header.status shouldBe Status.BAD_REQUEST
      println(await(readResult2).header.status)


//      println("content " + contentAsJson(readResult).as[JsValue])
//      println("content of dataModel "+ Json.toJson(dataModel))
      afterEach()
    }

  }

  "ApplicationController .update()" should {
    "update the book in the database based on id" in {
      beforeEach()
      // Creating a request for the creation of the dataModel
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson((dataModel)))
      // Calling the .create function in the ApplicationController
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // Calling the .read function in the ApplicationController
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      await(readResult).header.status shouldBe Status.OK
      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      // Creating a request for the updated dataModel
      val updateRequest: FakeRequest[JsValue] = FakeRequest().withBody[JsValue](Json.toJson(updatedDataModel))
      val updateResult: Future[Result] = TestApplicationController.update(dataModel._id)(updateRequest)

      val resultStatus = await(updateResult).header.status
      resultStatus shouldBe Status.ACCEPTED
      //    println(await(updateResult).header.status)
      val jsonResponse = contentAsJson(updateResult).as[JsValue]
      jsonResponse shouldBe Json.toJson(updatedDataModel)

      afterEach()
    }
  }

  "ApplicationController .delete()" should {
    "delete the book in the database based on id" in {
      beforeEach()
      // Creating a request for the creation of the dataModel
      val request: FakeRequest[JsValue] = buildGet("/api/${dataModel._id}").withBody[JsValue](Json.toJson((dataModel)))
      // Calling the .create function in the ApplicationController
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // Calling the .read function in the ApplicationController
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      await(readResult).header.status shouldBe Status.OK
      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      val deleteRequest: Future[Result] = TestApplicationController.delete(dataModel._id)(FakeRequest())
      val deleteResult = await(deleteRequest).header.status
//      println(deleteResult)
//      println(dataModel.toString)
      deleteResult shouldBe Status.ACCEPTED

      val deleteRequestFailed: Future[Result] = TestApplicationController.delete("2122")(FakeRequest())
      val deleteResultFailed2 = await(deleteRequestFailed).header.status
            println(deleteRequestFailed)
      deleteResultFailed2 shouldBe Status.BAD_REQUEST



      afterEach()
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())
  override def afterEach(): Unit = await(repository.deleteAll())


}
