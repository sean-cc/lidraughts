package views.html
package practice

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object index {

  def apply(data: lidraughts.practice.UserPractice)(implicit ctx: Context) = views.html.base.layout(
    title = "Practice draughts positions",
    side = Some(
      div(id := "practice_side", cls := "side_box")(
        div(cls := "home")(
          i(cls := "fat"),
          h1("Practice"),
          h2("makes your draughts perfect"),
          div(cls := "progress")(
            div(cls := "text")("Progress: ", data.progressPercent, "%"),
            div(cls := "bar", style := s"width: ${data.progressPercent}%")
          )
        /*form(id := "practice_reset", cls := "actions", action := routes.Practice.reset, method := "post")(
              if (ctx.isAuth) (data.nbDoneChapters > 0) option a(cls := "do-reset")("Reset my progress")
              else a(href := routes.Auth.signup)("Sign up to save your progress")
            )*/
        )
      )
    ),
    moreCss = cssTag("practice.css"),
    moreJs = embedJs(s"""$$('#practice_reset .do-reset').on('click', function() {
if (confirm('You will lose your practice progress!')) this.parentNode.submit();
});""")
  /*openGraph = lidraughts.app.ui.OpenGraph(
      title = "Practice your draughts",
      description = "Learn how to master the most common draughts positions",
      url = s"$netBaseUrl${routes.Practice.index}"
    ).some*/
  ) {
      div(id := "practice_app")(
        div(cls := "sections")(
          data.structure.sections.map { section =>
            div(cls := "section")(
              h2(section.name),
              div(cls := "studies")( /*section.studies.map { stud =>
                  val prog = data.progressOn(stud.id)
                  a(
                    cls := s"study ${if (prog.complete) "done" else "ongoing"}",
                    href := routes.Practice.show(section.id, stud.slug, stud.id.value)
                  )(
                      ctx.isAuth option span(cls := "ribbon-wrapper")(
                        span(cls := "ribbon")(prog.done, " / ", prog.total)
                      ),
                      i(cls := s"practice icon ${stud.id}"),
                      span(cls := "text")(
                        h3(stud.name),
                        em(stud.desc)
                      )
                    )
                }*/ )
            )
          }
        )
      )
    }
}
