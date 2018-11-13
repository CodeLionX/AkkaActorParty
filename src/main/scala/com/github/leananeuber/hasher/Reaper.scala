package com.github.leananeuber.hasher

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import scala.collection.mutable.ArrayBuffer

//reaper-companion
//reaper-messages
object Reaper {
  def props: Props = Props[Reaper]
  case class WatchMe(ref: ActorRef)
}


//Reaper actor
class Reaper extends Actor with ActorLogging{
  import Reaper._

  val watched = ArrayBuffer.empty[ActorRef]

  override def preStart(): Unit = {
    super.preStart()
    log.info("Started {}", this)
  }

  def receive = {
    case WatchMe(ref) =>
      context.watch(ref)
      watched+=ref
    case Terminated(ref) =>
      watched-=ref
      if(watched.isEmpty) terminateSystem()
    case unexpected =>
      log.error(s"ERROR: Unknown message: $unexpected")
  }

  def terminateSystem(): Unit = {
    context.system.terminate()
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("Stopped {}", this)
  }

}
