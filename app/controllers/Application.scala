package controllers

import models.User
import play.api.mvc._
import play.api.{Logger, Play}
import services.Instagram

class Application extends Controller {

  def index: Action[AnyContent] = Action {
    Ok(views.html.index())
  }

  def insta: Action[AnyContent] = Action { request =>

    val url = Instagram.getAuthUrl(request)
    Logger.info("Redirecting to: " + url)

    Redirect(url)
  }

  def instaCallback(code: String = "", error_reason: String = "", error: String = "", error_description: String = ""): Action[AnyContent] = Action { request =>

    Logger.info("Received code: " + code)
    Logger.info("Received error_reason: " + error_reason + "; error: " + error + "; error_description: " + error_description)

    if (code.isEmpty) {
      val errorMessage: String = "Instagram server has not sent a valid code. Instead: " + error + ": " + error_reason + " - " + error_description
      Logger.error(errorMessage)
      Ok(views.html.error(errorMessage))
    }
    else {
      val hit: Option[String] = Instagram.getAccessToken(request, code)

      hit match {
        case None => {
          val errorMessage: String = "No access token"
          Logger.error(errorMessage)
          Ok(views.html.error(errorMessage))
        }
        case Some(token) => {

          val following: Seq[User] = Instagram.fetchAllFollowing(token)
          Logger.info("Got following: " + following.length)

          val followers: Seq[User] = Instagram.fetchAllFollowers(token)
          Logger.info("Got followers: " + followers.length)

          val both: Seq[User] = followers.intersect(following)
          Logger.info("Got both: " + both.length)

          val followingNotFollowedBy: Seq[User] = following.filter(e => !followers.contains(e))
          Logger.info("Got followingNotFollowedBy: " + followingNotFollowedBy.length)

          val followersNotFollowing: Seq[User] = followers.filter(e => !following.contains(e))
          Logger.info("Got followersNotFollowing: " + followersNotFollowing.length)

          assert(both.length == followers.length - followersNotFollowing.length)
          assert(both.length == following.length - followingNotFollowedBy.length)

          val pendingInRequests: Seq[User] = Instagram.fetchPendingIn(token)
          Logger.info("Got pendingInRequests: " + pendingInRequests.length)


          val ghostFeature: Option[Boolean] = Play.current.configuration.getBoolean("feature.ghost")
          if(ghostFeature.isEmpty){
            Logger.warn("Ghost feature configuration not found. Using default value")
          }

          val ghosts: Seq[User] = if (ghostFeature.getOrElse(false)) {
            val ghostsFollowed: Seq[User] = Instagram.filterGhostUser(token, following)
            Logger.info(s"Got ${ghostsFollowed.length} ghosts: ${ghostsFollowed.map(_.name).mkString(",")}")
            ghostsFollowed
          } else {
            Logger.warn("Ghost feature disabled")
            Seq()
          }

          Ok(views.html.insta(following, followers, both, followingNotFollowedBy, followersNotFollowing, pendingInRequests, ghosts))
        }
      }
    }
  }
}
