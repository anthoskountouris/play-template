package services

import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, Book, DataModel}
import org.mongodb.scala.result
import play.api.libs.json.JsValue
import repositories.MockRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class RepositoryService  @Inject()(mockRepository: MockRepository)(implicit ex: ExecutionContext){

  def index():Future[Either[APIError.BadAPIResponse, Seq[DataModel]]] = mockRepository.index()

  def create(book: DataModel): Future[Either[JsValue, DataModel]] = mockRepository.create(book)

  def read(id:String): Future[Either[JsValue, DataModel]] = mockRepository.read(id)

  def update(id:String, book: DataModel): Future[result.UpdateResult] = mockRepository.update(id, book)

  def delete(id:String): Future[Either[JsValue, result.DeleteResult]] = mockRepository.delete(id)

  def findByName(name:String): Future[Either[JsValue, DataModel]] = mockRepository.findByName(name)

  def updateByField(id:String, fieldName: String, value:String): Future[Either[JsValue, result.UpdateResult]] =
    mockRepository.updateByField(id, fieldName, value)

//  def

}



