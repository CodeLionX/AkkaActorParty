package com.github.leananeuber.hasher.protocols

object MasterWorkerProtocol {

  case object RegisterWorker extends SerializableMessage
  case object RegisterWorkerAck extends SerializableMessage

}
