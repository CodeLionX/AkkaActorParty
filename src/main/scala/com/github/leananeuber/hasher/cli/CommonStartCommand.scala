package com.github.leananeuber.hasher.cli

import java.net.InetAddress

import org.backuity.clist

import scala.util.Try

object CommonStartCommand {

  val defaultMasterPort = 7877
  val defaultSlavePort = 7879
  val defaultNWorkers = 4
}

trait CommonStartCommand { this: clist.Command =>
  var host: String = clist.opt[String](
    description = "this machine's host name or IP to bind against",
    default = defaultHost
  )

  var port: Int = clist.opt[Int](
    description = "port to bind against",
    default = defaultPort
  )

  var nWorkers: Int = clist.opt[Int](
    name = "workers",
    description = "number of workers to start locally",
    default = CommonStartCommand.defaultNWorkers
  )

  def defaultHost: String = Try{
    InetAddress.getLocalHost.getHostAddress
  }.getOrElse("localhost")

  def defaultPort: Int

  def run(actorSystemName: String): Unit
}