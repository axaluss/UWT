package de.ax.uwt

import cats.instances.future
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by nyxos on 11.07.17.
  */
object Launscha extends App  with LazyLogging{

  logger.info("Launsching")

  val piRun = new PiRun()
  val runner=Future{
    piRun.run
  }


   val server = WebServer(Some(piRun))
  server.run()


}
