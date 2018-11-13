package com.github.leananeuber.hasher.actors.password_cracking

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import com.github.leananeuber.hasher.MasterWorkerProtocol.{RegisterWorker, RegisterWorkerAck}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CrackPasswordsCommand, PasswordsCrackedEvent}

import scala.collection.mutable


object PasswordCrackingMaster {

  def props: Props = Props[PasswordCrackingMaster]

}


class PasswordCrackingMaster extends Actor with ActorLogging {

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

    case CrackPasswordsCommand(secrets) =>
      receiverActor = sender
      val workPackages = splitWork(secrets)
      log.info(
        s"""$name: received command message
           |  available workers: ${workers.size}
           |  work packages:     ${workPackages.size}""".stripMargin)
      distributeWork(workPackages)

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

  def splitWork(secrets: Map[Int, String]): Seq[Map[Int, String]] = {
    val divisor = if(workers.size <= 1) 1 else workers.size - 1
    val rangeSize = (secrets.size / divisor) + 1
    (0 until workers.size).map( workerNumber =>
      secrets.slice(rangeSize * workerNumber, rangeSize * (workerNumber + 1))
    )
  }

  def distributeWork(workPackages: Seq[Map[Int, String]]): Unit = {
    workers.zipWithIndex.foreach{ case (ref, index) =>
      ref ! CrackPasswordsCommand(workPackages(index))
    }
  }
}
