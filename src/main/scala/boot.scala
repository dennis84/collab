package collab

import akka.actor._
import akka.io.IO
import spray.can.Http
import spray.can.server.websockets.Sockets
import com.redis._

object Boot extends App {

  implicit val system = ActorSystem("socket-system")

  val port = sys.env.get("PORT") map(_.toInt) getOrElse 9000

  val redis = RedisClient("localhost", 6379)

  val server = system.actorOf(
    Props(new SocketServer(redis)),
    "socket-server")

  IO(Sockets) ! Http.Bind(
    listener = server,
    interface = "0.0.0.0",
    port = port)
}
