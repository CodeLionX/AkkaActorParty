package com.github.leananeuber.hasher.actors.password_cracking

object PasswordCrackingProtocol {

  case class CrackPasswordsCommand(secrets: Map[Int, String])

  case class PasswordsCrackedEvent(cleartexts: Map[Int, Int])

}
