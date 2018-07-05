package com.island.ohara.manager

private case class ReturnMessage[T](data: T, message: String)
private case class User(name: String, password: Option[String])
private case class Users(users: Seq[User])
