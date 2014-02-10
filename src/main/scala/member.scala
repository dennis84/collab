package collab

import akka.actor._

case class Member(
  val id: String,
  val socket: ActorRef,
  val name: Option[String] = None)
