package models

import play.api.libs.json.{Json, OFormat}

// Data model
case class DataModel(_id:String, name: String, description: String, pageCount:Int)

// This allows for easily transforming the model to and from JSON.
object DataModel {
  implicit val formats: OFormat[DataModel] = Json.format[DataModel]
//  val bookOne = DataModel(_id = "id1", name = "Book name", description = "Author name", pageCount = 10)
//  val booktwo = DataModel(_id = "id1", name = "Book name", description = "Author name", pageCount = 10)
}

