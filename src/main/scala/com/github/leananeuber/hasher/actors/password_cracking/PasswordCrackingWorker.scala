package com.github.leananeuber.hasher.actors.password_cracking

import akka.actor.{Actor, ActorLogging, Props}
import com.github.leananeuber.hasher.HashUtil
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol._
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
      log.info(s"checking passwords in range ${range.headOption} to ${range.lastOption}")
      sender() ! PasswordsCrackedEvent(decrypt(secrets, range))

    case CalculateLinearCombinationCommand(passwords, index) =>
      var result = solve(passwords.values.toBuffer, index)
      result match {
        case Some(prefixes) =>
          val resultmap = passwords.keys.zip(prefixes).toMap
          sender ! LinearCombinationCalculatedEvent(resultmap)
        case None => sender ! NoCombinationFound(passwords)
      }

    // catch-all case: just log
    case m =>
      log.warning(s"$name: Received unknown message: $m")

  }

  def decrypt(secrets: Map[Int, String], range: Seq[Int]): Map[Int, Int] = {
    val rainbow = HashUtil.generateRainbow(range)
    for {
      idHashTuple <- secrets
      realValue <- HashUtil.unhash(idHashTuple._2, rainbow)
    } yield idHashTuple._1 -> realValue
  }

  def solve(numbers: mutable.Buffer[Int], index: Long): Option[mutable.Buffer[Int]] = {
    var windowSize = PasswordCrackingMaster.partitionSize
    var a = index*windowSize
    var e = if(index == Long.MaxValue/PasswordCrackingMaster.partitionSize) Long.MaxValue - 1 else (index+1)*windowSize -1
    //log.info(s"Start worker $index: $a")
    //log.info(s"End worker $index: $e")
    while(a < e){
      val binary = a.toBinaryString
      val prefixes = mutable.ArrayBuffer.fill(64)(1)

      var i = 0
      for(j <- binary.length-1 to 0 by -1){
        if(binary.charAt(j) == '1')
          prefixes.update(i, -1)
        i += 1
      }
      if(sum(numbers, prefixes) == 0)
        return Some(prefixes)
      a+=1
    }
    None
  }

  def sum(numbers: mutable.Buffer[Int], prefixes: mutable.Buffer[Int]): Int = numbers
    .zip(prefixes)
    .map{case(p,n) => p*n}
    .sum

}
