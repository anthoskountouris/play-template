package controllers

////import akka.actor.TypedActor.dispatcher
//import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.FutureRef
import models.{APIError, DataModel}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request, Results, Result}
import repositories.DataRepository
import services.{LibraryService, RepositoryService}
import play.filters.csrf.CSRF
import scala.concurrent.{ExecutionContext, Future}

// This class contains values necessary in most frontend controllers, such as multi-language support.
@Singleton
class ApplicationController @Inject() (val controllerComponents: ControllerComponents, val dataRepository: DataRepository, implicit val ec: ExecutionContext, val service: LibraryService, val repService: RepositoryService) extends BaseController with play.api.i18n.I18nSupport{

  def index(): Action[AnyContent] = Action.async { implicit request =>
    repService.index().map{
      case Right(item: Seq[DataModel]) => Ok {Json.toJson(item)}
      case Left(apiError: APIError) => InternalServerError(Json.obj("error" -> apiError.upstreamMessage))
    }
  }
  /*
  .find() is a built-in method in the library we're using, and will return all items in the data repository.
  - The result returned by this method is a Future - essentially a placeholder for the result of performing
    the lookup operation in the database. We use .map(items => Json.toJson(items)).map(result => Ok(result))
    to write what we want to do with the result.
  - In this case, we take the resulting object (of type Seq[DataModel]), transform it into JSON, and return it
    in the body of an Ok / 200 response
   */

  /*
  TODO is a Play feature that is essentially a default page for controller actions
   that haven't been completed. It is a useful way of keeping your app functioning
   while building functionality incrementally.
   */

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel: DataModel, _) =>
        repService.create(dataModel).map(_ => Created)
      case JsError(_) => Future(BadRequest) // The result of dataRepository.create() is a Future[Result], so even though we're not doing any lookup here, the type must be the same
    }
  }

  def read(id:String) = Action.async { implicit request =>
    repService.read(id).map {
      case Right(item) => Ok {
        Json.toJson(item)
      }
      case Left(_) => BadRequest(views.html.unableToFindBook(er = "Unable to find this book!"))
    }
  }

  def update(id:String) = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel: DataModel, _) =>
        repService.update(id, dataModel).map(_ => Accepted{Json.toJson(dataModel)})
      case JsError(_) => Future(BadRequest{views.html.unableToFindBook(er = "Something went wrong!")})
    }
  }

  def delete(id:String) = Action.async{ implicit request =>
    repService.delete(id).map {
      case Right(item) => Accepted("Book deleted successfully.")
      case Left(_) => BadRequest{Json.toJson("Something went wrong!")}
    }
  }

  def findByName(name:String): Action[AnyContent] = Action.async { implicit request =>
    repService.findByName(name).map{
      case Right(item) => Ok {Json.toJson(item)}
      case Left(_) => BadRequest{views.html.unableToFindBook(er = "Unable to find that book")}
    }
  }

//  def updateByField(id: String, map: Map[String, String]): Action[AnyContent] = Action.async { implicit request =>
//    dataRepository.updateByField(id, map).map {
//      case Right(updateBook) => Ok {Json.toJson(updateBook)}
//      case Left(err) => BadRequest{Json.toJson(s"The book was not found")}
//    }
//  }

  def updateByField(id: String, fieldName:String, value:String): Action[AnyContent] = Action.async { implicit request =>
    repService.updateByField(id, fieldName, value).map { case Right(_) => Accepted("Book updated successfully.")
    case Left(_) => BadRequest(views.html.unableToFindBook(er ="Something went wrong"))
    }
  }

//  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
//    service.getGoogleBook(search = search, term = term).value.map {
//      case Right(book) =>
//        Ok (Json.toJson(book.items.head))
//      case Left(_) => BadRequest(Json.toJson("Something went wrong"))
//    }
//  }

  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).value.flatMap {
      case Right(book) =>
        Json.toJson(book.items.head).validate[DataModel] match {
          case JsSuccess(dataModel, _) =>
            repService.create(dataModel).map(_ => Created(views.html.example(dataModel = dataModel)))
          case JsError(_) =>
            Future(BadRequest(views.html.unableToFindBook(er = "Invalid data model")))
//            Future(BadRequest(Json.toJson("Invalid data model")))
        }
      case Left(error) =>
        Future(BadRequest(views.html.unableToFindBook(er ="This book does not exist on Google Books API")))
    }
  }

//  request.body.validate[DataModel] match {
//    case JsSuccess(dataModel: DataModel, _) =>
//      repService.create(dataModel).map(_ => Created)
//    case JsError(_) => Future(BadRequest) // The result of dataRepository.create() is a Future[Result], so even though we're not doing any lookup here, the type must be the same
//  }

  def example(): Action[AnyContent] = Action.async {implicit request: Request[AnyContent] =>
    repService.read("OSchEAAAQBAJ").map {
      case Right(result) =>
      Ok(views.html.example(dataModel = result))
    }
  }

  // for the GET request that will load the form views page
  def addBook(): Action[AnyContent] = Action.async {implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.form(bookForm = DataModel.bookForm)))
  }

  /* Cross Site Request Forgery (CSRF) is a security exploit where an attacker
  tricks a victim’s browser into making a request using the victim’s session.
  */

  def accessToken(implicit request: Request[_]) = {
    CSRF.getToken
  }

  // for the POST request that will create the new book
  def addBookForm(): Action[AnyContent] =  Action.async {implicit request =>
    accessToken //call the accessToken method
    DataModel.bookForm
      .bindFromRequest()
      .fold( //from the implicit request we want to bind this to the form in our companion object
      formWithErrors => {
        //here write what you want to do if the form has errors
        Future.successful(BadRequest(views.html.form(formWithErrors)))
      },
      formData => {
        //here write how you would use this data to create a new book (DataModel)
        repService.create(formData).map { _ =>
          Ok(views.html.example(dataModel = formData))
        } recover
          { case _ => InternalServerError(views.html.unableToFindBook(er ="Could not create the book"))}
      }
    )
  }
}