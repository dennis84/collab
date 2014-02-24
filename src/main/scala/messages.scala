package collab

import spray.json._

case class Message(
  val name: String,
  val data: JsValue,
  val sender: Member)

object Messages {

  def Code(member: Member, data: JsValue) =
    Message("code", data, member)

  def Join(member: Member) =
    Message("join", JsString(member.id), member)

  def Leave(member: Member) =
    Message("leave", JsString(member.id), member)

  def Members(members: Iterable[Member], sender: Member) =
    Message("members", JsArray(members.map { member ⇒ JsObject(
      "id"   -> JsString(member.id),
      "name" -> JsString(member.name getOrElse member.id))
    }.toList), sender)

  def UpdateMember(member: Member) =
    Message("update-member", JsObject(
      "id"   -> JsString(member.id),
      "name" -> JsString(member.name getOrElse member.id)
    ), member)
}
