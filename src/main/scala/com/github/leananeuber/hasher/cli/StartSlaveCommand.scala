package com.github.leananeuber.hasher.cli

import org.backuity.clist


class StartSlaveCommand extends clist.Command(
  name = "slave",
  description = "start a slave actor system") with CommonStartCommand {

  var masterHost: String = clist.opt[String](
    name = "masterhost",
    abbrev = "mh",
    description = "host name or IP of the master",
    default = defaultHost
  )

  var masterPort: Int = clist.opt[Int](
    name = "masterport",
    abbrev = "mp",
    description = "port to bind against",
    default = CommonStartCommand.defaultMasterPort
  )

  override def defaultPort: Int = CommonStartCommand.defaultSlavePort
}