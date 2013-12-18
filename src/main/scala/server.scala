package collab

import akka.actor._
import akka.io.Tcp._
import spray.can.server.websockets._
import spray.http._
import spray.routing._
import HttpMethods._

class SocketServer extends Actor with HttpService {

  def actorRefFactory = context

  var rooms = Map.empty[String, ActorRef]

  def receive = {
    case x: Connected ⇒
      sender ! Register(self)

    case r @ HttpRequest(GET, Uri.Path(path), _, _, _) =>
      sender ! Sockets.UpgradeServer(
      Sockets.acceptAllFunction(r), getRoom(path.drop(1)))
  }

  def getRoom(id: String): ActorRef = (for {
    room ← rooms find(_._1 == id)
  } yield room._2) getOrElse {
    val room = context.actorOf(Props[Room], id)
    rooms = rooms + (id -> room)
    room
  }
}
