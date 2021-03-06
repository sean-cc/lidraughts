package views
package html.tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.richText

import controllers.routes

object side {

  private val separator = " • "

  def apply(
    tour: lidraughts.tournament.Tournament,
    verdicts: lidraughts.tournament.Condition.All.WithVerdicts,
    streamers: Set[lidraughts.user.User.ID],
    shieldOwner: Option[lidraughts.tournament.TournamentShield.OwnerId]
  )(implicit ctx: Context) = frag(
    div(cls := "side_box padded")(
      div(cls := "game_infos", dataIcon := tour.perfType.map(_.iconChar.toString))(
        div(cls := "header")(
          (isGranted(_.ManageTournament) || (ctx.userId.has(tour.createdBy) && tour.isCreated)) option frag(
            " ",
            a(href := routes.Tournament.edit(tour.id), title := trans.editTournament.txt(), style := "float:right")(iconTag("%"))
          ),
          span(cls := "setup")(
            tour.clock.show,
            separator,
            if (tour.variant.exotic) {
              views.html.game.bits.variantLink(
                tour.variant,
                tour.variant.name,
                cssClass = "hint--top"
              )
            } else tour.perfType.map(_.name),
            (!tour.position.initial) ?? s"• ${trans.thematic.txt()}",
            separator,
            tour.durationString
          ),
          tour.mode.fold(trans.casualTournament, trans.ratedTournament)(),
          separator,
          systemName(tour.system).capitalize,
          " ",
          a(cls := "blue help", href := routes.Tournament.help(tour.system.toString.toLowerCase.some), dataIcon := "")
        )
      ),
      tour.spotlight map { s =>
        div(cls := "game_infos spotlight")(
          lidraughts.common.String.html.markdownLinks(s.description),
          shieldOwner map { owner =>
            p(cls := "defender", dataIcon := "5")(
              "Defender:",
              userIdLink(owner.value.some)
            )
          }
        )
      },
      tour.description map { d =>
        div(cls := "game_infos spotlight")(richText(d))
      },
      verdicts.relevant option div(dataIcon := "7", cls := List(
        "game_infos conditions" -> true,
        "accepted" -> (ctx.isAuth && verdicts.accepted),
        "refused" -> (ctx.isAuth && !verdicts.accepted)
      ))(
        (verdicts.list.size < 2) option p(trans.conditionOfEntry()),
        verdicts.list map { v =>
          p(cls := List(
            "condition text" -> true,
            "accepted" -> v.verdict.accepted,
            "refused" -> !v.verdict.accepted
          ))(v.condition match {
            case lidraughts.tournament.Condition.TeamMember(teamId, teamName) =>
              trans.mustBeInTeam(teamLink(teamId, lidraughts.common.String.html.escapeHtml(teamName), withIcon = false))
            case c => c.name(ctx.lang)
          })
        }
      ),
      tour.noBerserk option div(cls := "text", dataIcon := "`")(trans.noBerserkAllowed()),
      !tour.isScheduled option frag(trans.by(userIdLink(tour.createdBy.some)), br),
      !tour.isStarted option absClientDateTime(tour.startsAt),
      (!tour.position.initial) ?? frag(
        br, br,
        a(target := "_blank", href := tour.position.url)(
          strong(tour.position.eco),
          s" ${tour.position.name}"
        )
      )
    ),
    streamers.toList map { id =>
      a(href := routes.Streamer.show(id), cls := "context-streamer text side_box", dataIcon := "")(
        usernameOrId(id),
        " is streaming"
      )
    }
  )
}
