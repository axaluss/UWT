package de.ax.uwt

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import StatusCodes._
import akka.stream.ActorMaterializer
import scala.io.StdIn

object WebServer {
  var errors:  Seq[Throwable] = Seq.empty


  def online = errors.isEmpty

  def main(args: Array[String]) {

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
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
