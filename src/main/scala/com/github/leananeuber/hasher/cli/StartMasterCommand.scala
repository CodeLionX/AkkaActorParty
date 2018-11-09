package com.github.leananeuber.hasher.cli

import java.io.File

import org.backuity.clist


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

  }
}
