package com.github.leananeuber.hasher.actors.password_cracking

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.github.leananeuber.hasher.MasterWorkerProtocol.{RegisterWorker, RegisterWorkerAck}
import com.github.leananeuber.hasher.actors.Reaper

import scala.collection.mutable


object PasswordCrackingMaster {

  def props: Props = Props[PasswordCrackingMaster]

}


class PasswordCrackingMaster extends Actor with ActorLogging {

  val name: String = this.getClass.getSimpleName

  val workers: mutable.Set[ActorRef] = mutable.Set.empty
  val receivedResponses: mutable.Map[ActorRef, Map[Int, Int]] = mutable.Map.empty
  var receiverActor: ActorRef = _

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $name")

  override def receive: Receive = {
    case RegisterWorker =>
      workers.add(sender)
      context.watch(sender)
      sender ! RegisterWorkerAck
      log.info(s"$name: worker $sender registered")

    case CrackPasswordsCommand(secrets) =>
      receiverActor = sender
      val workPackages = splitWork(secrets)
      distributeWork(workPackages)

    case PasswordsCrackedEvent(passwords) =>
      receivedResponses(sender) = passwords
      if(receivedResponses.size == workers.size) {
        val combinedPasswordMap = receivedResponses.values.reduce( _ ++ _)
        receiverActor ! PasswordsCrackedEvent(combinedPasswordMap)
      }

    case Terminated =>
      workers.remove(sender)
      log.warning(s"$name: worker $sender terminated - was it on purpose?")

    // catch-all case: just log
    case m =>
      log.warning(s"$name: Received unknown message: $m")
  }

  def splitWork(secrets: Map[Int, String]): Seq[Map[Int, String]] = {
    val rangeSize = (secrets.size / (workers.size - 1)) + 1
    (0 to workers.size).map( workerNumber =>
      secrets.slice(0 * workerNumber, rangeSize * (workerNumber + 1))
    )
  }

  def distributeWork(workPackages: Seq[Map[Int, String]]): Unit = {
    workers.zipWithIndex.foreach{ case (ref, index) =>
      ref ! workPackages(index)
    }
  }
}
