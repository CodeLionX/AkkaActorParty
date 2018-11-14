package com.github.leananeuber.hasher.parsing

sealed case class StudentRecord(id: Int, name: String, passwordHash: String, geneSeq: String)
