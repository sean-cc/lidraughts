package lidraughts.round

import scala.concurrent.duration._
import scala.math

import play.api.libs.json._

import lidraughts.common.ApiVersion
import lidraughts.game.JsonView._
import lidraughts.game.{ Pov, Game, Player => GamePlayer }
import lidraughts.pref.Pref
import lidraughts.user.{ User, UserRepo }

import draughts.format.{ Forsyth, FEN }
import draughts.{ Color, Clock }

import actorApi.SocketStatus

final class JsonView(
    noteApi: NoteApi,
    userJsonView: lidraughts.user.JsonView,
    getSocketStatus: Game.ID => Fu[SocketStatus],
    canTakeback: Game => Fu[Boolean],
    divider: lidraughts.game.Divider,
    evalCache: lidraughts.evalCache.EvalCacheApi,
    baseAnimationDuration: Duration,
    moretimeSeconds: Int
) {

  import JsonView._

  private def kingMoves(game: Game, color: Color) =
    (game.variant.frisianVariant) option game.history.kingMoves(color)

  private def commonPlayerJson(g: Game, p: GamePlayer, user: Option[User], withFlags: WithFlags): JsObject =
    Json.obj(
      "color" -> p.color.name
    ).add("user" -> user.map { userJsonView.minimal(_, g.perfType) })
      .add("rating" -> p.rating)
      .add("ratingDiff" -> p.ratingDiff)
      .add("provisional" -> p.provisional)
      .add("offeringRematch" -> p.isOfferingRematch)
      .add("offeringDraw" -> p.isOfferingDraw)
      .add("proposingTakeback" -> p.isProposingTakeback)
      .add("kingMoves" -> kingMoves(g, p.color))
      .add("berserk" -> p.berserk)
      .add("hold" -> (withFlags.blurs option hold(p)))
      .add("blurs" -> (withFlags.blurs ?? blurs(g, p)))

  def playerJson(
    pov: Pov,
    pref: Pref,
    apiVersion: ApiVersion,
    playerUser: Option[User],
    initialFen: Option[FEN],
    withFlags: WithFlags
  ): Fu[JsObject] = {
    getSocketStatus(pov.game.id) zip
      (pov.opponent.userId ?? UserRepo.byId) zip
      canTakeback(pov.game) map {
        case ((socket, opponentUser), takebackable) =>
          import pov._
          Json.obj(
            "game" -> gameJson(game, initialFen),
            "player" -> {
              commonPlayerJson(game, player, playerUser, withFlags) ++ Json.obj(
                "id" -> playerId,
                "version" -> socket.version
              )
            }.add("onGame" -> (player.isAi || socket.onGame(player.color))),
            "opponent" -> {
              commonPlayerJson(game, opponent, opponentUser, withFlags) ++ Json.obj(
                "color" -> opponent.color.name,
                "ai" -> opponent.aiLevel
              )
            }.add("isGone" -> (!opponent.isAi && socket.isGone(opponent.color)))
              .add("onGame" -> (opponent.isAi || socket.onGame(opponent.color))),
            "url" -> Json.obj(
              "socket" -> s"/$fullId/socket/v$apiVersion",
              "round" -> s"/$fullId"
            ),
            "pref" -> Json.obj(
              "animationDuration" -> animationDuration(pov, pref),
              "coords" -> pref.coords,
              "replay" -> pref.replay,
              "clockTenths" -> pref.clockTenths,
              "moveEvent" -> pref.moveEvent
            ).add("clockBar" -> pref.clockBar)
              .add("clockSound" -> pref.clockSound)
              .add("confirmResign" -> (pref.confirmResign == Pref.ConfirmResign.YES))
              .add("keyboardMove" -> (pref.keyboardMove == Pref.KeyboardMove.YES))
              .add("blindfold" -> pref.isBlindfold)
              .add("highlight" -> (pref.highlight || pref.isBlindfold))
              .add("destination" -> (pref.destination && !pref.isBlindfold))
              .add("enablePremove" -> pref.premove)
              .add("showCaptured" -> pref.captured)
              .add("submitMove" -> {
                import Pref.SubmitMove._
                pref.submitMove match {
                  case _ if game.hasAi => false
                  case ALWAYS => true
                  case CORRESPONDENCE_UNLIMITED if game.isCorrespondence => true
                  case CORRESPONDENCE_ONLY if game.hasCorrespondenceClock => true
                  case _ => false
                }
              })
          ).add("clock" -> game.clock.map(clockJson))
            .add("correspondence" -> game.correspondenceClock)
            .add("takebackable" -> takebackable)
            .add("possibleMoves" -> possibleMoves(pov))
            .add("possibleDrops" -> possibleDrops(pov))
            .add("captureLength" -> captureLength(pov))
            .add("expiration" -> game.expirable.option {
              Json.obj(
                "idleMillis" -> (nowMillis - game.movedAt.getMillis),
                "millisToMove" -> game.timeForFirstMove.millis
              )
            })
      }
  }

  private def commonWatcherJson(g: Game, p: GamePlayer, user: Option[User], withFlags: WithFlags): JsObject =
    Json.obj(
      "color" -> p.color.name,
      "name" -> p.name
    ).add("user" -> user.map { userJsonView.minimal(_, g.perfType) })
      .add("ai" -> p.aiLevel)
      .add("rating" -> p.rating)
      .add("ratingDiff" -> p.ratingDiff)
      .add("provisional" -> p.provisional)
      .add("kingMoves" -> kingMoves(g, p.color))
      .add("berserk" -> p.berserk)
      .add("hold" -> (withFlags.blurs option hold(p)))
      .add("blurs" -> (withFlags.blurs ?? blurs(g, p)))

  def watcherJson(
    pov: Pov,
    pref: Pref,
    apiVersion: ApiVersion,
    me: Option[User],
    tv: Option[OnTv],
    initialFen: Option[FEN] = None,
    withFlags: WithFlags
  ) =
    getSocketStatus(pov.game.id) zip
      UserRepo.pair(pov.player.userId, pov.opponent.userId) map {
        case (socket, (playerUser, opponentUser)) =>
          import pov._
          Json.obj(
            "game" -> gameJson(game, initialFen)
              .add("moveCentis" -> (withFlags.movetimes ?? game.moveTimes.map(_.map(_.centis))))
              .add("division" -> withFlags.division.option(divider(game, initialFen)))
              .add("opening" -> game.opening)
              .add("importedBy" -> game.pdnImport.flatMap(_.user)),
            "clock" -> game.clock.map(clockJson),
            "correspondence" -> game.correspondenceClock,
            "player" -> {
              commonWatcherJson(game, player, playerUser, withFlags) ++ Json.obj(
                "version" -> socket.version,
                "spectator" -> true
              )
            },
            "opponent" -> commonWatcherJson(game, opponent, opponentUser, withFlags),
            "captureLength" -> captureLength(pov),
            "orientation" -> pov.color.name,
            "url" -> Json.obj(
              "socket" -> s"/$gameId/${color.name}/socket",
              "round" -> s"/$gameId/${color.name}"
            ),
            "pref" -> Json.obj(
              "animationDuration" -> animationDuration(pov, pref),
              "coords" -> pref.coords,
              "replay" -> pref.replay,
              "clockTenths" -> pref.clockTenths
            ).add("clockBar" -> pref.clockBar)
              .add("highlight" -> (pref.highlight || pref.isBlindfold))
              .add("destination" -> (pref.destination && !pref.isBlindfold))
              .add("showCaptured" -> pref.captured),
            "evalPut" -> JsBoolean(me.??(evalCache.shouldPut))
          ).add("evalPut" -> me.??(evalCache.shouldPut))
            .add("tv" -> tv.collect {
              case OnLidraughtsTv(channel, flip) => Json.obj("channel" -> channel, "flip" -> flip)
            }).add("userTv" -> tv.collect {
              case OnUserTv(userId) => Json.obj("id" -> userId)
            })

      }

  private implicit val userLineWrites = OWrites[lidraughts.chat.UserLine] { l =>
    val j = Json.obj("u" -> l.username, "t" -> l.text)
    if (l.deleted) j + ("d" -> JsBoolean(true)) else j
  }

  def userAnalysisJson(
    pov: Pov,
    pref: Pref,
    initialFen: Option[FEN],
    orientation: draughts.Color,
    owner: Boolean,
    me: Option[User],
    division: Option[draughts.Division] = none
  ) = {
    import pov._
    val fen = Forsyth >> game.draughts
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "variant" -> game.variant,
        "opening" -> game.opening,
        "initialFen" -> initialFen.fold(draughts.format.Forsyth.initial)(_.value),
        "fen" -> fen,
        "turns" -> game.turns,
        "player" -> game.turnColor.name,
        "status" -> game.status
      ).add("division", division),
      "player" -> Json.obj(
        "id" -> owner.option(pov.playerId),
        "color" -> color.name
      ),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.aiLevel
      ),
      "orientation" -> orientation.name,
      "pref" -> Json.obj(
        "animationDuration" -> animationDuration(pov, pref),
        "coords" -> pref.coords
      ).add("highlight" -> (pref.highlight || pref.isBlindfold))
        .add("destination" -> (pref.destination && !pref.isBlindfold)),
      "path" -> pov.game.turns,
      "userAnalysis" -> true
    ).add("evalPut" -> me.??(evalCache.shouldPut))
  }

  def puzzleEditorJson(
                        pov: Pov,
                        pref: Pref,
                        initialFen: Option[FEN],
                        orientation: draughts.Color,
                        owner: Boolean,
                        me: Option[User],
                        division: Option[draughts.Division] = none
                      ) = {
    import pov._
    val fen = Forsyth >> game.draughts
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "variant" -> game.variant,
        "opening" -> game.opening,
        "initialFen" -> initialFen.fold(draughts.format.Forsyth.initial)(_.value),
        "fen" -> fen,
        "turns" -> game.turns,
        "player" -> game.turnColor.name,
        "status" -> game.status
      ).add("division", division),
      "player" -> Json.obj(
        "id" -> owner.option(pov.playerId),
        "color" -> color.name
      ),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.aiLevel
      ),
      "orientation" -> orientation.name,
      "pref" -> Json.obj(
        "animationDuration" -> animationDuration(pov, pref),
        "coords" -> pref.coords
      ).add("highlight" -> (pref.highlight || pref.isBlindfold))
        .add("destination" -> (pref.destination && !pref.isBlindfold)),
      "path" -> pov.game.turns,
      "userAnalysis" -> true,
      "puzzleEditor" -> true
    ).add("evalPut" -> me.??(evalCache.shouldPut))
  }

  private def blurs(game: Game, player: lidraughts.game.Player) =
    !player.blurs.isEmpty option {
      blursWriter.writes(player.blurs) +
        ("percent" -> JsNumber(game.playerBlurPercent(player.color)))
    }

  private def hold(player: lidraughts.game.Player) = player.holdAlert map { h =>
    Json.obj(
      "ply" -> h.ply,
      "mean" -> h.mean,
      "sd" -> h.sd
    )
  }

  private def clockJson(clock: Clock): JsObject =
    clockWriter.writes(clock) + ("moretime" -> JsNumber(moretimeSeconds))

  private def possibleMoves(pov: Pov): Option[Map[String, String]] = {
    (pov.game playableBy pov.player) option {
      if (pov.game.situation.ghosts > 0) {
        val move = pov.game.pdnMoves(pov.game.pdnMoves.length - 1)
        val destPos = draughts.Pos.posAt(move.substring(move.lastIndexOf('x') + 1))
        destPos match {
          case Some(dest) =>
            Map(dest -> pov.game.situation.destinationsFrom(dest)) map {
              case (from, dests) => from.key -> dests.mkString
            }
          case _ =>
            pov.game.situation.allDestinations map {
              case (from, dests) => from.key -> dests.mkString
            }
        }
      } else {
        pov.game.situation.allDestinations map {
          case (from, dests) => from.key -> dests.mkString
        }
      }
    }
  }

  private def possibleDrops(pov: Pov): Option[JsValue] = (pov.game playableBy pov.player) ?? {
    pov.game.situation.drops map { drops =>
      JsString(drops.map(_.key).mkString)
    }
  }

  private def captureLength(pov: Pov): Option[Int] =
    if (pov.game.situation.ghosts > 0) {
      val move = pov.game.pdnMoves(pov.game.pdnMoves.length - 1)
      val destPos = draughts.Pos.posAt(move.substring(move.lastIndexOf('x') + 1))
      destPos match {
        case Some(dest) => pov.game.situation.captureLengthFrom(dest)
        case _ => pov.game.situation.allMovesCaptureLength
      }
    } else
      pov.game.situation.allMovesCaptureLength

  private def animationFactor(pref: Pref): Float = pref.animation match {
    case 0 => 0
    case 1 => 0.5f
    case 2 => 1
    case 3 => 2
    case _ => 1
  }

  private def animationDuration(pov: Pov, pref: Pref) = math.round {
    animationFactor(pref) * baseAnimationDuration.toMillis * pov.game.finished.fold(
      1,
      math.max(0, math.min(1.2, ((pov.game.estimateTotalTime - 60) / 60) * 0.2))
    )
  }
}

object JsonView {

  case class WithFlags(
      opening: Boolean = false,
      movetimes: Boolean = false,
      division: Boolean = false,
      clocks: Boolean = false,
      blurs: Boolean = false
  )
}
