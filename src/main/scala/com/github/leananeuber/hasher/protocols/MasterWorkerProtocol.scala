package com.github.leananeuber.hasher.protocols

import akka.actor.{Actor, ActorLogging, ActorRef, Address, Cancellable, Terminated}
import com.github.leananeuber.hasher.actors.Session
import com.github.leananeuber.hasher.protocols.SessionSetupProtocol.MasterReady

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object MasterWorkerProtocol {

  case object RegisterWorker extends SerializableMessage
  case object RegisterWorkerAck extends SerializableMessage

  case class SetupConnectionTo(address: Address) extends SerializableMessage


  trait MasterActor extends Actor with ActorLogging {
    val name: String = self.path.name
    val nWorkers: Int
    val session: ActorRef
  }

  trait MasterHandling { this: MasterActor =>

    val workers: mutable.Set[ActorRef] = mutable.Set.empty

    def handleWorkerRegistrations: Receive = {
      case RegisterWorker =>
        workers.add(sender)
        context.watch(sender)
        sender ! RegisterWorkerAck
        log.info(s"worker $sender registered")

        if(workers.size == nWorkers) {
          session ! MasterReady
          log.info(s"master $name knows all workers")
        }

      case Terminated(actorRef) =>
        workers.remove(actorRef)
        log.warning(s"worker $actorRef terminated - was it on purpose?")
    }

    def splitWork[T](work: Seq[T]): Seq[Seq[T]] = {
      val nItems = work.size
      val nPackages = workers.size
      val remainder = nItems % nPackages != 0
      val partitionSize = (if(remainder) 1 else 0) + (nItems / nPackages)

      (0 until nPackages).map( i => work.slice(i*partitionSize, (i+1)*partitionSize) )
    }

  }

  trait WorkerHandling { this: Actor with ActorLogging =>

    var registerWorkerCancellable: Cancellable = _

    def handleMasterCommunicationTo(masterActorName: String): Receive = {
      case SetupConnectionTo(address) =>
        val masterSelection = context.actorSelection(s"$address/user/${Session.sessionName}/$masterActorName")
        registerWorkerCancellable = context.system.scheduler.schedule(Duration.Zero, 5 seconds){
          masterSelection ! RegisterWorker
        }

      case RegisterWorkerAck =>
        registerWorkerCancellable.cancel()
        log.info(s"successfully registered at master actor")
    }
  }

}
