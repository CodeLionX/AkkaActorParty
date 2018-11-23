package com.github.leananeuber.hasher.actors.password_cracking

import com.github.leananeuber.hasher.protocols.SerializableMessage

object PasswordCrackingProtocol {

  case class StartCrackingCommand(secrets: Map[Int, String]) extends SerializableMessage

  case class CrackPasswordsCommand(secrets: Map[Int, String], hashRange: Seq[Int]) extends SerializableMessage

  case class PasswordsCrackedEvent(cleartexts: Map[Int, Int]) extends SerializableMessage

}
