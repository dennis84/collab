package collab

import akka.actor._
import akka.io.Tcp._
import akka.util.ByteString
import spray.can.server.websockets._
import spray.can.server.websockets.model._
import spray.can.server.websockets.model.OpCode._
import spray.json._
import DefaultJsonProtocol._

case class Member(
  val id: String,
  val socket: ActorRef,
  val name: Option[String] = None)

case class Message(
  val name: String,
  val data: JsValue,
  val sender: Member,
  val frame: Option[Frame] = None)

class Room extends Actor {
  import context.dispatcher

  var members = scala.collection.mutable.ListBuffer.empty[Member]

  def receive = {
    case Sockets.Upgraded ⇒
      val id = Rand.nextString(8)
      val member = Member(id, sender)
      members += member
      sendToAll(Message("join", JsString(id), member))

    case m @ Message("code", data, sender, _) ⇒ sendToAll(m)

    case m @ Message("cursor", data, sender, _) ⇒ sendToAll(m)

    case m @ Message("members", data, sender, _) ⇒
      sendToAll(m.copy(data = members.map(member ⇒ Map(
        "id"   -> member.id,
        "name" -> member.name.getOrElse(member.id)
      )).toList.toJson))

    case Message("member", JsString(name), sender, _) ⇒ for {
      (member, i) ← members.zipWithIndex.find(_._1.socket == sender.socket)
    } yield {
      members.update(i, member.copy(name = Some(name)))
      sendToAll(Message("member", JsObject(
        "id" -> JsString(member.id),
        "name" -> JsString(name)
      ), sender))
    }

    case f @ Frame(fin, rsv, Text | Binary, maskingKey, data) ⇒ for {
      member <- members find (_.socket == sender)
    } yield {
      f.stringData.asJson.asJsObject.getFields("t", "d") match {
        case Seq(JsString(t), d) ⇒ self ! Message(t, d, member)
        case Seq(JsString(t)) ⇒ self ! Message(t, JsNull, member)
      }
    }

    case x: ConnectionClosed ⇒ for {
      member ← members.find(_.socket == sender)
    } yield {
      members -= member
      sendToAll(Message("leave", JsString(member.id), member))
    }
  }

  def sendToAll(message: Message) =
    members foreach { _.socket !
      (message.frame getOrElse Frame(opcode = Text, maskingKey = None)).copy(
        data = ByteString(JsObject(
          "t" -> JsString(message.name),
          "s" -> JsString(message.sender.id),
          "d" -> message.data
        ).prettyPrint))
    }
}
