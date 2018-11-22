package com.github.leananeuber.hasher.actors.password_cracking

import scala.collection.immutable.NumericRange
import scala.collection.mutable

object PasswordCrackingProtocol {

  case class StartCrackingCommand(secrets: Map[Int, String])

  case class CrackPasswordsCommand(secrets: Map[Int, String], hashRange: Range)

  case class PasswordsCrackedEvent(cleartexts: Map[Int, Int])

  case class CalculateLinearCombinationCommand(passwords: Map[Int, Int], range: IndexedSeq[Long])

  case class StartCalculateLinearCombinationCommand(cleartexts: Map[Int, Int])

  case class LinearCombinationCalculatedEvent(combination: Map[Int, Int])

}
