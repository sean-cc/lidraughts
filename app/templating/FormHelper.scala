package lidraughts.app
package templating

import play.api.data._
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.i18n.I18nDb

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Html = Html {
    s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args).render}</p>"""
  }

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map errMsg mkString
  }

  def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map errMsg

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")

  object form3 extends ui.ScalatagsPlay {

    import ui.ScalatagsTemplate._

    private val idPrefix = "form3"

    def id(field: Field): String = s"$idPrefix-${field.id}"

    private def groupLabel(field: Field) = label(cls := "form-label", `for` := id(field))
    private val helper = small(cls := "form-help")

    private def errors(errs: Seq[FormError])(implicit ctx: Context): Frag = errs map error
    private def errors(field: Field)(implicit ctx: Context): Frag = errors(field.errors)
    private def error(err: FormError)(implicit ctx: Context): Frag =
      p(cls := "error")(transKey(err.message, I18nDb.Site, err.args))

    private def validationModifiers(field: Field): Seq[Modifier] = field.constraints collect {
      /* Can't use constraint.required, because it applies to optional fields
         * such as `optional(nonEmptyText)`.
         * And we can't tell from the Field whether it's optional or not :(
         */
      // case ("constraint.required", _) => required
      case ("constraint.minLength", Seq(m: Int)) => minlength := m
      case ("constraint.maxLength", Seq(m: Int)) => maxlength := m
      case ("constraint.min", Seq(m: Int)) => min := m
      case ("constraint.max", Seq(m: Int)) => max := m
    }

    /* All public methods must return HTML
     * because twirl just calls toString on scalatags frags
     * and that escapes the content :( */

    val split = div(cls := "form-split")

    def group(
      field: Field,
      labelContent: Frag,
      klass: String = "",
      half: Boolean = false,
      help: Option[Frag] = None
    )(content: Field => Frag)(implicit ctx: Context): Html =
      div(cls := List(
        "form-group" -> true,
        "is-invalid" -> field.hasErrors,
        "form-half" -> half,
        klass -> klass.nonEmpty
      ))(
        groupLabel(field)(labelContent),
        content(field),
        errors(field),
        help map { helper(_) }
      )

    def input(field: Field, typ: String = "", klass: String = "", disabled: Boolean = false): BaseTagType =
      st.input(
        st.id := id(field),
        name := field.name,
        value := field.value,
        tpe := typ.nonEmpty.option(typ),
        cls := List("form-control" -> true, klass -> klass.nonEmpty),
        disabled option st.disabled
      )(validationModifiers(field))

    def inputHtml(field: Field, typ: String = "", klass: String = "", disabled: Boolean = false)(modifiers: Modifier*): Html =
      input(field, typ, klass, disabled)(modifiers)

    def checkbox(
      field: Field,
      labelContent: Frag,
      half: Boolean = false,
      help: Option[Frag] = None,
      disabled: Boolean = false
    ): Html =
      div(cls := List(
        "form-check form-group" -> true,
        "form-half" -> half
      ))(
        div(
          span(cls := "form-check-input")(
            st.input(
              st.id := id(field),
              name := field.name,
              value := "true",
              tpe := "checkbox",
              cls := "form-control cmn-toggle",
              field.value.has("true") option checked,
              disabled option st.disabled
            ),
            label(`for` := id(field))
          ),
          groupLabel(field)(labelContent)
        ),
        help map { helper(_) }
      )

    def select(
      field: Field,
      options: Iterable[(Any, String)],
      default: Option[String] = None,
      disabled: Boolean = false
    ): Html = frag(
      st.select(
        st.id := id(field),
        name := field.name,
        cls := "form-control",
        disabled option st.disabled
      )(validationModifiers(field))(
          default map { option(value := "")(_) },
          options.toSeq map {
            case (value, name) => option(
              st.value := value.toString,
              field.value.has(value.toString) option selected
            )(name)
          }
        ),
      disabled ?? hidden(field)
    )

    def textarea(
      field: Field,
      klass: String = ""
    )(modifiers: Modifier*): Html =
      st.textarea(
        st.id := id(field),
        name := field.name,
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )(validationModifiers(field))(modifiers)(~field.value)

    val actions = div(cls := "form-actions")
    val action = div(cls := "form-actions single")

    def submit(
      content: Frag,
      icon: Option[String] = Some("E"),
      nameValue: Option[(String, String)] = None,
      klass: String = "",
      confirm: Option[String] = None
    ): Html = button(
      tpe := "submit",
      dataIcon := icon,
      name := nameValue.map(_._1),
      value := nameValue.map(_._2),
      cls := List(
        "submit button" -> true,
        "text" -> icon.isDefined,
        "confirm" -> confirm.nonEmpty,
        klass -> klass.nonEmpty
      ),
      title := confirm
    )(content)

    def hidden(field: Field, value: Option[String] = None): Html =
      st.input(
        st.id := id(field),
        name := field.name,
        st.value := value.orElse(field.value),
        tpe := "hidden"
      )

    def password(field: Field, content: Frag)(implicit ctx: Context): Frag =
      group(field, content)(input(_, typ = "password")(required))

    def passwordModified(field: Field, content: Frag)(modifiers: Modifier*)(implicit ctx: Context): Frag =
      group(field, content)(input(_, typ = "password")(required)(modifiers))

    def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
      form.globalError map { err =>
        div(cls := "form-group is-invalid")(error(err))
      }

    def flatpickr(field: Field, withTime: Boolean = true): Html =
      input(field, klass = "flatpickr")(
        dataEnableTime := withTime,
        datatime24h := withTime
      )

    object file {
      def image(name: String): Html = st.input(tpe := "file", st.name := name, accept := "image/*")
      def pdn(name: String): Html = st.input(tpe := "file", st.name := name, accept := ".pdn")
    }
  }
}
