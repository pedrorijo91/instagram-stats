package services

import play.api.Play

object Config {

  val clientId: String = Play.current.configuration.getString("instagram.client.id").getOrElse("")
  val clientSecret: String = Play.current.configuration.getString("instagram.client.secret").getOrElse("")

  val accessToken: Option[String] = Play.current.configuration.getString("instagram.access.token")

  val ghostFeature: Option[Boolean] = Play.current.configuration.getBoolean("feature.ghost")
  val ghostFeatureDefault: Boolean = false

}
