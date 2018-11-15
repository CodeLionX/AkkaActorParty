package com.github.leananeuber.hasher.actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.github.leananeuber.hasher.protocols.SessionSetupProtocol.{RegisterAtSession, RegisteredAtSessionAck}
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingMaster
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CrackPasswordsCommand, PasswordsCrackedEvent}


object Session {

  val sessionName = "session"

  def props(nSlaves: Int): Props = Props(new Session(nSlaves))

}


class Session(nSlaves: Int) extends Actor with ActorLogging {
  import Session._

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
        log.debug(s"Starting pc-master with $overallWorkers workers")
        val pcMaster = context.actorOf(PasswordCrackingMaster.props(overallWorkers), PasswordCrackingMaster.name)

        // read `input`
        val pws = Seq(
          "7c3c58cdfb7dbc141c28cba84d4d07ff67b936e913080142eed1c6f5bcb6c43f",
          "9ad8a9a2f51c5621bd1432d8f6dc33bf0cfa91889033d0a6f4d3f020d7c01037",
          "1ba1604f3c7d04016990169a1fc9716d425d092cd38a2431954bfa06449b1469"
        ).zipWithIndex.map(_.swap).toMap

        // start processing
        pcMaster ! CrackPasswordsCommand(pws)
        context.become(running(newSlaveRegistry, pcMaster))
      }
  }

  def running(slaveRegistry: Map[ActorRef, Int], pcMaster: ActorRef): Receive = {

    case PasswordsCrackedEvent(cleartexts) =>
      println(cleartexts)
      pcMaster ! PoisonPill
      slaveRegistry.keys.foreach(_ ! PoisonPill)
      context.stop(self)
  }
}
