package views.html.user

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User

import controllers.routes

object mini {

  def apply(
    u: User,
    playing: Option[lidraughts.game.Pov],
    blocked: Boolean,
    followable: Boolean,
    rel: Option[lidraughts.relation.Relation],
    ping: Option[Int],
    crosstable: Option[lidraughts.game.Crosstable]
  )(implicit ctx: Context) = frag(
    div(cls := "title")(
      div(
        ping map bits.signalBars,
        u.profileOrDefault.countryInfo map { c =>
          val hasRoomForNameText = u.username.size + c.shortName.size < 20
          span(
            cls := (if (hasRoomForNameText) "country" else "country hint--top"),
            dataHint := (!hasRoomForNameText).option(c.name)
          )(
              img(cls := "flag", src := staticUrl(s"images/flags/${c.code}.png")),
              hasRoomForNameText option c.shortName
            )
        },
        userLink(u, withPowerTip = false)
      ),
      if (u.engine && !ctx.me.has(u) && !isGranted(_.UserSpy))
        div(cls := "warning", dataIcon := "j")(trans.thisPlayerUsesDraughtsComputerAssistance())
      else
        div(cls := "ratings")(u.best8Perfs map { showPerfRating(u, _) })
    ),
    ctx.userId map { myId =>
      frag(
        (myId != u.id && u.enabled) option div(cls := "actions")(
          a(cls := "button hint--bottom", dataHint := trans.watchGames.txt(), href := routes.User.tv(u.username))(
            iconTag("1")
          ),
          !blocked option frag(
            a(cls := "button hint--bottom", dataHint := trans.chat.txt(), href := s"${routes.Message.form()}?user=${u.username}")(
              iconTag("c")
            ),
            a(cls := "button hint--bottom", dataHint := trans.challengeToPlay.txt(), href := s"${routes.Lobby.home()}?user=${u.username}#friend")(
              iconTag("U")
            )
          ),
          views.html.relation.mini(u.id, blocked, followable, rel)
        ),
        crosstable.flatMap(_.nonEmpty) map { cross =>
          a(
            cls := "score hint--bottom",
            href := s"${routes.User.games(u.username, "me")}#games",
            dataHint := trans.nbGames.pluralTxt(cross.nbGames, cross.nbGames.localize)
          )(trans.yourScore(Html(s"""<strong>${cross.showScore(myId)}</strong> - <strong>${~cross.showOpponentScore(myId)}</strong>""")))
        }
      )
    },
    isGranted(_.UserSpy) option div(cls := "mod_info_box")(
      (u.lameOrTroll || u.disabled) option span(cls := "mod_marks")(mod.userMarks(u, None)),
      p(
        trans.nbGames.plural(u.count.game, u.count.game.localize),
        " ", momentFromNowOnce(u.createdAt)
      )
    ),
    (!ctx.pref.isBlindfold) ?? playing map { pov =>
      frag(
        gameFen(pov),
        div(cls := "game_legend")(
          playerText(pov.opponent, withRating = true),
          pov.game.clock map { c =>
            frag(" • ", c.config.show)
          }
        )
      )
    }
  )
}
