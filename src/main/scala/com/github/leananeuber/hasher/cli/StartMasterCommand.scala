package com.github.leananeuber.hasher.cli

import java.io.File

import akka.cluster.Cluster
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.{PasswordCrackingMaster, PasswordCrackingWorker}
import com.github.leananeuber.hasher.{AkkaQuickstart, HasherActorSystem}
import org.backuity.clist

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

object StartMasterCommand extends clist.Command(
    name = "master",
    description = "start a master actor system") with CommonStartCommand {

  val masterRole = "master"

  var nSlaves: Int = clist.opt[Int](
    name = "slaves",
    description = "number of slave actor systems to wait for",
    default = CommonStartCommand.defaultNSlaves
  )

  var input: File = clist.arg[File](
    name = "input",
    description = "path to the input CSV file"
  )

  override def defaultPort: Int = CommonStartCommand.defaultMasterPort

  override def run(actorSystemName: String): Unit = {
    val masterHost = host
    val masterPort = port
    val system = HasherActorSystem.actorSystem(actorSystemName, HasherActorSystem.configuration(
      actorSystemName,
      masterRole,
      host,
      port,
      masterHost,
      masterPort
    ))
    val cluster = Cluster(system)

    // TODO: handle cluster events

    // TODO: start processing
    cluster.registerOnMemberUp{
      // create actors
      val reaper = system.actorOf(Reaper.props, Reaper.reaperName)
      val pwMaster = system.actorOf(PasswordCrackingMaster.props, "pw-master")
      val pwWorker1 = system.actorOf(PasswordCrackingWorker.props(pwMaster), "pw-worker-1")
      val pwWorker2 = system.actorOf(PasswordCrackingWorker.props(pwMaster), "pw-worker-2")
      // read `input`
      // start processing
    }

    // TODO: remove
    //------------------------------------------------
    // run example code on status UP
    cluster.registerOnMemberUp{
      AkkaQuickstart.runQuickstartExampleOn(system)
    }
    // leave cluster after 2 seconds
    system.scheduler.scheduleOnce(2 seconds) {
      cluster.leave(cluster.selfAddress)
    }
    //------------------------------------------------
  }
}
