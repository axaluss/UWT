package de.ax.uwt

import java.io.File
import java.security.Security

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source
import com.github.nscala_time.time.Imports._
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

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

  Security.setProperty("jdk.tls.disabledAlgorithms", "")

  def getWeatherData: Try[WeatherDataSet] = Try {
    import sys.process._
    new File("tmpfile.json").delete()
    val i: Int = (s"wget --timeout=60 -O tmpfile.json https://api.darksky.net/forecast/d9db5456106658292cbd4a6dc3b6e18a/50.8958998,7.30826,${(System.currentTimeMillis() / 1000).toLong}?lang=de&units=ca" !)
    Source.fromFile("tmpfile.json", "UTF8").mkString
  }.flatMap { s =>
    Try {
      decode[WeatherDataSet](s).toOption.get
    }
  }

  getWeatherData.map { ds =>
    ds.hourly.data.filter(_.precipIntensity > 0).sortBy(_.dateTime).foreach(dt => logger.info(s"${Seq(dt.dateTime, dt.time, dt.precipIntensity, dt.precipIntensity, dt.precipProbability)}"))
    logger.info(s"got data count: ${ds.hourly.data.size}")
  }
  logger.info(s"error: ${getWeatherData.failed.map(t => t.printStackTrace())}")
  logger.info(s"getWeatherData:$getWeatherData")
}