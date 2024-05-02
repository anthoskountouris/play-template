package repositories

import com.google.inject.ImplementedBy
import models.{APIError, DataModel}
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{empty, equal}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import org.mongodb.scala.{Document, FindObservable, result}
import play.api.libs.json
import play.api.libs.json.{JsError, JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DataRepository])
trait MockRepository {
  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]]
  def create(book: DataModel): Future[Either[JsValue, DataModel]]
  def read(id: String): Future[Either[JsValue,DataModel]]
  def update(id: String, book:DataModel): Future[result.UpdateResult]
  def delete(id: String): Future[Either[JsValue, result.DeleteResult]]
  def findByName(name:String): Future[Either[JsValue, DataModel]]
  def updateByField(id:String, fieldName: String, value:String): Future[Either[JsValue, result.UpdateResult]]
}

@Singleton
class DataRepository @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext) extends PlayMongoRepository[DataModel](
  collectionName = "dataModels",
  mongoComponent = mongoComponent,
  domainFormat = DataModel.formats,
  indexes = Seq(IndexModel(
    Indexes.ascending("_id")
  )),
  replaceIndexes = false
  /*
  - "dataModels" is the name of the collection (you can set this to whatever you like).
  - DataModel.formats uses the implicit val formats we created earlier.
    It tells the driver how to read and write between a DataModel and JSON
    (the format that data is stored in Mongo)
  - indexes is shows the structure of the data stored in Mongo, notice we can ensure the bookId to be unique
   */
) with MockRepository{

  /*
  .find() returns a FindObservable, which is capable of returning multiple documents as the
  result of the query. This is akin to getting a cursor that can iterate over multiple results.
   */

  def index(): Future[Either[APIError.BadAPIResponse, Seq[DataModel]]] =
    collection.find().toFuture().map {
      case books: Seq[DataModel] => Right(books)
      case _ => Left(APIError.BadAPIResponse(404, "Books cannot be found"))
    }

  /*
  .first() is a method on FindObservable that modifies the behavior of the query such
  that only the first document that matches the query is returned. If no documents match,
  the result will be null.
   */

  def create(book: DataModel): Future[Either[JsValue, DataModel]] = {
    collection.find(byID(book._id)).first().toFuture() flatMap  {
      case bk: DataModel => Future(Left(Json.toJson(s"The book with the id: ${bk._id} already exists.")))
      case _ => collection
        .insertOne(book)
        .toFuture()
        .map(_ => Right(book))
    }
  }

  private def byID(id: String): Bson =
    Filters.and(
      Filters.equal("_id", id)
    )

  def read(id: String): Future[Either[JsValue,DataModel]] =
    collection.find(byID(id)).headOption flatMap {
      case Some(data) =>
        Future(Right(data))
      case None => Future(Left(Json.toJson(s"The book with the id: ${id} already exists.")))
    }

//  def read(id: String): Future[Either[JsValue, DataModel]] =
//    collection.find(byID(id)).headOption flatMap {
//      case Some(data) =>
//        Future(Right(data))
//      case None => Future(Left(Json.toJson(s"The book with the id: ${id} already exists.")))
//    }

//  def update(id: String, book:DataModel): Future[Either[JsValue,result.UpdateResult]] =
//    collection.replaceOne(
//      filter = byID(id),
//      replacement = book,
//      options = new ReplaceOptions().upsert(true)
//    ).toFuture().map(updateResult => Right(updateResult)).recover{case _ => Left(Json.toJson(s"Failed to update the document with id $id"))}

  def update(id: String, book:DataModel): Future[result.UpdateResult] = {
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(true)
    ).toFuture()
  }

  /*
  DataModel if bk != null ensures that a valid DataModel object was returned.
  Comparing directly against DataModel won't work if find returns null when no
  document is found, hence the if book != null guard
   */

  def delete(id: String): Future[Either[JsValue, result.DeleteResult]] =
    collection.find(byID(id)).first().toFuture() flatMap {
      case bk: DataModel if bk != null => collection.deleteOne(
        filter = byID(id)
      ).toFuture().map(deleteResult => Right(deleteResult))
      case _ => Future(Left(Json.toJson(s"No book found with the id: ${id}.")))
    }

  def deleteAll(): Future[Unit] = collection.deleteMany(empty()).toFuture().map(_ => ()) //Hint: needed for tests

      /*
  Each of these methods correspond to a CRUD function.
  create() adds a DataModel object to the database
  read() retrieves a DataModel object from the database. It uses an id parameter to find the data its looking for
  update() takes in a DataModel, finds a matching document with the same id and updates the document. It then returns the updated DataModel
  delete() deletes a document in the database that matches the id passed in
  delteAll() is similar to delete, this removes all data from Mongo with the same collection name
  All of the return types of these functions are asynchronous futures.
   */

  // TASK 2 PART 5

  private def byName(name: String): Bson =
    Filters.and(
      Filters.equal("name", name)
    )

  // Finding a book by name (first occurrence)
  def findByName(name:String): Future[Either[JsValue, DataModel]] =
    collection.find(byName(name)).headOption flatMap  {
      case Some(data) => Future(Right(data))
      case None => Future(Left(Json.toJson(s"The book with the name: ${name} does not exist.")))
    }

  // Finding a book by name (all books with that name if there are many)
//  def findByName(name:String): Future[Either[JsValue, Seq[DataModel]]] =
//    collection.find(byName(name)).toFuture().map  {
//      case books: Seq[DataModel] => Right(books)
//      case _ => Left(Json.toJson(s"The book with the name: ${name} does not exist."))
//    }

//   Updating a book field by only providing the _id, field name and change, not a whole book
//  def updateByField(id: String, fieldName: String, value:String): Future[Either[JsValue, result.UpdateResult]] = {
//      collection.updateOne(equal("_id", id), set(fieldName, value)).headOption flatMap {
//      case Some(x) => Future(Right(x))
//      case None => Future(Left(Json.toJson(s"No book found with the id: ${id}.")))
//    }
//  }

//  def updateByField(id: String, map: Map[String, String]): Future[Either[JsValue, result.UpdateResult]] = {
//
//    val updateDocument = Document("$set" -> Document(map))
//    collection.updateOne(Filters.eq("_id", BsonObjectId(id)), updateDocument).headOption() flatMap {
//      case Some(data) => Future(Right(data))
//      case None => Future(Left(Json.toJson(s"There is no such book or you've given invalid field")))
//    }
////      .toFuture()
//  }

  def updateByField(id:String, fieldName: String, value:String): Future[Either[JsValue, result.UpdateResult]] = {
    collection.find(byID(id)).headOption.flatMap {
      case Some(book) =>
        val updatedBook = fieldName match {
          case "_id" => Left(Json.toJson("Cannot change '_id' of a book."))
          case "name" => Right(book.copy(name = value))
          case "description" => Right(book.copy(description = value))
          case "pageCount" =>
            try {
              Right(book.copy(pageCount = value.toInt))
            } catch {
              case e: NumberFormatException => Left(Json.toJson("Invalid number format for pageCount."))
            }
          case _ => Left(Json.toJson(s"Field '$fieldName' is not recognized."))
        }

        updatedBook match {
          case Right(bk) => update(id, bk).map(result => Right(result))
          case Left(err) => Future(Left(err))
        }
      case None => Future(Left(Json.toJson(s"No book found with ID: $id")))
    }
  }
}