package services

import cats.data.EitherT
import connectors.LibraryConnector
import models.{APIError, ApiResponse, Book, DataModel}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LibraryService @Inject()(connector:LibraryConnector){
  def getGoogleBook(urlOverride: Option[String] = None, search:String, term: String)(implicit ex: ExecutionContext): EitherT[Future, APIError, ApiResponse] =
    connector.get[ApiResponse](urlOverride.getOrElse(s"https://www.googleapis.com/books/v1/volumes?q=$search:$term"))
}