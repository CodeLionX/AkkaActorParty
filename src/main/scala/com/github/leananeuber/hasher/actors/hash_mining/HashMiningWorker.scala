package com.github.leananeuber.hasher.actors.hash_mining

import akka.actor.{Actor, ActorLogging, Props}
import com.github.leananeuber.hasher.HashUtil
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.hash_mining.HashMiningProtocol.{EncryptCommand, EncryptedEvent}
import com.github.leananeuber.hasher.protocols.MasterWorkerProtocol.WorkerHandling

import scala.util.Random

object HashMiningWorker {

  def props: Props = Props[HashMiningWorker]

}

class HashMiningWorker extends Actor with ActorLogging with WorkerHandling {

  val name: String = self.path.name

  val rand: Random = new Random(926)

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $name")

  override def receive: Receive = super.handleMasterCommunicationTo(HashMiningMaster.name) orElse {
    case EncryptCommand(content, prefixLength) =>
      log.info(s"hashing ${content.size} partners with prefix length $prefixLength")
      val prefixMap = Map(-1 -> "0" * prefixLength, 1 -> "1" * prefixLength)

      val hashes = content.map{ case (id, (partner, prefix)) =>
        id -> findHash(partner, prefixMap(prefix))
      }
      sender ! EncryptedEvent(hashes)
  }

  private def findHash(partnerId: Int, prefixString: String): String = {
    var hash = ""

    while( !hash.startsWith(prefixString) ) {
      val nonce = rand.nextInt
      hash = HashUtil.hash(partnerId + nonce)
    }

    hash
  }
}
