package com.github.leananeuber.hasher.cli

import org.backuity.clist


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

  }
}