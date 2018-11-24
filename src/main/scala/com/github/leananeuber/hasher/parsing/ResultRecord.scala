package com.github.leananeuber.hasher.parsing

sealed case class ResultRecord(id: Int, name: String, password: Int, prefix: Int, partnerId: Int, hash: String)