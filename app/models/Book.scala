package models

import play.api.libs.json.{Json, OFormat}


//case class Book(id: String, volumeInfo: VolumeInfo, pageCount: Option[Int])
//
//object Book{
//  implicit val formats: OFormat[Book] = Json.format[Book]
//}
//
//case class VolumeInfo(title:String, description: Option[String])
//
//object VolumeInfo {
//  implicit val formats: OFormat[VolumeInfo] = Json.format[VolumeInfo]
//}

case class Book(id: String, title:String, description:Option[String], pageCount: Option[Int])

object Book{
  implicit val formats: OFormat[Book] = Json.format[Book]
}




