package com.github.leananeuber.hasher

import de.uni_potsdam.hpi.SerialAnalyzer

import scala.io.Source

object SATest extends App{

  val lines = Source.fromFile("src/main/resources/students.csv").getLines().drop(1).toArray
  //println(s"Read file with ${lines.length} lines")

  val analyzer = new SerialAnalyzer
  analyzer.analyze(lines)

}
