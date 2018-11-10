package com.github.leananeuber.hasher

import com.github.leananeuber.hasher.cli.{StartMasterCommand, StartSlaveCommand}
import org.backuity.clist.Cli

object HasherApp {

  val actorSystemName = "akka-actor-party"

  def main(args: Array[String]): Unit = {
    Cli.parse(args)
      .withProgramName("akka-actor-party")
      .withDescription("Analyze student record for passwords and genetic codes to mine hashes.")
      .version("0.0.1")
      .withCommands(StartMasterCommand, StartSlaveCommand)
      .foreach(_.run(actorSystemName))
  }

}
