package com.github.leananeuber.hasher.actors.gene_partners

import akka.actor.{Actor, ActorLogging, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.WorkerHandling

import scala.annotation.tailrec


object MatchGenePartnerWorker {

  def props: Props = Props[MatchGenePartnerWorker]

  case class CalculateLCSLengths(genes: Map[Int, String], indices: Seq[(Int, Int)])

  case class LCSLengthsCalculated(lengths: Map[(Int, Int), Int])

}


class MatchGenePartnerWorker extends Actor with ActorLogging with WorkerHandling {
  import MatchGenePartnerWorker._

  val name: String = self.path.name

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $name")

  override def receive: Receive = super.handleMasterCommunication orElse {
    case CalculateLCSLengths(genes, indices) =>
      val lengths = indices.map{ case (i, j) =>
        (i,j) -> longestCommonSubstringLength(genes(i), genes(j))
      }.toMap
      sender ! LCSLengthsCalculated(lengths)
  }

  private def longestCommonSubstringLength(a: String, b: String) = {

    @tailrec
    def loop(bestLengths: Map[(Int, Int), Int], bestIndices: (Int, Int), i: Int, j: Int): Int = {
      if (i > a.length) {
        val bestJ = bestIndices._2
        b.substring(bestJ - bestLengths(bestIndices), bestJ).length
      } else {
        val currentLength = if (a(i-1) == b(j-1)) bestLengths(i-1, j-1) + 1 else 0
        loop(
          bestLengths + ((i, j) -> currentLength),
          if (currentLength > bestLengths(bestIndices)) (i, j) else bestIndices,
          if (j == b.length) i + 1 else i,
          if (j == b.length) 1 else j + 1)
      }
    }

    loop(Map.empty[(Int, Int), Int].withDefaultValue(0), (0, 0), 1, 1)
  }
}
