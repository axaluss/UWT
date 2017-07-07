package de.ax.uwt

import com.github.nscala_time.time.Imports.DateTime

import scala.annotation.tailrec

/**
  * Created by nyxos on 14.06.17.
  */

trait UWT {
  implicit def i2p(i: Int): OutputPin

  implicit def i2p2(i: Int): InputPin

  trait Pin {


    def off

    def on

    def shutdown

  }

  trait OutputPin extends Pin

  trait InputPin extends Pin {
    var handlers: List[(Long) => Unit] = List.empty

    def addHandler(h: (Long) => Unit): Unit = {
      handlers = handlers :+ h
    }
  }


  abstract class Output {
    val name: String
    val pin: OutputPin
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

  abstract class Input {
    val name: String
    val pin: InputPin

  }


  case class Valve(name: String, pin: OutputPin) extends Output

  case class Pump(name: String, pin: OutputPin, flowMeter: FlowMeter) extends Output

  case class FlowMeter(name: String, pin: InputPin) extends Input


  case class Net(var elms: List[Output] = List.empty,
                 var flows: List[Flow] = List.empty) {
    def valve(name: String, pin: OutputPin): Valve = {
      val out1 = Valve(name, pin)

      addElm(out1)
      out1
    }

    var flowEvents: List[Long] = List.empty

    def addFlowEvent(i: Long): Unit = {
      flowEvents = (i +: flowEvents).filter((e: Long) => (curMs - e) < 1000)
    }


    def pump(name: String, pin: OutputPin, flowMeter: FlowMeter): Pump = {
      flowMeter.pin.addHandler(addFlowEvent)
      val out1 = Pump(name, pin, flowMeter)
      addElm(out1)
      out1
    }

    private def addElm(out1: Output): Unit = {
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


  case class Flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan)

  case class FlowPlan(name: String, minFlow: Double, maxFlow: Double)

  def setup: Net

  def getWeatherData: WeatherDataSet


  def pumpLitersPerMinute: Double = {
    val es = net.flowEvents.filter((e: Long) => (e - curMs) < 1000)
    es.size / 5.5
  }


  var wateringWaitTimeHours: Double = 4

  def valueFactor = wateringWaitTimeHours / 24.0

  def calcLitersFromFlowPlan(flowPlan: FlowPlan): Double = {
    val datas = getWeatherData.hourly.data.filter { dt =>
      val radius = (wateringWaitTimeHours / 2).toInt
      dt.dateTime.isAfter(DateTime.now.minusHours(radius)) || dt.dateTime.isBefore(DateTime.now.plusHours(radius))
    }
    val rainLiters = datas.map(dt => dt.precipIntensity * dt.precipProbability).sum * 2
    val temp = datas.map(dt => dt.temperature.orElse(dt.temperatureMax).get).max
    val minTemp = 15
    val maxTemp = 30
    val tFactor: Double = (temp - minTemp) / (maxTemp - minTemp)
    val v = ((flowPlan.maxFlow * valueFactor) - rainLiters * valueFactor) * (tFactor.abs)
    val normalized = math.min(math.max(flowPlan.minFlow * valueFactor, v), flowPlan.maxFlow * valueFactor)
    println(s"to water $v=$normalized ((${flowPlan.maxFlow}-$rainLiters)*${tFactor.abs})")
    normalized
  }

  def doWait(waitMs: Long): Unit

  val net = setup

  def waitForLiters(liters: Double): Unit = {
    var litersFlowed = 0.0
    val started = curMs
    while (pumpLitersPerMinute.isNaN || pumpLitersPerMinute.isInfinity) {
      println("waiting for pumpLitersPerMinute")
      doWait(100)
    }
    println(s"waiting for $liters l with $pumpLitersPerMinute l/m")
    var lastPrint = started
    while (litersFlowed < liters) {
      var lastCheck = curMs
      doWait(100)
      litersFlowed += (pumpLitersPerMinute / 60.0 / 1000.0) * (curMs - lastCheck)
      if (curMs - lastPrint > 1000|| litersFlowed > liters) {
        //      println(s"$curMs - $lastCheck = ${(curMs - lastCheck)}")
        lastPrint=curMs
        println(s"waited ${curMs - started} ms for $litersFlowed l with $pumpLitersPerMinute l/m")
      }
    }
  }

  def curMs: Long

  def doWater: Unit = {
    checkAllOff(net)
    net.flows.foreach { case f@Flow(name, pump, valve, flowPlan) =>
      val liters = calcLitersFromFlowPlan(flowPlan)
      println(s"watering $liters l for flow $f")
      try {
        valve.on
        pump.on
        waitForLiters(liters)
      } finally {
        pump.off
        valve.off
      }
    }
  }


  private def checkAllOff(net: Net) = {
    val relaises = net.elms.filter(_.isOn)
    assert(relaises.isEmpty, s"there were enabled elements: $relaises")
  }

  def shouldStop: Boolean

  @tailrec final def doSchedule(loopHours: Double, start: DateTime = DateTime.now()): Unit = {
    println(s"waiting for $start")
    wateringWaitTimeHours = loopHours
    if (!shouldStop) {
      if (DateTime.now.isAfter(start)) {
        doWater
        doSchedule(loopHours, start.plusSeconds((loopHours * 60 * 60).toInt))
      } else {
        doWait(1000)
        doSchedule(loopHours, start)
      }
    }
  }
}


