package de.ax.uwt

import java.io.File

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source
import com.github.nscala_time.time.Imports._
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}

/**
  * Created by nyxos on 20.06.17.
  */
case class WeatherData(time: Long, summary: String, temperature: Option[Double], temperatureMax: Option[Double], precipIntensity: Double, precipProbability: Double) {
  def dateTime: DateTime = new DateTime(time * 1000L)
}

case class Entry(data: List[WeatherData])

case class WeatherDataSet(currently: WeatherData, hourly: Entry, daily: Entry) {

}

object WeatherDataSet extends App with LazyLogging {
  def getIt(): Try[WeatherDataSet] = {
    Try {
      import sys.process._
      new File("tmpfile.json").delete()
      val i: Int = (s"wget --timeout=60 -O tmpfile.json https://api.darksky.net/forecast/d9db5456106658292cbd4a6dc3b6e18a/50.8958998,7.30826,${(System.currentTimeMillis() / 1000).toLong}?lang=de&units=ca" !)
      Source.fromFile("tmpfile.json", "UTF8").mkString
    }.flatMap { s =>
      decode[WeatherDataSet](s) match {
        case Left(e) => Failure(e)
        case Right(v) => Success(v)
      }
    }
  }

  var lastVal: Option[Try[WeatherDataSet]] = Option.empty
  var lastUpdate = System.currentTimeMillis()

  def getWeatherData: Try[WeatherDataSet] = {
    if (System.currentTimeMillis() - lastUpdate > (1000 * 60 * 60 * 2)) {
      lastVal = Option.empty
    }
    if (lastVal.forall(_.isFailure)) {
      lastVal = Some(getIt())
      lastUpdate = System.currentTimeMillis()
    }
    lastVal.getOrElse(Failure(new Exception("no cached weather data")))
  }

  getWeatherData.map { ds =>
    ds.hourly.data.filter(_.precipIntensity > 0).sortBy(_.dateTime).foreach(dt => logger.info(s"${Seq(dt.dateTime, dt.time, dt.precipIntensity, dt.precipIntensity, dt.precipProbability)}"))
    logger.info(s"got data count: ${ds.hourly.data.size}")
  }

  getWeatherData.failed.foreach(t => logger.error("weatherData Error", t))

  logger.info(s"getWeatherData:$getWeatherData")
}