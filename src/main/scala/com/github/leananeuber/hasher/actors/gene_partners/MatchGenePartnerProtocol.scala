package com.github.leananeuber.hasher.actors.gene_partners

import com.github.leananeuber.hasher.protocols.SerializableMessage


object MatchGenePartnerProtocol {

  case class StartMatchingGenes(genes: Map[Int, String]) extends SerializableMessage

  case class MatchedGenes(genePartners: Map[Int, Int]) extends SerializableMessage

  case class CalculateLCSLengths(genes: Map[Int, String], indices: Seq[(Int, Int)]) extends SerializableMessage

  case class LCSLengthsCalculated(lengths: Map[Int, (Int, Int)]) extends SerializableMessage

}
