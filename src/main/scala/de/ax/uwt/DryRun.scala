package de.ax.uwt

import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source

/**
  * Created by nyxos on 20.06.17.
  */
object DryRun extends App with UWT {

  case class IntOutputPin(i: Int) extends OutputPin {
    override def off: Unit = println(s"pin $i off")

    override def on: Unit = println(s"pin $i on")

    override def shutdown: Unit = {
      println(s"shutting down pin $i")
    }
  }

  case class IntInputPin(i: Int) extends InputPin {
    override def off: Unit = {}

    override def on: Unit = {}


    override def shutdown: Unit = {
      println(s"shutting down pin $i")
    }
  }

  implicit def i2p(i: Int): OutputPin = {
    IntOutputPin(i)
  }

  implicit def i2p2(i: Int): InputPin = {
    IntInputPin(i)
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
    println(s"sim waiting for $waitMs ms")
    val last = curMs
    while (curMs - last < waitMs) {
      Thread.sleep(math.min(waitMs,stepRangeMs/2))
    }
  }


  var lastMs = System.currentTimeMillis()

  val stepFactor = 20.0

  val stepRangeMs = 5

  def step: Unit = {
    {
      lastMs = lastMs + (stepRangeMs * stepFactor).toLong
    }
    {
      val x: Double = 1000 / (5.5 * 15)
      val ms = curMs
      val eventCount = (stepRangeMs * stepFactor) / x
      val actualCount = (0.15 * eventCount + (math.random() * 0.85 * eventCount)).toInt
      (1 to actualCount).toList.foreach(i => net.flows.head.pump.flowMeter.pin.handlers.foreach(h => {
        h((ms + (i * x)).toLong)
      }))
    }
  }

  override def curMs: Long = {
    lastMs
  }


  override def shouldStop: Boolean = false

  new Thread(() => {
    while (true) {
      step
      //      println(s"sleeping for $stepRangeMs")
      Thread.sleep(stepRangeMs)
    }
  }).start()
  while (true){
    doWater
    println("\n\n\nWATERED EVERYTHING \n\n\n")
  }
//  doSchedule(0.000277778)

}
