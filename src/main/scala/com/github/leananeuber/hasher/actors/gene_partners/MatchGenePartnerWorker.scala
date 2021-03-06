package com.github.leananeuber.hasher.actors.gene_partners

import akka.actor.{Actor, ActorLogging, Props}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerProtocol.{CalculateLCSLengths, LCSLengthsCalculated}
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerWorker.PotPartnerMap
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.WorkerHandling

import scala.collection.mutable


object MatchGenePartnerWorker {

  def props: Props = Props[MatchGenePartnerWorker]

  /** Maps an ID (student) to a partner -> LCS-length tuple, where LCS stands for
    * longest common substring
    */
  type PotPartnerMap = Map[Int, (Int, Int)]
}


class MatchGenePartnerWorker extends Actor with ActorLogging with WorkerHandling {

  val name: String = self.path.name

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $name")

  override def receive: Receive = super.handleMasterCommunicationTo(MatchGenePartnerMaster.name) orElse {
    case CalculateLCSLengths(genes, indices) =>
      log.info(s"calculating lengths of substrings on ${indices.length} index combinations")
      val lengths = createLengthMapPart(genes, indices)
      sender ! LCSLengthsCalculated(lengths)
  }

  private def createLengthMapPart(genes: Map[Int, String], indices: Seq[(Int, Int)]): PotPartnerMap = {
    val lengths: Map[Int, mutable.Buffer[(Int, Int)]] = genes.keys
      .map( key => key -> mutable.Buffer.empty[(Int, Int)] )
      .toMap

    indices.foreach{ case (i, j) =>
      val length = longestOverlap(genes(i), genes(j))
      lengths(i).append(j -> length)
      lengths(j).append(i -> length)
    }

    val potPartnerMappings: Iterable[(Int, (Int, Int))] = for {
      (i, potPartners) <- lengths
      if potPartners.nonEmpty
    } yield i -> potPartners.maxBy(_._2)
    potPartnerMappings.toMap
  }

  private def longestOverlap(str1p: String, str2p: String): Int = {
    var str1 = str1p
    var str2 = str2p

    if (str1.isEmpty || str2.isEmpty)
      return 0

    if (str1.length > str2.length) {
      val temp = str1
      str1 = str2
      str2 = temp
    }

    var currentRow = new Array[Int](str1.length)
    var lastRow = if (str2.length > 1) new Array[Int](str1.length) else null
    var longestSubstringLength = 0

    var str2Index = 0
    while (str2Index < str2.length) {
      val str2Char = str2.charAt(str2Index)

      var str1Index = 0
      while (str1Index < str1.length) {
        var newLength = 0
        if (str1.charAt(str1Index) == str2Char) {
          newLength = if (str1Index == 0 || str2Index == 0) 1 else lastRow(str1Index - 1) + 1
          if (newLength > longestSubstringLength) {
            longestSubstringLength = newLength
          }
        } else {
          newLength = 0
        }
        currentRow(str1Index) = newLength

        str1Index += 1
      }

      val temp = currentRow
      currentRow = lastRow
      lastRow = temp

      str2Index += 1
    }
    longestSubstringLength
  }
}
