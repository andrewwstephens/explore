// Copyright (c) 2016-2021 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package react.markdown

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import react.common._
import sttp.model.Uri

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation._

sealed trait ReactMarkdownPlugin extends js.Object

object ReactMarkdownPlugin {

  val RemarkGFM =
    if (scala.scalajs.runtime.linkingInfo.productionMode) RemarkGFMProd else RemarkGFMDev

  @js.native
  @JSImport("remark-gfm", JSImport.Namespace)
  object RemarkGFMDev extends js.Object with ReactMarkdownPlugin

  @js.native
  @JSImport("remark-gfm", JSImport.Default)
  object RemarkGFMProd extends js.Object with ReactMarkdownPlugin

}

final case class ReactMarkdown(
  content:                js.UndefOr[String],
  clazz:                  js.UndefOr[Css] = js.undefined,
  linkTarget:             js.UndefOr[String] = js.undefined,
  transformImageUri:      js.UndefOr[Uri => Uri] = js.undefined,
  plugins:                js.UndefOr[List[ReactMarkdownPlugin]] = js.undefined,
  override val modifiers: Seq[TagMod] = Seq.empty
) extends GenericFnComponentPA[ReactMarkdown.Props, ReactMarkdown] {
  override protected def cprops    = ReactMarkdown.props(this)
  override protected val component = ReactMarkdown.component
  override def addModifiers(modifiers: Seq[TagMod]) = copy(modifiers = this.modifiers ++ modifiers)
}

object ReactMarkdown {

  @js.native
  @JSImport("/react-markdown/react-markdown", JSImport.Default)
  object ReactMarkdown extends js.Function1[js.Any, js.Any] {
    def apply(arg: js.Any): js.Any = js.native
  }

  @js.native
  trait Props extends js.Object {
    var children: js.UndefOr[String]
    var className: js.UndefOr[String]
    var linkTarget: js.UndefOr[String]
    var transformImageUri: js.UndefOr[js.Function1[String, String]]
    var plugins: js.UndefOr[js.Array[js.Object]]
  }

  protected def props(p: ReactMarkdown): Props =
    rawprops(p.content, p.clazz, p.linkTarget, p.transformImageUri, p.plugins)

  protected def rawprops(
    content:           js.UndefOr[String],
    clazz:             js.UndefOr[Css],
    linkTarget:        js.UndefOr[String],
    transformImageUri: js.UndefOr[Uri => Uri],
    plugins:           js.UndefOr[List[ReactMarkdownPlugin]]
  ): Props = {
    val p = (new js.Object).asInstanceOf[Props]
    content.foreach(v => p.children = v)
    clazz.foreach(v => p.className = v.htmlClass)
    linkTarget.foreach(v => p.linkTarget = v)
    transformImageUri.foreach(v =>
      p.transformImageUri =
        ((u: String) => v(Uri.unsafeParse(u)).toString()): js.Function1[String, String]
    )
    plugins.foreach(l => p.plugins = l.map(_.asInstanceOf[js.Object]).toJSArray)
    p
  }

  private val component = JsFnComponent[Props, Children.None](ReactMarkdown)
}
