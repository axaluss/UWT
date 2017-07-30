package de.ax.uwt

import com.github.nscala_time.time.Imports
import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source
import scala.util.Random

/**
  * Created by nyxos on 20.06.17.
  */
class DryRun extends UWT {

  case class IntOutputPin(i: Int) extends OutputPin {
    override def off: Unit = println(s"pin $i off")

    override def on: Unit = println(s"pin $i on")

    override def shutdown: Unit = {
      println(s"shutting down pin $i")
    }

    override def identifier: Any = i
  }

  case class IntInputPin(i: Int) extends InputPin {

    override def shutdown: Unit = {
      println(s"shutting down pin $i")
    }

    override def identifier: Any = i
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
    val strings = List(
      "testdata/meisenbach.json",
      "testdata/anchorage.json",
      "testdata/karlsruhe.json",
      "testdata/memmingen.json",
      "testdata/mindelheim.json",
      "testdata/ulm.json")
    val res = decode[WeatherDataSet](Source.fromFile(strings(Random.nextInt(strings.length))).mkString)
    val option = res.toOption
    option.get
  }

  override def doWait(waitMs: Long): Unit = {
    //    println(s"sim waiting for $waitMs ms")
    val last = curMs
    stepHygroMeter
    while (curMs - last < waitMs) {
      Thread.sleep(math.min(waitMs, stepRangeMs / 2))
    }
    stepHygroMeter
  }


  var lastMs = System.currentTimeMillis()

  val stepFactor = 50.0

  val stepRangeMs = 5


  def step: Unit = {
    stepTime
    stepFlowMeter
    stepHygroMeter
  }

  def stepHygroMeter: Unit = {
    // hygro detects water only when ishigh=false
    net.flows.head.mSensor.pin.handlers.foreach(_.apply(47111, false))
  }

  private def stepTime = {
    lastMs = lastMs + (stepRangeMs * stepFactor).toLong
  }

  private def stepFlowMeter = {
    val x: Double = 1000 / (5.5 * 15)
    val ms = curMs
    val eventCount = (stepRangeMs * stepFactor) / x
    val actualCount = (0.15 * eventCount + (math.random() * 0.85 * eventCount)).toInt
    (1 to actualCount).toList.foreach(i => net.flows.head.pump.flowMeter.pin.handlers.foreach(h => {
      h((ms + (i * x)).toLong, true)
    }))
  }

  override def curMs: Long = {
    lastMs
  }


  override def shouldStop: Boolean = false

  def doWaitUntil(start: Imports.DateTime): Unit = {
    lastMs = start.getMillis
  }


  def run: Unit = {
    new Thread(() => {
      while (true) {
        step
        //      println(s"sleeping for $stepRangeMs")
        Thread.sleep(stepRangeMs)
      }
    }).start()
    //    while (true) {
    //      doWater
    //      println(s"history: ${flowHistory.mkString("\n")}")
    //      println("\n\n\nWATERED EVERYTHING \n\n\n")
    //    }
    doSchedule(4)
  }


}

object Dry extends App {
  println("Dry.run")
  new DryRun().run
}