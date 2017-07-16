package de.ax.uwt

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by nyxos on 11.07.17.
  */
object DryLaunscha extends App {

  println("DryLaunscha")

  val piRun = new DryRun()
  val runner = Future {
    println("running ryrun")
    piRun.run
  }

  println("running server")
  val server = WebServer(Some(piRun))
  server.run()


}
