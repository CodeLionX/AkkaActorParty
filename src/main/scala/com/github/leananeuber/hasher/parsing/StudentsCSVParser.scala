package com.github.leananeuber.hasher.parsing

import java.io.{File, PrintStream}

import com.univocity.parsers.common.record.Record
import com.univocity.parsers.csv._

import scala.collection.JavaConversions._
import scala.io.Codec

object StudentsCSVParser {

  private implicit val fileCodec: Codec = Codec.UTF8

  private val withHeader = true
  private val defaultFormat = {
    val csvFormat = new CsvFormat
    csvFormat.setQuote('"')
    csvFormat.setQuoteEscape('\\')
    csvFormat.setCharToEscapeQuoteEscaping('\\')
    csvFormat.setLineSeparator("\n")
    csvFormat.setDelimiter(';')
    csvFormat
  }

  private lazy val reader = {
    val settings = new CsvParserSettings
    settings.setFormat(defaultFormat)
    settings.setHeaderExtractionEnabled(withHeader)
    new CsvParser(settings)
  }

  private def stringWriter(out: PrintStream) = {
    val settings = new CsvWriterSettings
    settings.setFormat(defaultFormat)
    settings.setHeaderWritingEnabled(withHeader)
    settings.setHeaders("ID", "Name", "Password", "Prefix", "Partner", "Hash")
    new CsvWriter(out, settings)
  }

  private def parseRecord(record: Record): StudentRecord =
    StudentRecord(
      id = record.getInt("ID"),
      name = record.getString("Name"),
      passwordHash = record.getString("Password"),
      geneSeq = record.getString("Gene")
    )

  def readStudentCsvFile(file: File): Map[Int, StudentRecord] =
      reader.parseAllRecords(file)
        .map(parseRecord)
        .map( record => record.id -> record )
        .toMap

  def buildResultCsvString(result: Seq[ResultRecord], printWriter: PrintStream): Unit = {
    val writer = stringWriter(printWriter)
    writer.writeHeaders()
    result.foreach( result =>
      writer.writeRow(Array(result.id.toString, result.name, result.password.toString, result.prefix.toString, result.partnerId.toString, result.hash))
    )
    writer.flush()
  }
}
