package collab

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.websockets.Sockets

object Boot extends App {

  implicit val system = ActorSystem("socket-system")

  val port = sys.env.get("PORT") map(_.toInt) getOrElse 9000

  val server = system.actorOf(
    Props[SocketServer],
    "socket-server")

  IO(Sockets) ! Http.Bind(
    listener = server,
    interface = "0.0.0.0",
    port = port)
}
