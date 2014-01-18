package collab

import spray.json._
import DefaultJsonProtocol._
import spray.can.server.websockets.model.Frame

case object Online

case class Code(
  val frame: Frame,
  val buffer: String,
  val path: String,
  val lang: String)

object Code {

  def apply(f: Frame, d: JsValue): Code =
    d.asJsObject.getFields("buffer", "path", "lang") match {
      case Seq(
        JsString(b),
        JsString(p),
        JsString(l)
      ) ⇒ Code(f, b, p, l)
    }
}

case class Cursor(
  val frame: Frame,
  val x: Long,
  val y: Long)

object Cursor {

  def apply(f: Frame, d: JsValue): Cursor =
    d.asJsObject.getFields("x", "y") match {
      case Seq(
        JsNumber(x),
        JsNumber(y)
      ) ⇒ Cursor(f, x.toLong, y.toLong)
    }
}
