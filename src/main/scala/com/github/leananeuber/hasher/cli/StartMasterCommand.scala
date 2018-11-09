package com.github.leananeuber.hasher.cli

import org.backuity.clist


object StartMasterCommand extends clist.Command(
    name = "master",
    description = "start a master actor system") with CommonStartCommand {

  val masterRole = "master"

  override def defaultPort: Int = CommonStartCommand.defaultMasterPort

  override def run(actorSystemName: String): Unit = {

  }
}
