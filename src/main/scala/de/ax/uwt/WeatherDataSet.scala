package de.ax.uwt

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source
import com.github.nscala_time.time.Imports._

/**
  * Created by nyxos on 20.06.17.
  */
case class WeatherData(time: Long, summary: String, temperature: Option[Double], temperatureMax: Option[Double], precipIntensity: Double, precipProbability: Double) {
  def dateTime: DateTime = new DateTime(time * 1000L)
}

case class Entry(data: List[WeatherData])

case class WeatherDataSet(currently: WeatherData, hourly: Entry, daily: Entry) {

}

object TT extends App {

  def res = decode[WeatherDataSet](Source.fromFile("anchorage.json").mkString)
  res.map { ds =>
    ds.hourly.data.sortBy(_.dateTime).filter(_.precipIntensity > 0).foreach(dt => println(dt.dateTime, dt.time, dt.precipIntensity, dt.precipIntensity,dt.precipProbability))
    println()
    println(ds.hourly.data.size)
  }
  println(res)
}