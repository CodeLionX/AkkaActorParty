package com.github.leananeuber.hasher.cli

import java.net.InetAddress

import org.backuity.clist

import scala.util.Try

object CommonStartCommand {

  val defaultMasterPort: Int = 7877
  val defaultSlavePort: Int = 7879
  val defaultNWorkers: Int = 4
}

trait CommonStartCommand { this: clist.Command =>
  var host: String = clist.opt[String](
    abbrev = "h",
    description = "this machine's host name or IP to bind against",
    default = defaultHost
  )

  var port: Int = clist.opt[Int](
    abbrev = "p",
    description = "port to bind against",
    default = defaultPort
  )

  var nWorkers: Int = clist.opt[Int](
    name = "workers",
    abbrev = "w",
    description = "number of workers to start locally",
    default = CommonStartCommand.defaultNWorkers
  )

  def defaultHost: String = Try{
    InetAddress.getLocalHost.getHostAddress
  }.getOrElse("localhost")

  def defaultPort: Int
}