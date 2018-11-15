package com.github.leananeuber.hasher.cli

import akka.actor.Address
import akka.cluster.Cluster
import com.github.leananeuber.hasher.HasherActorSystem
import com.github.leananeuber.hasher.SessionSetupProtocol.SetupSessionConnectionTo
import com.github.leananeuber.hasher.actors.{Reaper, WorkerManager}
import org.backuity.clist

import scala.language.postfixOps

object StartSlaveCommand extends clist.Command(
  name = "slave",
  description = "start a slave actor system") with CommonStartCommand {

  val slaveRole = "slave"

  var masterHost: String = clist.opt[String](
    name = "masterhost",
    description = "host name or IP of the master",
    default = defaultHost
  )

  var masterPort: Int = clist.opt[Int](
    name = "masterport",
    description = "port to bind against",
    default = CommonStartCommand.defaultMasterPort
  )

  override def defaultPort: Int = CommonStartCommand.defaultSlavePort

  override def run(actorSystemName: String): Unit = {
    val system = HasherActorSystem.actorSystem(actorSystemName, HasherActorSystem.configuration(
      actorSystemName,
      slaveRole,
      host,
      port,
      masterHost,
      masterPort
    ))
    val cluster = Cluster(system)

    // TODO: start processing
    cluster.registerOnMemberUp{
      // create actors
      val reaper = system.actorOf(Reaper.props, Reaper.reaperName)
      val workerManager = system.actorOf(WorkerManager.props(nWorkers), WorkerManager.workerManagerName)

      // start processing
      workerManager ! SetupSessionConnectionTo(Address("akka", actorSystemName, masterHost, masterPort))
    }
  }
}