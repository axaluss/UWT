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
    def identifier: Any


    override def equals(obj: scala.Any): Boolean = {
      obj match {
        case null =>
          false
        case pin: Pin =>
          pin.identifier == identifier
        case _ =>
          false
      }
    }

    def shutdown

  }

  trait OutputPin extends Pin {


    def off

    def on
  }

  trait InputPin extends Pin {
    def clearHandlers = {
      handlers = List.empty
    }

    var handlers: List[(Long) => Unit] = List.empty

    def addHandler(h: (Long) => Unit): Unit = {
      handlers = handlers :+ h
    }
  }

  trait Put {
    val name: String
    val pin: Pin
  }

  abstract class Output extends Put {
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

  abstract class Input extends Put {


  }


  case class Valve(name: String, pin: OutputPin) extends Output

  case class Switch(name: String, pin: OutputPin) extends Output

  case class Pump(name: String, pin: OutputPin, flowMeter: FlowMeter) extends Output

  case class FlowMeter(name: String, pin: InputPin) extends Input

  case class MoistureSensor(name: String, pin: InputPin, switch: Switch) extends Input {
    def hasWater: Boolean = {
      var hasWater = false
      pin.addHandler((t) => {
        hasWater = true
      })
      switch.on
      doWait(500)
      switch.off
      pin.clearHandlers
      hasWater
    }
  }


  case class Net(var elms: List[Put] = List.empty,
                 var flows: List[Flow] = List.empty) {


    def valve(name: String, pin: OutputPin): Valve = {
      val out1 = Valve(name, pin)

      addElm(out1)
      out1
    }

    var switch12V: Option[Output] = Option.empty

    def switch12v(s: Output) = {
      switch12V = Some(s)
      switch12V.foreach(_.off)
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


    def moistureSensor(name: String, pin: InputPin, switch: Switch) = {
      val s = MoistureSensor(name, pin, switch)
      addElm(s)
      s
    }

    def switch(name: String, pin: OutputPin): Switch = {
      val out1 = Switch(name, pin)

      addElm(out1)
      out1
    }

    def flowMeter(name: String, pin: InputPin) = {
      val s = FlowMeter(name, pin)
      addElm(s)
      s
    }

    private def addElm(out1: Put): Unit = {
      val puts = elms.filter(_.pin == out1.pin)
      assert(puts.isEmpty, s"pin $out1 already in use in $puts")
      val puts1 = elms.filter(_.name == out1.name)
      assert(puts1.isEmpty, s"name '$out1' for valve already in use in $puts1")
      elms = elms :+ out1
    }

    def flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan, mSensor: MoistureSensor): Flow = {
      val flow1 = Flow(name: String, pump: Pump, valve: Valve, flowPlan, mSensor)
      assert(!flows.exists(f => f.valve == valve), s"valve $valve for flow $flow1 already in use")
      assert(!flows.exists(f => f.name == name), s"name '$name' for flow $flow1 already in use")
      flows = flows :+ flow1
      flow1
    }
  }

  case class Flow(name: String, pump: Pump, valve: Valve, flowPlan: FlowPlan, mSensor: MoistureSensor)

  case class FlowPlan(name: String, minFlow: Double, maxFlow: Double)

  def setup: Net

  def getWeatherData: WeatherDataSet


  def pumpLitersPerMinute: Double = {
    val es = net.flowEvents.filter((e: Long) => (curMs - e) < 1000)
    es.size / 5.5
  }


  var wateringWaitTimeHours: Double = 4

  def valueFactor = wateringWaitTimeHours / 24.0

  def calcLitersFromFlowPlan(flowPlan: FlowPlan, mSensor: MoistureSensor): Double = {
    val datas = getWeatherData.hourly.data.filter { dt =>
      val radius = (wateringWaitTimeHours / 2).toInt
      dt.dateTime.isAfter(DateTime.now.minusHours(radius)) || dt.dateTime.isBefore(DateTime.now.plusHours(radius))
    }
    val rainLiters = datas.map(dt => dt.precipIntensity * dt.precipProbability).sum * 2
    val temp = datas.map(dt => dt.temperature.orElse(dt.temperatureMax).get).max
    val minTemp = 15
    val maxTemp = 30
    val tFactor: Double = (temp - minTemp) / (maxTemp - minTemp)
    val v = ((flowPlan.maxFlow * valueFactor) - (rainLiters * valueFactor)) * tFactor.abs
    val normalized = math.min(math.max(flowPlan.minFlow * valueFactor, v), flowPlan.maxFlow * valueFactor)
    println(s"to water $v=$normalized ((${flowPlan.maxFlow} * $valueFactor -($rainLiters * $valueFactor))*${tFactor.abs})")
    if (mSensor.hasWater) {
      println("had water!")
      normalized * 0.1
    } else {
      normalized
    }
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
    while ((litersFlowed - liters).abs < 0.001) {
      var lastCheck = curMs
      doWait(100)
      litersFlowed += (pumpLitersPerMinute / 60.0 / 1000.0) * (curMs - lastCheck)
      if (curMs - lastPrint > 1000 || litersFlowed > liters) {
        //      println(s"$curMs - $lastCheck = ${(curMs - lastCheck)}")
        lastPrint = curMs
        println(s"waited ${curMs - started} ms for $litersFlowed l with $pumpLitersPerMinute l/m")
      }
    }
  }

  def curMs: Long

  case class FlowHistoryEntry(f: Flow, actualLiters: Double, durationMs: Long) {
    val time: Long = curMs
  }

  var flowHistory: Seq[FlowHistoryEntry] = Seq.empty

  def addHistory(e: FlowHistoryEntry) {
    flowHistory = (e +: flowHistory).filter(curMs - _.time < (1000 * 60 * 60 * 24 * 7))
  }

  def doWater: Unit = {
    checkAllOff(net)
    net.flows.foreach { case f@Flow(name, pump, valve, flowPlan, mSensor) =>
      val liters = calcLitersFromFlowPlan(flowPlan, mSensor)
      println(s"watering $liters l for flow $f")
      try {
        val start = curMs
        net.switch12V.foreach(_.on)
        valve.on
        pump.on
        waitForLiters(liters)
        addHistory(FlowHistoryEntry(f, liters, curMs - start))
      } finally {
        pump.off
        valve.off
        net.switch12V.foreach(_.off)
      }
    }
  }


  private def checkAllOff(net: Net) = {
    val relaises = net.elms.filter {
      case e: Output => e.isOn
      case _ => false
    }
    assert(relaises.isEmpty, s"there were enabled elements: $relaises")
  }

  def shouldStop: Boolean

  @tailrec final def doSchedule(loopHours: Double, start: DateTime = DateTime.now()): Unit = {
    println(s"waiting for $start")
    wateringWaitTimeHours = loopHours
    if (!shouldStop) {
      if (DateTime.now.isAfter(start)) {
        try {
          doWater
        } catch {
          case e => WebServer.errors = e +: WebServer.errors
            throw e
        }
        doSchedule(loopHours, start.plusSeconds((loopHours * 60 * 60).toInt))
      } else {
        doWait(1000)
        doSchedule(loopHours, start)
      }
    }
  }
}


