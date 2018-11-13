package com.github.leananeuber.hasher.cli

import java.io.File

import akka.actor.PoisonPill
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import com.github.leananeuber.hasher.HasherActorSystem
import com.github.leananeuber.hasher.actors.Reaper
import com.github.leananeuber.hasher.actors.password_cracking.PasswordCrackingProtocol.{CrackPasswordsCommand, PasswordsCrackedEvent}
import com.github.leananeuber.hasher.actors.password_cracking.{PasswordCrackingMaster, PasswordCrackingWorker}
import org.backuity.clist

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try


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
      val pws = Seq(
        "7c3c58cdfb7dbc141c28cba84d4d07ff67b936e913080142eed1c6f5bcb6c43f",
        "9ad8a9a2f51c5621bd1432d8f6dc33bf0cfa91889033d0a6f4d3f020d7c01037",
        "1ba1604f3c7d04016990169a1fc9716d425d092cd38a2431954bfa06449b1469"
      ).zipWithIndex.map(_.swap).toMap

      // start processing
      system.scheduler.scheduleOnce(2 seconds) {
        implicit val timeout: Timeout = Timeout(20 seconds)
        val resultFuture = pwMaster ? CrackPasswordsCommand(pws)

        // sync for reply before terminating system
        val result = Try(Await.result(resultFuture.mapTo[PasswordsCrackedEvent].map(_.cleartexts), timeout.duration))
          .recover {
            case e =>
              println(s"Master run() execution failed, reason: ${e.getMessage}")
              pwMaster ! PoisonPill
              Map.empty
          }
        println(result.getOrElse("No result!"))
        pwMaster ! PoisonPill
      }

      // try application-defined shutdown after 5 seconds
//      system.scheduler.scheduleOnce(5 seconds, pwMaster, PoisonPill)
    }
  }
}
