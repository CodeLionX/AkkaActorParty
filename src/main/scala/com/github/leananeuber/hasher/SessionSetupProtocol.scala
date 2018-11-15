package com.github.leananeuber.hasher

import akka.actor.Address

object SessionSetupProtocol {

  case class SetupSessionConnectionTo(masterNodeAddress: Address)

  case class RegisterAtSession(nWorker: Int)

  case object RegisteredAtSessionAck

}
