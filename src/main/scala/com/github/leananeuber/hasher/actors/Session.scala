package com.github.leananeuber.hasher.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.leananeuber.hasher.SessionSetupProtocol.RegisterAtSession


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

      if(newSlaveRegistry.size < nSlaves)
        context.become(sessionSetup(newSlaveRegistry))
      else
        context.become(ready(newSlaveRegistry))
  }

  def ready(slaveRegistry: Map[ActorRef, Int]): Receive = ???
}
