package controllers
import baseSpec.BaseSpecWithApplication // this contains many test helpers for writing BDD, doing assertions etc

class ApplicationControllerSpec extends BaseSpecWithApplication{
  val TestApplicationController = new ApplicationController(
    component
  )
  /*
  - This creates a test version of your controller that you can reuse for testing and
  call your new Action methods on.
  - Note component comes from the BaseSpecWithApplication, it is created as an instance
  of controller components and injected into the Controller.
   */

  "ApplicationController .index()" should {

  }

  "ApplicationController .create()" should {

  }

  "ApplicationController .read()" should {

  }

  "ApplicationController .update()" should {

  }

  "ApplicationController .delete()" should {

  }



}
