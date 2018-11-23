package com.github.leananeuber.hasher

import java.nio.charset.StandardCharsets
import java.security.MessageDigest


object HashUtil {

  def generateRainbow(range: Seq[Int]): Map[String, Int] = {
    range.map(x => hash(x) -> x).toMap
  }

  def hash(number: Int): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashedBytes = digest.digest(String.valueOf(number).getBytes(StandardCharsets.UTF_8))

    hashedBytes.map( byte =>
      ((byte & 0xff) + 0x100).toHexString.substring(1)
    ).mkString("")
  }

  def unhash(hexHash: String, rainbow: Map[String, Int]): Option[Int] =
    rainbow.get(hexHash)

}
