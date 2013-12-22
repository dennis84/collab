package collab

import akka.actor._
import akka.io.Tcp._
import akka.util.ByteString
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import scala.concurrent.duration._
import spray.json._

case object Online

class Room extends Actor {
  import context.dispatcher

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

    case f @ Frame(fin, rsv, Text | Binary, maskingKey, data) ⇒
      sockets foreach(_ ! f.copy(maskingKey = None))

    case x: ConnectionClosed =>
      if(sockets.contains(sender)) {
        sockets -= sender
      }
  }
}
