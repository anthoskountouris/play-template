package controllers
import akka.util.ByteString
import baseSpec.BaseSpecWithApplication
import com.google.common.base.Predicates.equalTo
import models.DataModel.bookForm
import models.{DataModel, VolumeInfo}
import org.mongodb.scala.result
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.data.Forms.mapping
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.i18n.{DefaultMessagesApi, Messages}
import play.api.libs.Comet.initialHtmlChunk.body
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, Redirect}
import play.api.mvc.{Action, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers._
import play.api.test._
import views.html.helper.form

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._



class ApplicationControllerSpec extends BaseSpecWithApplication with Injecting {
  val TestApplicationController = new ApplicationController(
   component, repository, executionContext, service, repService)
  /*
  - This creates a test version of your controller that you can reuse for testing and
  call your new Action methods on.
  - Note component comes from the BaseSpecWithApplication, it is created as an instance
  of controller components and injected into the Controller.
   */

  private val dataModel: DataModel = DataModel (
    id = "abcd",
    volumeInfo = VolumeInfo("Game of Thrones", Some("Fiction Story"), Some(100)))

  private val dataModel2: DataModel = DataModel (
    id = "abbb",
    volumeInfo = VolumeInfo("Game of Thrones 2", Some("Fiction Story 2"), Some(200)))

  private val updatedDataModel:DataModel = dataModel.copy(volumeInfo= VolumeInfo(dataModel.volumeInfo.title, Some("Dog Sitter"), dataModel.volumeInfo.pageCount)  )

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
//      beforeEach()
      val resultFuture = TestApplicationController.index()(FakeRequest())
//      println("Result Future: " + resultFuture) // Debug: print the Future object
      val result = Await.result(resultFuture, 2.seconds).header.status // Correctly awaiting the result
//      println("HTTP Status with .status: " + result) // Debug: print the status
      result shouldBe OK
//      afterEach()
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
//    beforeEach()
    val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson(dataModel))
    val createdResult: Future[Result] = TestApplicationController.create()(request)
    val result = await(createdResult).header.status // Correctly awaiting the result
//    println(result)
    result shouldBe Status.CREATED
//    println("The result is, " + result)
//    status(createdResult) shouldBe Status.BAD_REQUEST

    // when the book with the id exists
    val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
//    println(await(readResult).header.status)
//    println(Json.toJson(dataModel))

//    println(contentAsJson(readResult))
    await(readResult).header.status shouldBe Status.OK
    contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

//    afterEach()
    }
  }

  "ApplicationController .read()" should {
    "find a book in the database by id" in {
//      beforeEach()
      val request: FakeRequest[JsValue] = buildGet("/create").withBody[JsValue](Json.toJson((dataModel)))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // when the book with the id exists
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())

      await(readResult).header.status shouldBe Status.OK
      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      // when the book id does not exist in the database
      val readResult2: Future[Result] = TestApplicationController.read("aaaa")(FakeRequest())
      await(readResult2).header.status shouldBe Status.BAD_REQUEST
//      println(await(readResult2).header.status)


//      println("content " + contentAsJson(readResult).as[JsValue])
//      println("content of dataModel "+ Json.toJson(dataModel))
//      afterEach()
    }

  }

  "ApplicationController .update()" should {
    "update the book in the database based on id" in {
//      beforeEach()
      // Creating a request for the creation of the dataModel
      val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson((dataModel)))
      // Calling the .create function in the ApplicationController
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // Calling the .read function in the ApplicationController
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      await(readResult).header.status shouldBe Status.OK
      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      // Creating a request for the updated dataModel
      val updateRequest: FakeRequest[JsValue] = FakeRequest().withBody[JsValue](Json.toJson(updatedDataModel))
      val updateResult: Future[Result] = TestApplicationController.update(dataModel.id)(updateRequest)

      val resultStatus = await(updateResult).header.status
      resultStatus shouldBe Status.ACCEPTED
      //    println(await(updateResult).header.status)
      val jsonResponse = contentAsJson(updateResult).as[JsValue]
      jsonResponse shouldBe Json.toJson(updatedDataModel)

//      afterEach()
    }
  }

  "ApplicationController .delete()" should {
    "delete the book in the database based on id" in {
//      beforeEach()
      // Creating a request for the creation of the dataModel
      val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson((dataModel)))
      // Calling the .create function in the ApplicationController
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // Calling the .read function in the ApplicationController
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      await(readResult).header.status shouldBe Status.OK
//      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      val deleteRequest: Future[Result] = TestApplicationController.delete(dataModel.id)(FakeRequest())
      val deleteResult = await(deleteRequest).header.status
//      println(deleteResult)
//      println(dataModel.toString)
      deleteResult shouldBe Status.ACCEPTED

      val deleteRequestFailed: Future[Result] = TestApplicationController.delete("2122")(FakeRequest())
      val deleteResultFailed2 = await(deleteRequestFailed).header.status
            println(deleteRequestFailed)
      deleteResultFailed2 shouldBe Status.BAD_REQUEST

//      afterEach()
    }
  }

  "ApplicationController .findByName()" should {
    "find the book in the database based on the name" in {
      // Creating a request for the creation of the dataModel
      val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson((dataModel)))
      // Calling the .create function in the ApplicationController
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // Calling the .read function in the ApplicationController
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      await(readResult).header.status shouldBe Status.OK

      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      val findByNameRequest: Future[Result] = TestApplicationController.findByName("Game of Thrones")(FakeRequest())
      val findByNameRequestResult = await(findByNameRequest).header.status

      println(findByNameRequest)
      println(dataModel.toString)

      findByNameRequestResult shouldBe Status.OK

      val findByNameRequestFailed: Future[Result] = TestApplicationController.findByName("GOT")(FakeRequest())
      await(findByNameRequestFailed).header.status shouldBe Status.BAD_REQUEST
    }
  }

  "ApplicationController .updateByField()" should {
    "update a specific filed of a book in the database based on id" in {
//      beforeEach()
      // Creating a request for the creation of the dataModel
      val request: FakeRequest[JsValue] = buildPost("/create").withBody[JsValue](Json.toJson((dataModel)))
      // Calling the .create function in the ApplicationController
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      await(createdResult).header.status shouldBe Status.CREATED

      // Calling the .read function in the ApplicationController
      val readResult: Future[Result] = TestApplicationController.read("abcd")(FakeRequest())
      await(readResult).header.status shouldBe Status.OK

      contentAsJson(readResult).as[JsValue] shouldBe Json.toJson(dataModel)

      val updateByFieldRequest: Future[Result] = TestApplicationController.updateByField("abcd", "pageCount", "300")(FakeRequest())
      println(await(updateByFieldRequest).header.status)
      await(updateByFieldRequest).header.status shouldBe Status.ACCEPTED

      val updateByFieldRequestFailed: Future[Result] = TestApplicationController.updateByField("abcd", "pageCount", "apoel")(FakeRequest())
      println(await(updateByFieldRequestFailed).header.status)
      await(updateByFieldRequestFailed).header.status shouldBe Status.BAD_REQUEST
//      afterEach()
    }
  }

//
//  override def beforeEach(): Unit = await(repository.deleteAll())
//  override def afterEach(): Unit = await(repository.deleteAll())

  "ApplicationController GET .addBook" should {
    /*
    Because it manually creates the controller, this test is highly isolated.
    It does not depend on the Play application's injector or any other part of
    the application setup. This method ensures that no external application
    configurations or modules affect the test, making it very controlled.
    his approach is useful when you want to test the controller in complete
    isolation from the rest of your application components. It allows you to
    specifically test the functionality of the controller without any interference
    or dependencies that might be configured in the application.
     */
    "render the form page from a new instance" in {
      val formPage = TestApplicationController.addBook().apply(FakeRequest(GET, "/addbook/form").withCSRFToken)
      status(formPage) mustBe OK
      contentType(formPage) mustBe Some("text/html")
      contentAsString(formPage) must include ("Add Book")
    }
    /* his method ensures that the controller is provided with all its
    required dependencies as configured in the running Play application.
    Since this test uses the application's injector, it is more of an
    integration test than a unit test.
     */
    "render the from page from the application" in {
      val controller = inject[ApplicationController]
      val formPage = controller.addBook().apply(FakeRequest(GET, "/addbook/form").withCSRFToken)
      status(formPage) mustBe OK
      contentType(formPage) mustBe Some("text/html")
      contentAsString(formPage) must include ("Add Book")

    }

    "render the form page from the router" in {
      val request = FakeRequest(GET, "/addbook/form").withCSRFToken
      val formPage = route(app, request).get
      status(formPage) mustBe OK
      contentType(formPage) mustBe Some("text/html")
      contentAsString(formPage) must include ("Add Book")
    }
  }

  "ApplicationController .addBookForm" should {
    "redirect on valid form submission" in {
      implicit val request: Request[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, "/addbook/form")
          .withFormUrlEncodedBody(
            "id" -> "1223",
            "volumeInfo.title" -> "Test",
            "volumeInfo.description" -> "Test Test",
            "volumeInfo.pageCount" -> "23")
          .withCSRFToken

      val result = TestApplicationController.addBookForm().apply(request)
      status(result) mustBe OK

//
//      def errorFunc(badForm: Form[DataModel]) = {
//        BadRequest(views.html.form(badForm))
//      }
//
//      def successFunc(formData: DataModel) = {
//        Redirect(routes.ApplicationController.read(formData.id))
//        //        ACCEPTED(Json.toJson(formData))
//      }
//
//      val result = Future.successful(DataModel.bookForm.bindFromRequest().fold(errorFunc, successFunc))
//      await(result).header.status mustBe SEE_OTHER
//      println(await(result))
    }
      "redirect BadRequest on invalid form submission (Case 1 -> id is empty)" in {
      implicit val request: Request[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, "/addbook/form")
          .withFormUrlEncodedBody(
            "id" -> "",
            "volumeInfo.title" -> "Test",
            "volumeInfo.description" -> "Test Test",
            "volumeInfo.pageCount" -> "23")
          .withCSRFToken

      val result = TestApplicationController.addBookForm().apply(request)
      status(result) mustBe BAD_REQUEST
      println(result)
    }

    "redirect BadRequest on invalid form submission (Case 2 -> pageCount not number)" in {
      implicit val request: Request[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, "/addbook/form")
          .withFormUrlEncodedBody(
            "id" -> "2121",
            "volumeInfo.title" -> "Test",
            "volumeInfo.description" -> "Test Test",
            "volumeInfo.pageCount" -> "ABCD")
          .withCSRFToken

      val result = TestApplicationController.addBookForm().apply(request)
      status(result) mustBe BAD_REQUEST
      println(result)
    }
  }
}

/*

  **** Manually testing the rest of the API ****

  CREATE (POST)
  curl -H "Content-Type: application/json"
  -d '{ "_id" : "1", "name" : "testName", "description" : "testDescription", "pageCount" : 1 }'
  "localhost:9000/create" -i

  READ (GET)
  curl localhost:9000/read/1

  UPDATE (PUT)
  curl -X PUT "http://localhost:9000/update/1" \
  -H "Content-Type: application/json" \
  -d '{ "name" : "testNameUpdated", "description" : "testDescription", "pageCount" : 11 }'

  DELETE (DELETE)
  curl -X DELETE "http://localhost:9000/delete/2"

  CALLING THE GOOGLE API -> GET AND THEN CREATE
  curl localhost:9000/library/google/isbn/1544512260
 */
