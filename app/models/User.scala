package models

/**
 * Created by pedrorijo on 27/08/15.
 */
//case class User(val userId: String, val username: String, val profilePhoto: String, val name: String)

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class User(id: String, username: String, profilePhoto: String, name: String)

object User {
  implicit val jsonReads: Reads[User] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "username").read[String] and
      (JsPath \ "profile_picture").read[String] and
      (JsPath \ "full_name").read[String]
    )(User.apply _)
}

