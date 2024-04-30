package connectors

import cats.data.EitherT
import models.APIError
import play.api.libs.json.OFormat
import play.api.libs.ws.{WSClient, WSResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/*
  There is documentation for WS client here https://www.playframework.com/documentation/latest/ScalaWS.
  There are a couple of things used in .get[Response]() that need explaining:

  - This method is used for a GET method, the url we are calling must expect this.
  - The implicit rds: OFormat[Response] is needed to parse the Json response model as our model.
  [Response] is a type parameter for our method, this must be defined when calling the method.
  This allows us to use this method for several models, e.g. get[Tea]("tea.com") and get[Coffee]("coffee.com").
  - ws.url(url) creates our request using the url.
  - The response is made using WSClient's .get() method.
  - Finally, the result's json value is parsed as our response model.
 */

class LibraryConnector @Inject()(ws: WSClient) {
  def get[Response](url: String)(implicit rds: OFormat[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)
    val response = request.get()
    EitherT {
      response.map {
          result =>
            Right(result.json.as[Response])
        }
        .recover { case _: WSResponse =>
          Left(APIError.BadAPIResponse(500, "Could not connect"))
        }
    }
  }
}
