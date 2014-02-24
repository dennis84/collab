package collab

import akka.actor._
import spray.can.Http
import spray.can.server.websockets._
import spray.http._
import HttpMethods._

class SocketServer(redis: com.redis.RedisClient) extends Actor {

  def actorRefFactory = context

  var rooms = Map.empty[String, ActorRef]

  def receive = {
    case _: Http.Connected ⇒
      sender ! Http.Register(self)

    case r @ HttpRequest(GET, Uri.Path(path), _, _, _) ⇒
      sender ! Sockets.UpgradeServer(
        Sockets.acceptAllFunction(r), getRoom(path drop 1))
  }

  private def getRoom(id: String): ActorRef =
    (rooms get id) getOrElse {
      val room = context.actorOf(Props(new Room(id, redis)), id)
      rooms = rooms + (id -> room)
      room
    }
}
