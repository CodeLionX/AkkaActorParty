package com.github.leananeuber.hasher.actors.hash_mining

import com.github.leananeuber.hasher.protocols.SerializableMessage

object HashMiningProtocol {

  /** Maps an ID (student) to a partner -> prefix tuple.
    *                         ID   partnerId  prefix
    *                          |       |        |     */
  type PartnerPrefixMap = Map[Int,   (Int,     Int)]

  case class EncryptCommand(content: PartnerPrefixMap, prefixLength: Int) extends SerializableMessage
  case class EncryptedEvent(hashes: Map[Int, String]) extends SerializableMessage

  case class MineHashesFor(content: PartnerPrefixMap) extends SerializableMessage
  case class HashesMinedEvent(hashes: Map[Int, String]) extends SerializableMessage

}
