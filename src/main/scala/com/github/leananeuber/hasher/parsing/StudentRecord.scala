package com.github.leananeuber.hasher.parsing

sealed case class StudentRecord(id: Int, name: String, passwordHash: String, geneSeq: String) {

  def extractToResult: ResultRecord = ResultRecord(id, name, "", 0, 0, "")

}

sealed case class ResultRecord(id: Int, name: String, password: Int, prefix: Int, partnerId: Int, hash: String)
