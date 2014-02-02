package collab

import akka.actor._
import akka.io.Tcp._
import akka.util.ByteString
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import scala.concurrent.duration._
import spray.json._

case class Member(
  var name: String,
  val socket: ActorRef)

case class Message(
  val name: String,
  val data: JsValue,
  val frame: Frame,
  val sender: ActorRef)

case object Online

class Room extends Actor {
  import context.dispatcher
  import DefaultJsonProtocol._

  val members = scala.collection.mutable.ListBuffer.empty[Member]

  context.system.scheduler.schedule(
    0.milliseconds, 1.seconds, self, Online)

  def receive = {
    case Sockets.Upgraded ⇒
      members += Member(Rand.nextString(8), sender)

    case Message("code", data, frame, sender) ⇒
      members foreach (_.socket ! frame.copy(maskingKey = None))

    case Message("cursor", data, frame, sender) ⇒
      members foreach (_.socket ! frame.copy(maskingKey = None))

    case Message("user", JsString(name), frame, sender) ⇒
      members find (_.socket == sender) map (_.name = name)

    case Online ⇒
      members foreach (_.socket ! Frame(
        opcode = Text,
        maskingKey = None,
        data = ByteString(JsObject(
          "t" -> JsString("online"),
          "d" -> JsArray(members.map(m ⇒ JsString(m.name)).toList)
        ).prettyPrint)))

    case f @ Frame(fin, rsv, Text | Binary, maskingKey, data) ⇒
      f.stringData.asJson.asJsObject.getFields("t", "d") match {
        case Seq(JsString(t), d) ⇒ self ! Message(t, d, f, sender)
      }

    case x: ConnectionClosed ⇒
      members find (_.socket == sender) map (s ⇒ members -= s)
  }
}
