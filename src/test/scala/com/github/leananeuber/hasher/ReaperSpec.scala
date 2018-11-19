package com.github.leananeuber.hasher

import akka.actor.{ActorPath, ActorRef, ActorSystem, Terminated}
import akka.testkit.{TestKit, TestProbe}
import com.github.leananeuber.hasher.Greeter.WhoToGreet
import com.github.leananeuber.hasher.Printer.Greeting
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ReaperSpec(_system: ActorSystem)extends TestKit(_system) with Matchers
  with WordSpecLike
  with BeforeAndAfterAll {
  def this() = this(ActorSystem("ReaperSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Reaper Actor" should {
    "terminate the system when a Terminated message is received" in {
      //#specification-example
      val testProbe = TestProbe()
      val reaper = system.actorOf(Reaper.props)

      //reaper ! Terminated(ref)
    }
  }
}
