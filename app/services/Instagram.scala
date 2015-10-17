package services

import controllers.routes
import models.User
import play.api.Play.current
import play.api.libs.json.{JsError, JsLookupResult, JsSuccess}
import play.api.libs.ws.{WS, WSResponse}
import play.api.mvc.{AnyContent, Request}
import play.api.{Logger, Play}
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by pedrorijo on 22/08/15.
 */
object Instagram {

  private[this] val clientId: String = Play.current.configuration.getString("instagram.client.id").getOrElse("")
  private[this] val clientSecret: String = Play.current.configuration.getString("instagram.client.secret").getOrElse("")
  private[this] val redirectURL: play.api.mvc.Call = routes.Application.instaCallback()

  def getAuthUrl(request: Request[AnyContent]): String = {
    val path = (if (request.host.contains(":9000")) "http://" else "https://") + request.host + redirectURL
    Logger.info("Path: " + path)

    val scopes = "relationships"
    //TODO state

    "https://api.instagram.com/oauth/authorize/?client_id=" + clientId + "&redirect_uri=" + path + "&response_type=code" + "&scope=" + scopes
  }

  def getAccessToken(request: Request[_], code: String): Option[String] = {
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    val path = (if (request.host.contains(":9000")) "http://" else "https://") + request.host + redirectURL

    //TODO 2 seconds timeout
    val postParams: Map[String, Seq[String]] = Map("client_id" -> Seq(clientId), "client_secret" -> Seq(clientSecret),
      "grant_type" -> Seq("authorization_code"), "redirect_uri" -> Seq(path), "code" -> Seq(code))
    val result: WSResponse = Await.result(WS.url("https://api.instagram.com/oauth/access_token").post(postParams), 2 seconds)

    Logger.info("getAccessToken result: " + result.json.toString())

    val accessToken: JsLookupResult = result.json \ "access_token"
    accessToken.toOption match {
      case None => {
        Logger.error("Not possible to parse access token. Instead: " + result.json)
        None
      }
      case Some(token) => {
        Logger.info("AccessToken parsed: " + token)
        Some(token.toString)
      }
    }

    /*
    FIXME
    Due to the fact that Instagram API does not allows new apps to get access to relationships/likes scopes
    unless the target users are business, this app will not work.
    If you want to check stats for your own account just get an access token and replace use the below line

    You can get a token using the following url, providing authorization, making an endpoint call, and inspecting
    the outgoing request, looking for the access token:

    https://apigee.com/console/instagram
     */

    //Some("VALID ACCESS TOKEN") // Replace the token here and uncomment this line
  }

  def fetchAllFollowing(token: String): Seq[User] = {

    val url: String = "https://api.instagram.com/v1/users/self/follows?access_token=" + token

    fetchPageFollowing(url)
  }

  private[this] def fetchPageFollowing(url: String): Seq[User] = {
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    //TODO 2 seconds timeout
    //TODO check result
    val result: WSResponse = Await.result(WS.url(url).get(), 2 seconds)

    val pagination: JsLookupResult = result.json \ "pagination" \ "next_url"

    val users: Seq[User] = (result.json \ "data").validate[Seq[User]] match {
      case JsSuccess(value, _) => {
        Logger.info("Parsed users: " + value.length)
        value
      }
      case JsError(errors) => {
        Logger.error("Cannot parse User from data: " + errors)
        Nil
      }
    }

    pagination.toOption match {
      case None => {
        Logger.info("No more pagination for followings")
        users
      }
      case Some(nextUrl) => {
        val next: String = nextUrl.toString().filterNot(_ == '"')
        Logger.info("nextURL for followings: " + next)
        users ++ fetchPageFollowing(next)
      }
    }
  }

  def fetchAllFollowers(token: String): Seq[User] = {

    val url: String = "https://api.instagram.com/v1/users/self/followed-by?access_token=" + token

    fetchPageFollowers(url)
  }

  private[this] def fetchPageFollowers(url: String): Seq[User] = {
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    //TODO 2 seconds timeout
    //TODO check result
    val result: WSResponse = Await.result(WS.url(url).get(), 2 seconds)

    val pagination: JsLookupResult = result.json \ "pagination" \ "next_url"

    val users: Seq[User] = (result.json \ "data").validate[Seq[User]] match {
      case JsSuccess(value, _) => {
        Logger.info("Parsed users: " + value.length)
        value
      }
      case JsError(errors) => {
        Logger.error("Cannot parse User from data: " + errors)
        Nil
      }
    }

    pagination.toOption match {
      case None => {
        Logger.info("No more pagination for followers")
        users
      }
      case Some(nextUrl) => {
        val next: String = nextUrl.toString().filterNot(_ == '"')
        Logger.info("nextURL for followers: " + next)
        users ++ fetchPageFollowers(next)
      }
    }
  }

  def fetchPendingIn(token: String): Seq[User] = {

    val url: String = "https://api.instagram.com/v1/users/self/requested-by?access_token=" + token

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    //TODO 2 seconds timeout
    //TODO check result
    val result: WSResponse = Await.result(WS.url(url).get(), 2 seconds)

    val users: Seq[User] = (result.json \ "data").validate[Seq[User]] match {
      case JsSuccess(value, _) => {
        Logger.info("Parsed users: " + value.length)
        value
      }
      case JsError(errors) => {
        Logger.error("Cannot parse User from data: " + errors)
        Nil
      }
    }

    users
  }

  def filterGhostUser(token: String, users: Seq[User]): Seq[User] = {

    users.filter {
      case user =>
        val url: String = s"https://api.instagram.com/v1/users/${user.id}/?access_token=$token"

        implicit val context = scala.concurrent.ExecutionContext.Implicits.global

        //TODO 2 seconds timeout
        //TODO check result
        val result: WSResponse = Await.result(WS.url(url).get(), 2 seconds)

        (result.json \ "data" \ "counts" \ "media").validate[Int] match {
          case JsSuccess(value, _) => {
            Logger.debug(s"User ${user.id} with $value posts")
            if(value == 0) Logger.warn(s"Found user with 0 posts: ${user.id} - ${user.name}")
            value == 0
          }
          case JsError(errors) => {
            Logger.error(s"Cannot parse media count from data: $errors")
            false
          }
        }
    }
  }

}
