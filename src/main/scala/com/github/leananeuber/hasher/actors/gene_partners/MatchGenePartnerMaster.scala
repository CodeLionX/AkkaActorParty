package com.github.leananeuber.hasher.actors.gene_partners

import akka.actor.{Actor, ActorLogging, PoisonPill, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerMaster.StartMatching
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerWorker.CalculateLCSLengths
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.MasterHandling

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps


object MatchGenePartnerMaster {

  val name = "gp-master"

  def props(nWorkers: Int): Props = Props(new MatchGenePartnerMaster(nWorkers: Int))

  case class StartMatching(genes: Map[Int, String])

}


class MatchGenePartnerMaster(nWorkers: Int) extends Actor with ActorLogging with MasterHandling {

  val name: String = self.path.name

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
        val combinations = ???
        val ranges: Seq[Seq[(Int, Int)]] = ???

        workers.zip(ranges).foreach { case (ref, indexRange) =>
          ref ! CalculateLCSLengths(genes, indexRange)
        }
      }
  }
}
