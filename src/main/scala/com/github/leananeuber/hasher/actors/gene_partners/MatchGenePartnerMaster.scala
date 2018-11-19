package com.github.leananeuber.hasher.actors.gene_partners

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerMaster.StartMatching
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerWorker.{CalculateLCSLengths, LCSLengthsCalculated}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.MasterHandling

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object MatchGenePartnerMaster {

  val name = "mgp-master"

  def props(nWorkers: Int, session: ActorRef): Props = Props(new MatchGenePartnerMaster(nWorkers, session))

  case class StartMatching(genes: Map[Int, String])

}


class MatchGenePartnerMaster(nWorkers: Int, session: ActorRef) extends Actor with ActorLogging with MasterHandling {

  val name: String = self.path.name

  val receivedResponses: mutable.Map[ActorRef, Map[(Int, Int), Int]] = mutable.Map.empty

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit = {
    log.info(s"Stopping $name and all associated workers")
    workers.foreach(_ ! PoisonPill)
  }

  override def receive: Receive = super.handleWorkerRegistrations orElse {

    case StartMatching(genes) =>
      if(workers.size < nWorkers) {
        // delay processing of message until all workers are ready
        context.system.scheduler.scheduleOnce(1 second, self, StartMatching(genes))

      } else {
        // generate index combinations
        val n = genes.keys.max
        val combinations = (0 until n).flatMap(i => (i+1 until n).map(j => i -> j))
        val ranges = splitWork(combinations)

        workers.zip(ranges).foreach { case (ref, indexRange) =>
          ref ! CalculateLCSLengths(genes, indexRange)
        }
      }

    case LCSLengthsCalculated(lengths) =>
      log.info(s"$name: received ${lengths.size} lenghts from $sender")
      receivedResponses(sender) = lengths
      if(receivedResponses.size == workers.size) {
        val combinedLengthTable = receivedResponses.values.reduce( _ ++ _)
        log.info(s"The whole table:\n$combinedLengthTable")
        session ! "FINISHED!"
      }
  }
}
