package com.github.leananeuber.hasher.actors.hash_mining

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.Settings
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.hash_mining.HashMiningProtocol.{EncryptCommand, EncryptedEvent, HashesMinedEvent, MineHashesFor}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.MasterHandling

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object HashMiningMaster {

  val name = "hm-master"

  def props(nWorkers: Int, session: ActorRef): Props = Props(new HashMiningMaster(nWorkers, session))

}

class HashMiningMaster(nWorkers: Int, session: ActorRef) extends Actor with ActorLogging with MasterHandling {

  val prefixLength = Settings(context.system).prefixLength

  val name: String = self.path.name

  val receivedResponses: mutable.Map[ActorRef, Map[Int, String]] = mutable.Map.empty

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit = {
    log.info(s"Stopping $name and all associated workers")
    workers.foreach(_ ! PoisonPill)
  }

  override def receive: Receive = handleWorkerRegistrations orElse {

    case MineHashesFor(content) =>
      if(workers.size < nWorkers) {
        // delay processing of message until all workers are ready
        context.system.scheduler.scheduleOnce(1 second, self, MineHashesFor(content))

      } else {
        // distribute work
        val workPackages = splitWork(content.toSeq).map(_.toMap)
        workers.zip(workPackages).foreach { case (ref, work) =>
          ref ! EncryptCommand(work, prefixLength)
        }
      }

    case EncryptedEvent(hashes) =>
      log.info(s"received ${hashes.size} hashes from $sender")
      receivedResponses(sender) = hashes

      if(receivedResponses.size == workers.size) {
        val allHashes = receivedResponses.values.reduce(_ ++ _)
        session ! HashesMinedEvent(allHashes)
      }

    // catch-all case: just log
    case m =>
      log.warning(s"received unknown message: $m")
  }
}
