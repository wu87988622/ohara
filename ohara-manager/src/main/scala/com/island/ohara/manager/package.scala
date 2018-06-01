package com.island.ohara

/**
  * Created by vitojeng on 25/05/2018.
  */
package object manager {

  case class ReturnMessage[T](data: T, message: String)
  final case class User(name: String, password: Option[String])

  final case class Users(users: Seq[User])

}
