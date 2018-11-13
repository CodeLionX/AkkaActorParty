package com.github.leananeuber.hasher.actors

import java.security.MessageDigest

import akka.actor.{Actor, ActorLogging, ActorRef}


object PasswordCracking {

  case class CrackPasswordsCommand(secrets: Seq[String])
  case class PasswordsCrackedEvent(cleartexts: Seq[Int])
}

class PasswordCracking(searchStart: Int, searchStop: Int, master: ActorRef) extends Actor with ActorLogging {
  import PasswordCracking._

  override def preStart(): Unit = Reaper.watchWithDefault(self)

  override def receive: Receive = {
    case CrackPasswordsCommand(secrets) =>
      sender() ! PasswordsCrackedEvent(decrypt(secrets))
  }

  def decrypt(secrets: Seq[String]): Seq[Int] = secrets.flatMap(unhash)

  private def unhash(hexHash: String): Option[Int] = (searchStart to searchStop)
    .map(i => i -> hash(i))
    .find{ case (_, h) =>
      h.equals(hexHash)
    }
    .map{ case (i, h) => i }

  // copied from thorsten
  private def hash(number: Int) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedBytes = digest.digest(String.valueOf(number).getBytes("UTF-8"))
    val stringBuffer = new StringBuffer
    var i = 0

    while ( {
      i < hashedBytes.length
    }) {
      stringBuffer.append(Integer.toString((hashedBytes(i) & 0xff) + 0x100, 16).substring(1))

      {
        i += 1
        i - 1
      }
    }
    stringBuffer.toString
  }
}
