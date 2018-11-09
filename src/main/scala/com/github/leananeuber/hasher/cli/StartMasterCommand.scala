package com.github.leananeuber.hasher.cli

import org.backuity.clist


class StartMasterCommand extends clist.Command(
    name = "master",
    description = "start a master actor system") with CommonStartCommand {

  override def defaultPort: Int = CommonStartCommand.defaultMasterPort
}
