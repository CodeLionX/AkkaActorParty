package com.github.leananeuber.hasher.actors.password_cracking

object PasswordCrackingProtocol {

  case class StartCrackingCommand(secrets: Map[Int, String])

  case class CrackPasswordsCommand(secrets: Map[Int, String], hashRange: Range)

  case class PasswordsCrackedEvent(cleartexts: Map[Int, Int])

}
