package com.github.leananeuber.hasher

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object HasherActorSystem {

  def configuration(actorSystemName: String, actorSystemRole: String, host: String, port: Int, masterHost: String, masterPort: Int): Config = {
    ConfigFactory.parseString(
      s"""akka.remote.artery.canonical.hostname = "$host"
         |akka.remote.artery.canonical.port = "$port"
         |akka.cluster.roles = [$actorSystemRole]
         |akka.cluster.seed-nodes = [
         |  "akka://$actorSystemName@$masterHost:$masterPort"
         |]
       """.stripMargin)
      .withFallback(ConfigFactory.load("application"))
  }

  def actorSystem(actorSystemName: String, config: Config): ActorSystem = {
    val system = ActorSystem(actorSystemName, config)

    Cluster(system).registerOnMemberRemoved{
      system.terminate()

      new Thread() {
        override def run(): Unit = {
          Await.ready(system.terminate(), 10 seconds).recover{
            case _: Exception => System.exit(-1)
          }
        }
      }.start()
    }

    system
  }
}
