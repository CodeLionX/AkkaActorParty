package com.github.leananeuber.hasher.protocols

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

import scala.collection.mutable

object MasterWorkerProtocol {

  case object RegisterWorker extends SerializableMessage
  case object RegisterWorkerAck extends SerializableMessage


  trait MasterHandling { self: Actor with ActorLogging =>

    val workers: mutable.Set[ActorRef] = mutable.Set.empty

    override def receive: Receive = {
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

}
