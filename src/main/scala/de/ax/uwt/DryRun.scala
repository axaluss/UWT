package de.ax.uwt

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source

/**
  * Created by nyxos on 20.06.17.
  */
object DryRun extends App with UWT {

  case class IntPin(i: Int) extends Pin {
    override def off: Unit = println(s"pin $i off")

    override def on: Unit = println(s"pin $i on")
  }

  implicit def i2p(i: Int): Pin = {
    IntPin(i)
  }


  def setup: Net = {
    RealNet.net(this)
  }




  override def getWeatherData: WeatherDataSet = {
    val res = decode[WeatherDataSet](Source.fromFile("testdata/meisenbach.json").mkString)
    val option = res.toOption
    option.get
  }

  override def doWait(waitMs: Long): Unit = {
    val l = (waitMs / 1000) + 1
    val actual = 100 * l
    println(s"sim waiting for $waitMs ($actual) ms")
    Thread.sleep(actual)
  }

  doSchedule(1)

  override def shouldStop: Boolean = false
}
