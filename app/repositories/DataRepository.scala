package repositories

import models.DataModel
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.empty
import org.mongodb.scala.model._
import org.mongodb.scala.{FindObservable, result}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataRepository @Inject()(
                                mongoComponent: MongoComponent
                              )(implicit ec: ExecutionContext) extends PlayMongoRepository[DataModel](
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
) {

  def index(): Future[Either[Int, Seq[DataModel]]]  =
    collection.find().toFuture().map{
      case books: Seq[DataModel] => Right(books)
      case _ => Left(404)
    }

  def create(book: DataModel): Future[Either[JsValue, DataModel]] = {
    collection.find(byID(book._id)) match {
      case bk:DataModel => Future(Left(Json.toJson(s"The book with the id: ${bk._id} already exists.")))
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

  def read(id: String): Future[DataModel] =
    collection.find(byID(id)).headOption flatMap {
      case Some(data) =>
        Future(data)
    }

  def update(id: String, book: DataModel): Future[result.UpdateResult] =
    collection.replaceOne(
      filter = byID(id),
      replacement = book,
      options = new ReplaceOptions().upsert(true) //What happens when we set this to false?
    ).toFuture()

  def delete(id: String): Future[result.DeleteResult] =
    collection.deleteOne(
      filter = byID(id)
    ).toFuture()

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

}
