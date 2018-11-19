package com.github.leananeuber.hasher.actors.password_cracking

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.{RegisterWorker, RegisterWorkerAck}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CrackPasswordsCommand, PasswordsCrackedEvent, StartCrackingCommand}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object PasswordCrackingMaster {

  val passwordRange: Range = 0 to 1000000

  val name = "pc-master"

  def props(nWorkers: Int): Props = Props(new PasswordCrackingMaster(nWorkers))

}


class PasswordCrackingMaster(nWorkers: Int) extends Actor with ActorLogging {
  import PasswordCrackingMaster._

  val name: String = self.path.name

  val workers: mutable.Set[ActorRef] = mutable.Set.empty
  val receivedResponses: mutable.Map[ActorRef, Map[Int, Int]] = mutable.Map.empty
  var receiverActor: ActorRef = _

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit = {
    log.info(s"Stopping $name and all associated workers")
    workers.foreach(_ ! PoisonPill)
  }

  override def receive: Receive = {
    case RegisterWorker =>
      workers.add(sender)
      context.watch(sender)
      sender ! RegisterWorkerAck
      log.info(s"$name: worker $sender registered")

    case StartCrackingCommand(secrets) =>
      if(workers.size < nWorkers) {
        // delay processing of message until all workers are ready
        context.system.scheduler.scheduleOnce(1 second, self, StartCrackingCommand(secrets))

      } else {
        receiverActor = sender
        val workPackages = splitRange()
        log.info(
          s"""$name: received command message
             |  available workers: ${workers.size}
             |  work packages:     ${workPackages.size}""".stripMargin)
        distributeWork(workPackages, secrets)
      }

    case PasswordsCrackedEvent(passwords) =>
      receivedResponses(sender) = passwords
      if(receivedResponses.size == workers.size) {
        val combinedPasswordMap = receivedResponses.values.reduce( _ ++ _)
        receiverActor ! PasswordsCrackedEvent(combinedPasswordMap)
      }

    case Terminated(actorRef) =>
      workers.remove(actorRef)
      log.warning(s"$name: worker $actorRef terminated - was it on purpose?")

    // catch-all case: just log
    case m =>
      log.warning(s"$name: Received unknown message: $m")
  }

  def splitRange(): Seq[Range] = {

    val remainder = passwordRange.end % nWorkers != 0
    val partitionSize = (if(remainder) 1 else 0) + (passwordRange.end / nWorkers)

    (0 until nWorkers).map(workerIndex => passwordRange.slice(workerIndex*partitionSize, (workerIndex+1)*partitionSize))
  }

  def distributeWork(workPackages: Seq[Range], secrets: Map[Int, String]): Unit = {
    workers.zip(workPackages).foreach{ case (ref, workPackages) =>
      ref ! CrackPasswordsCommand(secrets, workPackages)
    }
  }
}
