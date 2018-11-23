package com.github.leananeuber.hasher.actors.password_cracking

import akka.actor.{Actor, ActorLogging, Props}
import com.github.leananeuber.hasher.HashUtil
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CrackPasswordsCommand, PasswordsCrackedEvent}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.WorkerHandling


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

    // catch-all case: just log
    case m =>
      log.warning(s"$name: Received unknown message: $m")
  }

  def decrypt(secrets: Map[Int, String], range: Seq[Int]): Map[Int, Int] = {
    val rainbow = HashUtil.generateRainbow(range)
    log.info("Successfully generated rainbow table! Yay!")
    for {
      idHashTuple <- secrets
      realValue <- HashUtil.unhash(idHashTuple._2, rainbow)
    } yield idHashTuple._1 -> realValue
  }

}
