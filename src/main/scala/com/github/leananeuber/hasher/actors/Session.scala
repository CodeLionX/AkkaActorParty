package com.github.leananeuber.hasher.actors

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerMaster
import com.github.leananeuber.hasher.actors.gene_partners.MatchGenePartnerMaster.{MatchedGenes, StartMatchingGenes}
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingMaster
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{PasswordsCrackedEvent, StartCrackingCommand, _}
import com.github.leananeuber.hasher.parsing.{StudentRecord, StudentsCSVParser}
import com.github.leananeuber.hasher.protocols.SessionSetupProtocol.{RegisterAtSession, RegisteredAtSessionAck}


object Session {

  val sessionName = "session"

  def props(nSlaves: Int, input: File): Props = Props(new Session(nSlaves, input))

}


class Session(nSlaves: Int, inputFile: File) extends Actor with ActorLogging {
  import Session._

  val records: Map[Int, StudentRecord] = StudentsCSVParser.readStudentCsvFile(inputFile)

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
        log.debug(s"Starting masters with $overallWorkers workers")
        val pcMaster = context.actorOf(PasswordCrackingMaster.props(overallWorkers, self), PasswordCrackingMaster.name)
        val mgpMaster = context.actorOf(MatchGenePartnerMaster.props(overallWorkers, self), MatchGenePartnerMaster.name)

        // read `input`
        val pws: Map[Int, String] = records.map{ case (id, record) =>
          id -> record.passwordHash
        }

        // start processing
        pcMaster ! StartCrackingCommand(pws)
        context.become(runningPasswordCracking(newSlaveRegistry, pcMaster, mgpMaster))
      }
  }

  def runningPasswordCracking(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef): Receive = {

    case PasswordsCrackedEvent(cleartexts) =>
      log.info(s"session received cleartexts")
      println(cleartexts)

      pcMaster ! StartCalculateLinearCombinationCommand(cleartexts)
      context.become(runningLinearCombination(slaveRegistry, pcMaster, mgpMaster))
  }

  def runningLinearCombination(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef): Receive = {

    case LinearCombinationCalculatedEvent(combinations) =>
      println(combinations)

      val genes: Map[Int, String] = records.map{ case (id, record) =>
        id -> record.geneSeq
      }
      mgpMaster ! StartMatchingGenes(genes)
      context.become(runningGeneMatching(slaveRegistry, pcMaster, mgpMaster))
  }

  def runningGeneMatching(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef): Receive = {

    case MatchedGenes(genePartners) =>
      log.info(s"session received gene partners")
      println(genePartners)
      shutdown(Seq(pcMaster, mgpMaster) ++ slaveRegistry.keys)

  }

  def shutdown(actors: Seq[ActorRef]): Unit = {
    actors.foreach(_ ! PoisonPill)
    context.stop(self)
  }

}
