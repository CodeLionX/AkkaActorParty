package com.github.leananeuber.hasher.actors

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.protocols.SessionSetupProtocol.{RegisterAtSession, RegisteredAtSessionAck}
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingMaster
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol._
import com.github.leananeuber.hasher.parsing.{StudentRecord, StudentsCSVParser}


object Session {

  val sessionName = "session"

  def props(nSlaves: Int, input: File): Props = Props(new Session(nSlaves, input))

}


class Session(nSlaves: Int, inputFile: File) extends Actor with ActorLogging {
  import Session._

  override def preStart(): Unit = {
    log.info(s"Starting $sessionName")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $sessionName")

  override def receive: Receive = sessionSetup(Map.empty, StudentsCSVParser.readStudentCsvFile(inputFile))

  def sessionSetup(slaveRegistry: Map[ActorRef, Int], records: Map[Int, StudentRecord]): Receive = {
    case RegisterAtSession(nWorker) =>
      val newSlaveRegistry = slaveRegistry + (sender -> nWorker)
      sender ! RegisteredAtSessionAck

      if(newSlaveRegistry.size < (nSlaves+1))
        context.become(sessionSetup(newSlaveRegistry, records))

      else {
        val overallWorkers = newSlaveRegistry.values.sum
        log.debug(s"Starting pc-master with $overallWorkers workers")
        val pcMaster = context.actorOf(PasswordCrackingMaster.props(overallWorkers, self), PasswordCrackingMaster.name)

        // read `input`
        val pws: Map[Int, String] = records.map{ case (id, record) =>
          id -> record.passwordHash
        }

        // start processing
        pcMaster ! StartCrackingCommand(pws)
        context.become(runningPasswordCrack(newSlaveRegistry, pcMaster))
      }
  }

  def runningPasswordCrack(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef): Receive = {

    case PasswordsCrackedEvent(cleartexts) =>
      println(cleartexts)
      pcMaster ! CalculateLinearCombinationCommand(cleartexts)
      context.become(runningLinearCombination(slaveRegistry,pcMaster))
  }

  def runningLinearCombination(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef): Receive = {

    case LinearCombinationCalculatedEvent(passwordPrefixes) =>
      //println(passwordPrefixes)
      pcMaster ! PoisonPill
      slaveRegistry.keys.foreach(_ ! PoisonPill)
      context.stop(self)
  }

}
