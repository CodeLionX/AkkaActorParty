package com.github.leananeuber.hasher.actors

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerMaster
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerProtocol.{MatchedGenes, StartMatchingGenes}
import com.github.leananeuber.hasher.actors.hash_mining.HashMiningMaster
import com.github.leananeuber.hasher.actors.hash_mining.HashMiningProtocol.{HashesMinedEvent, MineHashesFor}
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingMaster
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{PasswordsCrackedEvent, StartCrackingCommand, _}
import com.github.leananeuber.hasher.parsing.{ResultRecord, StudentRecord, StudentsCSVParser}
import com.github.leananeuber.hasher.protocols.SessionSetupProtocol.{RegisterAtSession, RegisteredAtSessionAck}


object Session {

  val sessionName = "session"

  def props(nSlaves: Int, input: File): Props = Props(new Session(nSlaves, input))

}


class Session(nSlaves: Int, inputFile: File) extends Actor with ActorLogging {
  import Session._

  val records: Map[Int, StudentRecord] = StudentsCSVParser.readStudentCsvFile(inputFile)
  val genes: Map[Int, String] = records.map{ case (id, record) =>
    id -> record.geneSeq
  }
  val pws: Map[Int, String] = records.map{ case (id, record) =>
    id -> record.passwordHash
  }

  override def preStart(): Unit = {
    log.info(s"Starting $sessionName")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $sessionName")

  override def receive: Receive = sessionSetup(Map.empty)

  def sessionSetup(slaveRegistry: Map[ActorRef, Int]): Receive = {
    case RegisterAtSession(nWorker) =>
      val newSlaveRegistry = slaveRegistry + (sender -> nWorker)
      sender ! RegisteredAtSessionAck

      if(newSlaveRegistry.size < (nSlaves+1))
        context.become(sessionSetup(newSlaveRegistry))

      else {
        val overallWorkers = newSlaveRegistry.values.sum
        val pcMaster = context.actorOf(PasswordCrackingMaster.props(overallWorkers, self), PasswordCrackingMaster.name)
        val mgpMaster = context.actorOf(MatchGenePartnerMaster.props(overallWorkers, self), MatchGenePartnerMaster.name)
        val hmMaster = context.actorOf(HashMiningMaster.props(overallWorkers, self), HashMiningMaster.name)

        // start processing
        pcMaster ! StartCrackingCommand(pws)

        // prepare output and go to next state
        val result = records.map{ case (id, record) => id -> record.extractToResult }
        context.become(runningPasswordCracking(newSlaveRegistry, pcMaster, mgpMaster, hmMaster, result))
      }
  }

  def runningPasswordCracking(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef, hmMaster: ActorRef, result: Map[Int, ResultRecord]): Receive = {

    case PasswordsCrackedEvent(cleartexts) =>
      pcMaster ! StartCalculateLinearCombinationCommand(cleartexts)

      log.info(s"session received cleartexts")
      val updatedResult = result.map{ case (id, record) =>
        id -> record.copy(
          password = cleartexts(id)
        )
      }
      context.become(runningLinearCombination(slaveRegistry, pcMaster, mgpMaster, hmMaster, updatedResult))
  }

  def runningLinearCombination(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef, hmMaster: ActorRef, result: Map[Int, ResultRecord]): Receive = {

    case LinearCombinationCalculatedEvent(combinations) =>
      mgpMaster ! StartMatchingGenes(genes)

      log.info("session received combinations")
      val updatedResult = result.map{ case (id, record) =>
        id -> record.copy(
          prefix = combinations(id)
        )
      }

      context.become(runningGeneMatching(slaveRegistry, pcMaster, mgpMaster, hmMaster, updatedResult))
  }

  def runningGeneMatching(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef, hmMaster: ActorRef, result: Map[Int, ResultRecord]): Receive = {

    case MatchedGenes(genePartners) =>
      val miningContent = genePartners.map{ case (id, partner) =>
        id -> (partner, result(id-1).prefix)
      }
      hmMaster ! MineHashesFor(miningContent)

      log.info(s"session received gene partners")
      val updatedResult = result.map{ case (id, record) =>
        id -> record.copy(
          partnerId = genePartners(id)
        )
      }

      context.become(runningHashMining(slaveRegistry, pcMaster, mgpMaster, hmMaster, updatedResult))
  }

  def runningHashMining(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef, hmMaster: ActorRef, result: Map[Int, ResultRecord]): Receive = {
    case HashesMinedEvent(hashes) =>
      log.info(s"session received hashes")

      val udpatedResult = result.map{ case (id, record) =>
        record.copy(
          hash = hashes(id)
        )
      }

      shutdown(Seq(pcMaster, mgpMaster, hmMaster) ++ slaveRegistry.keys)
  }

  def shutdown(actors: Seq[ActorRef]): Unit = {
    actors.foreach(_ ! PoisonPill)
    context.stop(self)
  }

}
