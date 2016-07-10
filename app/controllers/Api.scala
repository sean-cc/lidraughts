package controllers

import play.api.libs.json._
import play.api.mvc._, Results._
import scala.concurrent.duration._

import lila.app._

object Api extends LilaController {

  private val userApi = Env.api.userApi
  private val gameApi = Env.api.gameApi

  def status = Action { req =>
    val api = lila.api.Mobile.Api
    Ok(Json.obj(
      "api" -> Json.obj(
        "current" -> api.currentVersion,
        "olds" -> api.oldVersions.map { old =>
          Json.obj(
            "version" -> old.version,
            "deprecatedAt" -> old.deprecatedAt,
            "unsupportedAt" -> old.unsupportedAt)
        })
    )) as JSON
  }

  def user(name: String) = ApiResult { implicit ctx =>
    userApi one name
  }

  def users = ApiResult { implicit ctx =>
    get("team") ?? { teamId =>
      userApi.list(
        teamId = teamId,
        engine = getBoolOpt("engine"),
        nb = getInt("nb")
      ).map(_.some)
    }
  }

  private val GamesRateLimit = new lila.memo.RateLimit(100, 10 minutes, "user games API")

  def userGames(name: String) = ApiResult { implicit ctx =>
    if (lila.common.HTTPRequest.isBot(ctx.req)) fuccess(none)
    else GamesRateLimit(ctx.req.remoteAddress) {
      val page = getInt("page")
      if (~page > 200) fuccess(Json.obj("error" -> "Going too far back in time.").some)
      else lila.user.UserRepo named name flatMap {
        _ ?? { user =>
          gameApi.byUser(
            user = user,
            rated = getBoolOpt("rated"),
            analysed = getBoolOpt("analysed"),
            withAnalysis = getBool("with_analysis"),
            withMoves = getBool("with_moves"),
            withOpening = getBool("with_opening"),
            withMoveTimes = getBool("with_movetimes"),
            token = get("token"),
            nb = getInt("nb"),
            page = page
          ) map some
        }
      }
    }
  }

  def game(id: String) = ApiResult { implicit ctx =>
    gameApi.one(
      id = id take lila.game.Game.gameIdSize,
      withAnalysis = getBool("with_analysis"),
      withMoves = getBool("with_moves"),
      withOpening = getBool("with_opening"),
      withFens = getBool("with_fens"),
      withMoveTimes = getBool("with_movetimes"),
      token = get("token"))
  }

  private def ApiResult(js: lila.api.Context => Fu[Option[JsValue]]) = Open { implicit ctx =>
    js(ctx) map {
      case None => NotFound
      case Some(json) => get("callback") match {
        case None           => Ok(json) as JSON
        case Some(callback) => Ok(s"$callback($json)") as JAVASCRIPT
      }
    }
  }
}
