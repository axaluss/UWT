package de.ax.uwt

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by nyxos on 11.07.17.
  */
object DryLaunscha extends App with LazyLogging{

  logger.info("DryLaunscha")

  val piRun = new DryRun()
  val runner = Future {
    logger.info("running ryrun")
    piRun.run
  }

  logger.info("running server")
  val server = WebServer(Some(piRun))
  server.run()


}
