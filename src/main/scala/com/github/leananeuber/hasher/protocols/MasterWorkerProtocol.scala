package com.github.leananeuber.hasher.protocols

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Address, Cancellable, Terminated}
import com.github.leananeuber.hasher.actors.Session
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingMaster

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object MasterWorkerProtocol {

  case object RegisterWorker extends SerializableMessage
  case object RegisterWorkerAck extends SerializableMessage

  case class SetupConnectionTo(masterSelection: ActorSelection) extends SerializableMessage


  trait MasterHandling { this: Actor with ActorLogging =>

    val workers: mutable.Set[ActorRef] = mutable.Set.empty

    def handleWorkerRegistrations: Receive = {
      case RegisterWorker =>
        workers.add(sender)
        context.watch(sender)
        sender ! RegisterWorkerAck
        log.info(s"worker $sender registered")

      case Terminated(actorRef) =>
        workers.remove(actorRef)
        log.warning(s"worker $actorRef terminated - was it on purpose?")
    }
  }

  trait WorkerHandling { this: Actor with ActorLogging =>

    var registerWorkerCancellable: Cancellable = _

    def handleMasterCommunication: Receive = {
      case SetupConnectionTo(masterSelection) =>
        registerWorkerCancellable = context.system.scheduler.schedule(Duration.Zero, 5 seconds){
          masterSelection ! RegisterWorker
        }

      case RegisterWorkerAck =>
        registerWorkerCancellable.cancel()
        log.info(s"successfully registered at master actor")
    }
  }

}
