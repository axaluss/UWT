package de.ax.uwt

import com.github.nscala_time.time.Imports.DateTime

import scala.annotation.tailrec

/**
  * Created by nyxos on 14.06.17.
  */

trait UWT {
  implicit def i2p(i: Int): Pin

  trait Pin {


    def off

    def on

  }


  abstract class Relais {
    val name: String
    val pin: Pin
    var isOn = false
    off

    def isOff = !isOn

    def off: Unit = {
      pin.off
      isOn = false
    }

    def on: Unit = {
      pin.on
      isOn = true
    }

  }


  case class Net(var elms: List[Relais] = List.empty,
                 var flows: List[Flow] = List.empty) {
    def valve(name: String, pin: Pin): Valve = {
      val out1 = Valve(name, pin)

      addElm(out1)
      out1
    }

    def pump(name: String, pin: Pin): Pump = {
      val out1 = Pump(name, pin)
      addElm(out1)
      out1
    }

    private def addElm(out1: Relais): Unit = {
      assert(!elms.exists(_.pin == out1.pin), s"pin ${out1.pin} already in use")
      assert(!elms.exists(_.name == out1.name), s"name '${out1.name}' for valve already in use")
      elms = elms :+ out1
    }

    def flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan): Flow = {
      val flow1 = Flow(name: String, pump: Pump, valve: Valve, flowPlan)
      assert(!flows.exists(f => f.valve == valve), s"valve $valve for flow $flow1 already in use")
      assert(!flows.exists(f => f.name == name), s"name '$name' for flow $flow1 already in use")
      flows = flows :+ flow1
      flow1
    }
  }


  case class Valve(name: String, pin: Pin) extends Relais

  case class Pump(name: String, pin: Pin) extends Relais

  case class Flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan)

  case class FlowPlan(name: String, minFlow: Double, maxFlow: Double)

  def setup: Net

  def getWeatherData: WeatherDataSet

  var wateringWaitTimeHours = 4

  def valueFactor = wateringWaitTimeHours / 24.0


  def calcLitersFromFlowPlan(flowPlan: FlowPlan): Double = {
    val datas = getWeatherData.hourly.data.filter { dt =>
      val radius = wateringWaitTimeHours / 2
      dt.dateTime.isAfter(DateTime.now.minusHours(radius)) || dt.dateTime.isBefore(DateTime.now.plusHours(radius))
    }
    val rainLiters = datas.map(dt => dt.precipIntensity * dt.precipProbability).sum
    val temp = datas.map(dt => dt.temperature.orElse(dt.temperatureMax).get).max
    val minTemp = 15
    val maxTemp = 30
    val tFactor: Double = (temp - minTemp) / (maxTemp - minTemp)
    val v = ((flowPlan.maxFlow * valueFactor) - rainLiters) * (tFactor.abs)
    val normalized = math.min(math.max(flowPlan.minFlow * valueFactor, v), flowPlan.maxFlow * valueFactor)
    println(s"to water $v=$normalized ((${flowPlan.maxFlow}-$rainLiters)*${tFactor.abs})")
    normalized
  }

  def doWait(waitMs: Long): Unit

  def doWater: Unit = {
    val net = setup
    checkAllOff(net)
    net.flows.foreach { case f@Flow(name, pump, valve, flowPlan) =>
      val liters = calcLitersFromFlowPlan(flowPlan)
      val waitMs = calcFlowTimeFromLiters(liters)
      println(s"watering $liters l in $waitMs ms for flow $f")
      try {
        valve.on
        pump.on
        doWait(waitMs)
      } finally {
        pump.off
        valve.off
      }
    }
  }

  def pumpLitersPerMinute = 14.0

  private def calcFlowTimeFromLiters(liters: Double) = {
    ((liters / pumpLitersPerMinute) * 60 * 1000).toInt
  }

  private def checkAllOff(net: Net) = {
    val relaises = net.elms.filter(_.isOn)
    assert(relaises.isEmpty, s"there were enabled elements: $relaises")
  }

  def shouldStop: Boolean

  @tailrec final def doSchedule(loopHours: Int, start: DateTime = DateTime.now()): Unit = {
    wateringWaitTimeHours = loopHours
    if (!shouldStop) {
      if (DateTime.now.isAfter(start)) {
        doWater
        doSchedule(loopHours, start.plusHours(loopHours))
      } else {
        doWait(60 * 1000)
        doSchedule(loopHours, start)
      }
    }
  }
}


