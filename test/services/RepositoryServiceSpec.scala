package services

import baseSpec.BaseSpec
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import models.{APIError, DataModel}
import org.mongodb.scala.result
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import play.api.mvc.Results.Status
import repositories.MockRepository

import scala.concurrent.{ExecutionContext, Future}

class RepositoryServiceSpec extends BaseSpec with MockFactory with ScalaFutures with GuiceOneAppPerSuite {
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val mockRepository: MockRepository = mock[MockRepository]

  val testService = new RepositoryService(mockRepository)

  val book1: JsValue = Json.obj(
    "_id" -> "1",
    "name" -> "Book One",
    "description" -> "Description One",
    "pageCount" -> 100
  )

  val book2: JsValue = Json.obj(
    "_id" -> "2",
    "name" -> "Book two",
    "description" -> "Description Two",
    "pageCount" -> 200
  )

  val book3: JsValue = Json.obj(
    "_id" -> "3",
    "name" -> "Book Three",
    "description" -> "Description Three",
    "pageCount" -> 300
  )

  val dataModels: Seq[JsValue] = Seq(book1, book2, book3)

  val emptyDataModels: Seq[JsValue]= Seq()

//  implicit val dataModelReads: Reads[DataModel] = Json.reads[DataModel]
//  println(dataModelReads)

  "RepositoryService" should {
    "index" should {
      "return data models when index is successful" in {
        (mockRepository.index _) // or (() => mockRepository.index) // converting the method call into a function value
          .expects()
          .returning(Future(Right(dataModels.map(json => json.validate[DataModel] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => fail("JSON validation error: " + errors.toString)
          }))))

        whenReady(testService.index()) { result =>
          result shouldBe Right(dataModels.map(_.as[DataModel]))
        }
      }

    "return APIError.BadAPIResponse when index fails" in {
      (mockRepository.index _)
        .expects()
        .returning(Future(Left(APIError.BadAPIResponse(404, "Books cannot be found"))))

      whenReady(testService.index()) { result =>
        result shouldBe Left(APIError.BadAPIResponse(404, "Books cannot be found"))
        }
      }
    }

      "create" should {
        "return a new data model if the operation is successful" in {
          (mockRepository.create _)
            .expects(book1.as[DataModel])
            .returning(Future(Right(book1.as[DataModel])))

          whenReady(testService.create(book1.as[DataModel])) { result =>
            result shouldBe Right(book1.as[DataModel])
          }
        }

        "return an error message if the operation fails" in{
          (mockRepository.create _)
            .expects(book1.as[DataModel])
            .returning(Future(Left(Json.toJson(s"The book already exists."))))

          whenReady(testService.create(book1.as[DataModel])) { result =>
            result shouldBe Left(Json.toJson(s"The book already exists."))
          }
        }
      }

    "read" should {
      "return a data model if the operation is successful" in {
        (mockRepository.read _)
          .expects("2")
          .returning(Future(Right(book2.as[DataModel])))

        whenReady(testService.read("2")) { result =>
          result shouldBe Right(book2.as[DataModel])
        }
      }

      "return an error message if the operation fails" in {
        (mockRepository.read _)
          .expects("5")
          .returning(Future(Left(Json.toJson("The book with the id:5 does not exist."))))

        whenReady(testService.read("5")) { result =>
          result shouldBe Left(Json.toJson("The book with the id:5 does not exist."))
        }
      }
    }

    "update" should {
      "update a data model if the operation is successful" in {
        val fakeUpdateRequest = UpdateResult.acknowledged(1, 1L, null) // https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/client/result/UpdateResult.html#acknowledged-long-java.lang.Long-org.bson.BsonValue-
        (mockRepository.update _)
          .expects("2", book3.as[DataModel])
          .returning(Future(fakeUpdateRequest))

        whenReady(testService.update("2", book3.as[DataModel] )) { result =>
          result shouldBe fakeUpdateRequest
        }
      }
    }

    "delete" should {
      "delete a data model if the operation is successful" in {
        val fakeDeletedResult = DeleteResult.acknowledged(1)
        (mockRepository.delete _)
          .expects("2")
          .returning(Future(Right(fakeDeletedResult)))

        whenReady(testService.delete("2")) { result =>
          result shouldBe Right(fakeDeletedResult)
        }
      }

      "return an error message if the operation fails" in {
//        val fakeDeletedResult = DeleteResult.unacknowledged
        (mockRepository.delete _)
          .expects("2")
          .returning(Future(Left(Json.toJson(s"No book found with this id."))))

        whenReady(testService.delete("2")) { result =>
          result shouldBe Left(Json.toJson(s"No book found with this id."))
        }
      }
    }

    "findByName" should {
      "return a data model if there is a book with that name" in {
          (mockRepository.findByName _)
            .expects("Book One")
            .returning(Future(Right(book1.as[DataModel])))

          whenReady(testService.findByName("Book One")) { result =>
            result shouldBe Right(book1.as[DataModel])
          }
        }

      "return an error message if there is not such book" in {
        (mockRepository.findByName _)
          .expects("Book One")
          .returning(Future(Left(Json.toJson(s"The book with this name does not exist."))))

        whenReady(testService.findByName("Book One")) { result =>
          result shouldBe Left(Json.toJson(s"The book with this name does not exist."))
        }
      }
    }

    "updateByField" should {
      "update a data model if the operation is successful" in {
          val fakeUpdateRequest = UpdateResult.acknowledged(1, 1L, null) // https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/client/result/UpdateResult.html#acknowledged-long-java.lang.Long-org.bson.BsonValue-
          (mockRepository.updateByField _)
            .expects("2","description", "new description people")
            .returning(Future(Right(fakeUpdateRequest)))

          whenReady(testService.updateByField("2","description", "new description people")) { result =>
            result shouldBe Right(fakeUpdateRequest)
          }
      }

      "return an error message if the operation fails" in {
//        val fakeUpdateRequest = UpdateResult.acknowledged(1, 1L, null) // https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/client/result/UpdateResult.html#acknowledged-long-java.lang.Long-org.bson.BsonValue-
        (mockRepository.updateByField _)
          .expects("2","description", "new description people")
          .returning(Future(Left(Json.toJson("No book found with this id"))))

        whenReady(testService.updateByField("2","description", "new description people")) { result =>
          result shouldBe Left(Json.toJson("No book found with this id"))
        }
      }
    }




  }
}

//Right(List(DataModel("1", "Book One", "Description One", 100),
//  DataModel("2", "Book two", "Description Two", 200),
//  DataModel("3", "Book Three", "Description Three", 300))) was not equal to
//
//Right(List(JsSuccess(DataModel("1", "Book One", "Description One", 100), ), JsSuccess(DataModel("2", "Book two", "Description Two", 200), ), JsSuccess(DataModel("3", "Book Three", "Description Three", 300), ))) (RepositoryServiceSpec.scala:57)
