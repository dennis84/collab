package collab

import akka.actor._
import akka.util.ByteString
import akka.io.Tcp._
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import spray.json._
import DefaultJsonProtocol._

class Room extends Actor {
  import context.dispatcher

  val members = collection.mutable.Map.empty[String, Member]

  def receive = {
    case Sockets.Upgraded ⇒
      val id = Rand nextString 8
      val member = Member(id, sender)
      members += (id -> member)
      sendToAll(Messages.Join(member))

    case m @ Message("code", data, _) ⇒ sendToAll(m)

    case m @ Message("cursor", data, _) ⇒ sendToAll(m)

    case m @ Message("members", data, member) ⇒
      sendToAll(Messages.Members(members.values, member))

    case Message("member", JsString(name), member) ⇒
      members.update(member.id, member.copy(name = Some(name)))
      sendToAll(Messages.UpdateNick(member))

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

  private def sendToAll(message: Message) =
    members.values foreach { _.socket ! Frame(
      opcode = Text,
      data = ByteString(JsObject(
        "t" -> JsString(message.name),
        "s" -> JsString(message.sender.id),
        "d" -> message.data
      ).prettyPrint))
    }

  private def withMember(id: String)(f: Member ⇒ Unit) =
    members get id foreach f

  private def withMember(s: ActorRef)(f: Member ⇒ Unit) =
    members.values find (_.socket == s) foreach f
}
