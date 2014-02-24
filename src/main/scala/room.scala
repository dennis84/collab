package collab

import akka.actor._
import akka.util.ByteString
import akka.io.Tcp._
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import spray.json._
import DefaultJsonProtocol._
import com.redis._

class Room(roomId: String, redis: RedisClient) extends Actor {
  import context.dispatcher

  val members = collection.mutable.Map.empty[String, Member]

  def receive = {
    case Sockets.Upgraded ⇒
      val id = Rand nextString 8
      val member = Member(id, sender)
      members += (id -> member)
      sendToAll(Messages.Join(member))
      for {
        maybeData ← redis get roomId
      } yield for {
        data ← maybeData
      } yield {
        sendTo(member, Messages.Code(member, data.asJson))
      }

    case m @ Message("code", data, _) ⇒
      redis.set(roomId, data.prettyPrint)
      redis.expire(roomId, 60)
      sendToAll(m)

    case m @ Message("cursor", data, _) ⇒ sendToAll(m)

    case m @ Message("members", data, member) ⇒
      sendToAll(Messages.Members(members.values, member))

    case Message("update-nick", JsString(name), member) ⇒
      val updated = member.copy(name = Some(name))
      members.update(member.id, updated)
      sendToAll(Messages.UpdateMember(updated))

    case f @ Frame(fin, rsv, Text | Binary, maskingKey, data) ⇒ 
      withMember(sender) { member ⇒
        f.stringData.asJson.asJsObject.getFields("t", "d") match {
          case Seq(JsString(t), d) ⇒ self ! Message(t, d, member)
          case Seq(JsString(t)) ⇒ self ! Message(t, JsNull, member)
        }
      }

    case x: ConnectionClosed ⇒
      withMember(sender) { member ⇒
        members -= member.id
        sendToAll(Messages.Leave(member))
      }
  }

  private def sendToAll(message: Message) = {
    val frame = makeFame(message)
    members.values foreach (_.socket ! frame)
  }

  private def sendTo(member: Member, message: Message) =
    member.socket ! makeFame(message)

  private def makeFame(message: Message) = Frame(
    opcode = Text,
    data = ByteString(JsObject(
      "t" -> JsString(message.name),
      "s" -> JsString(message.sender.id),
      "d" -> message.data
    ).prettyPrint))

  private def withMember(id: String)(f: Member ⇒ Unit) =
    members get id foreach f

  private def withMember(s: ActorRef)(f: Member ⇒ Unit) =
    members.values find (_.socket == s) foreach f
}
