package com.github.leananeuber.hasher.protocols

import akka.actor.Address

object SessionSetupProtocol {

  case class SetupSessionConnectionTo(masterNodeAddress: Address) extends SerializableMessage

  case class RegisterAtSession(nWorker: Int) extends SerializableMessage

  case object RegisteredAtSessionAck extends SerializableMessage

  case object MasterReady extends SerializableMessage

}
