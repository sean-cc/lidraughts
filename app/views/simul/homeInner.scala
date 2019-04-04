package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object homeInner {

  def apply(
    createds: List[lidraughts.simul.Simul],
    starteds: List[lidraughts.simul.Simul],
    finisheds: List[lidraughts.simul.Simul]
  )(implicit ctx: Context) =
    div(cls := "box")(
      h1(trans.simultaneousExhibitions.frag()),
      table(cls := "slist slist-pad")(
        thead(
          tr(
            th(colspan := 2, cls := "large")(trans.createdSimuls.frag()),
            th(trans.host.frag()),
            th(trans.players.frag())
          )
        ),
        tbody(
          createds.map { sim =>
            tr(cls := "scheduled")(
              iconTd(sim),
              simTd(sim),
              simHost(sim),
              td(cls := "text", dataIcon := "r")(sim.applicants.size)
            )
          },
          ctx.isAuth option tr(cls := "create")(
            td(colspan := "4")(
              a(href := routes.Simul.form(), cls := "action button text")(trans.hostANewSimul.frag())
            )
          )
        ),
        starteds.nonEmpty option (frag(
          thead(
            tr(
              th(colspan := 2, cls := "large")(trans.eventInProgress.frag()),
              th(trans.host.frag()),
              th(trans.players.frag())
            )
          ),
          starteds.map { sim =>
            tr(
              iconTd(sim),
              simTd(sim),
              simHost(sim),
              td(cls := "text", dataIcon := "r")(sim.pairings.size)
            )
          }
        )),
        thead(
          tr(
            th(colspan := 2, cls := "large")(trans.finished.frag()),
            th(trans.host.frag()),
            th(trans.players.frag())
          )
        ),
        tbody(
          finisheds.map { sim =>
            tr(
              iconTd(sim),
              simTd(sim),
              simHost(sim),
              td(cls := "text", dataIcon := "r")(sim.pairings.size)
            )
          }
        )
      )
    )

  private def iconTd(sim: lidraughts.simul.Simul) =
    td(cls := List(
      "variant-icons" -> true,
      "rich" -> sim.variantRich
    ))(
      sim.perfTypes.map { pt =>
        span(cls := "is-gold", dataIcon := pt.iconChar)
      }
    )

  private def simTd(sim: lidraughts.simul.Simul)(implicit ctx: Context) =
    td(cls := "header")(
      a(href := routes.Simul.show(sim.id))(
        span(cls := "name")(sim.fullName),
        bits.setup(sim)
      )
    )

  private def simHost(sim: lidraughts.simul.Simul) =
    td(cls := "host")(
      userIdLink(sim.hostId.some, withOnline = false),
      br,
      strong(sim.hostRating)
    )
}
