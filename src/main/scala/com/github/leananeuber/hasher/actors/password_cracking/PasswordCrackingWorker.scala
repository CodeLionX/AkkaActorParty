package com.github.leananeuber.hasher.actors.password_cracking

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import akka.actor.{Actor, ActorLogging, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CalculateLinearCombinationCommand, CrackPasswordsCommand, LinearCombinationCalculatedEvent, PasswordsCrackedEvent}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.WorkerHandling

import scala.collection.mutable
import scala.language.postfixOps


object PasswordCrackingWorker {

  def props: Props = Props[PasswordCrackingWorker]

  case class CrackingFailedException(m: String) extends RuntimeException(m)

}


class PasswordCrackingWorker extends Actor with ActorLogging with WorkerHandling {

  val name: String = self.path.name

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $name")

  override def receive: Receive = super.handleMasterCommunicationTo(PasswordCrackingMaster.name) orElse {

    case CrackPasswordsCommand(secrets, range) =>
      log.info(s"$name: checking passwords in range $range")
      sender() ! PasswordsCrackedEvent(decrypt(secrets, range))

    case CalculateLinearCombinationCommand(passwords, range) =>
      var result = solve(passwords.values.toBuffer, range)
      var resultmap = passwords.keys.zip(result)
      sender ! LinearCombinationCalculatedEvent(resultmap.toMap)


    // catch-all case: just log
    case m =>
      log.warning(s"$name: Received unknown message: $m")
  }

  def decrypt(secrets: Map[Int, String], range: Seq[Int]): Map[Int, Int] = {
    val rainbow = generateRainbow(range)
    log.info("Successfully generated rainbow table! Yay!")
    for {
      idHashTuple <- secrets
      realValue <- unhash(idHashTuple._2, rainbow)
    } yield idHashTuple._1 -> realValue
  }

  private def unhash(hexHash: String, rainbow: Map[String, Int]): Option[Int] = rainbow.get(hexHash)

  private def generateRainbow(range: Seq[Int]): Map[String, Int] = {
    range.map(x => hash(x) -> x).toMap
  }

  private def hash(number: Int) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedBytes = digest.digest(String.valueOf(number).getBytes(StandardCharsets.UTF_8))

    hashedBytes.map( byte =>
      ((byte & 0xff) + 0x100).toHexString.substring(1)
    ).mkString("")
  }

  def solve(numbers: mutable.Buffer[Int], range: Seq[String]): mutable.Buffer[Int] = {
    for(a <- range){
      val prefixes = mutable.ArrayBuffer.fill(numbers.length)(1)

      var i = 0
      for(j <- a.length-1 to 0 by -1){
        if(a.charAt(j) == '1')
          prefixes.update(i, -1)
        i += 1
      }
      if(sum(numbers, prefixes) == 0)
        prefixes

    }
    throw new RuntimeException("Prefix not found!")
  }

  def sum(numbers: mutable.Buffer[Int], prefixes: mutable.Buffer[Int]): Int = numbers
    .zip(prefixes)
    .map{case(p,n) => p*n}
    .sum
}
