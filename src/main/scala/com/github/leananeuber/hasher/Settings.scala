package com.github.leananeuber.hasher

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config


class SettingsImpl(config: Config) extends Extension {

  private val namespace = "com.github.leananeuber.hasher"

  val linearizationPartitions: Int = config.getInt(s"$namespace.linearization-partitions")

  val prefixLength: Int = config.getInt(s"$namespace.prefix-length")

  val passwordRangeStart: Int = config.getInt(s"$namespace.password-range.start")

  val passwordRangeEnd: Int = config.getInt(s"$namespace.password-range.end")

}


object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): SettingsImpl = new SettingsImpl(system.settings.config)

  override def lookup(): ExtensionId[_ <: Extension] = Settings

}
