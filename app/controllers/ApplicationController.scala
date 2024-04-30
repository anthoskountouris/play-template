package controllers

////import akka.actor.TypedActor.dispatcher
//import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.FutureRef
import models.{APIError, DataModel}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request, Results}
import repositories.DataRepository
import services.LibraryService

import scala.concurrent.{ExecutionContext, Future}

// This class contains values necessary in most frontend controllers, such as multi-language support.
@Singleton
class ApplicationController @Inject() (val controllerComponents: ControllerComponents, val dataRepository: DataRepository, implicit val ec: ExecutionContext, val service: LibraryService) extends BaseController{

  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map{
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
        dataRepository.create(dataModel).map(_ => Created)
      case JsError(_) => Future(BadRequest) // The result of dataRepository.create() is a Future[Result], so even though we're not doing any lookup here, the type must be the same
    }
  }

  def read(id:String) = Action.async { implicit request =>
    dataRepository.read(id).map{
      case Right(item) => Ok {Json.toJson(item)}
      case Left(_) => BadRequest{Json.toJson("Unable to find that book")}
    }
  }

  def update(id:String) = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel: DataModel, _) =>
        dataRepository.update(id, dataModel).map(_ => Accepted{Json.toJson(dataModel)})
      case JsError(_) => Future(BadRequest)
    }
  }

  def delete(id:String) = Action.async{ implicit request =>
    dataRepository.delete(id).map {
      case Right(item) => Accepted("Book deleted successfully.")
      case Left(_) => BadRequest
    }
  }

  def findByName(name:String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.findByName(name).map{
      case Right(item) => Ok {Json.toJson(item)}
      case Left(_) => BadRequest{Json.toJson("Unable to find that book")}
    }
  }


  def getGoogleBook(search: String, term: String): Action[AnyContent] = Action.async { implicit request =>
    service.getGoogleBook(search = search, term = term).value.map {
      case Right(book) => Ok{Json.toJson(book)}
      case Left(error) => BadRequest
    }
  }
}