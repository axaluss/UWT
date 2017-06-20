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


  doWater

  override def getWeatherData: WeatherDataSet = {
    val res = decode[WeatherDataSet](Source.fromFile("testdata/meisenbach.json").mkString)
    val option = res.toOption
    option.get
  }

  override def doWait(waitMs: Long): Unit = {
    val l = (waitMs / 1000) + 1
    println(s"waiting for $waitMs ms")
    Thread.sleep(100 * l)
  }
}
