package models

import play.api.libs.json.{Json, OFormat}

case class VolumeInfo(title:String, description:Option[String], pageCount:Option[Int])

object VolumeInfo {
  implicit val formats: OFormat[VolumeInfo] = Json.format[VolumeInfo]
}

// Data model
case class DataModel(id:String, volumeInfo: VolumeInfo)

// This allows for easily transforming the model to and from JSON.
object DataModel {
  implicit val formats: OFormat[DataModel] = Json.format[DataModel]
//  val bookOne = DataModel(_id = "id1", name = "Book name", description = "Author name", pageCount = 10)
//  val booktwo = DataModel(_id = "id1", name = "Book name", description = "Author name", pageCount = 10)
}

case class ApiResponse(items: List[DataModel])

object ApiResponse {
  implicit val formats: OFormat[ApiResponse] = Json.format[ApiResponse]
}