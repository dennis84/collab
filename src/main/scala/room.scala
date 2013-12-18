package collab

import akka.actor._
import akka.io.Tcp._
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import spray.http._

class Room extends Actor {

  val sockets = scala.collection.mutable.ListBuffer.empty[ActorRef]

  def receive = {
    case Sockets.Upgraded ⇒
      sockets += sender

    case f @ Frame(fin, rsv, Text | Binary, maskingKey, data) ⇒
      sockets foreach(_ ! f.copy(maskingKey = None))

    case x: ConnectionClosed =>
      if(sockets.contains(sender)) {
        sockets -= sender
      }
  }
}
