package com.github.leananeuber.hasher.actors


package object password_cracking {

  case class CrackPasswordsCommand(secrets: Map[Int, String])
  case class PasswordsCrackedEvent(cleartexts: Map[Int, Int])

}
