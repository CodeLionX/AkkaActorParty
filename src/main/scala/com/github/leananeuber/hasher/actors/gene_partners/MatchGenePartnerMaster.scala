package com.github.leananeuber.hasher.actors.gene_partners

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerMaster.{MatchedGenes, StartMatchingGenes}
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerWorker.{CalculateLCSLengths, LCSLengthsCalculated}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.MasterHandling

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object MatchGenePartnerMaster {

  val name = "mgp-master"

  def props(nWorkers: Int, session: ActorRef): Props = Props(new MatchGenePartnerMaster(nWorkers, session))

  case class StartMatchingGenes(genes: Map[Int, String])

  case class MatchedGenes(genePartners: Map[Int, Int])

}


class MatchGenePartnerMaster(nWorkers: Int, session: ActorRef) extends Actor with ActorLogging with MasterHandling {

  val name: String = self.path.name

  val receivedResponses: mutable.Map[ActorRef, Map[Int, (Int, Int)]] = mutable.Map.empty
  var n: Int = _

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit = {
    log.info(s"Stopping $name and all associated workers")
    workers.foreach(_ ! PoisonPill)
  }

  override def receive: Receive = handleWorkerRegistrations orElse {

    case StartMatchingGenes(genes) =>
      if(workers.size < nWorkers) {
        // delay processing of message until all workers are ready
        context.system.scheduler.scheduleOnce(1 second, self, StartMatchingGenes(genes))

      } else {
        // generate index combinations
        n = genes.keys.max
        val combinations = (1 to n).flatMap(i => (i+1 to n).map(j => i -> j))
        val ranges = splitWork(combinations)

        workers.zip(ranges).foreach { case (ref, indexRange) =>
          ref ! CalculateLCSLengths(genes, indexRange)
        }
      }

    case LCSLengthsCalculated(lengths) =>
      log.info(s"$name: received ${lengths.size} lenghts from $sender")
      receivedResponses(sender) = lengths

      if(receivedResponses.size == workers.size) {
        val genePartners = (for{
          id <- 1 to n
          potPartners = receivedResponses.values.flatMap(_.get(id))
          if potPartners.nonEmpty
        } yield id -> potPartners.maxBy(_._2)._1).toMap
        session ! MatchedGenes(genePartners)
      }
  }
}
