package de.ax.uwt

import com.github.nscala_time.time.Imports
import com.pi4j.io.gpio._
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.parser._

import scala.io.Source
import scala.util.Try

/**
  * Created by nyxos on 20.06.17.
  */
class PiRun extends UWT {


  case class IntOutputPin(i: Int) extends OutputPin {
    private val pin = RaspiPin.getPinByName(s"GPIO $i")
    val digitalOutput: GpioPinDigitalOutput = GpioFactory.getInstance.provisionDigitalOutputPin(pin, pin.getName, PinState.LOW)
    off

    override def off: Unit = digitalOutput.low()

    override def on: Unit = digitalOutput.high()

    override def shutdown: Unit = {
      GpioFactory.getInstance.unprovisionPin(digitalOutput)
    }

    override def identifier: Any = i
  }

  case class IntInputPin(i: Int) extends InputPin {
    private val pin = RaspiPin.getPinByName(s"GPIO $i")
    val digitalInput: GpioPinDigitalInput = GpioFactory.getInstance.provisionDigitalInputPin(pin, pin.getName, PinPullResistance.PULL_DOWN)

    digitalInput.addListener(new GpioPinListenerDigital {
      override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
//        println(s"event state of $i is ${event.getState.getName}")
        val millis = curMs
        handlers.foreach(h => h(millis,event.getState.isHigh))
      }
    })

    override def shutdown: Unit = {
      GpioFactory.getInstance.unprovisionPin(digitalInput)
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


  override def getWeatherData: Try[WeatherDataSet] = {
    WeatherDataSet.getWeatherData
  }

  override def doWait(waitMs: Long): Unit = Thread.sleep(waitMs)

  override def shouldStop: Boolean = false


  override def curMs: Long = System.currentTimeMillis()

  def run: Unit = {
    doSchedule(3)
//    while(true){
//      doWater
//      println("waiting until next watering 5s....")
//      doWait(5000)
//    }
  }

  override def doWaitUntil(start: Imports.DateTime): Unit = {
    doWait(1000)
  }
}
