package views.html
package user

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User

import controllers.routes

object bits {

  def miniClosed(u: User)(implicit ctx: Context) = frag(
    div(cls := "title")(userLink(u, withPowerTip = false)),
    div(style := "padding: 20px 8px; text-align: center")(trans.thisAccountIsClosed())
  )

  def signalBars(v: Int) = raw {
    val bars = (1 to 4).map { b =>
      s"""<i${if (v < b) " class=\"off\"" else ""}></i>"""
    } mkString ""
    val title = v match {
      case 1 => "Poor connection"
      case 2 => "Decent connection"
      case 3 => "Good connection"
      case _ => "Excellent connection"
    }
    s"""<signal data-hint="$title" class="q$v hint--top">$bars</signal>"""
  }
}
