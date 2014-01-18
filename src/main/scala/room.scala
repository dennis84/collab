package collab

import akka.actor._
import akka.io.Tcp._
import akka.util.ByteString
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import scala.concurrent.duration._
import spray.json._

class Room extends Actor {
  import context.dispatcher
  import DefaultJsonProtocol._

  val sockets = scala.collection.mutable.ListBuffer.empty[ActorRef]

  context.system.scheduler.schedule(
    0.milliseconds, 1.seconds, self, Online)

  def receive = {
    case Sockets.Upgraded ⇒
      sockets += sender

    case Online ⇒
      sockets foreach(_ ! Frame(
        opcode = Text,
        maskingKey = None,
        data = ByteString(JsObject(
          "t" -> JsString("online"),
          "d" -> JsNumber(sockets.length)
        ).prettyPrint)))

    case Code(f, buffer, path, lang) ⇒
      sockets foreach(_ ! f.copy(maskingKey = None))

    case Cursor(f, x, y) ⇒
      sockets foreach(_ ! f.copy(maskingKey = None))

    case f @ Frame(fin, rsv, Text | Binary, maskingKey, data) ⇒
      f.stringData.asJson.asJsObject.getFields("t", "d") match {
        case Seq(JsString("code"), d)   ⇒ self ! Code(f, d)
        case Seq(JsString("cursor"), d) ⇒ self ! Cursor(f, d)
        case x ⇒ println("invalid maessage: " + x)
      }

    case x: ConnectionClosed =>
      if(sockets.contains(sender)) {
        sockets -= sender
      }
  }
}
