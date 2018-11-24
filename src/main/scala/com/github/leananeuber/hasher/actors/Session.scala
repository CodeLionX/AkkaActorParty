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
import com.github.leananeuber.hasher.protocols.SessionSetupProtocol.{MasterReady, RegisterAtSession, RegisteredAtSessionAck}


object Session {

  val sessionName = "session"

  def props(nSlaves: Int, input: File): Props = Props(new Session(nSlaves, input))

  case class State(slaveRegistry: Map[ActorRef, Int],
                   pcMaster: ActorRef,
                   mgpMaster: ActorRef,
                   hmMaster: ActorRef,
                   result: Map[Int, ResultRecord],
                   startTime: Long,
                   lastTime: Long)

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

        // wait for all workers to connect to their masters
        context.become(startProcessing(newSlaveRegistry, pcMaster, mgpMaster, hmMaster, 0))
      }
  }

  def startProcessing(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef, mgpMaster: ActorRef, hmMaster: ActorRef, readyMasters: Int): Receive = {
    case MasterReady =>
      val newReadyMasters = readyMasters + 1
      if(newReadyMasters == 3) {
        log.info("all masters ready, starting processing")
        // start processing
        val startMillis = System.currentTimeMillis()
        pcMaster ! StartCrackingCommand(pws)

        // prepare output and go to next state
        val state = State(
          slaveRegistry = slaveRegistry,
          pcMaster = pcMaster,
          mgpMaster = mgpMaster,
          hmMaster = hmMaster,
          result = records.map{ case (id, record) => id -> record.extractToResult },
          startTime = startMillis,
          lastTime = startMillis
        )
        context.become(runningPasswordCracking(state))

      } else {
        context.become(startProcessing(slaveRegistry, pcMaster, mgpMaster, hmMaster, newReadyMasters))
      }
  }

  def runningPasswordCracking(state: State): Receive = {
    case PasswordsCrackedEvent(cleartexts) =>
      state.pcMaster ! StartCalculateLinearCombinationCommand(cleartexts)
      val time = System.currentTimeMillis()
      println(s"Decryption: ${time - state.lastTime} ms")

      log.info(s"session received cleartexts")
      val updatedResult = state.result.map{ case (id, record) =>
        id -> record.copy(
          password = cleartexts(id)
        )
      }
      context.become(runningLinearCombination(state.copy(result = updatedResult, lastTime = time)))
  }

  def runningLinearCombination(state: State): Receive = {
    case LinearCombinationCalculatedEvent(combinations) =>
      state.mgpMaster ! StartMatchingGenes(genes)
      val time = System.currentTimeMillis()
      println(s"Linear Combination: ${time - state.lastTime} ms")

      log.info("session received combinations")
      val updatedResult = state.result.map{ case (id, record) =>
        id -> record.copy(
          prefix = combinations(id)
        )
      }

      context.become(runningGeneMatching(state.copy(result = updatedResult, lastTime = time)))
  }

  def runningGeneMatching(state: State): Receive = {
    case MatchedGenes(genePartners) =>
      val miningContent = genePartners.map{ case (id, partner) =>
        id -> (partner, state.result(id).prefix)
      }
      state.hmMaster ! MineHashesFor(miningContent)
      val time = System.currentTimeMillis()
      println(s"Substring: ${time - state.lastTime} ms")

      log.info(s"session received gene partners")
      val updatedResult = state.result.map{ case (id, record) =>
        id -> record.copy(
          partnerId = genePartners(id)
        )
      }

      context.become(runningHashMining(state.copy(result = updatedResult, lastTime = time)))
  }

  def runningHashMining(state: State): Receive = {
    case HashesMinedEvent(hashes) =>
      val time = System.currentTimeMillis()
      println(s"Encryption: ${time - state.lastTime} ms")
      println()
      println(s"Overall:    ${time - state.startTime} ms")
      log.info(s"session received hashes")

      val udpatedResult = state.result.map{ case (id, record) =>
        record.copy(
          hash = hashes(id)
        )
      }.toSeq
      StudentsCSVParser.buildResultCsvString(udpatedResult, System.out)

      shutdown(state)
  }

  def shutdown(state: State): Unit = {
    val actors = Seq(state.hmMaster, state.mgpMaster, state.pcMaster) ++ state.slaveRegistry.keys
    actors.foreach(_ ! PoisonPill)
    context.stop(self)
  }

}
