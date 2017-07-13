package de.ax.uwt

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import StatusCodes._
import akka.stream.ActorMaterializer

import scala.io.{Source, StdIn}
import scala.util.Random

case class WebServer(piRun: Option[HasHistory]) {
  var errors: Seq[Throwable] = Seq.empty


  def online = errors.isEmpty


  def run() {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route =
      path("") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      } ~ path("health") {
        get {
          if (online) {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>ok</h1>"))
          } else {
            complete((InternalServerError, s"not online!: <pre>$errors</pre>"))
          }
        }
      } ~ path("status") {
        get {
          piRun match {
            case Some(pi) => complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, Source.fromFile("src/main/resources/status.html").mkString))
            case None => complete((StatusCodes.NotFound, s"no uwt given"))
          }
        }
      } ~ path("statusjson") {
        get {
          piRun match {
            case Some(pi) => complete(HttpEntity(ContentTypes.`application/json`, JSGen.genJson(pi)))
            case None => complete((StatusCodes.NotFound, s"no uwt given"))
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

object ServerTest extends App {
  val weekInMs: Long = 1000L * 60L * 60L * 24L * 7L
  private val ticks = 100
  val step: Long = weekInMs / ticks

  val flows=List("Hortensie", "Rose", "Efeu")
  private val history = new HasHistory {
    var times = 1.to(ticks+1).map(i => System.currentTimeMillis + (step * i))

    override def curMs: Long = {
      val h = times.head
      times = times.tail
      h
    }

    val r=new Random()

    1.to(ticks).foreach(i => {
      flowHistory = FlowHistoryEntry(new FlowLike {
        override def name: String = flows(r.nextInt(flows.size))
      }, math.random()*10, 10) +: flowHistory
    })

  }
  private val server = WebServer(Some(history))
  server.run()

}