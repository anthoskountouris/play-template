package controllers
import baseSpec.BaseSpecWithApplication
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.mvc.Results
import play.api.test.Helpers._

import scala.concurrent.Await
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

  "ApplicationController .index" should {
    /*
    - Creates a new value result and assigns it the outcome of calling
    the function index() on the controller.
    - The FakeRequest() is needed to mimic an incoming HTTP request,
    the same as hitting the route in the browser.
     */
    "return 200 OK" in {
      val resultFuture = TestApplicationController.index()(FakeRequest())
      println("Result Future: " + resultFuture) // Debug: print the Future object
      val result = Await.result(resultFuture, 2.seconds).header.status // Correctly awaiting the result
//      println("HTTP Status with .status: " + result.header.status) // Debug: print the status
      result shouldBe OK
    }
    /*
    - Uses a helper method called status() to pull out the HTTP response status of calling the function
    - shouldBe is just one of the many ways of doing assertions in unit tests
    - Because Play's todoo method actually returns a 501 / NOT_IMPLEMENTED response, we can assert that the code we wrote in our controller means that a HTTP 501 is returned
    - status(result) shouldBe Status.NOT_IMPLEMENTED is the same as writing status(result) shouldBe 501

     */
  }

  "ApplicationController .create" should {

  }

  "ApplicationController .read()" should {

  }

  "ApplicationController .update()" should {

  }

  "ApplicationController .delete()" should {

  }



}
