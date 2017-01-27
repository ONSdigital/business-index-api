package uk.gov.ons.ingest

import org.apache.spark.sql.DataFrame
import org.rogach.scallop.ScallopConf

/**
  * Main executed file
  */
object SparkApp extends App {
  val opts = new ScallopConf(args) {

    banner("""This master Spark runner builds the individual indexes that help generate the master record""")

    val help = opt[Boolean]("help", noshort = true, descr = "Show this message")
  }

  if (!opts.help()) {
  } else {
    opts.printHelp()
  }

  private[this] def ingest() = {
    val client
    val spark = new SparkIngestion()
  }

}
