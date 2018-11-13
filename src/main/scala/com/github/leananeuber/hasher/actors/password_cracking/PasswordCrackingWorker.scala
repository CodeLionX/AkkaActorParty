package com.github.leananeuber.hasher.actors.password_cracking

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import com.github.leananeuber.hasher.MasterWorkerProtocol.{RegisterWorker, RegisterWorkerAck}
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingWorker.CrackingFailedException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object PasswordCrackingWorker {

  def props(searchStart: Int, searchStop: Int, master: ActorRef): Props =
    Props(new PasswordCrackingWorker(searchStart, searchStop, master))

  object CrackingFailedException extends RuntimeException
}

class PasswordCrackingWorker(searchStart: Int, searchStop: Int, master: ActorRef)
  extends Actor with ActorLogging {

  val name: String = this.getClass.getSimpleName
  val registerWorkerCancellable: Cancellable =
    context.system.scheduler.schedule(0 seconds, 5 seconds, master, RegisterWorker)

  override def preStart(): Unit = {
    log.info(s"Starting $name")
    Reaper.watchWithDefault(self)
  }

  override def postStop(): Unit =
    log.info(s"Stopping $name")

  override def receive: Receive = {
    case RegisterWorkerAck =>
      registerWorkerCancellable.cancel()

    case CrackPasswordsCommand(secrets) =>
      sender() ! PasswordsCrackedEvent(decrypt(secrets))
  }

  def decrypt(secrets: Map[Int, String]): Map[Int, Int] = secrets.map( idHashTuple => {
    val realValue = unhash(idHashTuple._2) match {
      case Some(real) => real
      case None => throw CrackingFailedException
    }
    idHashTuple._1 -> realValue
  })

  private def unhash(hexHash: String): Option[Int] = (searchStart to searchStop)
    .map ( i => i -> hash(i) )
    .find{ case (_, h) => h.equals(hexHash) }
    .map ( indexHashTuple => indexHashTuple._1 )

  private def hash(number: Int) = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedBytes = digest.digest(String.valueOf(number).getBytes(StandardCharsets.UTF_8))

    hashedBytes.map( byte =>
      ((byte & 0xff) + 0x100).toHexString.substring(1)
    ).mkString("")
  }
}
