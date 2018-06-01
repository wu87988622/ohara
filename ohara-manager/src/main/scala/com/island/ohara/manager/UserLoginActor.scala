package com.island.ohara.manager

import akka.actor.{Actor, ActorLogging, Props}

object UserLoginActor {
  final case object GetLoginUsers
  final case class Login(name: String, password: String)
  final case class Logout(name: String)

  def props: Props = Props[UserLoginActor]
}

class UserLoginActor extends Actor with ActorLogging {
  import UserLoginActor._

  private val ALL_USERS = Seq(
    User("vito", Some("vito123")),
    User("jack", Some("jack123")),
    User("stana", Some("stana123")),
    User("chia", Some("chia123")),
    User("jimin", Some("jimin123"))
  )

  private var loginUsers = Set.empty[User]

  override def receive: Receive = {
    case GetLoginUsers =>
      sender() ! Users(loginUsers.toSeq)
    case Login(name, password) =>
      val msg = loginUsers.find(_.name == name) match {
        case Some(user) =>
          ReturnMessage(false, "already login")
        case _ =>
          ALL_USERS
            .find { user =>
              user.name == name && user.password.contains(password)
            }
            .map { user =>
              loginUsers += user
              ReturnMessage(true, "login success")
            }
            .getOrElse {
              ReturnMessage(false, "login fail")
            }
      }
      sender() ! msg
    case Logout(name) =>
      val msg = loginUsers.find(_.name == name) match {
        case Some(user) =>
          loginUsers -= user
          ReturnMessage(true, "logout success")
        case _ =>
          ReturnMessage(false, "logout fail.")
      }
      sender() ! msg
  }
}
