package com.github.leananeuber.hasher.actors.password_cracking

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CrackPasswordsCommand, PasswordsCrackedEvent, StartCrackingCommand}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.MasterHandling

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object PasswordCrackingMaster {

  val passwordRange: Range = 0 to 1000000

  val name = "pc-master"

  def props(nWorkers: Int, session: ActorRef): Props = Props(new PasswordCrackingMaster(nWorkers, session))

}


class PasswordCrackingMaster(nWorkers: Int, session: ActorRef) extends Actor with ActorLogging with MasterHandling {
  import PasswordCrackingMaster._

  val name: String = self.path.name
  val receivedResponses: mutable.Map[ActorRef, Map[Int, Int]] = mutable.Map.empty

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit = {
    log.info(s"Stopping $name and all associated workers")
    workers.foreach(_ ! PoisonPill)
  }

  override def receive: Receive = handleWorkerRegistrations orElse {
    case StartCrackingCommand(secrets) =>
      if(workers.size < nWorkers) {
        // delay processing of message until all workers are ready
        context.system.scheduler.scheduleOnce(1 second, self, StartCrackingCommand(secrets))

      } else {
        val workPackages = splitWork(passwordRange)
        log.info(
          s"""$name: received command message
             |  available workers: ${workers.size}
             |  work packages:     ${workPackages.size}""".stripMargin)
        distributeWork(workPackages, secrets)
      }

    case PasswordsCrackedEvent(passwords) =>
      log.info(s"$name: received ${passwords.size} passwords from $sender")
      receivedResponses(sender) = passwords
      if(receivedResponses.size == workers.size) {
        val combinedPasswordMap = receivedResponses.values.reduce( _ ++ _)
        session ! PasswordsCrackedEvent(combinedPasswordMap)
      }

    // catch-all case: just log
    case m =>
      log.warning(s"$name: Received unknown message: $m")
  }

  def distributeWork(workPackages: Seq[Seq[Int]], secrets: Map[Int, String]): Unit = {
    workers.zip(workPackages).foreach{ case (ref, packages) =>
      ref ! CrackPasswordsCommand(secrets, packages)
    }
  }
}
