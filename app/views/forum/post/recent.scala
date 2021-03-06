package views
package html.forum.post

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object recent {

  def apply(posts: List[lidraughts.forum.MiniForumPost])(implicit ctx: Context) = ol(
    posts map { p =>
      li(
        a(dataIcon := p.isTeam.option("f"), cls := "post_link text", href := routes.ForumPost.redirect(p.postId), title := p.topicName)(
          shorten(p.topicName, 30)
        ),
        " ",
        userIdLink(p.userId, withOnline = false),
        " ",
        span(cls := "extract")(shorten(p.text, 70))
      )
    }
  )
}
