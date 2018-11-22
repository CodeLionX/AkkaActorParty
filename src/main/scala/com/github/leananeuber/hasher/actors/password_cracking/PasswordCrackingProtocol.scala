package com.github.leananeuber.hasher.actors.password_cracking

object PasswordCrackingProtocol {

  case class StartCrackingCommand(secrets: Map[Int, String])

  case class CrackPasswordsCommand(secrets: Map[Int, String], hashRange: Seq[Int])

  case class PasswordsCrackedEvent(cleartexts: Map[Int, Int])

}
